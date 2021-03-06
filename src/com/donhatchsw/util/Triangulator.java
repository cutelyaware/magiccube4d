// 2 # 1 "com/donhatchsw/util/Triangulator.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/Triangulator.prejava"
//
// Triangulator.h
// Functions to triangulate polygons.





//
// Note: Nothing here is really optimized.
//       An optimized implementation
//       would store all needed temporary arrays,
//       to avoid repeated allocations.
// XXX and it would fill a result array rather than allocating a new one!
//

package com.donhatchsw.util;

// 24 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 28 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 30 # 20 "com/donhatchsw/util/Triangulator.prejava" 2

/**
*  Functions to triangulate polygons.
* <p>
*  The polygon should all lie (approximately) in a plane;
*  all calculations are done in the plane of the two coordinate
*  axes in which the bounding box of the polygon is largest.
*  <!-- XXX NOT any more, since that's wrong! re-do this comment -->
* <p>
*  The polygons can consist of multiple contours and multiple connected
*  components.  Contours must be consistently oriented;
*  that is, all outer contours must be CCW and all inner contours CW,
*  or vice-versa.
* <p>
*  The contours must be simple (non-self-intersecting),
*  and pairs of contours must not intersect either.
* 
*/
public class Triangulator
{
    //
    // Debugging setting...
    // I just set this appropriately and recompile.
    // Note it's final, so debugging stuff gets compiled out
    // when not debugging.
    //     0 = nothing
    //     1 = on failure  (XXX nothing there currently, just throws exception)
    //     2 = basic flow (no big output for big contours)
    //     3 = full inputs and outputs on entry and exit from major function
    //     4 = full contour contents at every step that I needed to debug
    //
    private static final int verboseLevel = 0;

