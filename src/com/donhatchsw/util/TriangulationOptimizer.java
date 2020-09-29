// 2 # 1 "com/donhatchsw/util/TriangulationOptimizer.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/TriangulationOptimizer.prejava"
//
// TriangulationOptimizer.h
// Optimizes a triangulation by flipping quad diagonals.
//
// XXX We always flip the worst internal edge,
// XXX keeping the internal edges in a priority queue (heap)
// XXX so we can always find the worst and do adjustments
// XXX in O(log(n)) time.
// XXX However, I don't think this is really necessary--
// XXX a set structure (e.g. linked list) allowing O(1) insertion
// XXX and removal will suffice.
//

package com.donhatchsw.util;

// 21 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 25 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 27 # 17 "com/donhatchsw/util/TriangulationOptimizer.prejava" 2






/**
* A static utility function that optimizes (Delaunay-izes) a triangulation
* by flipping quad diagonals.
*/
public class TriangulationOptimizer
{
    private TriangulationOptimizer() {} // uninstantiatable

    //
    // Debugging setting...
    // I just set this appropriately and recompile.
    // Note it's final, so debugging stuff gets compiled out
    // when not debugging.
    //     0 = nothing
    //     1 = on failure  (XXX nothing there currently, just throws exception)
    //     2 = basic flow
    //     3 = show full triangulation at start and end
    //     4 = show full triangulation at every step
    //     5 = show full triangulation and heap at every step
    //
    private static final int verboseLevel = 0;

