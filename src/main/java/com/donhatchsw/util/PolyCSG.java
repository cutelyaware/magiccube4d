// 2 # 1 "com/donhatchsw/util/PolyCSG.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/PolyCSG.prejava"
// PolyCSG.prejava

/*
    To consider--
        - should be converting to/from SPolytopes, not Polytopes
            (but the obscure name is problematic...
             change to Polytope and UPolytope?)
        - should combine multiple segments shared by a given pair of faces
*/

package com.donhatchsw.util;

// 18 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 22 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 24 # 14 "com/donhatchsw/util/PolyCSG.prejava" 2


//
// Because I seem to be doing these same loops over and over...
// "faces" and "nFaces" should be declared externally;
// all the rest are names of local variables for the loop.
//
// 32 # 44 "com/donhatchsw/util/PolyCSG.prejava"
/**
*  The functions here simply provide a way
*  of translating back and forth
*  between a Poly (an easy-to-create-and-understand representation)
*  and a CSG.Polytope
*  (on which boolean operations can be done using the static functions
*  in the CSG utility class).
*/
public final class PolyCSG
{
    private PolyCSG() {} // uninstantiatable

    public static Poly PolyFromPolytope(CSG.Polytope ptope)
    {
        do { if (!(ptope.dim == 3)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+58 +"): " + "ptope.dim == 3" + ""); } while (false);
        do { if (!(CSG.isOrientedShallow(ptope))) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+59 +"): " + "CSG.isOrientedShallow(ptope)" + ""); } while (false); // preemptive insanity prevention

        CSG.SPolytope allFaces[] = ptope.facets;
        int nFaces = allFaces.length;

