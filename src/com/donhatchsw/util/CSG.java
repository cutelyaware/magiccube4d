// 2 # 1 "com/donhatchsw/util/CSG.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/CSG.prejava"
/*
* Copyright (c) 2005,2006 Don Hatch Software
*/

//
// CSG.prejava
//
// Contains functions for doing boolean operations (union, intersection, diff)
// on polytopes (i.e. polygons or polyhedra or ...).
//
// Depends on:
//      VecMath.prejava
//      SortStuff.prejava
//
/*
    Bugs to fix:
        - fix signs like I said I would (what did I say? I can't recall)
        Was:
            -dim=1: A&A arrayIndexOutOfBounds: -1, in SortStuff.swap
            -dim=1: A-A arrayIndexOutOfBounds, same
            -dim=1: A|A arrayIndexOutOfBounds, same

            -dim=2: A&A Assertion failed at CSG.prejava(1240): Arrays.indexOf(slicee.p.contributingHyperplanes, hyperplane) == -1
            -dim=2: A-A same assertion failure
            -dim=2: A|A same assertion failure
        Now:
            -dim=1: A&A gives bogus bombed-out structure
            -dim=2: A&A Assumption failed at CSG.prejava(2392): n == list.length
            and simlarly for the other tests listed above
        Bleah, looking it over again, it seems like the algorithm
        can't handle A&A-- it computes the boundary of the intersection
        of A&B as bd(A)&B union A&bd(B), which is null...
        need a fundamental insight to fix this.

    To consider:
        - Should try to explain what the signs mean.  Some things
          to maybe mention...
            - note, the sum of signed vertices on an edge
              is actually the directed length of the edge
            - going back over an edge (or face) in the opposite
              direction means erasing it
            - for a proper edge, the sum of all vert's signs must be 0
              (otherwise it's a ray or something)
            - for a closed loop or surface, the boundary must have
              no boundary, i.e. the sum of the boundary of the boundary
              must be zero.
            - intersection is really min, union is really max,
              complement is really 1-.  The allowed polytopes
              are those that are constant density outside
              a bounded region; that constant density
              is called "initialDensity".
              The most common ones are finite and cofinite (i.e.
              those that are density 1 on a finite region and 0 elsewhere,
              and vice-versa.)
              (Might also allow more some day, but not at the moment.)
            - hmm, assuming the a.e. constant density restriction,
              this means all but top level polytopes
              must have initial density 0.
            - interesting, if we have a varying-positive-density
              polytope and we want the density-1 version,
              just intersect (i.e. min) it with 1.

        - does initialDensity have a meaning for vertices? (should it be 1? -1?)
        - make params X,Y instead of A,B? (since A,B are typical
              arguments; this might make debugging less confusing)
              But then I'll have to think of an alternative
              for alpha,beta that is not confusing...
              yamma, zigga? yalpha, zeta?
              ha, my humor is above everyone's head.
*/

package com.donhatchsw.util;

// 79 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 83 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 85 # 75 "com/donhatchsw/util/CSG.prejava" 2


/**
*  Contains functions for doing n-dimensional Constructive Solid Geometry (CSG),
*  that is, boolean operations (union, intersection, diff)
*  on polytopes (i.e polygons or polyhedra or ...).
*  <p>
*  The objects that the boolean operators operate on
*  are CSG.Polytope and CSG.SPolytope (signed polytope).
*  The simplest way to create a CSG.Polytope
*  is probably by using the PolyCSG class.
*  Some primitive CSG.SPolytopes can also be made using makePolygon(),
*  makeSimplex(), and makeHypercube().
*  <p>
*  Caveats: not at all robust about concident or coplanar elements;
*  really only works reliably if everything is in general position.
*  <p>
*  Depends on classes:
*  <ul>
*       <li> Arrays </li>
*       <li> VecMath </li>
*       <li> SortStuff </li>
*       <li> FuzzyPointHashTable </li>
*  </ul>
*/
public final class CSG
{
    private CSG() {} // uninstantiatable
    //
    // Debugging setting...
    // I just set this appropriately and recompile.
    // XXX note, not final so that it can be changed in main.
    // XXX this should maybe be a compile-time option,
    // XXX since making it final gets rid of a lot of code.
    //
    //public static int verboseLevel = 2; // 3 = show gory details (XXX which are really gory and I should probably do away with)
    //public static int verboseLevel = 1; // 3 = show gory details (XXX which are really gory and I should probably do away with)
    //public static int verboseLevel = 0; // 3 = show gory details (XXX which are really gory and I should probably do away with)
    public static int verboseLevel = -1; // 3 = show gory details (XXX which are really gory and I should probably do away with)

    //
    // Data structures...
    //

        private static long nIds = 0; // so we can give a unique integer id to everything ever created
        private static Object nIdsLock = new Object(); // for synchronization
        private static java.util.Random randomGenerator = new java.util.Random(0);

        /**
        *  A hyperplane is the set of all
        *  points p such that (p dot normal) = offset.
        *  The closed halfspace it bounds is the set of all points p
        *  such that (p dot normal) <= offset.
        */
        public static class Hyperplane
        {
            public long id;
            public double normal[];
            public double offset;
            public double spanningPoints[][]; // optional; avoids recalculation of points (and resulting roundoff error) when points was the original representation.  XXX this idea seems to be lacking... e.g. if there are more than dim(plane)+1 points on the polygon, then we have to omit some.  think about this :-( )
            /** Creates a Hyperplane from a plane equation. */
            public Hyperplane(double normal[], double offset)
            {
                synchronized(nIdsLock)
                {
                    this.id = nIds++;
                }
                this.normal = normal;
                this.offset = offset;
                this.spanningPoints = null;
            } // Hyperplane from plane equation
            /** Creates a Hyperplane from a set of dim spanning points. */
            public Hyperplane(double spanningPoints[][])
            {
                synchronized(nIdsLock)
                {
                    this.id = nIds++;
                }
                this.spanningPoints = spanningPoints;

                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+155 +"): " + "false" + ""); } while (false); // XXX implement me! normal should be cross product of points vectors
                this.offset = VecMath.dot(this.normal,
                                          this.spanningPoints[0]);
            } // Hyperplane from spanning points

            public String toString()
            {
                return VecMath.toString(normal) + " " + offset;
            }
        } // class Hyperplane

        /**
        *  Unsigned (unoriented) polytope,
        *  defined by its boundary.
        *  <p>
        *  Note, the "dim" member wasn't stored here originally since
        *  it can be inferred as (dim of planes[0].normal) - (# of planes),
        *  except when (# of planes) == 0, in which case we can still infer
        *  it (recursively) as (dim of facets)+1, unless there
        *  are no facets... This was giving me a headache,
        *  so I'm just storing it explicitly here.
        *  <p>
        *  Similarly for "fullDim", only more so.
        */
        public static class Polytope
        {
            public long id;
            public int dim; // dimension of the polytope
            public int fullDim; // dimension of the space it lives in
            public SPolytope facets[];
            public Hyperplane contributingHyperplanes[]; // the hyperplanes whose intersection is the space of this Polytope, in sorted order. // XXX sigh, should probably be a HashableSortedArray so we don't have to keep creating and destorying them every time we want to look it up... but that means this definition is getting more and more obscured
            public Object aux; // user data; creator can set this if desired
            private double _coords[]; // if vertex, null otherwise.  XXX should probably calculate only on demand, but then need a way to differentiate between dirty and nonexistent (for example, this is needed in getBBox below). XXX ctually not currently calculated at all.
            private double _bbox[/*2*/][]; // bounding box of all vertices, so it can be finite even if the polytope is infinite (co-finite).  calculated Lazily via getBBox().
            private Polytope[/*dim+1*/][] _allElements; // sorted lists of vertices, edges, ..., ridges, facets, calculated lazily via getAllElements()
            private int[/*dim+1*/][][/*dim+1*/][] _allIncidences; // calculated lazily via getAllElements
            public void resetAllElements() { _allElements = null; _allIncidences = null; }
            
            public Polytope(int dim,
                            int fullDim,
                            SPolytope facets[],
                            Hyperplane contributingHyperplanes[],
                            Object aux)
            {
                if (verboseLevel >= 3)
                    System.out.println("in Polytope ctor");
                synchronized(nIdsLock)
                {
                    this.id = nIds++;
                }
                this.dim = dim;
                this.fullDim = fullDim;
                this.facets = facets;
                this.contributingHyperplanes = contributingHyperplanes;
                this.aux = aux;
                this._coords = null;
                this._bbox = null;
                this._allElements = null;

                if (verboseLevel >= 3)
                {
                    System.out.print("    making "+this.id+": [");
                    if (facets == null)
                        System.out.println("(null)");
                    else
                        for (int iFacet = 0; (iFacet) < (facets.length); ++iFacet)
                        {
                            System.out.print(" "
                                           + (facets[iFacet].sign==1?"+":facets[iFacet].sign==-1?"-":facets[iFacet].sign==0?"!":"?")
                                           + facets[iFacet].p.id);
                        }
                    System.out.println(" ]");
                }

                if (facets != null) // XXX maybe shouldn't be allowed, but makeHyperCube does it
                {
                    int nFacets = facets.length;
                    for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                    {
                        Polytope facet = facets[iFacet].p;
                        do { if (!(facet.fullDim == fullDim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+234 +"): " + "facet.fullDim == fullDim" + ""); } while (false);
                        do { if (!(facet.dim == dim-1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+235 +"): " + "facet.dim == dim-1" + ""); } while (false);
                    }
                }

                if (verboseLevel >= 3)
                    System.out.println("out Polytope ctor");
            }

            // Note this just copies the pointer,
            // so don't give it a scratch buffer that you will overwrite!
            // (should take a const double[]).
            public void setCoords(double coords[])
            {
                _coords = coords;
            }

            // Returns a pointer to coords.
            // It is illegal to modify the contents
            // (should return const double[]).
            public double[] getCoords()
            {
                if (_coords == null)
                {
                    do { if (!(dim == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+258 +"): " + "dim == 0" + ""); } while (false); // must be a vertex
                    _coords = intersectHyperplanes(contributingHyperplanes);
                }
                do { if (!(_coords != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+261 +"): " + "_coords != null" + ""); } while (false); // XXX not sure if I'm preventing this
                return _coords;
            } // getCoords

            //
            // When this is called, it is assumed
            // that the coords and connectivity
            // will not henceforth change.
            // XXX well I'm not completely firm on that, but if they do change, the bbox will have to be marked dirty, probably by setting it to null.
            //
            public double[/*2*/][] getBBox()
            {
                if (_bbox == null)
                {
                    // XXX should maybe say "if it's a vertex", i.e. if contributingHyperplanes.length == contributingHyperplanes[0].normal.length
                    if (dim == 0)
                    {
                        double coords[] = getCoords();
                        _bbox = new double[/*2*/][] {coords, coords};
                    }
                    else
                    {
                        _bbox = new double[2][fullDim];
                        VecMath.fillvec(_bbox[0], Double.POSITIVE_INFINITY);
                        VecMath.fillvec(_bbox[1], Double.NEGATIVE_INFINITY);
                        for (int iFacet = 0; (iFacet) < (facets.length); ++iFacet)
                        {
                            double facetBBox[][] = facets[iFacet].p.getBBox();
                            for (int iDim = 0; (iDim) < (_bbox[0].length); ++iDim)
                            {
                                _bbox[0][iDim] = ((_bbox[0][iDim])<=(facetBBox[0][iDim])?(_bbox[0][iDim]):(facetBBox[0][iDim]));
                                _bbox[1][iDim] = ((_bbox[1][iDim])>=(facetBBox[1][iDim])?(_bbox[1][iDim]):(facetBBox[1][iDim]));
                            }
                        }
                    }
                }
                return _bbox;
            } // getBBox

            /**
            * Get a list of all verts, edges, faces, ..., ridges, facets, self
            * of this polytope.
            * getAllElements()[iDim] is the list of iDim-dimensional sub-polytopes.
            */
            public Polytope[/*dim+1*/][] getAllElements()
            {
                if (_allElements == null)
                {
                    class VisitedAux
                    {
                        public Object savedAux;
                        public VisitedAux(Object savedAux)
                        {
                            this.savedAux = savedAux;
                        }
                    }

                    java.util.Vector lists[] = new java.util.Vector[dim+1];
                    {
                        for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                            lists[iDim] = new java.util.Vector();
                        java.util.Vector flatList = new java.util.Vector();

                        this.aux = new VisitedAux(this.aux); // mark it visited when queued
                        flatList.addElement(this);
                        for (int flatIndex = 0; (flatIndex) < (flatList.size()); ++flatIndex) // while flatList.size() is increasing!
                        {
                            Polytope elt = (Polytope)flatList.get(flatIndex);
                            lists[elt.dim].addElement(elt);
                            for (int iFacet = 0; (iFacet) < (elt.facets.length); ++iFacet)
                            {
                                Polytope facet = elt.facets[iFacet].p;
                                if (!(facet.aux instanceof VisitedAux)) // save even when elt.aux is null
                                {
                                    facet.aux = new VisitedAux(facet.aux); // mark it visited when queued
                                    flatList.addElement(facet);
                                }
                            }
                        }
                    }

                    _allElements = new Polytope[dim+1][];
                    for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                    {
                        _allElements[iDim] = new Polytope[lists[iDim].size()];
                        lists[iDim].copyInto(_allElements[iDim]);
                        SortStuff.sort(_allElements[iDim],
                                       new SortStuff.Comparator() {
                                           public int compare(Object _a, Object _b)
                                           {
                                               Polytope a = (Polytope)_a;
                                               Polytope b = (Polytope)_b;
                                               return a.id < b.id ? -1 :
                                                      a.id > b.id ? 1 : 0;
                                           }
                                       });
                        for (int iElt = 0; (iElt) < (_allElements[iDim].length); ++iElt)
                        {
                            Polytope elt = _allElements[iDim][iElt];
                            elt.aux = ((VisitedAux)elt.aux).savedAux;
                        }
                    }
                } // if _allElements == null
                return _allElements;
            } // getAllElements()

            /**
            * Get a list of all the incidences between elements
            * occurring inside this polytope.
            *     getAllIncidences()[iDim][iElt][jDim]
            * is the list of all indices (into getAllElements()[jDim])
            * of jDim-dimensional elements
            * that are incident on iDim-dimensional element
            *     getAllElements()[iDim][iElt].
            * The result of this function gets cached,
            * so it is not time-consuming to call it multiple times.
            */
            public int[/*dim+1*/][][/*dim+1*/][] getAllIncidences()
            {
                if (_allIncidences == null)
                {
                    Polytope allElts[][] = getAllElements();

                    //
                    // We don't know how many incidences each element
                    // has beforehand.  We could do this by starting
                    // with a gzillion Vectors and then converting to arrays
                    // at the end... but there are a gzillion of them.
                    // So instead, we do it in two passes--
                    // a counting pass, then allocate arrays
                    // of the right size, then a filling pass.
                    //

                    int counts[][][] = new int[allElts.length][][];
                    for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                    {
                        counts[iDim] = new int[allElts[iDim].length][];
                        for (int iElt = 0; (iElt) < (allElts[iDim].length); ++iElt)
                            counts[iDim][iElt] = new int[dim+1]; // zeros
                    }


                    // Mark each element temporarily with its index in allElts,
                    // so we don't have to do time consuming searches...
                    class GlobalIndexAux
                    {
                        public int globalIndex;
                        public Object savedAux;
                        public GlobalIndexAux(int globalIndex, Object savedAux)
                        {
                            this.globalIndex = globalIndex;
                            this.savedAux = savedAux;
                        }
                    }
                    for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                    for (int iElt = 0; (iElt) < (allElts[iDim].length); ++iElt)
                    {
                        Polytope elt = allElts[iDim][iElt];
                        elt.aux = new GlobalIndexAux(iElt, elt.aux);
                    }

                    for (int iPass = 0; (iPass) < (2); ++iPass) // iPass=0: count, iPass=1: fill
                    {
                        for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                        for (int iElt = 0; (iElt) < (allElts[iDim].length); ++iElt)
                        {
                            Polytope elt = allElts[iDim][iElt];
                            Polytope allEltsOfElt[][] = elt.getAllElements();
                            for (int jDim = 0; (jDim) < (elt.dim+1); ++jDim)
                            for (int jEltLocal = 0; (jEltLocal) < (allEltsOfElt[jDim].length); ++jEltLocal)
                            {
                                Polytope eltElt = allEltsOfElt[jDim][jEltLocal];
                                int jEltGlobal = ((GlobalIndexAux)eltElt.aux).globalIndex;
                                if (false)
                                    System.out.println("    elt "+dimToPrefix(iDim)+elt.id
                                             +"("+iDim+")"
                                             +" has eltElt "+dimToPrefix(jDim)+eltElt.id
                                             +"("+jEltGlobal+")"
                                             );
                                if (iPass == 1) // if it's the filling pass
                                    _allIncidences[jDim][jEltGlobal][iDim][counts[jDim][jEltGlobal][iDim]] = iElt;
                                counts[jDim][jEltGlobal][iDim]++;
                                if (jDim != iDim)
                                {
                                    if (iPass == 1) // if it's the filling pass
                                        _allIncidences[iDim][iElt][jDim][counts[iDim][iElt][jDim]] = jEltGlobal;
                                    counts[iDim][iElt][jDim]++;
                                }
                            }
                        }
                        if (iPass == 0)
                        {
                            // Got the counts,
                            // do the allocations.
                            _allIncidences = new int[counts.length][][][];
                            for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                            {
                                _allIncidences[iDim] = new int[counts[iDim].length][][];
                                for (int iElt = 0; (iElt) < (allElts[iDim].length); ++iElt)
                                {
                                    _allIncidences[iDim][iElt] = new int[counts[iDim][iElt].length][];
                                    for (int jDim = 0; (jDim) < (dim+1); ++jDim)
                                    {
                                        _allIncidences[iDim][iElt][jDim] = new int[counts[iDim][iElt][jDim]];
                                        counts[iDim][iElt][jDim] = 0;
                                    }
                                }
                            }
                        }
                    }

                    // restore old auxs...
                    for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                    for (int iElt = 0; (iElt) < (allElts[iDim].length); ++iElt)
                    {
                        Polytope elt = allElts[iDim][iElt];
                        elt.aux = ((GlobalIndexAux)elt.aux).savedAux;
                    }

                    if (false)
                    {
                        System.out.println("All incidences:");
                        System.out.println("_allIncidences" + " = " + Arrays.toStringCompact(_allIncidences));
                        for (int iDim = 0; (iDim) < (_allIncidences.length); ++iDim)
                        {
                            for (int iElt = 0; (iElt) < (_allIncidences[iDim].length); ++iElt)
                            {
                                Polytope elt = allElts[iDim][iElt];
                                System.out.println("    "+dimToPrefix(iDim)+elt.id+":");
                                for (int jDim = 0; (jDim) < (_allIncidences[iDim][iElt].length); ++jDim)
                                {
                                    System.out.print("    ");
                                    for (int jElt = 0; (jElt) < (_allIncidences[iDim][iElt][jDim].length); ++jElt)
                                    {
                                        Polytope inc = allElts[jDim][_allIncidences[iDim][iElt][jDim][jElt]];
                                        System.out.print(" "+dimToPrefix(jDim)+inc.id);
                                    }
                                    System.out.println("    ");
                                }
                            }
                            System.out.println();
                        }
                    }
                }
                return _allIncidences;
            } // getAllIncidences

            public String toString(String indentString,
                                   boolean showAux,
                                   boolean showGoryDetails,
                                   java.util.Hashtable printedAlready)
            {
                String nl = System.getProperty("line.separator");
                if (printedAlready == null)
                    printedAlready = new java.util.Hashtable();

                StringBuffer sb = new StringBuffer();
                if (showGoryDetails)
                {
                    sb.append("Polytope "+id+" {" + nl);
                    if (printedAlready.containsKey(this))
                    {
                        sb.append(indentString + "    (printed already)" + nl);
                    }
                    else
                    {
                        printedAlready.put(this,this);

                        if (showAux)
                            sb.append(indentString + "    (aux="+aux+")");
                        sb.append(indentString + "    "+facets.length+" facet"+(facets.length==1?"":"s")+": {" + nl);
                        for (int iFacet = 0; (iFacet) < (facets.length); ++iFacet)
                        {
                            sb.append(indentString + "        " + facets[iFacet].toString(indentString+"        ", showAux, showGoryDetails, printedAlready) + nl);
                        }
                        sb.append(indentString + "    }" + nl);

                        sb.append(indentString + "    "+contributingHyperplanes.length+" contributing hyperplane"+(contributingHyperplanes.length==1?"":"s")+": {" + nl);
                        for (int iHyperplane = 0; (iHyperplane) < (contributingHyperplanes.length); ++iHyperplane)
                        {
                            sb.append(indentString + "        " + contributingHyperplanes[iHyperplane] + nl);
                        }
                        sb.append(indentString + "    }" + nl);

                        sb.append(indentString + "    _coords = " + VecMath.toString(_coords) + nl);
                        sb.append(indentString + "    _bbox = " + VecMath.toString(_bbox) + nl);
                    }
                    sb.append(indentString + "}");
                }
                else
                {
                    sb.append(dimToPrefix(dim));
                    sb.append(id);

                    if (showAux)
                    {
                        sb.append(" (aux="+aux+")");
                    }

                    //sb.append("[");
                    if (printedAlready.containsKey(this))
                    {
                        if (true)
                            sb.append(" (see above)");
                    }
                    else
                    {
                        printedAlready.put(this,this);
                        for (int iHyperplane = 0; (iHyperplane) < (contributingHyperplanes.length); ++iHyperplane)
                        {
                            sb.append("  (" + contributingHyperplanes[iHyperplane] + ")");
                        }
                        if (dim == 0)
                        {
                            if (_coords != null)
                            {
                                sb.append("  :  " + VecMath.toString(_coords));
                                // XXX Should probably do the below anyway,
                                // XXX and also keep track
                                // XXX of whether coords have been
                                // XXX computed or set explicitly
                            }
                            else
                            {
                                // Find explicit coords of the vertex.
                                // We want column vector c
                                // such that
                                //    normal0 dot c == offset0
                                //    normal1 dot c == offset1
                                //    normal2 dot c == offset2
                                // etc.
                                // so c = inv(normalsMatrix) * offsetsColumnVector.

                                int nRows = contributingHyperplanes.length;
                                double normalsMatrix[][] = new double[nRows][nRows];
                                double offsetsColumnVector[][] = new double[nRows][1];
                                for (int iRow = 0; (iRow) < (nRows); ++iRow)
                                {
                                    VecMath.copyvec(normalsMatrix[iRow], contributingHyperplanes[iRow].normal);
                                    offsetsColumnVector[iRow][0] = contributingHyperplanes[iRow].offset;
                                }
                                // replace normalsMatrix by its inverse,
                                // replace the offsets in c by the solution
                                try {
                                    VecMath.gaussj(normalsMatrix, nRows,
                                                   offsetsColumnVector, 1);
                                    double c[] = VecMath.getcolumn(offsetsColumnVector, 0);

                                    sb.append("  ->  " + VecMath.toString(c));
                                }
                                catch (Exception e)
                                {
                                    sb.append("  -> !!!!!!!!!!!!!!!!!! singular matrix !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                }
                            }
                        }
                        for (int iFacet = 0; (iFacet) < (facets.length); ++iFacet)
                        {
                            sb.append(nl + indentString + "    ");
                            do { if (!(facets[iFacet] != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+620 +"): " + "facets[iFacet] != null" + ""); } while (false);
                            sb.append(facets[iFacet].toString(indentString+"    ", showAux, false, printedAlready));
                            if (iFacet+1 < facets.length)
                                sb.append(" ");
                        }
                    }
                    //sb.append("]");
                }
                return sb.toString();
            } // Polytope.toString
            public String toString(String indentString,
                                   boolean showAux,
                                   boolean showGoryDetails)
            {
                return toString(indentString,
                                showAux,
                                showGoryDetails,
                                null);
            }
            public String toString(String indentString,
                                   boolean showAux)
            {
                return toString(indentString,
                                showAux,
                                false);
            }
            public String toString(String indentString)
            {
                return toString(indentString,
                                false);
            }
            public String toString(boolean showAux)
            {
                return toString("",
                                showAux);
            }
            public String toString()
            {
                return toString("",
                                false);
            }

            /** Parses a polytope from a string the "pcalc" program's format. */
            // XXX should really do this from a reader, then fromString... could just hook up a StringReader and go
            public static Polytope fromStringInPcalcFormat(String s)
            {
                //System.out.println("scanning...");
                //System.out.flush();
                Polytope answer = null;
                {
                    int fullDim = -1;
                    Polytope facets[] = null;

                    String lines[] = s.trim().split("\\s*\n\\s*");
                    int iLine = 0;
                    for (int iDim = 0; ; iDim++)
                    {
                        if (iLine >= lines.length)
                            break;
                        String line = lines[iLine++];
                        // scan "%d %d-cel%*[^:]:", &nCells, &shouldBeIDim
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+) (\\d+)-cell[^:]*:").matcher(line);
                        if (!matcher.matches())
                            throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: expected number of "+iDim+"-cells, got \""+line+"\"!");
                        int nCells = Integer.parseInt(matcher.group(1));
                        int shouldBeIDim = Integer.parseInt(matcher.group(2));

                        if (shouldBeIDim != iDim)
                            throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: expected "+iDim+"-cells, got "+shouldBeIDim+"-cells!");
                        if (nCells == 0)
                            continue;
                        Polytope cells[] = new Polytope[nCells];

                        for (int iCell = 0; (iCell) < (nCells); ++iCell)
                        {
                            if (iLine >= lines.length)
                                throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: premature end-of-string expecting "+iDim+"-cell "+iCell+"!");
                            line = lines[iLine++];
                            // Read the index and colon.  This is for human-readability
                            // and sanity checking.
                            // scan "%d:", &shouldBeICell
                            matcher = java.util.regex.Pattern.compile("(\\d+):(.*)").matcher(line);
                            if (!matcher.matches())
                                throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: expected "+iDim+"-cell "+iCell+", got \""+line+"\"!");
                            int shouldBeICell = Integer.parseInt(matcher.group(1));
                            String rest = matcher.group(2);

                            if (shouldBeICell != iCell)
                                throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: expected "+iDim+"-cell "+iCell+", got "+shouldBeICell+"!");
                            String tokens[] = rest.trim().split("\\s+");
                            if (iDim == 0)
                            {
                                //
                                // Read vertex coords from tokens on this line.
                                //
                                if (fullDim == -1)
                                    fullDim = tokens.length;
                                else
                                    if (tokens.length != fullDim)
                                    {
                                        throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: inconsistent vertex dimensions "+fullDim+", "+tokens.length+"!");
                                    }
                                double coords[] = new double[fullDim];
                                for (int iCoord = 0; (iCoord) < (fullDim); ++iCoord)
                                    coords[iCoord] = Double.parseDouble(tokens[iCoord]);
                                cells[iCell] = new Polytope(iDim,
                                                            fullDim,
                                                            new SPolytope[0],
                                                            new Hyperplane[0], // contributing hyperplanes will get filled out later
                                                            null); // no aux
                                cells[iCell].setCoords(coords);
                            }
                            else
                            {
                                //
                                // Read signed facet indices from tokens on this line.
                                //
                                SPolytope mySignedFacets[] = new SPolytope[tokens.length];
                                for (int iToken = 0; (iToken) < (tokens.length); ++iToken)
                                {
                                    String token = tokens[iToken];
                                    int sign = 0;
                                    if (token.startsWith("-"))
                                        sign = -1;
                                    else if (token.startsWith("+"))
                                        sign = 1;
                                    else if (token.startsWith("!"))
                                        sign = 0;
                                    else
                                        throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: bad facet specification "+token+" in "+iDim+"-cell "+iCell+"!");
                                    token = token.substring(1);
                                    int iFacet = Integer.parseInt(token);
                                    if (iFacet < 0 || iFacet >= facets.length)
                                        throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: out-of-range facet specification "+token+" in "+iDim+"-cell "+iCell+"! (there are only "+facets.length+" possible facets)");
                                    mySignedFacets[iToken] = new SPolytope(0,sign,
                                                                           facets[iFacet]);
                                }
                                cells[iCell] = new Polytope(iDim,
                                                            fullDim,
                                                            mySignedFacets,
                                                            new Hyperplane[0], // contributing hyperplanes will get filled out later
                                                            null); // no aux
                            }
                        }

                        facets = cells;
                    }
                    if (facets == null || facets.length == 0)
                        throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: premature end-of-string or something");
                    answer = facets[0];
                }

                // Now we have to go through
                // and set the planes defining every element,
                // starting with the facets.
                if (answer.dim != answer.fullDim)
                    throw new IllegalArgumentException("Polytope.fromStringInPcalcFormat: dimension "+answer.dim+" does not match dimension of vertices "+answer.fullDim+"!");
                answer.contributingHyperplanes = new Hyperplane[0];
                for (int iFacet = 0; (iFacet) < (answer.facets.length); ++iFacet)
                {
                    SPolytope facet = answer.facets[iFacet];
                    double normal[] = new double[answer.fullDim];
                    areaNormal(normal, facet); // get area normal of facet
                    VecMath.normalize(normal,normal);
                    Polytope someVertexOnFacet = facet.p;
                    while (someVertexOnFacet.dim > 0)
                        someVertexOnFacet = someVertexOnFacet.facets[0].p;
                    double offset = VecMath.dot(normal, someVertexOnFacet.getCoords());
                    Hyperplane hyperplane = new Hyperplane(normal, offset);
                    Hyperplane hyperplanes[] = {hyperplane};

                    //
                    // Add this hyperplane as a contributing
                    // hyperplane to every cell of this facet
                    // (including this facet itself).
                    //
                    Polytope allElements[][] = facet.p.getAllElements();
                    for (int i = 0; (i) < (allElements.length); ++i)
                    for (int j = 0; (j) < (allElements[i].length); ++j)
                    {
                        Polytope e = allElements[i][j];
                        e.contributingHyperplanes = unionOfHyperplanesAndSort(e.contributingHyperplanes, hyperplanes);
                    }
                }

                //System.out.println("done.");
                //System.out.flush();
                return answer;
            } // fromStringInPcalcFormat
        } // class Polytope

        /**
        *  Signed (oriented) polytope.
        *  <br>
        *  InitialDensity is the "density at (-inf,-inf,...)",
        *  which is 0 for finite polytopes and 1 for cofinite ones
        *  (the concept simply breaks down for polytopes
        *  that are neither finite nor cofinite).
        *  <p>
        *  Representing both finite and cofinite polytopes
        *  in this way eases our job in implementing CSG:
        *  we need only implement intersection;
        *  then the other operations are expressible trivially:
        *  <pre>
        *       union(A,B) = -intersect(-A,-B)
        *       diff(A,B)  = intersect(A,-B)
        *  </pre>
        */
        public static class SPolytope
        {
            public long id;
            public int initialDensity; // 0 or 1
            public int sign; // +1 or -1
            public Polytope p;
            public SPolytope(int initialDensity,
                             int sign,
                             Polytope p)
            {
                synchronized(nIdsLock)
                {
                    this.id = nIds++;
                }
                this.initialDensity = initialDensity;
                this.sign = sign;
                this.p = p;
            }

            public String toString(String indentString,
                                   boolean showAux,
                                   boolean showGoryDetails,
                                   java.util.Hashtable printedAlready)
            {
                String nl = System.getProperty("line.separator");
                if (printedAlready == null)
                    printedAlready = new java.util.Hashtable();

                StringBuffer sb = new StringBuffer();
                if (showGoryDetails)
                {
                    sb.append("SPolytope "+id+" {" + nl);
                    if (printedAlready.containsKey(this))
                    {
                        sb.append(indentString + "    (printed already)" + nl);
                    }
                    else
                    {
                        printedAlready.put(this,this);

                        sb.append(indentString + "    initialDensity = " + initialDensity + nl);
                        sb.append(indentString + "    sign = " + sign + nl);
                        sb.append(indentString + "    p = " + p.toString(indentString+"    ", showAux, showGoryDetails, printedAlready) + nl);
                    }
                    sb.append(indentString + "}");
                }
                else
                {
                    if (initialDensity != 0)
                        sb.append(initialDensity);
                    sb.append(signToString(sign) + p.toString(indentString, showAux, showGoryDetails, printedAlready));
                }
                return sb.toString();
            } // SPolytope.toString
            public String toString(String indentString,
                                   boolean showAux,
                                   boolean showGoryDetails)
            {
                return toString(indentString,
                                showAux,
                                showGoryDetails,
                                null);
            }
            public String toString(String indentString,
                                   boolean showAux)
            {
                return toString(indentString, showAux, false);
            }
            public String toString(String indentString)
            {
                return toString(indentString, false);
            }
            public String toString(boolean showAux)
            {
                return toString("", showAux);
            }
            public String toString()
            {
                return toString("", false);
            }

            /** Parses a signed polytope from a string the "pcalc" program's format. */
            public static SPolytope fromStringInPcalcFormat(String s)
            {
                SPolytope answer = new SPolytope(0,1,Polytope.fromStringInPcalcFormat(s));
                //PRINT(volume(answer));
                double vol = CSG.volume(answer);
                if (vol < 0)
                    answer.sign *= -1; // XXX should do the push-down-sign thing?
                //PRINT(volume(answer));
                return answer;
            }

            public double volume()
            {
            	return CSG.volume( this );
            }
            
        } // class SPolytope



    //
    // Some primitive SPolytopes...
    //
        /** Makes a polygon from the given vertices in n dimensions. */
        public static SPolytope makePolygon(double verts[][])
        {
            int dim = 2;
            int fullDim = verts[0].length;
            do { if (!(fullDim == 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+933 +"): " + "fullDim == 2" + ""); } while (false); // XXX could try to do it in higher dimensions; would require getting a hyperplane basis for the othogonal complement space, I think
            int nVerts = verts.length;

            //
            // Create the hyperplanes...
            //
            Hyperplane hyperplanes[] = new Hyperplane[nVerts];
            {
                // scratch for loop... note edgeNormal can't be reused
                /// since the hyperplane stores a pointer to it.
                double edgeBackwards[] = new double[fullDim];
                for (int i = 0; (i) < (nVerts); ++i)
                {
                    VecMath.vmv(edgeBackwards, verts[i], verts[(i+1)%nVerts]);
                    double edgeNormal[] = VecMath.xv2(edgeBackwards);
                    hyperplanes[i] = new Hyperplane(edgeNormal,
                                                    VecMath.dot(edgeNormal,
                                                                verts[i]));
                }
            }
            //
            // Create a polytope for each vertex...
            //
            Polytope vertPolytopes[] = new Polytope[nVerts];
            {
                for (int i = 0; (i) < (nVerts); ++i)
                {
                    Hyperplane subHyperplanes[] = {hyperplanes[i],
                                                   hyperplanes[(i-1+nVerts)%nVerts]};
                    vertPolytopes[i] = new Polytope(0,
                                                    fullDim,
                                                    new SPolytope[0],
                                                    subHyperplanes,
                                                    null); // no aux
                }
            }
            //
            // Create a polytope for each edge...
            //
            SPolytope edgeSPolytopes[] = new SPolytope[nVerts];
            {
                for (int i = 0; (i) < (nVerts); ++i)
                {
                    Hyperplane subHyperplanes[] = {hyperplanes[i]};
                    SPolytope vertsThisEdge[] = {
                        new SPolytope(0,-1,vertPolytopes[i]),
                        new SPolytope(0,1, vertPolytopes[(i+1)%nVerts]),
                    };
                    edgeSPolytopes[i] = new SPolytope(0,1,
                                         new Polytope(1,
                                                      fullDim,
                                                      vertsThisEdge,
                                                      subHyperplanes,
                                                      null)); // no aux
                }
            }
            SPolytope faceSPolytope = new SPolytope(0,1,
                                          new Polytope(2,
                                                       fullDim,
                                                       edgeSPolytopes,
                                                       new Hyperplane[0],
                                                       null)); // no aux
            return faceSPolytope;
        } // makePolygon

        /**
        * Makes a regular (n/d)-gon of edge length 2, centered at the origin,
        * with a face pointing in the -Y axis direction.
        * (That makes it so that, when used as the right-hand-side
        * of a product, the result will appear face-first if -W means "first").
        */
        public static SPolytope makeRegularPolygon(int n, int d)
        {
            double verts[][] = new double[n][2];
            double R = 1./Math.sin(Math.PI*d/n); // circumradius
            for (int i = 0; (i) < (n); ++i)
            {
                double ang = -Math.PI*.5 + 2*Math.PI*d/n*(i+.5); // so a face points in the -Y axis dir
                verts[i][0] = R * Math.cos(ang);
                verts[i][1] = R * Math.sin(ang);
            }
            return makePolygon(verts);
        }

        /** Makes an n-dimensional simplex with edge length 2, centered at the origin. */
        public static SPolytope makeSimplex(int nDims)
        {
            // XXX stopgap special cases for now
            if (nDims == 2)
                return makeRegularPolygon(3,1);
            else if (nDims == 3)
                return SPolytope.fromStringInPcalcFormat(pcalcString33);
            else if (nDims == 4)
                return SPolytope.fromStringInPcalcFormat(pcalcString333);

            double verts[][] = new double[nDims+1][nDims];
            for (int iVert = 0; (iVert) < (nDims+1); ++iVert)
                for (int iAx = 0; (iAx) < (nDims); ++iAx)
                    if (iVert == 0)
                        verts[iVert][iAx] = Math.sqrt((double)(iAx+2)/(2*iAx+2)) / (iAx+2);
                    else if (iVert <= iAx)
                        verts[iVert][iAx] = verts[0][iAx];
                    else if (iVert == iAx+1)
                        verts[iVert][iAx] = -(iAx+1) * verts[iAx][iAx];
                    else
                        verts[iVert][iAx] = 0.;

            // We treat verts as dual verts,
            // to exercise the makeConvexPolytopeFromIntersectionOfHalfspaces function
            double dualVerts[][] = verts;
            verts = null;

            double dualCircumRadius = VecMath.norm(dualVerts[0]);
            double primalInRadius = 1./dualCircumRadius;
            // XXX edge length is wrong-- fix this! what do do with primal inRadius?

            //SPolytope result = makeConvexPolytopeFromIntersectionOfHalfspaces(nDims, dualVerts, VecMath.fillvec(nDims+1, 1.));
            //return result;
            do {if (true) throw new Error("Unimplemented at "+"com/donhatchsw/util/CSG.prejava"+"("+1051 +")"); } while (false);
            return null;
        } // makeSimplex


        /** Makes an n-dimensional hypercube with given center and in-radius. */
        public static SPolytope makeHypercube(double center[],
                                              double inRadius)
        {
            int fullDim = center.length;
            int dim = fullDim;
            if (verboseLevel >= 1)
                System.out.println("in makeHypercube, dim="+dim);

            int nVerts = 1<<dim;
            int totalNumberOfPolytopes = intpow(3, dim);

            //
            // Create the hyperplanes...
            //
            Hyperplane hyperplanes[][] = new Hyperplane[dim][2];
            {
                for (int iDim = 0; (iDim) < (dim); ++iDim)
                    for (int iDir = 0; (iDir) < (2); ++iDir)
                    {
                        double normal[] = VecMath.zerovec(dim);
                        normal[iDim] = (iDir == 0 ? -1. : 1.);
                        hyperplanes[iDim][iDir] = new Hyperplane(
                                normal,
                                VecMath.dot(normal, center) + inRadius);
                    }
            }

            //
            // Create a mondo array of Polytopes...
            //
            Polytope array[] = new Polytope[totalNumberOfPolytopes];
            {
                for (int i = 0; (i) < (totalNumberOfPolytopes); ++i)
                {
                    // dimension of polytope
                    // is number of 1's in its base 3 representation...
                    int subDim = 0;
                    for (int j = 0; (j) < (dim); ++j)
                        if (digit(i,j,3) == 1)
                            ++subDim;
                    int nSubHyperplanes = dim - subDim;
                    Hyperplane subHyperplanes[] = new Hyperplane[nSubHyperplanes];
                    int iSubHyperplane = 0;
                    for (int j = 0; (j) < (dim); ++j)
                    {
                        int digit = digit(i,j,3);
                        if (digit(i,j,3) != 1) // if it's 0 or 2
                            subHyperplanes[iSubHyperplane++] = hyperplanes[j][digit/2];
                    }
                    do { if (!(iSubHyperplane == nSubHyperplanes)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1106 +"): " + "iSubHyperplane == nSubHyperplanes" + ""); } while (false);
                    array[i] = new Polytope(subDim,
                                            fullDim,
                                            null, // placeholder for facets
                                            subHyperplanes,
                                            null); // no aux
                }
            }

            //
            // For each polytope, assign to it
            // all of its facets.
            //
            {
                for (int i = 0; (i) < (totalNumberOfPolytopes); ++i)
                {
                    Polytope p = array[i];
                    int subDim = p.dim;
                    int iFacet = 0, nFacets = 2 * subDim;
                    SPolytope facets[] = new SPolytope[nFacets];
                    for (int j = 0; (j) < (dim); ++j)
                        if (digit(i, j, 3) == 1)
                        {
                            int dist = intpow(3,j);
                            int sign;
                            {
                                //
                                // If there are no 0's or 2's
                                // in higher-order places than j,
                                // then sign is +.
                                // If there is one of them,
                                // then sign is -.
                                // If there are two of them,
                                // then sign is +.
                                // Etc.
                                // Don't ask me why this works.
                                // it took me years of pain
                                // before I discovered it.
                                //
                                sign = 1;
                                for (int k = j+1; k < dim; ++k)
                                    if (digit(i, k, 3) != 1)
                                        sign = -sign;
                            }
                            facets[iFacet++] = new SPolytope(0,-sign, array[i - dist]);
                            facets[iFacet++] = new SPolytope(0,sign, array[i + dist]);
                        }
                    do { if (!(iFacet == nFacets)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1153 +"): " + "iFacet == nFacets" + ""); } while (false);
                    p.facets = facets;
                }
            }

            //
            // Set coords.
            // (This isn't really necessary; I'm debating
            // whether it should be done.)
            // (XXX Note however that without it,
            // I can't print out the result if dim >= 4,
            // since the print routine attempts
            // to compute the coords if they have not been set,
            // which involves inverting a 4x4 matrix
            // which I haven't implemented yet :-() )
            //
            if (true)
            {
                for (int i = 0; (i) < (totalNumberOfPolytopes); ++i)
                {
                    Polytope p = array[i];
                    if (p.dim == 0)
                    {
                        double coords[] = new double[dim];
                        for (int iDim = 0; (iDim) < (dim); ++iDim)
                        {
                            int sign = digit(i, iDim, 3) - 1;
                            coords[iDim] = center[iDim] + sign * inRadius;
                        }
                        p.setCoords(coords);
                    }
                }
            }

            //
            // The master polytope is the one right in the center of the array.
            // 
            Polytope p = array[(totalNumberOfPolytopes-1)/2];
            if (true)
            {
                if (verboseLevel >= 1)
                    System.out.println("    checking orientations deep...");
                // Make sure we oriented everything consistently...
                do { if (!(isOrientedDeep(p))) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1196 +"): " + "isOrientedDeep(p)" + ""); } while (false);
            }
            SPolytope sp = new SPolytope(0,1, p);

            // XXX currently, orientDeep does cosmetic stuff.
            // XXX we want that cosmetic stuff...
            if (true)
            {
                orientDeep(sp);
            }

            if (verboseLevel >= 1)
                System.out.println("out makeHypercube");

            return sp;
        } // makeHypercube


        /** Makes an n-dimensional hypercube of in-radius 1, centered at the origin. */
        public static SPolytope makeHypercube(int dim)
        {
            return makeHypercube(VecMath.zerovec(dim), 1.);
        } // makeHypercube(unit inRadius, centered at origin)

        /**
        * Makes a regular polytope of edge length 2, centered at the origin,
        * from its schlafli symbol.
        * Currently not general, only knows about a few.
        * The orientation is pretty much arbitrary (might be face-first,
        * might be edge-first... depends on what you think of as "first", too.
        */
        public static SPolytope makeRegularPolytope(int schlafli[/*nDims-1*/])
        {
            return makeRegularStarPolytope(schlafli, null);
        } // makeRegularPolytope
        public static SPolytope makeRegularStarPolytope(int schlafli[/*nDims-1*/],
                                                        int schlafliDenoms[/*nDims-1*/])
        {
            if (schlafliDenoms == null)
                schlafliDenoms = VecMath.fillvec(schlafli.length, 1);
            do { if (!(schlafli.length == schlafliDenoms.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1236 +"): " + "schlafli.length == schlafliDenoms.length" + ""); } while (false);
            int nDims = schlafli.length+1;
            if (nDims<2 || schlafli[0]==4 && arrayIsAll(schlafli, 1, schlafli.length-1, 3))
            {
                // {4,3,...,3} -- hypercube
                if (!arrayIsAll(schlafliDenoms, 0, schlafliDenoms.length, 1))
                    return null;
                return makeHypercube(nDims);
            }
            else if (arrayIsAll(schlafli, 0, schlafli.length, 3))
            {
                // {3,3,...,3} -- simplex
                if (!arrayIsAll(schlafliDenoms, 0, schlafliDenoms.length, 1))
                    return null;
                return makeSimplex(nDims);
            }
            else if (nDims>=2 && arrayIsAll(schlafli, 0, schlafli.length-1, 3) && schlafli[schlafli.length-1] == 4)
            {
                // {3,...,3,4} -- cross
                do {if (true) throw new Error("Unimplemented at "+"com/donhatchsw/util/CSG.prejava"+"("+1255 +")"); } while (false); // XXX implement me! could do using reciprocation of hypercube
            }
            else if (nDims==2 && schlafli[0] >= 3)
            {
                // {p} -- polygon
                return makeRegularPolygon(schlafli[0], schlafliDenoms[0]);
            }
            else
            {
                // Finite number of special cases
                if (false)
                    ;
                else if (VecMath.equals(schlafli, new int[]{5,3})
                      && VecMath.equals(schlafliDenoms, new int[]{1,1}))
                {
                    // {5,3} -- dodecahedron
                    return SPolytope.fromStringInPcalcFormat(pcalcString53);
                }
                else if (VecMath.equals(schlafli, new int[]{5,3,3})
                      && VecMath.equals(schlafliDenoms, new int[]{1,1,1}))
                {
                    return SPolytope.fromStringInPcalcFormat(pcalcString533);
                }
                else
                {
                    // hack together a visible representation of it real quick
                    String s = "{";
                    for (int i = 0; (i) < (schlafli.length); ++i)
                    {
                        s += schlafli[i];
                        if (schlafliDenoms[i] != 1)
                            s += "/"+schlafliDenoms[i];
                        if (i+1 < schlafli.length)
                            s += ",";
                    }
                    s += "}";
                    throw new Error("Schlafli symbol "+s+" not implemented yet! (if it's even valid at all)");
                }
            }
            do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1294 +"): " + "false" + ""); } while (false); // XXX I think this is unreachable, but compiler is bitching
            return null;
        } // makeRegularStarPolytope

        // Tell whether array[i0]..array[i0+n-1] is all x's.
        private static boolean arrayIsAll(int array[], int i0, int n, int x)
        {
            for (int i = 0; i < n; ++i)
                if (array[i0+i] != x)
                    return false;
            return true;
        } // isAll

        /** Makes a product of regular (star) polytopes. */
        public static SPolytope makeRegularStarPolytopeCrossProduct(int schlaflis[][],
                                                                    int schlafliDenomss[][])
        {
            int nFactors = schlaflis.length;
            do { if (!(schlafliDenomss == null || schlafliDenomss.length == nFactors)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1313 +"): " + "schlafliDenomss == null || schlafliDenomss.length == nFactors" + ""); } while (false);

            SPolytope product = null;
            for (int iFactor = 0; (iFactor) < (nFactors); ++iFactor)
            {
                SPolytope factor = makeRegularStarPolytope(schlaflis[iFactor],
                                                           schlafliDenomss==null?null:schlafliDenomss[iFactor]);
                if (product == null)
                    product = factor;
                else
                    product = cross(product, factor);
            }
            // could actually initialize product to the following,
            // but that would just waste some time on the first cross product
            if (product == null)
                return new SPolytope(0,1,new Polytope(0,0,new SPolytope[0],new Hyperplane[0],null));
            return product;
        } // makeRegularStarPolytopeCrossProduct

        /**
        * Makes a product of regular (star) polytopes from a string specification,
        * for example "{4,3}x{}x{5,3,3}x{3}x {3, 3,3,3,3 }x{} x {5/2,5} "
        * '*' can be used in place of 'x' as well.
        */
        public static SPolytope makeRegularStarPolytopeCrossProductFromString(String s)
        {
            String factors[] = s.trim().split("[x*]");
            int schlaflis[][] = new int[factors.length][];
            int schlafliDenomss[][] = new int[factors.length][];
            for (int iFactor = 0; (iFactor) < (factors.length); ++iFactor)
            {
                String factor = factors[iFactor].trim();
                if (!factor.startsWith("{")
                 || !factor.endsWith("}"))
                    throw new IllegalArgumentException("makeRegularStartPolytopeCrossProductFromString: bad schlafli symbol \""+s+"\"");
                factor = factor.substring(1, factor.length()-1); // trim off { and }
                factor = factor.trim(); // and any white space inside
                String fractions[] = factor.split(",");
                if (factor.equals(""))
                    fractions = new String[0]; // to be safe (I don't trust split)
                schlaflis[iFactor] = new int[fractions.length];
                schlafliDenomss[iFactor] = new int[fractions.length];
                for (int iFraction = 0; (iFraction) < (fractions.length); ++iFraction)
                {
                    String fraction = fractions[iFraction].trim();
                    String n_and_d[] = fraction.split("/");
                    if (n_and_d.length == 2)
                    {
                        schlaflis[iFactor][iFraction] = Integer.parseInt(n_and_d[0]);
                        schlafliDenomss[iFactor][iFraction] = Integer.parseInt(n_and_d[1]);
                    }
                    else if (n_and_d.length == 1)
                    {
                        schlaflis[iFactor][iFraction] = Integer.parseInt(n_and_d[0]);
                        schlafliDenomss[iFactor][iFraction] = 1;
                    }
                    else
                        throw new IllegalArgumentException("makeRegularStartPolytopeCrossProductFromString: bad schlafli symbol \""+s+"\"");
                }
            }
            //PRINTARRAY(s);
            //PRINTARRAY(schlaflis);
            //PRINTARRAY(schlafliDenomss);
            return makeRegularStarPolytopeCrossProduct(schlaflis, schlafliDenomss);
        } // makeRegularStarPolytopeCrossProductFromString


    //
    // Utilities...
    //
        //
        // A polytope is "binary"
        // if all densities are 0 or 1.
        // XXX also checks whether it's finite-or-cofinite,
        // XXX so isBinary is a misnomer.
        //
        public static boolean isBinaryDensityShallow(Polytope p)
        {
            if (p.dim == 1)
            {
                int nPluses = 0, nMinuses = 0; // and counting
                SPolytope facets[] = p.facets;
                int nFacets = facets.length;
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    SPolytope facet = facets[iFacet];

                    if (facet.initialDensity != 0)
                        return false; // only top-level can have nonzero initial density in a finite-or-cofinite polytope

                    int sign = facet.sign;
                    if (sign == 1)
                        nPluses++;
                    else if (sign == -1)
                        nMinuses++;
                    else
                        return false;
                }
                if (nPluses != nMinuses)
                    return false;
            }
            // XXX else!?
            return true;
        } // isBinaryDensityShallow

        public static boolean isBinaryDensityDeep(SPolytope sp)
        {
            if (sp.initialDensity != 0
             && sp.initialDensity != 1)
                return false;
            Polytope allElements[][] = sp.p.getAllElements();
            int dim = sp.p.dim;
            for (int iDim = 0; (iDim) < (dim+1); ++iDim)
            {
                Polytope ofDim[] = allElements[iDim];
                int nOfDim = ofDim.length;
                for (int iOfDim = 0; (iOfDim) < (nOfDim); ++iOfDim)
                    if (!isBinaryDensityShallow(ofDim[iOfDim]))
                        return false;
            }
            return true;
        } // isBinaryDensityDeep



        //
        // Do one level of orientedness checking.
        // XXX This is O(n^2); should use a hash table
        // XXX instead of an array for boundaryRidges
        //
        public static boolean isOrientedShallow(Polytope p)
        {
            final boolean verbose = false;
            //
            // A polytope is considered oriented
            // if the summation of the signed facets of its
            // signed facets is zero.
            //
            SPolytope facets[] = p.facets;
            int nFacets = facets.length;
            int maxRidges;
            {
                maxRidges = 0; // and counting
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                    maxRidges += facets[iFacet].p.facets.length;
            }
            SPolytope boundaryRidges[] = new SPolytope[maxRidges];
            int nBoundaryRidges = 0; // and counting
            {
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    SPolytope facet = facets[iFacet];
                    SPolytope ridgesThisFacet[] = facet.p.facets;
                    int nRidgesThisFacet = ridgesThisFacet.length;
                    for (int iRidgeThisFacet = 0; (iRidgeThisFacet) < (nRidgesThisFacet); ++iRidgeThisFacet)
                    {
                        SPolytope ridge = ridgesThisFacet[iRidgeThisFacet];
                        int sign = facet.sign * ridge.sign;
                        // Add boundaryRidge to boundaryRidges.
                        {
                            if (verbose)
                                System.out.println("Adding "
                                                   +signToString(facet.sign)
                                                   +signToString(ridge.sign)
                                                   +"="
                                                   +signToString(sign)
                                                   +ridge.p.id);
                            int iBoundaryRidge;
                            for (iBoundaryRidge = 0; (iBoundaryRidge) < (nBoundaryRidges); ++iBoundaryRidge)
                            {
                                if (boundaryRidges[iBoundaryRidge].p
                                 == ridge.p)
                                {
                                    // Found it!  Add the new coefficient.
                                    if ((boundaryRidges[iBoundaryRidge].sign
                                       += sign) == 0)
                                    {
                                        // New coefficient for this ridge is 0;
                                        // remove the ridge from the list.

                                        boundaryRidges[iBoundaryRidge--] = boundaryRidges[--nBoundaryRidges]; // subtle-- decrement iBoundaryRidges too, so that the test after this loop will still be valid
                                        if (verbose)
                                            System.out.println("    (poof!)");
                                    }
                                    else
                                    {
                                        //
                                        // It's an unusual polytope
                                        // in which the cumulative sign
                                        // can ever be other than
                                        // 0, -1, or +1; as of this writing
                                        // I don't think I've provided a way
                                        // to make one.  But it's
                                        // theoretically possible.
                                        //
                                        if (verbose)
                                            System.out.println("    (SURPRISE, cumulative sign is "+signToString(boundaryRidges[iBoundaryRidge].sign)+")");
                                    }
                                    break;
                                }
                            }
                            if (iBoundaryRidge == nBoundaryRidges)
                            {
                                // Didn't find it; add it
                                boundaryRidges[nBoundaryRidges++] = new SPolytope(0, sign, ridge.p);
                                if (verbose)
                                    System.out.println("    (truly)");
                            }
                        }
                    }
                }
            }
            return nBoundaryRidges == 0;
        } // isOrientedShallow


        public static boolean isOrientedDeep(Polytope p)
        {
            Polytope allElements[][] = p.getAllElements();
            int dim = p.dim;
            for (int i = 0; (i) < (dim+1); ++i)
            {
                Polytope elementsOfDim[] = allElements[i];
                int nElementsOfDim = elementsOfDim.length;
                for (int j = 0; (j) < (nElementsOfDim); ++j)
                    if (!isOrientedShallow(elementsOfDim[j]))
                    {
                        if (verboseLevel >= 2)
                        {
                            System.out.println("isOrientedDeep returning false because element "+j+"/"+nElementsOfDim+" of dimension "+i+" is not oriented shallow:");
                            System.out.println("    " + "allElements" + "["+(i)+"]["+(j)+"] = " + (allElements)[i][j]);
                        }
                        return false;
                    }
            }
            return true;
        } // isOrientedDeep


        /**
        *  Count up number of vertices, edges, ...
        *  and return the counts in an array.
        */
        public static int[] counts(Polytope p)
        {
            return (int[])Arrays.arrayLengths(p.getAllElements(), 1, 1);
        } // counts

        private static boolean contains(Polytope p, Polytope q)
        {
            Polytope eltsOfDim[] = p.getAllElements()[q.dim];
            // XXX shouldn't have to keep making comparator...
            int index = SortStuff.bsearch(eltsOfDim, q,
                               new SortStuff.Comparator() {
                                   public int compare(Object _a, Object _b)
                                   {
                                       Polytope a = (Polytope)_a;
                                       Polytope b = (Polytope)_b;
                                       return a.id < b.id ? -1 :
                                              a.id > b.id ? 1 : 0;
                                   }
                               });
            return index != -1;
        } // contains


        /**
        * calculates the order of the group of symmetries
        * that are rotations in the 3-space containing cell3d,
        * about the axis through cell3d's center and subCell's center.
        * subCell's center must be different from cell3d's center
        * (which rules out some non-convex uniform polyhedra
        * that have faces or edges crossing the origin).
        *
        * returnUsefulMat is filled in with an orthogonal matrix
        * the last two rows of which are in the plane of the rotation.
        * So a rotation of any angle in that plane can be computed as:
        *        Take the rows of mat to the canonical basis vectors e[0]..e[n-1]
        *        rotate e[n-2] towards e[n-1]
        *        take the canonical basis vectors back to the original rows
        * I.e. rotMat = VecMath.mxmxm(VecMath.transpose(mat),
        *                             VecMath.makeRowRotMat(nDims,nDims-2,nDims-1, angle,
        *                             mat);
        * XXX isn't there a version of makeRowRotMat that does something like this?  check it out.  it looks overly complicated though, and assumes nDims==3 which is lame, should have a look at it
        *
        * Returns 0 when subCell is cell3d itself (and zeros out returnUselMat).
        */
        public static int calcRotationGroupOrder(Polytope p,
                                                 Polytope cell3d,
                                                 Polytope subCell,
                                                 double returnUsefulMat[][])
        {
            int nDims = p.fullDim;
            do { if (!(cell3d.dim == 3)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1605 +"): " + "cell3d.dim == 3" + ""); } while (false);

            Polytope cell3dAllElements[][] = cell3d.getAllElements();

            int maxPossibleOrder;
            {
                if (subCell.dim == 0)
                {
                    // number of edges in cell3d that are incident on this vertex
                    maxPossibleOrder = 0; // and counting
                    Polytope edges[] = cell3dAllElements[1];
                    for (int iEdge = 0; (iEdge) < (edges.length); ++iEdge)
                    {
                        Polytope edge = edges[iEdge];
                        for (int iVertOnEdge = 0; (iVertOnEdge) < (edge.facets.length); ++iVertOnEdge)
                            if (edge.facets[iVertOnEdge].p == subCell)
                                maxPossibleOrder++;
                    }
                }
                else if (subCell.dim == 1)
                    maxPossibleOrder = 2; // assumes two faces meet here, and two vertices
                else if (subCell.dim == 2)
                    maxPossibleOrder = subCell.facets.length; // gonality of the face
                else
                {
                    VecMath.zeromat(returnUsefulMat); // nothing better to do
                    return 0; // no rotation possible here at all
                }
            }

            //
            // Compute a rotation matrix
            // that rotates by 2pi/maxPossibleOrder
            // around the line through cell3d and subCell.
            //
            double maxPossibleOrderMat[][];
            {
                double pCenter[] = new double[nDims]; cgOfVerts(pCenter, p);
                double cell3dCenter[] = new double[nDims]; cgOfVerts(cell3dCenter, cell3d);
                double subCellCenter[] = new double[nDims]; cgOfVerts(subCellCenter, subCell);
                // Make the centers relative (so they are now vectors)
                VecMath.vmv(subCellCenter, subCellCenter, cell3dCenter);
                VecMath.vmv(cell3dCenter, cell3dCenter, pCenter);

                //
                // We want two orthogonal unit vectors
                // in the 3-space of cell3d
                // that are orthogonal to the line between
                // cell3d's center and CGsubCell's center.
                // We can get this by taking two random vectors
                // and gram-schmidting them against the normals
                // of all the hyperplanes defining cell3d's space,
                // and then against that line,
                // and then against each other.
                // XXX should try to do this without randomization!
                double mat[][] = new double[nDims][nDims];
                int iRow = 0;
                for (iRow = 0; (iRow) < (cell3d.contributingHyperplanes.length); ++iRow)
                    VecMath.copyvec(mat[iRow], cell3d.contributingHyperplanes[iRow].normal);
                VecMath.copyvec(mat[iRow++], subCellCenter);
                java.util.Random generator = new java.util.Random(3); // same every time
                VecMath.random(mat[iRow++], generator);
                VecMath.random(mat[iRow++], generator);
                do { if (!(iRow == mat.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1668 +"): " + "iRow == mat.length" + ""); } while (false);
                if (VecMath.det(mat) < 0.)
                    VecMath.vxs(mat[mat.length-1], mat[mat.length-1], -1.);

                // Gram-Schmidt (XXX should be a function)
                {
                    for (int i = 0; (i) < (mat.length); ++i)
                    {
                        for (int j = 0; (j) < (i); ++j)
                        {
                            // mat[j] is already unit length...
                            // mat[i] -= (mat[i] dot mat[j])*mat[j]
                            VecMath.vpsxv(mat[i],
                                          mat[i],
                                          -VecMath.dot(mat[i],mat[j]),
                                          mat[j]);
                        }
                        VecMath.normalize(mat[i], mat[i]);
                    }
                }
                do { if (!(VecMath.det(mat) > .9)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1688 +"): " + "VecMath.det(mat) > .9" + ""); } while (false); // should be 1

                //
                // Okay, mat is now orthogonal
                // and the last two rows are the vectors we want to turn.
                // So the rotation we want to describe is:
                //    Take the rows of mat to the canonical basis vectors e[0]..e[n-1]
                //    rotate e[n-2] towards e[n-1]
                //    take the canonical basis vectors back to the original rows
                maxPossibleOrderMat = VecMath.mxmxm(VecMath.transpose(mat),
                                                    VecMath.makeRowRotMat(nDims,nDims-2,nDims-1, 2*Math.PI/maxPossibleOrder),
                                                    mat);
                //
                // So caller can do the same thing...
                //
                VecMath.copymat(returnUsefulMat, mat);
            }

            FuzzyPointHashTable hashTable = new FuzzyPointHashTable(1e-9,
                                                                    1e-8,
                                                                    1./512);
            Polytope verts[] = p.getAllElements()[0];
            int nVerts = verts.length;
            {
                Object something = new Object();
                for (int iVert = 0; (iVert) < (nVerts); ++iVert)
                    hashTable.put(verts[iVert].getCoords(), something);
            }

            // The actual order will be some factor of maxPossibleOrder.
            // Try all possibilities, from big to little.
            //PRINT(maxPossibleOrder);
            for (int order = maxPossibleOrder; order >= 1; order--)
            {
                //System.err.println("(Trying order "+order+"/"+maxPossibleOrder+")");
                if (maxPossibleOrder % order != 0)
                    continue;
                double mat[][];
                {
                    // XXX should have VecMath.pow(double[][], int) that does it the smart way so it's only O(sqrt(n)) matrix multiplies
                    mat = VecMath.identitymat(nDims);
                    for (int i = 0; (i) < (maxPossibleOrder/order); ++i)
                        mat = VecMath.mxm(mat, maxPossibleOrderMat);
                }

                double scratchVert[] = new double[nDims];
                int iVert = 0;
                for (iVert = 0; (iVert) < (nVerts); ++iVert)
                {
                    VecMath.vxm(scratchVert, verts[iVert].getCoords(), mat);
                    if (hashTable.get(scratchVert) == null)
                    {
                        //System.err.print("(HEY! order "+order+"/"+maxPossibleOrder+" didn't work!)");
                        break; // no good!
                    }
                }
                if (iVert == nVerts) // everything was good!
                {
                    return order; // everything was good!
                }
            }
            // order 1 should have succeeded, so we can't get here.
            // Actually we could have just returned 1 if all larger orders failed,
            // but we tested it anyway as a sanity check-- it better have succeeded.
            do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1752 +"): " + "false" + ""); } while (false);
            return 1;
        } // calcRotationPeriod



        // XXX comment me!!
        public static void calcDensity(SPolytope sp,
                                       double point[],
                                       double eps,
                                       int result[/*2*/]) // interval arithmetic
        {
            //System.out.println("    in calcDensity (dim="+sp.p.dim+")");
            Polytope p = sp.p;
            int dim = p.dim;

            if (dim == 0)
            {
                // XXX logically should probably be something else, but this will do for now...
                result[0] = 1;
                result[1] = 1;
                //System.out.println("    out calcDensity (dim="+sp.p.dim+"), returning "+result[0]+","+result[1]+"");
                return;
            }

            // dir = a random vector in the space spanned by this polytope.
            // We get it by taking all the plane normals,
            // augmenting by random vectors if necessary to get
            // dim-1 vectors, and taking the cross product.
            double dir[];
            {
                int nContributingHyperplanes = p.contributingHyperplanes.length;
                int fullDim = sp.p.fullDim;
                do { if (!(nContributingHyperplanes < fullDim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1785 +"): " + "nContributingHyperplanes < fullDim" + ""); } while (false);
                double M[][] = new double[fullDim-1][fullDim];
                int iRow;
                for (iRow = 0; (iRow) < (nContributingHyperplanes); ++iRow)
                    VecMath.copyvec(M[iRow], p.contributingHyperplanes[iRow].normal);
                for (; iRow < fullDim-1; ++iRow)
                    VecMath.random(M[iRow], randomGenerator);
                dir = VecMath.crossprod(M);
                VecMath.normalize(dir, dir); // XXX not necessary if we want to be clever
            }
            double pointDotDir = VecMath.dot(point, dir);

            int densityMin = sp.initialDensity; // and counting
            int densityMax = sp.initialDensity; // and counting

            if (p.dim == 1)
            {
                SPolytope vertices[] = p.facets;
                int nVertices = vertices.length;
                for (int iVertex = 0; (iVertex) < (nVertices); ++iVertex)
                {
                    SPolytope v = vertices[iVertex];
                    int a = 0, b = 0;
                    double vDotDir = VecMath.dot(v.p.getCoords(),
                                                 dir);

                    if ((((pointDotDir)-(vDotDir)) <= eps))
                        a = v.sign;
                    if ((((vDotDir)-(pointDotDir)) > eps))
                        b = v.sign;
                    if (a > b)
                    {
                        int temp;
                        {temp=(a);a=(b);b=(temp);};
                    }
                    densityMin += a;
                    densityMax += b;
                }
            }
            else
            {
                do { if (!(p.dim >= 2)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1826 +"): " + "p.dim >= 2" + ""); } while (false); // XXX never ask for density of a point
                SPolytope facets[] = p.facets;
                int nFacets = facets.length;
                double qoint[] = new double[point.length]; // scratch for loop
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    SPolytope facet = facets[iFacet];

                    // We want qoint = intersection of point+t*dir with plane
                    //                   of the facet.
                    // So we want t such that
                    //          (point+t*dir) dot hyperplane.normal
                    //                          = hyperplane.offset
                    //          point dot hyperplane.normal + t*dir dot hyperplane.normal = hyperplane.offset
                    //  t = (hyperplane.offset - (point dot hyperplane.normal)) / (dir dot hyperplane.normal)
                    {
                        Hyperplane hyperplane = (Hyperplane)itemOfAThatsNotInB(facet.p.contributingHyperplanes, p.contributingHyperplanes);
                        double t = (hyperplane.offset - VecMath.dot(point, hyperplane.normal)) / VecMath.dot(dir, hyperplane.normal);
                        VecMath.vpsxv(qoint, point, t, dir);
                    }

                    double qointDotDir = VecMath.dot(qoint, dir);

                    int a = 0, b = 0;
                    if ((((pointDotDir)-(qointDotDir)) <= eps))
                        a = facet.sign;
                    if ((((qointDotDir)-(pointDotDir)) > eps))
                        b = facet.sign;
                    if (a > b)
                    {
                        int temp;
                        {temp=(a);a=(b);b=(temp);};
                    }

                    if (verboseLevel >= 2 && a != b)
                    {
                        System.out.println("uh oh,,,,,,,,,,,,,,,,,,,,,,,,,,");
                        System.out.println("p" + " = " + (p));
                        System.out.println("point" + " = " + VecMath.toString(point));
                        System.out.println("qoint" + " = " + VecMath.toString(qoint));
                        System.out.println("a" + " = " + (a));
                        System.out.println("b" + " = " + (b));
                    }
                    if (a != 0 || b != 0)
                    {
                        calcDensity(facet, qoint, eps, result); // using result as temporary
                        densityMin += a * result[0];
                        densityMax += b * result[1];
                    }
                }
            }

            if (verboseLevel >= 2 && densityMin != densityMax)
            {
                System.out.println("Uh oh................");
                System.out.println("p" + " = " + (p));
                System.out.println("point" + " = " + VecMath.toString(point));
                System.out.println("densityMin" + " = " + (densityMin));
                System.out.println("densityMax" + " = " + (densityMax));
            }

            result[0] = densityMin;
            result[1] = densityMax;

            //System.out.println("    out calcDensity (dim="+sp.p.dim+"), returning "+result[0]+","+result[1]+"");
        } // calcDensity


         // simplicallySubdivide
        private static double[/*nSimplices*/][/*k+1*/][/*n*/] simpliciallySubdivide(Polytope p)
        {
            int k = p.dim;
            int n = p.fullDim;

            if (k == 0)
            {
                return new double[][][] {
                    {p.getCoords()},
                };
            }
            if (p.facets.length == 0)
            {
                return new double[0][k+1][n];
            }


            Polytope v0; // first vertex
            {
                v0 = p;
                while (v0.dim > 0)
                    v0 = v0.facets[0].p;
            }
            double v0coords[] = v0.getCoords();

            // XXX it looks to me like Vector behavior is O(n^2) !?? stupid!
            java.util.Vector simplicesList = new java.util.Vector();
            {
                SPolytope facets[] = p.facets;
                int nFacets = facets.length;
                double simplex[][] = new double[k+1][/*n*/];

                if (k == 1) // XXX this case is not really necessary, but it is optimized a bit
                {
                    for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                    {
                        if (iFacet == 0)
                            continue;
                        SPolytope facet = facets[iFacet];
                        simplex[0] = v0coords;
                        simplex[1] = facet.p.getCoords();
                        if (facet.sign < 0)
                        {
                            // swap last two vertices on the simplex.
                            double temp[];
                            {temp=(simplex[k-1]);simplex[k-1]=(simplex[k]);simplex[k]=(temp);};
                        }
                        for (int i = (((facet.sign) < 0 ? -(facet.sign) : (facet.sign)))-1; (i) >= 0; --i)
                            simplicesList.addElement(VecMath.copymat(simplex));
                    }
                }
                else // k > 1
                {

                    for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                    {
                        if (iFacet == 0)
                            continue; // facet 0 definitely contains v0
                        SPolytope facet = facets[iFacet];
                        if (contains(facet.p, v0))
                            continue;
                        double facetSimplices[][][] = simpliciallySubdivide(facet.p);
                        int nFacetSimplices = facetSimplices.length;
                        for (int iFacetSimplex = 0; (iFacetSimplex) < (nFacetSimplices); ++iFacetSimplex)
                        {
                            simplex[0] = v0coords;
                            System.arraycopy(facetSimplices[iFacetSimplex], 0,
                                             simplex, 1,
                                             k);
                            if (facet.sign < 0)
                            {
                                // swap last two vertices on the simplex.
                                double temp[];
                                {temp=(simplex[k-1]);simplex[k-1]=(simplex[k]);simplex[k]=(temp);};
                            }
                            for (int i = (((facet.sign) < 0 ? -(facet.sign) : (facet.sign)))-1; (i) >= 0; --i)
                                simplicesList.addElement(VecMath.copymat(simplex));
                        }
                    }
                }
            }
            double array[][][] = new double[simplicesList.size()][k+1][n];
            simplicesList.copyInto(array);
            return array;
        } // simplicallySubdivide

        // This will be the actual (signed) volume
        // if all the hyperplanes are orthogonal and unit length
        // (which includes the case when there are no hyperplanes,
        // i.e. when sp.p.dim is the full dimension of the space).
        // Otherwise it will be scaled by the volume
        // of the parallelepiped spanned by those normals.
        private static double volume(SPolytope sp)
        {
            int nNormals = sp.p.contributingHyperplanes.length;
            double simplices[][][] = simpliciallySubdivide(sp.p);
            int nSimplices = simplices.length;
            if (nSimplices == 0)
                return 0.;
            int k = simplices[0].length-1; // dimension of each simplex
            int n = simplices[0][0].length; // full dimension of space

            do { if (!(k + nNormals == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+1997 +"): " + "k + nNormals == n" + ""); } while (false);
            double sum = 0.;
            double M[][] = new double[n][n];
            for (int iSimplex = 0; (iSimplex) < (nSimplices); ++iSimplex)
            {
                double simplex[][] = simplices[iSimplex];
                for (int iRow = 0; (iRow) < (k); ++iRow)
                    VecMath.vmv(M[iRow], simplex[iRow+1], simplex[0]);
                for (int iNormal = 0; (iNormal) < (nNormals); ++iNormal)
                    VecMath.copyvec(M[k+iNormal],
                                    sp.p.contributingHyperplanes[iNormal].normal);
                sum += VecMath.detDestructive(M);
            }
            return sum * sp.sign / factorial(k);
        } // volume

        // Get the normal of the n-1-dimensional polytope sp,
        // with length equal to the hyper-area of sp.
        private static void areaNormal(double result[], SPolytope sp)
        {
            VecMath.zerovec(result);
            double simplices[][][] = simpliciallySubdivide(sp.p);
            int nSimplices = simplices.length;
            if (nSimplices == 0)
                return;
            int k = simplices[0].length-1; // dimension of each simplex
            int n = simplices[0][0].length; // full dimension of space
            do { if (!(k == sp.p.dim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2024 +"): " + "k == sp.p.dim" + ""); } while (false);

            do { if (!(k == n-1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2026 +"): " + "k == n-1" + ""); } while (false);
            do { if (!(result.length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2027 +"): " + "result.length == n" + ""); } while (false); // make sure they passed in the right dimension result
            double M[][] = new double[k][n];
            double simplexNormal[] = new double[n];
            for (int iSimplex = 0; (iSimplex) < (nSimplices); ++iSimplex)
            {
                double simplex[][] = simplices[iSimplex];
                for (int iRow = 0; (iRow) < (k); ++iRow)
                    VecMath.vmv(M[iRow], simplex[iRow+1], simplex[0]);
                VecMath.crossprod(simplexNormal, M);
                VecMath.vpv(result, result, simplexNormal);
            }
            VecMath.vxs(result, result, (double)sp.sign/(double)factorial(k));
        } // areaNormal

        public static void cgOfVerts(double result[], Polytope p)
        {
            Polytope vertPolytopes[] = p.getAllElements()[0];
            VecMath.zerovec(result);
            for (int i = 0; (i) < (vertPolytopes.length); ++i)
                VecMath.vpv(result, result, vertPolytopes[i].getCoords());
            do { if (!(vertPolytopes.length != 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2047 +"): " + "vertPolytopes.length != 0" + ""); } while (false);
            VecMath.vxs(result, result, 1./vertPolytopes.length);
        } // cgOfVerts

        private static SPolytope cross(SPolytope A,
                                       SPolytope B)
        {
            SPolytope AB = new SPolytope(A.initialDensity*B.initialDensity,
                                         A.sign*B.sign,
                                         _cross(A.p,
                                                B.p,
                                                new java.util.Hashtable(),
                                                ""));

            // If I was smart, _cross would simply get the signs right
            // as it goes.  Something like,
            // negate everything whose dimensional contribution
            // from C is odd...
            // But I have never been able to figure it out.
            // So, just orient after the fact.  Sigh.
            //
            if (true)
                orientDeep(AB);

            return AB;
        } // cross

        // recursive work function used by cross().
        // doesn't even try to get the signs right.
        private static Polytope _cross(Polytope A,
                                       Polytope B,
                                       java.util.Hashtable ocean, // hashtable of crossings already done
                                       String indentString) // for debugging
        {
            String subIndentString = null;
            if (verboseLevel >= 1)
            {
                System.out.println(indentString+"in CSG._cross");
                subIndentString = indentString + "        ";
            }
            HashablePair key = new HashablePair(A, B);
            Polytope AB = (Polytope)ocean.get(key);
            if (AB == null)
            {
                SPolytope facets[] = new SPolytope[A.facets.length + B.facets.length];
                {
                    int iFacet = 0;
                    for (int iFacetA = 0; (iFacetA) < (A.facets.length); ++iFacetA)
                    {
                        Polytope a = A.facets[iFacetA].p;
                        Polytope aB = _cross(a, B, ocean, subIndentString);
                        facets[iFacet++] = new SPolytope(0,1,aB); // sign arbitrary, will be fixed later
                    }
                    for (int iFacetB = 0; (iFacetB) < (B.facets.length); ++iFacetB)
                    {
                        Polytope b = B.facets[iFacetB].p;
                        Polytope Ab = _cross(A, b, ocean, subIndentString);
                        facets[iFacet++] = new SPolytope(0,1,Ab); // sign arbitrary, will be fixed later
                    }
                    do { if (!(iFacet == facets.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2106 +"): " + "iFacet == facets.length" + ""); } while (false);
                }

                Hyperplane contributingHyperplanes[] = new Hyperplane[A.contributingHyperplanes.length + B.contributingHyperplanes.length];
                {

                    double leftZero[] = new double[A.fullDim];
                    double rightZero[] = new double[B.fullDim];

                    int iPlane = 0;

                    for (int iPlaneA = 0; (iPlaneA) < (A.contributingHyperplanes.length); ++iPlaneA)
                    {
                        Hyperplane a = A.contributingHyperplanes[iPlaneA];
                        Hyperplane a_zero = new Hyperplane((double[])Arrays.concat(a.normal,
                                                                                   rightZero),
                                                           a.offset);
                        // XXX need to look up a_zero in the ocean to see if it already exists! although it's probably not a functional problem if hyperplane lists get duplicated... maybe
                        contributingHyperplanes[iPlane++] = a_zero;
                    }
                    for (int iPlaneB = 0; (iPlaneB) < (B.contributingHyperplanes.length); ++iPlaneB)
                    {
                        Hyperplane b = B.contributingHyperplanes[iPlaneB];
                        Hyperplane zero_b = new Hyperplane((double[])Arrays.concat(leftZero,
                                                                                   b.normal),
                                                           b.offset);
                        // XXX need to look up zero_b in the ocean to see if it already exists! although it's probably not a functional problem if hyperplane lists get duplicated... maybe
                        contributingHyperplanes[iPlane++] = zero_b;
                    }
                    do { if (!(iPlane == contributingHyperplanes.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2135 +"): " + "iPlane == contributingHyperplanes.length" + ""); } while (false);
                }

                AB = new Polytope(A.dim+B.dim,
                                  A.fullDim+B.fullDim,
                                  facets,
                                  contributingHyperplanes,
                                  null); // loses aux
                ocean.put(key, AB);
            }

            if (verboseLevel >= 1)
                System.out.println(indentString+"out CSG._cross");
            return AB;
        } // _cross


        /**
         * Slices up a signed polytope into 3 parts: above, below, and on
         * the hyperplane.
         *    if any part of slicee is strictly above the hyperplane,
         *        returnAbove[0] will be set to that part, of dimension slicee.dim;
         *        otherwise it will be set to null.
         *    if any part of slicee is strictly below the hyperplane,
         *        returnBelow[0] will be set to that part, of dimension slicee.dim;
         *        otherwise it will be set to null.
         *    if slicee lies partly strictly above and partly strictly below
         *        the hyperplane, then returnOn[0] will be set to
         *        the cross-section polytope, of dimension slicee.dim-1;
         *        otherwise it will be set to null.
         * You can pass null in as any or all of returnAbove, returnOn,
         * or returnBelow, if you are not interested in them.
         */
        private static void slice(SPolytope slicee,
                                  Hyperplane hyperplane,
                                  Object aux, // to be put in the aux field of all new elements
                                  SPolytope returnAbove[/*1*/],
                                  SPolytope returnBelow[/*1*/],
                                  SPolytope returnOn[/*1*/])
        {
            SPolytope aboveBelowOn[/*3*/] = _slice(slicee,
                                                   hyperplane,
                                                   aux,
                                                   new java.util.Hashtable(),
                                                   "");
            do { if (!(aboveBelowOn != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2180 +"): " + "aboveBelowOn != null" + ""); } while (false);
            if (returnAbove != null)
                returnAbove[0] = aboveBelowOn[0];
            if (returnBelow != null)
                returnBelow[0] = aboveBelowOn[1];
            if (returnOn != null)
                returnOn[0] = aboveBelowOn[2];
        } // slice

        /**
         * Slices up the boundary of a signed polytope,
         * returning a new signed polytope.
         * XXX this is actually not a great way to do this,
         * XXX since often we make a bunch of parallel cuts
         * XXX and we know the stuff on a particular side of the first cut
         * XXX isn't going to be affected by the next cut
         */
        public static SPolytope sliceFacets(SPolytope slicee,
                                            Hyperplane hyperplane,
                                            Object aux) // to be put in the aux field of all new elements
        {
            if (verboseLevel >= 1)
            {
                System.out.println("in CSG.slice");
                System.out.println("hyperplane" + " = " + (hyperplane));
            }
            java.util.Hashtable ocean = new java.util.Hashtable();

            SPolytope newFacets[] = new SPolytope[2*slicee.p.facets.length]; // at most
            int nNewFacets = 0;
            for (int iFacet = 0; (iFacet) < (slicee.p.facets.length); ++iFacet)
            {
                SPolytope facetStuff[] = _slice(slicee.p.facets[iFacet], hyperplane, aux, ocean, "        ");
                if (facetStuff[0] != null)
                    newFacets[nNewFacets++] = facetStuff[0];
                if (facetStuff[1] != null)
                    newFacets[nNewFacets++] = facetStuff[1];
            }
            if (nNewFacets == slicee.p.facets.length)
                return slicee;
            newFacets = (SPolytope[])Arrays.subarray(newFacets, 0, nNewFacets); // resize
            SPolytope result = new SPolytope(slicee.initialDensity,
                                             slicee.sign,
                                             new Polytope(slicee.p.dim,
                                                          slicee.p.fullDim,
                                                          newFacets,
                                                          slicee.p.contributingHyperplanes,
                                                          null)); // aux gets lost
            if (verboseLevel >= 1)
                System.out.println("out CSG.slice");
            return result;
        } // sliceFacets

        // recursive work function used by slice() and sliceFacets().
        // returns an array {above,below,on}.
        private static SPolytope[/*3*/] _slice(SPolytope slicee,
                                               Hyperplane hyperplane,
                                               Object aux,
                                               java.util.Hashtable ocean, // hashtable of slicings already done
                                               String indentString) // for debugging
        {
            String subIndentString = null;
            if (verboseLevel >= 1)
            {
                System.out.println(indentString+"in CSG._slice (slicee dim = "+slicee.p.dim+")");
                subIndentString = indentString + "        ";
            }
            SPolytope aboveBelowOn[/*3*/] = (SPolytope[])ocean.get(slicee.p);
            if (aboveBelowOn == null)
            {
                // Not already in the ocean... need to calculate it
                SPolytope above = null, below = null, on = null;
                if (slicee.p.dim == 0)
                {
                    double coords[] = slicee.p.getCoords();
                    double height = VecMath.dot(coords, hyperplane.normal) - hyperplane.offset;
                    if (height > 0.) // XXX need to make this fuzzy
                        above = slicee;
                    else if (height < 0.) // XXX need to make this fuzzy
                        below = slicee;
                    else
                        do {if (true) throw new Error("Unimplemented at "+"com/donhatchsw/util/CSG.prejava"+"("+2261 +")"); } while (false); // vertices on the plane mess everything hup later
                }
                else if (slicee.p.dim == 1)
                {
                    //
                    // Slicee is a 1-dimensional polytope,
                    // i.e. a (multi-)segment.  Note that
                    // it may have more than two vertices:
                    // e.g. when a hatchet-chop is taken out of the
                    // edge of a regular polyhedron, the resulting pieces of the
                    // chopped edge are still considered to be part
                    // of a single edge, which now has 4 vertices
                    // (with signs -, +, -, + in order).
                    //

                    //
                    // Calculate the cumulative sign
                    // of all vertices lying above and below the plane...
                    //
                    SPolytope facetStuffs[][] = new SPolytope[slicee.p.facets.length][];
                    int nAbove = 0, nBelow = 0;
                    int totalSignAbove = 0;
                    int totalSignBelow = 0;
                    for (int iFacet = 0; (iFacet) < (slicee.p.facets.length); ++iFacet)
                    {
                        facetStuffs[iFacet] = _slice(slicee.p.facets[iFacet], hyperplane, aux, ocean, subIndentString);
                        if (facetStuffs[iFacet][0] != null)
                        {
                            nAbove++;
                            totalSignAbove += slicee.p.facets[iFacet].sign;
                        }
                        if (facetStuffs[iFacet][1] != null)
                        {
                            nBelow++;
                            totalSignBelow += slicee.p.facets[iFacet].sign;
                        }
                        if (facetStuffs[iFacet][2] != null)
                        {
                            do {if (true) throw new Error("Unimplemented at "+"com/donhatchsw/util/CSG.prejava"+"("+2299 +")"); } while (false);
                        }
                    }
                    totalSignAbove = 1; // XXX FUDGE
                    totalSignBelow = -1; // XXX FUDGE
                    do { if (!(totalSignAbove + totalSignBelow == 0)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2304 +"): " + "totalSignAbove + totalSignBelow == 0" + ""); } while (false);

                    if (nAbove != 0 && nBelow != 0)
                    {
                        SPolytope facetsAbove[] = new SPolytope[nAbove + (totalSignAbove!=0 ? 1 : 0)];
                        SPolytope facetsBelow[] = new SPolytope[nBelow + (totalSignBelow!=0 ? 1 : 0)];
                        {
                            int iAbove = 0, iBelow = 0;
                            for (int iFacet = 0; (iFacet) < (slicee.p.facets.length); ++iFacet)
                            {
                                if (facetStuffs[iFacet][0] != null)
                                    facetsAbove[iAbove++] = facetStuffs[iFacet][0];
                                if (facetStuffs[iFacet][1] != null)
                                    facetsBelow[iBelow++] = facetStuffs[iFacet][1];
                            }
                            do { if (!(iAbove == nAbove)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2319 +"): " + "iAbove == nAbove" + ""); } while (false);
                            do { if (!(iBelow == nBelow)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2320 +"): " + "iBelow == nBelow" + ""); } while (false);
                            if (totalSignAbove != 0) // i.e. if totalSignBelow != 0
                            {
                                // Need to make a new cutpoint.
                                // Note that we don't need to set the coords on it,
                                // that will be computed lazily
                                // from its hyperplanes
                                // the first time anyone calls getCoords() on it.
                                Polytope cutPoint = new Polytope(slicee.p.dim-1,
                                                                 slicee.p.fullDim,
                                                                 new SPolytope[0], // result vertex has no facets
                                                                 addOneHyperplaneAndSort(slicee.p.contributingHyperplanes, hyperplane),
                                                                 aux);
                                facetsAbove[iAbove++] = new SPolytope(0, // initialDensity always 0 for vertices, I think
                                                                      -totalSignAbove,
                                                                      cutPoint);
                                facetsBelow[iBelow++] = new SPolytope(0, // initialDensity always 0 for vertices, I think
                                                                      -totalSignBelow,
                                                                      cutPoint);
                                on = new SPolytope(slicee.initialDensity, // XXX I think this is wrong-- how the hell are we supposed to get an initialDensity?  maybe initialDensity isn't meaningful except for top-level polytopes?
                                                   1, // XXX arbitrary-- I think signs are completely messed up, will fix later
                                                   cutPoint);
                            }
                            do { if (!(iAbove == facetsAbove.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2343 +"): " + "iAbove == facetsAbove.length" + ""); } while (false);
                            do { if (!(iBelow == facetsBelow.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2344 +"): " + "iBelow == facetsBelow.length" + ""); } while (false);
                        }

                        above = new SPolytope(slicee.initialDensity,
                                              slicee.sign,
                                              new Polytope(slicee.p.dim,
                                                           slicee.p.fullDim,
                                                           facetsAbove,
                                                           slicee.p.contributingHyperplanes,
                                                           slicee.p.aux));
                        below = new SPolytope(slicee.initialDensity,
                                              slicee.sign,
                                              new Polytope(slicee.p.dim,
                                                           slicee.p.fullDim,
                                                           facetsBelow,
                                                           slicee.p.contributingHyperplanes,
                                                           slicee.p.aux));
                    }
                    else if (nAbove != 0)
                        above = slicee;
                    else if (nBelow != 0)
                        below = slicee;
                    else
                        do { if (!(false)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2367 +"): " + "false" + ""); } while (false);
                } // slicee.p.dim == 1
                else // slicee.p.dim >= 2
                {
                    do { if (!(slicee.p.dim >= 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2371 +"): " + "slicee.p.dim >= 2" + ""); } while (false);

                    SPolytope facetStuffs[][] = new SPolytope[slicee.p.facets.length][];
                    int nAbove = 0, nBelow = 0, nOn = 0;
                    for (int iFacet = 0; (iFacet) < (slicee.p.facets.length); ++iFacet)
                    {
                        facetStuffs[iFacet] = _slice(slicee.p.facets[iFacet], hyperplane, aux, ocean, subIndentString);
                        if (facetStuffs[iFacet][0] != null)
                            nAbove++;
                        if (facetStuffs[iFacet][1] != null)
                            nBelow++;
                        if (facetStuffs[iFacet][2] != null)
                            nOn++;
                    }
                    if (nAbove != 0 && nBelow != 0)
                    {
                        SPolytope facetsAbove[] = new SPolytope[nAbove + (nOn!=0 ? 1 : 0)];
                        SPolytope facetsBelow[] = new SPolytope[nBelow + (nOn!=0 ? 1 : 0)];
                        SPolytope ridgesOn[] = new SPolytope[nOn];
                        {
                            int iAbove = 0, iBelow = 0, iOn = 0;
                            for (int iFacet = 0; (iFacet) < (slicee.p.facets.length); ++iFacet)
                            {
                                if (facetStuffs[iFacet][0] != null)
                                    facetsAbove[iAbove++] = facetStuffs[iFacet][0];
                                if (facetStuffs[iFacet][1] != null)
                                    facetsBelow[iBelow++] = facetStuffs[iFacet][1];
                                if (facetStuffs[iFacet][2] != null)
                                    ridgesOn[iOn++] = facetStuffs[iFacet][2];
                            }
                            do { if (!(iAbove == nAbove)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2401 +"): " + "iAbove == nAbove" + ""); } while (false);
                            do { if (!(iBelow == nBelow)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2402 +"): " + "iBelow == nBelow" + ""); } while (false);
                            do { if (!(iOn == nOn)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2403 +"): " + "iOn == nOn" + ""); } while (false);
                            // XXX can some ridges occur multiple times?
                            // XXX I don't think so, since On is only *new* stuff... but if I'm wrong, will need to sort ridgesOn and combine

                            if (nOn != 0)
                            {
                                Polytope cutPoint = new Polytope(slicee.p.dim-1,
                                                                 slicee.p.fullDim,
                                                                 ridgesOn,
                                                                 addOneHyperplaneAndSort(slicee.p.contributingHyperplanes, hyperplane),
                                                                 aux);
                                facetsAbove[iAbove++] = new SPolytope(slicee.initialDensity, // XXX I think this is wrong-- how the hell are we supposed to get an initial density? maybe initialDensity isn't meaningful except for top-level polytopes?
                                                                      +1,
                                                                      cutPoint);
                                facetsBelow[iBelow++] = new SPolytope(slicee.initialDensity, // XXX I think this is wrong-- how the hell are we supposed to get an initialDensity?  maybe initialDensity isn't meaningful except for top-level polytopes?
                                                                      -1,
                                                                      cutPoint);
                                on = new SPolytope(slicee.initialDensity, // XXX I think this is wrong-- how the hell are we supposed to get an initialDensity?  maybe initialDensity isn't meaningful except for top-level polytopes?
                                                   1, // XXX arbitrary-- I think signs are completely messed up, will fix later
                                                   cutPoint);
                            }
                            do { if (!(iAbove == facetsAbove.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2424 +"): " + "iAbove == facetsAbove.length" + ""); } while (false);
                            do { if (!(iBelow == facetsBelow.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2425 +"): " + "iBelow == facetsBelow.length" + ""); } while (false);
                            do { if (!(iOn == ridgesOn.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2426 +"): " + "iOn == ridgesOn.length" + ""); } while (false);
                        }

                        above = new SPolytope(slicee.initialDensity,
                                              slicee.sign,
                                              new Polytope(slicee.p.dim,
                                                           slicee.p.fullDim,
                                                           facetsAbove,
                                                           slicee.p.contributingHyperplanes,
                                                           slicee.p.aux));
                        below = new SPolytope(slicee.initialDensity,
                                              slicee.sign,
                                              new Polytope(slicee.p.dim,
                                                           slicee.p.fullDim,
                                                           facetsBelow,
                                                           slicee.p.contributingHyperplanes,
                                                           slicee.p.aux));

                    }
                    else if (nAbove != 0)
                        above = slicee;
                    else if (nBelow != 0)
                        below = slicee;
                    else
                        do { if (!(false)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2450 +"): " + "false" + ""); } while (false);
                } // slicee.p.dim >= 2
                aboveBelowOn = new SPolytope[] {above, below, on};
                ocean.put(slicee.p, aboveBelowOn);
            }
            else
            {
                if (verboseLevel >= 1)
                    System.out.println(indentString+"    (found it in the ocean)");
            }
            if (true)
            {
                // XXX make sure we are not sharing!
                // XXX I think maybe we should be just working unsigned
                // XXX and then orient afterwards.
                // XXX bleah, unfortunately this doesn't seem to make any difference, the bugs must be elsewhere.
                for (int i = 0; (i) < (3); ++i)
                {
                    if (aboveBelowOn[i] != null)
                        aboveBelowOn[i] = new SPolytope(aboveBelowOn[i].initialDensity,
                                                        aboveBelowOn[i].sign,
                                                        aboveBelowOn[i].p);
                }
            }
            if (verboseLevel >= 1)
            {
                System.out.println("slicee" + " = " + Arrays.toStringCompact(slicee));
                System.out.println("aboveBelowOn" + " = " + Arrays.toStringCompact(aboveBelowOn));
                System.out.println(indentString+"out CSG._slice (slicee dim = "+slicee.p.dim+")");
            }
            return aboveBelowOn;
        } // _slice

        //
        // XXX OLD-- look through for signs of intelligence and then delete
        // Slice a signed polytope by a plane.
        // Returns a slicee.dim-1 dimensional signed polytope.
        //
        private static SPolytope oldSlice(SPolytope slicee,
                                          Hyperplane hyperplane,
                                          Object aux,
                                          String indentString) // for debugging
        {
            String subIndentString = null;
            if (verboseLevel >= 1)
            {
                System.out.println(indentString+"in CSG.oldSlice");
                subIndentString = indentString + "    ";
            }
            if (verboseLevel >= 2)
                System.out.println(subIndentString + "slicee = " + slicee.toString(subIndentString+"         ", false, false, null));
            if (verboseLevel >= 3)
                System.out.println(subIndentString + "slicee = " + slicee.toString(subIndentString, true, true, null));
            if (verboseLevel >= 2)
            {
                System.out.println(subIndentString + "hyperplane: " + hyperplane);
            }

            do { if (!(slicee.p.dim >= 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2508 +"): " + "slicee.p.dim >= 1" + ""); } while (false);
            do { if (!(Arrays.indexOfUsingEqualsSymbol(slicee.p.contributingHyperplanes, hyperplane) == -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2509 +"): " + "Arrays.indexOfUsingEqualsSymbol(slicee.p.contributingHyperplanes, hyperplane) == -1" + ""); } while (false);

            SPolytope result;
            if (slicee.p.dim == 1)
            {
                //
                // Slicee is a 1-dimensional polytope,
                // i.e. a (multi-)segment.  Note that
                // it may have more than two vertices:
                // e.g. when a hatchet-chop is taken out of the
                // edge of a regular polyhedron, the resulting pieces of the
                // chopped edge are still considered to be part
                // of a single edge, which now has 4 vertices
                // (with signs -, +, -, + in order).
                //

                //
                // Calculate the cumulative sign
                // of all vertices lying above the plane...
                //

                int minNumVertsAbovePlane = 0;
                int maxNumVertsAbovePlane = 0;

                int signOfVertsStrictlyAbovePlane = 0;
                int signOfVertsOnOrAbovePlane = 0;
                SPolytope vertices[] = slicee.p.facets;
                int iVertex, nVertices = vertices.length;
                for (iVertex = 0; (iVertex) < (nVertices); ++iVertex)
                {
                    SPolytope vertex = vertices[iVertex];
                    double vertexCoords[] = vertex.p.getCoords();
                    double heightOfVertexAbovePlane = VecMath.dot(hyperplane.normal, vertexCoords) - hyperplane.offset;
                    // XXX need to use an epsilon test!
                    if (heightOfVertexAbovePlane >= 0.)
                    {
                        maxNumVertsAbovePlane++;
                        if (heightOfVertexAbovePlane > 0.)
                            minNumVertsAbovePlane++;
                    }
                }
                // XXX use even-odd rule since I don't seem to be smart enough for anything else
                if (minNumVertsAbovePlane != maxNumVertsAbovePlane
                 || slicee.initialDensity == (minNumVertsAbovePlane & 1))
                {
                    // Plane doesn't cross this segment
                    // (or crosses it just barely, so we can say it doesn't).
                    result = null;
                }
                else
                {
                    result = new SPolytope(0, // XXX initialDensity always 0 for vertices, I think-- think about it.  could just follow example of >1 case and set it to slicee.initialDensity, but I'd like to understand what I'm doing
                                           1, // XXX using even-odd rule, anything nonzero is 1.  top-level caller will fix.
                                           new Polytope(slicee.p.dim-1, // == 0
                                                        slicee.p.fullDim,
                                                        new SPolytope[0], // result vertex has no facets
                                                        addOneHyperplaneAndSort(slicee.p.contributingHyperplanes, hyperplane),
                                                        aux));
                }
            } // slicee.p.dim == 1
            else
            {
                do { if (!(slicee.p.dim > 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2571 +"): " + "slicee.p.dim > 1" + ""); } while (false);

                //
                // The facets of (slicee sliced)
                // are (facets of slicee) sliced.
                //
                int iFacet, nFacets = slicee.p.facets.length;

                int maxSlicedFacets = nFacets;
                SPolytope slicedFacets[] = new SPolytope[maxSlicedFacets];
                int nSlicedFacets = 0;

                for (iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    SPolytope slicedFacet = oldSlice(slicee.p.facets[iFacet],
                                                  hyperplane,
                                                  aux,
                                                  subIndentString);
                    if (slicedFacet != null)
                        slicedFacets[nSlicedFacets++] = slicedFacet;
                }
                if (nSlicedFacets == 0
                 && slicee.initialDensity == 0)
                {
                    // No facets and initialDensity == 0
                    // and dim != 0
                    // and  so we are nothing.
                    do { if (!(slicee.initialDensity == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2598 +"): " + "slicee.initialDensity == 0" + ""); } while (false); // XXX bad, should go off
                    result = null;
                }
                else
                {
                    slicedFacets = (SPolytope[])Arrays.subarray(slicedFacets, 0, nSlicedFacets); // resize

                    result = new SPolytope(slicee.initialDensity,
                                           1, // XXX assuming even-odd rule; will fix later
                                           new Polytope(slicee.p.dim-1,
                                                        slicee.p.fullDim,
                                                        slicedFacets,
                                                        addOneHyperplaneAndSort(slicee.p.contributingHyperplanes, hyperplane),
                                                        slicee.p.aux));
                }
            } // slicee.p.dim > 1

            if (verboseLevel >= 2)
                System.out.println(subIndentString + "result = " + (result==null?"(null)":result.toString(subIndentString+"         ", false, false, null)));
            if (verboseLevel >= 3)
                System.out.println(subIndentString + "result = " + (result==null?"(null)":result.toString(subIndentString, true, true, null)));

            if (verboseLevel >= 1)
                System.out.println(indentString+"out CSG.oldSlice");

            return result;
        } // oldSlice

        // recursive work function used by intersect()
        private static SPolytope _intersect(SPolytope A,
                                            SPolytope B,
                                            java.util.Hashtable ocean, // hashtable of intersection polytopes created, keyed by contributing hyperplanes
                                            String indentString) // for debugging
        {
            String subIndentString = null;
            if (verboseLevel >= 1)
            {
                System.out.println(indentString+"in CSG._intersect");
                subIndentString = indentString + "        ";
            }
            if (verboseLevel >= 2)
                System.out.println(indentString+"    A = "+A.toString(indentString+"        ", false, false, null));
            if (verboseLevel >= 2)
                System.out.println(indentString+"    B = "+B.toString(indentString+"        ", false, false, null));
            if (verboseLevel >= 2
             && (A.p.facets.length > 0
              || B.p.facets.length > 0))
                System.out.println();

            int fullDim = A.p.fullDim;
            do { if (!(fullDim == B.p.fullDim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+2648 +"): " + "fullDim == B.p.fullDim" + ""); } while (false);

            if (true)
            {
                //
                // Bounding box test for quick rejection.
                // Intersect bounding boxes of vertices...
                //
                double bboxA[][] = A.p.getBBox();
                double bboxB[][] = B.p.getBBox();
                double bbox[][] = new double[2][fullDim];
                VecMath.bboxIntersect(bbox, bboxA, bboxB);
                boolean bboxIsEmpty = false; // and counting
                for (int iDim = 0; (iDim) < (fullDim); ++iDim)
                    if (bbox[0][iDim] > bbox[1][iDim]) // XXX should be fuzzy I think
                        bboxIsEmpty = true;
                if (bboxIsEmpty)
                {
                    if (A.initialDensity == 0
                     && B.initialDensity == 0)
                    {
                        if (verboseLevel >= 1)
                        {
                            System.out.println(indentString+"    empty bbox with both initial densities 0, returning null");
                            System.out.println(indentString+"out CSG.intersect");
                        }
                        return null;
                    }
                    else if (A.initialDensity == 0)
                    {
                        if (verboseLevel >= 1)
                        {
                            System.out.println(indentString+"    empty bbox, A's initial density is 0, B's is not, returning A");
                            System.out.println(indentString+"out CSG.intersect");
                        }
                        return A;
                    }
                    else if (B.initialDensity == 0)
                    {
                        if (verboseLevel >= 1)
                        {
                            System.out.println(indentString+"    empty bbox, B's initial density is 0, A's is not, returning B");
                            System.out.println(indentString+"out CSG.intersect");
                        }
                        return B;
                    }
                    else
                    {
                        // Both A and B have nonzero initial density.
                        // XXX Could optimize by simply concatenating
                        // XXX the arrays together, so to speak,
                        // XXX but I don't think this happens enough in
                        // XXX the current application to be worth worrying about.
                        if (verboseLevel >= 1)
                        {
                            System.out.println(indentString+"    empty bbox, both initial densities nonzero, but not smart enough to do anything about it yet :-(");
                        }
                    }
                }
            }


            Hyperplane contributingHyperplanes[] = unionOfHyperplanesAndSort(A.p.contributingHyperplanes, B.p.contributingHyperplanes);
            HashableSortedArray key = new HashableSortedArray(contributingHyperplanes);

            int resultDim = fullDim - contributingHyperplanes.length;

            if (resultDim < 0)
            {
                if (verboseLevel >= 1)
                {
                    System.out.println(indentString+"    too many hyperplanes! returning null");
                    System.out.println(indentString+"out CSG.intersect");
                }
                return null;
            }

            // Look in the ocean for already computed...
            {
                Object flotsam = ocean.get(key);
                if (flotsam != null)
                {
                    // Found it in the ocean, so it was previously computed.
                    if (verboseLevel >= 1)
                        System.out.println(indentString+"        found it in the ocean");
                    if (flotsam instanceof Polytope)
                    {
                        if (verboseLevel >= 2)
                        {
                            System.out.println(indentString+"        and it was good");
                            System.out.println(indentString+"out CSG.intersect");
                        }
                        return new SPolytope(0, // XXX initial density always 0 in this case?
                                             1, // XXX probably not right, fix later
                                             (Polytope)flotsam);
                    }
                    else
                    {
                        // It was previously computed and came out null.
                        if (verboseLevel >= 1)
                        {
                            System.out.println(indentString+"        and it was null");
                            System.out.println(indentString+"out CSG.intersect");
                        }
                        return null;
                    }
                }
            } // looked in the ocean

            SPolytope result;
            if (resultDim == 0)
            {
                double coords[];
                if (A.p.dim == 0)
                    coords = A.p.getCoords();
                else if (B.p.dim == 0)
                    coords = B.p.getCoords();
                else
                    coords = intersectHyperplanes(contributingHyperplanes);

                {
                    // XXX could use something smaller than union
                    // XXX in various cases of initialDensity? think about it
                    double unionBBox[][] = VecMath.bboxUnion(A.p.getBBox(),
                                                             B.p.getBBox());
                    double bigEps = 1e-3; // need not be very exact; the bigger the safer 
                    if (coords != null
                     && !VecMath.closedBBoxContains(unionBBox, coords, bigEps))
                    {
                        if (verboseLevel >= 2)
                        {
                            System.out.println(indentString + "        HA! No way! coords were out of there!");
                        }
                        coords = null;
                    }

                    // XXX research question: can something like this
                    // XXX be done even if resultDim > 0?
                }

                if (coords != null)
                {
                    boolean inA;
                    {
                        double eps = 1e-12; // XXX ad-hoc, need to make a coherent scheme
                        int densityMinMax[] = new int[2];
                        calcDensity(A,
                                    coords,
                                    eps,
                                    densityMinMax);
                        int density = densityMinMax[0];
                        if (density != densityMinMax[1])
                        {
                            if (verboseLevel >= 0)
                            {
                                System.out.println("WARNING:");
                                System.out.println("A" + " = " + (A));
                                System.out.println("coords" + " = " + VecMath.toString(coords));
                                System.out.println("densityMinMax[0]" + " = " + (densityMinMax[0]));
                                System.out.println("densityMinMax[1]" + " = " + (densityMinMax[1]));
                            }
                            //assert(false);
                        }
                        inA = ((density&1) == 1); // XXX even-odd rule for now
                    }
                    boolean inB = false; // shut up compiler
                    if (inA) // otherwise inB doesn't matter
                    {
                        double eps = 1e-12; // XXX ad-hoc, need to make a coherent scheme
                        int densityMinMax[] = new int[2];
                        calcDensity(B,
                                    coords,
                                    eps,
                                    densityMinMax);
                        int density = densityMinMax[0];
                        if (density != densityMinMax[1])
                        {
                            if (verboseLevel >= 0)
                            {
                                System.out.println("WARNING:");
                                System.out.println("B" + " = " + (B));
                                System.out.println("coords" + " = " + VecMath.toString(coords));
                                System.out.println("densityMinMax[0]" + " = " + (densityMinMax[0]));
                                System.out.println("densityMinMax[1]" + " = " + (densityMinMax[1]));
                            }
                            //assert(false);
                        }
                        inB = ((density&1) == 1); // XXX even-odd rule for now
                    }
                    if (inA && inB)
                    {
                        if (A.p.dim == 0)
                            result = A;
                        else if (B.p.dim == 0)
                            result = B;
                        else
                        {
                            result = new SPolytope(
                                            0, 1, // XXX nonsense, will straighten out later
                                            new Polytope(0,
                                                         fullDim,
                                                         new SPolytope[0],
                                                         contributingHyperplanes,
                                                         null)); // no aux, since it's not a full-dimensional subset of any existing polytope
                            result.p.setCoords(coords); // so they won't need to be recalculated
                        }
                    }
                    else
                        result = null;
                }
                else
                    result = null;
            }
            else // resultDim > 0
            {
                //
                // Trying to maybe get it right
                // in the case of flush stuff
                // (e.g. A&A of dim 1 or 2 in test program).
                // I don't know if this will work,
                // and it's certainly WAY more compute intensive...
                // I think maybe the union-of-hyperplanes logic
                // isn't right though :-(
                // Also, need to do this first
                // so that we won't get a "found it in the ocean and it was
                // null".
                // NOTE, we really only need to do this
                // when a's subspace contains b's subspace (i.e.
                // one set of hyperplanes contains the other).
                // XXX need to think about whether there's an efficient
                // XXX way of enumerating all pairs for which this is true
                //
                int nMoreFacets = 0;
                SPolytope moreFacets[] = null;
                if (false) // maybe pursue this some other time
                {
                    if (verboseLevel >= 1)
                        System.out.println(indentString+"    Trying more expensive boundary stuff...");
                    moreFacets = new SPolytope[A.p.facets.length
                                             * B.p.facets.length];
                    for (int i = 0; (i) < (A.p.facets.length); ++i)
                    for (int j = 0; (j) < (B.p.facets.length); ++j)
                    {
                        SPolytope a = A.p.facets[i];
                        SPolytope b = B.p.facets[j];
                        // Only proceed if a's subspace contains b's subspace
                        // or vice-versa.
                        {
                            Hyperplane unionOfHyperplanes[] = unionOfHyperplanesAndSort(a.p.contributingHyperplanes, b.p.contributingHyperplanes);
                            if (unionOfHyperplanes.length
                             != ((a.p.contributingHyperplanes.length)<=(b.p.contributingHyperplanes.length)?(a.p.contributingHyperplanes.length):(b.p.contributingHyperplanes.length)))

                                continue;
                        }
                        if (verboseLevel >= 2)
                        {
                            System.out.println(indentString+"            a = "+a.toString(indentString+"            ", false, false));
                            System.out.println(indentString+"            b = "+b.toString(indentString+"            ", false, false));
                        }
                        SPolytope a_intersect_b = _intersect(a, b, ocean, subIndentString);
                        if (a_intersect_b != null)
                            moreFacets[nMoreFacets++] = a_intersect_b;
                    }
                    if (verboseLevel >= 1)
                        System.out.println(indentString+"    Found "+nMoreFacets+"/"+moreFacets.length+" more facets");
                }


                int nFacets = 0;
                SPolytope facets[] = new SPolytope[A.p.facets.length
                                                 + B.p.facets.length]; // max possible

                for (int i = 0; (i) < (A.p.facets.length); ++i)
                {
                    SPolytope a = A.p.facets[i];
                    if (verboseLevel >= 2)
                        System.out.println(indentString+"        a = "+a.toString(indentString+"            ", false, false));
                    SPolytope a_intersect_B = _intersect(a, B, ocean, subIndentString);
                    if (a_intersect_B != null)
                        facets[nFacets++] = a_intersect_B;
                }
                for (int i = 0; (i) < (B.p.facets.length); ++i)
                {
                    SPolytope b = B.p.facets[i];
                    if (verboseLevel >= 2)
                        System.out.println(indentString+"        b = "+b.toString(indentString+"            ", false, false));
                    SPolytope A_intersect_b = _intersect(A, b, ocean, subIndentString);
                    if (A_intersect_b != null)
                        facets[nFacets++] = A_intersect_b;
                }

                facets = (SPolytope[])Arrays.subarray(facets, 0, nFacets); // resize

                if (nMoreFacets > 0)
                {
                    moreFacets = (SPolytope[])Arrays.subarray(moreFacets, 0, nMoreFacets); // resize
                    facets = (SPolytope[])Arrays.concat(facets, moreFacets);
                }

                result = new SPolytope(
                                       ((A.initialDensity)<=(B.initialDensity)?(A.initialDensity):(B.initialDensity)), // XXX is this right? have to think about it
                                       A.sign, // XXX probably completely irrelevant
                                       new Polytope(resultDim,
                                                    fullDim,
                                                    facets,
                                                    contributingHyperplanes,
                                                    resultDim==A.p.dim ? A.p.aux : resultDim==B.p.dim ? B.p.aux : null)); // XXX prefers A's aux. probably not much better we can do, unless we want to combine the auxes somehow, but that's probably too much trouble and the current application doesn't need it
                //
                // Hack after the fact:
                // if it's equal to A or B, return A or B instead
                // (XXX should detect that earlier and save lots of work
                // and unnecessary allocations, maybe)
                // XXX this may be defunct,
                // XXX have to think about it
                //
                {
                    if (result.p.dim == A.p.dim
                     && VecMath.equalsUsingEqualsSymbol(result.p.facets, A.p.facets))
                    {
                        result = new SPolytope(result.initialDensity,
                                               result.sign,
                                               A.p);
                    }
                    else if (result.p.dim == B.p.dim
                     && VecMath.equalsUsingEqualsSymbol(result.p.facets, B.p.facets))
                    {
                        result = new SPolytope(result.initialDensity,
                                               result.sign,
                                               B.p);
                    }
                }
            }

            if (result != null
             && result.p.dim >= 1
             && result.initialDensity == 0
             && result.p.facets.length == 0)
                result = null;

            if (result != null)
                ocean.put(key, result.p);
            else
                ocean.put(key, new Object()); // means null when fished out

            if (verboseLevel >= 2)
                System.out.println(indentString+"    result = "+(result==null?"(null)":result.toString(indentString+"             ", false, false, null)));

            if (verboseLevel >= 1)
                System.out.println(indentString+"out CSG._intersect");
            return result;
        } // _intersect

        /** Computes the intersection of two signed polytopes. */
        public static SPolytope intersect(SPolytope A,
                                          SPolytope B)
        {
            SPolytope result = _intersect(A, B,
                                          new java.util.Hashtable(),
                                          "");

            // _intersect returns an empty polytope
            // as null for efficiency, but the rest of the world
            // can't be expected to handle it.
            if (result == null)
            {
                int fullDim = A.p.fullDim;
                do { if (!(fullDim == B.p.fullDim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3014 +"): " + "fullDim == B.p.fullDim" + ""); } while (false);
                int dim = fullDim - ((fullDim-A.p.dim)
                                   + (fullDim-B.p.dim));
                result = new SPolytope(0,1,
                                       new Polytope(dim,
                                                    fullDim,
                                                    new SPolytope[0],
                                                    new Hyperplane[0], // XXX uh oh, zero hyperplanes isn't really right, maybe should do the hyperplane union thing in case someone actually looks at this
                                                    null));
            }

            orientDeep(result); // XXX get rid of this when I get intersect to not fuck up the orientations to begin with
            return result;
        } // intersect

        /** Computes the complement of a signed polytope. */
        public static SPolytope complement(SPolytope sp)
        {
            return new SPolytope(1-sp.initialDensity,
                                 -sp.sign,
                                 sp.p);
        } // complement

        /** Computes the union of two signed polytopes. */
        public static SPolytope union(SPolytope A, SPolytope B)
        {
            return complement(intersect(complement(A), complement(B)));
        } // union

        /** Computes the difference of two signed polytopes. */
        private static SPolytope diff(SPolytope A, SPolytope B)
        {
            return intersect(A, complement(B));
        } // diff


        //
        // Assign signs to a (non-yet-finished)
        // n-dimensional polytope P,
        // its facets, and the facets of the facets, etc., recursively
        // in such a way that, for every flag:
        //      P, facet F, ridge R, ..., face f, edge e, vertex v,
        // the product of the signs of P, F, R, ..., f, e, v
        // should be the same as the sign of the volume of the simplex
        // whose vertices are the respective centers of those n+1 elements,
        // in order.
        // I.e. it should be equal to the sign of the determinant of the matrix
        // whose rows are the n vectors
        // i(F)-i(P), i(R)-(P), ..., i(f)-i(P), i(e)-i(P), i(v)-i(P)
        // where i() denotes any interior point of the respective polytope.
        //
        // So, in particular,
        //    If n==1:  From left to right,
        //              segment starts are -, segment ends are +
        //    If n==2:  Walking CCW around a contour
        //              (i.e. with inside on the left and outside on the right),
        //              e.sign * initialVertex.sign is -
        //              and e.sign * finalVertex.sign is +
        //    If n==3: Walking on the surface
        //              along an edge e CCW around a face f,
        //              f.sign * e.sign * initialVertex.sign is -
        //              f.sign * e.sign * finalVertex.sign is +
        // etc.
        //
        // XXX should split this up into functional stuff
        // XXX and cosmetic stuff,
        // XXX and cosmetic stuff should be applied even to hypercubes
        // XXX and stuff.
        //

        public static void orientDeep(SPolytope sp) // XXX should not be public!  change back to private after PolytopePuzzleDescription doesn't need it any more!
        {
            if (verboseLevel >= 1)
                System.out.println("in orientDeep");
            if (verboseLevel >= 2)
            {
                System.out.println("    Before anything:");
                System.out.println("        "+sp.toString("        "));
            }

            int dim = sp.p.dim;

            Polytope allElements[][] = sp.p.getAllElements();

            if (true)
            {
                for (int iDim = 0; (iDim) < (dim+1); ++iDim)
                {
                    Polytope ofDim[] = allElements[iDim];
                    int nOfDim = ofDim.length;
                    for (int iOfDim = 0; (iOfDim) < (nOfDim); ++iOfDim)
                        orientFacetsConsistently(ofDim[iOfDim]);
                }

                if (verboseLevel >= 2)
                {
                    System.out.println("    After recursively orienting everyone consistently:");
                    System.out.println("        "+sp.toString("        "));
                }
            }

            if (false)
            {
                //
                // If total sign is wrong
                // (along some arbitrary flag),
                // then reverse the sign of sp so it becomes right.
                //
                System.out.println("WARNING: orientDeep global orientation not implemented yet"); // XXX
                if (verboseLevel >= 2)
                {
                    System.out.println("    After fixing global sign if it was wrong:");
                    System.out.println("        "+sp.toString("        "));
                }
            }


            if (true)
            {
                if (dim >= 1)
                {
                    //
                    // Push sign of sp down into facets,
                    // making sign of sp +1.
                    //
                    int mySign = sp.sign;
                    SPolytope facets[] = sp.p.facets;
                    int nFacets = facets.length;
                    for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                        facets[iFacet].sign *= mySign;
                    sp.sign = 1;

                    if (verboseLevel >=2)
                    {
                        System.out.println("    After pushing sign down into facets:");
                        System.out.println("        "+sp.toString("        "));
                    }

                    //
                    // Push sign of facet down into ridges,
                    // making the signs of all facets +1.
                    // Note this only makes sense when dim > 1.
                    // Also, it's pointless to do this any further:
                    // we can't make all ridge signs +,
                    // since they occur on multiple facets,
                    // in opposite-signed pairs.
                    //
                    if (dim >= 2)
                    {
                        for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                        {
                            SPolytope facet = facets[iFacet];
                            int facetSign = facet.sign;
                            if (facetSign != 1)
                            {
                                SPolytope ridgesThisFacet[] = facet.p.facets;
                                int nRidgesThisFacet = ridgesThisFacet.length;
                                for (int iRidgeThisFacet = 0; (iRidgeThisFacet) < (nRidgesThisFacet); ++iRidgeThisFacet)
                                    ridgesThisFacet[iRidgeThisFacet].sign *= facetSign;
                                facet.sign = 1;
                            }
                        }

                        if (verboseLevel >= 2)
                        {
                            System.out.println("    After pushing facet signs down into ridges:");
                            System.out.println("        "+sp.toString("        "));
                        }
                    }
                }
            }

            //
            // Edge cosmetic tweak:
            // for each edge, put the - vertex first and the + one second.
            // (If there are more than two vertices on the edge,
            // then don't worry about it.)
            //
            if (true)
            {
                if (dim >= 1)
                {
                    Polytope edges[] = sp.p.getAllElements()[1];
                    int nEdges = edges.length;
                    for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
                    {
                        Polytope edge = edges[iEdge];
                        SPolytope vertsThisEdge[] = edge.facets;
                        if (vertsThisEdge.length == 2
                         && vertsThisEdge[0].sign == 1
                         && vertsThisEdge[1].sign == -1)
                        {
                            SPolytope temp;
                            {temp=(vertsThisEdge[0]);vertsThisEdge[0]=(vertsThisEdge[1]);vertsThisEdge[1]=(temp);};
                        }
                    }
                }

                if (verboseLevel >= 2)
                {
                    System.out.println("    After edge cosmetic tweak:");
                    System.out.println("        "+sp.toString("        "));
                }
            }


            //
            // Face cosmetic tweak:
            // for each 2-d face, put the edges on each contour in order.
            // This requires that the edge cosmetic tweak (above)
            // was done first.
            // XXX this is O(n^2) where n is face size,
            // XXX so can be time-consuming
            // XXX for huge faces.
            //
            if (true)
            {
                if (dim >= 2)
                {
                    Polytope faces[] = sp.p.getAllElements()[2];
                    int nFaces = faces.length;
                    for (int iFace = 0; (iFace) < (nFaces); ++iFace)
                    {
                        Polytope p = faces[iFace];
                        SPolytope edges[] = p.facets;
                        int nEdges = edges.length;

                        int contourStart = 0;


                        Polytope initialVertexOnContour = null; // initialization not necessary, but to shut up compiler
                        if (contourStart < nEdges)
                        {
                            do { if (!(edges[contourStart].p.facets.length >= 2)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3247 +"): " + "edges[contourStart].p.facets.length >= 2" + ""); } while (false); // XXX can't we have an edge that's all of the line!? think about this (also same below)
                            initialVertexOnContour = edges[contourStart].p.facets[((edges[contourStart].sign)==1?0:1)].p;
                        }


                        //
                        // See whether this face has any multi-edges.
                        // If it does, it's no use trying to cosmeticize
                        // this face.
                        //
                        {
                            int iEdge;
                            for (iEdge = 0; (iEdge) < (nEdges); ++iEdge)
                            {
                                SPolytope sedgeI = edges[iEdge];
                                Polytope edgeI = sedgeI.p;
                                if (edgeI.facets.length != 2
                                 || edgeI.facets[0].sign != -1
                                 || edgeI.facets[1].sign != 1)
                                    break;
                            }
                            if (iEdge < nEdges)
                                continue; // this face has a multi-edge or something, so it's hopeless
                        }

                        for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
                        {
                            SPolytope sedgeI = edges[iEdge];
                            Polytope edgeI = sedgeI.p;
                            do { if (!(edgeI.facets.length == 2 && edgeI.facets[0].sign == -1 && edgeI.facets[1].sign == 1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3278 +"): " + "edgeI.facets.length == 2 && edgeI.facets[0].sign == -1 && edgeI.facets[1].sign == 1" + ""); } while (false);


                            Polytope finalVertexOnEdgeI = edgeI.facets[((sedgeI.sign)==1?1:0)].p;
                            if (finalVertexOnEdgeI
                             == initialVertexOnContour)
                            {
                                // iEdge is the last edge on this contour.
                                contourStart = iEdge+1;
                                if (contourStart < nEdges)
                                {
                                    do { if (!(edges[contourStart].p.facets.length >= 2)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3287 +"): " + "edges[contourStart].p.facets.length >= 2" + ""); } while (false); // XXX can't we have an edge that's all of the line!? think about this. (also same above)
                                    initialVertexOnContour = edges[contourStart].p.facets[((edges[contourStart].sign)==1?0:1)].p;
                                }
                            }
                            else
                            {
                                // iEdge is not the last edge on this contour;
                                // find the next one.
                                int jEdge;
                                for (jEdge = iEdge+1; jEdge < nEdges; jEdge++)
                                {
                                    SPolytope sedgeJ = edges[jEdge];
                                    Polytope edgeJ = sedgeJ.p;
                                    Polytope initialVertexOnEdgeJ = edgeJ.facets[((sedgeJ.sign)==1?0:1)].p;
                                    if (initialVertexOnEdgeJ == finalVertexOnEdgeI)
                                        break; // found next
                                }
                                do { if (!(jEdge < nEdges)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3304 +"): " + "jEdge < nEdges" + ""); } while (false); // found next
                                if (jEdge > iEdge+1)
                                {
                                    SPolytope temp;
                                    {temp=(edges[iEdge+1]);edges[iEdge+1]=(edges[jEdge]);edges[jEdge]=(temp);};
                                }
                            }
                        }
                    }
                }
                if (verboseLevel >= 2)
                {
                    System.out.println("    After face cosmetic tweak:");
                    System.out.println("        "+sp.toString("        "));
                }
            } // face cosmetic tweak

            //
            // XXX One more potential nice cosmetic tweak:
            // XXX prefer nice orderings for earlier facets.
            //
            {
            }

            if (verboseLevel >= 1)
                System.out.println("out orientDeep");
        } // orientDeep



        //
        // p is an unfinished polytope (meaning we can and do
        // change it in place).
        // Assumes all facets' facets, etc. are already consistently oriented;
        // i.e. this must be called in a bottom-up order.
        //
        private static void orientFacetsConsistently(Polytope p)
        {
            if (verboseLevel >= 2)
            {
                System.out.println("in orientFacetsConsistently("+dimToPrefix(p.dim)+p.id+")");
                if (verboseLevel >= 3)
                    System.out.println("p" + " = " + (p));
            }
            SPolytope facets[] = p.facets;
            int nFacets = facets.length;

            if (nFacets == 0)
            {
                if (verboseLevel >= 2)
                    System.out.println("out orientFacetsConsistently, boy that was hard :-)");
                return;
            }

            if (p.dim == 1)
            {
                if (nFacets > 2) // XXX maybe should do this anyway, for testing?
                {
                    //
                    // Take the farthest vertex from v0,
                    // and call it b.  Take the farthest vertex from b,
                    // and call it a.
                    // 
                    double a[], b[];
                    {
                        double from[] = facets[0].p.getCoords();
                        double farthest[] = null;
                        {
                            double farthestDistSqrd = Double.NEGATIVE_INFINITY;
                            for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                            {
                                double thisCoords[] = facets[iFacet].p.getCoords();
                                double thisDistSqrd = VecMath.distsqrd(from, thisCoords);
                                if (thisDistSqrd > farthestDistSqrd)
                                {
                                    farthest = thisCoords;
                                    farthestDistSqrd = thisDistSqrd;
                                }
                            }
                            do { if (!(farthest != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3383 +"): " + "farthest != null" + ""); } while (false);
                        }
                        b = farthest;
                        from = b;
                        {
                            double farthestDistSqrd = Double.NEGATIVE_INFINITY;
                            for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                            {
                                double thisCoords[] = facets[iFacet].p.getCoords();
                                double thisDistSqrd = VecMath.distsqrd(from, thisCoords);
                                if (thisDistSqrd > farthestDistSqrd)
                                {
                                    farthest = thisCoords;
                                    farthestDistSqrd = thisDistSqrd;
                                }
                            }
                            do { if (!(farthest != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3399 +"): " + "farthest != null" + ""); } while (false);
                        }
                        a = farthest;
                    }

                    final double dir[] = VecMath.vmv(b, a);

                    //
                    // Sort along dir...
                    //
                    SortStuff.sort(facets,
                                   new SortStuff.Comparator() {
                                       public int compare(Object _a, Object _b)
                                       {
                                           SPolytope a = (SPolytope)_a;
                                           SPolytope b = (SPolytope)_b;
                                           double aDot = VecMath.dot(a.p.getCoords(), dir);
                                           double bDot = VecMath.dot(b.p.getCoords(), dir);
                                           return aDot < bDot ? -1 :
                                                  aDot > bDot ? 1 : 0;
                                       }
                                   });
                } // if (nFacets > 2)

                int sign = -1;
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    facets[iFacet].sign = sign;
                    sign = -sign;
                }
                if (verboseLevel >= 2)
                    System.out.println("out orientFacetsConsistently, edges are easy");
                return;
            }

            //
            // facetNeighbors[iFacet][iRidgeOnFacet][0] is the index
            // of the other facet sharing that ridge, and
            // facetNeighbors[iFacet][iRidgeOnFacet][1]
            // is the index of the ridge in the neighbor facet's facet list.
            // XXX perhaps finished polytopes should always have these tables
            // XXX so we don't have to recalculate them?
            //
            int facetNeighbors[][][] = new int[nFacets][][/*2*/];
            {
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                    facetNeighbors[iFacet] = new int[facets[iFacet].p.facets.length][/*2*/];
                java.util.Hashtable firstFacetContainingRidge = new java.util.Hashtable();
                for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                {
                    Polytope facet = facets[iFacet].p;
                    SPolytope ridgesThisFacet[] = facet.facets;
                    int nRidgesThisFacet = ridgesThisFacet.length;
                    for (int iRidgeThisFacet = 0; (iRidgeThisFacet) < (nRidgesThisFacet); ++iRidgeThisFacet)
                    {
                        Polytope ridge = ridgesThisFacet[iRidgeThisFacet].p;
                        int myInfo[] = {iFacet, iRidgeThisFacet};
                        int neighborInfo[] = (int[])firstFacetContainingRidge.remove(ridge);
                        if (neighborInfo == null)
                        {
                            firstFacetContainingRidge.put(ridge, myInfo);
                        }
                        else
                        {
                            facetNeighbors[iFacet][iRidgeThisFacet] = neighborInfo;
                            facetNeighbors[neighborInfo[0]][neighborInfo[1]] = myInfo;
                        }
                    }
                }
                do { if (!(firstFacetContainingRidge.size() == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3468 +"): " + "firstFacetContainingRidge.size() == 0" + ""); } while (false); // everything matched XXX change this assert to failure I think
            } // facetNeighbors

            int signOfFirstFacet = 1; // arbitrarily XXX is there a better choice?
            boolean isSigned[] = new boolean[nFacets];
            for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                isSigned[iFacet] = false;

            SPolytope contours[][] = new SPolytope[nFacets][]; // worst case
            int nContours = 0; // and counting
            SPolytope currentContour[] = new SPolytope[nFacets];

            int stackHeight = 0;
            int stack[] = new int[nFacets];
            for (int contourStart = 0; (contourStart) < (nFacets); ++contourStart)
            {
                if (!isSigned[contourStart])
                {
                    int currentContourSize = 0;
                    {
                        int iFacet = contourStart;
                        SPolytope facet = facets[iFacet];
                        facet.sign = signOfFirstFacet;
                        isSigned[iFacet] = true;
                        stack[stackHeight++] = iFacet; // push iFacet on to stack
                        currentContour[currentContourSize++] = facets[iFacet];
                    }
                    while (stackHeight != 0)
                    {
                        int iFacet = stack[--stackHeight]; // pop iFacet off of stack
                        SPolytope facet = facets[iFacet];
                        SPolytope ridgesThisFacet[] = facet.p.facets;
                        int nRidgesThisFacet = ridgesThisFacet.length;
                        for (int iRidgeThisFacet = 0; (iRidgeThisFacet) < (nRidgesThisFacet); ++iRidgeThisFacet)
                        {
                            int neighborInfo[/*2*/] = facetNeighbors[iFacet][iRidgeThisFacet];
                            int iNeighbor = neighborInfo[0];
                            int iRidgeNeighborFacet = neighborInfo[1];

                            if (!isSigned[iNeighbor])
                            {
                                //System.out.println(""+iFacet+" -> "+iNeighbor+"");
                                SPolytope neighborFacet = facets[iNeighbor];
                                SPolytope ridgesNeighborFacet[] = neighborFacet.p.facets;

                                // Want facets[iFacet].sign
                                //          * ridgesThisFacet[iRidgeThisFacet].sign
                                //  = - facets[iNeighbor].sign
                                //          * ridgesNeighborFacet[iRidgeNeighborFacet].sign.
                                int signOfRidgeOnNeighbor = ridgesNeighborFacet[iRidgeNeighborFacet].sign;
                                do { if (!(signOfRidgeOnNeighbor == 1 || signOfRidgeOnNeighbor == -1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3519 +"): " + "signOfRidgeOnNeighbor == 1 || signOfRidgeOnNeighbor == -1" + ""); } while (false); // otherwise should be / instead of * below, and I'm not sure what it means if it doesn't divide evenly, have to think about it some day


                                neighborFacet.sign = -facet.sign
                                                   * ridgesThisFacet[iRidgeThisFacet].sign
                                                   * signOfRidgeOnNeighbor;
                                isSigned[iNeighbor] = true;
                                stack[stackHeight++] = iNeighbor; // push iNeighbor on to stack
                                currentContour[currentContourSize++] = facets[iNeighbor];
                            }
                            else
                            {
                                //System.out.println("("+iFacet+" -> "+iNeighbor+")");
                            }
                        }
                    } // while (stackHeight != 0)
                    contours[nContours++] = (SPolytope[])Arrays.subarray(currentContour, 0, currentContourSize);
                }
            } // for contourStart
            for (int iFacet = 0; (iFacet) < (nFacets); ++iFacet)
                do { if (!(isSigned[iFacet])) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3538 +"): " + "isSigned[iFacet]" + ""); } while (false);

            //
            // Make temporary SPolytopes out of the contours...
            // XXX should avoid this if only one contour
            //
            SPolytope contourSPolytopes[] = new SPolytope[nContours];
            {
                for (int iContour = 0; (iContour) < (nContours); ++iContour)
                    contourSPolytopes[iContour] = new SPolytope(0,1,
                                        new Polytope(p.dim,
                                                     p.fullDim,
                                                     contours[iContour],
                                                     p.contributingHyperplanes,
                                                     p.aux));
            }

            contours = null; // make sure we don't use it by accident after this

            //
            // Find the coordinate axis in which the bbox is the largest,
            // and sort the contour list according to
            // minimum coordinate along this coordinate axis.
            // Then an item in the sorted list
            // cannot contain any earlier item.
            //
            double bbox[][] = p.getBBox();
            double bboxSize[] = VecMath.vmv(bbox[1], bbox[0]);
            final int ax = VecMath.maxi(bboxSize);
            SortStuff.sort(contourSPolytopes,
                   new SortStuff.Comparator() {
                       public int compare(Object _a, Object _b)
                       {
                           SPolytope a = (SPolytope)_a;
                           SPolytope b = (SPolytope)_b;
                           double aDot = a.p.getBBox()[0][ax];
                           double bDot = b.p.getBBox()[0][ax];
                           return aDot < bDot ? -1 :
                                  aDot > bDot ? 1 : 0;
                       }
                   });


            if (verboseLevel >= 2
             && nContours > 1)
            {
                System.out.println("The contours:\n");
                for (int iContour = 0; (iContour) < (nContours); ++iContour)
                {
                    System.out.println("    "+iContour+":");
                    System.out.println(contourSPolytopes[iContour]);
                }
            }


            //
            // Make a tree whose nodes are the contours,
            // ordered by contour inclusion.
            //
            int root = 0;
            int firstChild[] = VecMath.fillvec(nContours, -1);
            int nextSibling[] = VecMath.fillvec(nContours, -1);
            {
                double eps = 1e-12; // XXX ad-hoc, need to make a coherent scheme
                // scratch for loop...
                    int densityMinMax[] = new int[2];

                for (int iContour = 1; iContour < nContours; ++iContour) // skip 0, it's already on the tree as the root
                {
                    double anyCoordsOnContour[];
                    {
                        // v = any vertex on contour iContour
                        Polytope v = contourSPolytopes[iContour].p;
                        while (v.dim > 0)
                            v = v.facets[0].p;
                        anyCoordsOnContour = v.getCoords();
                    }

                    // Hang iContour on the tree.
                    // Walk down the tree starting with root...
                    for (int iNode = root; ;)
                    {
                        SPolytope nodeSPolytope = contourSPolytopes[iNode];
                        calcDensity(nodeSPolytope,
                                    anyCoordsOnContour,
                                    eps,
                                    densityMinMax);
                        int density = densityMinMax[0];
                        do { if (!(density == densityMinMax[1])) throw new Error("Assumption failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3626 +"): " + "density == densityMinMax[1]" + ""); } while (false); // must be unambiguous
                        boolean isContainedInNode = (density&1) == 1;
                        if (isContainedInNode)
                        {
                            // proceed on to children
                            if (firstChild[iNode] == -1)
                            {
                                firstChild[iNode] = iContour;
                                break;
                            }
                            iNode = firstChild[iNode];
                        }
                        else
                        {
                            // proceed on to next sibling
                            if (nextSibling[iNode] == -1)
                            {
                                nextSibling[iNode] = iContour;
                                break;
                            }
                            iNode = nextSibling[iNode];
                        }
                    }
                }
            }
            if (verboseLevel >= 2
             && nContours > 1)
            {
                System.out.println("firstChild" + " =\n" + VecMath.toString(firstChild));
                System.out.println("nextSibling" + " =\n" + VecMath.toString(nextSibling));
            }

            double normals[][] = new double[p.contributingHyperplanes.length][];
            {
                for (int iHyperplane = 0; (iHyperplane) < (normals.length); ++iHyperplane)
                    normals[iHyperplane] = p.contributingHyperplanes[iHyperplane].normal;
            }

            //
            // Start by orienting all the contours in the same direction
            // (e.g. all counterclockwise).
            //
            {
                for (int iContour = 0; (iContour) < (nContours); ++iContour)
                {
                    SPolytope contourSPolytope = contourSPolytopes[iContour];
                    double volume = volume(contourSPolytope);
                    if (volume < 0)
                    {
                        SPolytope facetsThisContour[] = contourSPolytope.p.facets;
                        int nFacetsThisContour = facetsThisContour.length;
                        for (int iFacetThisContour = 0; (iFacetThisContour) < (nFacetsThisContour); ++iFacetThisContour)
                        {
                            SPolytope facet = facetsThisContour[iFacetThisContour];
                            facet.sign = -facet.sign;
                        }
                    }
                }
            }

            //
            // Reverse the signs of alternate levels of contours.
            // We use each contourSPolytope's sign to store +-1
            // depending on whether we are reversing it or not.
            // (the final value of its sign doesn't matter
            // since it is getting thrown away).
            //
            {
                for (int iContour = 0; (iContour) < (nContours); ++iContour)
                {
                    SPolytope contourSPolytope = contourSPolytopes[iContour];
                    int sign = contourSPolytope.sign;
                    if (sign < 0)
                    {
                        SPolytope facetsThisContour[] = contourSPolytope.p.facets;
                        int nFacetsThisContour = facetsThisContour.length;
                        for (int iFacetThisContour = 0; (iFacetThisContour) < (nFacetsThisContour); ++iFacetThisContour)
                        {
                            SPolytope facet = facetsThisContour[iFacetThisContour];
                            facet.sign = -facet.sign;
                        }
                    }
                    if (firstChild[iContour] != -1)
                        contourSPolytopes[firstChild[iContour]].sign = -sign;
                    if (nextSibling[iContour] != -1)
                        contourSPolytopes[nextSibling[iContour]].sign = sign;
                }
            }

            if (verboseLevel >= 2)
                System.out.println("out orientFacetsConsistently("+dimToPrefix(p.dim)+p.id+")");
        } // orientFacetsConsistently


// XXX I think PolyCSG should deal with SPolytopes, not Polytopes...
// XXX then this won't be necessary.  At least I think that's what I think.


        /** Computes the union of two unsigned polytopes. */
        public static Polytope union(Polytope A, Polytope B)
        {
            return union(new SPolytope(0,1,A),
                         new SPolytope(0,1,B)).p;
        } // union of unsigned Polytopes
        /** Computes the intersection of two unsigned polytopes. */
        public static Polytope intersect(Polytope A, Polytope B)
        {
            return intersect(new SPolytope(0,1,A),
                             new SPolytope(0,1,B)).p;
        } // intersect of unsigned Polytopes
        /** Computes the difference of two unsigned polytopes. */
        public static Polytope diff(Polytope A, Polytope B)
        {
            return diff(new SPolytope(0,1,A),
                        new SPolytope(0,1,B)).p;
        } // diff of unsigned Polytopes


        private static String signToString(int sign)
        {
            return sign == 1 ? "+" :
                   sign == -1 ? "-" :
                   sign == 0 ? "!" :
                   "(sign="+sign+")";
        } // signToString

        private static String dimToPrefix(int dim)
        {
            if (dim <= 4)
                return "vefch".substring(dim,dim+1);
            else
                return "(" + dim + "d)"; // sort of lame but it's the best I can think of at the moment, and it doesn't seem to look too bad
        } // dimToPrefix

        // XXX this is really lame, should maybe restructure so we don't need it
        private static Object itemOfAThatsNotInB(Object A[], Object B[])
        {
            int iA, nA = A.length;
            int iB, nB = B.length;
            for (iA = 0; (iA) < (nA); ++iA)
            {
                for (iB = 0; (iB) < (nB); ++iB)
                    if (A[iA] == B[iB])
                        break;
                if (iB == nB) // A[iA] was not in B
                    return A[iA];
            }
            do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3773 +"): " + "false" + ""); } while (false);
            return null;
        } // itemOfAThatsNotInB

        // XXX used by slice, remove if I get rid of slice
        private static Hyperplane[] addOneHyperplaneAndSort(Hyperplane[] list, Hyperplane last)
        {
            list = (Hyperplane[])Arrays.append(list, last);
            SortStuff.sort(list,
                           new SortStuff.Comparator() {
                               public int compare(Object _a, Object _b)
                               {
                                   Hyperplane a = (Hyperplane)_a;
                                   Hyperplane b = (Hyperplane)_b;
                                   return a.id < b.id ? -1 :
                                          a.id > b.id ? 1 : 0;
                               }
                           });
            return list;
        } // addOneHyperplaneAndSort

        private static Hyperplane[] unionOfHyperplanesAndSort(Hyperplane[] these, Hyperplane[] those)
        {
            Hyperplane list[] = (Hyperplane[])Arrays.concat(these, those);
            SortStuff.Comparator comparator =
               new SortStuff.Comparator() {
                   public int compare(Object _a, Object _b)
                   {
                       Hyperplane a = (Hyperplane)_a;
                       Hyperplane b = (Hyperplane)_b;
                       return a.id < b.id ? -1 :
                              a.id > b.id ? 1 : 0;
                   }
               };

            SortStuff.sort(list, comparator);
            int n = SortStuff.nodup(list, list.length, comparator);
            list = (Hyperplane[])Arrays.subarray(list, 0, n);
            return list;
        } // unionOfHyperplanesAndSort

        // return the point that is the intersection of n hyperplanes in n dimensions.
        private static double[] intersectHyperplanes(Hyperplane hyperplanes[])
        {
            int nHyperplanes = hyperplanes.length;
            if (nHyperplanes == 0)
                return new double[0];
            int iHyperplane;
            for (iHyperplane = 0; (iHyperplane) < (nHyperplanes); ++iHyperplane)
                if (hyperplanes[iHyperplane].spanningPoints == null)
                    break; // this hyperplane is not defined by spanning points
            if (iHyperplane == nHyperplanes)
            {
                //
                // All hyperplanes
                // are from spanning points.
                // We just need to find the one point
                // that is in the spanning set of all of them.
                //
                do { if (!(nHyperplanes > 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3832 +"): " + "nHyperplanes > 0" + ""); } while (false); // we would have returned early above
                int iSpanningPoint, nSpanningPoints = hyperplanes[0].spanningPoints.length;
                for (iSpanningPoint = 0; (iSpanningPoint) < (nSpanningPoints); ++iSpanningPoint)
                {
                    double point[] = hyperplanes[0].spanningPoints[iSpanningPoint];
                    for (iHyperplane = 1; iHyperplane < nHyperplanes; ++iHyperplane)
                        if (Arrays.indexOfUsingEqualsSymbol(hyperplanes[iHyperplane].spanningPoints, point) == -1)
                            break; // it's not in this one
                    if (iHyperplane == nHyperplanes)
                    {
                        // It's in all of them!
                        return point;
                    }
                }
                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/CSG.prejava"+"("+3846 +"): " + "false" + ""); } while (false); // XXX not sure if this can happen or not, so flag it for now and think about it if it goes off
            }

            //
            // Calculate explicit coords of the vertex.
            // We want column vector c
            // such that
            //    normal0 dot c == offset0
            //    normal1 dot c == offset1
            //    normal2 dot c == offset2
            //    ...
            // so c = inv(normalsMatrix) * offsetsColumnVector.
            //
// 3870 # 3885 "com/donhatchsw/util/CSG.prejava"
            int nRows = hyperplanes.length;
            double normalsMatrix[][] = new double[nRows][nRows];
            double offsetsColumnVector[] = new double[nRows];
            for (int iRow = 0; (iRow) < (nRows); ++iRow)
            {
                VecMath.copyvec(normalsMatrix[iRow], hyperplanes[iRow].normal);
                offsetsColumnVector[iRow] = hyperplanes[iRow].offset;
            }
            double c[] = VecMath.invmxv(normalsMatrix, offsetsColumnVector);

            // XXX not an adequate test, I don't think
            if (c.length > 0
             && (Double.isInfinite(c[0])
              || Double.isNaN(c[0])))
                return null;

            return c;
        } // intersectHyperplanes



        //
        // Wrapper for a sorted array,
        // allowing it to be used as a hash key and value.
        // Equality is based on equality of the component items.
        // The hash code is the xor of all the component items' hash codes.
        // XXX should use sum, like some java structures do? think about it
        //
        private static class HashableSortedArray
        {
            private Object array[];
            public HashableSortedArray(Object array[])
            {
                this.array = array;
            }
            public int hashCode()
            {
                int hashCode = 0;
                int n = array.length;
                for (int i = 0; (i) < (n); ++i)
                    hashCode ^= array[i].hashCode();
                return hashCode;
            } // equals
            public boolean equals(Object _that)
            {
                HashableSortedArray that = (HashableSortedArray)_that;
                Object[] these = this.array;
                Object[] those = that.array;
                int nThese = these.length, nThose = those.length;
                if (nThese != nThose)
                    return false;
                for (int i = 0; (i) < (nThese); ++i)
                    if (!these[i].equals(those[i]))
                        return false;
                return true;
            } // equals
        } // class HashableSortedArray

        private static class HashablePair
        {
            private Object first, second;
            public HashablePair(Object first, Object second)
            {
                this.first = first;
                this.second = second;
            }
            public int hashCode()
            {
                return 3*first.hashCode() + second.hashCode();
            }
            public boolean equals(Object _that)
            {
                HashablePair that = (HashablePair)_that;
                return this.first.equals(that.first)
                    && this.second.equals(that.second);
            }
        } // class HashablePair


        private static int factorial(int n)
        {
            return n==0 ? 1 : n*factorial(n-1);
        }
        private static int intpow(int a, int b)
        {
            return b==0 ? 1 : intpow(a, b-1) * a;
        }
        private static int digit(int x, int i, int base)
        {
            return x / intpow(base,i) % base;
        }
        private static String intToString(int x, int base)
        {
            return (x>base ? intToString(x/base,base) : "") + x%base;
        }

        private static int numOccurancesOf(String str, String substr)
        {
            int numOccurances = 0;
            int i = -1;
            while ((i = str.indexOf(substr, i+1)) != -1)
                numOccurances++;
            return numOccurances;
        } // numOccurancesOf


    private static final boolean evalVerbose = false;
    /**
    *  Evaluate an expression such as "<code>(-((A-B) i (A i D))) u C</code>"
    *  from the beginning of a string,
    *  leaving the unparsed remainder of the string in the "rest" parameter.
    */
    public static SPolytope eval(
                String expr, // XXX term, really... sorry for the confusion
                int dim,
                int fullDim,
                String varNames[/*1*/][], // array is passed by ref so can be resized
                SPolytope varValues[/*1*/][], // array is passed by ref so can be resized
                String rest[/*1*/]) // unparsed remainder of string is returned here
        throws java.text.ParseException
    {
        SPolytope result = null;
        String orig_expr = expr; // for error messages

        expr = expr.trim();
        if (expr.length() == 0)
            throw new java.text.ParseException("Empty expression \""+expr+"\"", orig_expr.length()-expr.length());
        char firstChar = expr.charAt(0);
        expr = expr.substring(1);

        if (firstChar == '0')
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> 0");
            result = new SPolytope(0,1,
                                   new Polytope(dim,
                                                fullDim,
                                                new SPolytope[0], // no facets
                                                new Hyperplane[0], // no hyperplanes
                                                null)); // no aux // XXX use substring?
        }
        else if (firstChar == '1')
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> 1");
            result = new SPolytope(1,1,
                                   new Polytope(dim,
                                                fullDim,
                                                new SPolytope[0], // no facets
                                                new Hyperplane[0], // no hyperplanes
                                                null)); // no aux // XXX use substring?
        }
        else if ("-~".indexOf(firstChar) != -1)
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> -expr");
            SPolytope operand = eval(expr,
                                     dim,
                                     fullDim,
                                     varNames,
                                     varValues,
                                     rest);
            expr = rest[0];
            result = complement(operand);
        }
        else if (firstChar == 's'
              && expr.startsWith("can(\""))
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> scan(\"quoted_string\")");
            int endQuoteIndex = expr.indexOf("\"", 5);
            if (endQuoteIndex < 0)
                throw new java.text.ParseException("bad bad bad expression \""+orig_expr+"\"", orig_expr.length()-expr.length());
            String fileName = expr.substring(5, endQuoteIndex);

            expr = expr.substring(endQuoteIndex+1);
            if (!expr.startsWith(")"))
                throw new java.text.ParseException("bad bad bad expression \""+orig_expr+"\"", orig_expr.length()-expr.length());
            expr = expr.substring(1);

            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(fileName));
                StringBuffer sb = new StringBuffer();
                char buf[] = new char[1024];
                int numRead = 0;
                while ((numRead = reader.read(buf)) > -1)
                    sb.append(String.valueOf(buf, 0, numRead));
                reader.close();
                String fileContents = sb.toString();
                result = SPolytope.fromStringInPcalcFormat(fileContents);
                // XXX LAME!  should be a method that reads from a Reader instead of having to slurp the whole file!
            }
            catch (java.io.IOException e)
            {
                throw new java.text.ParseException("Fooey, something went wrong reading file \""+fileName+"\": "+e, orig_expr.length()-expr.length());
            }
        }
        else if (firstChar == '(')
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> (something)  (not sure))");
            SPolytope LHS = eval(expr,
                                 dim,
                                 fullDim,
                                 varNames,
                                 varValues,
                                 rest);
            expr = rest[0];
            expr = expr.trim(); // XXX actually only need to trim at beginning
            if (expr.length() == 0)
                throw new java.text.ParseException("Premature end of expression \""+orig_expr+"\"",orig_expr.length()-expr.length());
            char operatorChar = expr.charAt(0);
            expr = expr.substring(1);

            if (operatorChar == ')')
            {
                if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> (expr)  (now I'm sure)");
                result = LHS;
            }
            else
            {
                SPolytope RHS = eval(expr,
                                     dim,
                                     fullDim,
                                     varNames,
                                     varValues,
                                     rest);
                expr = rest[0];
                if (expr.length() == 0)
                    throw new java.text.ParseException("Unmatched left paren in expression \""+orig_expr+"\"",orig_expr.length()-expr.length());
                char shouldBeCloseParen = expr.charAt(0);
                expr = expr.substring(1);
                if (shouldBeCloseParen != ')')
                    throw new java.text.ParseException("Expected ')', got '"+shouldBeCloseParen+"' in expression \""+orig_expr+"\"",orig_expr.length()-expr.length());

                if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> (expr binop expr)  (now I'm sure)");

                if ("i&".indexOf(operatorChar) != -1)
                    result = intersect(LHS, RHS);
                else if ("u|".indexOf(operatorChar) != -1)
                    result = union(LHS, RHS);
                else if ("dm-\\".indexOf(operatorChar) != -1)
                    result = diff(LHS, RHS);
                else if ("*x".indexOf(operatorChar) != -1)
                    result = cross(LHS, RHS);
                else
                    throw new java.text.ParseException("Unknown binary operation '"+operatorChar+"' in expression \""+orig_expr+"\"",orig_expr.length()-expr.length());
                if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> (expr "+operatorChar+" expr)");
            }
        }
        else if (Character.isJavaIdentifierStart(firstChar))
        {
            if (evalVerbose) System.out.println("\""+orig_expr+"\": expr -> varName and maybe assignment, not sure");
            String varName = ""+firstChar;
            while (expr.length() > 0
                && Character.isJavaIdentifierPart(firstChar = expr.charAt(0)))
            {
                varName += firstChar;
                expr = expr.substring(1);
            }

            int iVar, nVars = varNames[0].length;
            for (iVar = 0; (iVar) < (nVars); ++iVar)
                if (varName.equals(varNames[0][iVar]))
                    break;

            // XXX grammar is violated here... we are making varName=value
            // XXX into a term when it should be an expression;
            // XXX e.g.  A - B = C  is legal
            // XXX and is equivalent to A - (B = C),
            // XXX when it shouldn't be legal :-(
            // XXX also A=B-C gets evaluated as (A=B)-C :-( :-(
            // XXX really need to fix this
            expr = expr.trim();
            if (expr.length() >= 1
             && expr.charAt(0) == '=')
            {
                expr = expr.substring(1);

                expr = expr.trim(); // XXX should be only at beginning
                if (expr.length() == 0
                 || expr.startsWith(")"))
                {
                    // unset the variable-- that is, crunch it out
                    // of both arrays, keeping the arrays in order.
                    if (iVar < nVars)
                    {
                        varNames[0] = (String[])Arrays.concat(
                            Arrays.subarray(varNames[0],0,iVar),
                            Arrays.subarray(varNames[0],iVar+1,nVars-(iVar+1)));
                        varValues[0] = (SPolytope[])Arrays.concat(
                            Arrays.subarray(varValues[0],0,iVar),
                            Arrays.subarray(varValues[0],iVar+1,nVars-(iVar+1)));
                    }

                    result = null;
                }
                else
                {
                    SPolytope RHS = eval(expr,
                                         dim,
                                         fullDim,
                                         varNames,
                                         varValues,
                                         rest);
                    expr = rest[0];

                    if (iVar == nVars)
                    {
                        // append the new name,value pair
                        varNames[0] = (String[])Arrays.append(varNames[0], varName);
                        varValues[0] = (SPolytope[])Arrays.append(varValues[0], RHS);
                    }
                    else
                    {
                        // replace old value
                        varValues[0][iVar] = RHS;
                    }

                    result = RHS;
                }
            }
            else
            {
                if (iVar == nVars)
                    throw new java.text.ParseException("Undefined variable \""+varName+"\" in expression \""+orig_expr+"\"",orig_expr.length()-expr.length());
                result = varValues[0][iVar];
            }
        }
        else
        {
            throw new java.text.ParseException("Unexpected first char '"+firstChar+"' in expression \""+orig_expr+"\"",orig_expr.length()-expr.length());
        }

        rest[0] = expr;
        return result;
    } // eval

    /**
    *  Evaluate an expression such as "<code>(-((A-B) i (A i D))) u C</code>".
    *  The grammar is:
    *  <pre>
    * 
    *           expr -&gt; term
    *           expr -&gt; term [i&amp;^*] term // intersect(LHS,RHS)
    *           expr -&gt; term [u|+] term  // union(LHS,RHS)
    *           expr -&gt; term [-\dm] term // diff(LHS,RHS)
    *           expr -&gt; varName = expr   // sets varName's value to value of expr
    *           term -&gt; "0"              // empty set
    *           term -&gt; "1"              // all of space
    *           term -&gt; varName          // corresponding varValue
    *           term -&gt; [~-] term        // complement(RHS)
    *           term -&gt; "(" expr ")"
    *           term -&gt; scan "(" quoted_string ")"
    *  </pre>
    * 
    *  Note all the alternative operator chars; I'm wishy washy.
    *  The ones that are letters must be separated from adjacent variable names
    *  by spaces.
    * 
    *  XXX need a way to unset a variable
    */
    public static SPolytope eval(
                String expr,
                int dim,
                int fullDim,
                String varNames[/*1*/][], // array is passed by ref so can be resized
                SPolytope varValues[/*1*/][]) // array is passed by ref so can be resized
        throws java.text.ParseException
    {
        String rest[] = new String[1];
        String term = "("+expr+")";


        // XXX hack that my little brain seems to want...
        // XXX add as many parens at beginning as necessary
        while (numOccurancesOf(term, "(")
             < numOccurancesOf(term, ")"))
        {
            term = "(" + term;
        }


        SPolytope result = eval(term,
                                dim,
                                fullDim,
                                varNames,
                                varValues,
                                rest);
        if (rest[0].length() != 0)
        {
            int errorOffsetInTerm = term.length() - rest[0].length();
            throw new java.text.ParseException("Trailing garbage \""+rest[0]+"\" in expression \""+term+"\"",errorOffsetInTerm);
        }

        if (!isOrientedDeep(result.p))
        {
            System.out.println("WARNING: top-level eval returning a badly oriented polytope!");
        }
        return result;
    } // eval



    /**
    *  A little test program...
    *  Take two hypercubes, and do union, intersection, and difference.
    */
    public static void main(String args[])
        throws java.io.IOException
    {
        System.out.println("in main");

        if (args.length < 1)
        {
            System.err.println("Usage: CSG <dim> [<expr>]");
            System.exit(1);
        }

        int fullDim = Integer.parseInt(args[0]);
        int dim = fullDim;

        if (false)
        {
            // just test simplex
            System.out.println("makeSimplex(dim)" + " = " + (makeSimplex(dim)));
            return;
        }

        SPolytope A = makeHypercube(VecMath.fillvec(dim, -1.), 2.);
        SPolytope B = makeHypercube(VecMath.fillvec(dim, 1.), 2.);
        SPolytope C = makeHypercube(VecMath.fillvec(dim, 0.), 2.);
        //SPolytope simplex = makeSimplex(dim);
        SPolytope simplex = A; // XXX not ready yet
// 4300 # 4359 "com/donhatchsw/util/CSG.prejava"
        if (args.length >= 2)
        {
            //
            // Evaluate given input expression
            //
            String expr = args[1];
            String varNames[][] = {{"A","B","C","simplex"}};
            SPolytope varValues[][] = {{ A, B, C, simplex }};
            SPolytope result = null;
            try
            {
                result = eval(expr,
                              dim,
                              fullDim,
                              varNames,
                              varValues);
            }
            catch (java.text.ParseException e)
            {
                System.err.println("Exception parsing expression \""+expr+"\": "+e);
            }
            System.out.println("expr" + " = " + (expr));
            System.out.println("result" + " = " + (result));
        }
        else
        {
            //
            // Do a command shell.  Woohoo!
            //
            java.io.BufferedReader reader =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));

            // XXX hellacious hack for one-time-only pushback.
            // XXX it doesn't work anyway; the newline doesn't seem to make it.
            // XXX why not???
            if (false)
            {
                String nl = System.getProperty("line.separator");
                java.io.PushbackReader pushbackReader = new java.io.PushbackReader(new java.io.InputStreamReader(System.in), 100);
                pushbackReader.unread(("help"+nl).toCharArray());
                reader = new java.io.BufferedReader(pushbackReader);
            }

            String prompt = "yeah? ";

            String varNames[][] = {{"A","B","C","simplex"}};
            SPolytope varValues[][] = {{ A, B, C, simplex }};

            {
                System.out.println("Current variables:");
                for (int iVar = 0; (iVar) < (varNames[0].length); ++iVar)
                {
                    System.out.print("    " + varNames[0][iVar] + " = ");
                    System.out.println(varValues[0][iVar].toString("        "));
                }
            }

            while (true)
            {
                System.out.print(prompt);
                String line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.equals(""))
                    continue;

                //
                // Special variables...
                //
                if (line.startsWith("v=")
                 || line.startsWith("v ="))
                {
                    verboseLevel = Integer.parseInt(line.substring(line.indexOf('=')+1));
                    System.out.println("verboseLevel" + " = " + (verboseLevel));
                    continue;
                }
                if (line.startsWith("d=")
                 || line.startsWith("d =")
                 || line.startsWith("dim=")
                 || line.startsWith("dim ="))
                {
                    fullDim = Integer.parseInt(line.substring(line.indexOf('=')+1));
                    dim = fullDim;
                    System.out.println("dim" + " = " + (dim));
                    System.out.println("WARNING: polytopes of different dimensions will act strangely when mixed");
                    continue;
                }
                if (line.equals("q")
                 || line.equals("quit")
                 || line.equals("exit"))
                {
                    break;
                }
                if (line.equals("help"))
                {
                    {
                        System.out.println("Current variables:");
                        for (int iVar = 0; (iVar) < (varNames[0].length); ++iVar)
                        {
                            System.out.print("    " + varNames[0][iVar] + " = ");
                            System.out.println(varValues[0][iVar].toString("        "));
                        }
                    }
                    continue;
                }

                SPolytope result = null;
                try
                {
                    result = eval(
                                 line,
                                 dim,
                                 fullDim,
                                 varNames,
                                 varValues);
                }
                catch (java.text.ParseException pe)
                {
                    System.err.println("Parse error in expression \""+line+"\": ");
                    System.err.println("    "+pe.getLocalizedMessage()+", position "+pe.getErrorOffset());
                }
                if (result != null)
                {
                    System.out.println(result);
                    System.out.println("counts(result.p)" + " = " + VecMath.toString(counts(result.p)));
                    System.out.println("volume(result)" + " = " + (volume(result)));

                    for (int iFacet = 0; (iFacet) < (result.p.facets.length); ++iFacet)
                    {
                        double areaNormalOfFacet[] = new double[dim];
                        areaNormal(areaNormalOfFacet, result.p.facets[iFacet]);
                        System.out.println("areaNormal(facets["+iFacet+"]) = "+ VecMath.toString(areaNormalOfFacet));
                        System.out.println("volume(facets["+iFacet+"]) = "+ volume(result.p.facets[iFacet]));
                    }

                    if (!isOrientedDeep(result.p))
                        System.out.println("WARNING: that was badly oriented!");
                    if (!isBinaryDensityDeep(result))
                        System.out.println("WARNING: that didn't have binary density!  Or something!");
                }
            }
        }

        System.out.println("out main");
    } // main


    //
    // Some special purpose polytope specifications,
    // pending getting a fully general makeRegularPolytope working.
    // These were generated using the commands:
    //    pcalc "{3,3}"
    //    pcalc "{5,3}"
    //    pcalc "{3,3,3}"
    //    pcalc "{5,3,3}"
    private static String pcalcString33 = ""
        +"4 0-cells:\n"
        +" 0:     0.5 0.28867513459481287 0.20412414523193151\n"
        +" 1:     0 0 -0.61237243569579447\n"
        +" 2:     0 -0.57735026918962573 0.20412414523193151\n"
        +" 3:     -0.5 0.28867513459481287 0.20412414523193151\n"
        +"6 1-cells:\n"
        +" 0:    -0 +1\n"
        +" 1:    -2 +1\n"
        +" 2:    -0 +2\n"
        +" 3:    -3 +1\n"
        +" 4:    -3 +2\n"
        +" 5:    -0 +3\n"
        +"4 2-cells:\n"
        +" 0:    +0 -1 -2\n"
        +" 1:    +1 -3 +4\n"
        +" 2:    -0 +5 +3\n"
        +" 3:    +2 -4 -5\n"
        +"1 3-cell:\n"
        +" 0:    +0 +1 +2 +3\n"
        ;
    private static String pcalcString333 = ""
        +"5 0-cells:\n"
        +" 0:     0.5 0.28867513459481287 0.20412414523193151 0.15811388300841897\n"
        +" 1:     0 0 0 -0.63245553203367588\n"
        +" 2:     0 0 -0.61237243569579447 0.15811388300841897\n"
        +" 3:     0 -0.57735026918962573 0.20412414523193151 0.15811388300841897\n"
        +" 4:     -0.5 0.28867513459481287 0.20412414523193151 0.15811388300841897\n"
        +"10 1-cells:\n"
        +" 0:    -0 +1\n"
        +" 1:    -2 +1\n"
        +" 2:    -0 +2\n"
        +" 3:    -3 +1\n"
        +" 4:    -3 +2\n"
        +" 5:    -0 +3\n"
        +" 6:    -4 +1\n"
        +" 7:    -4 +3\n"
        +" 8:    -0 +4\n"
        +" 9:    -4 +2\n"
        +"10 2-cells:\n"
        +" 0:    +0 -1 -2\n"
        +" 1:    +1 -3 +4\n"
        +" 2:    +0 -3 -5\n"
        +" 3:    +2 -4 -5\n"
        +" 4:    +3 -6 +7\n"
        +" 5:    +0 -6 -8\n"
        +" 6:    +5 -7 -8\n"
        +" 7:    +4 -9 +7\n"
        +" 8:    +2 -9 -8\n"
        +" 9:    +1 -6 +9\n"
        +"5 3-cells:\n"
        +" 0:    +0 +1 -2 +3\n"
        +" 1:    +2 +4 -5 +6\n"
        +" 2:    -3 -7 +8 -6\n"
        +" 3:    -0 -9 +5 -8\n"
        +" 4:    -1 -4 +9 +7\n"
        +"1 4-cell:\n"
        +" 0:    +0 +1 +2 +3 +4\n"
        ;
    private static String pcalcString53 = ""
        +"20 0-cells:\n"
        +"  0:     1.3090169943749477 0.5 0\n"
        +"  1:     0.80901699437494745 0.80901699437494745 -0.80901699437494745\n"
        +"  2:     0 1.3090169943749477 -0.5\n"
        +"  3:     0 1.3090169943749477 0.5\n"
        +"  4:     0.80901699437494745 0.80901699437494745 0.80901699437494745\n"
        +"  5:     0.5 0 -1.3090169943749477\n"
        +"  6:     -0.5 0 -1.3090169943749477\n"
        +"  7:     -0.80901699437494745 0.80901699437494745 -0.80901699437494745\n"
        +"  8:     1.3090169943749477 -0.5 0\n"
        +"  9:     0.80901699437494745 -0.80901699437494745 -0.80901699437494745\n"
        +" 10:     0 -1.3090169943749477 -0.5\n"
        +" 11:     -0.80901699437494745 -0.80901699437494745 -0.80901699437494745\n"
        +" 12:     -1.3090169943749477 -0.5 0\n"
        +" 13:     -1.3090169943749477 0.5 0\n"
        +" 14:     -0.80901699437494745 0.80901699437494745 0.80901699437494745\n"
        +" 15:     -0.80901699437494745 -0.80901699437494745 0.80901699437494745\n"
        +" 16:     -0.5 0 1.3090169943749477\n"
        +" 17:     0 -1.3090169943749477 0.5\n"
        +" 18:     0.80901699437494745 -0.80901699437494745 0.80901699437494745\n"
        +" 19:     0.5 0 1.3090169943749477\n"
        +"30 1-cells:\n"
        +"  0:    -0  +1\n"
        +"  1:    -2  +1\n"
        +"  2:    -3  +2\n"
        +"  3:    -3  +4\n"
        +"  4:    -0  +4\n"
        +"  5:    -5  +1\n"
        +"  6:    -5  +6\n"
        +"  7:    -6  +7\n"
        +"  8:    -2  +7\n"
        +"  9:    -0  +8\n"
        +" 10:    -8  +9\n"
        +" 11:    -5  +9\n"
        +" 12:    -10 +9\n"
        +" 13:    -10 +11\n"
        +" 14:    -6  +11\n"
        +" 15:    -12 +11\n"
        +" 16:    -13 +12\n"
        +" 17:    -13 +7\n"
        +" 18:    -13 +14\n"
        +" 19:    -3  +14\n"
        +" 20:    -12 +15\n"
        +" 21:    -16 +15\n"
        +" 22:    -16 +14\n"
        +" 23:    -17 +15\n"
        +" 24:    -17 +18\n"
        +" 25:    -19 +18\n"
        +" 26:    -19 +16\n"
        +" 27:    -19 +4\n"
        +" 28:    -8  +18\n"
        +" 29:    -17 +10\n"
        +"12 2-cells:\n"
        +"  0:    +0  -1  -2  +3  -4\n"
        +"  1:    +1  -5  +6  +7  -8\n"
        +"  2:    -0  +9  +10 -11 +5\n"
        +"  3:    -6  +11 -12 +13 -14\n"
        +"  4:    -7  +14 -15 -16 +17\n"
        +"  5:    +2  +8  -17 +18 -19\n"
        +"  6:    +20 -21 +22 -18 +16\n"
        +"  7:    -23 +24 -25 +26 +21\n"
        +"  8:    -3  +19 -22 -26 +27\n"
        +"  9:    +4  -27 +25 -28 -9\n"
        +" 10:    +12 -10 +28 -24 +29\n"
        +" 11:    -13 -29 +23 -20 +15\n"
        +"1 3-cell:\n"
        +" 0:    +0  +1  +2  +3  +4  +5  +6  +7  +8  +9  +10 +11\n"
        ;

    // Similar definition for pcalcString533,
    // too big to include here
// 4589 # 1 "com/donhatchsw/util/CSGcrap.h" 1
    // This one exceeds the max allowed string literal length,
    // so do it in two parts...
    private static String pcalcString533_firstHalf = ""
        +"600 0-cells:\n"
        +"   0:     2.0697375990463267 2.0697375990463267 2.0697375990463267 0.92561479341095798\n"
        +"   1:     1.851229586821916 1.851229586821916 1.851229586821916 1.851229586821916\n"
        +"   2:     0.92561479341095798 2.0697375990463267 2.0697375990463267 2.0697375990463267\n"
        +"   3:     0.57206140281768414 2.4232909896396002 2.4232909896396002 1.2791681840042317\n"
        +"   4:     1.2791681840042317 2.4232909896396002 2.4232909896396002 0.57206140281768414\n"
        +"   5:     2.0697375990463267 0.92561479341095798 2.0697375990463267 2.0697375990463267\n"
        +"   6:     1.2791681840042317 0.57206140281768414 2.4232909896396002 2.4232909896396002\n"
        +"   7:     0.57206140281768414 1.2791681840042317 2.4232909896396002 2.4232909896396002\n"
        +"   8:     2.4232909896396002 0.57206140281768414 2.4232909896396002 1.2791681840042317\n"
        +"   9:     2.4232909896396002 1.2791681840042317 2.4232909896396002 0.57206140281768414\n"
        +"  10:     1.1441228056353683 0 2.9953523924572845 1.851229586821916\n"
        +"  11:     1.851229586821916 0 2.9953523924572845 1.1441228056353683\n"
        +"  12:     0 1.1441228056353683 2.9953523924572845 1.851229586821916\n"
        +"  13:     0.35355339059327373 0.35355339059327373 3.3489057830505584 1.4976761962286422\n"
        +"  14:     0 1.851229586821916 2.9953523924572845 1.1441228056353683\n"
        +"  15:     0.35355339059327373 1.4976761962286422 3.3489057830505584 0.35355339059327373\n"
        +"  16:     1.1441228056353683 1.851229586821916 2.9953523924572845 0\n"
        +"  17:     0.57206140281768414 0.57206140281768414 3.5674137952749687 0.57206140281768414\n"
        +"  18:     1.4976761962286422 0.35355339059327373 3.3489057830505584 0.35355339059327373\n"
        +"  19:     1.851229586821916 1.1441228056353683 2.9953523924572845 0\n"
        +"  20:     2.0697375990463267 2.0697375990463267 0.92561479341095798 2.0697375990463267\n"
        +"  21:     1.2791681840042317 2.4232909896396002 0.57206140281768414 2.4232909896396002\n"
        +"  22:     0.57206140281768414 2.4232909896396002 1.2791681840042317 2.4232909896396002\n"
        +"  23:     2.4232909896396002 2.4232909896396002 0.57206140281768414 1.2791681840042317\n"
        +"  24:     2.4232909896396002 2.4232909896396002 1.2791681840042317 0.57206140281768414\n"
        +"  25:     1.1441228056353683 2.9953523924572845 0 1.851229586821916\n"
        +"  26:     1.851229586821916 2.9953523924572845 0 1.1441228056353683\n"
        +"  27:     0 2.9953523924572845 1.1441228056353683 1.851229586821916\n"
        +"  28:     0.35355339059327373 3.3489057830505584 0.35355339059327373 1.4976761962286422\n"
        +"  29:     0 2.9953523924572845 1.851229586821916 1.1441228056353683\n"
        +"  30:     0.35355339059327373 3.3489057830505584 1.4976761962286422 0.35355339059327373\n"
        +"  31:     1.1441228056353683 2.9953523924572845 1.851229586821916 0\n"
        +"  32:     0.57206140281768414 3.5674137952749687 0.57206140281768414 0.57206140281768414\n"
        +"  33:     1.4976761962286422 3.3489057830505584 0.35355339059327373 0.35355339059327373\n"
        +"  34:     1.851229586821916 2.9953523924572845 1.1441228056353683 0\n"
        +"  35:     0.92561479341095798 2.0697375990463267 2.7768443802328737 -0.92561479341095798\n"
        +"  36:     0.92561479341095798 2.7768443802328737 2.0697375990463267 -0.92561479341095798\n"
        +"  37:     1.4976761962286422 1.4976761962286422 2.641799001864011 -1.4976761962286422\n"
        +"  38:     2.0697375990463267 0.92561479341095798 2.7768443802328737 -0.92561479341095798\n"
        +"  39:     1.851229586821916 1.851229586821916 1.851229586821916 -1.851229586821916\n"
        +"  40:     1.4976761962286422 2.641799001864011 1.4976761962286422 -1.4976761962286422\n"
        +"  41:     2.641799001864011 1.4976761962286422 1.4976761962286422 -1.4976761962286422\n"
        +"  42:     2.7768443802328737 0.92561479341095798 2.0697375990463267 -0.92561479341095798\n"
        +"  43:     2.7768443802328737 2.0697375990463267 0.92561479341095798 -0.92561479341095798\n"
        +"  44:     2.0697375990463267 2.7768443802328737 0.92561479341095798 -0.92561479341095798\n"
        +"  45:     2.9953523924572845 1.1441228056353683 1.851229586821916 0\n"
        +"  46:     2.9953523924572845 1.851229586821916 1.1441228056353683 0\n"
        +"  47:     1.851229586821916 2.9953523924572845 0 -1.1441228056353683\n"
        +"  48:     1.1441228056353683 2.9953523924572845 0 -1.851229586821916\n"
        +"  49:     0.92561479341095798 2.7768443802328737 0.92561479341095798 -2.0697375990463267\n"
        +"  50:     1.4976761962286422 3.3489057830505584 -0.35355339059327373 -0.35355339059327373\n"
        +"  51:     0.35355339059327373 3.3489057830505584 -0.35355339059327373 -1.4976761962286422\n"
        +"  52:     0.57206140281768414 3.5674137952749687 -0.57206140281768414 -0.57206140281768414\n"
        +"  53:     -0.35355339059327373 3.3489057830505584 0.35355339059327373 -1.4976761962286422\n"
        +"  54:     0 2.9953523924572845 1.1441228056353683 -1.851229586821916\n"
        +"  55:     -0.57206140281768414 3.5674137952749687 0.57206140281768414 -0.57206140281768414\n"
        +"  56:     0 3.7024591736438319 0 0\n"
        +"  57:     0 2.9953523924572845 1.851229586821916 -1.1441228056353683\n"
        +"  58:     -0.35355339059327373 3.3489057830505584 1.4976761962286422 -0.35355339059327373\n"
        +"  59:     -0.35355339059327373 3.3489057830505584 -1.4976761962286422 0.35355339059327373\n"
        +"  60:     0.35355339059327373 3.3489057830505584 -1.4976761962286422 -0.35355339059327373\n"
        +"  61:     -0.57206140281768414 3.5674137952749687 -0.57206140281768414 0.57206140281768414\n"
        +"  62:     1.851229586821916 2.9953523924572845 -1.1441228056353683 0\n"
        +"  63:     1.1441228056353683 2.9953523924572845 -1.851229586821916 0\n"
        +"  64:     0.92561479341095798 2.7768443802328737 -2.0697375990463267 0.92561479341095798\n"
        +"  65:     2.0697375990463267 2.7768443802328737 -0.92561479341095798 0.92561479341095798\n"
        +"  66:     1.4976761962286422 2.641799001864011 -1.4976761962286422 1.4976761962286422\n"
        +"  67:     0.92561479341095798 2.7768443802328737 -0.92561479341095798 2.0697375990463267\n"
        +"  68:     -0.35355339059327373 3.3489057830505584 -0.35355339059327373 1.4976761962286422\n"
        +"  69:     0 2.9953523924572845 -1.1441228056353683 1.851229586821916\n"
        +"  70:     0 2.9953523924572845 -1.851229586821916 1.1441228056353683\n"
        +"  71:     2.4232909896396002 1.2791681840042317 0.57206140281768414 2.4232909896396002\n"
        +"  72:     2.9953523924572845 1.1441228056353683 0 1.851229586821916\n"
        +"  73:     2.9953523924572845 1.851229586821916 0 1.1441228056353683\n"
        +"  74:     2.7768443802328737 0.92561479341095798 -0.92561479341095798 2.0697375990463267\n"
        +"  75:     2.0697375990463267 0.92561479341095798 -0.92561479341095798 2.7768443802328737\n"
        +"  76:     1.851229586821916 1.1441228056353683 0 2.9953523924572845\n"
        +"  77:     2.641799001864011 1.4976761962286422 -1.4976761962286422 1.4976761962286422\n"
        +"  78:     2.7768443802328737 2.0697375990463267 -0.92561479341095798 0.92561479341095798\n"
        +"  79:     1.4976761962286422 1.4976761962286422 -1.4976761962286422 2.641799001864011\n"
        +"  80:     1.851229586821916 1.851229586821916 -1.851229586821916 1.851229586821916\n"
        +"  81:     1.1441228056353683 1.851229586821916 0 2.9953523924572845\n"
        +"  82:     0.92561479341095798 2.0697375990463267 -0.92561479341095798 2.7768443802328737\n"
        +"  83:     -0.92561479341095798 2.0697375990463267 -2.0697375990463267 2.0697375990463267\n"
        +"  84:     -0.57206140281768414 2.4232909896396002 -1.2791681840042317 2.4232909896396002\n"
        +"  85:     0 1.851229586821916 -1.1441228056353683 2.9953523924572845\n"
        +"  86:     0 1.1441228056353683 -1.851229586821916 2.9953523924572845\n"
        +"  87:     -0.57206140281768414 1.2791681840042317 -2.4232909896396002 2.4232909896396002\n"
        +"  88:     -0.57206140281768414 2.4232909896396002 -2.4232909896396002 1.2791681840042317\n"
        +"  89:     0.92561479341095798 2.0697375990463267 -2.7768443802328737 0.92561479341095798\n"
        +"  90:     0 1.851229586821916 -2.9953523924572845 1.1441228056353683\n"
        +"  91:     1.4976761962286422 1.4976761962286422 -2.641799001864011 1.4976761962286422\n"
        +"  92:     0 1.1441228056353683 -2.9953523924572845 1.851229586821916\n"
        +"  93:     0.92561479341095798 0.92561479341095798 -2.7768443802328737 2.0697375990463267\n"
        +"  94:     0.92561479341095798 0.92561479341095798 -2.0697375990463267 2.7768443802328737\n"
        +"  95:     2.9953523924572845 1.851229586821916 -1.1441228056353683 0\n"
        +"  96:     2.9953523924572845 1.1441228056353683 -1.851229586821916 0\n"
        +"  97:     2.4232909896396002 1.2791681840042317 -2.4232909896396002 -0.57206140281768414\n"
        +"  98:     2.0697375990463267 2.0697375990463267 -2.0697375990463267 -0.92561479341095798\n"
        +"  99:     2.4232909896396002 2.4232909896396002 -1.2791681840042317 -0.57206140281768414\n"
        +" 100:     1.851229586821916 1.1441228056353683 -2.9953523924572845 0\n"
        +" 101:     2.0697375990463267 0.92561479341095798 -2.7768443802328737 0.92561479341095798\n"
        +" 102:     2.7768443802328737 0.92561479341095798 -2.0697375990463267 0.92561479341095798\n"
        +" 103:     1.1441228056353683 1.851229586821916 -2.9953523924572845 0\n"
        +" 104:     1.2791681840042317 2.4232909896396002 -2.4232909896396002 -0.57206140281768414\n"
        +" 105:     3.3489057830505584 0.35355339059327373 0.35355339059327373 1.4976761962286422\n"
        +" 106:     3.5674137952749687 0.57206140281768414 0.57206140281768414 0.57206140281768414\n"
        +" 107:     3.3489057830505584 1.4976761962286422 0.35355339059327373 0.35355339059327373\n"
        +" 108:     2.9953523924572845 0 -1.1441228056353683 1.851229586821916\n"
        +" 109:     3.3489057830505584 -0.35355339059327373 -0.35355339059327373 1.4976761962286422\n"
        +" 110:     2.9953523924572845 0 -1.851229586821916 1.1441228056353683\n"
        +" 111:     3.3489057830505584 -0.35355339059327373 -1.4976761962286422 0.35355339059327373\n"
        +" 112:     3.5674137952749687 -0.57206140281768414 -0.57206140281768414 0.57206140281768414\n"
        +" 113:     3.3489057830505584 0.35355339059327373 -1.4976761962286422 -0.35355339059327373\n"
        +" 114:     3.5674137952749687 0.57206140281768414 -0.57206140281768414 -0.57206140281768414\n"
        +" 115:     3.7024591736438319 0 0 0\n"
        +" 116:     3.3489057830505584 1.4976761962286422 -0.35355339059327373 -0.35355339059327373\n"
        +" 117:     2.9953523924572845 0 1.1441228056353683 1.851229586821916\n"
        +" 118:     2.9953523924572845 0 1.851229586821916 1.1441228056353683\n"
        +" 119:     3.3489057830505584 0.35355339059327373 1.4976761962286422 0.35355339059327373\n"
        +" 120:     2.7768443802328737 -0.92561479341095798 0.92561479341095798 2.0697375990463267\n"
        +" 121:     2.9953523924572845 -1.1441228056353683 0 1.851229586821916\n"
        +" 122:     2.641799001864011 -1.4976761962286422 1.4976761962286422 1.4976761962286422\n"
        +" 123:     2.7768443802328737 -0.92561479341095798 2.0697375990463267 0.92561479341095798\n"
        +" 124:     2.9953523924572845 -1.1441228056353683 1.851229586821916 0\n"
        +" 125:     3.3489057830505584 -0.35355339059327373 1.4976761962286422 -0.35355339059327373\n"
        +" 126:     3.3489057830505584 -1.4976761962286422 0.35355339059327373 -0.35355339059327373\n"
        +" 127:     2.9953523924572845 -1.851229586821916 1.1441228056353683 0\n"
        +" 128:     3.5674137952749687 -0.57206140281768414 0.57206140281768414 -0.57206140281768414\n"
        +" 129:     2.7768443802328737 -2.0697375990463267 0.92561479341095798 0.92561479341095798\n"
        +" 130:     2.9953523924572845 -1.851229586821916 0 1.1441228056353683\n"
        +" 131:     3.3489057830505584 -1.4976761962286422 -0.35355339059327373 0.35355339059327373\n"
        +" 132:     2.9953523924572845 1.851229586821916 0 -1.1441228056353683\n"
        +" 133:     2.9953523924572845 0 1.851229586821916 -1.1441228056353683\n"
        +" 134:     2.7768443802328737 0.92561479341095798 0.92561479341095798 -2.0697375990463267\n"
        +" 135:     2.9953523924572845 0 1.1441228056353683 -1.851229586821916\n"
        +" 136:     2.9953523924572845 1.1441228056353683 0 -1.851229586821916\n"
        +" 137:     3.3489057830505584 0.35355339059327373 -0.35355339059327373 -1.4976761962286422\n"
        +" 138:     3.3489057830505584 -0.35355339059327373 0.35355339059327373 -1.4976761962286422\n"
        +" 139:     2.0697375990463267 -2.0697375990463267 2.0697375990463267 -0.92561479341095798\n"
        +" 140:     2.4232909896396002 -2.4232909896396002 1.2791681840042317 -0.57206140281768414\n"
        +" 141:     2.4232909896396002 -2.4232909896396002 0.57206140281768414 -1.2791681840042317\n"
        +" 142:     2.0697375990463267 -2.0697375990463267 0.92561479341095798 -2.0697375990463267\n"
        +" 143:     1.851229586821916 -1.851229586821916 1.851229586821916 -1.851229586821916\n"
        +" 144:     2.9953523924572845 -1.851229586821916 0 -1.1441228056353683\n"
        +" 145:     2.9953523924572845 -1.1441228056353683 0 -1.851229586821916\n"
        +" 146:     2.4232909896396002 -1.2791681840042317 0.57206140281768414 -2.4232909896396002\n"
        +" 147:     2.4232909896396002 -0.57206140281768414 1.2791681840042317 -2.4232909896396002\n"
        +" 148:     2.4232909896396002 -0.57206140281768414 2.4232909896396002 -1.2791681840042317\n"
        +" 149:     2.0697375990463267 -0.92561479341095798 2.0697375990463267 -2.0697375990463267\n"
        +" 150:     2.4232909896396002 -1.2791681840042317 2.4232909896396002 -0.57206140281768414\n"
        +" 151:     0.35355339059327373 -0.35355339059327373 1.4976761962286422 -3.3489057830505584\n"
        +" 152:     0 -1.1441228056353683 1.851229586821916 -2.9953523924572845\n"
        +" 153:     0 -1.851229586821916 1.1441228056353683 -2.9953523924572845\n"
        +" 154:     0.35355339059327373 -1.4976761962286422 0.35355339059327373 -3.3489057830505584\n"
        +" 155:     0.57206140281768414 -0.57206140281768414 0.57206140281768414 -3.5674137952749687\n"
        +" 156:     0.57206140281768414 -1.2791681840042317 2.4232909896396002 -2.4232909896396002\n"
        +" 157:     0.57206140281768414 -2.4232909896396002 1.2791681840042317 -2.4232909896396002\n"
        +" 158:     0.92561479341095798 -2.0697375990463267 2.0697375990463267 -2.0697375990463267\n"
        +" 159:     1.2791681840042317 -2.4232909896396002 0.57206140281768414 -2.4232909896396002\n"
        +" 160:     1.1441228056353683 -1.851229586821916 0 -2.9953523924572845\n"
        +" 161:     1.851229586821916 -1.1441228056353683 0 -2.9953523924572845\n"
        +" 162:     1.2791681840042317 -0.57206140281768414 2.4232909896396002 -2.4232909896396002\n"
        +" 163:     1.1441228056353683 0 1.851229586821916 -2.9953523924572845\n"
        +" 164:     1.851229586821916 0 1.1441228056353683 -2.9953523924572845\n"
        +" 165:     1.4976761962286422 -0.35355339059327373 0.35355339059327373 -3.3489057830505584\n"
        +" 166:     0.92561479341095798 -0.92561479341095798 -2.0697375990463267 -2.7768443802328737\n"
        +" 167:     1.1441228056353683 0 -1.851229586821916 -2.9953523924572845\n"
        +" 168:     1.851229586821916 0 -1.1441228056353683 -2.9953523924572845\n"
        +" 169:     2.0697375990463267 -0.92561479341095798 -0.92561479341095798 -2.7768443802328737\n"
        +" 170:     1.4976761962286422 -1.4976761962286422 -1.4976761962286422 -2.641799001864011\n"
        +" 171:     0.35355339059327373 0.35355339059327373 -1.4976761962286422 -3.3489057830505584\n"
        +" 172:     0.57206140281768414 0.57206140281768414 -0.57206140281768414 -3.5674137952749687\n"
        +" 173:     1.4976761962286422 0.35355339059327373 -0.35355339059327373 -3.3489057830505584\n"
        +" 174:     -0.35355339059327373 -0.35355339059327373 -1.4976761962286422 -3.3489057830505584\n"
        +" 175:     0 -1.1441228056353683 -1.851229586821916 -2.9953523924572845\n"
        +" 176:     -0.57206140281768414 -0.57206140281768414 -0.57206140281768414 -3.5674137952749687\n"
        +" 177:     0 0 0 -3.7024591736438319\n"
        +" 178:     0 -1.851229586821916 -1.1441228056353683 -2.9953523924572845\n"
        +" 179:     -0.35355339059327373 -1.4976761962286422 -0.35355339059327373 -3.3489057830505584\n"
        +" 180:     0.92561479341095798 -2.0697375990463267 -0.92561479341095798 -2.7768443802328737\n"
        +" 181:     -1.4976761962286422 -1.4976761962286422 1.4976761962286422 -2.641799001864011\n"
        +" 182:     -0.92561479341095798 -0.92561479341095798 2.0697375990463267 -2.7768443802328737\n"
        +" 183:     -1.1441228056353683 0 1.851229586821916 -2.9953523924572845\n"
        +" 184:     -1.851229586821916 0 1.1441228056353683 -2.9953523924572845\n"
        +" 185:     -2.0697375990463267 -0.92561479341095798 0.92561479341095798 -2.7768443802328737\n"
        +" 186:     -0.35355339059327373 0.35355339059327373 1.4976761962286422 -3.3489057830505584\n"
        +" 187:     -0.92561479341095798 -2.0697375990463267 0.92561479341095798 -2.7768443802328737\n"
        +" 188:     -1.1441228056353683 -1.851229586821916 0 -2.9953523924572845\n"
        +" 189:     -1.851229586821916 -1.1441228056353683 0 -2.9953523924572845\n"
        +" 190:     -1.4976761962286422 0.35355339059327373 0.35355339059327373 -3.3489057830505584\n"
        +" 191:     -1.4976761962286422 -0.35355339059327373 -0.35355339059327373 -3.3489057830505584\n"
        +" 192:     -0.57206140281768414 0.57206140281768414 0.57206140281768414 -3.5674137952749687\n"
        +" 193:     -2.0697375990463267 -0.92561479341095798 -2.0697375990463267 -2.0697375990463267\n"
        +" 194:     -1.2791681840042317 -0.57206140281768414 -2.4232909896396002 -2.4232909896396002\n"
        +" 195:     -0.57206140281768414 -1.2791681840042317 -2.4232909896396002 -2.4232909896396002\n"
        +" 196:     -0.92561479341095798 -2.0697375990463267 -2.0697375990463267 -2.0697375990463267\n"
        +" 197:     -1.851229586821916 -1.851229586821916 -1.851229586821916 -1.851229586821916\n"
        +" 198:     -1.1441228056353683 0 -1.851229586821916 -2.9953523924572845\n"
        +" 199:     -0.57206140281768414 -2.4232909896396002 -1.2791681840042317 -2.4232909896396002\n"
        +" 200:     -1.2791681840042317 -2.4232909896396002 -0.57206140281768414 -2.4232909896396002\n"
        +" 201:     -2.0697375990463267 -2.0697375990463267 -0.92561479341095798 -2.0697375990463267\n"
        +" 202:     -2.4232909896396002 -1.2791681840042317 -0.57206140281768414 -2.4232909896396002\n"
        +" 203:     -2.4232909896396002 -0.57206140281768414 -1.2791681840042317 -2.4232909896396002\n"
        +" 204:     -1.851229586821916 0 -1.1441228056353683 -2.9953523924572845\n"
        +" 205:     -2.7768443802328737 0.92561479341095798 -2.0697375990463267 -0.92561479341095798\n"
        +" 206:     -2.0697375990463267 0.92561479341095798 -2.7768443802328737 -0.92561479341095798\n"
        +" 207:     -1.4976761962286422 1.4976761962286422 -2.641799001864011 -1.4976761962286422\n"
        +" 208:     -1.851229586821916 1.851229586821916 -1.851229586821916 -1.851229586821916\n"
        +" 209:     -2.641799001864011 1.4976761962286422 -1.4976761962286422 -1.4976761962286422\n"
        +" 210:     -1.851229586821916 0 -2.9953523924572845 -1.1441228056353683\n"
        +" 211:     -1.1441228056353683 0 -2.9953523924572845 -1.851229586821916\n"
        +" 212:     -0.92561479341095798 0.92561479341095798 -2.7768443802328737 -2.0697375990463267\n"
        +" 213:     -2.4232909896396002 -0.57206140281768414 -2.4232909896396002 -1.2791681840042317\n"
        +" 214:     -2.9953523924572845 0 -1.851229586821916 -1.1441228056353683\n"
        +" 215:     -0.92561479341095798 0.92561479341095798 -2.0697375990463267 -2.7768443802328737\n"
        +" 216:     -1.4976761962286422 1.4976761962286422 -1.4976761962286422 -2.641799001864011\n"
        +" 217:     -2.0697375990463267 0.92561479341095798 -0.92561479341095798 -2.7768443802328737\n"
        +" 218:     -2.7768443802328737 0.92561479341095798 -0.92561479341095798 -2.0697375990463267\n"
        +" 219:     -2.9953523924572845 0 -1.1441228056353683 -1.851229586821916\n"
        +" 220:     -2.4232909896396002 -1.2791681840042317 -2.4232909896396002 -0.57206140281768414\n"
        +" 221:     -2.0697375990463267 -2.0697375990463267 -2.0697375990463267 -0.92561479341095798\n"
        +" 222:     -2.9953523924572845 -1.1441228056353683 -1.851229586821916 0\n"
        +" 223:     -3.3489057830505584 -0.35355339059327373 -1.4976761962286422 -0.35355339059327373\n"
        +" 224:     -2.9953523924572845 -1.851229586821916 -1.1441228056353683 0\n"
        +" 225:     -3.3489057830505584 -1.4976761962286422 -0.35355339059327373 -0.35355339059327373\n"
        +" 226:     -3.5674137952749687 -0.57206140281768414 -0.57206140281768414 -0.57206140281768414\n"
        +" 227:     -2.4232909896396002 -2.4232909896396002 -1.2791681840042317 -0.57206140281768414\n"
        +" 228:     -2.4232909896396002 -2.4232909896396002 -0.57206140281768414 -1.2791681840042317\n"
        +" 229:     -2.9953523924572845 -1.851229586821916 0 -1.1441228056353683\n"
        +" 230:     -2.9953523924572845 -1.1441228056353683 0 -1.851229586821916\n"
        +" 231:     -3.3489057830505584 -0.35355339059327373 -0.35355339059327373 -1.4976761962286422\n"
        +" 232:     -0.35355339059327373 -3.3489057830505584 -1.4976761962286422 -0.35355339059327373\n"
        +" 233:     -1.1441228056353683 -2.9953523924572845 -1.851229586821916 0\n"
        +" 234:     -1.851229586821916 -2.9953523924572845 -1.1441228056353683 0\n"
        +" 235:     -1.4976761962286422 -3.3489057830505584 -0.35355339059327373 -0.35355339059327373\n"
        +" 236:     -0.57206140281768414 -3.5674137952749687 -0.57206140281768414 -0.57206140281768414\n"
        +" 237:     -1.2791681840042317 -2.4232909896396002 -2.4232909896396002 -0.57206140281768414\n"
        +" 238:     -0.57206140281768414 -2.4232909896396002 -2.4232909896396002 -1.2791681840042317\n"
        +" 239:     0 -2.9953523924572845 -1.851229586821916 -1.1441228056353683\n"
        +" 240:     0 -2.9953523924572845 -1.1441228056353683 -1.851229586821916\n"
        +" 241:     -0.35355339059327373 -3.3489057830505584 -0.35355339059327373 -1.4976761962286422\n"
        +" 242:     -1.1441228056353683 -2.9953523924572845 0 -1.851229586821916\n"
        +" 243:     -1.851229586821916 -2.9953523924572845 0 -1.1441228056353683\n"
        +" 244:     -1.4976761962286422 -3.3489057830505584 0.35355339059327373 0.35355339059327373\n"
        +" 245:     -1.851229586821916 -2.9953523924572845 1.1441228056353683 0\n"
        +" 246:     -2.4232909896396002 -2.4232909896396002 1.2791681840042317 0.57206140281768414\n"
        +" 247:     -2.4232909896396002 -2.4232909896396002 0.57206140281768414 1.2791681840042317\n"
        +" 248:     -1.851229586821916 -2.9953523924572845 0 1.1441228056353683\n"
        +" 249:     -2.0697375990463267 -2.7768443802328737 0.92561479341095798 -0.92561479341095798\n"
        +" 250:     -2.7768443802328737 -2.0697375990463267 0.92561479341095798 -0.92561479341095798\n"
        +" 251:     -2.9953523924572845 -1.851229586821916 1.1441228056353683 0\n"
        +" 252:     -2.0697375990463267 -2.7768443802328737 -0.92561479341095798 0.92561479341095798\n"
        +" 253:     -2.7768443802328737 -2.0697375990463267 -0.92561479341095798 0.92561479341095798\n"
        +" 254:     -2.9953523924572845 -1.851229586821916 0 1.1441228056353683\n"
        +" 255:     -3.3489057830505584 -1.4976761962286422 0.35355339059327373 0.35355339059327373\n"
        +" 256:     -2.0697375990463267 -2.0697375990463267 2.0697375990463267 0.92561479341095798\n"
        +" 257:     -2.0697375990463267 -2.0697375990463267 0.92561479341095798 2.0697375990463267\n"
        +" 258:     -1.851229586821916 -1.851229586821916 1.851229586821916 1.851229586821916\n"
        +" 259:     -2.9953523924572845 -1.1441228056353683 0 1.851229586821916\n"
        +" 260:     -2.4232909896396002 -1.2791681840042317 0.57206140281768414 2.4232909896396002\n"
        +" 261:     -3.3489057830505584 -0.35355339059327373 0.35355339059327373 1.4976761962286422\n"
        +" 262:     -3.5674137952749687 -0.57206140281768414 0.57206140281768414 0.57206140281768414\n"
        +" 263:     -2.4232909896396002 -0.57206140281768414 1.2791681840042317 2.4232909896396002\n"
        +" 264:     -2.9953523924572845 0 1.1441228056353683 1.851229586821916\n"
        +" 265:     -2.0697375990463267 -0.92561479341095798 2.0697375990463267 2.0697375990463267\n"
        +" 266:     -2.9953523924572845 0 1.851229586821916 1.1441228056353683\n"
        +" 267:     -2.4232909896396002 -0.57206140281768414 2.4232909896396002 1.2791681840042317\n"
        +" 268:     -3.3489057830505584 -0.35355339059327373 1.4976761962286422 0.35355339059327373\n"
        +" 269:     -2.9953523924572845 -1.1441228056353683 1.851229586821916 0\n"
        +" 270:     -2.4232909896396002 -1.2791681840042317 2.4232909896396002 0.57206140281768414\n"
        +" 271:     -0.92561479341095798 -2.0697375990463267 2.0697375990463267 2.0697375990463267\n"
        +" 272:     -0.57206140281768414 -1.2791681840042317 2.4232909896396002 2.4232909896396002\n"
        +" 273:     0 -1.1441228056353683 2.9953523924572845 1.851229586821916\n"
        +" 274:     0 -1.851229586821916 2.9953523924572845 1.1441228056353683\n"
        +" 275:     -0.57206140281768414 -2.4232909896396002 2.4232909896396002 1.2791681840042317\n"
        +" 276:     -1.2791681840042317 -0.57206140281768414 2.4232909896396002 2.4232909896396002\n"
        +" 277:     -1.1441228056353683 0 2.9953523924572845 1.851229586821916\n"
        +" 278:     -0.35355339059327373 -0.35355339059327373 3.3489057830505584 1.4976761962286422\n"
        +" 279:     -1.851229586821916 0 2.9953523924572845 1.1441228056353683\n"
        +" 280:     -1.4976761962286422 -0.35355339059327373 3.3489057830505584 0.35355339059327373\n"
        +" 281:     -0.57206140281768414 -0.57206140281768414 3.5674137952749687 0.57206140281768414\n"
        +" 282:     -1.851229586821916 -1.1441228056353683 2.9953523924572845 0\n"
        +" 283:     -1.2791681840042317 -2.4232909896396002 2.4232909896396002 0.57206140281768414\n"
        +" 284:     -0.35355339059327373 -1.4976761962286422 3.3489057830505584 0.35355339059327373\n"
        +" 285:     -1.1441228056353683 -1.851229586821916 2.9953523924572845 0\n"
        +" 286:     -1.1441228056353683 -2.9953523924572845 1.851229586821916 0\n"
        +" 287:     -0.92561479341095798 -2.0697375990463267 2.7768443802328737 -0.92561479341095798\n"
        +" 288:     -0.92561479341095798 -2.7768443802328737 2.0697375990463267 -0.92561479341095798\n"
        +" 289:     -2.0697375990463267 -0.92561479341095798 2.7768443802328737 -0.92561479341095798\n"
        +" 290:     -1.4976761962286422 -1.4976761962286422 2.641799001864011 -1.4976761962286422\n"
        +" 291:     -1.4976761962286422 -2.641799001864011 1.4976761962286422 -1.4976761962286422\n"
        +" 292:     -1.851229586821916 -1.851229586821916 1.851229586821916 -1.851229586821916\n"
        +" 293:     -2.7768443802328737 -0.92561479341095798 2.0697375990463267 -0.92561479341095798\n"
        +" 294:     -2.641799001864011 -1.4976761962286422 1.4976761962286422 -1.4976761962286422\n"
        +" 295:     -2.7768443802328737 -0.92561479341095798 0.92561479341095798 -2.0697375990463267\n"
        +" 296:     -0.92561479341095798 -2.7768443802328737 0.92561479341095798 -2.0697375990463267\n"
        +" 297:     0 -2.9953523924572845 1.1441228056353683 -1.851229586821916\n"
        +" 298:     0.92561479341095798 -2.7768443802328737 -0.92561479341095798 -2.0697375990463267\n"
        +" 299:     0.35355339059327373 -3.3489057830505584 0.35355339059327373 -1.4976761962286422\n"
        +" 300:     1.1441228056353683 -2.9953523924572845 0 -1.851229586821916\n"
        +" 301:     1.4976761962286422 -2.641799001864011 -1.4976761962286422 -1.4976761962286422\n"
        +" 302:     1.851229586821916 -1.851229586821916 -1.851229586821916 -1.851229586821916\n"
        +" 303:     2.641799001864011 -1.4976761962286422 -1.4976761962286422 -1.4976761962286422\n"
        +" 304:     2.7768443802328737 -2.0697375990463267 -0.92561479341095798 -0.92561479341095798\n"
        +" 305:     2.0697375990463267 -2.7768443802328737 -0.92561479341095798 -0.92561479341095798\n"
        +" 306:     2.7768443802328737 -0.92561479341095798 -0.92561479341095798 -2.0697375990463267\n"
        +" 307:     1.851229586821916 -2.9953523924572845 0 -1.1441228056353683\n"
        +" 308:     2.9953523924572845 -1.851229586821916 -1.1441228056353683 0\n"
        +" 309:     2.9953523924572845 -1.1441228056353683 -1.851229586821916 0\n"
        +" 310:     2.7768443802328737 -0.92561479341095798 -2.0697375990463267 -0.92561479341095798\n"
        +" 311:     2.9953523924572845 0 -1.851229586821916 -1.1441228056353683\n"
        +" 312:     2.9953523924572845 0 -1.1441228056353683 -1.851229586821916\n"
        +" 313:     2.4232909896396002 -2.4232909896396002 -0.57206140281768414 1.2791681840042317\n"
        +" 314:     1.851229586821916 -2.9953523924572845 0 1.1441228056353683\n"
        +" 315:     2.0697375990463267 -2.7768443802328737 0.92561479341095798 0.92561479341095798\n"
        +" 316:     1.4976761962286422 -3.3489057830505584 -0.35355339059327373 0.35355339059327373\n"
        +" 317:     1.4976761962286422 -3.3489057830505584 0.35355339059327373 -0.35355339059327373\n"
        +" 318:     1.851229586821916 -2.9953523924572845 1.1441228056353683 0\n"
        +" 319:     1.851229586821916 -2.9953523924572845 -1.1441228056353683 0\n"
        +" 320:     2.4232909896396002 -2.4232909896396002 -1.2791681840042317 0.57206140281768414\n"
        +" 321:     1.2791681840042317 -2.4232909896396002 -2.4232909896396002 0.57206140281768414\n"
        +" 322:     1.1441228056353683 -1.851229586821916 -2.9953523924572845 0\n"
        +" 323:     1.851229586821916 -1.1441228056353683 -2.9953523924572845 0\n"
        +" 324:     2.4232909896396002 -1.2791681840042317 -2.4232909896396002 0.57206140281768414\n"
        +" 325:     2.0697375990463267 -2.0697375990463267 -2.0697375990463267 0.92561479341095798\n"
        +" 326:     0.92561479341095798 -2.0697375990463267 -2.7768443802328737 -0.92561479341095798\n"
        +" 327:     1.4976761962286422 -1.4976761962286422 -2.641799001864011 -1.4976761962286422\n"
        +" 328:     2.0697375990463267 -0.92561479341095798 -2.7768443802328737 -0.92561479341095798\n"
        +" 329:     0.92561479341095798 -2.7768443802328737 -2.0697375990463267 -0.92561479341095798\n"
        +" 330:     1.1441228056353683 -2.9953523924572845 -1.851229586821916 0\n"
        +" 331:     1.851229586821916 0 -2.9953523924572845 -1.1441228056353683\n"
        +" 332:     2.4232909896396002 0.57206140281768414 -2.4232909896396002 -1.2791681840042317\n"
        +" 333:     1.1441228056353683 0 -2.9953523924572845 -1.851229586821916\n"
        +" 334:     0.92561479341095798 -0.92561479341095798 -2.7768443802328737 -2.0697375990463267\n"
        +" 335:     1.2791681840042317 0.57206140281768414 -2.4232909896396002 -2.4232909896396002\n"
        +" 336:     2.0697375990463267 0.92561479341095798 -2.0697375990463267 -2.0697375990463267\n"
        +" 337:     2.4232909896396002 0.57206140281768414 -1.2791681840042317 -2.4232909896396002\n"
        +" 338:     1.851229586821916 1.851229586821916 -1.851229586821916 -1.851229586821916\n"
        +" 339:     2.4232909896396002 2.4232909896396002 -0.57206140281768414 -1.2791681840042317\n"
        +" 340:     2.0697375990463267 2.0697375990463267 -0.92561479341095798 -2.0697375990463267\n"
        +" 341:     2.4232909896396002 1.2791681840042317 -0.57206140281768414 -2.4232909896396002\n"
        +" 342:     0 1.1441228056353683 -1.851229586821916 -2.9953523924572845\n"
        +" 343:     0.57206140281768414 1.2791681840042317 -2.4232909896396002 -2.4232909896396002\n"
        +" 344:     0 1.851229586821916 -1.1441228056353683 -2.9953523924572845\n"
        +" 345:     0.35355339059327373 1.4976761962286422 -0.35355339059327373 -3.3489057830505584\n"
        +" 346:     0.57206140281768414 2.4232909896396002 -1.2791681840042317 -2.4232909896396002\n"
        +" 347:     0.92561479341095798 2.0697375990463267 -2.0697375990463267 -2.0697375990463267\n"
        +" 348:     1.2791681840042317 2.4232909896396002 -0.57206140281768414 -2.4232909896396002\n"
        +" 349:     1.1441228056353683 1.851229586821916 0 -2.9953523924572845\n"
        +" 350:     1.851229586821916 1.1441228056353683 0 -2.9953523924572845\n"
        +" 351:     0.92561479341095798 2.0697375990463267 0.92561479341095798 -2.7768443802328737\n"
        +" 352:     2.0697375990463267 0.92561479341095798 0.92561479341095798 -2.7768443802328737\n"
        +" 353:     1.4976761962286422 1.4976761962286422 1.4976761962286422 -2.641799001864011\n"
        +" 354:     0 1.851229586821916 1.1441228056353683 -2.9953523924572845\n"
        +" 355:     -0.57206140281768414 2.4232909896396002 1.2791681840042317 -2.4232909896396002\n"
        +" 356:     0 1.1441228056353683 1.851229586821916 -2.9953523924572845\n"
        +" 357:     0.92561479341095798 0.92561479341095798 2.0697375990463267 -2.7768443802328737\n"
        +" 358:     -0.57206140281768414 1.2791681840042317 2.4232909896396002 -2.4232909896396002\n"
        +" 359:     -0.92561479341095798 2.0697375990463267 2.0697375990463267 -2.0697375990463267\n"
        +" 360:     0.92561479341095798 0.92561479341095798 2.7768443802328737 -2.0697375990463267\n"
        +" 361:     0 1.1441228056353683 2.9953523924572845 -1.851229586821916\n"
        +" 362:     0 1.851229586821916 2.9953523924572845 -1.1441228056353683\n"
        +" 363:     -0.57206140281768414 2.4232909896396002 2.4232909896396002 -1.2791681840042317\n"
        +" 364:     -1.1441228056353683 2.9953523924572845 0 -1.851229586821916\n"
        +" 365:     -1.2791681840042317 2.4232909896396002 0.57206140281768414 -2.4232909896396002\n"
        +" 366:     -2.0697375990463267 2.0697375990463267 0.92561479341095798 -2.0697375990463267\n"
        +" 367:     -2.4232909896396002 2.4232909896396002 0.57206140281768414 -1.2791681840042317\n"
        +" 368:     -1.851229586821916 2.9953523924572845 0 -1.1441228056353683\n"
        +" 369:     -1.4976761962286422 3.3489057830505584 0.35355339059327373 -0.35355339059327373\n"
        +" 370:     -1.851229586821916 2.9953523924572845 1.1441228056353683 0\n"
        +" 371:     -2.4232909896396002 2.4232909896396002 1.2791681840042317 -0.57206140281768414\n"
        +" 372:     -1.1441228056353683 2.9953523924572845 1.851229586821916 0\n"
        +" 373:     -1.2791681840042317 2.4232909896396002 2.4232909896396002 -0.57206140281768414\n"
        +" 374:     -2.0697375990463267 2.0697375990463267 2.0697375990463267 -0.92561479341095798\n"
        +" 375:     -1.851229586821916 1.851229586821916 1.851229586821916 -1.851229586821916\n"
        +" 376:     -0.35355339059327373 1.4976761962286422 0.35355339059327373 -3.3489057830505584\n"
        +" 377:     -1.1441228056353683 1.851229586821916 0 -2.9953523924572845\n"
        +" 378:     -1.851229586821916 1.1441228056353683 0 -2.9953523924572845\n"
        +" 379:     -2.4232909896396002 1.2791681840042317 0.57206140281768414 -2.4232909896396002\n"
        +" 380:     -2.4232909896396002 0.57206140281768414 1.2791681840042317 -2.4232909896396002\n"
        +" 381:     -2.0697375990463267 0.92561479341095798 2.0697375990463267 -2.0697375990463267\n"
        +" 382:     -1.2791681840042317 0.57206140281768414 2.4232909896396002 -2.4232909896396002\n"
        +" 383:     -0.35355339059327373 0.35355339059327373 3.3489057830505584 -1.4976761962286422\n"
        +" 384:     -1.1441228056353683 0 2.9953523924572845 -1.851229586821916\n"
        +" 385:     -0.35355339059327373 1.4976761962286422 3.3489057830505584 -0.35355339059327373\n"
        +" 386:     -0.57206140281768414 0.57206140281768414 3.5674137952749687 -0.57206140281768414\n"
        +" 387:     -1.1441228056353683 1.851229586821916 2.9953523924572845 0\n"
        +" 388:     -1.4976761962286422 0.35355339059327373 3.3489057830505584 -0.35355339059327373\n"
        +" 389:     -1.851229586821916 1.1441228056353683 2.9953523924572845 0\n"
        +" 390:     -2.4232909896396002 1.2791681840042317 2.4232909896396002 -0.57206140281768414\n"
        +" 391:     -2.4232909896396002 0.57206140281768414 2.4232909896396002 -1.2791681840042317\n"
        +" 392:     -1.851229586821916 0 2.9953523924572845 -1.1441228056353683\n"
        +" 393:     -2.9953523924572845 0 1.851229586821916 -1.1441228056353683\n"
        +" 394:     -3.3489057830505584 0.35355339059327373 1.4976761962286422 -0.35355339059327373\n"
        +" 395:     -2.0697375990463267 0.92561479341095798 2.7768443802328737 0.92561479341095798\n"
        +" 396:     -2.7768443802328737 0.92561479341095798 2.0697375990463267 0.92561479341095798\n"
        +" 397:     -2.9953523924572845 1.1441228056353683 1.851229586821916 0\n"
        +" 398:     -0.92561479341095798 2.7768443802328737 2.0697375990463267 0.92561479341095798\n"
        +" 399:     -0.92561479341095798 2.0697375990463267 2.7768443802328737 0.92561479341095798\n"
        +" 400:     -1.4976761962286422 1.4976761962286422 2.641799001864011 1.4976761962286422\n"
        +" 401:     -2.9953523924572845 1.851229586821916 1.1441228056353683 0\n"
        +" 402:     -2.0697375990463267 2.7768443802328737 0.92561479341095798 0.92561479341095798\n"
        +" 403:     -2.7768443802328737 2.0697375990463267 0.92561479341095798 0.92561479341095798\n"
        +" 404:     -1.4976761962286422 2.641799001864011 1.4976761962286422 1.4976761962286422\n"
        +" 405:     -1.851229586821916 1.851229586821916 1.851229586821916 1.851229586821916\n"
        +" 406:     -2.641799001864011 1.4976761962286422 1.4976761962286422 1.4976761962286422\n"
        +" 407:     -2.9953523924572845 1.851229586821916 0 1.1441228056353683\n"
        +" 408:     -2.9953523924572845 1.1441228056353683 0 1.851229586821916\n"
        +" 409:     -2.7768443802328737 0.92561479341095798 0.92561479341095798 2.0697375990463267\n"
        +" 410:     -1.851229586821916 2.9953523924572845 0 1.1441228056353683\n"
        +" 411:     -2.4232909896396002 2.4232909896396002 -0.57206140281768414 1.2791681840042317\n"
        +" 412:     -0.92561479341095798 2.7768443802328737 0.92561479341095798 2.0697375990463267\n"
        +" 413:     -1.1441228056353683 2.9953523924572845 0 1.851229586821916\n"
        +" 414:     -1.2791681840042317 2.4232909896396002 -0.57206140281768414 2.4232909896396002\n"
        +" 415:     -2.0697375990463267 2.0697375990463267 -0.92561479341095798 2.0697375990463267\n"
        +" 416:     -2.4232909896396002 1.2791681840042317 -0.57206140281768414 2.4232909896396002\n"
        +" 417:     -1.851229586821916 1.1441228056353683 0 2.9953523924572845\n"
        +" 418:     -1.1441228056353683 1.851229586821916 0 2.9953523924572845\n"
        +" 419:     -0.92561479341095798 2.0697375990463267 0.92561479341095798 2.7768443802328737\n"
        +" 420:     -1.4976761962286422 1.4976761962286422 1.4976761962286422 2.641799001864011\n"
        +" 421:     -2.0697375990463267 0.92561479341095798 0.92561479341095798 2.7768443802328737\n"
        +" 422:     -0.92561479341095798 0.92561479341095798 2.7768443802328737 2.0697375990463267\n"
        +" 423:     -0.92561479341095798 0.92561479341095798 2.0697375990463267 2.7768443802328737\n"
        +" 424:     0 1.1441228056353683 1.851229586821916 2.9953523924572845\n"
        +" 425:     0 1.851229586821916 1.1441228056353683 2.9953523924572845\n"
        +" 426:     -1.4976761962286422 3.3489057830505584 -0.35355339059327373 0.35355339059327373\n"
        +" 427:     0 2.9953523924572845 -1.1441228056353683 -1.851229586821916\n"
        +" 428:     -0.92561479341095798 2.7768443802328737 -0.92561479341095798 -2.0697375990463267\n"
        +" 429:     0 2.9953523924572845 -1.851229586821916 -1.1441228056353683\n"
        +" 430:     -0.92561479341095798 2.7768443802328737 -2.0697375990463267 -0.92561479341095798\n"
        +" 431:     -1.4976761962286422 2.641799001864011 -1.4976761962286422 -1.4976761962286422\n"
        +" 432:     -1.1441228056353683 2.9953523924572845 -1.851229586821916 0\n"
        +" 433:     -2.0697375990463267 2.7768443802328737 -0.92561479341095798 -0.92561479341095798\n"
        +" 434:     -1.851229586821916 2.9953523924572845 -1.1441228056353683 0\n"
        +" 435:     -1.2791681840042317 2.4232909896396002 -2.4232909896396002 0.57206140281768414\n"
        +" 436:     -2.0697375990463267 2.0697375990463267 -2.0697375990463267 0.92561479341095798\n"
        +" 437:     -2.4232909896396002 2.4232909896396002 -1.2791681840042317 0.57206140281768414\n"
        +" 438:     -1.851229586821916 1.851229586821916 -1.851229586821916 1.851229586821916\n"
        +" 439:     -0.35355339059327373 1.4976761962286422 -0.35355339059327373 3.3489057830505584\n"
        +" 440:     -1.4976761962286422 0.35355339059327373 -0.35355339059327373 3.3489057830505584\n"
        +" 441:     -0.57206140281768414 0.57206140281768414 -0.57206140281768414 3.5674137952749687\n"
        +" 442:     -2.4232909896396002 0.57206140281768414 -1.2791681840042317 2.4232909896396002\n"
        +" 443:     -2.0697375990463267 0.92561479341095798 -2.0697375990463267 2.0697375990463267\n"
        +" 444:     -1.2791681840042317 0.57206140281768414 -2.4232909896396002 2.4232909896396002\n"
        +" 445:     -1.1441228056353683 0 -1.851229586821916 2.9953523924572845\n"
        +" 446:     -1.851229586821916 0 -1.1441228056353683 2.9953523924572845\n"
        +" 447:     -0.35355339059327373 0.35355339059327373 -1.4976761962286422 3.3489057830505584\n"
        +" 448:     -1.1441228056353683 1.851229586821916 -2.9953523924572845 0\n"
        +" 449:     -1.851229586821916 1.1441228056353683 -2.9953523924572845 0\n"
        +" 450:     -2.4232909896396002 1.2791681840042317 -2.4232909896396002 0.57206140281768414\n"
        +" 451:     -0.35355339059327373 1.4976761962286422 -3.3489057830505584 0.35355339059327373\n"
        +" 452:     -0.57206140281768414 0.57206140281768414 -3.5674137952749687 0.57206140281768414\n"
        +" 453:     -1.4976761962286422 0.35355339059327373 -3.3489057830505584 0.35355339059327373\n"
        +" 454:     -0.35355339059327373 0.35355339059327373 -3.3489057830505584 1.4976761962286422\n"
        +" 455:     -1.1441228056353683 0 -2.9953523924572845 1.851229586821916\n"
        +" 456:     -1.851229586821916 0 -2.9953523924572845 1.1441228056353683\n"
        +" 457:     -2.4232909896396002 0.57206140281768414 -2.4232909896396002 1.2791681840042317\n"
        +" 458:     -2.9953523924572845 1.1441228056353683 -1.851229586821916 0\n"
        +" 459:     -2.9953523924572845 1.851229586821916 -1.1441228056353683 0\n"
        +" 460:     -3.3489057830505584 0.35355339059327373 -1.4976761962286422 0.35355339059327373\n"
        +" 461:     -2.9953523924572845 0 -1.851229586821916 1.1441228056353683\n"
        +" 462:     -3.5674137952749687 0.57206140281768414 -0.57206140281768414 0.57206140281768414\n"
        +" 463:     -3.3489057830505584 1.4976761962286422 -0.35355339059327373 0.35355339059327373\n"
        +" 464:     -2.9953523924572845 0 -1.1441228056353683 1.851229586821916\n"
        +" 465:     -3.3489057830505584 0.35355339059327373 -0.35355339059327373 1.4976761962286422\n"
        +" 466:     -3.5674137952749687 0.57206140281768414 0.57206140281768414 -0.57206140281768414\n"
        +" 467:     -3.7024591736438319 0 0 0\n"
        +" 468:     -3.3489057830505584 1.4976761962286422 0.35355339059327373 -0.35355339059327373\n"
        +" 469:     -1.851229586821916 0 1.1441228056353683 2.9953523924572845\n"
        +" 470:     -1.1441228056353683 0 1.851229586821916 2.9953523924572845\n"
        +" 471:     0.35355339059327373 0.35355339059327373 1.4976761962286422 3.3489057830505584\n"
        +" 472:     -0.35355339059327373 -0.35355339059327373 1.4976761962286422 3.3489057830505584\n"
        +" 473:     1.1441228056353683 0 1.851229586821916 2.9953523924572845\n"
        +" 474:     0 -1.1441228056353683 1.851229586821916 2.9953523924572845\n"
        +" 475:     0.92561479341095798 -0.92561479341095798 2.0697375990463267 2.7768443802328737\n"
        +" 476:     0.92561479341095798 -0.92561479341095798 2.7768443802328737 2.0697375990463267\n"
        +" 477:     -0.57206140281768414 -2.4232909896396002 1.2791681840042317 2.4232909896396002\n"
        +" 478:     0 -1.851229586821916 1.1441228056353683 2.9953523924572845\n"
        +" 479:     -0.35355339059327373 -1.4976761962286422 0.35355339059327373 3.3489057830505584\n"
        +" 480:     -0.57206140281768414 -0.57206140281768414 0.57206140281768414 3.5674137952749687\n"
        +" 481:     -1.1441228056353683 -1.851229586821916 0 2.9953523924572845\n"
        +" 482:     -1.2791681840042317 -2.4232909896396002 0.57206140281768414 2.4232909896396002\n"
        +" 483:     -1.851229586821916 -1.1441228056353683 0 2.9953523924572845\n"
        +" 484:     -1.4976761962286422 -0.35355339059327373 0.35355339059327373 3.3489057830505584\n"
        +" 485:     0 -1.851229586821916 -1.1441228056353683 2.9953523924572845\n"
        +" 486:     0 -1.1441228056353683 -1.851229586821916 2.9953523924572845\n"
        +" 487:     -0.92561479341095798 -0.92561479341095798 -2.0697375990463267 2.7768443802328737\n"
        +" 488:     -1.4976761962286422 -1.4976761962286422 -1.4976761962286422 2.641799001864011\n"
        +" 489:     -0.92561479341095798 -2.0697375990463267 -0.92561479341095798 2.7768443802328737\n"
        +" 490:     0.35355339059327373 -0.35355339059327373 -1.4976761962286422 3.3489057830505584\n"
        +" 491:     0.35355339059327373 -1.4976761962286422 -0.35355339059327373 3.3489057830505584\n"
        +" 492:     0.57206140281768414 -0.57206140281768414 -0.57206140281768414 3.5674137952749687\n"
        +" 493:     0 0 0 3.7024591736438319\n"
        +" 494:     -2.0697375990463267 -0.92561479341095798 -0.92561479341095798 2.7768443802328737\n"
        +" 495:     2.0697375990463267 -0.92561479341095798 0.92561479341095798 2.7768443802328737\n"
        +" 496:     1.851229586821916 0 1.1441228056353683 2.9953523924572845\n"
        +" 497:     1.4976761962286422 0.35355339059327373 0.35355339059327373 3.3489057830505584\n"
        +" 498:     1.4976761962286422 -0.35355339059327373 -0.35355339059327373 3.3489057830505584\n"
        +" 499:     1.851229586821916 -1.1441228056353683 0 2.9953523924572845\n"
        +" 500:     0.57206140281768414 0.57206140281768414 0.57206140281768414 3.5674137952749687\n"
        +" 501:     1.4976761962286422 -1.4976761962286422 1.4976761962286422 2.641799001864011\n"
        +" 502:     1.1441228056353683 -1.851229586821916 0 2.9953523924572845\n"
        +" 503:     0.92561479341095798 -2.0697375990463267 0.92561479341095798 2.7768443802328737\n"
        +" 504:     1.851229586821916 0 -1.1441228056353683 2.9953523924572845\n"
        +" 505:     1.1441228056353683 0 -1.851229586821916 2.9953523924572845\n"
        +" 506:     0.35355339059327373 1.4976761962286422 0.35355339059327373 3.3489057830505584\n"
        +" 507:     2.4232909896396002 0.57206140281768414 1.2791681840042317 2.4232909896396002\n"
        +" 508:     -2.7768443802328737 -0.92561479341095798 -0.92561479341095798 2.0697375990463267\n"
        +" 509:     -2.641799001864011 -1.4976761962286422 -1.4976761962286422 1.4976761962286422\n"
        +" 510:     -2.7768443802328737 -0.92561479341095798 -2.0697375990463267 0.92561479341095798\n"
        +" 511:     -0.92561479341095798 -0.92561479341095798 -2.7768443802328737 2.0697375990463267\n"
        +" 512:     -1.4976761962286422 -1.4976761962286422 -2.641799001864011 1.4976761962286422\n"
        +" 513:     -1.851229586821916 -1.851229586821916 -1.851229586821916 1.851229586821916\n"
        +" 514:     -2.0697375990463267 -0.92561479341095798 -2.7768443802328737 0.92561479341095798\n"
        +" 515:     -0.92561479341095798 -2.7768443802328737 -2.0697375990463267 0.92561479341095798\n"
        +" 516:     -1.4976761962286422 -2.641799001864011 -1.4976761962286422 1.4976761962286422\n"
        +" 517:     -1.1441228056353683 -1.851229586821916 -2.9953523924572845 0\n"
        +" 518:     -0.92561479341095798 -2.0697375990463267 -2.7768443802328737 0.92561479341095798\n"
        +" 519:     -1.851229586821916 -1.1441228056353683 -2.9953523924572845 0\n"
        +" 520:     -1.1441228056353683 -2.9953523924572845 0 1.851229586821916\n"
        +" 521:     -0.92561479341095798 -2.7768443802328737 -0.92561479341095798 2.0697375990463267\n"
        +" 522:     0 -2.9953523924572845 -1.1441228056353683 1.851229586821916\n"
        +" 523:     0 -2.9953523924572845 -1.851229586821916 1.1441228056353683\n"
        +" 524:     0.57206140281768414 -2.4232909896396002 -1.2791681840042317 2.4232909896396002\n"
        +" 525:     0.92561479341095798 -2.0697375990463267 -2.0697375990463267 2.0697375990463267\n"
        +" 526:     0.57206140281768414 -2.4232909896396002 -2.4232909896396002 1.2791681840042317\n"
        +" 527:     0.57206140281768414 -1.2791681840042317 -2.4232909896396002 2.4232909896396002\n"
        +" 528:     0 -1.1441228056353683 -2.9953523924572845 1.851229586821916\n"
        +" 529:     0 -1.851229586821916 -2.9953523924572845 1.1441228056353683\n"
        +" 530:     2.4232909896396002 -1.2791681840042317 -0.57206140281768414 2.4232909896396002\n"
        +" 531:     2.4232909896396002 -0.57206140281768414 -1.2791681840042317 2.4232909896396002\n"
        +" 532:     1.2791681840042317 -2.4232909896396002 -0.57206140281768414 2.4232909896396002\n"
        +" 533:     2.0697375990463267 -2.0697375990463267 -0.92561479341095798 2.0697375990463267\n"
        +" 534:     1.2791681840042317 -0.57206140281768414 -2.4232909896396002 2.4232909896396002\n"
        +" 535:     1.851229586821916 -1.851229586821916 -1.851229586821916 1.851229586821916\n"
        +" 536:     2.0697375990463267 -0.92561479341095798 -2.0697375990463267 2.0697375990463267\n"
        +" 537:     1.1441228056353683 0 -2.9953523924572845 1.851229586821916\n"
        +" 538:     2.4232909896396002 -0.57206140281768414 -2.4232909896396002 1.2791681840042317\n"
        +" 539:     1.851229586821916 0 -2.9953523924572845 1.1441228056353683\n"
        +" 540:     0.35355339059327373 -1.4976761962286422 -3.3489057830505584 0.35355339059327373\n"
        +" 541:     0.35355339059327373 -0.35355339059327373 -3.3489057830505584 1.4976761962286422\n"
        +" 542:     0.57206140281768414 -0.57206140281768414 -3.5674137952749687 0.57206140281768414\n"
        +" 543:     1.4976761962286422 -0.35355339059327373 -3.3489057830505584 0.35355339059327373\n"
        +" 544:     1.1441228056353683 -2.9953523924572845 0 1.851229586821916\n"
        +" 545:     0.35355339059327373 -3.3489057830505584 -0.35355339059327373 1.4976761962286422\n"
        +" 546:     0.57206140281768414 -3.5674137952749687 -0.57206140281768414 0.57206140281768414\n"
        +" 547:     0.35355339059327373 -3.3489057830505584 -1.4976761962286422 0.35355339059327373\n"
        +" 548:     0.92561479341095798 -2.7768443802328737 0.92561479341095798 2.0697375990463267\n"
        +" 549:     1.4976761962286422 -2.641799001864011 1.4976761962286422 1.4976761962286422\n"
        +" 550:     1.851229586821916 -1.851229586821916 1.851229586821916 1.851229586821916\n"
        +" 551:     1.4976761962286422 -1.4976761962286422 2.641799001864011 1.4976761962286422\n"
        +" 552:     2.0697375990463267 -0.92561479341095798 2.7768443802328737 0.92561479341095798\n"
        +" 553:     1.1441228056353683 -2.9953523924572845 1.851229586821916 0\n"
        +" 554:     0.92561479341095798 -2.7768443802328737 2.0697375990463267 0.92561479341095798\n"
        +" 555:     1.2791681840042317 -2.4232909896396002 2.4232909896396002 -0.57206140281768414\n"
        +" 556:     1.1441228056353683 -1.851229586821916 2.9953523924572845 0\n"
        +" 557:     0.92561479341095798 -2.0697375990463267 2.7768443802328737 0.92561479341095798\n"
        +" 558:     1.851229586821916 -1.1441228056353683 2.9953523924572845 0\n"
        +" 559:     0 -2.9953523924572845 1.1441228056353683 1.851229586821916\n"
        +" 560:     0 -2.9953523924572845 1.851229586821916 1.1441228056353683\n"
        +" 561:     -0.35355339059327373 -3.3489057830505584 0.35355339059327373 1.4976761962286422\n"
        +" 562:     -0.57206140281768414 -3.5674137952749687 0.57206140281768414 0.57206140281768414\n"
        +" 563:     0 -3.7024591736438319 0 0\n"
        +" 564:     -0.35355339059327373 -3.3489057830505584 1.4976761962286422 0.35355339059327373\n"
        +" 565:     0.35355339059327373 -3.3489057830505584 1.4976761962286422 -0.35355339059327373\n"
        +" 566:     0.57206140281768414 -3.5674137952749687 0.57206140281768414 -0.57206140281768414\n"
        +" 567:     0 -2.9953523924572845 1.851229586821916 -1.1441228056353683\n"
        +" 568:     0.57206140281768414 -2.4232909896396002 2.4232909896396002 -1.2791681840042317\n"
        +" 569:     0.35355339059327373 -1.4976761962286422 3.3489057830505584 -0.35355339059327373\n"
        +" 570:     0 -1.851229586821916 2.9953523924572845 -1.1441228056353683\n"
        +" 571:     0 -1.1441228056353683 2.9953523924572845 -1.851229586821916\n"
        +" 572:     0.35355339059327373 -0.35355339059327373 3.3489057830505584 -1.4976761962286422\n"
        +" 573:     0.57206140281768414 -0.57206140281768414 3.5674137952749687 -0.57206140281768414\n"
        +" 574:     1.4976761962286422 -0.35355339059327373 3.3489057830505584 -0.35355339059327373\n"
        +" 575:     1.851229586821916 0 2.9953523924572845 -1.1441228056353683\n"
        +" 576:     1.1441228056353683 0 2.9953523924572845 -1.851229586821916\n"
        +" 577:     0 0 3.7024591736438319 0\n"
        +" 578:     0.57206140281768414 2.4232909896396002 -2.4232909896396002 -1.2791681840042317\n"
        +" 579:     -0.92561479341095798 2.0697375990463267 -0.92561479341095798 -2.7768443802328737\n"
        +" 580:     -0.92561479341095798 -0.92561479341095798 2.7768443802328737 -2.0697375990463267\n"
        +" 581:     -0.35355339059327373 -1.4976761962286422 -3.3489057830505584 -0.35355339059327373\n"
        +" 582:     0 -1.851229586821916 -2.9953523924572845 -1.1441228056353683\n"
        +" 583:     -0.35355339059327373 -0.35355339059327373 -3.3489057830505584 -1.4976761962286422\n"
        +" 584:     -1.4976761962286422 -0.35355339059327373 -3.3489057830505584 -0.35355339059327373\n"
        +" 585:     -0.57206140281768414 -0.57206140281768414 -3.5674137952749687 -0.57206140281768414\n"
        +" 586:     0 -1.1441228056353683 -2.9953523924572845 -1.851229586821916\n"
        +" 587:     -2.7768443802328737 2.0697375990463267 -0.92561479341095798 -0.92561479341095798\n"
        +" 588:     -2.9953523924572845 1.1441228056353683 0 -1.851229586821916\n"
        +" 589:     -3.3489057830505584 0.35355339059327373 0.35355339059327373 -1.4976761962286422\n"
        +" 590:     -2.9953523924572845 1.851229586821916 0 -1.1441228056353683\n"
        +" 591:     -2.9953523924572845 0 1.1441228056353683 -1.851229586821916\n"
        +" 592:     0 1.1441228056353683 -2.9953523924572845 -1.851229586821916\n"
        +" 593:     0 1.851229586821916 -2.9953523924572845 -1.1441228056353683\n"
        +" 594:     -0.92561479341095798 2.0697375990463267 -2.7768443802328737 -0.92561479341095798\n"
        +" 595:     0.35355339059327373 1.4976761962286422 -3.3489057830505584 -0.35355339059327373\n"
        +" 596:     0.35355339059327373 0.35355339059327373 -3.3489057830505584 -1.4976761962286422\n"
        +" 597:     0 0 -3.7024591736438319 0\n"
        +" 598:     0.57206140281768414 0.57206140281768414 -3.5674137952749687 -0.57206140281768414\n"
        +" 599:     1.4976761962286422 0.35355339059327373 -3.3489057830505584 -0.35355339059327373\n"
        +"1200 1-cells:\n"
        +"    0:    -0   +1   \n"
        +"    1:    -2   +1   \n"
        +"    2:    -2   +3   \n"
        +"    3:    -4   +3   \n"
        +"    4:    -0   +4   \n"
        +"    5:    -5   +1   \n"
        +"    6:    -5   +6   \n"
        +"    7:    -6   +7   \n"
        +"    8:    -2   +7   \n"
        +"    9:    -5   +8   \n"
        +"   10:    -9   +8   \n"
        +"   11:    -0   +9   \n"
        +"   12:    -6   +10  \n"
        +"   13:    -11  +10  \n"
        +"   14:    -8   +11  \n"
        +"   15:    -7   +12  \n"
        +"   16:    -13  +12  \n"
        +"   17:    -13  +10  \n"
        +"   18:    -3   +14  \n"
        +"   19:    -14  +12  \n"
        +"   20:    -15  +14  \n"
        +"   21:    -15  +16  \n"
        +"   22:    -4   +16  \n"
        +"   23:    -17  +13  \n"
        +"   24:    -17  +15  \n"
        +"   25:    -17  +18  \n"
        +"   26:    -18  +19  \n"
        +"   27:    -19  +16  \n"
        +"   28:    -18  +11  \n"
        +"   29:    -9   +19  \n"
        +"   30:    -20  +1   \n"
        +"   31:    -20  +21  \n"
        +"   32:    -21  +22  \n"
        +"   33:    -2   +22  \n"
        +"   34:    -20  +23  \n"
        +"   35:    -24  +23  \n"
        +"   36:    -0   +24  \n"
        +"   37:    -21  +25  \n"
        +"   38:    -26  +25  \n"
        +"   39:    -23  +26  \n"
        +"   40:    -22  +27  \n"
        +"   41:    -28  +27  \n"
        +"   42:    -28  +25  \n"
        +"   43:    -3   +29  \n"
        +"   44:    -29  +27  \n"
        +"   45:    -30  +29  \n"
        +"   46:    -30  +31  \n"
        +"   47:    -4   +31  \n"
        +"   48:    -32  +28  \n"
        +"   49:    -32  +30  \n"
        +"   50:    -32  +33  \n"
        +"   51:    -33  +34  \n"
        +"   52:    -34  +31  \n"
        +"   53:    -33  +26  \n"
        +"   54:    -24  +34  \n"
        +"   55:    -35  +16  \n"
        +"   56:    -36  +35  \n"
        +"   57:    -36  +31  \n"
        +"   58:    -37  +35  \n"
        +"   59:    -37  +38  \n"
        +"   60:    -38  +19  \n"
        +"   61:    -37  +39  \n"
        +"   62:    -40  +39  \n"
        +"   63:    -40  +36  \n"
        +"   64:    -41  +39  \n"
        +"   65:    -41  +42  \n"
        +"   66:    -42  +38  \n"
        +"   67:    -41  +43  \n"
        +"   68:    -43  +44  \n"
        +"   69:    -40  +44  \n"
        +"   70:    -42  +45  \n"
        +"   71:    -46  +45  \n"
        +"   72:    -43  +46  \n"
        +"   73:    -9   +45  \n"
        +"   74:    -24  +46  \n"
        +"   75:    -44  +34  \n"
        +"   76:    -44  +47  \n"
        +"   77:    -47  +48  \n"
        +"   78:    -49  +48  \n"
        +"   79:    -40  +49  \n"
        +"   80:    -50  +47  \n"
        +"   81:    -51  +48  \n"
        +"   82:    -52  +51  \n"
        +"   83:    -52  +50  \n"
        +"   84:    -53  +51  \n"
        +"   85:    -53  +54  \n"
        +"   86:    -49  +54  \n"
        +"   87:    -55  +53  \n"
        +"   88:    -55  +56  \n"
        +"   89:    -52  +56  \n"
        +"   90:    -57  +54  \n"
        +"   91:    -58  +57  \n"
        +"   92:    -55  +58  \n"
        +"   93:    -36  +57  \n"
        +"   94:    -30  +58  \n"
        +"   95:    -32  +56  \n"
        +"   96:    -33  +50  \n"
        +"   97:    -59  +60  \n"
        +"   98:    -52  +60  \n"
        +"   99:    -61  +56  \n"
        +"  100:    -61  +59  \n"
        +"  101:    -50  +62  \n"
        +"  102:    -62  +63  \n"
        +"  103:    -60  +63  \n"
        +"  104:    -64  +63  \n"
        +"  105:    -65  +62  \n"
        +"  106:    -66  +65  \n"
        +"  107:    -66  +64  \n"
        +"  108:    -65  +26  \n"
        +"  109:    -67  +25  \n"
        +"  110:    -28  +68  \n"
        +"  111:    -68  +69  \n"
        +"  112:    -67  +69  \n"
        +"  113:    -61  +68  \n"
        +"  114:    -59  +70  \n"
        +"  115:    -70  +69  \n"
        +"  116:    -64  +70  \n"
        +"  117:    -66  +67  \n"
        +"  118:    -20  +71  \n"
        +"  119:    -71  +72  \n"
        +"  120:    -73  +72  \n"
        +"  121:    -23  +73  \n"
        +"  122:    -74  +72  \n"
        +"  123:    -74  +75  \n"
        +"  124:    -75  +76  \n"
        +"  125:    -71  +76  \n"
        +"  126:    -77  +74  \n"
        +"  127:    -77  +78  \n"
        +"  128:    -78  +73  \n"
        +"  129:    -79  +75  \n"
        +"  130:    -79  +80  \n"
        +"  131:    -77  +80  \n"
        +"  132:    -76  +81  \n"
        +"  133:    -82  +81  \n"
        +"  134:    -79  +82  \n"
        +"  135:    -21  +81  \n"
        +"  136:    -67  +82  \n"
        +"  137:    -78  +65  \n"
        +"  138:    -66  +80  \n"
        +"  139:    -83  +84  \n"
        +"  140:    -84  +85  \n"
        +"  141:    -85  +86  \n"
        +"  142:    -87  +86  \n"
        +"  143:    -83  +87  \n"
        +"  144:    -84  +69  \n"
        +"  145:    -82  +85  \n"
        +"  146:    -83  +88  \n"
        +"  147:    -88  +70  \n"
        +"  148:    -64  +89  \n"
        +"  149:    -89  +90  \n"
        +"  150:    -88  +90  \n"
        +"  151:    -91  +89  \n"
        +"  152:    -90  +92  \n"
        +"  153:    -93  +92  \n"
        +"  154:    -91  +93  \n"
        +"  155:    -87  +92  \n"
        +"  156:    -94  +86  \n"
        +"  157:    -93  +94  \n"
        +"  158:    -79  +94  \n"
        +"  159:    -91  +80  \n"
        +"  160:    -95  +96  \n"
        +"  161:    -97  +96  \n"
        +"  162:    -98  +97  \n"
        +"  163:    -98  +99  \n"
        +"  164:    -99  +95  \n"
        +"  165:    -97  +100 \n"
        +"  166:    -101 +100 \n"
        +"  167:    -102 +101 \n"
        +"  168:    -102 +96  \n"
        +"  169:    -100 +103 \n"
        +"  170:    -89  +103 \n"
        +"  171:    -91  +101 \n"
        +"  172:    -104 +103 \n"
        +"  173:    -98  +104 \n"
        +"  174:    -104 +63  \n"
        +"  175:    -99  +62  \n"
        +"  176:    -78  +95  \n"
        +"  177:    -77  +102 \n"
        +"  178:    -105 +72  \n"
        +"  179:    -106 +105 \n"
        +"  180:    -106 +107 \n"
        +"  181:    -107 +73  \n"
        +"  182:    -74  +108 \n"
        +"  183:    -109 +108 \n"
        +"  184:    -105 +109 \n"
        +"  185:    -110 +108 \n"
        +"  186:    -102 +110 \n"
        +"  187:    -111 +110 \n"
        +"  188:    -112 +111 \n"
        +"  189:    -112 +109 \n"
        +"  190:    -111 +113 \n"
        +"  191:    -113 +96  \n"
        +"  192:    -114 +113 \n"
        +"  193:    -114 +115 \n"
        +"  194:    -112 +115 \n"
        +"  195:    -116 +95  \n"
        +"  196:    -114 +116 \n"
        +"  197:    -107 +116 \n"
        +"  198:    -106 +115 \n"
        +"  199:    -105 +117 \n"
        +"  200:    -118 +117 \n"
        +"  201:    -119 +118 \n"
        +"  202:    -106 +119 \n"
        +"  203:    -120 +117 \n"
        +"  204:    -120 +121 \n"
        +"  205:    -109 +121 \n"
        +"  206:    -122 +120 \n"
        +"  207:    -123 +118 \n"
        +"  208:    -122 +123 \n"
        +"  209:    -123 +124 \n"
        +"  210:    -125 +124 \n"
        +"  211:    -119 +125 \n"
        +"  212:    -126 +127 \n"
        +"  213:    -127 +124 \n"
        +"  214:    -128 +125 \n"
        +"  215:    -128 +126 \n"
        +"  216:    -122 +129 \n"
        +"  217:    -129 +127 \n"
        +"  218:    -129 +130 \n"
        +"  219:    -131 +130 \n"
        +"  220:    -131 +126 \n"
        +"  221:    -130 +121 \n"
        +"  222:    -112 +131 \n"
        +"  223:    -128 +115 \n"
        +"  224:    -107 +46  \n"
        +"  225:    -119 +45  \n"
        +"  226:    -43  +132 \n"
        +"  227:    -116 +132 \n"
        +"  228:    -125 +133 \n"
        +"  229:    -42  +133 \n"
        +"  230:    -41  +134 \n"
        +"  231:    -134 +135 \n"
        +"  232:    -133 +135 \n"
        +"  233:    -134 +136 \n"
        +"  234:    -137 +136 \n"
        +"  235:    -138 +137 \n"
        +"  236:    -138 +135 \n"
        +"  237:    -132 +136 \n"
        +"  238:    -114 +137 \n"
        +"  239:    -128 +138 \n"
        +"  240:    -139 +140 \n"
        +"  241:    -140 +141 \n"
        +"  242:    -142 +141 \n"
        +"  243:    -142 +143 \n"
        +"  244:    -139 +143 \n"
        +"  245:    -141 +144 \n"
        +"  246:    -126 +144 \n"
        +"  247:    -140 +127 \n"
        +"  248:    -144 +145 \n"
        +"  249:    -138 +145 \n"
        +"  250:    -142 +146 \n"
        +"  251:    -146 +145 \n"
        +"  252:    -147 +135 \n"
        +"  253:    -146 +147 \n"
        +"  254:    -148 +133 \n"
        +"  255:    -149 +148 \n"
        +"  256:    -149 +147 \n"
        +"  257:    -150 +124 \n"
        +"  258:    -150 +148 \n"
        +"  259:    -139 +150 \n"
        +"  260:    -149 +143 \n"
        +"  261:    -151 +152 \n"
        +"  262:    -153 +152 \n"
        +"  263:    -154 +153 \n"
        +"  264:    -155 +154 \n"
        +"  265:    -155 +151 \n"
        +"  266:    -156 +152 \n"
        +"  267:    -157 +153 \n"
        +"  268:    -158 +157 \n"
        +"  269:    -158 +156 \n"
        +"  270:    -159 +157 \n"
        +"  271:    -159 +160 \n"
        +"  272:    -154 +160 \n"
        +"  273:    -142 +159 \n"
        +"  274:    -158 +143 \n"
        +"  275:    -161 +160 \n"
        +"  276:    -146 +161 \n"
        +"  277:    -162 +156 \n"
        +"  278:    -149 +162 \n"
        +"  279:    -151 +163 \n"
        +"  280:    -162 +163 \n"
        +"  281:    -147 +164 \n"
        +"  282:    -164 +163 \n"
        +"  283:    -155 +165 \n"
        +"  284:    -165 +164 \n"
        +"  285:    -165 +161 \n"
        +"  286:    -166 +167 \n"
        +"  287:    -168 +167 \n"
        +"  288:    -169 +168 \n"
        +"  289:    -170 +169 \n"
        +"  290:    -170 +166 \n"
        +"  291:    -171 +167 \n"
        +"  292:    -172 +171 \n"
        +"  293:    -172 +173 \n"
        +"  294:    -173 +168 \n"
        +"  295:    -171 +174 \n"
        +"  296:    -174 +175 \n"
        +"  297:    -166 +175 \n"
        +"  298:    -176 +174 \n"
        +"  299:    -176 +177 \n"
        +"  300:    -172 +177 \n"
        +"  301:    -178 +175 \n"
        +"  302:    -179 +178 \n"
        +"  303:    -176 +179 \n"
        +"  304:    -180 +178 \n"
        +"  305:    -170 +180 \n"
        +"  306:    -180 +160 \n"
        +"  307:    -154 +179 \n"
        +"  308:    -169 +161 \n"
        +"  309:    -165 +173 \n"
        +"  310:    -155 +177 \n"
        +"  311:    -181 +182 \n"
        +"  312:    -182 +183 \n"
        +"  313:    -184 +183 \n"
        +"  314:    -185 +184 \n"
        +"  315:    -181 +185 \n"
        +"  316:    -186 +151 \n"
        +"  317:    -182 +152 \n"
        +"  318:    -186 +183 \n"
        +"  319:    -187 +153 \n"
        +"  320:    -181 +187 \n"
        +"  321:    -187 +188 \n"
        +"  322:    -179 +188 \n"
        +"  323:    -185 +189 \n"
        +"  324:    -189 +188 \n"
        +"  325:    -190 +184 \n"
        +"  326:    -190 +191 \n"
        +"  327:    -191 +189 \n"
        +"  328:    -192 +186 \n"
        +"  329:    -192 +190 \n"
        +"  330:    -176 +191 \n"
        +"  331:    -192 +177 \n"
        +"  332:    -193 +194 \n"
        +"  333:    -194 +195 \n"
        +"  334:    -196 +195 \n"
        +"  335:    -196 +197 \n"
        +"  336:    -193 +197 \n"
        +"  337:    -195 +175 \n"
        +"  338:    -174 +198 \n"
        +"  339:    -194 +198 \n"
        +"  340:    -199 +178 \n"
        +"  341:    -196 +199 \n"
        +"  342:    -200 +199 \n"
        +"  343:    -200 +188 \n"
        +"  344:    -201 +197 \n"
        +"  345:    -201 +200 \n"
        +"  346:    -201 +202 \n"
        +"  347:    -202 +203 \n"
        +"  348:    -193 +203 \n"
        +"  349:    -202 +189 \n"
        +"  350:    -191 +204 \n"
        +"  351:    -203 +204 \n"
        +"  352:    -204 +198 \n"
        +"  353:    -205 +206 \n"
        +"  354:    -207 +206 \n"
        +"  355:    -207 +208 \n"
        +"  356:    -209 +208 \n"
        +"  357:    -209 +205 \n"
        +"  358:    -206 +210 \n"
        +"  359:    -210 +211 \n"
        +"  360:    -212 +211 \n"
        +"  361:    -207 +212 \n"
        +"  362:    -213 +210 \n"
        +"  363:    -213 +214 \n"
        +"  364:    -205 +214 \n"
        +"  365:    -194 +211 \n"
        +"  366:    -193 +213 \n"
        +"  367:    -212 +215 \n"
        +"  368:    -215 +198 \n"
        +"  369:    -216 +215 \n"
        +"  370:    -216 +208 \n"
        +"  371:    -216 +217 \n"
        +"  372:    -218 +217 \n"
        +"  373:    -209 +218 \n"
        +"  374:    -217 +204 \n"
        +"  375:    -218 +219 \n"
        +"  376:    -203 +219 \n"
        +"  377:    -214 +219 \n"
        +"  378:    -220 +213 \n"
        +"  379:    -221 +220 \n"
        +"  380:    -221 +197 \n"
        +"  381:    -220 +222 \n"
        +"  382:    -223 +222 \n"
        +"  383:    -223 +214 \n"
        +"  384:    -224 +222 \n"
        +"  385:    -225 +224 \n"
        +"  386:    -226 +225 \n"
        +"  387:    -226 +223 \n"
        +"  388:    -221 +227 \n"
        +"  389:    -227 +224 \n"
        +"  390:    -228 +229 \n"
        +"  391:    -225 +229 \n"
        +"  392:    -227 +228 \n"
        +"  393:    -229 +230 \n"
        +"  394:    -231 +230 \n"
        +"  395:    -226 +231 \n"
        +"  396:    -202 +230 \n"
        ;
    private static String pcalcString533_secondHalf = ""
        +"  397:    -201 +228 \n"
        +"  398:    -231 +219 \n"
        +"  399:    -232 +233 \n"
        +"  400:    -234 +233 \n"
        +"  401:    -235 +234 \n"
        +"  402:    -236 +235 \n"
        +"  403:    -236 +232 \n"
        +"  404:    -237 +233 \n"
        +"  405:    -221 +237 \n"
        +"  406:    -227 +234 \n"
        +"  407:    -237 +238 \n"
        +"  408:    -238 +239 \n"
        +"  409:    -232 +239 \n"
        +"  410:    -196 +238 \n"
        +"  411:    -239 +240 \n"
        +"  412:    -199 +240 \n"
        +"  413:    -241 +240 \n"
        +"  414:    -236 +241 \n"
        +"  415:    -241 +242 \n"
        +"  416:    -200 +242 \n"
        +"  417:    -243 +242 \n"
        +"  418:    -235 +243 \n"
        +"  419:    -228 +243 \n"
        +"  420:    -244 +245 \n"
        +"  421:    -246 +245 \n"
        +"  422:    -246 +247 \n"
        +"  423:    -247 +248 \n"
        +"  424:    -244 +248 \n"
        +"  425:    -249 +245 \n"
        +"  426:    -250 +249 \n"
        +"  427:    -250 +251 \n"
        +"  428:    -246 +251 \n"
        +"  429:    -249 +243 \n"
        +"  430:    -244 +235 \n"
        +"  431:    -250 +229 \n"
        +"  432:    -252 +248 \n"
        +"  433:    -252 +234 \n"
        +"  434:    -253 +252 \n"
        +"  435:    -253 +254 \n"
        +"  436:    -247 +254 \n"
        +"  437:    -253 +224 \n"
        +"  438:    -255 +225 \n"
        +"  439:    -255 +254 \n"
        +"  440:    -255 +251 \n"
        +"  441:    -256 +246 \n"
        +"  442:    -257 +247 \n"
        +"  443:    -257 +258 \n"
        +"  444:    -256 +258 \n"
        +"  445:    -254 +259 \n"
        +"  446:    -260 +259 \n"
        +"  447:    -257 +260 \n"
        +"  448:    -261 +259 \n"
        +"  449:    -262 +261 \n"
        +"  450:    -262 +255 \n"
        +"  451:    -260 +263 \n"
        +"  452:    -263 +264 \n"
        +"  453:    -261 +264 \n"
        +"  454:    -265 +263 \n"
        +"  455:    -265 +258 \n"
        +"  456:    -266 +264 \n"
        +"  457:    -267 +266 \n"
        +"  458:    -265 +267 \n"
        +"  459:    -268 +266 \n"
        +"  460:    -262 +268 \n"
        +"  461:    -268 +269 \n"
        +"  462:    -270 +269 \n"
        +"  463:    -270 +267 \n"
        +"  464:    -251 +269 \n"
        +"  465:    -256 +270 \n"
        +"  466:    -271 +272 \n"
        +"  467:    -272 +273 \n"
        +"  468:    -274 +273 \n"
        +"  469:    -275 +274 \n"
        +"  470:    -271 +275 \n"
        +"  471:    -276 +277 \n"
        +"  472:    -278 +277 \n"
        +"  473:    -278 +273 \n"
        +"  474:    -276 +272 \n"
        +"  475:    -279 +277 \n"
        +"  476:    -280 +279 \n"
        +"  477:    -281 +280 \n"
        +"  478:    -281 +278 \n"
        +"  479:    -267 +279 \n"
        +"  480:    -265 +276 \n"
        +"  481:    -270 +282 \n"
        +"  482:    -280 +282 \n"
        +"  483:    -271 +258 \n"
        +"  484:    -283 +275 \n"
        +"  485:    -256 +283 \n"
        +"  486:    -284 +274 \n"
        +"  487:    -284 +285 \n"
        +"  488:    -283 +285 \n"
        +"  489:    -281 +284 \n"
        +"  490:    -282 +285 \n"
        +"  491:    -245 +286 \n"
        +"  492:    -283 +286 \n"
        +"  493:    -287 +285 \n"
        +"  494:    -288 +287 \n"
        +"  495:    -288 +286 \n"
        +"  496:    -289 +282 \n"
        +"  497:    -290 +289 \n"
        +"  498:    -290 +287 \n"
        +"  499:    -291 +288 \n"
        +"  500:    -291 +249 \n"
        +"  501:    -290 +292 \n"
        +"  502:    -291 +292 \n"
        +"  503:    -293 +289 \n"
        +"  504:    -294 +293 \n"
        +"  505:    -294 +292 \n"
        +"  506:    -293 +269 \n"
        +"  507:    -294 +250 \n"
        +"  508:    -295 +230 \n"
        +"  509:    -294 +295 \n"
        +"  510:    -295 +185 \n"
        +"  511:    -181 +292 \n"
        +"  512:    -296 +242 \n"
        +"  513:    -291 +296 \n"
        +"  514:    -296 +187 \n"
        +"  515:    -296 +297 \n"
        +"  516:    -157 +297 \n"
        +"  517:    -298 +240 \n"
        +"  518:    -298 +180 \n"
        +"  519:    -299 +241 \n"
        +"  520:    -299 +300 \n"
        +"  521:    -298 +300 \n"
        +"  522:    -299 +297 \n"
        +"  523:    -159 +300 \n"
        +"  524:    -301 +302 \n"
        +"  525:    -303 +302 \n"
        +"  526:    -303 +304 \n"
        +"  527:    -304 +305 \n"
        +"  528:    -301 +305 \n"
        +"  529:    -170 +302 \n"
        +"  530:    -306 +169 \n"
        +"  531:    -303 +306 \n"
        +"  532:    -301 +298 \n"
        +"  533:    -306 +145 \n"
        +"  534:    -307 +300 \n"
        +"  535:    -141 +307 \n"
        +"  536:    -305 +307 \n"
        +"  537:    -304 +144 \n"
        +"  538:    -304 +308 \n"
        +"  539:    -308 +309 \n"
        +"  540:    -310 +309 \n"
        +"  541:    -303 +310 \n"
        +"  542:    -111 +309 \n"
        +"  543:    -131 +308 \n"
        +"  544:    -113 +311 \n"
        +"  545:    -310 +311 \n"
        +"  546:    -137 +312 \n"
        +"  547:    -311 +312 \n"
        +"  548:    -306 +312 \n"
        +"  549:    -313 +314 \n"
        +"  550:    -315 +314 \n"
        +"  551:    -129 +315 \n"
        +"  552:    -313 +130 \n"
        +"  553:    -316 +314 \n"
        +"  554:    -316 +317 \n"
        +"  555:    -317 +318 \n"
        +"  556:    -315 +318 \n"
        +"  557:    -316 +319 \n"
        +"  558:    -320 +319 \n"
        +"  559:    -320 +313 \n"
        +"  560:    -317 +307 \n"
        +"  561:    -305 +319 \n"
        +"  562:    -140 +318 \n"
        +"  563:    -320 +308 \n"
        +"  564:    -321 +322 \n"
        +"  565:    -323 +322 \n"
        +"  566:    -324 +323 \n"
        +"  567:    -325 +324 \n"
        +"  568:    -325 +321 \n"
        +"  569:    -326 +322 \n"
        +"  570:    -327 +326 \n"
        +"  571:    -327 +328 \n"
        +"  572:    -328 +323 \n"
        +"  573:    -329 +326 \n"
        +"  574:    -329 +330 \n"
        +"  575:    -321 +330 \n"
        +"  576:    -301 +329 \n"
        +"  577:    -327 +302 \n"
        +"  578:    -319 +330 \n"
        +"  579:    -325 +320 \n"
        +"  580:    -324 +309 \n"
        +"  581:    -310 +328 \n"
        +"  582:    -328 +331 \n"
        +"  583:    -332 +331 \n"
        +"  584:    -332 +311 \n"
        +"  585:    -331 +333 \n"
        +"  586:    -334 +333 \n"
        +"  587:    -327 +334 \n"
        +"  588:    -335 +333 \n"
        +"  589:    -336 +335 \n"
        +"  590:    -336 +332 \n"
        +"  591:    -334 +166 \n"
        +"  592:    -335 +167 \n"
        +"  593:    -337 +168 \n"
        +"  594:    -336 +337 \n"
        +"  595:    -337 +312 \n"
        +"  596:    -97  +332 \n"
        +"  597:    -98  +338 \n"
        +"  598:    -336 +338 \n"
        +"  599:    -99  +339 \n"
        +"  600:    -340 +339 \n"
        +"  601:    -340 +338 \n"
        +"  602:    -339 +132 \n"
        +"  603:    -341 +136 \n"
        +"  604:    -340 +341 \n"
        +"  605:    -341 +337 \n"
        +"  606:    -171 +342 \n"
        +"  607:    -343 +342 \n"
        +"  608:    -335 +343 \n"
        +"  609:    -344 +342 \n"
        +"  610:    -345 +344 \n"
        +"  611:    -172 +345 \n"
        +"  612:    -346 +344 \n"
        +"  613:    -347 +346 \n"
        +"  614:    -347 +343 \n"
        +"  615:    -348 +346 \n"
        +"  616:    -348 +349 \n"
        +"  617:    -345 +349 \n"
        +"  618:    -347 +338 \n"
        +"  619:    -340 +348 \n"
        +"  620:    -173 +350 \n"
        +"  621:    -341 +350 \n"
        +"  622:    -350 +349 \n"
        +"  623:    -339 +47  \n"
        +"  624:    -348 +48  \n"
        +"  625:    -351 +349 \n"
        +"  626:    -49  +351 \n"
        +"  627:    -352 +350 \n"
        +"  628:    -353 +352 \n"
        +"  629:    -353 +351 \n"
        +"  630:    -134 +352 \n"
        +"  631:    -353 +39  \n"
        +"  632:    -351 +354 \n"
        +"  633:    -355 +354 \n"
        +"  634:    -355 +54  \n"
        +"  635:    -354 +356 \n"
        +"  636:    -357 +356 \n"
        +"  637:    -353 +357 \n"
        +"  638:    -358 +356 \n"
        +"  639:    -359 +358 \n"
        +"  640:    -359 +355 \n"
        +"  641:    -360 +357 \n"
        +"  642:    -360 +361 \n"
        +"  643:    -358 +361 \n"
        +"  644:    -37  +360 \n"
        +"  645:    -362 +361 \n"
        +"  646:    -35  +362 \n"
        +"  647:    -363 +362 \n"
        +"  648:    -359 +363 \n"
        +"  649:    -363 +57  \n"
        +"  650:    -53  +364 \n"
        +"  651:    -365 +364 \n"
        +"  652:    -365 +355 \n"
        +"  653:    -366 +367 \n"
        +"  654:    -367 +368 \n"
        +"  655:    -368 +364 \n"
        +"  656:    -366 +365 \n"
        +"  657:    -369 +370 \n"
        +"  658:    -371 +370 \n"
        +"  659:    -371 +367 \n"
        +"  660:    -369 +368 \n"
        +"  661:    -370 +372 \n"
        +"  662:    -373 +372 \n"
        +"  663:    -374 +373 \n"
        +"  664:    -374 +371 \n"
        +"  665:    -58  +372 \n"
        +"  666:    -373 +363 \n"
        +"  667:    -55  +369 \n"
        +"  668:    -374 +375 \n"
        +"  669:    -359 +375 \n"
        +"  670:    -366 +375 \n"
        +"  671:    -186 +356 \n"
        +"  672:    -376 +354 \n"
        +"  673:    -192 +376 \n"
        +"  674:    -365 +377 \n"
        +"  675:    -376 +377 \n"
        +"  676:    -378 +377 \n"
        +"  677:    -190 +378 \n"
        +"  678:    -366 +379 \n"
        +"  679:    -379 +378 \n"
        +"  680:    -379 +380 \n"
        +"  681:    -381 +380 \n"
        +"  682:    -381 +375 \n"
        +"  683:    -380 +184 \n"
        +"  684:    -382 +183 \n"
        +"  685:    -381 +382 \n"
        +"  686:    -382 +358 \n"
        +"  687:    -383 +361 \n"
        +"  688:    -383 +384 \n"
        +"  689:    -382 +384 \n"
        +"  690:    -385 +362 \n"
        +"  691:    -386 +385 \n"
        +"  692:    -386 +383 \n"
        +"  693:    -373 +387 \n"
        +"  694:    -385 +387 \n"
        +"  695:    -388 +389 \n"
        +"  696:    -389 +387 \n"
        +"  697:    -386 +388 \n"
        +"  698:    -390 +389 \n"
        +"  699:    -374 +390 \n"
        +"  700:    -390 +391 \n"
        +"  701:    -381 +391 \n"
        +"  702:    -392 +384 \n"
        +"  703:    -391 +392 \n"
        +"  704:    -388 +392 \n"
        +"  705:    -293 +393 \n"
        +"  706:    -394 +393 \n"
        +"  707:    -268 +394 \n"
        +"  708:    -395 +279 \n"
        +"  709:    -396 +395 \n"
        +"  710:    -396 +266 \n"
        +"  711:    -280 +388 \n"
        +"  712:    -395 +389 \n"
        +"  713:    -289 +392 \n"
        +"  714:    -391 +393 \n"
        +"  715:    -394 +397 \n"
        +"  716:    -390 +397 \n"
        +"  717:    -396 +397 \n"
        +"  718:    -398 +399 \n"
        +"  719:    -399 +387 \n"
        +"  720:    -398 +372 \n"
        +"  721:    -400 +395 \n"
        +"  722:    -400 +399 \n"
        +"  723:    -371 +401 \n"
        +"  724:    -401 +397 \n"
        +"  725:    -402 +370 \n"
        +"  726:    -403 +402 \n"
        +"  727:    -403 +401 \n"
        +"  728:    -404 +398 \n"
        +"  729:    -404 +402 \n"
        +"  730:    -400 +405 \n"
        +"  731:    -404 +405 \n"
        +"  732:    -406 +396 \n"
        +"  733:    -406 +405 \n"
        +"  734:    -406 +403 \n"
        +"  735:    -403 +407 \n"
        +"  736:    -407 +408 \n"
        +"  737:    -409 +408 \n"
        +"  738:    -406 +409 \n"
        +"  739:    -402 +410 \n"
        +"  740:    -411 +410 \n"
        +"  741:    -411 +407 \n"
        +"  742:    -412 +413 \n"
        +"  743:    -410 +413 \n"
        +"  744:    -404 +412 \n"
        +"  745:    -414 +413 \n"
        +"  746:    -415 +411 \n"
        +"  747:    -415 +414 \n"
        +"  748:    -416 +408 \n"
        +"  749:    -415 +416 \n"
        +"  750:    -417 +418 \n"
        +"  751:    -414 +418 \n"
        +"  752:    -416 +417 \n"
        +"  753:    -419 +418 \n"
        +"  754:    -412 +419 \n"
        +"  755:    -420 +419 \n"
        +"  756:    -421 +417 \n"
        +"  757:    -420 +421 \n"
        +"  758:    -409 +421 \n"
        +"  759:    -420 +405 \n"
        +"  760:    -399 +14  \n"
        +"  761:    -398 +29  \n"
        +"  762:    -422 +12  \n"
        +"  763:    -400 +422 \n"
        +"  764:    -422 +423 \n"
        +"  765:    -423 +424 \n"
        +"  766:    -7   +424 \n"
        +"  767:    -420 +423 \n"
        +"  768:    -425 +424 \n"
        +"  769:    -419 +425 \n"
        +"  770:    -22  +425 \n"
        +"  771:    -412 +27  \n"
        +"  772:    -68  +413 \n"
        +"  773:    -426 +410 \n"
        +"  774:    -61  +426 \n"
        +"  775:    -426 +369 \n"
        +"  776:    -51  +427 \n"
        +"  777:    -428 +427 \n"
        +"  778:    -428 +364 \n"
        +"  779:    -429 +427 \n"
        +"  780:    -430 +429 \n"
        +"  781:    -431 +430 \n"
        +"  782:    -431 +428 \n"
        +"  783:    -60  +429 \n"
        +"  784:    -59  +432 \n"
        +"  785:    -430 +432 \n"
        +"  786:    -433 +368 \n"
        +"  787:    -433 +434 \n"
        +"  788:    -426 +434 \n"
        +"  789:    -431 +433 \n"
        +"  790:    -434 +432 \n"
        +"  791:    -435 +432 \n"
        +"  792:    -436 +435 \n"
        +"  793:    -436 +437 \n"
        +"  794:    -437 +434 \n"
        +"  795:    -435 +88  \n"
        +"  796:    -83  +438 \n"
        +"  797:    -436 +438 \n"
        +"  798:    -414 +84  \n"
        +"  799:    -437 +411 \n"
        +"  800:    -415 +438 \n"
        +"  801:    -439 +418 \n"
        +"  802:    -440 +417 \n"
        +"  803:    -441 +440 \n"
        +"  804:    -441 +439 \n"
        +"  805:    -439 +85  \n"
        +"  806:    -416 +442 \n"
        +"  807:    -443 +442 \n"
        +"  808:    -443 +438 \n"
        +"  809:    -444 +445 \n"
        +"  810:    -446 +445 \n"
        +"  811:    -442 +446 \n"
        +"  812:    -443 +444 \n"
        +"  813:    -447 +445 \n"
        +"  814:    -440 +446 \n"
        +"  815:    -441 +447 \n"
        +"  816:    -447 +86  \n"
        +"  817:    -444 +87  \n"
        +"  818:    -435 +448 \n"
        +"  819:    -449 +448 \n"
        +"  820:    -450 +449 \n"
        +"  821:    -436 +450 \n"
        +"  822:    -451 +448 \n"
        +"  823:    -452 +451 \n"
        +"  824:    -452 +453 \n"
        +"  825:    -453 +449 \n"
        +"  826:    -451 +90  \n"
        +"  827:    -454 +92  \n"
        +"  828:    -452 +454 \n"
        +"  829:    -444 +455 \n"
        +"  830:    -454 +455 \n"
        +"  831:    -456 +455 \n"
        +"  832:    -457 +456 \n"
        +"  833:    -443 +457 \n"
        +"  834:    -453 +456 \n"
        +"  835:    -450 +457 \n"
        +"  836:    -450 +458 \n"
        +"  837:    -459 +458 \n"
        +"  838:    -437 +459 \n"
        +"  839:    -460 +458 \n"
        +"  840:    -460 +461 \n"
        +"  841:    -457 +461 \n"
        +"  842:    -462 +460 \n"
        +"  843:    -462 +463 \n"
        +"  844:    -463 +459 \n"
        +"  845:    -461 +464 \n"
        +"  846:    -465 +464 \n"
        +"  847:    -462 +465 \n"
        +"  848:    -442 +464 \n"
        +"  849:    -465 +408 \n"
        +"  850:    -463 +407 \n"
        +"  851:    -466 +394 \n"
        +"  852:    -466 +467 \n"
        +"  853:    -262 +467 \n"
        +"  854:    -468 +401 \n"
        +"  855:    -466 +468 \n"
        +"  856:    -463 +468 \n"
        +"  857:    -462 +467 \n"
        +"  858:    -261 +465 \n"
        +"  859:    -409 +264 \n"
        +"  860:    -422 +277 \n"
        +"  861:    -263 +469 \n"
        +"  862:    -469 +470 \n"
        +"  863:    -276 +470 \n"
        +"  864:    -421 +469 \n"
        +"  865:    -423 +470 \n"
        +"  866:    -13  +278 \n"
        +"  867:    -471 +424 \n"
        +"  868:    -471 +472 \n"
        +"  869:    -472 +470 \n"
        +"  870:    -471 +473 \n"
        +"  871:    -6   +473 \n"
        +"  872:    -472 +474 \n"
        +"  873:    -475 +474 \n"
        +"  874:    -475 +473 \n"
        +"  875:    -272 +474 \n"
        +"  876:    -476 +475 \n"
        +"  877:    -476 +273 \n"
        +"  878:    -476 +10  \n"
        +"  879:    -477 +478 \n"
        +"  880:    -478 +474 \n"
        +"  881:    -271 +477 \n"
        +"  882:    -479 +478 \n"
        +"  883:    -480 +479 \n"
        +"  884:    -480 +472 \n"
        +"  885:    -479 +481 \n"
        +"  886:    -482 +481 \n"
        +"  887:    -482 +477 \n"
        +"  888:    -483 +481 \n"
        +"  889:    -257 +482 \n"
        +"  890:    -260 +483 \n"
        +"  891:    -484 +483 \n"
        +"  892:    -484 +469 \n"
        +"  893:    -480 +484 \n"
        +"  894:    -485 +486 \n"
        +"  895:    -487 +486 \n"
        +"  896:    -488 +487 \n"
        +"  897:    -488 +489 \n"
        +"  898:    -489 +485 \n"
        +"  899:    -490 +486 \n"
        +"  900:    -447 +490 \n"
        +"  901:    -487 +445 \n"
        +"  902:    -491 +485 \n"
        +"  903:    -492 +490 \n"
        +"  904:    -492 +491 \n"
        +"  905:    -441 +493 \n"
        +"  906:    -492 +493 \n"
        +"  907:    -494 +446 \n"
        +"  908:    -488 +494 \n"
        +"  909:    -484 +440 \n"
        +"  910:    -494 +483 \n"
        +"  911:    -489 +481 \n"
        +"  912:    -479 +491 \n"
        +"  913:    -480 +493 \n"
        +"  914:    -495 +496 \n"
        +"  915:    -497 +496 \n"
        +"  916:    -497 +498 \n"
        +"  917:    -498 +499 \n"
        +"  918:    -495 +499 \n"
        +"  919:    -500 +497 \n"
        +"  920:    -496 +473 \n"
        +"  921:    -500 +471 \n"
        +"  922:    -501 +495 \n"
        +"  923:    -501 +475 \n"
        +"  924:    -499 +502 \n"
        +"  925:    -503 +502 \n"
        +"  926:    -501 +503 \n"
        +"  927:    -491 +502 \n"
        +"  928:    -492 +498 \n"
        +"  929:    -503 +478 \n"
        +"  930:    -500 +493 \n"
        +"  931:    -498 +504 \n"
        +"  932:    -75  +504 \n"
        +"  933:    -497 +76  \n"
        +"  934:    -504 +505 \n"
        +"  935:    -490 +505 \n"
        +"  936:    -94  +505 \n"
        +"  937:    -506 +439 \n"
        +"  938:    -506 +81  \n"
        +"  939:    -500 +506 \n"
        +"  940:    -71  +507 \n"
        +"  941:    -5   +507 \n"
        +"  942:    -507 +496 \n"
        +"  943:    -506 +425 \n"
        +"  944:    -508 +494 \n"
        +"  945:    -508 +464 \n"
        +"  946:    -508 +259 \n"
        +"  947:    -509 +510 \n"
        +"  948:    -510 +222 \n"
        +"  949:    -509 +253 \n"
        +"  950:    -460 +223 \n"
        +"  951:    -510 +461 \n"
        +"  952:    -226 +467 \n"
        +"  953:    -509 +508 \n"
        +"  954:    -511 +487 \n"
        +"  955:    -512 +511 \n"
        +"  956:    -512 +513 \n"
        +"  957:    -488 +513 \n"
        +"  958:    -511 +455 \n"
        +"  959:    -509 +513 \n"
        +"  960:    -514 +456 \n"
        +"  961:    -510 +514 \n"
        +"  962:    -512 +514 \n"
        +"  963:    -515 +233 \n"
        +"  964:    -516 +252 \n"
        +"  965:    -516 +515 \n"
        +"  966:    -237 +517 \n"
        +"  967:    -518 +517 \n"
        +"  968:    -515 +518 \n"
        +"  969:    -519 +517 \n"
        +"  970:    -220 +519 \n"
        +"  971:    -512 +518 \n"
        +"  972:    -514 +519 \n"
        +"  973:    -516 +513 \n"
        +"  974:    -248 +520 \n"
        +"  975:    -482 +520 \n"
        +"  976:    -521 +520 \n"
        +"  977:    -516 +521 \n"
        +"  978:    -521 +489 \n"
        +"  979:    -521 +522 \n"
        +"  980:    -523 +522 \n"
        +"  981:    -515 +523 \n"
        +"  982:    -524 +522 \n"
        +"  983:    -525 +524 \n"
        +"  984:    -525 +526 \n"
        +"  985:    -526 +523 \n"
        +"  986:    -524 +485 \n"
        +"  987:    -527 +486 \n"
        +"  988:    -511 +528 \n"
        +"  989:    -527 +528 \n"
        +"  990:    -529 +528 \n"
        +"  991:    -518 +529 \n"
        +"  992:    -526 +529 \n"
        +"  993:    -525 +527 \n"
        +"  994:    -530 +499 \n"
        +"  995:    -530 +531 \n"
        +"  996:    -531 +504 \n"
        +"  997:    -532 +502 \n"
        +"  998:    -533 +532 \n"
        +"  999:    -533 +530 \n"
        +" 1000:    -532 +524 \n"
        +" 1001:    -534 +527 \n"
        +" 1002:    -534 +505 \n"
        +" 1003:    -525 +535 \n"
        +" 1004:    -536 +535 \n"
        +" 1005:    -536 +534 \n"
        +" 1006:    -533 +535 \n"
        +" 1007:    -536 +531 \n"
        +" 1008:    -531 +108 \n"
        +" 1009:    -93  +537 \n"
        +" 1010:    -534 +537 \n"
        +" 1011:    -538 +110 \n"
        +" 1012:    -536 +538 \n"
        +" 1013:    -101 +539 \n"
        +" 1014:    -538 +539 \n"
        +" 1015:    -539 +537 \n"
        +" 1016:    -540 +529 \n"
        +" 1017:    -540 +322 \n"
        +" 1018:    -321 +526 \n"
        +" 1019:    -541 +528 \n"
        +" 1020:    -542 +541 \n"
        +" 1021:    -542 +540 \n"
        +" 1022:    -541 +537 \n"
        +" 1023:    -542 +543 \n"
        +" 1024:    -543 +539 \n"
        +" 1025:    -543 +323 \n"
        +" 1026:    -324 +538 \n"
        +" 1027:    -325 +535 \n"
        +" 1028:    -532 +544 \n"
        +" 1029:    -314 +544 \n"
        +" 1030:    -533 +313 \n"
        +" 1031:    -545 +544 \n"
        +" 1032:    -546 +545 \n"
        +" 1033:    -546 +316 \n"
        +" 1034:    -545 +522 \n"
        +" 1035:    -547 +523 \n"
        +" 1036:    -546 +547 \n"
        +" 1037:    -547 +330 \n"
        +" 1038:    -530 +121 \n"
        +" 1039:    -120 +495 \n"
        +" 1040:    -548 +544 \n"
        +" 1041:    -548 +503 \n"
        +" 1042:    -549 +315 \n"
        +" 1043:    -549 +548 \n"
        +" 1044:    -549 +550 \n"
        +" 1045:    -122 +550 \n"
        +" 1046:    -501 +550 \n"
        +" 1047:    -551 +476 \n"
        +" 1048:    -551 +552 \n"
        +" 1049:    -552 +11  \n"
        +" 1050:    -551 +550 \n"
        +" 1051:    -507 +117 \n"
        +" 1052:    -8   +118 \n"
        +" 1053:    -123 +552 \n"
        +" 1054:    -318 +553 \n"
        +" 1055:    -554 +553 \n"
        +" 1056:    -549 +554 \n"
        +" 1057:    -555 +553 \n"
        +" 1058:    -139 +555 \n"
        +" 1059:    -555 +556 \n"
        +" 1060:    -557 +556 \n"
        +" 1061:    -554 +557 \n"
        +" 1062:    -558 +556 \n"
        +" 1063:    -150 +558 \n"
        +" 1064:    -551 +557 \n"
        +" 1065:    -552 +558 \n"
        +" 1066:    -477 +559 \n"
        +" 1067:    -548 +559 \n"
        +" 1068:    -557 +274 \n"
        +" 1069:    -275 +560 \n"
        +" 1070:    -554 +560 \n"
        +" 1071:    -560 +559 \n"
        +" 1072:    -561 +545 \n"
        +" 1073:    -561 +559 \n"
        +" 1074:    -562 +561 \n"
        +" 1075:    -562 +563 \n"
        +" 1076:    -546 +563 \n"
        +" 1077:    -564 +560 \n"
        +" 1078:    -562 +564 \n"
        +" 1079:    -565 +553 \n"
        +" 1080:    -564 +565 \n"
        +" 1081:    -566 +317 \n"
        +" 1082:    -566 +563 \n"
        +" 1083:    -566 +565 \n"
        +" 1084:    -564 +286 \n"
        +" 1085:    -562 +244 \n"
        +" 1086:    -288 +567 \n"
        +" 1087:    -565 +567 \n"
        +" 1088:    -567 +297 \n"
        +" 1089:    -566 +299 \n"
        +" 1090:    -236 +563 \n"
        +" 1091:    -547 +232 \n"
        +" 1092:    -329 +239 \n"
        +" 1093:    -568 +567 \n"
        +" 1094:    -158 +568 \n"
        +" 1095:    -555 +568 \n"
        +" 1096:    -569 +570 \n"
        +" 1097:    -568 +570 \n"
        +" 1098:    -569 +556 \n"
        +" 1099:    -156 +571 \n"
        +" 1100:    -570 +571 \n"
        +" 1101:    -572 +571 \n"
        +" 1102:    -573 +569 \n"
        +" 1103:    -573 +572 \n"
        +" 1104:    -574 +558 \n"
        +" 1105:    -573 +574 \n"
        +" 1106:    -148 +575 \n"
        +" 1107:    -574 +575 \n"
        +" 1108:    -572 +576 \n"
        +" 1109:    -162 +576 \n"
        +" 1110:    -575 +576 \n"
        +" 1111:    -352 +164 \n"
        +" 1112:    -357 +163 \n"
        +" 1113:    -360 +576 \n"
        +" 1114:    -38  +575 \n"
        +" 1115:    -18  +574 \n"
        +" 1116:    -15  +385 \n"
        +" 1117:    -383 +572 \n"
        +" 1118:    -573 +577 \n"
        +" 1119:    -386 +577 \n"
        +" 1120:    -17  +577 \n"
        +" 1121:    -281 +577 \n"
        +" 1122:    -284 +569 \n"
        +" 1123:    -104 +578 \n"
        +" 1124:    -578 +429 \n"
        +" 1125:    -347 +578 \n"
        +" 1126:    -346 +427 \n"
        +" 1127:    -428 +579 \n"
        +" 1128:    -579 +344 \n"
        +" 1129:    -579 +377 \n"
        +" 1130:    -376 +345 \n"
        +" 1131:    -580 +571 \n"
        +" 1132:    -580 +384 \n"
        +" 1133:    -580 +182 \n"
        +" 1134:    -290 +580 \n"
        +" 1135:    -287 +570 \n"
        +" 1136:    -561 +520 \n"
        +" 1137:    -540 +581 \n"
        +" 1138:    -581 +517 \n"
        +" 1139:    -326 +582 \n"
        +" 1140:    -581 +582 \n"
        +" 1141:    -238 +582 \n"
        +" 1142:    -583 +211 \n"
        +" 1143:    -584 +210 \n"
        +" 1144:    -585 +584 \n"
        +" 1145:    -585 +583 \n"
        +" 1146:    -583 +586 \n"
        +" 1147:    -195 +586 \n"
        +" 1148:    -582 +586 \n"
        +" 1149:    -585 +581 \n"
        +" 1150:    -584 +519 \n"
        +" 1151:    -206 +449 \n"
        +" 1152:    -205 +458 \n"
        +" 1153:    -453 +584 \n"
        +" 1154:    -587 +459 \n"
        +" 1155:    -209 +587 \n"
        +" 1156:    -218 +588 \n"
        +" 1157:    -589 +588 \n"
        +" 1158:    -589 +231 \n"
        +" 1159:    -587 +590 \n"
        +" 1160:    -590 +588 \n"
        +" 1161:    -468 +590 \n"
        +" 1162:    -466 +589 \n"
        +" 1163:    -587 +433 \n"
        +" 1164:    -367 +590 \n"
        +" 1165:    -379 +588 \n"
        +" 1166:    -380 +591 \n"
        +" 1167:    -589 +591 \n"
        +" 1168:    -393 +591 \n"
        +" 1169:    -295 +591 \n"
        +" 1170:    -217 +378 \n"
        +" 1171:    -216 +579 \n"
        +" 1172:    -215 +342 \n"
        +" 1173:    -431 +208 \n"
        +" 1174:    -343 +592 \n"
        +" 1175:    -212 +592 \n"
        +" 1176:    -578 +593 \n"
        +" 1177:    -593 +592 \n"
        +" 1178:    -594 +593 \n"
        +" 1179:    -430 +594 \n"
        +" 1180:    -207 +594 \n"
        +" 1181:    -594 +448 \n"
        +" 1182:    -595 +593 \n"
        +" 1183:    -451 +595 \n"
        +" 1184:    -596 +583 \n"
        +" 1185:    -596 +592 \n"
        +" 1186:    -452 +597 \n"
        +" 1187:    -585 +597 \n"
        +" 1188:    -598 +596 \n"
        +" 1189:    -598 +597 \n"
        +" 1190:    -598 +595 \n"
        +" 1191:    -599 +100 \n"
        +" 1192:    -543 +599 \n"
        +" 1193:    -595 +103 \n"
        +" 1194:    -598 +599 \n"
        +" 1195:    -542 +597 \n"
        +" 1196:    -454 +541 \n"
        +" 1197:    -334 +586 \n"
        +" 1198:    -596 +333 \n"
        +" 1199:    -599 +331 \n"
        +"720 2-cells:\n"
        +"   0:    +0    -1    +2    -3    -4    \n"
        +"   1:    +1    -5    +6    +7    -8    \n"
        +"   2:    +0    -5    +9    -10   -11   \n"
        +"   3:    +6    +12   -13   -14   -9    \n"
        +"   4:    +7    +15   -16   +17   -12   \n"
        +"   5:    +2    +18   +19   -15   -8    \n"
        +"   6:    +3    +18   -20   +21   -22   \n"
        +"   7:    +23   +16   -19   -20   -24   \n"
        +"   8:    +25   +26   +27   -21   -24   \n"
        +"   9:    +14   -28   +26   -29   +10   \n"
        +"  10:    +13   -17   -23   +25   +28   \n"
        +"  11:    +4    +22   -27   -29   -11   \n"
        +"  12:    +1    -30   +31   +32   -33   \n"
        +"  13:    +0    -30   +34   -35   -36   \n"
        +"  14:    +31   +37   -38   -39   -34   \n"
        +"  15:    +32   +40   -41   +42   -37   \n"
        +"  16:    +2    +43   +44   -40   -33   \n"
        +"  17:    +3    +43   -45   +46   -47   \n"
        +"  18:    +48   +41   -44   -45   -49   \n"
        +"  19:    +50   +51   +52   -46   -49   \n"
        +"  20:    +39   -53   +51   -54   +35   \n"
        +"  21:    +38   -42   -48   +50   +53   \n"
        +"  22:    +4    +47   -52   -54   -36   \n"
        +"  23:    +22   -55   -56   +57   -47   \n"
        +"  24:    +27   -55   -58   +59   +60   \n"
        +"  25:    +56   -58   +61   -62   +63   \n"
        +"  26:    +61   -64   +65   +66   -59   \n"
        +"  27:    +62   -64   +67   +68   -69   \n"
        +"  28:    +65   +70   -71   -72   -67   \n"
        +"  29:    +29   -60   -66   +70   -73   \n"
        +"  30:    +11   +73   -71   -74   -36   \n"
        +"  31:    +72   -74   +54   -75   -68   \n"
        +"  32:    +57   -52   -75   -69   +63   \n"
        +"  33:    +76   +77   -78   -79   +69   \n"
        +"  34:    +80   +77   -81   -82   +83   \n"
        +"  35:    +78   -81   -84   +85   -86   \n"
        +"  36:    +82   -84   -87   +88   -89   \n"
        +"  37:    +87   +85   -90   -91   -92   \n"
        +"  38:    +79   +86   -90   -93   -63   \n"
        +"  39:    +93   -91   -94   +46   -57   \n"
        +"  40:    +88   -95   +49   +94   -92   \n"
        +"  41:    +83   -96   -50   +95   -89   \n"
        +"  42:    +80   -76   +75   -51   +96   \n"
        +"  43:    +97   -98   +89   -99   +100  \n"
        +"  44:    +101  +102  -103  -98   +83   \n"
        +"  45:    +104  -102  -105  -106  +107  \n"
        +"  46:    +105  -101  -96   +53   -108  \n"
        +"  47:    +109  -42   +110  +111  -112  \n"
        +"  48:    +48   +110  -113  +99   -95   \n"
        +"  49:    +100  +114  +115  -111  -113  \n"
        +"  50:    +97   +103  -104  +116  -114  \n"
        +"  51:    +107  +116  +115  -112  -117  \n"
        +"  52:    +117  +109  -38   -108  -106  \n"
        +"  53:    +118  +119  -120  -121  -34   \n"
        +"  54:    +119  -122  +123  +124  -125  \n"
        +"  55:    +120  -122  -126  +127  +128  \n"
        +"  56:    +123  -129  +130  -131  +126  \n"
        +"  57:    +124  +132  -133  -134  +129  \n"
        +"  58:    +118  +125  +132  -135  -31   \n"
        +"  59:    +136  +133  -135  +37   -109  \n"
        +"  60:    +121  -128  +137  +108  -39   \n"
        +"  61:    +131  -138  +106  -137  -127  \n"
        +"  62:    +130  -138  +117  +136  -134  \n"
        +"  63:    +139  +140  +141  -142  -143  \n"
        +"  64:    +112  -144  +140  -145  -136  \n"
        +"  65:    +146  +147  +115  -144  -139  \n"
        +"  66:    +148  +149  -150  +147  -116  \n"
        +"  67:    +151  +149  +152  -153  -154  \n"
        +"  68:    +152  -155  -143  +146  +150  \n"
        +"  69:    +156  -142  +155  -153  +157  \n"
        +"  70:    +157  -158  +130  -159  +154  \n"
        +"  71:    +156  -141  -145  -134  +158  \n"
        +"  72:    +151  -148  -107  +138  -159  \n"
        +"  73:    +160  -161  -162  +163  +164  \n"
        +"  74:    +165  -166  -167  +168  -161  \n"
        +"  75:    +166  +169  -170  -151  +171  \n"
        +"  76:    +169  -172  -173  +162  +165  \n"
        +"  77:    +170  -172  +174  -104  +148  \n"
        +"  78:    +163  +175  +102  -174  -173  \n"
        +"  79:    +176  -164  +175  -105  -137  \n"
        +"  80:    +159  -131  +177  +167  -171  \n"
        +"  81:    +177  +168  -160  -176  -127  \n"
        +"  82:    +120  -178  -179  +180  +181  \n"
        +"  83:    +178  -122  +182  -183  -184  \n"
        +"  84:    +182  -185  -186  -177  +126  \n"
        +"  85:    +183  -185  -187  -188  +189  \n"
        +"  86:    +186  -187  +190  +191  -168  \n"
        +"  87:    +190  -192  +193  -194  +188  \n"
        +"  88:    +191  -160  -195  -196  +192  \n"
        +"  89:    +181  -128  +176  -195  -197  \n"
        +"  90:    +180  +197  -196  +193  -198  \n"
        +"  91:    +179  +184  -189  +194  -198  \n"
        +"  92:    +179  +199  -200  -201  -202  \n"
        +"  93:    +199  -203  +204  -205  -184  \n"
        +"  94:    +206  +203  -200  -207  -208  \n"
        +"  95:    +209  -210  -211  +201  -207  \n"
        +"  96:    +212  +213  -210  -214  +215  \n"
        +"  97:    +216  +217  +213  -209  -208  \n"
        +"  98:    +218  -219  +220  +212  -217  \n"
        +"  99:    +205  -221  -219  -222  +189  \n"
        +" 100:    +204  -221  -218  -216  +206  \n"
        +" 101:    +220  -215  +223  -194  +222  \n"
        +" 102:    +202  +211  -214  +223  -198  \n"
        +" 103:    +180  +224  +71   -225  -202  \n"
        +" 104:    +224  -72   +226  -227  -197  \n"
        +" 105:    +228  -229  +70   -225  +211  \n"
        +" 106:    +230  +231  -232  -229  -65   \n"
        +" 107:    +233  -234  -235  +236  -231  \n"
        +" 108:    +227  +237  -234  -238  +196  \n"
        +" 109:    +226  +237  -233  -230  +67   \n"
        +" 110:    +235  -238  +193  -223  +239  \n"
        +" 111:    +236  -232  -228  -214  +239  \n"
        +" 112:    +240  +241  -242  +243  -244  \n"
        +" 113:    +245  -246  +212  -247  +241  \n"
        +" 114:    +215  +246  +248  -249  -239  \n"
        +" 115:    +250  +251  -248  -245  -242  \n"
        +" 116:    +252  -236  +249  -251  +253  \n"
        +" 117:    +252  -232  -254  -255  +256  \n"
        +" 118:    +257  -210  +228  -254  -258  \n"
        +" 119:    +240  +247  +213  -257  -259  \n"
        +" 120:    +244  -260  +255  -258  -259  \n"
        +" 121:    +243  -260  +256  -253  -250  \n"
        +" 122:    +261  -262  -263  -264  +265  \n"
        +" 123:    +266  -262  -267  -268  +269  \n"
        +" 124:    +263  -267  -270  +271  -272  \n"
        +" 125:    +268  -270  -273  +243  -274  \n"
        +" 126:    +275  -271  -273  +250  +276  \n"
        +" 127:    +269  -277  -278  +260  -274  \n"
        +" 128:    +279  -280  +277  +266  -261  \n"
        +" 129:    +281  +282  -280  -278  +256  \n"
        +" 130:    +283  +284  +282  -279  -265  \n"
        +" 131:    +284  -281  -253  +276  -285  \n"
        +" 132:    +272  -275  -285  -283  +264  \n"
        +" 133:    +286  -287  -288  -289  +290  \n"
        +" 134:    +287  -291  -292  +293  +294  \n"
        +" 135:    +286  -291  +295  +296  -297  \n"
        +" 136:    +292  +295  -298  +299  -300  \n"
        +" 137:    +298  +296  -301  -302  -303  \n"
        +" 138:    +297  -301  -304  -305  +290  \n"
        +" 139:    +302  -304  +306  -272  +307  \n"
        +" 140:    +289  +308  +275  -306  -305  \n"
        +" 141:    +288  -294  -309  +285  -308  \n"
        +" 142:    +293  -309  -283  +310  -300  \n"
        +" 143:    +299  -310  +264  +307  -303  \n"
        +" 144:    +311  +312  -313  -314  -315  \n"
        +" 145:    +316  +261  -317  +312  -318  \n"
        +" 146:    +311  +317  -262  -319  -320  \n"
        +" 147:    +263  -319  +321  -322  -307  \n"
        +" 148:    +315  +323  +324  -321  -320  \n"
        +" 149:    +314  -325  +326  +327  -323  \n"
        +" 150:    +328  +318  -313  -325  -329  \n"
        +" 151:    +330  -326  -329  +331  -299  \n"
        +" 152:    +310  -331  +328  +316  -265  \n"
        +" 153:    +303  +322  -324  -327  -330  \n"
        +" 154:    +332  +333  -334  +335  -336  \n"
        +" 155:    +333  +337  -296  +338  -339  \n"
        +" 156:    +334  +337  -301  -340  -341  \n"
        +" 157:    +302  -340  -342  +343  -322  \n"
        +" 158:    +335  -344  +345  +342  -341  \n"
        +" 159:    +336  -344  +346  +347  -348  \n"
        +" 160:    +343  -324  -349  -346  +345  \n"
        +" 161:    +349  -327  +350  -351  -347  \n"
        +" 162:    +338  -352  -350  -330  +298  \n"
        +" 163:    +332  +339  -352  -351  -348  \n"
        +" 164:    +353  -354  +355  -356  +357  \n"
        +" 165:    +354  +358  +359  -360  -361  \n"
        +" 166:    +353  +358  -362  +363  -364  \n"
        +" 167:    +359  -365  -332  +366  +362  \n"
        +" 168:    +365  -360  +367  +368  -339  \n"
        +" 169:    +369  -367  -361  +355  -370  \n"
        +" 170:    +356  -370  +371  -372  -373  \n"
        +" 171:    +352  -368  -369  +371  +374  \n"
        +" 172:    +351  -374  -372  +375  -376  \n"
        +" 173:    +357  +364  +377  -375  -373  \n"
        +" 174:    +366  +363  +377  -376  -348  \n"
        +" 175:    +366  -378  -379  +380  -336  \n"
        +" 176:    +381  -382  +383  -363  -378  \n"
        +" 177:    +382  -384  -385  -386  +387  \n"
        +" 178:    +388  +389  +384  -381  -379  \n"
        +" 179:    +390  -391  +385  -389  +392  \n"
        +" 180:    +386  +391  +393  -394  -395  \n"
        +" 181:    +390  +393  -396  -346  +397  \n"
        +" 182:    +396  -394  +398  -376  -347  \n"
        +" 183:    +387  +383  +377  -398  -395  \n"
        +" 184:    +388  +392  -397  +344  -380  \n"
        +" 185:    +399  -400  -401  -402  +403  \n"
        +" 186:    +400  -404  -405  +388  +406  \n"
        +" 187:    +399  -404  +407  +408  -409  \n"
        +" 188:    +405  +407  -410  +335  -380  \n"
        +" 189:    +410  +408  +411  -412  -341  \n"
        +" 190:    +403  +409  +411  -413  -414  \n"
        +" 191:    +412  -413  +415  -416  +342  \n"
        +" 192:    +415  -417  -418  -402  +414  \n"
        +" 193:    +416  -417  -419  -397  +345  \n"
        +" 194:    +419  -418  +401  -406  +392  \n"
        +" 195:    +420  -421  +422  +423  -424  \n"
        +" 196:    +421  -425  -426  +427  -428  \n"
        +" 197:    +420  -425  +429  -418  -430  \n"
        +" 198:    +426  +429  -419  +390  -431  \n"
        +" 199:    +424  -432  +433  -401  -430  \n"
        +" 200:    +423  -432  -434  +435  -436  \n"
        +" 201:    +406  -433  -434  +437  -389  \n"
        +" 202:    +437  -385  -438  +439  -435  \n"
        +" 203:    +427  -440  +438  +391  -431  \n"
        +" 204:    +422  +436  -439  +440  -428  \n"
        +" 205:    +441  +422  -442  +443  -444  \n"
        +" 206:    +442  +436  +445  -446  -447  \n"
        +" 207:    +439  +445  -448  -449  +450  \n"
        +" 208:    +448  -446  +451  +452  -453  \n"
        +" 209:    +447  +451  -454  +455  -443  \n"
        +" 210:    +452  -456  -457  -458  +454  \n"
        +" 211:    +449  +453  -456  -459  -460  \n"
        +" 212:    +457  -459  +461  -462  +463  \n"
        +" 213:    +440  +464  -461  -460  +450  \n"
        +" 214:    +441  +428  +464  -462  -465  \n"
        +" 215:    +463  -458  +455  -444  +465  \n"
        +" 216:    +466  +467  -468  -469  -470  \n"
        +" 217:    +471  -472  +473  -467  -474  \n"
        +" 218:    +472  -475  -476  -477  +478  \n"
        +" 219:    +471  -475  -479  -458  +480  \n"
        +" 220:    +476  -479  -463  +481  -482  \n"
        +" 221:    +466  -474  -480  +455  -483  \n"
        +" 222:    +470  -484  -485  +444  -483  \n"
        +" 223:    +469  -486  +487  -488  +484  \n"
        +" 224:    +473  -468  -486  -489  +478  \n"
        +" 225:    +477  +482  +490  -487  -489  \n"
        +" 226:    +481  +490  -488  -485  +465  \n"
        +" 227:    +491  -492  -485  +441  +421  \n"
        +" 228:    +488  -493  -494  +495  -492  \n"
        +" 229:    +493  -490  -496  -497  +498  \n"
        +" 230:    +491  -495  -499  +500  +425  \n"
        +" 231:    +494  -498  +501  -502  +499  \n"
        +" 232:    +497  -503  -504  +505  -501  \n"
        +" 233:    +496  -481  +462  -506  +503  \n"
        +" 234:    +506  -464  -427  -507  +504  \n"
        +" 235:    +507  +426  -500  +502  -505  \n"
        +" 236:    +393  -508  -509  +507  +431  \n"
        +" 237:    +396  -508  +510  +323  -349  \n"
        +" 238:    +509  +510  -315  +511  -505  \n"
        +" 239:    +512  -417  -429  -500  +513  \n"
        +" 240:    +511  -502  +513  +514  -320  \n"
        +" 241:    +321  -343  +416  -512  +514  \n"
        +" 242:    +267  -319  -514  +515  -516  \n"
        +" 243:    +304  -340  +412  -517  +518  \n"
        +" 244:    +517  -413  -519  +520  -521  \n"
        +" 245:    +522  -515  +512  -415  -519  \n"
        +" 246:    +516  -522  +520  -523  +270  \n"
        +" 247:    +306  -271  +523  -521  +518  \n"
        +" 248:    +524  -525  +526  +527  -528  \n"
        +" 249:    +525  -529  +289  -530  -531  \n"
        +" 250:    +524  -529  +305  -518  -532  \n"
        +" 251:    +530  +308  -276  +251  -533  \n"
        +" 252:    +273  +523  -534  -535  -242  \n"
        +" 253:    +532  +521  -534  -536  -528  \n"
        +" 254:    +536  -535  +245  -537  +527  \n"
        +" 255:    +526  +537  +248  -533  -531  \n"
        +" 256:    +538  +539  -540  -541  +526  \n"
        +" 257:    +188  +542  -539  -543  -222  \n"
        +" 258:    +540  -542  +190  +544  -545  \n"
        +" 259:    +546  -547  -544  -192  +238  \n"
        +" 260:    +541  +545  +547  -548  -531  \n"
        +" 261:    +249  -533  +548  -546  -235  \n"
        +" 262:    +537  -246  -220  +543  -538  \n"
        +" 263:    +549  -550  -551  +218  -552  \n"
        +" 264:    +550  -553  +554  +555  -556  \n"
        +" 265:    +549  -553  +557  -558  +559  \n"
        +" 266:    +554  +560  -536  +561  -557  \n"
        +" 267:    +555  -562  +241  +535  -560  \n"
        +" 268:    +551  +556  -562  +247  -217  \n"
        +" 269:    +559  +552  -219  +543  -563  \n"
        +" 270:    +561  -558  +563  -538  +527  \n"
        +" 271:    +564  -565  -566  -567  +568  \n"
        +" 272:    +565  -569  -570  +571  +572  \n"
        +" 273:    +564  -569  -573  +574  -575  \n"
        +" 274:    +570  -573  -576  +524  -577  \n"
        +" 275:    +574  -578  -561  -528  +576  \n"
        +" 276:    +568  +575  -578  -558  -579  \n"
        +" 277:    +580  -539  -563  -579  +567  \n"
        +" 278:    +566  -572  -581  +540  -580  \n"
        +" 279:    +571  -581  -541  +525  -577  \n"
        +" 280:    +581  +582  -583  +584  -545  \n"
        +" 281:    +571  +582  +585  -586  -587  \n"
        +" 282:    +583  +585  -588  -589  +590  \n"
        +" 283:    +588  -586  +591  +286  -592  \n"
        +" 284:    +577  -529  +290  -591  -587  \n"
        +" 285:    +589  +592  -287  -593  -594  \n"
        +" 286:    +288  -593  +595  -548  +530  \n"
        +" 287:    +584  +547  -595  -594  +590  \n"
        +" 288:    +584  -544  +191  -161  +596  \n"
        +" 289:    +590  -596  -162  +597  -598  \n"
        +" 290:    +163  +599  -600  +601  -597  \n"
        +" 291:    +195  -164  +599  +602  -227  \n"
        +" 292:    +600  +602  +237  -603  -604  \n"
        +" 293:    +595  -546  +234  -603  +605  \n"
        +" 294:    +594  -605  -604  +601  -598  \n"
        +" 295:    +592  -291  +606  -607  -608  \n"
        +" 296:    +606  -609  -610  -611  +292  \n"
        +" 297:    +607  -609  -612  -613  +614  \n"
        +" 298:    +610  -612  -615  +616  -617  \n"
        +" 299:    +615  -613  +618  -601  +619  \n"
        +" 300:    +589  +608  -614  +618  -598  \n"
        +" 301:    +593  -294  +620  -621  +605  \n"
        +" 302:    +611  +617  -622  -620  -293  \n"
        +" 303:    +616  -622  -621  -604  +619  \n"
        +" 304:    +623  -76   -68   +226  -602  \n"
        +" 305:    +600  +623  +77   -624  -619  \n"
        +" 306:    +78   -624  +616  -625  -626  \n"
        +" 307:    +625  -622  -627  -628  +629  \n"
        +" 308:    +621  -627  -630  +233  -603  \n"
        +" 309:    +628  -630  -230  +64   -631  \n"
        +" 310:    +79   +626  -629  +631  -62   \n"
        +" 311:    +626  +632  -633  +634  -86   \n"
        +" 312:    +629  +632  +635  -636  -637  \n"
        +" 313:    +633  +635  -638  -639  +640  \n"
        +" 314:    +638  -636  -641  +642  -643  \n"
        +" 315:    +631  -61   +644  +641  -637  \n"
        +" 316:    +644  +642  -645  -646  -58   \n"
        +" 317:    +639  +643  -645  -647  -648  \n"
        +" 318:    +647  -646  -56   +93   -649  \n"
        +" 319:    +634  -90   -649  -648  +640  \n"
        +" 320:    +650  -651  +652  +634  -85   \n"
        +" 321:    +653  +654  +655  -651  -656  \n"
        +" 322:    +657  -658  +659  +654  -660  \n"
        +" 323:    +661  -662  -663  +664  +658  \n"
        +" 324:    +662  -665  +91   -649  -666  \n"
        +" 325:    +92   +665  -661  -657  -667  \n"
        +" 326:    +87   +650  -655  -660  -667  \n"
        +" 327:    +648  -666  -663  +668  -669  \n"
        +" 328:    +664  +659  -653  +670  -668  \n"
        +" 329:    +640  -652  -656  +670  -669  \n"
        +" 330:    +671  -635  -672  -673  +328  \n"
        +" 331:    +674  -675  +672  -633  -652  \n"
        +" 332:    +673  +675  -676  -677  -329  \n"
        +" 333:    +676  -674  -656  +678  +679  \n"
        +" 334:    +678  +680  -681  +682  -670  \n"
        +" 335:    +677  -679  +680  +683  -325  \n"
        +" 336:    +681  +683  +313  -684  -685  \n"
        +" 337:    +638  -671  +318  -684  +686  \n"
        +" 338:    +639  -686  -685  +682  -669  \n"
        +" 339:    +643  -687  +688  -689  +686  \n"
        +" 340:    +687  -645  -690  -691  +692  \n"
        +" 341:    +693  -694  +690  -647  -666  \n"
        +" 342:    +695  +696  -694  -691  +697  \n"
        +" 343:    +698  +696  -693  -663  +699  \n"
        +" 344:    +700  -701  +682  -668  +699  \n"
        +" 345:    +689  -702  -703  -701  +685  \n"
        +" 346:    +688  -702  -704  -697  +692  \n"
        +" 347:    +704  -703  -700  +698  -695  \n"
        +" 348:    +461  -506  +705  -706  -707  \n"
        +" 349:    +479  -708  -709  +710  -457  \n"
        +" 350:    +708  -476  +711  +695  -712  \n"
        +" 351:    +496  -482  +711  +704  -713  \n"
        +" 352:    +705  -714  +703  -713  -503  \n"
        +" 353:    +714  -706  +715  -716  +700  \n"
        +" 354:    +707  +715  -717  +710  -459  \n"
        +" 355:    +712  -698  +716  -717  +709  \n"
        +" 356:    +718  +719  -693  +662  -720  \n"
        +" 357:    +721  +712  +696  -719  -722  \n"
        +" 358:    +664  +723  +724  -716  -699  \n"
        +" 359:    +658  -725  -726  +727  -723  \n"
        +" 360:    +661  -720  -728  +729  +725  \n"
        +" 361:    +718  -722  +730  -731  +728  \n"
        +" 362:    +721  -709  -732  +733  -730  \n"
        +" 363:    +717  -724  -727  -734  +732  \n"
        +" 364:    +734  +726  -729  +731  -733  \n"
        +" 365:    +734  +735  +736  -737  -738  \n"
        +" 366:    +726  +739  -740  +741  -735  \n"
        +" 367:    +742  -743  -739  -729  +744  \n"
        +" 368:    +745  -743  -740  -746  +747  \n"
        +" 369:    +748  -736  -741  -746  +749  \n"
        +" 370:    +750  -751  -747  +749  +752  \n"
        +" 371:    +742  -745  +751  -753  -754  \n"
        +" 372:    +755  +753  -750  -756  -757  \n"
        +" 373:    +737  -748  +752  -756  -758  \n"
        +" 374:    +738  +758  -757  +759  -733  \n"
        +" 375:    +731  -759  +755  -754  -744  \n"
        +" 376:    +18   -760  -718  +761  -43   \n"
        +" 377:    +19   -762  -763  +722  +760  \n"
        +" 378:    +15   -762  +764  +765  -766  \n"
        +" 379:    +730  -759  +767  -764  -763  \n"
        +" 380:    +767  +765  -768  -769  -755  \n"
        +" 381:    +8    +766  -768  -770  -33   \n"
        +" 382:    +769  -770  +40   -771  +754  \n"
        +" 383:    +761  +44   -771  -744  +728  \n"
        +" 384:    +761  -45   +94   +665  -720  \n"
        +" 385:    +41   -771  +742  -772  -110  \n"
        +" 386:    +772  -743  -773  -774  +113  \n"
        +" 387:    +657  -725  +739  -773  +775  \n"
        +" 388:    +667  -775  -774  +99   -88   \n"
        +" 389:    +776  -777  +778  -650  +84   \n"
        +" 390:    +777  -779  -780  -781  +782  \n"
        +" 391:    +98   +783  +779  -776  -82   \n"
        +" 392:    +780  -783  -97   +784  -785  \n"
        +" 393:    +660  -786  +787  -788  +775  \n"
        +" 394:    +655  -778  -782  +789  +786  \n"
        +" 395:    +781  +785  -790  -787  -789  \n"
        +" 396:    +784  -790  -788  -774  +100  \n"
        +" 397:    +790  -791  -792  +793  +794  \n"
        +" 398:    +784  -791  +795  +147  -114  \n"
        +" 399:    +792  +795  -146  +796  -797  \n"
        +" 400:    +144  -111  +772  -745  +798  \n"
        +" 401:    +740  -773  +788  -794  +799  \n"
        +" 402:    +793  +799  -746  +800  -797  \n"
        +" 403:    +796  -800  +747  +798  -139  \n"
        +" 404:    +801  -750  -802  -803  +804  \n"
        +" 405:    +801  -751  +798  +140  -805  \n"
        +" 406:    +806  -807  +808  -800  +749  \n"
        +" 407:    +809  -810  -811  -807  +812  \n"
        +" 408:    +813  -810  -814  -803  +815  \n"
        +" 409:    +752  -802  +814  -811  -806  \n"
        +" 410:    +816  -141  -805  -804  +815  \n"
        +" 411:    +813  -809  +817  +142  -816  \n"
        +" 412:    +143  -817  -812  +808  -796  \n"
        +" 413:    +818  -819  -820  -821  +792  \n"
        +" 414:    +819  -822  -823  +824  +825  \n"
        +" 415:    +150  -826  +822  -818  +795  \n"
        +" 416:    +826  +152  -827  -828  +823  \n"
        +" 417:    +827  -155  -817  +829  -830  \n"
        +" 418:    +829  -831  -832  -833  +812  \n"
        +" 419:    +830  -831  -834  -824  +828  \n"
        +" 420:    +820  -825  +834  -832  -835  \n"
        +" 421:    +821  +835  -833  +808  -797  \n"
        +" 422:    +821  +836  -837  -838  -793  \n"
        +" 423:    +836  -839  +840  -841  -835  \n"
        +" 424:    +837  -839  -842  +843  +844  \n"
        +" 425:    +840  +845  -846  -847  +842  \n"
        +" 426:    +807  +848  -845  -841  -833  \n"
        +" 427:    +846  -848  -806  +748  -849  \n"
        +" 428:    +741  -850  +844  -838  +799  \n"
        +" 429:    +847  +849  -736  -850  -843  \n"
        +" 430:    +460  +707  -851  +852  -853  \n"
        +" 431:    +854  +724  -715  -851  +855  \n"
        +" 432:    +727  -854  -856  +850  -735  \n"
        +" 433:    +843  +856  -855  +852  -857  \n"
        +" 434:    +449  +858  -847  +857  -853  \n"
        +" 435:    +849  -737  +859  -453  +858  \n"
        +" 436:    +710  +456  -859  -738  +732  \n"
        +" 437:    +860  -475  -708  -721  +763  \n"
        +" 438:    +454  +861  +862  -863  -480  \n"
        +" 439:    +859  -452  +861  -864  -758  \n"
        +" 440:    +767  +865  -862  -864  -757  \n"
        +" 441:    +764  +865  -863  +471  -860  \n"
        +" 442:    +16   -762  +860  -472  -866  \n"
        +" 443:    +765  -867  +868  +869  -865  \n"
        +" 444:    +7    +766  -867  +870  -871  \n"
        +" 445:    +872  -873  +874  -870  +868  \n"
        +" 446:    +875  -873  -876  +877  -467  \n"
        +" 447:    +12   -878  +876  +874  -871  \n"
        +" 448:    +17   -878  +877  -473  -866  \n"
        +" 449:    +863  -869  +872  -875  -474  \n"
        +" 450:    +879  +880  -875  -466  +881  \n"
        +" 451:    +872  -880  -882  -883  +884  \n"
        +" 452:    +885  -886  +887  +879  -882  \n"
        +" 453:    +888  -886  -889  +447  +890  \n"
        +" 454:    +881  -887  -889  +443  -483  \n"
        +" 455:    +891  -890  +451  +861  -892  \n"
        +" 456:    +884  +869  -862  -892  -893  \n"
        +" 457:    +883  +885  -888  -891  -893  \n"
        +" 458:    +894  -895  -896  +897  +898  \n"
        +" 459:    +895  -899  -900  +813  -901  \n"
        +" 460:    +902  +894  -899  -903  +904  \n"
        +" 461:    +903  -900  -815  +905  -906  \n"
        +" 462:    +896  +901  -810  -907  -908  \n"
        +" 463:    +909  +814  -907  +910  -891  \n"
        +" 464:    +897  +911  -888  -910  -908  \n"
        +" 465:    +902  -898  +911  -885  +912  \n"
        +" 466:    +905  -913  +893  +909  -803  \n"
        +" 467:    +912  -904  +906  -913  +883  \n"
        +" 468:    +914  -915  +916  +917  -918  \n"
        +" 469:    +919  +915  +920  -870  -921  \n"
        +" 470:    +874  -920  -914  -922  +923  \n"
        +" 471:    +918  +924  -925  -926  +922  \n"
        +" 472:    +917  +924  -927  -904  +928  \n"
        +" 473:    +929  -882  +912  +927  -925  \n"
        +" 474:    +873  -880  -929  -926  +923  \n"
        +" 475:    +921  +868  -884  +913  -930  \n"
        +" 476:    +916  -928  +906  -930  +919  \n"
        +" 477:    +916  +931  -932  +124  -933  \n"
        +" 478:    +931  +934  -935  -903  +928  \n"
        +" 479:    +932  +934  -936  -158  +129  \n"
        +" 480:    +935  -936  +156  -816  +900  \n"
        +" 481:    +145  -805  -937  +938  -133  \n"
        +" 482:    +937  -804  +905  -930  +939  \n"
        +" 483:    +132  -938  -939  +919  +933  \n"
        +" 484:    +5    -30   +118  +940  -941  \n"
        +" 485:    +940  +942  -915  +933  -125  \n"
        +" 486:    +6    +871  -920  -942  -941  \n"
        +" 487:    +939  +943  +768  -867  -921  \n"
        +" 488:    +135  -938  +943  -770  -32   \n"
        +" 489:    +943  -769  +753  -801  -937  \n"
        +" 490:    +756  -802  -909  +892  -864  \n"
        +" 491:    +811  -907  -944  +945  -848  \n"
        +" 492:    +910  -890  +446  -946  +944  \n"
        +" 493:    +946  -448  +858  +846  -945  \n"
        +" 494:    +947  +948  -384  -437  -949  \n"
        +" 495:    +948  -382  -950  +840  -951  \n"
        +" 496:    +842  +950  -387  +952  -857  \n"
        +" 497:    +386  -438  -450  +853  -952  \n"
        +" 498:    +435  +445  -946  -953  +949  \n"
        +" 499:    +945  -845  -951  -947  +953  \n"
        +" 500:    +896  -954  -955  +956  -957  \n"
        +" 501:    +954  +901  -809  +829  -958  \n"
        +" 502:    +959  -957  +908  -944  -953  \n"
        +" 503:    +951  -841  +832  -960  -961  \n"
        +" 504:    +955  +958  -831  -960  -962  \n"
        +" 505:    +962  -961  -947  +959  -956  \n"
        +" 506:    +963  -400  -433  -964  +965  \n"
        +" 507:    +963  -404  +966  -967  -968  \n"
        +" 508:    +405  +966  -969  -970  -379  \n"
        +" 509:    +969  -967  -971  +962  +972  \n"
        +" 510:    +965  +968  -971  +956  -973  \n"
        +" 511:    +970  -972  -961  +948  -381  \n"
        +" 512:    +434  -964  +973  -959  +949  \n"
        +" 513:    +442  +423  +974  -975  -889  \n"
        +" 514:    +974  -976  -977  +964  +432  \n"
        +" 515:    +911  -886  +975  -976  +978  \n"
        +" 516:    +973  -957  +897  -978  -977  \n"
        +" 517:    +979  -980  -981  -965  +977  \n"
        +" 518:    +980  -982  -983  +984  +985  \n"
        +" 519:    +986  -898  -978  +979  -982  \n"
        +" 520:    +987  -895  -954  +988  -989  \n"
        +" 521:    +955  +988  -990  -991  -971  \n"
        +" 522:    +984  +992  +990  -989  -993  \n"
        +" 523:    +981  -985  +992  -991  -968  \n"
        +" 524:    +986  +894  -987  -993  +983  \n"
        +" 525:    +917  -994  +995  +996  -931  \n"
        +" 526:    +994  +924  -997  -998  +999  \n"
        +" 527:    +997  -927  +902  -986  -1000 \n"
        +" 528:    +899  -987  -1001 +1002 -935  \n"
        +" 529:    +1001 -993  +1003 -1004 +1005 \n"
        +" 530:    +998  +1000 -983  +1003 -1006 \n"
        +" 531:    +995  -1007 +1004 -1006 +999  \n"
        +" 532:    +996  +934  -1002 -1005 +1007 \n"
        +" 533:    +932  -996  +1008 -182  +123  \n"
        +" 534:    +1002 -936  -157  +1009 -1010 \n"
        +" 535:    +1008 -185  -1011 -1012 +1007 \n"
        +" 536:    +167  +1013 -1014 +1011 -186  \n"
        +" 537:    +1009 -1015 -1013 -171  +154  \n"
        +" 538:    +1005 +1010 -1015 -1014 -1012 \n"
        +" 539:    +992  -1016 +1017 -564  +1018 \n"
        +" 540:    +990  -1019 -1020 +1021 +1016 \n"
        +" 541:    +989  -1019 +1022 -1010 +1001 \n"
        +" 542:    +1023 +1024 +1015 -1022 -1020 \n"
        +" 543:    +1025 -566  +1026 +1014 -1024 \n"
        +" 544:    +1017 -565  -1025 -1023 +1021 \n"
        +" 545:    +1026 -1012 +1004 -1027 +567  \n"
        +" 546:    +984  -1018 -568  +1027 -1003 \n"
        +" 547:    +1028 -1029 -549  -1030 +998  \n"
        +" 548:    +1029 -1031 -1032 +1033 +553  \n"
        +" 549:    +1028 -1031 +1034 -982  -1000 \n"
        +" 550:    +1032 +1034 -980  -1035 -1036 \n"
        +" 551:    +1035 -985  -1018 +575  -1037 \n"
        +" 552:    +1033 +557  +578  -1037 -1036 \n"
        +" 553:    +1027 -1006 +1030 -559  -579  \n"
        +" 554:    +1008 -183  +205  -1038 +995  \n"
        +" 555:    +187  -1011 -1026 +580  -542  \n"
        +" 556:    +1038 -221  -552  -1030 +999  \n"
        +" 557:    +1039 +918  -994  +1038 -204  \n"
        +" 558:    +925  -997  +1028 -1040 +1041 \n"
        +" 559:    +1040 -1029 -550  -1042 +1043 \n"
        +" 560:    +551  -1042 +1044 -1045 +216  \n"
        +" 561:    +926  -1041 -1043 +1044 -1046 \n"
        +" 562:    +1046 -1045 +206  +1039 -922  \n"
        +" 563:    +13   -878  -1047 +1048 +1049 \n"
        +" 564:    +876  -923  +1046 -1050 +1047 \n"
        +" 565:    +1039 +914  -942  +1051 -203  \n"
        +" 566:    +9    +1052 +200  -1051 -941  \n"
        +" 567:    +14   -1049 -1053 +207  -1052 \n"
        +" 568:    +1050 -1045 +208  +1053 -1048 \n"
        +" 569:    +1042 +556  +1054 -1055 -1056 \n"
        +" 570:    +1054 -1057 -1058 +240  +562  \n"
        +" 571:    +1055 -1057 +1059 -1060 -1061 \n"
        +" 572:    +1058 +1059 -1062 -1063 -259  \n"
        +" 573:    +1064 +1060 -1062 -1065 -1048 \n"
        +" 574:    +1065 -1063 +257  -209  +1053 \n"
        +" 575:    +1044 -1050 +1064 -1061 -1056 \n"
        +" 576:    +1041 +929  -879  +1066 -1067 \n"
        +" 577:    +877  -468  -1068 -1064 +1047 \n"
        +" 578:    +1068 -469  +1069 -1070 +1061 \n"
        +" 579:    +1066 -1071 -1069 -470  +881  \n"
        +" 580:    +1043 +1067 -1071 -1070 -1056 \n"
        +" 581:    +1040 -1031 -1072 +1073 -1067 \n"
        +" 582:    +1032 -1072 -1074 +1075 -1076 \n"
        +" 583:    +1074 +1073 -1071 -1077 -1078 \n"
        +" 584:    +1077 -1070 +1055 -1079 -1080 \n"
        +" 585:    +1033 +554  -1081 +1082 -1076 \n"
        +" 586:    +1079 -1054 -555  -1081 +1083 \n"
        +" 587:    +1075 -1082 +1083 -1080 -1078 \n"
        +" 588:    +1078 +1084 -491  -420  -1085 \n"
        +" 589:    +1086 -1087 -1080 +1084 -495  \n"
        +" 590:    +1087 +1088 -522  -1089 +1083 \n"
        +" 591:    +1086 +1088 -515  -513  +499  \n"
        +" 592:    +1085 +430  -402  +1090 -1075 \n"
        +" 593:    +1089 +519  -414  +1090 -1082 \n"
        +" 594:    +1036 +1091 -403  +1090 -1076 \n"
        +" 595:    +1091 +409  -1092 +574  -1037 \n"
        +" 596:    +1092 +411  -517  -532  +576  \n"
        +" 597:    +520  -534  -560  -1081 +1089 \n"
        +" 598:    +268  +516  -1088 -1093 -1094 \n"
        +" 599:    +1093 -1087 +1079 -1057 +1095 \n"
        +" 600:    +1058 +1095 -1094 +274  -244  \n"
        +" 601:    +1096 -1097 -1095 +1059 -1098 \n"
        +" 602:    +1099 -1100 -1097 -1094 +269  \n"
        +" 603:    +1101 -1100 -1096 -1102 +1103 \n"
        +" 604:    +1098 -1062 -1104 -1105 +1102 \n"
        +" 605:    +1104 -1063 +258  +1106 -1107 \n"
        +" 606:    +1099 -1101 +1108 -1109 +277  \n"
        +" 607:    +1107 +1110 -1108 -1103 +1105 \n"
        +" 608:    +278  +1109 -1110 -1106 -255  \n"
        +" 609:    +1111 -281  +252  -231  +630  \n"
        +" 610:    +628  +1111 +282  -1112 -637  \n"
        +" 611:    +280  -1112 -641  +1113 -1109 \n"
        +" 612:    +254  -229  +66   +1114 -1106 \n"
        +" 613:    +1114 +1110 -1113 -644  +59   \n"
        +" 614:    +26   -60   +1114 -1107 -1115 \n"
        +" 615:    +21   -55   +646  -690  -1116 \n"
        +" 616:    +1113 -1108 -1117 +687  -642  \n"
        +" 617:    +1117 -1103 +1118 -1119 +692  \n"
        +" 618:    +25   +1115 -1105 +1118 -1120 \n"
        +" 619:    +24   +1116 -691  +1119 -1120 \n"
        +" 620:    +711  -697  +1119 -1121 +477  \n"
        +" 621:    +20   -760  +719  -694  -1116 \n"
        +" 622:    +23   +866  -478  +1121 -1120 \n"
        +" 623:    +1068 -486  +1122 +1098 -1060 \n"
        +" 624:    +1122 -1102 +1118 -1121 +489  \n"
        +" 625:    +28   -1049 +1065 -1104 -1115 \n"
        +" 626:    +10   +1052 -201  +225  -73   \n"
        +" 627:    +940  +1051 -199  +178  -119  \n"
        +" 628:    +121  -181  +224  -74   +35   \n"
        +" 629:    +175  -101  +80   -623  -599  \n"
        +" 630:    +103  -174  +1123 +1124 -783  \n"
        +" 631:    +173  +1123 -1125 +618  -597  \n"
        +" 632:    +1124 +779  -1126 -613  +1125 \n"
        +" 633:    +1126 -776  +81   -624  +615  \n"
        +" 634:    +1126 -777  +1127 +1128 -612  \n"
        +" 635:    +778  -651  +674  -1129 -1127 \n"
        +" 636:    +1129 -675  +1130 +610  -1128 \n"
        +" 637:    +625  -617  -1130 +672  -632  \n"
        +" 638:    +611  -1130 -673  +331  -300  \n"
        +" 639:    +279  -1112 +636  -671  +316  \n"
        +" 640:    +620  -627  +1111 -284  +309  \n"
        +" 641:    +1131 -1101 -1117 +688  -1132 \n"
        +" 642:    +312  -684  +689  -1132 +1133 \n"
        +" 643:    +317  -266  +1099 -1131 +1133 \n"
        +" 644:    +1134 +1133 -311  +511  -501  \n"
        +" 645:    +1134 +1131 -1100 -1135 -498  \n"
        +" 646:    +1135 -1097 +1093 -1086 +494  \n"
        +" 647:    +487  -493  +1135 -1096 -1122 \n"
        +" 648:    +1069 -1077 +1084 -492  +484  \n"
        +" 649:    +975  -1136 +1073 -1066 -887  \n"
        +" 650:    +1074 +1136 -974  -424  -1085 \n"
        +" 651:    +976  -1136 +1072 +1034 -979  \n"
        +" 652:    +1035 -981  +963  -399  -1091 \n"
        +" 653:    +991  -1016 +1137 +1138 -967  \n"
        +" 654:    +1017 -569  +1139 -1140 -1137 \n"
        +" 655:    +1139 -1141 +408  -1092 +573  \n"
        +" 656:    +1140 -1141 -407  +966  -1138 \n"
        +" 657:    +1142 -359  -1143 -1144 +1145 \n"
        +" 658:    +1146 -1147 -333  +365  -1142 \n"
        +" 659:    +1141 +1148 -1147 -334  +410  \n"
        +" 660:    +1148 -1146 -1145 +1149 +1140 \n"
        +" 661:    +1149 +1138 -969  -1150 -1144 \n"
        +" 662:    +1143 -362  -378  +970  -1150 \n"
        +" 663:    +820  -1151 -353  +1152 -836  \n"
        +" 664:    +1151 -825  +1153 +1143 -358  \n"
        +" 665:    +1150 -972  +960  -834  +1153 \n"
        +" 666:    +1152 -839  +950  +383  -364  \n"
        +" 667:    +1152 -837  -1154 -1155 +357  \n"
        +" 668:    +398  -375  +1156 -1157 +1158 \n"
        +" 669:    +1159 +1160 -1156 -373  +1155 \n"
        +" 670:    +1161 +1160 -1157 -1162 +855  \n"
        +" 671:    +952  -852  +1162 +1158 -395  \n"
        +" 672:    +1154 -844  +856  +1161 -1159 \n"
        +" 673:    +654  -786  -1163 +1159 -1164 \n"
        +" 674:    +794  -787  -1163 +1154 -838  \n"
        +" 675:    +659  +1164 -1161 +854  -723  \n"
        +" 676:    +653  +1164 +1160 -1165 -678  \n"
        +" 677:    +1157 -1165 +680  +1166 -1167 \n"
        +" 678:    +1166 -1168 -714  -701  +681  \n"
        +" 679:    +1162 +1167 -1168 -706  -851  \n"
        +" 680:    +394  -508  +1169 -1167 +1158 \n"
        +" 681:    +705  +1168 -1169 -509  +504  \n"
        +" 682:    +702  -1132 -1134 +497  +713  \n"
        +" 683:    +1169 -1166 +683  -314  -510  \n"
        +" 684:    +1170 -679  +1165 -1156 +372  \n"
        +" 685:    +677  -1170 +374  -350  -326  \n"
        +" 686:    +1171 +1129 -676  -1170 -371  \n"
        +" 687:    +338  -368  +1172 -606  +295  \n"
        +" 688:    +609  -1172 -369  +1171 +1128 \n"
        +" 689:    +1173 -370  +1171 -1127 -782  \n"
        +" 690:    +1172 -607  +1174 -1175 +367  \n"
        +" 691:    +1125 +1176 +1177 -1174 -614  \n"
        +" 692:    +780  -1124 +1176 -1178 -1179 \n"
        +" 693:    +361  +1175 -1177 -1178 -1180 \n"
        +" 694:    +781  +1179 -1180 +355  -1173 \n"
        +" 695:    +785  -791  +818  -1181 -1179 \n"
        +" 696:    +1181 -819  -1151 -354  +1180 \n"
        +" 697:    +1163 -789  +1173 -356  +1155 \n"
        +" 698:    +822  -1181 +1178 -1182 -1183 \n"
        +" 699:    +1184 +1142 -360  +1175 -1185 \n"
        +" 700:    +1186 -1187 +1144 -1153 -824  \n"
        +" 701:    +1145 -1184 -1188 +1189 -1187 \n"
        +" 702:    +1188 +1185 -1177 -1182 -1190 \n"
        +" 703:    +1183 -1190 +1189 -1186 +823  \n"
        +" 704:    +1024 -1013 +166  -1191 -1192 \n"
        +" 705:    +1191 +169  -1193 -1190 +1194 \n"
        +" 706:    +170  -1193 -1183 +826  -149  \n"
        +" 707:    +1023 +1192 -1194 +1189 -1195 \n"
        +" 708:    +1196 -1020 +1195 -1186 +828  \n"
        +" 709:    +1009 -1022 -1196 +827  -153  \n"
        +" 710:    +988  -1019 -1196 +830  -958  \n"
        +" 711:    +1195 -1187 +1149 -1137 -1021 \n"
        +" 712:    +570  +1139 +1148 -1197 -587  \n"
        +" 713:    +1197 -1146 -1184 +1198 -586  \n"
        +" 714:    +1188 +1198 -585  -1199 -1194 \n"
        +" 715:    +1025 -572  +582  -1199 -1192 \n"
        +" 716:    +583  -1199 +1191 -165  +596  \n"
        +" 717:    +588  -1198 +1185 -1174 -608  \n"
        +" 718:    +1193 -172  +1123 +1176 -1182 \n"
        +" 719:    +1147 -1197 +591  +297  -337  \n"
        +"120 3-cells:\n"
        +"   0:    +0   +1   -2   -3   -4   -5   +6   -7   +8   -9   -10  +11  \n"
        +"   1:    -0   -12  +13  +14  +15  +16  -17  +18  -19  +20  +21  -22  \n"
        +"   2:    -11  +23  -24  +25  -26  +27  +28  -29  -30  +31  +22  -32  \n"
        +"   3:    +33  -34  +35  -36  -37  +38  +39  +40  +41  +42  +32  +19  \n"
        +"   4:    -43  +44  +45  +46  -41  -21  +47  -48  +49  +50  -51  -52  \n"
        +"   5:    +53  -54  +55  +56  +57  -58  +59  -14  +52  +60  +61  -62  \n"
        +"   6:    -63  +64  -65  +66  -67  +68  +69  -70  -71  +62  +72  +51  \n"
        +"   7:    +73  -74  -75  +76  -77  -78  +79  -45  -61  -72  -80  +81  \n"
        +"   8:    +82  +83  -55  -84  +85  -86  +87  +88  -81  -89  -90  +91  \n"
        +"   9:    +92  -93  -94  +95  -96  +97  +98  -99  +100 -101 +102 -91  \n"
        +"  10:    -103 +104 -28  +105 -106 -107 +108 -109 -110 +90  -102 +111 \n"
        +"  11:    +112 -113 -114 -115 -116 -111 +117 -118 +96  -119 +120 -121 \n"
        +"  12:    +122 -123 +124 -125 +126 +121 +127 +128 -129 +130 -131 +132 \n"
        +"  13:    -133 -134 +135 -136 -137 +138 -139 -140 -141 +142 -132 +143 \n"
        +"  14:    -144 +145 -122 +146 -147 -148 -149 +150 -151 -152 -143 -153 \n"
        +"  15:    -154 +155 -156 +137 +157 +158 -159 -160 +153 -161 -162 +163 \n"
        +"  16:    +164 +165 -166 -167 -168 -169 +170 -171 -172 -173 +174 -163 \n"
        +"  17:    +175 -176 -177 -178 -179 -180 +181 +182 +183 -174 +159 +184 \n"
        +"  18:    +185 +186 -187 +188 +189 -190 +191 -192 +193 +194 -184 -158 \n"
        +"  19:    -195 -196 +197 -198 -194 -199 +200 -201 +202 +179 +203 +204 \n"
        +"  20:    +205 -204 +206 -207 -208 +209 +210 -211 +212 +213 -214 -215 \n"
        +"  21:    +216 +217 +218 -219 +220 +215 -221 +222 +223 -224 +225 -226 \n"
        +"  22:    -227 +228 +229 +226 +214 +196 +230 +231 +232 +233 +234 +235 \n"
        +"  23:    -181 +236 -237 +238 +148 +160 -193 +239 +198 -235 -240 +241 \n"
        +"  24:    +147 -242 -241 -157 +243 -191 +244 -245 -246 -247 -124 +139 \n"
        +"  25:    -248 -249 +250 +140 -251 -126 +247 -252 +253 +254 +115 +255 \n"
        +"  26:    +256 +257 +258 -87  +259 +260 +261 -255 +262 +114 +110 +101 \n"
        +"  27:    +263 +264 -265 -266 -267 +268 +113 -262 -98  +269 +270 -254 \n"
        +"  28:    -271 -272 +273 -274 -275 +276 -270 -277 -256 -278 +279 +248 \n"
        +"  29:    -279 -280 +281 -282 -283 -284 +249 +133 -285 +286 +287 -260 \n"
        +"  30:    -287 -259 +288 -88  -73  +289 +290 -291 +292 -108 -293 -294 \n"
        +"  31:    +285 -295 +134 +296 -297 +298 +299 -300 +294 +301 +302 -303 \n"
        +"  32:    -292 -304 -33  +305 -306 +303 -307 +308 -309 +109 -27  -310 \n"
        +"  33:    +310 -311 +312 -313 -314 -315 -25  +316 -317 -318 -38  +319 \n"
        +"  34:    -319 +37  +320 -321 +322 +323 +324 +325 -326 -327 -328 +329 \n"
        +"  35:    +313 +330 +331 +332 +333 -329 -334 +335 -336 -150 +337 +338 \n"
        +"  36:    +317 -339 -340 -341 +342 -343 +327 +344 -345 +346 +347 -338 \n"
        +"  37:    +348 -233 -220 -349 -350 +351 -347 -352 -353 +354 -355 -212 \n"
        +"  38:    -323 -356 -357 +355 +343 +358 +359 +360 +361 +362 +363 +364 \n"
        +"  39:    +365 +366 +367 -368 +369 -370 -371 -372 +373 +374 +375 -364 \n"
        +"  40:    +5   -376 -377 +378 +379 -375 -380 +381 -382 -16  +383 -361 \n"
        +"  41:    -325 +384 -18  +385 -383 -367 +386 -387 -360 -388 +48  -40  \n"
        +"  42:    +326 +389 +390 +391 +392 +43  +36  +388 +393 +394 +395 -396 \n"
        +"  43:    +396 +397 -398 +399 +65  -49  +400 -386 +368 +401 -402 -403 \n"
        +"  44:    +404 +370 -405 +403 -406 +407 -408 -409 +410 +63  +411 +412 \n"
        +"  45:    +413 +414 +415 +416 -68  +417 -412 -418 +419 +420 +421 -399 \n"
        +"  46:    +422 -423 +424 +425 +426 +427 +406 -421 +402 -428 +429 -369 \n"
        +"  47:    +430 -354 -431 -363 -432 -433 -434 +435 -365 -429 +436 +211 \n"
        +"  48:    -436 -210 +349 -437 +219 +438 -439 +440 -441 -379 -374 -362 \n"
        +"  49:    +4   -378 +442 +441 +443 -444 -445 +446 +447 -448 -217 +449 \n"
        +"  50:    +450 +451 -452 +453 -454 -209 +455 -438 -456 -449 +221 +457 \n"
        +"  51:    -458 -459 +460 +461 +408 -462 +463 +464 -465 -457 -466 +467 \n"
        +"  52:    -468 -469 -470 -471 +472 +473 -451 +474 +445 -475 -467 +476 \n"
        +"  53:    +477 -478 +479 -480 +71  -410 +481 +482 -461 -476 +483 -57  \n"
        +"  54:    -1   -484 +12  +58  +485 +486 +444 -381 -487 +488 -483 +469 \n"
        +"  55:    +489 +382 -488 -481 +405 +371 -400 -47  -15  -385 -59  -64  \n"
        +"  56:    -440 -443 +487 -489 +380 +372 -404 +490 +466 +475 +456 -482 \n"
        +"  57:    -435 -373 -427 +491 -463 +409 -490 -455 +492 +208 +439 +493 \n"
        +"  58:    +177 -494 +495 +496 +497 -202 +207 -498 -493 +434 -425 -499 \n"
        +"  59:    -500 -501 +462 -407 -491 +502 +499 -426 +503 +418 -504 -505 \n"
        +"  60:    -506 -186 +507 -508 -509 +510 +505 -511 +494 +178 +201 +512 \n"
        +"  61:    +513 -200 -514 +515 -453 -206 +498 -492 -502 +516 -464 -512 \n"
        +"  62:    -517 -518 +519 +458 -520 +500 +521 +522 -523 -510 -516 -524 \n"
        +"  63:    +525 +526 -472 +527 -460 +524 -528 -529 +530 -531 -532 +478 \n"
        +"  64:    +533 +532 -479 +534 +70  -56  +80  +84  -535 -536 -537 +538 \n"
        +"  65:    -522 +539 +540 -541 -542 -543 +271 -544 +545 -538 +529 +546 \n"
        +"  66:    +547 +548 -549 +550 +518 -530 -546 +551 -276 -552 +265 +553 \n"
        +"  67:    -554 -85  +535 -555 -545 +531 -553 -556 +99  -269 -257 +277 \n"
        +"  68:    -557 +471 -526 +558 -547 +559 -263 -560 +561 +562 -100 +556 \n"
        +"  69:    +3   -447 +563 +564 +470 -486 +565 +566 +567 +568 -562 +94  \n"
        +"  70:    +560 +569 -268 -570 +571 -572 +573 +574 +119 -97  -568 -575 \n"
        +"  71:    -561 -576 -474 -450 -446 -564 +575 +577 -216 +578 +579 -580 \n"
        +"  72:    -559 -548 +581 -582 -583 +580 -584 -569 -264 +585 -586 +587 \n"
        +"  73:    -588 +589 +590 -591 +245 -239 +192 -197 -230 -592 +593 -587 \n"
        +"  74:    +594 -595 +190 -596 -244 -593 -585 +266 +597 -253 +275 +552 \n"
        +"  75:    +246 -590 -598 -599 +586 +570 +600 +125 +252 -597 +267 -112 \n"
        +"  76:    -600 -601 +602 -603 -604 +572 -605 -120 -127 -606 -607 -608 \n"
        +"  77:    +309 +609 +129 -610 +611 +315 +26  +106 -117 -612 +613 +608 \n"
        +"  78:    -8   +614 +24  -615 -316 -616 +607 -613 -617 +618 -619 +340 \n"
        +"  79:    +7   -442 +377 +437 -218 +350 -620 -342 +357 +621 +619 -622 \n"
        +"  80:    +10  +448 -563 -577 +224 -623 +624 +604 -573 -625 -618 +622 \n"
        +"  81:    +9   +625 -567 -574 +605 +118 -95  -105 +612 +29  -614 -626 \n"
        +"  82:    +2   +484 -13  -53  -627 -566 +626 -92  +103 -628 -82  +30  \n"
        +"  83:    +628 +89  -60  -79  +291 +629 -46  -42  +304 -31  -20  -104 \n"
        +"  84:    -290 -629 -44  +78  -630 +631 +632 -391 +633 +34  -305 -299 \n"
        +"  85:    +306 -633 -389 +634 +635 -320 -331 +636 +637 -298 +311 -35  \n"
        +"  86:    +307 -302 -637 +638 -330 -312 -639 +610 -640 -130 +152 -142 \n"
        +"  87:    -286 -301 +141 +640 -308 -609 +131 +116 +107 +293 -261 +251 \n"
        +"  88:    +314 +639 -611 +616 +606 -641 +339 +642 -337 -145 -128 -643 \n"
        +"  89:    -644 +643 +123 -146 +242 +240 -231 +645 -602 +646 +598 +591 \n"
        +"  90:    -578 -223 +623 +647 -228 -646 +601 +599 -589 +584 -571 +648 \n"
        +"  91:    -579 +583 -649 -650 -513 +195 +227 -648 +588 -222 -205 +454 \n"
        +"  92:    +650 +651 +582 +592 +199 +514 +517 -550 -652 +506 -185 -594 \n"
        +"  93:    +652 +523 -551 -539 +653 +654 -273 -655 +656 +187 -507 +595 \n"
        +"  94:    +657 +167 +658 -659 +660 -661 -656 -188 +154 -175 +662 +508 \n"
        +"  95:    +663 +664 -420 -665 +511 -503 +423 -666 -495 +176 +166 -662 \n"
        +"  96:    -667 -424 +666 -496 -183 +173 -668 -669 +670 +671 +433 -672 \n"
        +"  97:    -322 -359 +387 -366 -401 -393 +673 -674 +672 +428 +432 +675 \n"
        +"  98:    +328 -675 +676 -670 -677 +334 +678 -679 +353 +431 -358 -344 \n"
        +"  99:    +180 -203 -213 -234 -236 +680 +681 +679 -348 -430 -671 -497 \n"
        +" 100:    -681 -678 +352 +345 +682 -642 +336 -683 +144 -238 +644 -232 \n"
        +" 101:    -182 -680 +237 +683 +677 +684 -335 +149 +685 +172 +668 +161 \n"
        +" 102:    +151 -685 +686 -636 -332 -638 +136 -687 +171 -688 -296 +162 \n"
        +" 103:    -390 -632 -634 -689 +169 +690 +297 +688 +691 -692 +693 -694 \n"
        +" 104:    -395 +695 -397 -413 +696 -663 -164 +694 +697 +667 -422 +674 \n"
        +" 105:    +321 -673 -394 -635 -333 -676 +669 -684 -170 +689 -686 -697 \n"
        +" 106:    -696 -414 -698 -693 +699 -657 -165 -664 -700 +701 +702 -703 \n"
        +" 107:    +542 -704 +537 +75  -705 +706 +703 -707 -708 -416 +67  -709 \n"
        +" 108:    +520 +459 +528 +541 -710 +709 -417 -69  -411 +501 +480 -534 \n"
        +" 109:    -521 +710 -540 +708 -711 +700 +661 -653 +509 +504 -419 +665 \n"
        +" 110:    +544 +272 -654 +712 -660 +713 -701 -714 -281 +715 +707 +711 \n"
        +" 111:    +543 +278 -715 +280 +716 +704 +536 +555 -258 +86  -288 +74  \n"
        +" 112:    +282 +714 +717 -702 -691 +718 -76  -631 +300 -289 -716 +705 \n"
        +" 113:    -392 +630 +77  -718 +692 +698 -695 +398 -415 -706 -66  -50  \n"
        +" 114:    +283 -713 -658 -719 -135 +295 +687 +168 -699 -690 -717 -155 \n"
        +" 115:    +274 +655 +659 -712 +719 +284 -250 -138 +156 -243 -189 +596 \n"
        +" 116:    -682 +641 +603 -645 -647 -229 -225 -351 +620 -346 +617 -624 \n"
        +" 117:    -558 -527 -473 +452 -515 +649 -651 -581 +549 -519 +465 +576 \n"
        +" 118:    +627 -565 +468 -485 -477 -525 +557 +554 -83  +54  -533 +93  \n"
        +" 119:    -6   +376 -621 +356 +341 +615 -23  +318 -39  -324 -384 +17  \n"
        +"1 4-cell:\n"
        +" 0:    +0   +1   +2   +3   +4   +5   +6   +7   +8   +9   +10  +11  +12  +13  +14  +15  +16  +17  +18  +19  +20  +21  +22  +23  +24  +25  +26  +27  +28  +29  +30  +31  +32  +33  +34  +35  +36  +37  +38  +39  +40  +41  +42  +43  +44  +45  +46  +47  +48  +49  +50  +51  +52  +53  +54  +55  +56  +57  +58  +59  +60  +61  +62  +63  +64  +65  +66  +67  +68  +69  +70  +71  +72  +73  +74  +75  +76  +77  +78  +79  +80  +81  +82  +83  +84  +85  +86  +87  +88  +89  +90  +91  +92  +93  +94  +95  +96  +97  +98  +99  +100 +101 +102 +103 +104 +105 +106 +107 +108 +109 +110 +111 +112 +113 +114 +115 +116 +117 +118 +119 \n"
        ;
    private static String pcalcString533 = pcalcString533_firstHalf
                                         + pcalcString533_secondHalf;
// 7244 # 4648 "com/donhatchsw/util/CSG.prejava" 2

} // CSG