    /**
    *  Triangulate a simple polygon.
    *  The polygon may pass through vertices more than once.
    *  The contour must be correctly oriented (CCW)
    *  in the xAxis,yAxis plane.
    */
    public int[][/*3*/] triangulateSimple(double verts[][],
                                          int contour[],
                                          int contourLength,
                                          int xAxis, int yAxis,
                                          double eps,
                                          boolean optimize)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "            ";
            System.out.println(indentString + "in triangulateSimple");
            System.out.println(indentString + "    nVerts = "+verts.length+"");
            if (verboseLevel >= 3)
            {
                System.out.println(indentString + "    verts = ");
                System.out.println(Arrays.toStringNonCompact(verts, indentString+"        ", "    "));
            }
            System.out.println(indentString + "    contourLength = "+contourLength);
            if (verboseLevel >= 3)
                System.out.println(indentString + "    contour = " + Arrays.toStringCompact(Arrays.subarray(contour, 0, contourLength)));
        }

        //
        // Check for trivial cases...
        //
        {
            if (contourLength == 3)
                return new int[/*1*/][/*3*/] {{contour[0],contour[1],contour[2]}};
            if (contourLength < 3)
                return new int[/*0*/][/*3*/] {};
        }

        // Replace contour by a scratch array that we can modify...
        {
            int scratchContour[] = new int[contourLength];
            System.arraycopy(contour, 0,
                             scratchContour, 0,
                             contourLength);
            contour = scratchContour;
        }

        if (verboseLevel >= 2)
            System.out.println(indentString + "    Removing dups...");

        //
        // Remove any dups...
        // (note, this includes the silly dup
        // made when connecting an inner contour of a single vertex
        // to the outer contour)
        //
        {
            double eps2 = eps*eps; // for area comparisons

            int iNew = 0;
            for (int iOld = 0; (iOld) < (contourLength); ++iOld)
            {
                if (iNew == 0
                 || VecMath.distsqrd(verts[contour[iNew-1]],
                                     verts[contour[iOld]]) > eps2)
                    contour[iNew++] = contour[iOld];
            }
            contourLength = iNew;
            if (contourLength >= 2
             && VecMath.distsqrd(verts[contour[0]],
                                 verts[contour[contourLength-1]]) <= eps2)
                contourLength--;
        }

        if (verboseLevel >= 2)
            System.out.println(indentString + "    contourLength = "+contourLength);
        if (verboseLevel >= 4)
            System.out.println(indentString + "    contour = " + Arrays.toStringCompact(Arrays.subarray(contour, 0, contourLength)));

        //
        // And check for trivial case again.
        // XXX same as above, could be cleaner
        {
            if (contourLength == 3)
                return new int[/*1*/][/*3*/] {{contour[0],contour[1],contour[2]}};
            if (contourLength < 3)
                return new int[/*0*/][/*3*/] {};
        }

        int nTris = contourLength-2;
        int tris[][] = new int[nTris][3];

        //
        // Ear clipping algorithm.
        // There must be at least two ears;
        // find one and clip it, and repeat.
        //
        if (verboseLevel >= 2)
            System.out.println(indentString + "    Clipping ears...");
        for (int iTri = 0; (iTri) < (nTris); ++iTri)
        {
            if (verboseLevel >= 4)
            {
                System.out.println(indentString + "        contourLength = "+contourLength);
                System.out.println(indentString + "            contour = " + Arrays.toStringCompact(Arrays.subarray(contour, 0, contourLength)));
            }

            int tri[] = tris[iTri];

            int i;
            for (i = (contourLength)-1; (i) >= 0; --i)
            {
                int preI = (i-1+contourLength)%contourLength;
                int postI = (i+1)%contourLength;
                double v[] = verts[contour[i]];
                double preV[] = verts[contour[preI]];
                double postV[] = verts[contour[postI]];

                if (twiceTriangleArea(preV, v, postV, xAxis, yAxis) <= 0.)
                    continue; // i is a concave or flat vertex, so it's not an ear

                int j;
                for (j = 0; (j) < (contourLength); ++j)
                {
                    if (j == i
                     || j == preI
                     || j == postI)
                        continue; // j is i or a neighbor of i

                    int preJ = (j-1+contourLength)%contourLength;
                    int postJ = (j+1)%contourLength;
                    double w[] = verts[contour[j]];
                    double preW[] = verts[contour[preJ]];
                    double postW[] = verts[contour[postJ]];

                    if ((((twiceTriangleArea(preW, w, postW, xAxis, yAxis))-(0.)) > eps*eps))
                        continue; // j is a convex vertex; we only need to check concave or flat (or spike) ones XXX do we need to check flat ones? think about whether a dot product test would also work or be better, or maybe whether a full angle test or the equivalent
                    if ((((0.)-(twiceTriangleArea(preV, v, w, xAxis, yAxis))) <= eps*eps)
                     && (((0.)-(twiceTriangleArea(v, postV, w, xAxis, yAxis))) <= eps*eps)
                     && (((0.)-(twiceTriangleArea(postV, preV, w, xAxis, yAxis))) <= eps*eps))
                        break; // contains w (maybe just barely), so i is not an ear
                }
                if (j == contourLength)
                {
                    //
                    // It didn't contain any concave vertices,
                    // so i is an ear.  Clip it off.
                    //
                    if (verboseLevel >= 4)
                    {
                        System.out.println(indentString + "            ear = "+contour[i]+" (at index "+i+")");
                    }
                    tri[0] = contour[preI];
                    tri[1] = contour[i];
                    tri[2] = contour[postI];
                    // delete i from contour...
                    System.arraycopy(contour, i+1,
                                     contour, i,
                                     contourLength-(i+1));
                    contourLength--;
                    break; // start another ear search
                }
            }
            do { if (!(i >= 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+217 +"): " + "i >= 0" + ""); } while (false); // found an ear
        }
        do { if (!(contourLength == 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+219 +"): " + "contourLength == 2" + ""); } while (false);
        if (verboseLevel >= 2)
            System.out.println(indentString + "    Done clipping ears.");

        if (optimize)
        {
            if (verboseLevel >= 2)
                System.out.println(indentString + "    calling optimize");
            TriangulationOptimizer.optimizeTriangulationInPlace(verts, tris, nTris, xAxis, yAxis, eps);
            if (verboseLevel >= 2)
                System.out.println(indentString + "    returned from optimize");
            if (verboseLevel >= 3)
            {
                System.out.println(indentString + "    verts = ");
                System.out.println(Arrays.toStringNonCompact(verts, indentString+"        ", "    "));
                System.out.println(indentString + "    tris = " + Arrays.toStringCompact(Arrays.subarray(tris, 0, nTris)));
            }
        }

        if (verboseLevel >= 2)
            System.out.println(indentString + "out triangulateSimple");

        return tris;
    } // triangulateSimple


    /**
    *  Merge a connected component (consisting of an outer contour
    *  and zero or more inner contours) into a single contour,
    *  by adding out-and-back edges to each inner contour.
    */
    public int[] simplifyConnected(double verts[][],
                                   int contours[][], // outer and inners
                                   int nContours,
                                   int xAxis, int yAxis,
                                   double eps)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "            ";
            System.out.println(indentString + "in simplifyConnected");
            System.out.println(indentString + "    nVerts = "+verts.length+"");
            if (verboseLevel >= 3)
            {
                System.out.println(indentString + "    verts = ");
                System.out.println(Arrays.toStringNonCompact(verts, indentString+"        ", "    "));
            }
            System.out.println(indentString + "    nContours = "+nContours);
            if (verboseLevel >= 3)
                System.out.println(indentString + "    contours = " + Arrays.toStringCompact(Arrays.subarray(contours, 0, nContours)));
        }

        if (nContours == 1)
            return contours[0];
        if (nContours == 0)
            return new int[] {};

        int bigContourMax = Arrays.subarrayLength(contours, 0, nContours, 2, null)
                             + (nContours-1) * 2; // 2 extra for every inner contour, to connect to outer contour, there and back.

        int bigContour[] = new int[bigContourMax];
        int scratch[] = new int[bigContourMax];
        int bigContourLength = 0; // and counting

        if (verboseLevel >= 2)
            System.out.println(indentString + "    Merging into one big contour...");
        for (int iContour = 0; (iContour) < (nContours); ++iContour)
        {
            int contour[] = contours[iContour];
            int contourLength = contour.length;
            if (verboseLevel >= 4)
            {
                System.out.println(indentString + "        iContour = " + iContour);
            }
            if (verboseLevel >= 4)
            {
                System.out.println(indentString + "            bigContour = " + Arrays.toStringCompact(Arrays.subarray(bigContour,0,bigContourLength)));
                System.out.println(indentString + "            contour = " + Arrays.toStringCompact(contour));
            }

            if (iContour == 0) // contour is the outer contour
            {
                System.arraycopy(contour, 0,
                                 bigContour, bigContourLength,
                                 contourLength);
                bigContourLength += contourLength;
            }
            else // contour is an inner contour
            {
                //
                // Find innerI = index of the leftmost vertex
                // on this inner contour, with biggest angle among
                // all such
                //
                int innerI;
                double innerI_xCoord;
                {
                    // XXX duplicate code below, should make a function
                    int bestI = -1;
                    double bestXcoord = Double.POSITIVE_INFINITY; // and counting
                    double bestAngle = Double.NaN;
                    for (int iVertOnContour = 0; (iVertOnContour) < (contourLength); ++iVertOnContour)
                    {
                        double thisXcoord = verts[contour[iVertOnContour]][xAxis];
                        if (thisXcoord <= bestXcoord)
                        {
                            double vPrev[] = verts[contour[(iVertOnContour-1+contourLength)%contourLength]];
                            double v[] = verts[contour[iVertOnContour]];
                            double vNext[] = verts[contour[(iVertOnContour+1)%contourLength]];
                            double thisAngle = angle(v,vNext,
                                                     v,vPrev,
                                                     xAxis,yAxis);

                            if (thisXcoord < bestXcoord
                             || thisAngle > bestAngle) // and thisXcoord == bestXcoord, from previous test
                            {
                                bestI = iVertOnContour;
                                bestXcoord = thisXcoord;
                                bestAngle = thisAngle;
                            }
                        }

                    }
                    do { if (!(bestI != -1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+343 +"): " + "bestI != -1" + ""); } while (false);
                    innerI = bestI;
                    innerI_xCoord = bestXcoord;
                }

                if (verboseLevel >= 4)
                    System.out.println(indentString + "                innerI = " + innerI + " (vertex "+contour[innerI]+")");

                //
                // Find outerI = index of a vertex on the outer (big)
                // contour that is visible from vertex innerI
                //
                int outerI;
                {
                    outerI = -1;
                    double innerV[] = verts[contour[innerI]];
                    for (int iVertOnBigContour = 0; (iVertOnBigContour) < (bigContourLength); ++iVertOnBigContour)
                    {
                        if (verboseLevel >= 4)
                            System.out.println(indentString + "                    outerI candidate = " + iVertOnBigContour + " (vertex "+bigContour[iVertOnBigContour]+")");

                        double outerV[] = verts[bigContour[iVertOnBigContour]];
                        if (outerV[xAxis] >= innerI_xCoord)
                            continue; // need not consider anything to right of innerV (or vertical from it)
                        if (verboseLevel >= 4)
                            System.out.println(indentString + "                        seems to be to left of innerV");

                        double preOuterV[] = verts[bigContour[(iVertOnBigContour-1+bigContourLength)%bigContourLength]];
                        double postOuterV[] = verts[bigContour[(iVertOnBigContour+1)%bigContourLength]];

                        //
                        // See whether outerV points in the correct
                        // direction...
                        //
                        boolean outerVIsConvex = (twiceTriangleArea(preOuterV, outerV, postOuterV, xAxis, yAxis) >= 0.);
                        if (verboseLevel >= 4)
                            System.out.println(indentString + "                        outerVIsConvex = " + outerVIsConvex);
                        if (outerVIsConvex)
                        {
                            // innerV is required to be solidly on correct side
                            // of both edges
                            if ((((twiceTriangleArea(preOuterV, outerV, innerV, xAxis, yAxis))-(0.)) <= eps*eps)
                             || (((twiceTriangleArea(outerV, postOuterV, innerV, xAxis, yAxis))-(0.)) <= eps*eps))
                                continue;
                        }
                        else
                        {
                            // innerV is required to be solidly on correct side
                            // of at least one of the two edges
                            if ((((twiceTriangleArea(preOuterV, outerV, innerV, xAxis, yAxis))-(0.)) <= eps*eps)
                             && (((twiceTriangleArea(outerV, postOuterV, innerV, xAxis, yAxis))-(0.)) <= eps*eps))
                                continue;
                        }
                        if (verboseLevel >= 4)
                            System.out.println(indentString + "                        seems to be facing the right direction");

                        //
                        // See whether outerV is visible...
                        // It's visible if the segment joining innerV
                        // with outerV doesn't cross any edge
                        // of the outer contour.
                        // XXX there's an issue about getting the right
                        // XXX instance of outerV in the case of multiple
                        // XXX instances, but I think maybe it's
                        // XXX okay as long as the original polygon
                        // XXX had no multiple instances.  However,
                        // XXX I'd like to make it more robust
                        //

                        int iOuterEdge;
                        for (iOuterEdge = 0; (iOuterEdge) < (bigContourLength); ++iOuterEdge)
                        {
                            int v0 = bigContour[iOuterEdge];
                            int v1 = bigContour[(iOuterEdge+1)%bigContourLength];
                            if (closedSegmentsCross(verts[v0], verts[v1],
                                                    innerV, outerV,
                                                    xAxis, yAxis,
                                                    eps))
                                break; // not visible
                        }
                        if (iOuterEdge < bigContourLength)
                            continue; // not visible

                        if (verboseLevel >= 4)
                            System.out.println(indentString + "                        it's visible!");
                        outerI = iVertOnBigContour; // visible!
                        break;
                    }
                    do { if (!(outerI != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+431 +"): " + "outerI != -1" + ""); } while (false);
                }

                if (verboseLevel >= 4)
                    System.out.println(indentString + "                outerI = " + outerI + " (vertex "+bigContour[outerI]+")");

                bigContourLength = mergeContours(scratch,
                              bigContour, bigContourLength, outerI,
                              contour, contourLength, innerI);
                // put it back into bigContour...
                {
                    int temp[];
                    {temp=(scratch);scratch=(bigContour);bigContour=(temp);};
                }
            }
            if (verboseLevel >= 4)
            {
                System.out.println(indentString + "            bigContour = " + Arrays.toStringCompact(Arrays.subarray(bigContour,0,bigContourLength)));
            }
        }
        do { if (!(bigContourLength == bigContourMax)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+451 +"): " + "bigContourLength == bigContourMax" + ""); } while (false); // would be less if we removed dups, but we don't (we leave that to triangulate). XXX but we probably should, now that this function can be used standalone!
        if (verboseLevel >= 2)
            System.out.println(indentString + "    Done merging.");

        bigContour = (int[])Arrays.subarray(bigContour, 0, bigContourLength);

        if (verboseLevel >= 2)
            System.out.println(indentString + "out simplifyConnected");
        return bigContour;
    } // simplifyConnected


    /**
    *  Triangulate a connected multiple-contour polygon,
    *  consisting of an outer contour and zero or more inner contours.
    *  Contours may have as few as one point
    *  (a one-point inner contour is a Steiner point).
    *  All the contours must be correctly-oriented (CCW)
    *  in the xAxis,yAxis plane.
    */
    public int[][/*3*/] triangulateConnected(double verts[][],
                                             int contours[][], // outer and inners
                                             int nContours,
                                             int xAxis, int yAxis,
                                             double eps,
                                             boolean optimize)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "        ";
            System.out.println(indentString + "in triangulateConnected");
            System.out.println(indentString + "    nVerts = "+verts.length+"");
            if (verboseLevel >= 3)
            {
                System.out.println(indentString + "    verts = ");
                System.out.println(Arrays.toStringNonCompact(verts, indentString+"        ", "    "));
            }
            System.out.println(indentString + "    nContours = "+nContours);
            if (verboseLevel >= 3)
                System.out.println(indentString + "    contours = " + Arrays.toStringCompact(Arrays.subarray(contours, 0, nContours)));
        }

        int bigContour[] = simplifyConnected(verts,
                                             contours,
                                             nContours,
                                             xAxis, yAxis,
                                             eps);

        int tris[][/*3*/] = triangulateSimple(verts,
                                              bigContour,
                                              bigContour.length,
                                              xAxis, yAxis,
                                              eps,
                                              optimize);

        if (verboseLevel >= 2)
            System.out.println(indentString + "out triangulateConnected");
        return tris;
    } // triangulateConnected


    /**
    *  Triangulate an arbitrary non-self-intersecting
    *  correctly-oriented multiple-contour polygon,
    *  possibly having multiple connected components.
    *  Inner contours may have as few as one point
    *  (a one-point inner contour is a Steiner point).
    *  All the contours must be correctly-oriented (CCW)
    *  in the xAxis,yAxis plane.
    * 
    *  XXX ASSUMPTION: input has no Infs or NaNs.
    * 
    *  XXX ASSUMPTION: an edge cannot occur twice in the polygon
    *                  (regardless of direction), though a vertex can.
    *                   
    *  XXX ASSUMPTION: the vertices of one contour cannot all
    *                  lie on the boundary of another.
    */
    public int[][/*3*/] triangulate(double verts[][],
                                    int contours[][],
                                    int nContours,
                                    int xAxis, int yAxis,
                                    double eps,
                                    boolean optimize)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "    ";
            System.out.println(indentString + "in triangulate (xAxis="+xAxis+" yAxis="+yAxis+")");
        }
        if (nContours == 1)
            return triangulateSimple(verts, contours[0], contours[0].length, xAxis, yAxis, eps, optimize);
        if (nContours == 0)
            return new int[][] {};

        //
        // The strategy will be to divide contours into groups such that
        // the first contour in each group is an outer contour,
        // and the others in the group are its inner contours.
        // Each such group forms a connected component
        // of the polygon, on which we can call triangulateConnected().
        //

        int connectedComponents[][][] = groupIntoConnectedComponents(verts, contours, nContours, xAxis, yAxis, eps);
        int nComponents = connectedComponents.length;

        //
        // Full triangulation
        // is concatenation of triangulations of connected components.
        //
        int tris[][];
        {
            int nTris;
            {
                nTris = 0; // and counting
                for (int iComponent = 0; (iComponent) < (nComponents); ++iComponent)
                {
                    int component[][] = connectedComponents[iComponent];
                    int nContoursThisComponent = component.length;
                    for (int iContourThisComponent = 0; (iContourThisComponent) < (nContoursThisComponent); ++iContourThisComponent)
                    {
                        int contourLength = component[iContourThisComponent].length;
                        if (iContourThisComponent == 0)
                        {
                            if (contourLength >= 3) // outer contours of length <= 2 produce no triangles
                                nTris += contourLength - 2;
                        }
                        else // it's an inner contour
                        {
                            nTris += contourLength + 2;
                        }
                    }
                }
            }
            tris = new int[nTris][];
            int iTri = 0;

            for (int iComponent = 0; (iComponent) < (nComponents); ++iComponent)
            {
                int component[][] = connectedComponents[iComponent];
                int groupTris[][/*3*/] = triangulateConnected(verts,
                                                              component,
                                                              component.length,
                                                              xAxis, yAxis,
                                                              eps,
                                                              optimize);
                int nGroupTris = groupTris.length;
                System.arraycopy(groupTris, 0,
                                 tris, iTri,
                                 nGroupTris);
                iTri += nGroupTris;
            }

            // actual number of tris can be less than our computed nTris,
            // if dups were encountered or if there were steiner points.
            if (iTri < nTris)
            {
                tris = (int[][])Arrays.subarray(tris, 0, iTri);
                nTris = iTri;
            }
        }

        if (verboseLevel >= 2)
            System.out.println(indentString + "out triangulate (xAxis="+xAxis+" yAxis="+yAxis+")");

        return tris;
    } // triangulate with known work plane

    private int[][][] groupIntoConnectedComponents(double verts[][],
                                                   int contours[][],
                                                   int nContours,
                                                   int xAxis, int yAxis,
                                                   double eps)
    {
        String indentString = null;
        if (verboseLevel >= 2)
        {
            indentString = "        ";
            System.out.println(indentString + "in groupIntoConnectedComponents (xAxis="+xAxis+" yAxis="+yAxis+")");
        }
        if (nContours == 1)
        {



            return new int[][][] {{contours[0]}};

        }
        if (nContours == 0)
            return new int[][][] {};

        int leftmostVertOnContour[] = new int[nContours];
        final double leftmostXcoordOnContour[] = new double[nContours];
        final double angleAtLeftmostVertOnContour[] = new double[nContours];
        {
            for (int iContour = 0; (iContour) < (nContours); ++iContour)
            {
                int contour[] = contours[iContour];
                int nVertsOnContour = contour.length;

                // XXX duplicate code above, should make a function
                int bestI = -1;
                double bestXcoord = Double.POSITIVE_INFINITY; // and counting
                double bestAngle = Double.NaN;
                for (int iVertOnContour = 0; (iVertOnContour) < (nVertsOnContour); ++iVertOnContour)
                {
                    double thisXcoord = verts[contour[iVertOnContour]][xAxis];
                    if (thisXcoord <= bestXcoord)
                    {
                        double vPrev[] = verts[contour[(iVertOnContour-1+nVertsOnContour)%nVertsOnContour]];
                        double v[] = verts[contour[iVertOnContour]];
                        double vNext[] = verts[contour[(iVertOnContour+1)%nVertsOnContour]];
                        double thisAngle = angle(v,vNext,
                                                 v,vPrev,
                                                 xAxis,yAxis);

                        if (thisXcoord < bestXcoord
                         || thisAngle > bestAngle) // and thisXcoord == bestXcoord, from previous test
                        {
                            bestI = iVertOnContour;
                            bestXcoord = thisXcoord;
                            bestAngle = thisAngle;
                        }
                    }

                }
                do { if (!(bestI != -1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+679 +"): " + "bestI != -1" + ""); } while (false);
                leftmostVertOnContour[iContour] = bestI;
                leftmostXcoordOnContour[iContour] = bestXcoord;
                angleAtLeftmostVertOnContour[iContour] = bestAngle;
            }
        }

        //
        // Sort the contours by leftmost vertex.
        // Among those with a common leftmost vertex,
        // put the one with the largest angle at that vertex first.
        // This guarantees that whenever one contour encloses another,
        // the enclosing contour will occur earlier on the list.
        //
        int contoursOrder[] = new int[nContours];
        {
            VecMath.identityperm(contoursOrder);
            SortStuff.sort(contoursOrder, new SortStuff.IntComparator() {
                public int compare(int i, int j)
                {
                    double iX = leftmostXcoordOnContour[i];
                    double jX = leftmostXcoordOnContour[j];
                    if (iX < jX) return -1;
                    if (iX > jX) return 1;
                    double iAngle = angleAtLeftmostVertOnContour[i];
                    double jAngle = angleAtLeftmostVertOnContour[j];
                    if (iAngle > jAngle) return -1; // bigger first
                    if (iAngle < jAngle) return 1; // bigger first
                    return 0;
                }
            });
        }

        //
        // Make a tree whose nodes are the contours,
        // ordered by contour inclusion.
        //
        int root;
        int firstChild[] = new int[nContours];
        int nextSibling[] = new int[nContours];
        {
            VecMath.fillvec(firstChild, -1);
            VecMath.fillvec(nextSibling, -1);

            root = 0;
            int rootContour[] = contours[contoursOrder[root]];
            for (int iContour = 1; iContour < nContours; ++iContour) // skip 0, it's already on the tree as the root
            {
                int contour[] = contours[contoursOrder[iContour]];
                int nVertsOnContour = contour.length;
                // Hang iContour on the tree.
                // Walk down the tree starting with root...
                for (int iNode = root; ;)
                {
                    int nodeContour[] = contours[contoursOrder[iNode]];
                    //
                    // Find density of nodeContour at some vertex
                    // of contour iContour...
                    //
                    int density = 0; // shut up compiler
                    {
                        int iVertOnContour;
                        for (iVertOnContour = 0; (iVertOnContour) < (nVertsOnContour); ++iVertOnContour)
                        {
                            density = contourDensity(verts,
                                                     nodeContour,
                                                     nodeContour.length,
                                                     verts[contour[iVertOnContour]],
                                                     xAxis, yAxis,
                                                     eps);
                            if ((density & 1) == 0)
                                break; // solidly in or out
                        }
                        do { if (!(iVertOnContour < nVertsOnContour)) throw new Error("Assumption failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+752 +"): " + "iVertOnContour < nVertsOnContour" + ""); } while (false); // the boundary of one contour cannot completely include the boundary of another
                    }

                    do { if (!((density & 1) == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+755 +"): " + "(density & 1) == 0" + ""); } while (false); // solidly in or out
                    // 0 means outside
                    // 2 means inside CCW (outer) contour
                    // -2 means inside CW (inner) contour
                    do { if (!(density == 0 || density == 2 || density == -2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+761 +"): " + "density == 0 || density == 2 || density == -2" + ""); } while (false);



                    boolean isEnclosedByNode = ((density&2) != 0);
                    if (isEnclosedByNode)
                    {
                        // proceed on to children of node
                        if (firstChild[iNode] == -1)
                        {
                            firstChild[iNode] = iContour;
                            break;
                        }
                        iNode = firstChild[iNode];
                    }
                    else
                    {
                        // proceed on to next sibling of node
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

        //
        // The outer contours are the ones whose depth in the tree
        // is even.
        //
        int contourDirections[] = new int[nContours]; // 0=uninit, 1=CCW, -1=CW
        {
            VecMath.fillvec(contourDirections, 0);
            contourDirections[root] = 1; // CCW, i.e. outer
            for (int iContour = 0; (iContour) < (nContours); ++iContour)
            {
                int dir = contourDirections[iContour];
                do { if (!(dir != 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+799 +"): " + "dir != 0" + ""); } while (false);
                if (firstChild[iContour] != -1)
                    contourDirections[firstChild[iContour]] = -dir;
                if (nextSibling[iContour] != -1)
                    contourDirections[nextSibling[iContour]] = dir;
            }
        }

        //
        // The number of connected components is the number
        // of CCW contours.
        //
        int nConnectedComponents;
        {
            nConnectedComponents = 0; // and counting
            for (int iContour = 0; (iContour) < (nContours); ++iContour)
                if (contourDirections[iContour] == 1)
                    nConnectedComponents++;
        }
        int connectedComponents[][][] = new int[nConnectedComponents][][];
        {
            int iConnectedComponent = 0;
            int group[][] = new int[nContours][]; // worst case
            for (int iContour = 0; (iContour) < (nContours); ++iContour)
            {
                if (contourDirections[iContour] == 1) // if it's outer
                {
                    int groupSize = 0; // and counting
                    group[groupSize++] = contours[contoursOrder[iContour]];
                    for (int iNode = firstChild[iContour];
                         iNode != -1;
                         iNode = nextSibling[iNode])
                        group[groupSize++] = contours[contoursOrder[iNode]];
                    connectedComponents[iConnectedComponent++] = (int[][])Arrays.subarray(group, 0, groupSize);
                }
            }
            do { if (!(iConnectedComponent == nConnectedComponents)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+835 +"): " + "iConnectedComponent == nConnectedComponents" + ""); } while (false);
        }

        if (verboseLevel >= 2)
            System.out.println(indentString + "out groupIntoConnectedComponents (xAxis="+xAxis+" yAxis="+yAxis+")");

        return connectedComponents;
    } // groupIntoConnectedComponents


    /**
    *  Return a list of simple contours,
    *  one for each connected component of the polygon,
    *  assuming a known work plane.
    *  (Each simple contour is the merge of the outer contour
    *  and inner contours of that component.)
    */
    public int[][] simplify(double verts[][],
                            int contours[][],
                            int nContours,
                            int xAxis, int yAxis,
                            double eps)
    {
        int components[][][] = groupIntoConnectedComponents(verts,
                                                            contours,
                                                            nContours,
                                                            xAxis, yAxis,
                                                            eps);
        int nComponents = components.length;
        int result[][] = new int[nComponents][];
        for (int iComponent = 0; (iComponent) < (nComponents); ++iComponent)
        {
            int component[][] = components[iComponent];
            result[iComponent] = simplifyConnected(verts,
                                                   component,
                                                   component.length,
                                                   xAxis, yAxis,
                                                   eps);
        }
        return result;
    } // simplify, known work plane

    /**
    *  Triangulate an arbitrary non-self-intersecting
    *  consistently-oriented multiple-contour polygon.
    *  Contours may have as few as one point
    *  (a one-point inner contour is a Steiner point).
    *  All the contours must have the same orientation
    *  (i.e. outer ones CCW and inner ones CW, when viewed
    *  in some direction).
    */
    public int[][/*3*/] triangulate(double verts[][],
                                    int contours[][],
                                    int nContours,
                                    double eps,
                                    boolean optimize)
    {
        if (verboseLevel >= 2)
            System.out.println("in triangulate");
        if (nContours == 1)
        {
            int contourLength = contours[0].length;
            if (contourLength <= 2)
                return new int[][] {};
            if (contourLength == 3)
            {



                return new int[][] {contours[0]};

            }
        }
        int nVerts = verts.length;
        if (nContours == 0
         || nVerts == 0)
            return new int[][] {};

        int axes[] = new int[2];
        selectPolygonWorkPlane(axes,verts,contours,nContours);
        int tris[][/*3*/] = triangulate(verts, contours, nContours, axes[0], axes[1], eps, optimize);

        if (verboseLevel >= 2)
            System.out.println("out triangulate");

        return tris;

    } // triangulate, unknown work plane



    /**
    *  Return a list of simple contours,
    *  one for each connected component of the polygon.
    *  (Each simple contour is the merge of the outer contour
    *  and inner contours of that component.)
    */
    public int[][] simplify(double verts[][],
                            int contours[][],
                            int nContours,
                            double eps)
    {
        if (verboseLevel >= 2)
            System.out.println("in simplify");

        if (nContours == 1)
        {



            return new int[][] {contours[0]};

        }
        int nVerts = verts.length;
        if (nContours == 0
         || nVerts == 0)
            return new int[][] {};

        int axes[] = new int[2];
        selectPolygonWorkPlane(axes,verts,contours,nContours);
        int result[][] = simplify(verts, contours, nContours, axes[0], axes[1], eps);
        if (verboseLevel >= 2)
            System.out.println("out simplify");

        return result;
    } // simplify, unknown work plane



    /**
    *  Find the pair of coordinate axes in which
    *  the magnitude of the area of the polygon is greatest,
    *  and order those two axes so that the area is positive.
    *  NOTE, this doesn't necessarily work for self-intersecting
    *  polygons (e.g. bowtie), but we don't claim to handle
    *  such polygons anyway.
    * 
    *  (A strategy that doesn't work is to find the plane
    *  in which the bounding box is largest...
    *  a counterexample is the quadrilateral
    *  {{4,0,0},{0,4,0},{0,4,2},{4,0,2}}.)
    */
    public static void selectPolygonWorkPlane(int resultAxes[/*2*/],
                                              double verts[][],
                                              int contours[][],
                                              int nContours)
    {
        // Find the largest triangle
        // in trivial triangulations of all the contours,
        // and then pick the plane in which the area of that triangle
        // largest, with the two axes ordered so that the polygon
        // area is positive.
        int nAxes = (verts.length == 0 ? 2 : verts[0].length);
        do { if (!(nAxes >= 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+988 +"): " + "nAxes >= 2" + ""); } while (false);

        double bestArea = Double.NaN;
        double absBestArea = -1.; // initialize to impossibly low
        int bestXAxis = -1, bestYAxis = -1;

        for (int yAxis = 0; (yAxis) < (nAxes); ++yAxis)
            for (int xAxis = 0; (xAxis) < (yAxis); ++xAxis)
            {
                double thisArea = twicePolygonArea(verts, contours, nContours, xAxis, yAxis);
                double absThisArea = ((thisArea) < 0 ? -(thisArea) : (thisArea));
                if (absThisArea > absBestArea)
                {
                    absBestArea = absThisArea;
                    bestArea = thisArea;
                    bestXAxis = xAxis;
                    bestYAxis = yAxis;
                }
            }
        do { if (!(absBestArea >= 0.)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+1007 +"): " + "absBestArea >= 0." + ""); } while (false);
        if (bestArea < 0.)
        {
            int temp;
            {temp=(bestXAxis);bestXAxis=(bestYAxis);bestYAxis=(temp);};
        }
        resultAxes[0] = bestXAxis;
        resultAxes[1] = bestYAxis;
    } // selectPolygonWorkPlane

    //
    // Private utilities...
    // XXX a couple of these are public... should document this
    //

        private static boolean closedSegmentsCross(double a0[], double a1[],
                                                   double b0[], double b1[],
                                                   int xAxis, int yAxis,
                                                   double eps)
        {
            // XXX if either segment is zero length, we will wrongly
            // XXX return true
            double eps4 = eps*eps*eps*eps;
            {
                double a0a1b0 = twiceTriangleArea(a0,a1,b0,xAxis,yAxis);
                double a0a1b1 = twiceTriangleArea(a0,a1,b1,xAxis,yAxis);
                if ((((0.)-(a0a1b0 * a0a1b1)) <= eps4))
                    return false;
            }
            {
                double b0b1a0 = twiceTriangleArea(b0,b1,a0,xAxis,yAxis);
                double b0b1a1 = twiceTriangleArea(b0,b1,a1,xAxis,yAxis);
                if ((((0.)-(b0b1a0 * b0b1a1)) <= eps4))
                    return false;
            }
            return true;
        } // closedSegmentsCross

        private static double angle(double A0[], double A1[],
                                    double B0[], double B1[],
                                    int xAxis, int yAxis)
        {
            double Ax = A1[xAxis]-A0[xAxis];
            double Ay = A1[yAxis]-A0[yAxis];
            double Bx = B1[xAxis]-B0[xAxis];
            double By = B1[yAxis]-B0[yAxis];

            double cosAngle = Ax*Bx + Ay*By;
            double sinAngle = Ax*By - Ay*Bx;

            return Math.atan2(sinAngle, cosAngle);
        } // angle

        /** Calculate twice the area of the given triangle projected into the given work plane. */
        public static double twiceTriangleArea(double v0[],
                                               double v1[],
                                               double v2[],
                                               int xAxis, int yAxis)
        {
            double x1 = v1[xAxis]-v0[xAxis];
            double y1 = v1[yAxis]-v0[yAxis];
            double x2 = v2[xAxis]-v0[xAxis];
            double y2 = v2[yAxis]-v0[yAxis];
            return x1*y2 - x2*y1;
        } // twiceTriangleArea

        /** Calculate twice the area of the given contour projected into the given work plane. */
        public static double twiceContourArea(double verts[][],
                                              int contour[],
                                              int contourLength,
                                              int xAxis, int yAxis)
        {
            double twiceAreaSum = 0.;
            if (contourLength >= 3)
            {
                double v0[] = verts[contour[0]];
                double v1[] = verts[contour[1]];
                for (int iTriThisContour = 0; (iTriThisContour) < (contourLength-2); ++iTriThisContour)
                {
                    double v2[] = verts[contour[iTriThisContour+2]];
                    twiceAreaSum += twiceTriangleArea(v0, v1, v2, xAxis, yAxis);
                    v1 = v2; // for next iteration
                }
            }
            return twiceAreaSum;
        } // twiceContourArea

        private static double twicePolygonArea(double verts[][],
                                               int contours[][],
                                               int nContours,
                                               int xAxis, int yAxis)
        {
            double twiceAreaSum = 0.;
            for (int iContour = 0; (iContour) < (nContours); ++iContour)
            {
                int contour[] = contours[iContour];
                twiceAreaSum += twiceContourArea(verts,
                                                 contour,
                                                 contour.length,
                                                 xAxis, yAxis);
            }
            return twiceAreaSum;
        } // twicePolygonArea

        //
        // Contour-point inclusion test.
        // Possible return values are:
        //    ...
        //    -2: inside a CW contour
        //    -1: on boundary of a CW contour
        //     0: outside contour
        //     1: on boundary of a CCW contour
        //     2: inside a CCW contour
        //     ...
        //
        // Taken from Graphics Gems V, chapter 7-2, by Green/Hatch
        //  http://www.acm.org/tog/GraphicsGems/gemsv/ch7-2/pcube.c
        //
        // XXX might miss some cases of being on boundary
        //
        private static int contourDensity(double verts[][],
                                          int contour[],
                                          int contourLength,
                                          double point[],
                                          int xAxis, int yAxis,
                                          double eps)
        {
            if (verboseLevel >= 2)
                System.out.println("        in contourDensity");

            double eps2 = eps*eps; // for area comparisons




            double x = point[xAxis];
            double y = point[yAxis];
            int density = 0;
            for (int iVertOnContour = 0; (iVertOnContour) < (contourLength); ++iVertOnContour)
            {
                double v0[] = verts[contour[iVertOnContour]];
                double v1[] = verts[contour[(iVertOnContour+1)%contourLength]];
                double x0 = v0[xAxis];
                double x1 = v1[xAxis];
                int xDensity = (((x1)>(x)?1:0) - ((x0)>(x)?1:0));
                if (xDensity != 0)
                {
                    double y0 = v0[yAxis];
                    double y1 = v1[yAxis];
                    int yDensity = (((y1)>(y)?1:0) - ((y0)>(y)?1:0));
                    if (yDensity != 0)
                    {
                        double temp = xDensity * ((x-x0)*(y1-y0)
                                                - (y-y0)*(x1-x0));
                        if ((((temp)-(0.)) <= eps2))
                        {
                            if ((((0.)-(temp)) > eps2))
                                density += 2 * xDensity;
                            else
                                density += 1 * xDensity;
                        }
                    }
                    else
                    {
                        if (y0 <= y)
                            density += 2 * xDensity;
                    }
                }
                double area = twiceTriangleArea(v0, v1, point, xAxis, yAxis);
            }

            if (verboseLevel >= 2)
                System.out.println("        out contourDensity, returning "+density+"");

            return density;
        } // contourDensity

        //
        // Merge two contours, by adding an edge from con0[i0] to con1[i1]
        // and back.
        // The length of the resulting contour is len0+len1+2,
        // which the result array must be big enough to hold.
        // The result array must not be the same as either input contour.
        //
        private static int mergeContours(int result[],
                                         int con0[], int len0, int i0,
                                         int con1[], int len1, int i1)
        {
            do { if (!(result != con0 && result != con1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Triangulator.prejava"+"("+1196 +"): " + "result != con0 && result != con1" + ""); } while (false);

            System.arraycopy(con0, i0,
                             result, 0,
                             len0-i0);
            System.arraycopy(con0, 0,
                             result, len0-i0,
                             i0+1);
            System.arraycopy(con1, i1,
                             result, len0+1,
                             len1-i1);
            System.arraycopy(con1, 0,
                             result, len0+1+len1-i1,
                             i1+1);
            return len0 + len1 + 2;
        } // mergeContours

} // class Triangulator