        //
        // Make lists of all edges and all vertices...
        //
        int nEdges, nVerts;
        CSG.Polytope allEdges[], allVerts[];
        {
            nEdges = 0; // and counting
            nVerts = 0; // and counting
            {
                for (int iFace = 0; (iFace) < (nFaces); ++iFace)
                {
                    CSG.Polytope face = allFaces[iFace].p;
                    CSG.SPolytope edgesThisFace[] = face.facets;
                    int nEdgesThisFace = edgesThisFace.length;
                    nEdges += nEdgesThisFace;
                    for (int iEdgeThisFace = 0; (iEdgeThisFace) < (nEdgesThisFace); ++iEdgeThisFace)
                    {
                        CSG.Polytope edge = edgesThisFace[iEdgeThisFace].p;
                        int nVertsThisEdge = edge.facets.length;
                        do { if (!(nVertsThisEdge % 2 == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+83 +"): " + "nVertsThisEdge % 2 == 0" + ""); } while (false);
                        nVerts += nVertsThisEdge/2;
                    }
                }
                // each edge was counted twice...
                do { if (!(nEdges % 2 == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+88 +"): " + "nEdges % 2 == 0" + ""); } while (false);
                nEdges /= 2;
                // each vertex was counted lots of times;
                // we will nodup them out a little later.
            }

            if (false)
            {
                System.out.println("nEdges" + " = " + (nEdges));
                System.out.println("before nodup:");
                System.out.println("nVerts" + " = " + (nVerts));
            }

            allEdges = new CSG.Polytope[nEdges];
            allVerts = new CSG.Polytope[nVerts];

            int iEdge = 0, iVert = 0;
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                CSG.SPolytope face = allFaces[iFace];
                CSG.SPolytope edgesThisFace[] = face.p.facets;
                int nEdgesThisFace = edgesThisFace.length;
                for (int iEdgeThisFace = 0; (iEdgeThisFace) < (nEdgesThisFace); ++iEdgeThisFace)
                {
                    CSG.SPolytope edge = edgesThisFace[iEdgeThisFace];
                    if (face.sign * edge.sign > 0)
                    {
                        allEdges[iEdge++] = edge.p;
                        CSG.SPolytope vertsThisEdge[] = edge.p.facets;
                        int nVertsThisEdge = vertsThisEdge.length;
                        for (int iVertThisEdge = 0; (iVertThisEdge) < (nVertsThisEdge); ++iVertThisEdge)
                        {
                            CSG.SPolytope vert = vertsThisEdge[iVertThisEdge];
                            allVerts[iVert++] = vert.p;
                        }
                    }
                }
            }
            do { if (!(iEdge == nEdges)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+126 +"): " + "iEdge == nEdges" + ""); } while (false);
            do { if (!(iVert == nVerts)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+127 +"): " + "iVert == nVerts" + ""); } while (false);
        } // allEdges, allVerts

        SortStuff.Comparator idComparator = new SortStuff.Comparator() {
            public int compare(Object _a, Object _b)
            {
                CSG.Polytope a = (CSG.Polytope)_a;
                CSG.Polytope b = (CSG.Polytope)_b;
                return a.id < b.id ? -1 :
                       a.id > b.id ? 1 : 0;
            }
        };

        //
        // Sort the lists and remove dups...
        //
        {
            SortStuff.sort(allEdges, idComparator);
            do { if (!(SortStuff.nodup(allEdges, idComparator) == nEdges)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+145 +"): " + "SortStuff.nodup(allEdges, idComparator) == nEdges" + ""); } while (false); // there are no dups
            SortStuff.sort(allVerts, idComparator);
            nVerts = SortStuff.nodup(allVerts, idComparator);
            allVerts = (CSG.Polytope[])Arrays.subarray(allVerts, 0, nVerts); // resize
            if (false)
            {
                System.out.println("after nodup:");
                System.out.println("nVerts" + " = " + (nVerts));
            }
        }

        //
        // Make lookup tables f2e, e2v
        // with signs f2eSigns, e2vSigns.
        //
        int f2e[][], f2eSigns[][];
        int e2v[][], e2vSigns[][];
        {
            f2e = new int[nFaces][];
            f2eSigns = new int[nFaces][];
            e2v = new int[nEdges][];
            e2vSigns = new int[nEdges][];
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                CSG.SPolytope face = allFaces[iFace];
                CSG.SPolytope edgesThisFace[] = face.p.facets;
                int nEdgesThisFace = edgesThisFace.length;
                f2e[iFace] = new int[nEdgesThisFace];
                f2eSigns[iFace] = new int[nEdgesThisFace];
                for (int iEdgeThisFace = 0; (iEdgeThisFace) < (nEdgesThisFace); ++iEdgeThisFace)
                {
                    CSG.SPolytope edge = edgesThisFace[iEdgeThisFace];
                    int ind = SortStuff.bsearch(allEdges, edge.p, idComparator);
                    f2e[iFace][iEdgeThisFace] = ind;
                    f2eSigns[iFace][iEdgeThisFace] = edge.sign;
                }
            }
            for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
            {
                CSG.Polytope edge = allEdges[iEdge];
                CSG.SPolytope vertsThisEdge[] = edge.facets;
                int nVertsThisEdge = vertsThisEdge.length;
                e2v[iEdge] = new int[nVertsThisEdge];
                e2vSigns[iEdge] = new int[nVertsThisEdge];
                for (int iVertThisEdge = 0; (iVertThisEdge) < (nVertsThisEdge); ++iVertThisEdge)
                {
                    CSG.SPolytope vert = vertsThisEdge[iVertThisEdge];
                    int ind = SortStuff.bsearch(allVerts, vert.p, idComparator);
                    e2v[iEdge][iVertThisEdge] = ind;
                    e2vSigns[iEdge][iVertThisEdge] = vert.sign;
                }
            }
        } // f2e, e2v

        allEdges = null; // no longer needed

        //
        // Make final vertex coord list...
        //
        final double vertexCoords[][] = new double[nVerts][/*3*/];
        {
            for (int iVert = 0; (iVert) < (nVerts); ++iVert)
                vertexCoords[iVert] = allVerts[iVert].getCoords();
        }

        //PRINTVEC(allVerts);
        allVerts = null; // no longer needed