    /**
    * 
    *  Try to delaunayize a given triangulation by flipping quad diagonals.
    *  Note, the result will be (constrained-) Delaunay in the projected plane,
    *  but not necessarily in the plane of the polygon
    *  (it might be warped a bit).
    */
    public static void optimizeTriangulationInPlace(double verts[][],
                                                    int tris[][],
                                                    int nTris,
                                                    int xAxis, int yAxis,
                                                    double eps)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "";
            System.out.println("in optimizeTriangulationInPlace");
            System.out.println(indentString + "    nVerts = " + verts.length);
            if (verboseLevel >= 3)
                System.out.println(Arrays.toStringNonCompact(verts, indentString+"        ", "    "));
            System.out.println(indentString + "    nTris = " + nTris);
            if (verboseLevel >= 3)
                System.out.println(indentString + "    tris = " + Arrays.toStringCompact(Arrays.subarray(tris, 0, nTris)));
        }

        //
        // Make the edge graph...
        //
        DirectedEdge edges[][] = new DirectedEdge[nTris][3];
        int nInternalEdges = getContourEdges(edges, tris, nTris);

        //
        // Find comfort of all quads (pairs of adjacent triangles),
        // and put them in a heap from which we can always find
        // the smallest...
        //
        Heap heap = new Heap(nInternalEdges);
        for (int i = 0; (i) < (nTris); ++i)
        {
            DirectedEdge edgesThisTri[] = edges[i];
            int tri[] = tris[i];
            for (int ii = 0; (ii) < (3); ++ii)
            {
                DirectedEdge edge = edgesThisTri[ii];
                DirectedEdge oppositeEdge = edge.opposite;
                if (oppositeEdge != null) // if it's an internal edge
                {
                    int initialVertex = edge.initialVertex;
                    int finalVertex = edge.finalVertex;
                    if (initialVertex < finalVertex) // so we only include one direction of the edge
                    {
                        int j = oppositeEdge.iTri;
                        int jj = oppositeEdge.iEdgeOnTri;
                        heap.add(edge,
                                 calcQuadComfort(verts[finalVertex],
                                                 verts[tri[(ii+2)%3]],
                                                 verts[initialVertex],
                                                 verts[tris[j][(jj+2)%3]],
                                                 xAxis, yAxis));
                    }
                }
            }
        }

        //
        // Repeatedly take the smallest (most uncomfortable) quad
        // and reverse its diagonal, until no more want to be reversed.
        //
        double eps4 = eps*eps*eps*eps;
        int maxIters = nInternalEdges*nInternalEdges; // XXX what is the right bound?
        maxIters++; // XXX definitely too low for a quad, but that may be due to screwy control flow in this loop... should see if I can improve it

        int iIter = 0;
        for (iIter = 0; (iIter) < (maxIters); ++iIter)
        {
            if (verboseLevel >= 5)
            {
                System.out.println(indentString+"        edges on tris: ");
                for (int iTri = 0; (iTri) < (nTris); ++iTri)
                {
                    for (int iEdgeOnTri = 0; (iEdgeOnTri) < (3); ++iEdgeOnTri)
                    {
                        DirectedEdge e = edges[iTri][iEdgeOnTri];
                        System.out.println(indentString+"            "+iTri+" "+iEdgeOnTri+": "+e);
                    }
                }

                System.out.println(indentString+"        edges in heap: ");
                for (int iHeap = 0; (iHeap) < (heap.n); ++iHeap)
                {
                    DirectedEdge e = (DirectedEdge)heap.heap[iHeap];
                    System.out.println(indentString+"            "+iHeap+": "+e);
                }
            }

            //
            // If any printing being done by default
            // (i.e. verboseLevel >= 2),
            // then we don't care about speed,
            // so do full super duper sanity check.
            //
            if (verboseLevel >= 2)
            {
                for (int iTri = 0; (iTri) < (nTris); ++iTri)
                {
                    for (int iEdgeOnTri = 0; (iEdgeOnTri) < (3); ++iEdgeOnTri)
                    {
                        DirectedEdge e = edges[iTri][iEdgeOnTri];
                        do { if (!(e.iTri == iTri)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+154 +"): " + "e.iTri == iTri" + ""); } while (false);
                        do { if (!(e.iEdgeOnTri == iEdgeOnTri)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+155 +"): " + "e.iEdgeOnTri == iEdgeOnTri" + ""); } while (false);
                        do { if (!(e.initialVertex == tris[iTri][iEdgeOnTri])) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+156 +"): " + "e.initialVertex == tris[iTri][iEdgeOnTri]" + ""); } while (false);
                        do { if (!(e.finalVertex == tris[iTri][(iEdgeOnTri+1)%3])) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+157 +"): " + "e.finalVertex == tris[iTri][(iEdgeOnTri+1)%3]" + ""); } while (false);
                        if (e.opposite != null)
                        {
                            do { if (!(e.opposite.opposite == e)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+160 +"): " + "e.opposite.opposite == e" + ""); } while (false);
                            do { if (!(e.opposite.initialVertex == e.finalVertex)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+161 +"): " + "e.opposite.initialVertex == e.finalVertex" + ""); } while (false);
                            do { if (!(e.opposite.finalVertex == e.initialVertex)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+162 +"): " + "e.opposite.finalVertex == e.initialVertex" + ""); } while (false);
                        }
                    }
                }
                for (int iHeap = 0; (iHeap) < (heap.n); ++iHeap)
                {
                    DirectedEdge e = (DirectedEdge)heap.heap[iHeap];
                    do { if (!(edges[e.iTri][e.iEdgeOnTri] == e)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+169 +"): " + "edges[e.iTri][e.iEdgeOnTri] == e" + ""); } while (false);
                    do { if (!(e.opposite != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+170 +"): " + "e.opposite != null" + ""); } while (false);
                    if ((((iHeap)-1)>>1) >= 0)
                        do { if (!(heap.heap[(((iHeap)-1)>>1)].getVal() <= e.getVal())) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+173 +"): " + "heap.heap[HEAP_PARENT(iHeap)].getVal() <= e.getVal()" + ""); } while (false);

                }
            }


            DirectedEdge edge = (DirectedEdge)heap.min();
            if ((((0.)-(edge.getVal())) <= eps))
                break;
            DirectedEdge oppositeEdge = edge.opposite;

            int originalInitialVertex = edge.initialVertex;
            int originalFinalVertex = edge.finalVertex;

            //
            // Change the edge (and oppositeEdge)
            // to be the other diagonal on the quad.
            //
            {
                DirectedEdge i_ii = edge;
                int i = i_ii.iTri;
                int ii = i_ii.iEdgeOnTri;
                int iip1 = (ii+1)%3;
                int iip2 = (ii+2)%3;
                DirectedEdge i_iip1 = edges[i][iip1];
                DirectedEdge j_jj = oppositeEdge;
                int j = j_jj.iTri;
                int jj = j_jj.iEdgeOnTri;
                int jjp1 = (jj+1)%3;
                int jjp2 = (jj+2)%3;
                DirectedEdge j_jjp1 = edges[j][jjp1];

                tris[i][iip1] = tris[j][jjp2];
                tris[j][jjp1] = tris[i][iip2];

                edges[i][ii] = j_jjp1;
                edges[i][ii].iTri = i;
                edges[i][ii].iEdgeOnTri = ii;

                edges[j][jj] = i_iip1;
                edges[j][jj].iTri = j;
                edges[j][jj].iEdgeOnTri = jj;

                // edge (not oppositeEdge) is the one in the heap,
                // i.e. edge.initialVertex < edge.finalVertex.
                // keep it that way...
                if (tris[i][iip1] < tris[j][jjp1])
                {
                    edges[i][iip1] = edge;
                    edges[j][jjp1] = oppositeEdge;
                }
                else
                {
                    edges[j][jjp1] = edge;
                    edges[i][iip1] = oppositeEdge;
                }

                edges[i][iip1].initialVertex = tris[i][iip1];
                edges[i][iip1].finalVertex = tris[i][iip2];
                edges[i][iip1].iTri = i;
                edges[i][iip1].iEdgeOnTri = iip1;


                edges[j][jjp1].initialVertex = tris[j][jjp1];
                edges[j][jjp1].finalVertex = tris[j][jjp2];
                edges[j][jjp1].iTri = j;
                edges[j][jjp1].iEdgeOnTri = jjp1;



                //
                // Recompute the goodnesses
                // of the flipped edge
                // and the four quad perimeter edges.
                //
                if (true)
                {
                    adjustEdgeOrOpposite(heap,
                                         edge,
                                         verts, tris, xAxis, yAxis);
                    adjustEdgeOrOpposite(heap,
                                         edges[i][iip2],
                                         verts, tris, xAxis, yAxis);
                    adjustEdgeOrOpposite(heap,
                                         edges[i][ii],
                                         verts, tris, xAxis, yAxis);
                    adjustEdgeOrOpposite(heap,
                                         edges[j][jjp2],
                                         verts, tris, xAxis, yAxis);
                    adjustEdgeOrOpposite(heap,
                                         edges[j][jj],
                                         verts, tris, xAxis, yAxis);
                }

            }

            if (verboseLevel >= 3)
                System.out.println(indentString + "        flipping "+originalInitialVertex+","+originalFinalVertex+" into "+edge.initialVertex+","+edge.finalVertex+"");
            if (verboseLevel >= 4)
                System.out.println(indentString + "    tris = " + Arrays.toStringCompact(Arrays.subarray(tris, 0, nTris)));
        }
        do { if (!(iIter < maxIters)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+273 +"): " + "iIter < maxIters" + ""); } while (false);

        if (verboseLevel >= 2)
        {
            System.out.println(indentString + "    nInternalEdges = "+nInternalEdges);
            System.out.println(indentString + "    flipped "+iIter+" edges");
            if (verboseLevel >= 3)
                System.out.println(indentString + "    tris = " + Arrays.toStringCompact(Arrays.subarray(tris, 0, nTris)));
            System.out.println(indentString + "out optimizeTriangulationInPlace");
        }
    } // optimizeTriangulationInPlace


        // recompute the comfort of whichever of the two
        // instances of the edge is in the heap.
        private static void adjustEdgeOrOpposite(Heap heap,
                                                 DirectedEdge edge,
                                                 double verts[][],
                                                 int tris[][],
                                                 int xAxis, int yAxis)
        {
            if (edge.opposite == null)
                return;
            if (edge.initialVertex > edge.finalVertex)
                edge = edge.opposite;
            DirectedEdge opposite = edge.opposite;
            // Now edge is the instance in the heap,
            // and opposite is the other one
            double val = calcQuadComfort(verts[edge.finalVertex],
                                         verts[tris[edge.iTri][(edge.iEdgeOnTri+2)%3]],
                                         verts[edge.initialVertex],
                                         verts[tris[opposite.iTri][(opposite.iEdgeOnTri+2)%3]],
                                         xAxis, yAxis);
            heap.adjust(edge, val);
        } // adjustEdgeOrOpposite





    //========================================================================
    // Private utility stuff begins here...
    // 
        private static class DirectedEdge
            extends TriangulationOptimizer.Heap.HeapItem
        {
            public int initialVertex;
            public int finalVertex;
            public int iTri;
            public int iEdgeOnTri;
            public DirectedEdge opposite;
            public DirectedEdge() {}
            public DirectedEdge(int initialVertex,
                         int finalVertex,
                         int iTri,
                         int iEdgeOnTri)
            {
                this.initialVertex = initialVertex;
                this.finalVertex = finalVertex;
                this.iTri = iTri;
                this.iEdgeOnTri = iEdgeOnTri;
                this.opposite = null;
            }
            public String toString()
            {
                return "v="+initialVertex+" w="+finalVertex+" iTri="+iTri+" iEdgeOnTri="+iEdgeOnTri+" opp="+(opposite==null?"null   ":"nonnull")+" val="+getVal();
            }
        } // class DirectedEdge

        private static int getContourEdges(
                        DirectedEdge contourEdges[/*nContours*/][/*nVertsThisContour*/], // result goes here
                        int contours[/*nContours*/][/*nVertsThisContour*/],
                        int nContours)
        {
            int nEdges = Arrays.subarrayLength(contours,
                                               0, nContours,
                                               2, // depth
                                               null); // which
            DirectedEdge edges[] = new DirectedEdge[nEdges];
            {
                int iEdge = 0;
                for (int iContour = 0; (iContour) < (nContours); ++iContour)
                {
                    int contour[] = contours[iContour];
                    DirectedEdge edgesThisContour[] = contourEdges[iContour];
                    int nVertsThisContour = contour.length;
                    for (int iVertThisContour = 0; (iVertThisContour) < (nVertsThisContour); ++iVertThisContour)
                    {
                        DirectedEdge edge =
                            new DirectedEdge(contour[iVertThisContour],
                                             contour[(iVertThisContour+1)%nVertsThisContour],
                                             iContour,
                                             iVertThisContour);
                        edges[iEdge++] = edge;
                        edgesThisContour[iVertThisContour] = edge;
                    }
                }
                do { if (!(iEdge == nEdges)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+370 +"): " + "iEdge == nEdges" + ""); } while (false);
            }
            SortStuff.Comparator edgeComparator = new SortStuff.Comparator() {
                public int compare(Object _a, Object _b)
                {
                    DirectedEdge a = (DirectedEdge)_a;
                    DirectedEdge b = (DirectedEdge)_b;
                    return a.initialVertex < b.initialVertex ? -1 :
                           a.initialVertex > b.initialVertex ? 1 :
                           a.finalVertex < b.finalVertex ? -1 :
                           a.finalVertex > b.finalVertex ? 1 : 0;
                }
            };
            SortStuff.sort(edges, edgeComparator);

            int nInternalEdges = 0; // and counting
            DirectedEdge key = new DirectedEdge();
            for (int iEdge = 0; (iEdge) < (nEdges); ++iEdge)
            {
                DirectedEdge edge = edges[iEdge];
                int initialVertex = edge.initialVertex;
                int finalVertex = edge.finalVertex;
                if (initialVertex < finalVertex) // only need to do this in one of the directions
                {
                    key.initialVertex = finalVertex;
                    key.finalVertex = initialVertex;
                    int i = SortStuff.bsearch(edges, key, edgeComparator);
                    if (i != -1)
                    {
                        DirectedEdge oppositeEdge = edges[i];
                        edge.opposite = oppositeEdge;
                        oppositeEdge.opposite = edge;
                        nInternalEdges++;
                    }
                }
            }
            return nInternalEdges;
        } // getContourEdges

        // The diagonal is between a and c.
        // A negative comfort means it would be better between b and d.
        // I.e. we want negative if d is inside the circle abc.
        //     http://www-2.cs.cmu.edu/~quake/robust.html
        // (XXX oops! that page has wrong subscripts, had to get it from
        // the paper)
        //
        // XXX might be good to try to normalize it somehow? think about it
        //
        // The scale of this is approximately the fourth power
        // of the scale of the coordinates.
        //
        static double calcQuadComfort(double a[],
                                      double b[],
                                      double c[],
                                      double d[],
                                      int xAxis, int yAxis)
        {
            if (false)
            {
                System.out.println("\"===================\"" + " = " + ("==================="));
                System.out.println("a" + " = " + VecMath.toString(a));
                System.out.println("b" + " = " + VecMath.toString(b));
                System.out.println("c" + " = " + VecMath.toString(c));
                System.out.println("d" + " = " + VecMath.toString(d));
            }

            double ax = a[xAxis];
            double ay = a[yAxis];
            double bx = b[xAxis];
            double by = b[yAxis];
            double cx = c[xAxis];
            double cy = c[yAxis];
            double dx = d[xAxis];
            double dy = d[yAxis];
            // XXX allocation
            double M[][] = {
                {ax-dx, ay-dy, ((ax-dx)*(ax-dx)) + ((ay-dy)*(ay-dy))},
                {bx-dx, by-dy, ((bx-dx)*(bx-dx)) + ((by-dy)*(by-dy))},
                {cx-dx, cy-dy, ((cx-dx)*(cx-dx)) + ((cy-dy)*(cy-dy))},
            };
            double det = VecMath.det(M);

            if (false)
            {
                System.out.println("det" + " = " + (det));
                System.out.println("-det" + " = " + (-det));

                System.out.println("\"===================\"" + " = " + ("==================="));
            }

            // empirically, a positive determinant means inside,
            // which is the opposite of what we want...
            return -det;
        } // calcQuadComfort




        private static class Heap
        {
            private static class HeapItem
            {
                private int indexInHeap;
                private double val;
                public double getVal()
                {
                    return val;
                }
            } // class HeapItem

            private HeapItem heap[];
            private int n; // current number of elements in heap

            public Heap(int max)
            {
                heap = new HeapItem[max];
                n = 0;
            }
            public void reset(int max)
            {
                if (max > heap.length)
                    heap = new HeapItem[max];
                n = 0;
            }

            public HeapItem min()
            {
                do { if (!(n > 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/TriangulationOptimizer.prejava"+"("+497 +"): " + "n > 0" + ""); } while (false);
                return heap[0];
            }
            public void add(HeapItem item, double val)
            {
                item.val = val;
                heap[n] = item;
                item.indexInHeap = n;
                n++;
                adjust(item, val);
            }


            public void adjust(HeapItem item, double val)
            {
                item.val = val;

                int i = item.indexInHeap;
                int iParent;
                if (i > 0
                    && val < heap[(iParent = (((i)-1)>>1))].val)
                {
                    do
                    {
                        // swap i with iParent
                        heap[i] = heap[iParent];
                        heap[i].indexInHeap = i;
                        i = iParent;
                    }
                    while (i > 0
                        && val < heap[(iParent = (((i)-1)>>1))].val);
                }
                else
                {
                    while (true)
                    {
                        int iChild0 = (((i)<<1)+1);
                        if (iChild0 >= n)
                            break; // at leaf of tree; done

                        int iChild1 = (((i)<<1)+2);
                        int iChild = (iChild1 < n
                                   && heap[iChild1].val < heap[iChild0].val ? iChild1 : iChild0);

                        if (val > heap[iChild].val)
                        {
                            // swap i with smaller of its children
                            heap[i] = heap[iChild];
                            heap[i].indexInHeap = i;
                            i = iChild;
                        }
                        else
                            break; // less than children; done
                    }
                }

                heap[i] = item;
                item.indexInHeap = i;
            } // adjust
        } // class Heap



} // class TriangulationOptimizer
