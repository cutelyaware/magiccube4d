// 2 # 1 "com/donhatchsw/util/Arrows.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/Arrows.prejava"
/*
* Copyright (c) 2005 Don Hatch Software
*/

// 10 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 14 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 16 # 6 "com/donhatchsw/util/Arrows.prejava" 2
//
// Arrows.java -- utilities for drawing arrows
// which have multiple heads (destination points)
// and multiple tails (source points).
//

package com.donhatchsw.util;

/**
 * Utilities for drawing arrows which have multiple heads (destination points)
 * and multiple tails (source points).  The interesting function here
 * is getCurves().
 */

/* not in javadoc for now */
/* <p>
 * ISSUES:
 * <ul>
 *     <li> arrow head direction heuristic often misbehaves, can (and will)
 *          be improved a lot
 *     <li> it works well if an asset is both a src and dst
 *          as long as there is at least one other src and one other dst,
 *          otherwise, it's not really sensible-- should show a loop I think.
 *     <li> do something special for src with multiplicity,
 *          or dst with multiplicity?
 *          (argh, I went to special effort to not let an asset be added multiple times... this makes things more sane when deleting a membership, but don't we want to express that in general? I need to make sure I handle it)
 *     <li> the only thing that has no representation at all
 *          is a no-src no-dst event
 *     <li> think about whether I can jitter or something
 *          to handle multiple events with exact same srcs and dsts
 *     <li> arrowheads on single-sources? not sure (I made it a parameter)
 * </ul>
 * XXX to do for test:
 * <ul>
 *     <li> hold down shift-D in the middle-- gets assertion failures (curves==null)?? doesn't happen when slow.
 *     <li> Clean&center (and Auto Clean) doesn't seem to do anything when only 2 assets
 *     <li> asset fail in mouseReleased, in one-curve mode when I position a control point near the edge
 *     <li> middle mouse should also be way to connect things (same as ctrl-left)
 *     <li> help should go to a help window, not stdout
 *     <li> should adjust radius of what gets shown in red:
 *       <ul>
 *           <li> if control is down, use select radius (obj affected by click)
 *           <li> if shift is down, use infinity (obj affected by 'D')
 *           <li> if neither is down, use delete radius (obj affected by 'd')
 *           <li> if ctrl-dragging, use appropriate drop radius
 *       </ul>
 *     <li> grey out Clean Circle and/or Clean&Center when already clean
 *     <li> do NOT steal focus on startup!! dang it!
 *        <ul>
 *         <li> and what the hell is that beep on startup??? damn!!!
 *        </ul>
 *     <li> thicken lines? nah...
 *     <li> should be passing around tooFarSqrd, not tooFar, maybe
 * </ul>
 */
public class Arrows
{
    private Arrows() {} // non-instantiatable