        //
        // Break up all edges into segments.
        // allSegments is a list of pairs of indices into allVerts.
        //
        int nSegments;
        int allSegments[][/*2*/];
        int e2s[][]; // e2sSigns not needed, they are all 1's
        {
            nSegments = 0; // and counting
            for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
            {
                int nVertsThisEdge = e2v[iEdge].length;
                do { if (!(nVertsThisEdge % 2 == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+225 +"): " + "nVertsThisEdge % 2 == 0" + ""); } while (false);
                nSegments += nVertsThisEdge/2;
            }

            allSegments = new int[nSegments][2];
            e2s = new int[nEdges][];

            // scratch for loop...
            final double edgeDir[] = new double[vertexCoords[0].length];

            int iSegment = 0;
            for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
            {
                final int vertsThisEdge[] = e2v[iEdge];
                int vertsThisEdgeSigns[] = e2vSigns[iEdge];
                int nVertsThisEdge = vertsThisEdge.length;
                int nSegsThisEdge = nVertsThisEdge/2;
                e2s[iEdge] = new int[nSegsThisEdge];
                //
                // Break up edge into segments.
                //

                // First we calculate the edge direction...
                {
                    VecMath.zerovec(edgeDir);
                    for (int iVertThisEdge = 0; (iVertThisEdge) < (nVertsThisEdge); ++iVertThisEdge)
                        VecMath.vpsxv(edgeDir, edgeDir,
                                      vertsThisEdgeSigns[iVertThisEdge],
                                      vertexCoords[vertsThisEdge[iVertThisEdge]]);
                }

                //
                // Now sort the indices 0..nVertsThisEdge-1
                // into increasing order along the edge.
                // (Note, if these lists were long, it be more efficient
                // to compute all the dot products and put them in an array
                // rather than computing them on the fly and possibly
                // redundantly, but nVertsThisEdge is almost always 2,
                // so we don't bother.)
                //
                int inds[] = new int[nVertsThisEdge];
                for (int iInd = 0; (iInd) < (nVertsThisEdge); ++iInd)
                    inds[iInd] = iInd;
                SortStuff.sort(inds, new SortStuff.IntComparator() {
                    public int compare(int i, int j)
                    {
                        double dotProdI = VecMath.dot(
                                                vertexCoords[vertsThisEdge[i]],
                                                edgeDir);
                        double dotProdJ = VecMath.dot(
                                                vertexCoords[vertsThisEdge[j]],
                                                edgeDir);
                        return dotProdI < dotProdJ ? -1 :
                               dotProdI > dotProdJ ? 1 : 0;
                    }
                });

                //
                // Starting from the low end,
                // match up each vertex with the next higher
                // unmatched one of the opposite sign.
                // XXX I *think* this is the reasonable thing to do;
                // XXX not sure how weird the input can get
                //
                int iSegThisEdge = 0;
                for (int iInd = 0; (iInd) < (nVertsThisEdge); ++iInd)
                {
                    int iVertThisEdge = inds[iInd];
                    if (iVertThisEdge != -1) // if not sent already
                    {
                        int signI = vertsThisEdgeSigns[iVertThisEdge];
                        int jInd;
                        for (jInd = iInd+1; jInd < nVertsThisEdge; jInd++)
                        {
                            int jVertThisEdge = inds[jInd];
                            if (jVertThisEdge != -1) // if not sent already
                            {
                                int signJ = vertsThisEdgeSigns[jVertThisEdge];
                                // if they have opposite signs,
                                // send the segment.
                                if (signI == -signJ)
                                {
                                    int sign = signJ; // + if signI,signJ are in order, - if out of order

                                    allSegments[iSegment][((sign)==1?0:1)] = vertsThisEdge[iVertThisEdge];
                                    allSegments[iSegment][((sign)==1?1:0)] = vertsThisEdge[jVertThisEdge];
                                    inds[jInd] = -1; // mark it sent
                                    e2s[iEdge][iSegThisEdge++] = iSegment;
                                    iSegment++;
                                    break;
                                }
                            }
                        }
                        do { if (!(jInd < nVertsThisEdge)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+318 +"): " + "jInd < nVertsThisEdge" + ""); } while (false); // make sure we matched
                    }
                }
                do { if (!(iSegThisEdge == nSegsThisEdge)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+321 +"): " + "iSegThisEdge == nSegsThisEdge" + ""); } while (false);
            }
            do { if (!(iSegment == nSegments)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+323 +"): " + "iSegment == nSegments" + ""); } while (false);
        } // allSegments, e2s

        //PRINTMAT(allSegments);
        //PRINTMAT(e2s);

        //
        // Make a lookup table from faces to segments, with signs...
        //
        int f2s[][];
        int f2sSigns[][];
        {
            f2s = new int[nFaces][];
            f2sSigns = new int[nFaces][];

            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                int edgesThisFace[] = f2e[iFace];
                int nEdgesThisFace = edgesThisFace.length;
                int nSegsThisFace;
                {
                    nSegsThisFace = 0; // and counting
                    for (int iEdgeThisFace = 0; (iEdgeThisFace) < (nEdgesThisFace); ++iEdgeThisFace)
                    {
                        int iEdge = edgesThisFace[iEdgeThisFace];
                        nSegsThisFace += e2s[iEdge].length;
                    }
                }
                f2s[iFace] = new int[nSegsThisFace];
                f2sSigns[iFace] = new int[nSegsThisFace];
                int iSegThisFace = 0;
                for (int iEdgeThisFace = 0; (iEdgeThisFace) < (nEdgesThisFace); ++iEdgeThisFace)
                {
                    int iEdge = edgesThisFace[iEdgeThisFace];
                    int nSegsThisEdge = e2s[iEdge].length;
                    for (int iSegThisEdge = 0; (iSegThisEdge) < (nSegsThisEdge); ++iSegThisEdge)
                    {
                        f2s[iFace][iSegThisFace] = e2s[iEdge][iSegThisEdge];
                        f2sSigns[iFace][iSegThisFace] = f2eSigns[iFace][iEdgeThisFace];
                        iSegThisFace++;
                    }
                }
                do { if (!(iSegThisFace == nSegsThisFace)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+365 +"): " + "iSegThisFace == nSegsThisFace" + ""); } while (false);
            }
        } // f2s

        //PRINTMAT(f2s);
        //PRINTMAT(f2sSigns);

        //
        // Now that we have segments,
        // we don't need edges any more.
        //
        f2e = null;
        f2eSigns = null;
        e2v = null;
        e2vSigns = null;

        //
        // Piece together contours out of the segments.
        //
        int faceContours[][][] = new int[nFaces][][];

        Object auxs;
        {
            if (nFaces > 0
             && allFaces[0].p.aux != null)
                auxs = java.lang.reflect.Array.newInstance(allFaces[0].p.aux.getClass(), nFaces);
            else
                auxs = null;
        }

        {
            int scratchContour[] = new int[nVerts]; // big enough to hold any contour
            int scratchFace[][] = new int[nVerts][]; // big enough to hold any face's worth of contours

            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                int faceSign = allFaces[iFace].sign;
                int segsThisFace[] = f2s[iFace];
                int segsThisFaceSigns[] = f2sSigns[iFace];
                int nSegsThisFace = segsThisFace.length;
                int nContoursThisFace = 0; // and counting
                for (int iSegThisFace = 0; (iSegThisFace) < (nSegsThisFace); ++iSegThisFace)
                {
                    int iSeg = segsThisFace[iSegThisFace];
                    if (iSeg != -1) // if not already sent
                    {
                        segsThisFace[iSegThisFace] = -1; // mark it sent
                        int segI[/*2*/] = allSegments[iSeg];
                        int segISign = faceSign * segsThisFaceSigns[iSegThisFace];
                        int seg[/*2*/] = segI;
                        int segSign = segISign;
                        int nSegsThisContour = 0; // and counting
                        while (true)
                        {
                            // send seg's initial point...
                            scratchContour[nSegsThisContour++] = seg[((segSign)==1?0:1)];

                            //
                            // End of this contour
                            // is when seg's final point
                            // is segI's initial point.
                            //
                            if (seg[((segSign)==1?1:0)]
                             == segI[((segISign)==1?0:1)])
                                break; // end of this contour

                            //
                            // Find next segment on contour:
                            // an unsent segment
                            // whose initial point is seg's final point.
                            //
                            int jSegThisFace;
                            for (jSegThisFace = iSegThisFace+1;
                                 jSegThisFace < nSegsThisFace;
                                 jSegThisFace++)
                            {
                                int jSeg = segsThisFace[jSegThisFace];
                                if (jSeg != -1) // if not already sent
                                {
                                    int segJ[/*2*/] = allSegments[jSeg];
                                    int segJSign = faceSign * segsThisFaceSigns[jSegThisFace];
                                    if (segJ[((segJSign)==1?0:1)] == seg[((segSign)==1?1:0)])
                                    {
                                        segsThisFace[jSegThisFace] = -1; // mark it sent
                                        seg = segJ;
                                        segSign = segJSign;
                                        break;
                                    }
                                }
                            }
                            // assert we found the next segment
                            // on the contour...
                            do { if (!(jSegThisFace < nSegsThisFace)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+457 +"): " + "jSegThisFace < nSegsThisFace" + ""); } while (false);
                        }
                        scratchFace[nContoursThisFace++] = (int[])Arrays.subarray(scratchContour, 0, nSegsThisContour);
                    }
                }
                faceContours[iFace] = (int[][])Arrays.subarray(scratchFace, 0, nContoursThisFace);
                if (auxs != null)
                    java.lang.reflect.Array.set(auxs, iFace,
                                                allFaces[iFace].p.aux);
            }
        } // faceContours

        //System.out.println("faceContours = \n"+VecMath.toString(faceContours,"\n",""));

        Poly poly = new Poly(vertexCoords,
                             faceContours,
                             auxs,
                             null);
        return poly;
    } // PolyFromPolytope

    public static CSG.Polytope PolytopeFromPoly(Poly poly)
    {
        double[][] verts = (double[][])poly.verts;
        int nVerts = verts.length;
        int faces[][][] = poly.getInds3();
        int nFaces = faces.length;

        Object auxs;
        {
            if (poly.aux == null)
                auxs = null;
            else
            {
                // take a deep breath...
                // need to flatten auxs in exactly the same manner that inds were
                // flattened.
                int diff = Arrays.getDim(poly.inds)
                         - Arrays.getDim(faces);
                auxs = Arrays.flatten(poly.aux, 0, diff+1); // flattening diff+1 dimensions into 1 gives a difference of (diff+1)-1 = diff
            }
        }

        int segs[][/*2*/];
        int nSegs;
        {
            int twiceNumSegs = Arrays.arrayLength(faces, 3);
            do { if (!(twiceNumSegs % 2 == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+504 +"): " + "twiceNumSegs % 2 == 0" + ""); } while (false);
            nSegs = twiceNumSegs/2;
            segs = new int[nSegs][2];
            {
                int iSeg = 0;
                for (int iFace = 0; (iFace) < (nFaces); ++iFace) for (int face[][] = faces[iFace], nContoursThisFace = face.length, iContourThisFace = 0; iContourThisFace < nContoursThisFace; iContourThisFace++) for (int contour[] = face[iContourThisFace], nVertsThisContour = contour.length, iVertThisContour = 0; iVertThisContour < nVertsThisContour; iVertThisContour++)



                {
                    int a = contour[iVertThisContour];
                    int b = contour[(iVertThisContour+1)%nVertsThisContour];
                    if (a < b) // if in canonical order
                    {
                        int seg[] = segs[iSeg++];
                        seg[0] = a;
                        seg[1] = b;
                    }
                }
                do { if (!(iSeg == nSegs)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+523 +"): " + "iSeg == nSegs" + ""); } while (false);
            }
        }

        SortStuff.Comparator segComparator = new SortStuff.Comparator() {
            public int compare(Object _a, Object _b)
            {
                int a[/*2*/] = (int[])_a;
                int b[/*2*/] = (int[])_b;
                if (a[0] < b[0]) return -1;
                if (a[0] > b[0]) return 1;
                if (a[1] < b[1]) return -1;
                if (a[1] > b[1]) return 1;
                return 0;
            }
        };

        //System.out.println("before sort:");
        //PRINTMAT(segs);

        SortStuff.sort(segs, segComparator);

        //System.out.println("after sort:");
        //PRINTMAT(segs);

        //
        // Figure out which segs are incident on which faces...
        //
        int segsPerFace[][] = new int[nFaces][];
        int dirsOfSegsPerFace[][] = new int[nFaces][];
        {
            int scratchSeg[] = new int[2];
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                int nSegsThisFace = Arrays.arrayLength(faces[iFace], 2);
                segsPerFace[iFace] = new int[nSegsThisFace];
                dirsOfSegsPerFace[iFace] = new int[nSegsThisFace];
            }

            // XXX argh, this is almost the canonical loop but not quite...
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                int face[][] = faces[iFace];
                int nSegsThisFace = Arrays.arrayLength(face, 2);
                int segsThisFace[] = segsPerFace[iFace] = new int[nSegsThisFace];
                int dirsOfSegsThisFace[] = dirsOfSegsPerFace[iFace] = new int[nSegsThisFace];
                int iSegThisFace = 0;

                int iContourThisFace, nContoursThisFace = face.length;
                for (iContourThisFace = 0; (iContourThisFace) < (nContoursThisFace); ++iContourThisFace)
                {
                    int contour[] = face[iContourThisFace];
                    int iVertThisContour, nVertsThisContour = contour.length;
                    for (iVertThisContour = 0; (iVertThisContour) < (nVertsThisContour); ++iVertThisContour)
                    {
                        int a = contour[iVertThisContour];
                        int b = contour[(iVertThisContour+1)%nVertsThisContour];
                        if (a > b)
                        {
                            int temp;
                            {temp=(a);a=(b);b=(temp);};
                            dirsOfSegsThisFace[iSegThisFace] = -1;
                        }
                        else
                            dirsOfSegsThisFace[iSegThisFace] = 1;
                        scratchSeg[0] = a;
                        scratchSeg[1] = b;
                        int ind = SortStuff.bsearch(segs, scratchSeg, segComparator);
                        do { if (!(ind != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+591 +"): " + "ind != -1" + ""); } while (false);
                        segsThisFace[iSegThisFace] = ind;
                        iSegThisFace++;
                    }
                }
                do { if (!(iSegThisFace == nSegsThisFace)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+596 +"): " + "iSegThisFace == nSegsThisFace" + ""); } while (false);
            }
        } // segsPerFace

        //
        // Figure out the plane equations for each face...
        // XXX maybe this should be a function in class Poly
        // XXX also, it might be simpler if we are willing to add
        // XXX from origin; might be a simple general content procedure
        //
        CSG.Hyperplane faceHyperplanes[] = new CSG.Hyperplane[nFaces];
        {
            // scratch for loop...
                double e1[] = new double[3];
                double e2[] = new double[3];
                double crossProd[] = new double[3];
                double temp[];

            // XXX argh, this is almost the canonical loop but not quite
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                double normal[] = VecMath.zerovec(3);

                int face[][] = faces[iFace];
                int iContourThisFace, nContoursThisFace = face.length;
                for (iContourThisFace = 0; (iContourThisFace) < (nContoursThisFace); ++iContourThisFace)
                {
                    int contour[] = face[iContourThisFace];
                    int i, n = contour.length;

                    VecMath.vmv(e1,
                                verts[contour[1]],
                                verts[contour[0]]);
                    for (i = 0; (i) < (n-2); ++i)
                    {
                        // e1 is verts[contour][i+1] - verts[contour[0]], and...
                        VecMath.vmv(e2,
                                    verts[contour[i+2]],
                                    verts[contour[0]]);
                        VecMath.vxv3(crossProd, e1, e2);
                        VecMath.vpv(normal, normal, crossProd);

                        // e2 is next iteration's e1...
                        {temp=(e1);e1=(e2);e2=(temp);};
                    }
                }

                do { if (!(face.length >= 1 && face[0].length >= 1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+644 +"): " + "face.length >= 1 && face[0].length >= 1" + ""); } while (false);

                faceHyperplanes[iFace] = new CSG.Hyperplane(normal,
                                                            VecMath.dot(normal, verts[face[0][0]]));
            }
        } // faceHyperplanes

        // Each segment has two hyperplanes associated with it
        // (the hyperplanes of the two faces the segment is on).
        CSG.Hyperplane segHyperplanes[][] = new CSG.Hyperplane[nSegs][2];
        {
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                CSG.Hyperplane faceHyperplane = faceHyperplanes[iFace];
                int segsThisFace[] = segsPerFace[iFace];
                int iSegThisFace, nSegsThisFace = segsThisFace.length;
                for (iSegThisFace = 0; (iSegThisFace) < (nSegsThisFace); ++iSegThisFace)
                {
                    CSG.Hyperplane thisSegHyperplanes[] = segHyperplanes[segsThisFace[iSegThisFace]];
                    int ind = Arrays.indexOfUsingEqualsSymbol(thisSegHyperplanes, null);
                    do { if (!(ind != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+663 +"): " + "ind != -1" + ""); } while (false);
                    thisSegHyperplanes[ind] = faceHyperplane;
                }
            }
        } // segHyperplanes

        // Each vertex has three hyperplanes associated with it.
        // (the hyperplanes of any three faces it's on).
        // XXX argh, this is going to bomb when a vertex
        // XXX is on more than three faces... need to rethink this.
        CSG.Hyperplane vertHyperplanes[][] = new CSG.Hyperplane[nVerts][3];
        {
            for (int iFace = 0; (iFace) < (nFaces); ++iFace) for (int face[][] = faces[iFace], nContoursThisFace = face.length, iContourThisFace = 0; iContourThisFace < nContoursThisFace; iContourThisFace++) for (int contour[] = face[iContourThisFace], nVertsThisContour = contour.length, iVertThisContour = 0; iVertThisContour < nVertsThisContour; iVertThisContour++)


            {
                CSG.Hyperplane thisVertHyperplanes[] = vertHyperplanes[contour[iVertThisContour]];
                int ind = Arrays.indexOfUsingEqualsSymbol(thisVertHyperplanes, null);
                //assumpt(ind != -1); // XXX fuck fuck fuck
                if (ind == -1)
                    System.out.println("Warning: more than 3 hyperplanes meet at a point?");
                if (ind != -1)
                    thisVertHyperplanes[ind] = faceHyperplanes[iFace];
            }
        } // vertHyperplanes


        //
        // Now we have all the info we need...
        // Build up the polytope, vertices then edges then faces.
        //
        CSG.Polytope vertexPolytopes[] = new CSG.Polytope[nVerts];
        {
            for (int iVert = 0; (iVert) < (nVerts); ++iVert)
            {
                Object vertexAux = null; // XXX no aux for vertex. should really propagate our own aux down (splitting it up as necessary) except we don't know the right dimension info, so don't try it yet.
                vertexPolytopes[iVert] = new CSG.Polytope(0,
                                                          3,
                                                          new CSG.SPolytope[0], // no facets
                                                          vertHyperplanes[iVert],
                                                          vertexAux);
                vertexPolytopes[iVert].setCoords(verts[iVert]);
            }
        } // vertexPolytopes
        CSG.Polytope segPolytopes[] = new CSG.Polytope[nSegs];
        {
            for (int iSeg = 0; (iSeg) < (nSegs); ++iSeg)
            {
                CSG.SPolytope endPoints[] = new CSG.SPolytope[] {
                    new CSG.SPolytope(0, -1, vertexPolytopes[segs[iSeg][0]]),
                    new CSG.SPolytope(0, 1, vertexPolytopes[segs[iSeg][1]]),
                };
                Object segAux = null; // XXX no aux for edge. should really propagate our own aux down (splitting it up as necessary)
                segPolytopes[iSeg] = new CSG.Polytope(1,
                                                      3,
                                                      endPoints,
                                                      segHyperplanes[iSeg],
                                                      segAux);
            }
        } // segPolytopes
        CSG.Polytope facePolytopes[] = new CSG.Polytope[nFaces];
        {
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
            {
                int segsThisFace[] = segsPerFace[iFace];
                int dirsOfSegsThisFace[] = dirsOfSegsPerFace[iFace];
                int iSegThisFace, nSegsThisFace = segsThisFace.length;
                CSG.SPolytope sides[] = new CSG.SPolytope[nSegsThisFace];
                for (iSegThisFace = 0; (iSegThisFace) < (nSegsThisFace); ++iSegThisFace)
                {
                    int segIndex = segsThisFace[iSegThisFace];
                    int seg[] = segs[segIndex];
                    sides[iSegThisFace] = new CSG.SPolytope(
                                0,
                                dirsOfSegsThisFace[iSegThisFace],
                                segPolytopes[segIndex]);
                }





                Object faceAux = (auxs != null && auxs.getClass().isArray() ? java.lang.reflect.Array.get(auxs, iFace) : null);
                facePolytopes[iFace] = new CSG.Polytope(2,
                                                        3,
                                                        sides,
                                                        new CSG.Hyperplane[] {faceHyperplanes[iFace]},
                                                        faceAux);
            }
        } // facePolytopes


        CSG.Polytope cellPolytope;
        {
            CSG.SPolytope facets[] = new CSG.SPolytope[nFaces];
            for (int iFace = 0; (iFace) < (nFaces); ++iFace)
                facets[iFace] = new CSG.SPolytope(0, 1, facePolytopes[iFace]);
            cellPolytope = new CSG.Polytope(3,
                                            3,
                                            facets,
                                            new CSG.Hyperplane[0], // zero hyperplanes
                                            auxs);
        }

        // XXX some other error handling here?
        do { if (!(CSG.isOrientedShallow(cellPolytope))) throw new Error("Assertion failed at "+"com/donhatchsw/util/PolyCSG.prejava"+"("+768 +"): " + "CSG.isOrientedShallow(cellPolytope)" + ""); } while (false);

        return cellPolytope;
    } // PolytopeFromPoly

    //
    // A little test program...
    //
    public static void main(String args[])
    {
        System.out.println("in main");

        /*
        if (args.length < 1)
        {
            System.err.println("Usage: PolyCSG <dim>");
            System.exit(1);
        }
        */

        //
        // Poly to CSG.Polytope and back...
        //
        if (false)
        {
            //Poly poly0 = Poly.cube;
            //Poly poly0 = Poly.dodeca;
            Poly poly0 = Poly.donut;
            System.out.println("poly0" + " = " + (poly0));
            CSG.Polytope ptope = PolytopeFromPoly(poly0);
            System.out.println("ptope" + " = " + (ptope));
            Poly poly1 = PolyFromPolytope(ptope);
            System.out.println("poly1" + " = " + (poly1));
        }

        //
        // CSG.Polytope to Poly and back...
        //
        if (false)
        {
            CSG.Polytope ptope0 = CSG.makeHypercube(3).p;
            System.out.println("ptope0" + " = " + (ptope0));
            Poly poly = PolyFromPolytope(ptope0);
            System.out.println("poly" + " = " + (poly));
            CSG.Polytope ptope1 = PolytopeFromPoly(poly);
            System.out.println("ptope1" + " = " + (ptope1));
        }

        //
        // Intersect a tetrahedron with a slightly shrunken cube.
        //
        if (false)
        {
            CSG.Polytope cube = PolytopeFromPoly(
                          Poly.transform(Poly.cube,
                                         VecMath.makeRowScaleMat(3, .9)));
            CSG.Polytope tetra = PolytopeFromPoly(Poly.tetra);
            CSG.Polytope intersection_ptope = CSG.intersect(cube, tetra);
            System.out.println("intersection_ptope" + " = " + (intersection_ptope));
            Poly intersection_poly = PolyFromPolytope(intersection_ptope);
            System.out.println("intersection_poly" + " = " + (intersection_poly));
        }

        //
        // Test case that was failing...
        //
        if (true)
        {
            CSG.Polytope tetra0 = PolytopeFromPoly(Poly.tetra);
            CSG.Polytope tetra1 = PolytopeFromPoly(
                    Poly.transform(Poly.tetra,
                                   VecMath.makeRowTransMat(1.,0.,0.)));
            CSG.Polytope union_ptope = CSG.union(tetra0, tetra1);
            System.out.println("union_ptope" + " = " + (union_ptope));
            System.out.println("CSG.counts(union_ptope)" + " = " + VecMath.toString(CSG.counts(union_ptope)));
            Poly union_poly = PolyFromPolytope(union_ptope);
            System.out.println("union_poly" + " = " + (union_poly));
        }

        System.out.println("out main");
    } // main

} // class PolyCSG