    /**
     * Get curves representing an event (multi-headed-and-tailed arrow)
     * with zero or more sources and zero or more destinations.
     * <p>
     * Policy of the array returned:
     * the first nSrcs curves will be the "legs"
     * of the srces, the next nDsts curves will be the "legs" of the dsts,
     * the next curve (if any) will be the "handle" or common part
     * in the middle, the rest is any arrow head decoration
     * (which may not be curves at all).  In this way,
     * it's possible to select a particular src or dst of an event.
     * <p>
     * See code for parameter descriptions.
     */
    public static java.awt.Shape[] getCurves(
            double srcs[][/*2*/], int srcInds[], // inds may be null
            double dsts[][/*2*/], int dstInds[], // inds may be null
            double srcRadii[], // keep arrows this far away from srcs
            double dstRadii[], // keep arrows this far away from dsts
            double arrowHeadLength,

            double graphCenterForSingle[/*2*/],
            double arrowLengthForSingle,
            boolean doArrowHeadOnSingleSrc)
    {
        // XXX nSrcs,nDsts should really be params,
        // XXX to allow caller to re-use a big array
        // XXX without having to reallocate to exact size each time
        int nSrcs = (srcInds != null ? srcInds.length : srcs.length);
        int nDsts = (dstInds != null ? dstInds.length : dsts.length);

        //
        // Adjust positions, moved by radius towards the average
        // of all the srcs and dsts.
        //
        if (nSrcs + nDsts >= 1)
        {
            double eventCenter[/*2*/];
            {
                double centers[/*2*/][/*2*/] = {
                    average(srcs, srcInds),
                    average(dsts, dstInds),
                };
                eventCenter = centers[1] == null ? centers[0] :
                              centers[0] == null ? centers[1] :
                              average(centers, null);
                if (nSrcs + nDsts == 1)
                    eventCenter = lerp(graphCenterForSingle, eventCenter, 2.);
            }
            double _srcs[][] = new double[nSrcs][];
            double _dsts[][] = new double[nDsts][];

            if (srcRadii != null && srcRadii.length > 0)
            {
                for (int i = 0; i < nSrcs; ++i)
                {
                    double src[/*2*/] = (srcInds != null ? srcs[srcInds[i]] : srcs[i]);
                    double radius = srcRadii[i % srcRadii.length];
                    double dist = dist(src, eventCenter);
                    double frac = (dist==0 ? .5 : radius / dist);
                    _srcs[i] = lerp(src, eventCenter, frac);
                }
                srcs = _srcs;
                srcInds = null;
            }

            if (dstRadii != null && dstRadii.length > 0)
            {
                for (int i = 0; i < nDsts; ++i)
                {
                    double dst[/*2*/] = (dstInds != null ? dsts[dstInds[i]] : dsts[i]);
                    double radius = dstRadii[i % dstRadii.length];
                    double dist = dist(dst, eventCenter);
                    double frac = (dist==0 ? .5 : radius / dist);
                    _dsts[i] = lerp(dst, eventCenter, frac);
                }
                dsts = _dsts;
                dstInds = null;
            }
        } // handle radii


        if (nSrcs == 0 && nDsts == 0)
        {
            // XXX represent this somehow?
            return new java.awt.Shape[0];
        }
        else if (nSrcs == 1 && nDsts == 0)
        {
            //
            // Just one src and that's all.
            // Make an arrow from the src to nothingness.
            //
            int nCurves = doArrowHeadOnSingleSrc ? 3 : 1;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double src[] = (srcInds != null ? srcs[srcInds[0]] : srcs[0]);
            if (distSqrd(src, graphCenterForSingle) == 0.)
                graphCenterForSingle = new double[] {graphCenterForSingle[0], graphCenterForSingle[1]+1}; // XXX assumes y increases downwards on screen
            double nothingness[] = lerp(graphCenterForSingle, src,
                                 1. + arrowLengthForSingle/dist(graphCenterForSingle[0],graphCenterForSingle[1], src[0],src[1]));
            int iCurve = 0;
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            src[0], src[1],
                                            src[0], src[1],
                                            nothingness[0], nothingness[1],
                                            nothingness[0], nothingness[1]);
            if (doArrowHeadOnSingleSrc)
            {
                curves[iCurve++] = arrowHeadSide(src[0], src[1],
                                                 nothingness[0], nothingness[1],
                                                 arrowHeadLength,
                                                 Math.PI*5/6);
                curves[iCurve++] = arrowHeadSide(src[0], src[1],
                                                 nothingness[0], nothingness[1],
                                                 arrowHeadLength,
                                                 -Math.PI*5/6);
            }
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+182 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else if (nDsts == 1 && nSrcs == 0)
        {
            //
            // Just one dst and that's all.
            // Make an arrow from nothingness to the dst.
            //
            int nCurves = 3;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double dst[] = (dstInds != null ? dsts[dstInds[0]] : dsts[0]);
            if (distSqrd(dst, graphCenterForSingle) == 0.)
                graphCenterForSingle = new double[] {graphCenterForSingle[0], graphCenterForSingle[1]+1}; // XXX assumes y increases downwards on screen
            double nothingness[] = lerp(graphCenterForSingle, dst,
                                 1. + arrowLengthForSingle/dist(graphCenterForSingle[0],graphCenterForSingle[1], dst[0],dst[1]));
            int iCurve = 0;
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            nothingness[0], nothingness[1],
                                            nothingness[0], nothingness[1],
                                            dst[0], dst[1],
                                            dst[0], dst[1]);
            curves[iCurve++] = arrowHeadSide(nothingness[0], nothingness[1],
                                             dst[0], dst[1],
                                             arrowHeadLength,
                                             Math.PI*5/6);
            curves[iCurve++] = arrowHeadSide(nothingness[0], nothingness[1],
                                             dst[0], dst[1],
                                             arrowHeadLength,
                                             -Math.PI*5/6);
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+212 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else if (nSrcs == 2 && nDsts == 0)
        {
            // Just two sources and that's it--- special-case so we get a bar.
            int nCurves = nSrcs + 1;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double src0[/*2*/] = (srcInds != null ? srcs[srcInds[0]] : srcs[0]);
            double src1[/*2*/] = (srcInds != null ? srcs[srcInds[1]] : srcs[1]);
            double A[] = lerp(src0, src1, 1./3.);
            double B[] = lerp(src0, src1, 2./3.);
            int iCurve = 0;
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                src0[0], src0[1],
                                                src0[0], src0[1],
                                                A[0], A[1],
                                                A[0], A[1]);
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                B[0], B[1],
                                                B[0], B[1],
                                                src1[0], src1[1],
                                                src1[0], src1[1]);
            // bar must go last
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                A[0], A[1],
                                                A[0], A[1],
                                                B[0], B[1],
                                                B[0], B[1]);
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+241 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else if (nDsts == 0)
        {
            //
            // Only srcs, and at least three of them
            //
            int nCurves = nSrcs;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double srcCenter[/*2*/] = average(srcs, srcInds);
            int iCurve = 0;
            for (int iSrc = 0; iSrc < nSrcs; ++iSrc)
            {
                double src[/*2*/] = (srcInds != null ? srcs[srcInds[iSrc]] : srcs[iSrc]);
                curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            src[0], src[1],
                                            src[0], src[1],
                                            srcCenter[0], srcCenter[1],
                                            srcCenter[0], srcCenter[1]);
            }
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+262 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else if (nDsts == 2 && nSrcs == 0)
        {
            // Just two dsts and that's it--- special-case so we get a bar.
            int nCurves = 1 + 3*nDsts;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double dst0[/*2*/] = (dstInds != null ? dsts[dstInds[0]] : dsts[0]);
            double dst1[/*2*/] = (dstInds != null ? dsts[dstInds[1]] : dsts[1]);
            double A[] = lerp(dst0, dst1, 1./3.);
            double B[] = lerp(dst0, dst1, 2./3.);
            int iCurve = 0;
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                dst0[0], dst0[1],
                                                dst0[0], dst0[1],
                                                A[0], A[1],
                                                A[0], A[1]);
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                B[0], B[1],
                                                B[0], B[1],
                                                dst1[0], dst1[1],
                                                dst1[0], dst1[1]);
            // bar must go last
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                                A[0], A[1],
                                                A[0], A[1],
                                                B[0], B[1],
                                                B[0], B[1]);
            // and then arrowheads even laster
            for (int iDst = 0; iDst < nDsts; ++iDst)
            {
                double dst[/*2*/] = (dstInds != null ? dsts[dstInds[iDst]] : dsts[iDst]);
                curves[iCurve++] = arrowHeadSide(A[0], A[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 Math.PI*5/6);
                curves[iCurve++] = arrowHeadSide(A[0], A[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 -Math.PI*5/6);
            }
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+304 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else if (nSrcs == 0)
        {
            //
            // Only dsts, and at least three of them
            //
            int nCurves = nDsts*3;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double dstCenter[/*2*/] = average(dsts, dstInds);
            int iCurve = 0;
            for (int iDst = 0; iDst < nDsts; ++iDst)
            {
                double dst[/*2*/] = (dstInds != null ? dsts[dstInds[iDst]] : dsts[iDst]);
                curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            dst[0], dst[1],
                                            dst[0], dst[1],
                                            dstCenter[0], dstCenter[1],
                                            dstCenter[0], dstCenter[1]);
            }
            // (all the arrow heads must come last)
            for (int iDst = 0; iDst < nDsts; ++iDst)
            {
                double dst[/*2*/] = (dstInds != null ? dsts[dstInds[iDst]] : dsts[iDst]);
                curves[iCurve++] = arrowHeadSide(dstCenter[0], dstCenter[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 Math.PI*5/6);
                curves[iCurve++] = arrowHeadSide(dstCenter[0], dstCenter[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 -Math.PI*5/6);
            }
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+338 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
        else // nSrcs > 0 && nDsts > 0
        {
            //
            // At least one src and at least one dst
            //
            int nCurves = nSrcs + 1 + nDsts*3;
            java.awt.Shape curves[] = new java.awt.Shape[nCurves];
            double srcCenter[/*2*/] = average(srcs, srcInds);
            double dstCenter[/*2*/] = average(dsts, dstInds);
            double centerCenter[/*2*/] = lerp(srcCenter, dstCenter, .5);
            double A[/*2*/] = lerp(srcCenter, centerCenter, .5);
            double B[/*2*/] = lerp(dstCenter, centerCenter, .5);

            //double controlA[/*2*/] = lerp(A, srcCenter, 1./3.);
            //double controlB[/*2*/] = lerp(B, dstCenter, 1./3.);
            // XXX experimenting... it's nicer (more curvy) when closer to srcCenter, but then sometimes the arrow heads and up cramped
            double controlA[/*2*/] = lerp(A, srcCenter, .5);
            double controlB[/*2*/] = lerp(B, dstCenter, .5);


            // hack sort of... if only one src or only one dst
            // but more of the other, put the split right at the global center.
            if (nSrcs == 1 && nDsts >= 2)
                B = lerp(srcCenter, dstCenter, (double)nDsts/(nSrcs+nDsts));
            if (nDsts == 1 && nSrcs >= 2)
                A = lerp(srcCenter, dstCenter, (double)nDsts/(nSrcs+nDsts));

            int iCurve = 0;
            for (int iSrc = 0; iSrc < nSrcs; ++iSrc)
            {
                double src[/*2*/] = (srcInds != null ? srcs[srcInds[iSrc]] : srcs[iSrc]);
                curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            src[0], src[1],
                                            src[0], src[1],
                                            controlA[0], controlA[1],
                                            A[0], A[1]);
            }
            for (int iDst = 0; iDst < nDsts; ++iDst)
            {
                double dst[/*2*/] = (dstInds != null ? dsts[dstInds[iDst]] : dsts[iDst]);
                curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                                            B[0], B[1],
                                            controlB[0], controlB[1],
                                            dst[0], dst[1],
                                            dst[0], dst[1]);
            }
            // (all the arrow heads must come last)
            for (int iDst = 0; iDst < nDsts; ++iDst)
            {
                double dst[/*2*/] = (dstInds != null ? dsts[dstInds[iDst]] : dsts[iDst]);
                // XXX note, controlB is kind of a hack... but it sorta works most of the time.
                curves[iCurve++] = arrowHeadSide(controlB[0], controlB[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 Math.PI*5/6);
                curves[iCurve++] = arrowHeadSide(controlB[0], controlB[1],
                                                 dst[0], dst[1],
                                                 arrowHeadLength,
                                                 -Math.PI*5/6);
            }

            // Straight line between A and B...
            curves[iCurve++] = new java.awt.geom.CubicCurve2D.Double(
                A[0], A[1],
                A[0], A[1],
                B[0], B[1],
                B[0], B[1]);
            do { if (!(iCurve == nCurves)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+408 +"): " + "iCurve == nCurves" + ""); } while (false);
            return curves;
        }
    } // getCurves


        private static java.awt.Shape arrowHeadSide(double x0, double y0,
                                                    double x1, double y1,
                                                    double arrowHeadLength,
                                                    double angDelta)
        {
            double ang = Math.atan2(y1-y0,x1-x0);
            double newAng = ang + angDelta;
            double x = x1 + arrowHeadLength * Math.cos(newAng);
            double y = y1 + arrowHeadLength * Math.sin(newAng);
            return new java.awt.geom.Line2D.Double(x1,y1, x,y);
        } // arrowHeadSide

        private static double[] average(double points[][/*dim*/], int inds[])
        {
            int n = (inds != null ? inds.length : points.length);
            if (n == 0)
                return null;
            int dim = points[0].length;
            double result[] = new double[dim]; // initially all zeros
            for (int i = 0; i < n; ++i)
            {
                double point[] = points[inds != null ? inds[i] : i];
                do { if (!(point.length == dim)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+436 +"): " + "point.length == dim" + ""); } while (false);
                for (int j = 0; j < dim; ++j)
                    result[j] += point[j];
            }
            for (int j = 0; j < dim; ++j)
                result[j] *= 1./n;
            return result;
        } // average


    //========================================================================
    // The rest of this is for main(), which is a little demonstration program.
    //

    // A fuzzy point, i.e. a closed unit disk with center x,y and radius r.
    // We store r*r instead of r, since r itself is hardly
    // ever needed so this saves computing a lot of square roots.
    private static class FuzzyPoint {
        public double x, y, rSqrd;
    }
    /**
     * Get a bounding ball of a CubicCurve2D.
     * Not necessarily very tight.
     */
    public static void getCurveBoundingBall(java.awt.geom.CubicCurve2D curve,
                                            FuzzyPoint result)
    {
        // A CubicCurve2D is always contained inside
        // the bounding box of its 4 definining points...
        double x0 = curve.getX1();
        double y0 = curve.getY1();
        double x1 = curve.getCtrlX1();
        double y1 = curve.getCtrlY1();
        double x2 = curve.getCtrlX2();
        double y2 = curve.getCtrlY2();
        double x3 = curve.getX2();
        double y3 = curve.getY2();
        double x = (x0+x1+x2+x3)*.25;
        double y = (y0+y1+y2+y3)*.25;

        double distSqrd0 = distSqrd(x,y, x0,y0);
        double distSqrd1 = distSqrd(x,y, x1,y1);
        double distSqrd2 = distSqrd(x,y, x2,y2);
        double distSqrd3 = distSqrd(x,y, x3,y3);
        double rSqrd = max4(distSqrd0,distSqrd1,distSqrd2,distSqrd3);

        result.x = x;
        result.y = y;
        result.rSqrd = rSqrd;
    } // getCurveBoundingBall



    /**
     * Gets the closest point on a CubicCurve2D,
     * within a specified tolerance.
     */
    public static boolean getClosestPointOnCubicCurve(
                               double x, double y,
                               java.awt.geom.CubicCurve2D curve,
                               double tooFar,
                               double maxFuzzyRadius,
                               FuzzyPoint closestPoint, // output
                               int verbose)
    {
        // recursive function assumes closestPoint
        // has been filled in with a bounding ball...
        // (this is an optimization because
        // in all recursive calls, the caller calculates
        // the bounding ball first anyway).
        getCurveBoundingBall(curve, closestPoint);
        int nSubdivsCalled[] = {0};
        //System.out.println("========");
        boolean result = _getClosestPointOnCubicCurve(x,y, curve, tooFar,
                                                      maxFuzzyRadius,
                                                      closestPoint,
                                                      nSubdivsCalled,

                                                      null
                                                      //"." use this instead to debug

                                                      );
        if (verbose >= 1) System.out.println("    num subdivs called = "+nSubdivsCalled[0]+"");
        return result;
    } // getClosestPointOnCubicCurve

    // The recursive work function used by getClosestPointOnCubicCurve().
    // closestPoint must be initialized to a bounding ball
    // of the curve (caller does this using getCurveBoundingBall).
    private static boolean _getClosestPointOnCubicCurve(
                               double x, double y,
                               java.awt.geom.CubicCurve2D curve,
                               double tooFar,
                               double maxFuzzyRadius,
                               FuzzyPoint closestPoint, // input/output
                               int nSubdivsCalled[/*1*/], // output statistics
                               String addressForDebugging)
    {
        if (addressForDebugging != null)
            System.out.println("Looking at: "+addressForDebugging);
        // Caller has already filled in closestPoint with
        // a bounding ball of the curve...
        double distSqrd = distSqrd(x,y, closestPoint.x,closestPoint.y);
        if (closestPoint.rSqrd <= maxFuzzyRadius*maxFuzzyRadius)
        {
            // fuzzy point is small enough, no need to subdivide further,
            // and closestPoint is already filled in.
            //System.out.println("    SMALL ENOUGH");
            return distSqrd < tooFar*tooFar;
        }
        double minPossibleDist = Math.sqrt(distSqrd)
                               - Math.sqrt(closestPoint.rSqrd);
        if (minPossibleDist < 0)
            minPossibleDist = 0;
        //PRINT(minPossibleDist);
        if (minPossibleDist*minPossibleDist >= tooFar*tooFar)
        {
            //System.out.println("    TOO FAR");
            return false;
        }
        //System.out.println("    SUBDIVIDING");

        //
        // Subdivide
        //
        java.awt.geom.CubicCurve2D left = new java.awt.geom.CubicCurve2D.Double();
        java.awt.geom.CubicCurve2D right = new java.awt.geom.CubicCurve2D.Double();
        curve.subdivide(left, right);
        ++nSubdivsCalled[0];

        FuzzyPoint leftPoint = new FuzzyPoint();
        FuzzyPoint rightPoint = new FuzzyPoint();

        getCurveBoundingBall(left, leftPoint);
        getCurveBoundingBall(right, rightPoint);

        double leftDistSqrd = distSqrd(x,y,leftPoint.x,leftPoint.y);
        double rightDistSqrd = distSqrd(x,y,rightPoint.x,rightPoint.y);

        //System.out.println("    dists = "+Math.sqrt(leftDistSqrd)+","+Math.sqrt(rightDistSqrd));
        boolean swapped = false; // for debugging
        if (rightDistSqrd < leftDistSqrd)
        //if (rightDistSqrd > leftDistSqrd) // XXX WRONG! testing, should be grossly inefficient
        {
            // Swap left and right so that left is closer
            { java.awt.geom.CubicCurve2D temp = left; left = right; right = temp; }
            { FuzzyPoint temp = leftPoint; leftPoint = rightPoint; rightPoint = temp; }
            swapped = true;
        }

        boolean leftResult = _getClosestPointOnCubicCurve(x, y, left,
                                                          tooFar,
                                                          maxFuzzyRadius,
                                                          leftPoint,
                                                          nSubdivsCalled,
                                                          addressForDebugging != null ? "    "+addressForDebugging+(swapped?"l":"L"):null);
        if (leftResult)
        {
            double newTooFarSqrd = distSqrd(x, y, leftPoint.x, leftPoint.y);
            do { if (!(newTooFarSqrd < tooFar*tooFar)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+595 +"): " + "newTooFarSqrd < tooFar*tooFar" + ""); } while (false); // it shouldn't have succeeded otherwise
            tooFar = Math.sqrt(newTooFarSqrd);
        }
        boolean rightResult = _getClosestPointOnCubicCurve(x, y, right,
                                                           tooFar,
                                                           maxFuzzyRadius,
                                                           rightPoint,
                                                           nSubdivsCalled,
                                                           addressForDebugging != null ? "    "+addressForDebugging+(swapped?"r":"R"):null);
        if (rightResult)
        {
            closestPoint.x = rightPoint.x;
            closestPoint.y = rightPoint.y;
            closestPoint.rSqrd = rightPoint.rSqrd;
            //System.out.println("    RETURNING RIGHTRESULT");
            return true;
        }
        else if (leftResult)
        {
            closestPoint.x = leftPoint.x;
            closestPoint.y = leftPoint.y;
            closestPoint.rSqrd = leftPoint.rSqrd;
            //System.out.println("    RETURNING LEFTRESULT");
            return true;
        }
        else
        {
            return false;
        }
    } // _getClosestPointOnCubicCurve


    /**
     * Gets the closest point on an array of curves.
     * ignores any shapes that are not of type CubicCurve2D.
     */
    public static int[/*2*/] getClosestPointOnCubicCurves(
                                   double x, double y,
                                   java.awt.Shape curves[][],
                                   double tooFar,
                                   double maxFuzzyRadius,
                                   FuzzyPoint closestPoint) // output
    {
        int bestI = -1;
        int bestJ = -1;
        double bestX = Double.NaN;
        double bestY = Double.NaN;
        double bestRSqrd = Double.NaN;

        for (int i = 0; i < curves.length; ++i)
        for (int j = 0; j < curves[i].length; ++j)
        {
            if (curves[i][j] instanceof java.awt.geom.CubicCurve2D)
            {
                java.awt.geom.CubicCurve2D curve = (java.awt.geom.CubicCurve2D)curves[i][j];
                if (getClosestPointOnCubicCurve(x, y, curve,
                                                tooFar,
                                                maxFuzzyRadius,
                                                closestPoint,
                                                0)) // verbose
                {
                    bestI = i;
                    bestJ = j;
                    bestX = closestPoint.x;
                    bestY = closestPoint.y;
                    bestRSqrd = closestPoint.rSqrd;
                    double newTooFarSqrd = distSqrd(x, y, bestX, bestY);
                    do { if (!(newTooFarSqrd < tooFar*tooFar)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+662 +"): " + "newTooFarSqrd < tooFar*tooFar" + ""); } while (false); // it shouldn't have succeeded otherwise
                    tooFar = Math.sqrt(newTooFarSqrd);
                }
            }
        }

        closestPoint.x = bestX;
        closestPoint.y = bestY;
        closestPoint.rSqrd = bestRSqrd;

        return new int[] {bestI, bestJ};
    } // getClosestPointOnCubicCurves




    //-----------------------------------------------------------------------
    // Little math utils...
    //
        private static double max4(double a, double b, double c, double d)
        {
            double max = a;
            if (b > max) max = b;
            if (c > max) max = c;
            if (d > max) max = d;
            return max;
        } // max4
        private static double min4(double a, double b, double c, double d)
        {
            double min = a;
            if (b < min) min = b;
            if (c < min) min = c;
            if (d < min) min = d;
            return min;
        } // min4
        private static int min4i(double a, double b, double c, double d)
        {
            int i = 0;
            double min = a;
            if (b < min) {i = 1; min = b;}
            if (c < min) {i = 2; min = c;}
            if (d < min) {i = 3; min = d;}
            return i;
        } // min4i
        private static double distSqrd(double x0, double y0, double x1, double y1)
        {
            return (x1-x0)*(x1-x0) + (y1-y0)*(y1-y0);
        } // distSqrd
        private static double dist(double x0, double y0, double x1, double y1)
        {
            return Math.sqrt(distSqrd(x0,y0, x1,y1));
        }
        private static double distSqrd(double p0[], double p1[])
        {
            return distSqrd(p0[0],p0[1],p1[0],p1[1]);
        }
        private static double dist(double p0[], double p1[])
        {
            return dist(p0[0],p0[1],p1[0],p1[1]);
        }
        private static double lerp(double a, double b, double t)
        {
            return (1.-t)*a + t*b;
        }
        private static double[] lerp(double a[], double b[], double t)
        {
            int n = a.length;
            do { if (!(n == b.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+729 +"): " + "n == b.length" + ""); } while (false);
            double result[] = new double[n];
            for (int i = 0; i < n; ++i)
                result[i] = (1.-t)*a[i] + t*b[i];
            return result;
        } // lerp
    //
    // End of little math utils.
    //-----------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // Little drawing utils...
    //
        /**
         * Draws a string with arbitrary justification.
         * <p>
         * xJustify : -1,0,1 -> Left,Center,Right
         * <br>
         * yJustify : -1,0,1 -> Top,Center,Bottom  (if y increases from top to bottom)
         * <br>
         * and anything in between.
         * <br>
         * XXX fix param descriptions for javadoc
         */
        public static void drawString(java.awt.Graphics2D g2d,
                                      String s, double x, double y,
                                      double xJustify, // -1,0,1 -> L,C,R
                                      double yJustify) // -1,0,1 -> T,C,B
        {
            java.awt.FontMetrics fm = g2d.getFontMetrics();

            /*
            x:
                -1 -> 0
                0 -> -.5
                1 -> -1

            y:
                -1 -> 1
                0 -> .5
                1 -> 0
            */

            g2d.drawString(s, (float)(x - fm.stringWidth(s)*(xJustify+1.)*.5),
                              (float)(y + fm.getAscent()*(1.-yJustify)*.5));
                            // XXX centered height seems to come out a bit low-- maybe fiddle with leading?
        }
        /** Calls drawString with xJustify=0 and yJustify=0. */
        public static void drawStringCentered(java.awt.Graphics2D g2d, String s, double x, double y)
        {
            drawString(g2d, s, x, y, 0, 0);
        }
        /** calls drawString with xJustify=1 and yJustify=0. */
        public static void drawStringRightJustified(java.awt.Graphics2D g2d, String s, double x, double y)
        {
            drawString(g2d, s, x, y, 1, 0);
        }
        /** Calls drawString with xJustify=-1 and yJustify=0. */
        public static void drawStringLeftJustified(java.awt.Graphics2D g2d, String s, double x, double y)
        {
            drawString(g2d, s, x, y, -1, 0);
        }
        /**
         * Calls drawString with appropriate justification params
         * so that the string is placed margin pixels away
         * from the point, in the direction specified by dirAngle (in radians).
         */
        static void drawStringNextToPoint(java.awt.Graphics2D g2d, String labelText, double x, double y, double dirAngle, double margin)
        {
            double cos = Math.cos(dirAngle);
            double sin = Math.sin(dirAngle);

            if (false)
            {
                g2d.draw(new java.awt.geom.Line2D.Double(x, y, x+cos*margin, y+sin*margin));
            }

            double xJustify = -cos;
            double yJustify = -sin;
            double max = Math.max(Math.abs(xJustify),
                                  Math.abs(yJustify));
            xJustify /= max;
            yJustify /= max;
            // Now xJustify,yJustify lie on the unit square
            drawString(g2d, labelText,
                       x + cos*margin,
                       y + sin*margin,
                       xJustify, yJustify);
        } // drawStringNextToPoint

        /**
         * Draw a little square.
         */
        public static void drawSquare(java.awt.Graphics2D g2d,
                                      double x, double y, double r)
        {
            java.awt.geom.Rectangle2D.Double square = new java.awt.geom.Rectangle2D.Double(x-r, y-r, 2.*r, 2.*r);
            g2d.draw(square);
        } // drawSquare
        /**
         * Draw a little circle.
         */
        public static void drawCircle(java.awt.Graphics2D g2d,
                                      double x, double y, double r)
        {
            java.awt.geom.Ellipse2D.Double circle = new java.awt.geom.Ellipse2D.Double(x-r, y-r, 2.*r, 2.*r);
            g2d.draw(circle);
        } // drawCircle

        /**
         * Draw a curve showing binary subdivision points, for debugging.
         * If nLevels is 0, just draw normally.
         */
        public static void drawCurveSubdivided(java.awt.Graphics2D g2d, java.awt.geom.CubicCurve2D curve, int nLevels)
        {
            if (nLevels > 0)
            {
                java.awt.geom.CubicCurve2D left = new java.awt.geom.CubicCurve2D.Double();
                java.awt.geom.CubicCurve2D right = new java.awt.geom.CubicCurve2D.Double();
                curve.subdivide(left, right);
                drawCurveSubdivided(g2d, left, nLevels-1);
                drawCurveSubdivided(g2d, right, nLevels-1);
                // both should end up in same place...
                drawSquare(g2d, left.getX2(),left.getY2(), 2);
                drawSquare(g2d, right.getX1(),right.getY1(), 4);
            }
            else
            {
                // Sanity check: g2d.draw will infinite loop on NaN,
                // so make sure we throw an exception instead.
                do { if (!(!Double.isNaN(curve.getX1()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+859 +"): " + "!Double.isNaN(curve.getX1())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getY1()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+860 +"): " + "!Double.isNaN(curve.getY1())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getCtrlX1()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+861 +"): " + "!Double.isNaN(curve.getCtrlX1())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getCtrlY1()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+862 +"): " + "!Double.isNaN(curve.getCtrlY1())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getCtrlX2()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+863 +"): " + "!Double.isNaN(curve.getCtrlX2())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getCtrlY2()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+864 +"): " + "!Double.isNaN(curve.getCtrlY2())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getX2()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+865 +"): " + "!Double.isNaN(curve.getX2())" + ""); } while (false);
                do { if (!(!Double.isNaN(curve.getY2()))) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+866 +"): " + "!Double.isNaN(curve.getY2())" + ""); } while (false);
                g2d.draw(curve);
            }
        } // drawCurveSubdivided
    //
    // End of little drawing utils.
    //-----------------------------------------------------------------------


    private static class MyDemoPanel
        extends javax.swing.JPanel
    {
        int paintCount = 0;

        public class MyCanvas
            extends javax.swing.JPanel
        {
            // PAINT
            public void paintComponent(java.awt.Graphics g)
            {
                if (eventVerbose >= 2) System.out.println("    in paintComponent");

                if (false)
                {
                    // pause the second time, so I can ponder the resize
                    // that happens after the first paint for some reason
                    if (paintCount++ == 1)
                    {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) { }
                    }
                }

                // we delay creating assets till now, when we know window size...
                if (assets == null)
                {
                    int nAssets = 18;
                    assets = makeCircle(nAssets, canvas.getWidth()/2, canvas.getHeight()/2, Math.min(canvas.getWidth(), canvas.getHeight()) / 3);
                    assetVelocities = new double[assets.length][2]; // zeros
                    futureAssets = new double[assets.length][2]; // arbitrary
                    nFramesUntilFutures = new int[assets.length]; // zeros
                    connections = new int[][/*2*/][] {
                        {{5},{4}},
                        {{6},{3,2}},
                        {{7,8},{1}},
                        {{9,10},{17,0}},
                        {{11},{}},
                        {{},{16}},
                        {{12,13},{}},
                        {{},{14,15}},
                    };
                }

                {
                    boolean needsRepaint = false;
                    double p_and_v[] = new double[2]; // scratch
                    for (int i = 0; i < assets.length; ++i)
                    {
                        if (nFramesUntilFutures[i] > 0)
                        {
                            if (false)
                            {
                                // XXX shouldn't replace
                                assets[i] = lerp(assets[i], futureAssets[i], 1./nFramesUntilFutures[i]); // linear
                            }
                            else
                            {
                                for (int iDim = 0; iDim < 2; ++iDim)
                                {
                                    p_and_v[0] = assets[i][iDim] - futureAssets[i][iDim];
                                    p_and_v[1] = assetVelocities[i][iDim];
                                    update_p_and_v(p_and_v, -nFramesUntilFutures[i]);
                                    assets[i][iDim] = futureAssets[i][iDim] + p_and_v[0];
                                    assetVelocities[i][iDim] = p_and_v[1];
                                }
                            }

                            if (--nFramesUntilFutures[i] > 0)
                                needsRepaint = true;
                        }
                    }
                    if (needsRepaint)
                    {
                        connectionCurves = null; // dirty
                        repaint(); // so we'll come back around again
                    }
                }

                clear(g);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D)g;


                if (mouseState == STATE_MOVING)
                {
                    if (false)
                    {
                        g2d.setColor(java.awt.Color.blue);
                        drawSquare(g2d, curX, curY, selectRadiusPixels);
                    }
                }

                g2d.setColor(java.awt.Color.black);

                if (mode == MODE_ONE_CURVE)
                {
                    for (int i = 0; i < controlPoints.length; ++i)
                    {
                        double x = controlPoints[i][0];
                        double y = controlPoints[i][1];
                        double r = 3;
                        drawSquare(g2d, x, y, r);
                    }
                    java.awt.geom.CubicCurve2D curve = new java.awt.geom.CubicCurve2D.Double(
                            controlPoints[0][0],
                            controlPoints[0][1],
                            controlPoints[1][0],
                            controlPoints[1][1],
                            controlPoints[2][0],
                            controlPoints[2][1],
                            controlPoints[3][0],
                            controlPoints[3][1]);
                    java.awt.Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(new java.awt.BasicStroke(2)); // line thickness = 2
                    //g2d.draw(curve);
                    drawCurveSubdivided(g2d, curve, nDebugSubdivisionLevels);
                    g2d.setStroke(oldStroke);

                    FuzzyPoint closestPoint = new FuzzyPoint();
                    boolean result = getClosestPointOnCubicCurve(
                                         curX, curY,
                                         curve,

                                         Double.POSITIVE_INFINITY, // tooFar
                                         //selectRadiusPixels // tooFar

                                         maxFuzzyRadiusPixels,
                                         closestPoint,
                                         verbose);
                    if (result)
                    {
                        double x = closestPoint.x;
                        double y = closestPoint.y;
                        double r = Math.sqrt(closestPoint.rSqrd);
                        g2d.setColor(java.awt.Color.red);
                        drawSquare(g2d, x, y, 1);
                        drawSquare(g2d, x, y, r); // dot in the middle
                    }
                }
                else if (mode == MODE_ASSETS)
                {

                    if (connectionCurves == null)
                        recalculateCurves();

                    if (false)
                        for (int i = 0; i < assets.length; ++i)
                            drawSquare(g2d, assets[i][0],assets[i][1], 2);

                    boolean doAntiAlias = true; // XXX need an option to toggle
                    if (doAntiAlias)
                    {
                        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                             java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                        // XXX not sure if the following does anything?
                        if (false)
                            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                                 java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                    }

                    if (true)
                    {
                        for (int i = 0; i < assets.length; ++i)
                            drawCircle(g2d, assets[i][0],assets[i][1], Math.max(.9*srcRadii[0], .5));
                    }

                    java.awt.Stroke oldStroke = g2d.getStroke();

                    boolean order = true;
                    java.awt.BasicStroke blackStroke = new java.awt.BasicStroke((float)lineThickness);
                    java.awt.BasicStroke whiteStroke = new java.awt.BasicStroke((float)shadowThickness);

                    g2d.setStroke(blackStroke);

                    for (int i = 0; i < connectionCurves.length; ++i)
                    {
                        for (int multiplicity = 0; multiplicity < (shadowThickness > lineThickness ? 2 : 1); ++multiplicity)
                        {
                            if (multiplicity == 0 && shadowThickness > lineThickness)
                            {
                                g2d.setStroke(whiteStroke);
                                g2d.setColor(getBackground());
                            }
                            for (int j = 0; j < connectionCurves[i].length; ++j)
                            {
                                //g2d.draw(connectionCurves[i][j]);
                                if (connectionCurves[i][j] instanceof java.awt.geom.CubicCurve2D)
                                    drawCurveSubdivided(g2d,(java.awt.geom.CubicCurve2D)connectionCurves[i][j],nDebugSubdivisionLevels);
                                else
                                    g2d.draw(connectionCurves[i][j]);
                            }
                            if (multiplicity == 0 && shadowThickness > lineThickness)
                            {
                                g2d.setStroke(blackStroke);
                                g2d.setColor(java.awt.Color.black);
                            }
                        }
                    }
                    g2d.setStroke(oldStroke);
                    if (doAntiAlias)
                    {
                        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                             java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
                    }

                    if (mouseState != STATE_NOTHING)
                    {
                        // Just draw a nonobtrusive dot on the closest thing
                        FuzzyPoint closestPoint = new FuzzyPoint();
                        int closestIndices[/*2*/] = getClosestPointOnCubicCurves(
                                                        curX, curY,
                                                        connectionCurves,

                                                        //selectRadiusPixels
                                                        Double.POSITIVE_INFINITY,

                                                        maxFuzzyRadiusPixels,
                                                        closestPoint);
                        if (closestIndices[0] != -1)
                        {
                            double x = closestPoint.x;
                            double y = closestPoint.y;
                            double r = Math.sqrt(closestPoint.rSqrd);
                            g2d.setColor(java.awt.Color.black);
                            drawSquare(g2d, x, y, r); // dot in the middle
                            drawSquare(g2d, x, y, 1); // dot in the middle
                        }
                    }

                    if (showLabels)
                    {
                        g2d.setColor(java.awt.Color.blue);
                        double center[/*2*/] = average(assets, null);
                        for (int i = 0; i < assets.length; ++i)
                        {
                            String labelText = "Asset "+i;
                            double x = assets[i][0];
                            double y = assets[i][1];
                            double dirAngle = Math.atan2(y-center[1], x-center[0]);
                            drawStringNextToPoint(g2d, labelText, x, y, dirAngle, srcRadii[0]+5);
                        }
                    }

                    if (mouseState != STATE_NOTHING) // XXX probably not right
                    {
                        boolean isCtrlDragging = (mouseState==STATE_CTRL_DRAGGING_NOTHING||mouseState==STATE_CTRL_DRAGGING_EVENT||mouseState==STATE_CTRL_DRAGGING_ASSET);
                        //double tooFar = Double.POSITIVE_INFINITY;
                        double tooFar = isCtrlDragging ? selectRadiusPixels : deleteRadiusPixels;
                        FuzzyPoint closestPoint = new FuzzyPoint();
                        int indicesOfClosestThing[/*3*/] = getIndicesOfClosestThing(
                                                        curX, curY,
                                                        assets,
                                                        connectionCurves,
                                                        tooFar,
                                                        maxFuzzyRadiusPixels,
                                                        closestPoint,
                                                        mouseState==STATE_DRAGGING_ASSET);
                        //printArray("indicesOfClosestThing", indicesOfClosestThing);
                        if (indicesOfClosestThing[0] != -1) // an asset
                        {
                            double x = closestPoint.x;
                            double y = closestPoint.y;
                            double r = Math.sqrt(closestPoint.rSqrd);
                            g2d.setColor(java.awt.Color.red);
                            drawSquare(g2d, x, y, 5);
                            if (showLabels)
                            {
                                int ind = indicesOfClosestThing[0];
                                String labelText = "Asset " + ind;

                                double center[/*2*/] = average(assets, null);
                                double dirAngle = Math.atan2(y-center[1], x-center[0]);
                                drawStringNextToPoint(g2d, labelText, x, y, dirAngle, srcRadii[0]+5);

                            }
                        }
                        if (indicesOfClosestThing[1] != -1) // a curve
                        {
                            double x = closestPoint.x;
                            double y = closestPoint.y;
                            double r = Math.sqrt(closestPoint.rSqrd);
                            g2d.setColor(java.awt.Color.red);
                            drawSquare(g2d, x, y, 1);

                            // Re-draw the asset in red (XXX this doesn't look great when antialiasing... should suppress drawing it in black to begin with)
                            {
                                drawCurveSubdivided(g2d,(java.awt.geom.CubicCurve2D)connectionCurves[indicesOfClosestThing[1]][indicesOfClosestThing[2]],nDebugSubdivisionLevels);
                            }

                            if (showLabels)
                            {
                                int ind = indicesOfClosestThing[1];
                                String labelText = "Event " + arrayToString(connections[ind][0])+ "->" + arrayToString(connections[ind][1]) + "";
                                //
                                // Try to analyze which leg we have
                                //
                                {
                                    int event[][] = connections[ind];
                                    int legInd = indicesOfClosestThing[2];
                                    int nSrcs = event[0].length;
                                    int nDsts = event[1].length;
                                    if (legInd < nSrcs)
                                    {
                                        labelText = "Asset "+event[0][legInd]+" as src of "+labelText;
                                    }
                                    else if (legInd < nSrcs+nDsts)
                                    {
                                        labelText = "Asset "+event[1][legInd-nSrcs]+" as dst of "+labelText;
                                    }
                                }
                                double center[/*2*/] = average(assets, null);
                                double dirAngle = Math.atan2(y-center[1], x-center[0]);
                                // XXX Bleah, putting it on the opposite side of the label assets is cool because less collisions, but on the other hand it's cool when one merges smoothly into the other
                                //dirAngle += Math.PI; // put it on opposite side, towards the center
                                drawStringNextToPoint(g2d, labelText, x, y, dirAngle, 10);
                            }
                        }

                    } // mouseState != STATE_NOTHING

                    if (mouseState == STATE_CTRL_DRAGGING_ASSET)
                    {
                        double x = assets[indexOfThingBeingDragged][0];
                        double y = assets[indexOfThingBeingDragged][1];
                        g2d.setColor(java.awt.Color.green);
                        drawSquare(g2d, x, y, 5);
                        g2d.setColor(java.awt.Color.lightGray); // XXX dotted?
                        g2d.draw(new java.awt.geom.Line2D.Double(x, y, curX, curY));
                    }
                    else if (mouseState == STATE_CTRL_DRAGGING_EVENT)
                    {
                        g2d.setColor(java.awt.Color.green);
                        java.awt.geom.CubicCurve2D curve = (java.awt.geom.CubicCurve2D)connectionCurves[indexOfThingBeingDragged][subIndexOfThingBeingDragged];
                        drawCurveSubdivided(g2d,curve,nDebugSubdivisionLevels);
                        // Find some point on the curve. XXX maybe should have saved the closest point when we originally selected it?
                        double x, y;
                        {
                            java.awt.geom.CubicCurve2D left = new java.awt.geom.CubicCurve2D.Double();
                            java.awt.geom.CubicCurve2D right = new java.awt.geom.CubicCurve2D.Double();
                            curve.subdivide(left, right);
                            x = left.getX2();
                            y = left.getY2();
                        }
                        g2d.setColor(java.awt.Color.lightGray); // XXX dotted?
                        g2d.draw(new java.awt.geom.Line2D.Double(x, y, curX, curY));
                    }

                } // mode == MODE_ASSETS

                if (eventVerbose >= 2) System.out.println("    out paintComponent");
            } // paintComponent

            public MyCanvas()
            {
                //
                // Add listeners for everything
                // in the canvas...
                //
                this.addComponentListener(new java.awt.event.ComponentListener() {
                    public void componentResized(java.awt.event.ComponentEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in canvas componentResized: "+e);
                        int oldWidth = curWidth;
                        int oldHeight = curHeight;
                        int newWidth = e.getComponent().getWidth();
                        int newHeight = e.getComponent().getHeight();
                        int oldSize = Math.min(oldWidth, oldHeight);
                        int newSize = Math.min(newWidth, newHeight);

                        if (oldWidth != -1)
                        {
                            if (eventVerbose >= 1) System.out.println("Resizing "+oldWidth+"x"+oldHeight+" -> "+newWidth+"x"+newHeight+"");
                            // XXX this is silly, shouldn't need to recalculate connectionCurves, should just tweak the window transform matrix
                            transform(assets,
                                      oldWidth/2., oldHeight/2.,
                                      (double)newSize/oldSize,
                                      newWidth/2., newHeight/2.);
                            transform(assetVelocities, 0., 0.,
                                                       (double)newSize/oldSize,
                                                       0., 0.);
                            transform(futureAssets, oldWidth/2., oldHeight/2.,
                                                    (double)newSize/oldSize,
                                                    newWidth/2., oldWidth/2.);
                            connectionCurves = null; // dirty;
                            repaint();
                        }

                        curWidth = newWidth;
                        curHeight = newHeight;

                        if (eventVerbose >= 1) System.out.println("    out canvas componentResized");
                    }
                    public void componentMoved(java.awt.event.ComponentEvent e)
                    {
                        if (eventVerbose >= 0) System.out.println("    in componentMoved");
                        if (eventVerbose >= 0) System.out.println("    out componentMoved");
                    }
                    public void componentShown(java.awt.event.ComponentEvent e)
                    {
                        if (eventVerbose >= 2) System.out.println("    in componentShown");
                        if (eventVerbose >= 2) System.out.println("    out componentShown");
                    }
                    public void componentHidden(java.awt.event.ComponentEvent e)
                    {
                        if (eventVerbose >= 2) System.out.println("    in componentHidden");
                        if (eventVerbose >= 2) System.out.println("    out componentHidden");
                    }
                });
                this.addMouseMotionListener(new java.awt.event.MouseMotionListener() {
                    public void mouseMoved(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 2) System.out.println("    in mouseMoved to "+e.getX()+","+e.getY()+"");
                        curX = e.getX();
                        curY = e.getY();
                        if (mouseState == STATE_NOTHING)
                            mouseState = STATE_MOVING;
                        repaint();
                        if (eventVerbose >= 2) System.out.println("    out mouseMoved");
                    }
                    public void mouseDragged(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mouseDragged to "+e.getX()+","+e.getY()+"");

                        int prevX = curX;
                        int prevY = curY;
                        curX = e.getX();
                        curY = e.getY();
                        double dx = curX-prevX;
                        double dy = curY-prevY;
                        if (mode == MODE_ONE_CURVE)
                        {
                            if (indexOfThingBeingDragged != -1)
                            {
                                controlPoints[indexOfThingBeingDragged][0] += dx;
                                controlPoints[indexOfThingBeingDragged][1] += dy;
                            }
                            else
                            {
                                pan(controlPoints, dx, dy);
                            }
                        }
                        else if (mode == MODE_ASSETS)
                        {
                            if (mouseState == STATE_DRAGGING_ASSET)
                            {
                                if (indexOfThingBeingDragged != -1)
                                {
                                    assets[indexOfThingBeingDragged][0] += dx;
                                    assets[indexOfThingBeingDragged][1] += dy;
                                    connectionCurves = null; // dirty
                                }
                            }
                            else if (mouseState == STATE_DRAGGING_EVENT)
                            {
                                // dragging an event doesn't really do anything.
                            }
                            else if (mouseState == STATE_DRAGGING_NOTHING)
                            {
                                // pan the whole scene.
                                // XXX this is silly, shouldn't need to recalculate connectionCurves, should just tweak the window transform matrix
                                pan(assets, dx, dy);
                                pan(futureAssets, dx, dy);
                                connectionCurves = null; // dirty;
                            }
                        }

                        repaint();
                        if (eventVerbose >= 1) System.out.println("    out mouseDragged");
                    }
                });
                this.addMouseListener(new java.awt.event.MouseListener() {
                    public void mouseClicked(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mouseClicked at "+e.getX()+","+e.getY()+"");
                        if (eventVerbose >= 1) System.out.println("    out mouseClicked");
                    }
                    public void mousePressed(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mousePressed at "+e.getX()+","+e.getY()+"");

                        // Set controlPointBeingDragged to the closest
                        // control point
                        if (mode == MODE_ONE_CURVE)
                        {
                            indexOfThingBeingDragged = findClosest(curX, curY, controlPoints, selectRadiusPixels);
                            //System.out.println("indexOfThingBeingDragged = "+indexOfThingBeingDragged);
                        }
                        else if (mode == MODE_ASSETS)
                        {
                            double tooFar = selectRadiusPixels;
                            FuzzyPoint closestPoint = new FuzzyPoint();
                            int indicesOfClosestThing[/*3*/] = getIndicesOfClosestThing(
                                                            curX, curY,
                                                            assets,
                                                            connectionCurves,
                                                            tooFar,
                                                            maxFuzzyRadiusPixels,
                                                            closestPoint,
                                                            true);
                            //printArray("indicesOfClosestThing", indicesOfClosestThing);
                            //PRINT(e.isControlDown());
                            if (indicesOfClosestThing[0] != -1)
                            {
                                mouseState = (e.isControlDown() ? STATE_CTRL_DRAGGING_ASSET : STATE_DRAGGING_ASSET);
                                nFramesUntilFutures[indicesOfClosestThing[0]] = 0; // XXX set future position to current too?
                                indexOfThingBeingDragged = indicesOfClosestThing[0];
                                subIndexOfThingBeingDragged = -1;
                            }
                            else if (indicesOfClosestThing[1] != -1)
                            {
                                mouseState = (e.isControlDown() ? STATE_CTRL_DRAGGING_EVENT : STATE_DRAGGING_EVENT);
                                indexOfThingBeingDragged = indicesOfClosestThing[1];
                                subIndexOfThingBeingDragged = indicesOfClosestThing[2];
                            }
                            else
                            {
                                mouseState = (e.isControlDown() ? STATE_CTRL_DRAGGING_NOTHING : STATE_DRAGGING_NOTHING); // panning
                                indexOfThingBeingDragged = -1;
                                subIndexOfThingBeingDragged = -1;
                                for (int i = 0; i < assets.length; ++i)
                                    nFramesUntilFutures[i] = 0; // XXX set future position to current too?
                            }
                            //System.out.println("indexOfThingBeingDragged = "+indexOfThingBeingDragged+","+subIndexOfThingBeingDragged+"");
                        }
                        repaint();

                        if (eventVerbose >= 1) System.out.println("    out mousePressed");
                    }
                    public void mouseReleased(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mouseReleased at "+e.getX()+","+e.getY()+"");

                        //
                        // Sometimes this will fail,
                        // notably when the mouse is being dragged
                        // back onto the window... curX,curY
                        // are out of the window
                        // but the event's coords are in the window.
                        if (curX != e.getX()
                         || curY != e.getY())
                        {
                            System.out.println("I'M CONFUSED: current position was "+curX+","+curY+", but mouse released at "+e.getX()+","+e.getY()+"");
                            // Should we set curX,curY to the release position
                            // in this case?  Not sure...
                            // I'm saying no.
                            if (false)
                            {
                                curX = e.getX();
                                curY = e.getY();
                            }
                        }
                        if (mode == MODE_ASSETS)
                        {
                            double tooFar = dropRadiusPixels;
                            FuzzyPoint closestPoint = new FuzzyPoint();
                            if (connectionCurves == null)
                                recalculateCurves();
                            int indicesOfClosestThing[/*3*/] = getIndicesOfClosestThing(
                                                            curX, curY,
                                                            assets,
                                                            connectionCurves,
                                                            tooFar,
                                                            maxFuzzyRadiusPixels,
                                                            closestPoint,
                                                            true);
                            //printArray("indicesOfClosestThing", indicesOfClosestThing);

                            if (mouseState == STATE_DRAGGING_ASSET)
                            {
                                // Finished dragging an asset
                                if (autoClean)
                                    scheduleCleanup(doCenterWhenAutoClean);
                            }
                            else if (mouseState == STATE_DRAGGING_NOTHING)
                            {
                                // Finished panning
                                if (autoClean)
                                    scheduleCleanup(doCenterWhenAutoClean);
                            }
                            else if (mouseState == STATE_CTRL_DRAGGING_ASSET)
                            {
                                // attach the asset, as src,
                                // to the event or other asset (or the "nothing")
                                // we just dragged it to.
                                if (indicesOfClosestThing[0] != -1)
                                {
                                    // to an asset
                                    System.out.println("dragged an asset to another asset");
                                    printArray("connections", connections);
                                    int newEvent[][] = {{indexOfThingBeingDragged},{indicesOfClosestThing[0]}};
                                    connections = (int[][][])append(connections, newEvent);
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                                else if (indicesOfClosestThing[1] != -1)
                                {
                                    // to an event
                                    System.out.println("dragged an asset to an event");
                                    printArray("connections", connections);
                                    if (indexOf(connections[indicesOfClosestThing[1]][0], indexOfThingBeingDragged) == -1)
                                        connections[indicesOfClosestThing[1]][0] =
                                                (int[])append(connections[indicesOfClosestThing[1]][0], new Integer(indexOfThingBeingDragged));
                                    else
                                        System.out.println("\007This asset is already a src of this event!"); // XXX bell
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                                else
                                {
                                    // to nothingness
                                    System.out.println("dragged an asset to nothing");
                                    printArray("connections", connections);
                                    int newEvent[][] = {{indexOfThingBeingDragged},{}};
                                    connections = (int[][][])append(connections, newEvent);
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                            }
                            else if (mouseState == STATE_CTRL_DRAGGING_EVENT)
                            {
                                // if event was dragged to an asset,
                                // add that asset to the event.
                                // if event was dragged to another event,
                                // merge the events.
                                if (indicesOfClosestThing[0] != -1)
                                {
                                    // to an asset
                                    System.out.println("dragged an event to an asset");
                                    printArray("connections", connections);
                                    if (indexOf(connections[indexOfThingBeingDragged][1], indicesOfClosestThing[0]) == -1)
                                        connections[indexOfThingBeingDragged][1] = (int[])append(connections[indexOfThingBeingDragged][1], new Integer(indicesOfClosestThing[0]));
                                    else
                                        System.out.println("\007This asset is already a dst of this event!"); // XXX bell
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                                else if (indicesOfClosestThing[1] != -1)
                                {
                                    // to an event
                                    System.out.println("dragged an event to an event");
                                    int i0 = indexOfThingBeingDragged;
                                    int i1 = indicesOfClosestThing[1];
                                    printArray("connections", connections);
                                    connections[i0][0] = (int[])concat(connections[i0][0], connections[i1][0]);
                                    connections[i0][1] = (int[])concat(connections[i0][1], connections[i1][1]);
                                    connections = (int[][][])deleteElement(connections, i1);
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                                else
                                {
                                    // to nothingness
                                    System.out.println("dragged an event to nothing");
                                }
                            }
                            else if (mouseState == STATE_CTRL_DRAGGING_NOTHING)
                            {
                                // if "nothing" was dragged to an asset,
                                // make a new event with no srcs
                                // and just that asset as its destination.
                                if (indicesOfClosestThing[0] != -1)
                                {
                                    // to an asset
                                    System.out.println("dragged nothingness to an asset");
                                    printArray("connections", connections);

                                    int newEvent[][] = {{},{indicesOfClosestThing[0]}};
                                    connections = (int[][][])append(connections, newEvent);
                                    printArray("connections", connections);
                                    connectionCurves = null; // dirty
                                }
                                else if (indicesOfClosestThing[1] != -1)
                                {
                                    // to an event
                                    System.out.println("dragged nothingness to an event (that does nothing)");
                                }
                                else
                                {
                                    // to nothingness
                                    System.out.println("dragged nothingness to nothing (that does nothing)");
                                }
                            }
                        }
                        indexOfThingBeingDragged = -1;
                        subIndexOfThingBeingDragged = -1;
                        mouseState = STATE_NOTHING;
                        repaint();

                        if (eventVerbose >= 1) System.out.println("    out mouseReleased");
                    }
                    public void mouseEntered(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mouseEntered at "+e.getX()+","+e.getY()+"");

                        canvas.requestFocus();

                        // XXX this was messing things up
                        //mouseState = STATE_MOVING;
                        repaint();
                        if (eventVerbose >= 1) System.out.println("    out mouseEntered");
                    }
                    public void mouseExited(java.awt.event.MouseEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in mouseExited at "+e.getX()+","+e.getY()+"");
                        // XXX this was messing things up
                        //mouseState = STATE_NOTHING;
                        repaint();
                        if (eventVerbose >= 1) System.out.println("    out mouseExited");
                    }
                });
                this.addKeyListener(new java.awt.event.KeyListener() {
                    public void keyPressed(java.awt.event.KeyEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in keyPressed");
                        char c = e.getKeyChar();
                        switch(c)
                        {
                            case 'h':
                            case '?':
                                help();
                                break;

                            case '+': case '=':
                            case '-':
                                zoom(c);
                                break;

                            case 'l':
                                setShowLabels(!showLabels);
                                break;

                            case 'T':
                                shadowThickness += 1.;
                                System.out.println("shadowThickness = "+shadowThickness);
                                repaint();
                                break;

                            case 't':
                                shadowThickness -= 1.;
                                shadowThickness = Math.max(shadowThickness, 1.);
                                System.out.println("shadowThickness = "+shadowThickness);
                                repaint();
                                break;

                            case 'R':
                                srcRadii[0] += 1.;
                                dstRadii[0] += 1.;
                                if (srcRadii[0] == dstRadii[0])
                                    System.out.println("radius = "+srcRadii[0]);
                                else
                                {
                                    System.out.println("srcRadius = "+srcRadii[0]);
                                    System.out.println("dstRadius = "+dstRadii[0]);
                                }

                                connectionCurves = null; // dirty
                                repaint();
                                break;
                            case 'r':
                                if ((srcRadii[0] -= 1.) < 0.)
                                    srcRadii[0] = 0.;
                                if ((dstRadii[0] -= 1.) < 0.)
                                    dstRadii[0] = 0.;
                                if (srcRadii[0] == dstRadii[0])
                                    System.out.println("radius = "+srcRadii[0]);
                                else
                                {
                                    System.out.println("srcRadius = "+srcRadii[0]);
                                    System.out.println("dstRadius = "+dstRadii[0]);
                                }

                                connectionCurves = null; // dirty
                                repaint();
                                break;

                            case 's':
                                doArrowHeadOnSingleSrc ^= true;
                                connectionCurves = null; // dirty
                                repaint();
                                break;
                            case 'm': // switch between MODE_ASSETS and MODE_ONE_CURVE
                                mode = 1-mode;
                                repaint();
                                break;
                            case 'a': // toggle autoClean
                                if (mode == MODE_ASSETS)
                                {
                                    autoClean ^= true;
                                    if (autoClean)
                                        scheduleCleanup(doCenterWhenAutoClean);
                                    autoCleanCheckBox.setSelected(autoClean);
                                    cleanCircleButton.setEnabled(!autoClean);
                                    cleanAndCenterButton.setEnabled(!autoClean);
                                }
                                repaint();
                                break;
                            case 'C': // clean up circle and re-center
                            case 'c': // clean up circle and don't re-center
                                {
                                    if (mode == MODE_ASSETS)
                                    {
                                        scheduleCleanup(c=='C');
                                        connectionCurves = null; // dirty
                                    }
                                }
                                repaint();
                                break;
                            case 'n': // add a new asset
                                if (mode == MODE_ASSETS)
                                {
                                    assets = (double[][])append(assets, new double[] {curX, curY});
                                    assetVelocities = (double[][])append(assetVelocities, new double[] {0.,0.});
                                    futureAssets = (double[][])append(futureAssets, new double[] {0.,0.});
                                    nFramesUntilFutures = (int[])append(nFramesUntilFutures, new Integer(0));
                                    // no need to dirty curves since this is added on the end and isn't a part of any curve
                                    if (autoClean)
                                        scheduleCleanup(doCenterWhenAutoClean);
                                }
                                repaint();
                                break;
                            case java.awt.event.KeyEvent.VK_DELETE:
                            case 'D': // delete closest asset at any distance
                            case 'd': // delete an asset within deleteRadiusPixels
                                if (mode == MODE_ASSETS)
                                {
                                    // delete closest thing
                                    // (asset, event, or membership)
                                    double tooFar = (c=='D' ? Double.POSITIVE_INFINITY : deleteRadiusPixels);
                                    FuzzyPoint closestPoint = new FuzzyPoint();
                                    int indicesOfClosestThing[/*3*/] = getIndicesOfClosestThing(
                                                                    curX, curY,
                                                                    assets,
                                                                    connectionCurves,
                                                                    tooFar,
                                                                    maxFuzzyRadiusPixels,
                                                                    closestPoint,
                                                                    false);
                                    if (indicesOfClosestThing[0] != -1)
                                    {
                                        int ind = indicesOfClosestThing[0]; // asset index to delete
                                        System.out.println("    deleting asset "+ind+"/"+assets.length+"");
                                        assets = (double[][])deleteElement(assets, ind);
                                        assetVelocities = (double[][])deleteElement(assetVelocities, ind);
                                        futureAssets = (double[][])deleteElement(futureAssets, ind);
                                        nFramesUntilFutures = (int[])deleteElement(nFramesUntilFutures, ind);
                                        printArray("connections", connections);
                                        {
                                            for (int i = 0; i < connections.length; ++i)
                                            for (int j = 0; j < connections[i].length; ++j)
                                            {
                                                int nNew = 0;
                                                for (int kOld = 0; kOld < connections[i][j].length; ++kOld)
                                                {
                                                    int old = connections[i][j][kOld];
                                                    if (old != ind)
                                                        connections[i][j][nNew++] = (old>ind ? old-1 : old);
                                                }
                                                connections[i][j] = (int[])subarray(connections[i][j], 0, nNew);
                                            }
                                        }
                                        printArray("connections", connections);

                                        if (autoClean)
                                            scheduleCleanup(doCenterWhenAutoClean);
                                    }
                                    else if (indicesOfClosestThing[1] != -1)
                                    {
                                        // XXX should just delete the connection
                                        int ind = indicesOfClosestThing[1];
                                        int legInd = indicesOfClosestThing[2];
                                        int event[][] = connections[ind];
                                        int nSrcs = event[0].length;
                                        int nDsts = event[1].length;
                                        if (legInd < nSrcs)
                                        {
                                            System.out.println("    deleting asset "+connections[ind][0][legInd]+" from srcs of Event "+arrayToString(connections[ind]));
                                            connections[ind][0] = (int[])deleteElement(connections[ind][0], legInd);
                                        }
                                        else if (legInd < nSrcs+nDsts)
                                        {
                                            System.out.println("    deleting asset "+connections[ind][1][legInd-nSrcs]+" from dsts of Event "+arrayToString(connections[ind]));
                                            connections[ind][1] = (int[])deleteElement(connections[ind][1], legInd-nSrcs);
                                        }
                                        else
                                        {
                                            System.out.println("    deleting event "+ind+"/"+connections.length+" (closest subpart was "+legInd+")");
                                            connections = (int[][][])deleteElement(connections, ind);
                                        }
                                    }
                                }
                                connectionCurves = null; // dirty
                                repaint();
                                break;
                            case 's'-'a'+1: // ctrl-s -- save
                                save("DUMP.txt");
                                break;
                            case 'l'-'a'+1: // ctrl-l -- load
                                // dirties and repaints if it succeeds...
                                load("DUMP.txt");
                                break;

                            case 'q':
                            case 'Q':
                            case 'q'-'a'+1: // ctrl-q
                            case 'c'-'a'+1: // ctrl-c
                            case java.awt.event.KeyEvent.VK_ESCAPE:
                                System.out.println("Chow!");
                                System.exit(0);
                                break;

                            case 65535:
                                // XXX empirically, shift and ctrl-- weird
                                break;
                            default:
                                {
                                    System.out.println("Unknown key '"+c+"'("+(int)c+")");
                                }
                                break;
                        }
                        if (eventVerbose >= 1) System.out.println("    out keyPressed");
                    }
                    public void keyReleased(java.awt.event.KeyEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in keyReleased");
                        if (eventVerbose >= 1) System.out.println("    out keyReleased");
                    }
                    public void keyTyped(java.awt.event.KeyEvent e)
                    {
                        if (eventVerbose >= 1) System.out.println("    in keyTyped");
                        if (eventVerbose >= 1) System.out.println("    out keyTyped");
                    }
                });
            } // MyCanvas ctor

        } // MyCanvas

        public MyCanvas canvas = new MyCanvas(); // XXX weird place for this maybe

        // super.paintComponent clears offscreen pixmap,
        // since we're using double buffering by default.
        // XXX what the heck did that mean?  I took this from an example off the web and I don't understand what it means.
        protected void clear(java.awt.Graphics g)
        {
            super.paintComponent(g);
        }

        public MyDemoPanel(boolean useSplitPane)
        {
            super();

            //
            // Make the layout manager be a BorderLayout...
            //
            setLayout(new java.awt.GridBagLayout());

            //
            // Add children...
            //

            javax.swing.JPanel buttonRow1 = new javax.swing.JPanel();
            buttonRow1.setBackground(java.awt.Color.lightGray); // so it stands out
            javax.swing.JPanel buttonRow2 = new javax.swing.JPanel();
            buttonRow2.setBackground(java.awt.Color.lightGray); // so it stands out

            {
                buttonRow1.add(new javax.swing.JButton(new javax.swing.AbstractAction("+") {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        zoom('+');
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                }));
            }
            {
                buttonRow1.add(new javax.swing.JButton(new javax.swing.AbstractAction("-") {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        zoom('-');
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                }));
            }

            cleanCircleButton = new javax.swing.JButton("Clean Circle");
            cleanAndCenterButton = new javax.swing.JButton("Clean&Center");

            {
                javax.swing.AbstractButton child = cleanCircleButton;
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        scheduleCleanup(false); // don't re-center
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow1.add(child);
            }
            {
                javax.swing.AbstractButton child = cleanAndCenterButton;
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        scheduleCleanup(true); // do re-center
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow1.add(child);
            }
            {
                buttonRow1.add(autoCleanCheckBox = new javax.swing.JCheckBox(new javax.swing.AbstractAction("Auto Clean") {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        //setAutoClean(autoCleanCheckBox.isSelected());
                        setAutoClean(((javax.swing.JCheckBox)e.getSource()).isSelected());
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                }));
            }
            {
                final javax.swing.JCheckBox child = new javax.swing.JCheckBox("Labels");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        setShowLabels(child.isSelected());
                        repaint();
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow1.add(child);

                // We need to keep track of it to keep it in sync...
                showLabelsCheckBox = child;
            }

            {
                javax.swing.AbstractButton child = new javax.swing.JButton("+ radius");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        srcRadii[0] += 1.;
                        dstRadii[0] += 1.;
                        if (srcRadii[0] == dstRadii[0])
                            System.out.println("radius = "+srcRadii[0]);
                        else
                        {
                            System.out.println("srcRadius = "+srcRadii[0]);
                            System.out.println("dstRadius = "+dstRadii[0]);
                        }
                        connectionCurves = null; // dirty
                        repaint();
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow2.add(child);
            }
            {
                javax.swing.AbstractButton child = new javax.swing.JButton("- radius");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        if ((srcRadii[0] -= 1.) < 0.)
                            srcRadii[0] = 0.;
                        if ((dstRadii[0] -= 1.) < 0.)
                            dstRadii[0] = 0.;
                        if (srcRadii[0] == dstRadii[0])
                            System.out.println("radius = "+srcRadii[0]);
                        else
                        {
                            System.out.println("srcRadius = "+srcRadii[0]);
                            System.out.println("dstRadius = "+dstRadii[0]);
                        }
                        connectionCurves = null; // dirty
                        repaint();
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow2.add(child);
            }
            {
                javax.swing.AbstractButton child = new javax.swing.JButton("Save DUMP.txt");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        save("DUMP.txt");
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow2.add(child);
            }
            {
                javax.swing.AbstractButton child = new javax.swing.JButton("Load DUMP.txt");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        load("DUMP.txt");
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow2.add(child);
            }
            {
                javax.swing.AbstractButton child = new javax.swing.JButton("Help");
                child.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        help();
                        if (allButtonsRelinqushFocusToCanvas)
                            canvas.requestFocus();
                    }
                });
                buttonRow2.add(child);
            }

            //
            // Make sure gui elements are in sync
            // (checkbox states and enabledness)
            //
            setAutoClean(autoClean);
            setShowLabels(showLabels);


            {
                javax.swing.JPanel buttonBars = new javax.swing.JPanel();
                buttonBars.setLayout(new javax.swing.BoxLayout(buttonBars, javax.swing.BoxLayout.Y_AXIS));
                buttonBars.add(buttonRow1);
                buttonBars.add(buttonRow2);

                if (useSplitPane)
                {
                    javax.swing.JSplitPane splitPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, true);
                    this.add(splitPane);
                    splitPane.add(canvas);
                    splitPane.add(buttonBars);
                }
                else
                {
                    {
                        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
                        c.gridy = 0;
                        c.fill = java.awt.GridBagConstraints.BOTH; // stretch component to fill space
                        c.weightx = 1.; // stretch space horizontally
                        c.weighty = 1.; // stretch space vertically
                        this.add(canvas, c);
                    }
                    {
                        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
                        c.gridy = 1;
                        c.fill = java.awt.GridBagConstraints.BOTH; // stretch component to fill space
                        c.weightx = 1.; // stretch space horizontally
                        c.weighty = 0.; // don't stretch space vertically
                        this.add(buttonBars, c);
                    }
                }
            }

            //
            // Make the help window
            //
            {
                helpWindow = new javax.swing.JFrame("Asset Graph Stuff Help");
                String htmlHelpMessage = "<html><pre>";
                for (int i = 0; i < helpMessage.length; ++i)
                    htmlHelpMessage += helpMessage[i] + "  \n";
                htmlHelpMessage += "</pre></html>";
                helpWindow.getContentPane().add(new javax.swing.JLabel(htmlHelpMessage));
                helpWindow.setLocation(200,200); // XXX do I really want this? can I center it instead?  doing it so the help window doesn't end up in same place as main window.
                helpWindow.pack();
                //helpWindow.setVisible(true); // uncomment when debugging it
            }

        } // MyDemoPanel() ctor

        //
        // Given position p and velocity v, at time t,
        // find position and velocity
        // at time t+1, assuming
        // it's folling a cubic trajectory
        // whose position and velocity at t=0 is 0.
        //
        static void update_p_and_v(double p_and_v[/*2*/], double t)
        {
            double p = p_and_v[0];
            double v = p_and_v[1];

            double newP, newV;
            if (false)
            {
                // Linear -- instantaneous start and stop
                newP = lerp(p, 0, -1./t);
                newV = p/t;
            }
            else
            {
                // Cubic! woohoo!
                // Did this on paper.
                double invT = 1./t;
                double a = (-2*p*invT + v)*invT*invT;
                double b = (3*p*invT - v)*invT;
                // The cubic path we are following is:
                // f(x) =    a x^3 +   b x^2
                // f'(x) = 3 a x^2 + 2 b x
                // newP = f(t+1)
                // newV = f'(t+1)
                double tPlus1 = t+1;
                newP = (a*tPlus1 + b)*tPlus1*tPlus1;
                newV = (3*a*tPlus1 + 2*b)*tPlus1;
            }

            p_and_v[0] = newP;
            p_and_v[1] = newV;
        } // update_p_and_v

        private void scheduleCleanup(boolean doCenter)
        {
            double newCenter[] = (doCenter ? new double[] {canvas.getWidth()/2, canvas.getHeight()/2} : null);
            scheduleFutureAssets(cleanUpCircle(assets, newCenter));
        } // scheduleCleanup
        private void scheduleFutureAssets(double newFutureAssets[][])
        {
            futureAssets = newFutureAssets;
            for (int i = 0; i < assets.length; ++i)
                nFramesUntilFutures[i] = 100; // XXX not sure should hard code?
            repaint();
        } // scheduleFutureAssets

        private void setAutoClean(boolean newAutoClean)
        {
            autoClean = newAutoClean;
            if (autoClean)
                scheduleCleanup(doCenterWhenAutoClean);
            cleanCircleButton.setEnabled(!autoClean);
            cleanAndCenterButton.setEnabled(!autoClean);
            autoCleanCheckBox.setSelected(autoClean);
        } // setAutoClean
        private void setShowLabels(boolean newShowLabels)
        {
            showLabels = newShowLabels;
            showLabelsCheckBox.setSelected(showLabels);
            repaint();
        } // setShowLabels

        private void zoom(char c) // '-' or '+'/'='
        {
            double center[] = average(mode==MODE_ONE_CURVE ? controlPoints : assets, null);
            double scale = Math.pow(2., .1);
            if (c == '-')
            {
                scale = 1./scale;
                // Attempt clever way to make everything
                // drift towards centered--
                // expand from center of object,
                // but shrink towards center of screen!
                center = new double[] {canvas.getWidth()/2,canvas.getHeight()/2};
            }
            if (mode == MODE_ONE_CURVE)
                for (int i = 0; i < controlPoints.length; ++i)
                    controlPoints[i] = lerp(center, controlPoints[i], scale);
            else if (mode == MODE_ASSETS)
                for (int i = 0; i < assets.length; ++i)
                    assets[i] = lerp(center, assets[i], scale);
            connectionCurves = null; // dirty
            repaint();
        } // zoom

        static final String helpMessage[] = {
            "",
            "    Mouse:",
            "            Left-drag - move asset or pan whole picture",
            "            Ctrl-Left drag - connect something:",
            "                asset -> asset -- new event from src to dst",
            "                asset -> event -- add the asset to the event as another src",
            "                event -> asset -- add the asset to the event as another dst",
            "                event -> event -- merge the events",
            "                asset -> nothingness -- new event with just the single src",
            "                nothingness -> asset -- new event with just the single dst",

            "    Keys:",
            "            n -- new asset",
            "     Delete/d -- delete asset or event",
            "            D -- delete nearest asset or event at any distance",
            "            s -- toggle arrowheads on single-src events",
            "            m -- toggle mode between asset graph and single cubic curve",
            "",
            "          =/+ -- zoom in",
            "            - -- zoom out",
            "            c -- clean up circle (after n, d, and dragging assets around)",
            "            C -- clean up circle and re-center",
            "            a -- toggle auto-clean",
            "            l -- toggle labels",
            "",
            "            R -- increase asset radius",
            "            r -- decrease asset radius",
            "            R -- increase white shadow thickness",
            "            r -- decrease white shadow thickness",
            "       Ctrl-s -- save to DUMP.txt",
            "       Ctrl-l -- load from DUMP.txt",
            "          ?/h -- show this help message",
            "    Escape/Ctrl-c/Ctrl-q/q/Q -- quit",
            "",
        }; // helpMessage

        private void help()
        {
            helpWindow.setState(java.awt.Frame.NORMAL); // not ICONIFIED
            helpWindow.setVisible(true);
            helpWindow.toFront();

            if (false)
            {
                for (int i = 0; i < helpMessage.length; ++i)
                    System.out.println(helpMessage[i]);
            }

        } // help



        private static void transform(double points[][/*2*/],
                                      double oldX, double oldY,
                                      double scale,
                                      double newX, double newY)
        {
            for (int i = 0; i < points.length; ++i)
            {
                points[i][0] = (points[i][0] - oldX)*scale + newX;
                points[i][1] = (points[i][1] - oldY)*scale + newY;
            }
        } // transform
        private static void pan(double points[][/*2*/],
                                double dx,
                                double dy)
        {
            transform(points, 0, 0, 1., dx, dy);
        } // pan


        private static double[][] makeCircle(int n, double x, double y, double r)
        {
            double assets[][] = new double[n][2];
            for (int i = 0; i < n; ++i)
            {
                double ang = 2*Math.PI*i/n;
                assets[i][0] = x + r*Math.cos(ang);
                assets[i][1] = y - r*Math.sin(ang); // XXX upside down, for now, til I get coord space fixed
            }
            return assets;
        } // makeCircle

        /**
         * A neat-o function,
         */
        private static double[][/*2*/] cleanUpCircle(double oldCircle[][/*2*/],
                                                     double newCenter[]) // may be null
        {
            int n = oldCircle.length;
            if (n <= 2)
                return oldCircle;
            double oldCenter[/*2*/] = Arrows.average(oldCircle,null);
            if (newCenter == null)
                newCenter = oldCenter;
            double distsFromCenter[] = new double[n];
            for (int i = 0; i < n; ++i)
                distsFromCenter[i] = dist(oldCenter[0],oldCenter[1],oldCircle[i][0], oldCircle[i][1]);
            double newRadius = median(distsFromCenter);

            final double angs[] = new double[n];
            for (int i = 0; i < n; ++i)
                angs[i] = Math.atan2(oldCircle[i][1]-oldCenter[1],
                                     oldCircle[i][0]-oldCenter[0]);
            int inds[] = getSortedInds(angs);

            //
            // We want ang0
            // such that ang0 + 2*Math.PI*i/n  is as close as possible 
            // to angs[inds[i]] for all i.
            //
            double ang0 = 0.;
            {
                double totalAdjustmentNeeded = 0.;
                for (int i = 0; i < n; ++i)
                    totalAdjustmentNeeded += angs[inds[i]]
                                           - (ang0 + 2*Math.PI*i/n);
                ang0 += totalAdjustmentNeeded / n;
            }

            double newCircle[][] = new double[n][2];
            for (int i = 0; i < n; ++i)
            {
                double oldAng = angs[inds[i]];
                double newAng = ang0 + 2*Math.PI*i/n;
                newCircle[inds[i]][0] = newCenter[0] + newRadius*Math.cos(newAng);
                newCircle[inds[i]][1] = newCenter[1] + newRadius*Math.sin(newAng);
            }
            return newCircle;
        } // cleanUpCircle

        /**
         * Returns the median of an array of doubles.
         */
        private static double median(final double A[])
        {
            int n = A.length;
            int inds[] = getSortedInds(A);
            if (n == 0)
                return 0.;
            else if (n % 2 == 0)
                return (A[inds[n/2-1]] + A[inds[n/2]]) / 2;
            else
                return A[inds[(n-1)/2]];
        } // median

        // Return a list of indices into A,
        // such that A[inds[0]] <= A[inds[1]] <= ... <= A[inds[n-1]].
        private static int[] getSortedInds(final double A[])
        {
            int n = A.length;
            Integer Inds[] = new Integer[n];
            for (int i = 0; i < n; ++i)
                Inds[i] = new Integer(i);
            //java.util.Arrays.sort(Inds, new java.util.Comparator<Integer>() // for >= 1.5
            java.util.Arrays.sort(Inds, new java.util.Comparator() // for <= 1.4
            {
                public int compare(Object aObj, Object bObj) // for <= 1.4
                {
                    int a = ((Integer)aObj).intValue();
                    int b = ((Integer)bObj).intValue();
                    return A[a] < A[b] ? -1 :
                           A[a] > A[b] ? 1 : 0;
                }
                public int compare(Integer aObj, Integer bObj) // for >= 1.5
                {
                    int a = ((Integer)aObj).intValue();
                    int b = ((Integer)bObj).intValue();
                    return A[a] < A[b] ? -1 :
                           A[a] > A[b] ? 1 : 0;
                }
            });
            int inds[] = new int[n];
            for (int i = 0; i < n; ++i)
                inds[i] = Inds[i].intValue();
            return inds;
        } // getSortedInds


        private static int findClosest(double x, double y, double points[][/*2*/], double tooFar)
        {
            int bestI = -1;
            double bestDistSqrd = tooFar*tooFar;

            for (int i = 0; i < points.length; ++i)
            {
                double thisDistSqrd = distSqrd(x, y, points[i][0], points[i][1]);
                if (thisDistSqrd < bestDistSqrd)
                {
                    bestDistSqrd = thisDistSqrd;
                    bestI = i;
                }
            }
            return bestI;
        } // findClosest

        //
        // If closest thing is an asset,
        // return its index in [0] and set [1] and [2] to -1.
        // If closest thing is a curve segment of an event,
        // then set [1] and [2] to its indices and set [0]
        // to zero.
        //
        private static int[/*3*/] getIndicesOfClosestThing(
                                      double curX, double curY,
                                      double assets[][/*2*/],
                                      java.awt.Shape curves[][],
                                      double tooFar,
                                      double maxFuzzyRadius,
                                      FuzzyPoint closestPoint, // output
                                      boolean assetsShouldTakePrecedenceIfFound)
        {
            int closestAssetIndex = findClosest(curX, curY, assets, tooFar);
            //PRINT(closestAssetIndex);
            //PRINT(assetsShouldTakePrecedenceIfFound);
            if (closestAssetIndex != -1)
                tooFar = dist(curX,curY,assets[closestAssetIndex][0],assets[closestAssetIndex][1]);
            int closestCurveIndices[];
            if (closestAssetIndex != -1 && assetsShouldTakePrecedenceIfFound)
            {
                closestCurveIndices = new int[] {-1,-1};
            }
            else
            {
                do { if (!(curves != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+2375 +"): " + "curves != null" + ""); } while (false);
                closestCurveIndices = getClosestPointOnCubicCurves(
                                            curX, curY,
                                            curves,
                                            tooFar,
                                            maxFuzzyRadiusPixels,
                                            closestPoint);
            }
            if (closestCurveIndices[0] != -1)
                return new int[] {-1, closestCurveIndices[0], closestCurveIndices[1]};
            else if (closestAssetIndex != -1)
            {
                closestPoint.x = assets[closestAssetIndex][0];
                closestPoint.y = assets[closestAssetIndex][1];
                closestPoint.rSqrd = 0.;
                return new int[] {closestAssetIndex, -1, -1};
            }
            else
                return new int[] {-1,-1,-1};
        } // getIndicesOfClosestThing



        // How close does the mouse have to be to select something?
        private static final double selectRadiusPixels = 10;
        private static final double dropRadiusPixels = 20;
        private static final double deleteRadiusPixels = 30;
        private static final double maxFuzzyRadiusPixels = .5; // how precisely to get closest point on curves


        private int curX = 0;
        private int curY = 0;
        private int curWidth = -1;
        private int curHeight = -1;

        private int eventVerbose = 0;
        private int verbose = 0;
        private int nDebugSubdivisionLevels = 0; // set to higher to see curve subdivisions
        boolean autoClean = false;
        boolean doCenterWhenAutoClean = true; // not sure what it should be; a preference maybe.  actually now that I tried false I think it has to be true, maybe can get rid
        boolean showLabels = false;
        boolean allButtonsRelinqushFocusToCanvas = true; // XXX seems to be what I want-- allows using keys right after clicking a button.  I think that's what I want, anyway.

        // these track autoClean and showLabels
        javax.swing.JCheckBox autoCleanCheckBox;
        javax.swing.JCheckBox showLabelsCheckBox;
        javax.swing.JButton cleanCircleButton;
        javax.swing.JButton cleanAndCenterButton;
        javax.swing.JFrame helpWindow;

        private static final int MODE_ONE_CURVE = 0;
        private static final int MODE_ASSETS = 1;
        private int mode = MODE_ASSETS;
        //private int mode = MODE_ONE_CURVE;

        private static final int STATE_NOTHING = 0;
        private static final int STATE_MOVING = 1;
        private static final int STATE_CTRL_MOVING = 2;
        private static final int STATE_DRAGGING_ASSET = 3;
        private static final int STATE_CTRL_DRAGGING_ASSET = 4;
        private static final int STATE_DRAGGING_EVENT = 5;
        private static final int STATE_CTRL_DRAGGING_EVENT = 6;
        private static final int STATE_DRAGGING_NOTHING = 7;
        private static final int STATE_CTRL_DRAGGING_NOTHING = 8;

        private int mouseState = STATE_NOTHING;

            // these mean something when mouseState is ...DRAGGING...
            private int indexOfThingBeingDragged = -1;
            private int subIndexOfThingBeingDragged = -1;


            //
            // For when mode is MODE_ONE_CURVE...
            //
                double controlPoints[][] = {
                    {10,100},
                    {20, 50},
                    {80, 50},
                    {90, 100},
                };
            //
            // For when mode is MODE_ASSETS...
            //
                double srcRadii[] = {20}; // how far to keep arrow feet away
                double dstRadii[] = {20}; // how far to keep arrow heads away
                double arrowHeadLength = 10;
                double arrowLengthForSingle = 40;
                boolean doArrowHeadOnSingleSrc = false; // XXX need way to toggle
                double lineThickness = 1;
                double shadowThickness = 7;

                double assets[][] = null; // create in first paint()
                double assetVelocities[][] = null; // create in first paint()
                int connections[][/*2*/][] = null; // create in first paint()

                // If easing in to a reconfiguration,
                // we set futureAssets and a number of frames
                // in which to ease into it.
                double futureAssets[][] = null;
                int nFramesUntilFutures[] = null;

                java.awt.Shape connectionCurves[][] = null; // dirty; calculated on demand

        private void recalculateCurves()
        {
            connectionCurves = new java.awt.Shape[connections.length][];
            for (int i = 0; i < connections.length; ++i)
            {
                connectionCurves[i] = getCurves(
                                assets,connections[i][0],
                                assets,connections[i][1],
                                srcRadii,
                                dstRadii,
                                arrowHeadLength,
                                average(assets,null),
                                arrowLengthForSingle,
                                doArrowHeadOnSingleSrc);
                do { if (!(connectionCurves[i] != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/Arrows.prejava"+"("+2493 +"): " + "connectionCurves[i] != null" + ""); } while (false);
            }
        } // calculateCurves

        private void save(String fileName)
        {
            //System.out.println("double assets[][/*2*/] = "+arrayToString(assets)+";");
            //System.out.println("int connections[][/*2*/][] = "+arrayToString(connections)+";");
            System.out.print("Saving to "+fileName+"... ");
            System.out.flush();
            try {
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(fileName)));
                writer.println("double assets[][/*2*/] = "+arrayToString(assets)+";");
                writer.println("int connections[][/*2*/][] = "+arrayToString(connections)+";");
                writer.flush();
                writer.close();
            }
            catch (Exception exc)
            {
                System.out.println("save to file "+fileName+" failed:" + exc);
            }
            System.out.println("done.");
        } // save
        private void load(String fileName)
        {
            System.out.print("Loading from "+fileName+"... ");
            System.out.flush();
            try {
                java.io.LineNumberReader reader = new java.io.LineNumberReader(new java.io.FileReader(fileName)); // LineNumberReader is already a buffered reader
                String buf = "";
                String line;
                while ((line = reader.readLine()) != null)
                    buf += line + "\n";
                reader.close();

                //PRINT(buf);
                int firstBrace = buf.indexOf('{');
                int firstSemiColon = buf.indexOf(';');
                int secondBrace = buf.indexOf('{', firstSemiColon+1);
                int secondSemiColon = buf.indexOf(';',firstSemiColon+1);
                if (firstBrace != -1
                 && firstSemiColon != -1
                 && secondBrace != -1
                 && secondSemiColon != -1
                 && firstBrace < firstSemiColon
                 && secondBrace < secondSemiColon)
                {
                    String assetsContents = buf.substring(firstBrace, firstSemiColon);
                    //PRINT(assetsContents);
                    String connectionsContents = buf.substring(secondBrace, secondSemiColon);
                    //PRINT(connectionsContents);

                    double newAssets[][] = (double[][])parseArray(assetsContents, double[][].class);
                    int newConnections[][][] = (int[][][])parseArray(connectionsContents, int[][][].class);
                    if (newAssets != null
                     && newConnections != null)
                    {
                        // XXX should validate more!

                        // cute-- if same length, ease in
                        //PRINT(newAssets.length);
                        //PRINT(assets.length);
                        if (newAssets.length == assets.length)
                            scheduleFutureAssets(newAssets);
                        else
                        {
                            assets = newAssets;
                            assetVelocities = new double[assets.length][2]; // zeros
                            futureAssets = new double[assets.length][2]; // arbitrary
                            nFramesUntilFutures = new int[assets.length]; // zeros
                        }
                        connections = newConnections;

                        connectionCurves = null; // dirty
                        repaint();
                    }
                }
                else
                {
                    System.out.print("(DAMN, messed up) ");
                    System.out.flush();
                }
            }
            catch (Exception exc)
            {
                System.out.println("load from file "+fileName+" failed:" + exc);
            }
            System.out.println("done.");
        } // load






        //--------------------------------------------------------------------
        // Little array utils...
        //
        private static Object subarray(Object array, int start, int n)
        {
            Object subarray = java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), n);
            System.arraycopy(array, start, subarray, 0, n);
            return subarray;
        } // subarray
        private static Object deleteElement(Object array, int ind)
        {
            int n = java.lang.reflect.Array.getLength(array);
            Object newArray = java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), n-1);
            System.arraycopy(array, 0, newArray, 0, ind);
            System.arraycopy(array, ind+1, newArray, ind, n-1-ind);
            return newArray;
        } // deleteElement
        /**
         * Returns a newly allocated array
         * consisting of the old array with the "last" object appended.
         * <br>
         * array can have scalar components,
         * in which case last should be of the corresponding wrapper type.
         */
        public static Object append(Object array,
                                    Object last)
        {
            int n = java.lang.reflect.Array.getLength(array);
            Object result = java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), n+1);
            System.arraycopy(array, 0, result, 0, n);
            java.lang.reflect.Array.set(result, n, last);
            return result;
        } // append

        public static int indexOf(int array[], int last)
        {
            for (int i = 0; i < array.length; ++i)
                if (array[i] == last)
                    return i;
            return -1;
        } // indexOf

        /**
         * Returns a newly allocated array
         * that is the concatenation of the given arrays.
         */
        public static Object concat(Object array0,
                                    Object array1)
        {
            int n0 = java.lang.reflect.Array.getLength(array0);
            int n1 = java.lang.reflect.Array.getLength(array1);
            Object newArray = java.lang.reflect.Array.newInstance(
                                        array0.getClass().getComponentType(),
                                        n0 + n1);
            System.arraycopy(array0, 0, newArray, 0, n0);
            System.arraycopy(array1, 0, newArray, n0, n1);
            return newArray;
        } // concat

        // XXX wtf? jikes chokes on both of these using 1.5 libraries
        public static String arrayToString(Object array)
        {
            StringBuffer sb = new StringBuffer();
            appendArrayToString(array, sb);
            return sb.toString();
        }
        public static void appendArrayToString(Object array, StringBuffer sb)
        {
            if (array == null)
                sb.append("null");
            else if (!array.getClass().isArray())
                sb.append(array);
            else
            {
                int n = java.lang.reflect.Array.getLength(array);
                sb.append("{");
                for (int i = 0; i < n; ++i)
                {
                    if (i > 0)
                        sb.append(",");
                    appendArrayToString(java.lang.reflect.Array.get(array,i), sb);
                }
                sb.append("}");
            }
        }

        // When using the C preprocessor, I use this all the time:
        //  #define PRINTARRAY(x) System.out.println(#x + " = " + arrayToString(x))
        // The following function is the best I can do without it.
        public static void printArray(String name, Object array)
        {
            System.out.println(name + " = " + arrayToString(array));
        } // printArray


        // not really efficient, but who cares.
        // XXX sort of lame, can't tell the different
        // XXX between null success and failure
        public Object parseArray(String s, Class type)
        {
            int index[] = {0};
            Object result = _parseArray(s, type, index);
            return result;
        }

        // recursive work function used by parseArray()
        private Object _parseArray(String s,
                                   Class type,
                                   int _index[/*1*/]) // input/output
        {
            //System.out.println("in _parseArray(s=\""+s+"\", type = "+type+", index = "+_index[0]+"");
            int len = s.length();
            int index = _index[0];
            int origIndex = index; // in case of failure
            if (type == int.class
             || type == double.class)
            {
                String numBuf = "";
                while (index < len
                    && "-.0123456789eE".indexOf(s.charAt(index)) != -1)
                {
                    numBuf += s.charAt(index);
                    index++;
                }
                _index[0] = index;
                try {
                    if (type == int.class)
                        return Integer.valueOf(numBuf);
                    else // type == double.class
                        return Double.valueOf(numBuf);
                } catch (NumberFormatException e)
                {
                    _index[0] = origIndex;
                    return null;
                }
            }
            else if (type.isArray())
            {
                if (s.indexOf("null", index) == index)
                {
                    index += 4;
                    _index[0] = index;
                    return null; // success
                }
                if (index < len
                 && s.charAt(index) == '{')
                {
                    Class componentType = type.getComponentType();
                    Object result = java.lang.reflect.Array.newInstance(
                                                            componentType, 0);
                    index++;
                    while (index < len
                        && s.charAt(index) != '}')
                    {
                        _index[0] = index;
                        Object item = _parseArray(s, componentType, _index);
                        if (_index[0] == index) // if it failed
                        {
                            _index[0] = origIndex;
                            return null; // failure
                        }
                        index = _index[0];

                        result = append(result, item);

                        if (index < len
                         && s.charAt(index) == ',')
                            index++;
                    }
                    if (index == len
                     || s.charAt(index) != '}')
                    {
                        _index[0] = origIndex;
                        return null; // failure
                    }
                    index++;
                    _index[0] = index;
                    return result;
                }
                else
                {
                    _index[0] = origIndex;
                    return null; // failure
                }
            }
            else
            {
                // XXX unrecognized type... warning?
                return null;
            }
        } // _parseArray

        /*
        {
            PRINTARRAY(parseArray("null", int[][][].class));
            PRINTARRAY(parseArray("{}", int.class));
            PRINTARRAY(parseArray("{}", int[].class));
            PRINTARRAY(parseArray("{}", int[][][].class));
            PRINTARRAY(parseArray("{4}", int[].class));
            PRINTARRAY(parseArray("{{}}", int[][].class));
            PRINTARRAY(parseArray("{{}}", int[][][].class));
            PRINTARRAY(parseArray("{{},{}}", int[][][].class));
            PRINTARRAY(parseArray("{{{}}}", int[][][].class));
            PRINTARRAY(parseArray("{{{}},{}}", int[][][].class));
            PRINTARRAY(parseArray("{{{2}},{}}", int[][][].class));
            PRINTARRAY(parseArray("{{{2,3},{},{4,5,6}},{{5,6}}}", int[][][].class));
        }
        */
        // End of little array utils.
        //
        //--------------------------------------------------------------------
    } // class MyDemoPanel



    /**
     * Little demonstration program for getCurves().
     */
    public static void main(String args[])
    {
        System.out.println("in main");
        String title = "Asset Graph Stuff";

        javax.swing.JFrame frame = new javax.swing.JFrame(title);

        frame.setForeground(java.awt.Color.white);
        frame.setBackground(java.awt.Color.black);

        boolean useSplitPane = false;
        if (args.length >= 1
         && args[0].equals("--useSplitPane"))
        {
            useSplitPane = true;
            args = (String[])MyDemoPanel.subarray(args, 1, args.length-1);
        }

        final MyDemoPanel myDemoPanel = new MyDemoPanel(useSplitPane);
        frame.setContentPane(myDemoPanel);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent event)
            {
                System.out.println("Chow!");
                System.exit(0);
            }
        });

        int w = 800, h = 800;
        if (args.length == 2)
        {
            w = Integer.parseInt(args[0]);
            h = Integer.parseInt(args[1]);
        }

        myDemoPanel.canvas.setPreferredSize(new java.awt.Dimension(w,h)); // XXX hmm, is it strange for a preferred size to be set by the caller? oh well
        frame.pack();
        frame.setLocation(50,50); // XXX do I really want this? can I center it instead?  doing it so the help window doesn't end up in same place.
        frame.setVisible(true);

        myDemoPanel.canvas.requestFocus(); // seems to be needed initially

        System.out.println("out main");
    } // main

} // class Arrows
