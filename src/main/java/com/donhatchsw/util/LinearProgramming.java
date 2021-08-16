// 2 # 1 "com/donhatchsw/util/LinearProgramming.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/LinearProgramming.prejava"
/*
* Copyright (c) 2006 Don Hatch Software
*/
//
// LinearProgramming.prejava
//
// Author: Don Hatch (hatch@plunk.org)
// This code may be used for any purpose as long as it is good and not evil.
//

package com.donhatchsw.util;

// 18 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 22 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 24 # 14 "com/donhatchsw/util/LinearProgramming.prejava" 2



/**
 * Linear programming, using the the simplex method,
 * straight out of Numerical Recipes in C,
 * but with simpler interface functions
 * and more robust tests for convergence
 * regardless of the scale of the input data.
 */

// XXX TODO: I think it fails now if I set objective function to -1,-1,...
// which is weird, I thought I tested that
// XXX TODO: I don't understand why it's succeeding
// in the case of m2 variables... aren't they minus what they
// should be on output?


public final class LinearProgramming
{
    private LinearProgramming() {} // uninstantiatable

    /**
     * Find x that minimizes ||Ax-b|| in the one-norm
     * (manhattan distance).
     * <p>
     * If n is the length of x and m is the length of b,
     * this is phrased as a linear programming problem
     * by adding m new variables <code>z = {z[0]..z[m-1]}</code>
     * and minimizing <code>z[0]+...+z[m-1]</code>
     * subject to:
     * <pre>
     *         z >= Ax-b     (m equations)
     *         z >= -(Ax-b)  (m equations)
     * </pre>
     * I.e.
     * <pre>
     * <pre>
     *        Ax - z <= b    (m equations)
     *       -Ax - z <= -b   (m equations)
     *        maximize -z[0]-...-z[m-1]
     * </pre>
     * To express this in terms of LPSolve, we set:
     * <pre>
     *        A' = empty (no equality constraints)
     *        b' = empty (no equality constraints)
     *              A|-I
     *        C' = --+--  where I = mxm identity matrix
     *             -A|-I
     *        d' = b followed by -b
     *        q' = n 0's followed by m -1's
     * </pre>
     * and we are solving for:
     * <pre>
     *        x' = x followed by z
     * </pre>
     * so we call:
     * <pre>
     *     LPSolve(x', null, A', b', c', d', q', false)
     * </pre>
     * Returns 0 (and fills in x) on success,
     * 1 if the linear programming problem is unbounded,
     * -1 if infeasible.
     */
    public static int L1Solve(double x[],
                              double A[][],
                              double b[])
    {
        int n = x.length;
        int m = A.length;
        do { if (!(m == b.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+84 +"): " + "m == b.length" + ""); } while (false);

        double xx[] = new double[n+m];
        double AA[][] = new double[0][n+m];
        double bb[] = new double[0];
        double CC[][]; // initialized below
        double dd[] = (double[])Arrays.concat(b, VecMath.sxv(-1.,b));
        double qq[] = (double[])Arrays.concat(VecMath.fillvec(n, 0.),
                                              VecMath.fillvec(m, -1.));
        //       A|-I
        // CC = --+--
        //      -A|-I
        double minusI[][] = VecMath.identitymat(m);
            VecMath.sxm(minusI, -1., minusI);
        double minusA[][] = VecMath.sxm(-1., A);
        double parts[][][][] = {{A,minusI},{minusA,minusI}};
        CC = VecMath.joinmats(parts);
        if (debugLevel >= 1)
        {
            System.out.println("A" + " =\n" + VecMath.toString(A));
            System.out.println("CC" + " =\n" + VecMath.toString(CC));
        }

        int ret = LPSolve(xx, null, AA, bb, CC, dd, qq, false);
        if (ret == 0)
            for (int i = 0; i < n; ++i)
                x[i] = xx[i];
        return ret;
    } // L1Solve

    /**
     * Find x that minimizes ||Ax-b|| in the infinity (max) norm.
     * <p>
     * If n is the length of x and m is the length of b,
     * this is phrased as a linear programming problem
     * by adding a single variable <code>z</code>
     * and minimizing <code>z</code>, subject to:
     * <pre>
     *          [z,z,...,z] >= Ax-b     (n equations)
     *          [z,z,...,z] >= -(Ax-b)  (n equations)
     * </pre>
     * i.e.
     * <pre>
     *           Ax - [z,...,z] <= b    (n equations)
     *          -Ax - [z,...,z] <= -b   (n equations)
     *          maximize -z
     * </pre>
     * To express this in terms of LPSolve, we set:
     * <pre>
     *          A' = empty (no equality constraints)
     *          b' = empty (no equality constraints)
     *          C' = A on top of -A, with a column of -1's added on the right
     *          d' = b followed by -b
     *          q' = n 0's followed by a single -1 at the end
     * </pre>
     * and we are solving for:
     * <pre>
     *          x' = x with a single z appended at the end
     * </pre>
     * so we call:
     * <pre>
     *     LPSolve(x', null, A', b', c', d', q', false)
     * </pre>
     * Returns 0 (and fills in x) on success,
     * 1 if the linear programming problem is unbounded,
     * -1 if infeasible.
     */
    public static int LInfinitySolve(double x[],
                                     double A[][],
                                     double b[])
    {
        int n = x.length;
        double xx[] = new double[n+1];
        double AA[][] = new double[0][2*n];
        double bb[] = new double[0];
        double CC[][] = new double[2*A.length][n+1]; // initialized below
        double dd[] = (double[])Arrays.concat(b, VecMath.sxv(-1.,b));
        double qq[] = VecMath.fillvec(n+1, 0.); qq[n] = -1.;

        // initialize CC to be A on top of -A
        // with a column of -1's added on the right...
        for (int iRow = 0; iRow < A.length; ++iRow)
        {
            do { if (!(A[iRow].length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+167 +"): " + "A[iRow].length == n" + ""); } while (false);
            for (int i = 0; i < n; ++i)
                CC[A.length+iRow][i] = -(CC[iRow][i] = A[iRow][i]);
            CC[iRow][n] = -1;
            CC[A.length+iRow][n] = -1;
        }
        if (debugLevel >= 1)
        {
            System.out.println("A" + " =\n" + VecMath.toString(A));
            System.out.println("CC" + " =\n" + VecMath.toString(CC));
        }

        int ret = LPSolve(xx, null, AA, bb, CC, dd, qq, false);
        if (ret == 0)
            for (int i = 0; i < n; ++i)
                x[i] = xx[i];
        return ret;
    } // LInfinitySolve

    /**
     * Find x such that the vector Mx has sum 1
     * and maximal minimum element (NOT taking absolute values).
     * Returns 0 (and fills in x) on success,
     * returns 1 if unbounded, or -1 if infeasible.
     * <p>
     * This is a convenience function
     * that is implemented in terms of maximizeMinimum as follows:
     * <pre>
     *     A = [[1,1,..,1] * M]         (1 x n matrix)
     *     b = [1]
     *     C = M
     *     d = [0,0,...,0]
     *     return maximizeMinimum(x, A, b, C, d);
     * </pre>
     */
    public static int maximizeMinimumAffine(double x[],
                                            double M[][])
    {
        int n = x.length;
        double A[][] = {VecMath.vxm(VecMath.fillvec(M.length, 1.), M)};
        double b[] = {1.};
        double C[][] = M;
        double d[] = VecMath.fillvec(M.length, 0.);
        return maximizeMinimum(x, A, b, C, d);
    } // maximizeMinimumAffine

    /**
     * Find a solution vector x
     * that satisfies A x == b
     * and maximizes the minimum element of C x - d.
     * Returns 0 (and fills in x) on success,
     * returns 1 if unbounded, or -1 if infeasible.
     * <p>
     * This is a convenience function; it is implemented
     * in terms of LPSolve as follows:
     * <pre>
     * We want to maximize the smallest element of:
     *        ((C[0] dot x)   - d[0])
     *        ((C[1] dot x)   - d[1])
     *        ...
     *        ((C[m-1] dot x) - d[m-1])
     *      
     * The trick is to add another variable "smallest"
     * with constraints:
     *      smallest <= ((C[0] dot x)   - d[0])
     *      smallest <= ((C[1] dot x)   - d[1])
     *      ...
     *      smallest <= ((C[m-1] dot x) - d[m-1])
     * i.e.
     *      ((-C[0] dot x)   + smallest <= -d[0])
     *      ((-C[1] dot x)   + smallest <= -d[1])
     *      ...
     *      ((-C[m-1] dot x) + smallest <= -d[m-1])
     * so the new variables are x[0]..x[n-1],smallest
     * and our object is to maximize smallest;
     * this is clearly a linear programming problem now.
     * To express it in the language of our LPSolve function,
     *     A' = A with a column of 0's added on the right
     *     b' = b
     *     C' = -C with a column of 1's added on the right
     *     d' = -d
     *     q' = n 0's with a single 1 at the end
     * and we are solving for
     *     x' = x with smallest appended at the end,
     * so we call:
     *     LPSolve(x', null, A', b', c', d', q', false)
     * </pre>
     */
    public static int maximizeMinimum(double x[], // solution
                                      double A[][], double b[], // equalities Ax==b
                                      double C[][], double d[]) // maximize minimum element of Cx - d
    {
        int n = x.length;
        do { if (!(A.length == b.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+260 +"): " + "A.length == b.length" + ""); } while (false);
        do { if (!(C.length == d.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+261 +"): " + "C.length == d.length" + ""); } while (false);
        // row length assertions will be done below

        double xx[] = new double[n+1];
        double AA[][] = new double[A.length][n+1];
        double bb[] = b;
        double CC[][] = new double[C.length][n+1];
        double dd[] = new double[d.length];
        double qq[] = new double[n+1];

        for (int iA = 0; iA < A.length; ++iA)
        {
            do { if (!(A[iA].length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+273 +"): " + "A[iA].length == n" + ""); } while (false);
            for (int i = 0; i < n; ++i)
                AA[iA][i] = A[iA][i];
            AA[iA][n] = 0.;
        }
        for (int iC = 0; iC < C.length; ++iC)
        {
            do { if (!(C[iC].length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+280 +"): " + "C[iC].length == n" + ""); } while (false);
            for (int i = 0; i < n; ++i)
                CC[iC][i] = -C[iC][i];
            CC[iC][n] = 1.;
            dd[iC] = -d[iC];
        }
        for (int i = 0; i < n; ++i)
            qq[i] = 0.;
        qq[n] = 1.;

        int ret = LPSolve(xx, null, AA, bb, CC, dd, qq, false);
        if (ret == 0)
            for (int i = 0; i < n; ++i)
                x[i] = xx[i];
        return ret;
    } // maximizeMinimum


    /**
     *
     * Solve a general linear programming problem.
     * Finds x such that:
     * <pre>
     *         A x == b
     *         C x <= d  componentwise
     *           x >= 0  componentwise  (but only if implicitRestrictNonNegative is set)
     *     q dot x maximal.
     * </pre>
     * Returns -1 if infeasible (in which case x will be undefined),
     * 1 if unbounded (in which case x will be undefined), or
     * 0 if finite (in which case x is filled in with the answer,
     *              and y, if non-null, is filled in
     *              with the non-negative values <code>d - C x</code>
     *              with exact zeros for those inequalities
     *              that were satisfied as equalities).
     * <p>
     * IMPLEMENTATION NOTE: setting implicitRestrictNonNegative to false
     * doubles the number of variables solved for internally,
     * so it is more time consuming; set it to true if you
     * can get away with it.  Future versions might
     * have a smarter implementation for which it doesn't matter.
     * <p>
     * IMPLEMENTATION NOTE: this is implemented
     * using the code from Numerical Recipes In C,
     * but with the initial rows (objective function and constraints)
     * rescaled so that the maximum coefficient in each row is 1,
     * so that the book's caveat about EPS should no longer apply.
     * <p>
     * XXX TODO: really implicitRestrictNonNegative should be
     * on a per-variable basis, since often
     * the extra variables are automatically non-negative...
     * hmm, can we automatically detect that and prune the system?
     * Nah, this becomes moot if we replace it with a smarter implementation.
     */
    public static int LPSolve(double x[], // solution
                              double y[], // slack d-Cx at solution
                              double A[][], double b[], // equalities Ax == b
                              double C[][], double d[], // inequalities Cx <= d
                              double q[], // objective: maximize q dot x
                              boolean implicitRestrictNonNegative)
    {
        int n = x.length; // number of variables
        int m = A.length + C.length; // number of constraints

        do { if (!(q.length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+344 +"): " + "q.length == n" + ""); } while (false);
        do { if (!(A.length == b.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+345 +"): " + "A.length == b.length" + ""); } while (false);
        do { if (!(C.length == d.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+346 +"): " + "C.length == d.length" + ""); } while (false);
        for (int i = 0; i < A.length; ++i)
            do { if (!(A[i].length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+348 +"): " + "A[i].length == n" + ""); } while (false);
        for (int i = 0; i < C.length; ++i)
            do { if (!(C[i].length == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+350 +"): " + "C[i].length == n" + ""); } while (false);

        if (!implicitRestrictNonNegative)
        {
            //
            // simplx solves using additional constraints x >= 0.
            // To get around this, add n more variables,
            // which will recieve the absolute values of the negative
            // components of the final solution,
            // solve the twice-as-big problem,
            // and then mix the two halves of the final solution together.
            //
            // This is really pretty lame, since we could also do it
            // by solving for x+BIG where BIG is a vector of large
            // positive numbers, and that new system
            // is the same size as the original one,
            // but unfortunately it's impossible
            // to know how big those numbers need to be beforehand
            // (in particular, if it's unbounded in a negative direction,
            // then we can never get it right by doing it that way).
            //
            // XXX TODO:
            // See the paper "The generalized simplex method for minimizing
            // a linear form under linear inequality restraints"
            // by Dantzig, Orden, Wolfe... I think it has
            // a modification that will do this intelligently.
            //
            double xx[] = new double[2*n];
            double AA[][] = new double[A.length][2*n];
            double CC[][] = new double[C.length][2*n];
            double qq[] = new double[2*n];

            for (int i = 0; i < n; ++i)
            {
                qq[n+i] = -(qq[i] = q[i]);
                for (int iA = 0; iA < A.length; ++iA)
                    AA[iA][n+i] = -(AA[iA][i] = A[iA][i]);
                for (int iC = 0; iC < C.length; ++iC)
                    CC[iC][n+i] = -(CC[iC][i] = C[iC][i]);
            }

            int ret = LPSolve(xx, y, AA, b, CC, d, qq, true);

            if (ret == 0)
                for (int i = 0; i < n; ++i)
                    x[i] = xx[i] - xx[n+i];

            return ret;
        } // if (!implicitRestrictNonNegative)

        if (n == 0)
            return 0; // success; don't bother with simplx since it will bomb in this case XXX I think--- maybe try it and see
        // I think it should work even if m is 0... should always return 1 though (unbounded).

        double M[][] = new double [1+1+m+1][];
        for (int i = 1; i < M.length; ++i) // start at 1, leave row 0 null
        {
            M[i] = new double[1+1+n];
            M[i][0] = Double.NaN;
        }

        // Fill in M...
        int nEQ = 0; // and counting
        int nLE = 0; // and counting
        int nGE = 0; // and counting
        int scrambled2unscrambledY[] = new int[m]; // since we scramble the inequalities since the >= ones need to go at the end
        {
            // Objective function row...
            M[1][1+0] = 0.;
            for (int i = 0; i < n; ++i)
                M[1][1+1+i] = q[i];

            for (int iNE = 0; iNE < C.length; ++iNE)
            {
                // simplx requires the constant to be >= 0,
                // so if that's not the case we negate the <= constraint
                // to get a >= constraint.
                int sign = d[iNE] < 0. ? -1 : 1;
                int iNEscrambled = (sign == -1 ? (m-1)-(nGE++) : (nLE++));
                int iRow = 1+1+iNEscrambled;
                M[iRow][1+0] = sign * d[iNE];
                for (int i = 0; i < n; ++i)
                    M[iRow][1+1+i] = sign * -C[iNE][i];
                scrambled2unscrambledY[iNEscrambled] = iNE;
            }
            for (int iEQ = 0; iEQ < A.length; ++iEQ)
            {
                // simplx requires constant column of input to be >= 0,
                // so if that's not the case we simply negate the equation.
                int sign = b[iEQ] < 0. ? -1 : 1;
                int iRow = 1+1+nLE+nGE+(nEQ++);
                M[iRow][1+0] = sign * b[iEQ];
                for (int i = 0; i < n; ++i)
                    M[iRow][1+1+i] = sign * -A[iEQ][i];
            }
        }


        int izrov[] = new int[1+n]; izrov[0] = -999;
        int iposv[] = new int[1+m]; iposv[0] = -999;
        int icase = simplx(M,
                           m,
                           n,
                           nLE,
                           nGE,
                           nEQ,
                           izrov,
                           iposv);
        do { if (!(M[0] == null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+458 +"): " + "M[0] == null" + ""); } while (false);
        for (int i = 1; i < M.length; ++i)
            do { if (!(Double.isNaN(M[i][0]))) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+460 +"): " + "Double.isNaN(M[i][0])" + ""); } while (false);
        do { if (!(izrov[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+461 +"): " + "izrov[0] == -999" + ""); } while (false);
        do { if (!(iposv[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+462 +"): " + "iposv[0] == -999" + ""); } while (false);

        if (icase == 0) // if got a finite result
        {
            for (int i = 0; i < x.length; ++i)
                x[i] = Double.NaN;
            if (y != null)
                for (int i = 0; i < y.length; ++i)
                    y[i] = Double.NaN;

            for (int i = 0; i < m; ++i)
                if (iposv[1+i]-1 < n)
                    x[iposv[1+i]-1] = M[1+1+i][1];
                else if (y != null && iposv[1+i]-1 - n < y.length)
                    y[scrambled2unscrambledY[iposv[1+i]-1 - n]] = M[1+1+i][1];
            for (int i = 0; i < n; ++i)
                if (izrov[1+i]-1 < n)
                    x[izrov[1+i]-1] = 0.;
                else if (y != null && izrov[1+i]-1 - n < y.length)
                    y[scrambled2unscrambledY[izrov[1+i]-1 - n]] = 0.;

            for (int i = 0; i < x.length; ++i)
                do { if (!(!Double.isNaN(x[i]))) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+484 +"): " + "!Double.isNaN(x[i])" + ""); } while (false);
            if (y != null)
                for (int i = 0; i < y.length; ++i)
                    do { if (!(!Double.isNaN(y[i]))) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+487 +"): " + "!Double.isNaN(y[i])" + ""); } while (false);
        }

        return icase;
    } // LPsolve

    /** You don't want to know.  If this is <code>>= 1</code>, print out the tableaux at various times during the internal simplx function call. If it's <code>>= 2</code>, print out even more stuff.  See, I told you, you can't handle the truth. */
    public static int debugLevel = 0;

    private final static double EPS = 1e-12;
    // Here EPS is the absolute precision; the book had 1e-6
    // for float, so we use 1e-12 for double.
    // The book says this should be adjusted to the scale of your variables,
    // but we try to do better: at the beginning of simplx,
    // we rescale each row so that the max coeff in each row is 1.

    //
    // Utilities used by printing...
    //
        private static String repeat(char s, int n)
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < n; ++i)
                sb.append(s);
            return sb.toString();
        }
        private static String ljustify(String s, int width)
        {
            do { if (!(s.length() <= width)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+515 +"): " + "s.length() <= width" + ""); } while (false);
            return s + repeat(' ', width-s.length());
        }
        private static String rjustify(String s, int width)
        {
            do { if (!(s.length() <= width)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+520 +"): " + "s.length() <= width" + ""); } while (false);
            return repeat(' ', width-s.length()) + s;
        }
        private static String lcenter(String s, int width)
        {
            do { if (!(s.length() <= width)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+525 +"): " + "s.length() <= width" + ""); } while (false);
            return repeat(' ', (width-s.length())/2) + s + repeat(' ', (width-s.length()+1)/2);
        }
        private static String rcenter(String s, int width)
        {
            do { if (!(s.length() <= width)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+530 +"): " + "s.length() <= width" + ""); } while (false);
            return repeat(' ', (width-s.length()+1)/2) + s + repeat(' ', (width-s.length())/2);
        }
        private static String justify(double x, int lWidth, int rWidth)
        {
            String s = ""+x;
            int indexOfDecimalPoint = s.indexOf('.');
            do { if (!(indexOfDecimalPoint != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+537 +"): " + "indexOfDecimalPoint != -1" + ""); } while (false);
            int nBefore = indexOfDecimalPoint;
            int nAfter = s.length() - (indexOfDecimalPoint+1);
            return repeat(' ', lWidth-nBefore) + s + repeat(' ', rWidth-nAfter);
        }
        static void appendSeparatorRow(StringBuffer sb,
                                       int n,
                                       int colWidths[],
                                       char primaryChar,
                                       String singleCross,
                                       String doubleCross)
        {
            sb.append(repeat(primaryChar, colWidths[0]));
            sb.append(doubleCross);
            for (int iCol = 1; iCol <= n+1; ++iCol)
            {
                sb.append(repeat(primaryChar,colWidths[iCol]));
                sb.append(iCol==n+1 ? doubleCross : singleCross);
            }
            sb.append("\n");
        } // appendSeparatorRow
        static String varLabel(int iVar, int m, int n)
        {
            do { if (!(iVar <= m+n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+560 +"): " + "iVar <= m+n" + ""); } while (false);
            if (iVar <= n)
                return "x"+iVar; // original variable
            else if (iVar <= n+m)
                return "y"+(iVar-n); // slack variable
            else
                return "z" + (iVar-(n+m)); // artificial variable
        }

    private static String TableauToString(double a[/*1+1+m+1*/][/*1+1+n*/],
                                          int m,
                                          int n,
                                          int m1,
                                          int m2,
                                          int izrov[/*1+n*/],
                                          int iposv[/*1+m*/])
    {
        StringBuffer sb = new StringBuffer();

        //
        // Sanity check the arrays...
        //
        do { if (!(a.length == 1+1+m+1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+582 +"): " + "a.length == 1+1+m+1" + ""); } while (false);
        do { if (!(a[0] == null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+583 +"): " + "a[0] == null" + ""); } while (false);
        do { if (!(a[1].length == 1+1+n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+584 +"): " + "a[1].length == 1+1+n" + ""); } while (false);
        for (int iRow = 1; iRow < a.length; ++iRow)
            do { if (!(Double.isNaN(a[iRow][0]))) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+586 +"): " + "Double.isNaN(a[iRow][0])" + ""); } while (false);
        do { if (!(izrov[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+587 +"): " + "izrov[0] == -999" + ""); } while (false);
        do { if (!(iposv[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+588 +"): " + "iposv[0] == -999" + ""); } while (false);

        //
        // Figure out optimal column widths
        // XXX could concievably overflow if many thousands of variables
        //
        int colWidths[] = new int[1+1+n];
        int maxBeforeDecimal[] = new int[1+1+n];
        int maxAfterDecimal[] = new int[1+1+n];
        for (int iCol = 1; iCol <= n+1; ++iCol)
        {
            maxBeforeDecimal[iCol] = 2; // minimum so colWidth is at least 5
            maxAfterDecimal[iCol] = 2; // minimum so colWidth is at least 5
            for (int iRow = 1; iRow <= m+2; ++iRow)
            {
                String s = ""+a[iRow][iCol];
                int indexOfDecimalPoint = s.indexOf('.');
                do { if (!(indexOfDecimalPoint != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+605 +"): " + "indexOfDecimalPoint != -1" + ""); } while (false);
                int nBefore = indexOfDecimalPoint;
                int nAfter = s.length() - (indexOfDecimalPoint+1);
                if (nBefore > maxBeforeDecimal[iCol]) maxBeforeDecimal[iCol] = nBefore;
                if (nAfter > maxAfterDecimal[iCol]) maxAfterDecimal[iCol] = nAfter;
            }
            colWidths[iCol] = maxBeforeDecimal[iCol] + 1 + maxAfterDecimal[iCol];
        }
        colWidths[0] = 4; // width of label column

        //
        // Print top labels
        //
        {
            sb.append(repeat(' ', colWidths[0]));
            sb.append("  ");
            for (int iCol = 1; iCol <= n+1; ++iCol)
            {
                String label;
                if (iCol == 1)
                    label = "";
                else
                    label = varLabel(izrov[iCol-1], m, n);
                label = lcenter(label, colWidths[iCol]);
                sb.append(label);
                sb.append(iCol==n+1 ? "||" : "|");
            }
            sb.append("\n");
        }
        appendSeparatorRow(sb, n, colWidths, '=', "+", "++");
        for (int iRow = 1; iRow <= m+2; ++iRow)
        {
            if (iRow == m+2)
                appendSeparatorRow(sb, n, colWidths, '=', "+", "++");
            String label;
            if (iRow == 1)
                label = "z";
            else if (iRow == m+2)
                label = "z'";
            else
                label = varLabel(iposv[iRow-1], m, n);
            label = ljustify(label.length()==colWidths[0] ? label : " "+label, colWidths[0]);
            sb.append(label);
            sb.append("||");
            for (int iCol = 1; iCol <= n+1; ++iCol)
            {
                sb.append(justify(a[iRow][iCol], maxBeforeDecimal[iCol], maxAfterDecimal[iCol]));
                sb.append(iCol==n+1 ? "||" : "|");
            }
            sb.append("\n");
            if (iRow == 1)
                appendSeparatorRow(sb, n, colWidths, '-', "+", "++");
        }
        appendSeparatorRow(sb, n, colWidths, '-', "+", "++");

        sb.deleteCharAt(sb.length()-1); // delete final newline
        return sb.toString();
    } // printTableau




    private static void myCheckForFeasibility(double a[/*1+1+m+1*/][/*1+1+n*/],
                                              int m,
                                              int n,
                                              int m1,
                                              int m2,
                                              int izrov[/*1+n*/],
                                              int iposv[/*1+m*/],
                                              boolean useAuxRow)
    {
        double objectiveFunctionRow[] = a[useAuxRow ? m+1 : 1];
    }

    //
    // Simplex method for linear programming.
    //
    // On output, the tableau A
    // is indexed by two returned arrays of integers.
    // iposv[j] contains, for j = 1..m, the number i in 1..m
    // whose original variable x_i is now represented by row j+1 of a.
    // These are thus the left-hand variables in the solution.
    // (The first row if A is of course the z-row.)
    // A value i > N indicates that the variable is a y_i
    // rather than an x_i, x_(N+j) === y_j.  XXX I didn't follow this
    // Likewise, izrov[j] contains, for j = 1...n,
    // the number i in 1..m
    // whose original variable x_i is now a right-hand variable,
    // represented by column j+1 of A.
    // These variables are all zero in the solution.
    // The meaning of i>n is the same as above, except that i>n+m1+m2
    // denotes an artificial or slack variable which was used only internally
    // and should now be entirely ignored.
    //
    // The function returns 0 if a finite solution is found,
    // +1 if the objective function is unbounded,
    // -1 if no solution satisfies the given constraints.
    //
    //
    private static int simplx(double a[/*1+1+m+1*/][/*1+1+n*/], // in/out
                              int m, // in
                              int n, // in
                              int m1, // in: number of <='s
                              int m2, // in: number of >='s
                              int m3, // in; number of =='s
                              int izrov[/*1+n*/], // out
                              int iposv[/*1+m*/]) // out
    {
        if (debugLevel >= 1) System.out.println("    In simplx");
        if (m != (m1+m2+m3))
            throw new Error("Bad input constraint counts in simplx");

        // Initialize l1, which is the index list of variable-columns admissible for exchange,
        // and make all original variables initially right-hand.
        int l1[] = new int[1+n]; l1[0] = -999; // note the book allocates 1..n+1 but only 1..n are used so that's what I'm allocating
        int nl1 = n;
        if (debugLevel >=1 ) System.out.println("n" + " = " + (n));
        if (debugLevel >=1 ) System.out.println("m" + " = " + (m));
        for (int k = 1; k <= n; k++)
            l1[k] = izrov[k] = k;
        for (int i = 1; i <= m; i++)
        {
            if (a[i+1][1] < 0.)
                throw new Error("Bad input tableau in simplx");
            // Constants b[i] must be nonnegative.
            iposv[i] = n+i;
            // Initial left-hand variables.
            // m1 (<=) type constraints are represented by having their slack
            // variable initially left-hand, with no artificial variable.
            // m2 (>=) type constraints have their slack variable
            // initially left-hand, with a minus sign, and their artificial
            // variable handled implicitly during their first exchange.
            // m3 (==) type constraints have their artificial variables
            // initially left-hand.
        }

        //
        // Try to make it more robust (i.e. fix the book's caveat about EPS)
        // by scaling each row (z and y's) to have maximum element 1.
        // So then working_y[i] = original_y[i] / max(original_y[i]).
        // We will need to unscale everything at the end...
        // if the row still appears as a row, we'll multiply it
        // by the original max;
        // if it appears as a column, we'll divide it by
        // the original max.
        // XXX TODO: could also rescale the columns too if we cared...
        //
        double origRowMaxes[] = null;
        boolean doConditioning = true;
        if (doConditioning)
        {
            if (debugLevel >= 1)
            {
                System.out.println("        BEFORE CONDITIONING:");
                System.out.println("izrov" + " = " + VecMath.toString(izrov));
                System.out.println("iposv" + " = " + VecMath.toString(iposv));
                System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
            }
            origRowMaxes = new double[1+1+m]; origRowMaxes[0] = Double.NaN;
            for (int iRow = 1; iRow <= 1+m; ++iRow)
            {
                double max = 0.;
                for (int iCol = 1; iCol <= 1+n; ++iCol)
                    max = ((max)>=(((a[iRow][iCol]) < 0 ? -(a[iRow][iCol]) : (a[iRow][iCol])))?(max):(((a[iRow][iCol]) < 0 ? -(a[iRow][iCol]) : (a[iRow][iCol]))));
                origRowMaxes[iRow] = max;
                if (max != 0.)
                    VecMath.vxs(a[iRow], a[iRow], 1./max);
            }
            if (debugLevel >= 1) System.out.println("origRowMaxes" + " = " + VecMath.toString(origRowMaxes));
        }

        int kpScratch[] = new int[1];
        double bmaxScratch[] = new double[1];
        if (m2+m3 != 0)
        {
            // Origin is not a feasible starting solution: we must do phase one.

            // Initialize l3, which is the list of constraints whose slack variables
            // have never been exchanged out of the initial basis.
            boolean l3[] = new boolean[1+m2]; l3[0] = true; //  note the book allocates 1..m but only 1..m2 are used so that's what I'm allocating
            for (int i = 1; i <= m2; i++)
                l3[i] = true;
            for (int k = 1; k <= n+1; k++)
            {
                // Compute the auxiliary objective function.
                double q1 = 0.;

                // NOTE: In the text,
                // it gives an aux objective function that would be obtained
                // by considering all 4 rows, i.e. starting at i=1.
                // In the code, we skip the two m1-type rows, starting at i=1+m1 instead
                // and only considering the last 2 rows in that example.
                // However if I blindly use 1 instead of 1+m1 here, it returns infeasible.
                // But... is this the cruz of why I don't get the same answer
                // as the book?
                for (int i = 1+m1; i <= m; i++)
                    q1 += a[i+1][k];
                a[m+2][k] = -q1;
            }

            if (debugLevel >= 1) System.out.println("        PHASE ONE");
            if (debugLevel >= 4)
            {
                System.out.println("a" + " =\n" + VecMath.toString(a));
            }
            if (debugLevel >= 1)
            {
                System.out.println("izrov" + " = " + VecMath.toString(izrov));
                System.out.println("iposv" + " = " + VecMath.toString(iposv));
                System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
            }

            for (;;)
            {
                boolean goingto_one = false; // simulate goto with bubblegum and string
                int ip = -999; // doesn't really need initializing, but compiler thinks so

                // Find max. coeff. of auxiliary objective fn.
                simp1(a, m+1, l1, nl1, false, kpScratch, bmaxScratch);
                int kp = kpScratch[0];
                double bmax = bmaxScratch[0];
                if (bmax <= EPS && a[m+2][1] < -EPS)
                {
                    // Auxiliary objective function is still negative
                    // and can't be improved, hence no
                    // feasible solution exists.
                    do { if (!(l1[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+831 +"): " + "l1[0] == -999" + ""); } while (false);
                    do { if (!(l3[0] == true)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+832 +"): " + "l3[0] == true" + ""); } while (false);
                    if (debugLevel >= 1) System.out.println("    Out simplx (infeasible)");
                    return -1;
                }
                else if (bmax <= EPS && a[m+2][1] <= EPS)
                {
                    // Auxiliary objective function is zero and
                    // can't be improved; we have a feasible starting vector.
                    // Clean out the artificial variables
                    // corresponding to any remaining m3 (equality) constraints
                    // by goto one and then move on to phase two.
                    if (debugLevel >= 1) System.out.println("            WE HAVE A FEASIBLE STARTING VECTOR");
                    for (ip = m1+m2+1; ip <= m; ip++)
                    {
                        if (iposv[ip] == (ip+n))
                        {
                            // Found an artificial variable for an
                            // equality constraint.
                            simp1(a,ip,l1,nl1,true,kpScratch,bmaxScratch);
                            kp = kpScratch[0];
                            bmax = bmaxScratch[0];
                            // Exchange with column corresponding to maximum
                            // pivot element in row ip.
                            if (bmax > EPS)
                            {
                                //goto one;
                                goingto_one = true;
                                System.out.println("                GOING TO ONE");
                                break;
                            }
                        }
                    }
                    if (!goingto_one)
                    {
                        // Change sign of row for any m2 constraints
                        // still present from the initial basis.
                        for (int i = m1+1; i <= m1+m2; i++)
                        {
                            if (l3[i-m1])
                                for (int k = 1; k <= n+1; k++)
                                    a[i+1][k] = -a[i+1][k];
                        }
                        break; // Go to phase two.
                    }
                }
                if (!goingto_one)
                {
                    // Locate a pivot element (phase one).
                    ip = simp2(a,m,n,kp);
                    if (ip == 0)
                    {
                        // Maximum of auxiliary objective function is
                        // unbounded, so no feasible solution exists.
                        do { if (!(l1[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+885 +"): " + "l1[0] == -999" + ""); } while (false);
                        do { if (!(l3[0] == true)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+886 +"): " + "l3[0] == true" + ""); } while (false);
                        if (debugLevel >= 1) System.out.println("    Out simplx (infeasible, aux objective function unbounded)");
                        return -1;
                    }
                }
//one:
                goingto_one = false;
                if (debugLevel >= 1) System.out.println("                pivoting row "+ip+" ("+varLabel(iposv[ip],m,n)+") with col "+kp+" ("+varLabel(izrov[kp],m,n)+")");
                pivot(a,m+1,n,ip,kp);
                // Exchange a left- and a right-hand variable (phase one),
                // then update lists.
                if (iposv[ip] >= n+m1+m2+1)
                {
                    // Exchanged out an artificial variable
                    // for an equality constraint.
                    // Make sure it stays out by removing it from the l1 list.
                    int k;
                    for (k = 1; k <= nl1; k++)
                        if (l1[k] == kp)
                            break;
                    --nl1;
                    for (int is = k; is <= nl1; is++)
                        l1[is] = l1[is+1];
                }
                else
                {
                    int kh = iposv[ip]-m1-n;
                    if (kh >= 1 && l3[kh])
                    {
                        // Exchanged out an m2 type constraint
                        // for the first time.
                        // Correct the pivot column for the minus sign
                        // and the implicit artificial variable.
                        l3[kh] = false;
                        ++a[m+2][kp+1];
                        for (int i = 1; i <= m+2; i++)
                        {
                            a[i][kp+1] = -a[i][kp+1];
                        }
                    }
                }

                // Update lists of left-and right-hand variables.
                {
                    // SWAP(izrov[kp], iposv[ip]);
                    int is = izrov[kp];
                    izrov[kp] = iposv[ip];
                    iposv[ip] = is;
                }
            } // Still in phase 1, go back to for(;;).
            do { if (!(l3[0] == true)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+936 +"): " + "l3[0] == true" + ""); } while (false);
        } // if (m2+m3 != 0)

        if (debugLevel >= 1) System.out.println("        PHASE TWO");
        if (debugLevel >= 4)
        {
            System.out.println("a" + " =\n" + VecMath.toString(a));
        }
        if (debugLevel >= 1)
        {
            System.out.println("izrov" + " = " + VecMath.toString(izrov));
            System.out.println("iposv" + " = " + VecMath.toString(iposv));
            System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
        }

        // End of phase one code for finding an initial feasible solution.
        // Now, in phase two, optimize it.
        for (;;)
        {
            simp1(a,0,l1,nl1,false,kpScratch,bmaxScratch); // Test the z-row for doneness.
            int kp = kpScratch[0];
            double bmax = bmaxScratch[0];
            if (bmax <= EPS)
            {
                // Done.  Solution found.  Return with the good news.

                //
                // Oh wait, unscale first.
                //
                if (doConditioning)
                {
                    if (debugLevel >= 1)
                    {
                        System.out.println("        SOLUTION BEFORE UNCONDITIONING:");
                        System.out.println("izrov" + " = " + VecMath.toString(izrov));
                        System.out.println("iposv" + " = " + VecMath.toString(iposv));
                        System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
                    }

                    if (origRowMaxes[1] != 0.)
                    {
                        if (debugLevel >= 3) System.out.println("Unconditioning final z row by multiplying by "+origRowMaxes[1]);
                        VecMath.vxs(a[1], a[1], 1./origRowMaxes[1]);
                    }
                    for (int i = 1; i <= m; ++i)
                        if (((1 <=(iposv[i]-n))&&((iposv[i]-n)<= m)))
                        {
                            double origRowMax = origRowMaxes[1+iposv[i]-n];
                            if (origRowMax > 0.)
                            {
                                if (debugLevel >= 3) System.out.println("Unconditioning final row var "+i+" by multiplying by "+origRowMax);
                                VecMath.vxs(a[1+i], a[1+i], origRowMax);
                            }
                        }
                    for (int i = 1; i <= n; ++i)
                        if (((1 <=(izrov[i]-n))&&((izrov[i]-n)<= m)))
                        {
                            double origRowMax = origRowMaxes[1+(izrov[i]-n)];
                            if (origRowMax >= 0.)
                            {
                                if (debugLevel >= 3) System.out.println("Unconditioning final column var "+i+" by dividing by "+origRowMax);
                                double invOrigRowMax = 1./origRowMax;
                                for (int j = 1; j <= m+1; ++j)
                                    a[j][1+i] *= invOrigRowMax;
                            }
                        }
                }




                do { if (!(l1[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1007 +"): " + "l1[0] == -999" + ""); } while (false);
                if (debugLevel >= 4)
                {
                    System.out.println("a" + " =\n" + VecMath.toString(a));
                }
                if (debugLevel >= 1)
                {
                    System.out.println("        SOLUTION:");
                    System.out.println("izrov" + " = " + VecMath.toString(izrov));
                    System.out.println("iposv" + " = " + VecMath.toString(iposv));
                    System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
                }
                if (debugLevel >= 1) System.out.println("    Out simplx (finite)");
                return 0;
            }
            int ip = simp2(a,m,n,kp); // Locate a pivot element (phase two).
            if (ip == 0)
            {
                // Objective function is unbounded.  Report and return.
                do { if (!(l1[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1026 +"): " + "l1[0] == -999" + ""); } while (false);
                if (debugLevel >= 1) System.out.println("    Out simplx (unbounded)");
                return 1;
            }
            if (debugLevel >= 1) System.out.println("                pivoting row "+ip+" ("+varLabel(iposv[ip],m,n)+") with col "+kp+" ("+varLabel(izrov[kp],m,n)+")");
            if (debugLevel >= 3)
            {
                System.out.println("Before:\n");
                System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
            }
            pivot(a,m,n,ip,kp); // Exchange a left- and right-hand variable (phase two).
            {
                // SWAP(izrov[kp], iposv[ip]);
                int is = izrov[kp];
                izrov[kp] = iposv[ip];
                iposv[ip] = is;
            }
            if (debugLevel >= 2)
            {
                System.out.println("After:\n");
                System.out.println("a" + " =\n" + TableauToString(a,m,n,m1,m2,izrov,iposv));
            }
        } // and return for another iteration.

    } // simplx

    //
    // Utility functions...
    //

        // Determines the maximum of those elements whose index is contained
        // in the supplied list ll,
        // either with or without taking the absolute value,
        // as flagged by iabf.
        private static void simp1(double a[][], // in
                                  int mm, // in
                                  int ll[], // in
                                  int nll, // in
                                  boolean iabf, // in
                                  int kp[/*1*/], // out
                                  double bmax[/*1*/]) // out
        {
            if (nll <= 0)
                bmax[0] = 0.; // No eligible columns.
            else
            {
                kp[0] = ll[1];
                bmax[0] = a[mm+1][kp[0]+1];
                for (int k = 2; k <= nll; k++)
                {
                    double test =
                        iabf ? Math.abs(a[mm+1][ll[k]+1]) - Math.abs(bmax[0])
                             : a[mm+1][ll[k]+1] - bmax[0];
                    if (test > 0.)
                    {
                        bmax[0] = a[mm+1][ll[k]+1];
                        kp[0] = ll[k];
                    }
                }
            }
        } // simp1

        // Locate a pivot element, taking degeneracy into account.
        private static int simp2(double a[][],
                                 int m,
                                 int n,
                                 int kp)
        {
            int i;
            for (i = 1; i <= m; i++)
                if (a[i+1][kp+1] < -EPS) break; // Any possible pivots?
            if (i > m)
                return 0;
            double q1 = -a[i+1][1]/a[i+1][kp+1];
            int ip = i;
            for (i = ip+1; i <= m; i++)
            {
                if (a[i+1][kp+1] < -EPS)
                {
                    double q = -a[i+1][1]/a[i+1][kp+1];
                    if (q < q1)
                    {
                        ip = i;
                        q1 = q;
                    }
                    else if (q == q1) // We have a degeneracy.
                    {
                        double q0=0., qp=0.; // doesn't really need initializing, assuming n>0 XXX which perhaps should be asserted
                        for (int k = 1; k <= n; k++)
                        {
                            qp = -a[ip+1][k+1]/a[ip+1][kp+1];
                            q0 = -a[i+1][k+1]/a[i+1][kp+1];
                            if (q0 != qp)
                                break;
                        }
                        if (q0 < qp)
                            ip = i;
                    }
                }
            }
            return ip;
        } // simp2

        // Matrix operations to exchange
        // a left-hand (row) and right-hand (column) variable.
        // Pivots row variable ip with column variable kp,
        // i.e. pivots row 1+ip with column 1+kp.
        private static void pivot(double a[][],
                                  int i1, // last row to worry about is 1+i1
                                  int k1, // last col to worry about is 1+k1
                                  int ip,
                                  int kp)
        {
            double piv = 1./a[ip+1][kp+1];
            for (int ii = 1; ii <= i1+1; ii++)
            {
                if (ii-1 != ip)
                {
                    a[ii][1+kp] *= piv;
                    for (int kk = 1; kk <= 1+k1; kk++)
                    {
                        if (kk-1 != kp)
                            a[ii][kk] -= a[1+ip][kk]*a[ii][1+kp];
                    }
                }
            }
            for (int kk = 1; kk <= 1+k1; kk++)
                if (kk-1 != kp)
                    a[1+ip][kk] *= -piv;
            a[1+ip][1+kp] = piv;
        } // pivot
    //=======================================================================

    // Function for testing small problems...
    private static int LPSolveByBruteForce(
                              double x[], // solution
                              double y[], // slack d-Cx at solution
                              double A[][], double b[], // equalities Ax == b
                              double C[][], double d[], // inequalities Cx <= d
                              double q[], // objective: maximize q dot x
                              boolean implicitRestrictNonNegative)
    {
        int debugThisFunction = 0;

        if (debugThisFunction >= 1) System.out.println("    In LPSolveByBruteForce");
        double AC[][] = (double[][])Arrays.concat(A,C);
        double bd[] = (double[])Arrays.concat(b,d);

        int nDims = x.length;
        if (implicitRestrictNonNegative)
        {
            // Add additional constraints
            // that say each x[i] >= 0
            double minusI[][] = VecMath.identitymat(nDims);
            VecMath.sxm(minusI, -1., minusI);
            AC = (double[][])Arrays.concat(AC, minusI);
            bd = (double[])Arrays.concat(bd, VecMath.zerovec(nDims));
        }

        {
            // Add additional constraints to bound the problem...
            // if these constraints are hit, we will report
            // that the solution is unbounded.
            double I[][] = VecMath.identitymat(nDims);
            double minusI[][] = VecMath.sxm(-1., I);
            double offsets[] = VecMath.fillvec(nDims, 1e6);
            AC = (double[][])Arrays.concat(AC, I);
            bd = (double[])Arrays.concat(bd, offsets);
            if (!implicitRestrictNonNegative)
            {
                AC = (double[][])Arrays.concat(AC, minusI);
                bd = (double[])Arrays.concat(bd, offsets);
            }
        }

        int nConstraintsToEnforce = AC.length;
        if (!implicitRestrictNonNegative)
        {
            // Add the coordinate planes,
            // even though we will not enforce them as constraints.
            // If we don't do this, we will report unbounded
            // in some cases when simplx reports a solution
            // when the objective function is normal
            // to an infinite face.
            // (e.g. the segVerts2d and segVerts3d examples,
            // when the objective function is 1,1,1 or -1,-1,-1).
            double minusI[][] = VecMath.identitymat(nDims);
            VecMath.sxm(minusI, -1., minusI);
            AC = (double[][])Arrays.concat(AC, minusI);
            bd = (double[])Arrays.concat(bd, VecMath.zerovec(nDims));
        }
        int nPlanes = AC.length;


        double bestDot = Double.NEGATIVE_INFINITY;
        double bestPoint[] = new double[nDims];
        int bestMultiIndex[] = new int[nDims];
        double thisPoint[] = new double[nDims]; // scratch for loop

        int multiIndex[] = new int[nDims];
        initMultiIndex(nPlanes, multiIndex);
        if (debugThisFunction >= 2) System.out.println("        ------------");
        while (incrementMultiIndex(nPlanes, multiIndex))
        {
            if (debugThisFunction >= 2) System.out.println("multiIndex" + " = " + VecMath.toString(multiIndex));
            double normals[][] = (double[][])Arrays.getMany(AC, multiIndex);
            double offsets[] = (double[])Arrays.getMany(bd, multiIndex);
            double det = VecMath.det(normals);
            if (((det) < 0 ? -(det) : (det)) < 1e-12) // XXX crappy test for degeneracy actually
            {
                if (debugThisFunction >= 2) System.out.println("        (degenerate equations, skipping)");
                continue;
            }
            VecMath.invmxv(thisPoint, normals, offsets);
            double thisDot = VecMath.dot(thisPoint, q);
            if (debugThisFunction >= 2) System.out.println("thisPoint" + " = " + VecMath.toString(thisPoint));
            if (debugThisFunction >= 2) System.out.println("thisDot" + " = " + (thisDot));
            if (thisDot > bestDot)
            {
                boolean okaySoFar = true;
                for (int iPlane = 0; (iPlane) < (nConstraintsToEnforce); ++iPlane)
                {
                    if (Arrays.indexOfUsingEqualsSymbol(normals, AC[iPlane]) != -1)
                        continue; // it's definitely on this plane
                    double foo = VecMath.dot(AC[iPlane], thisPoint) - bd[iPlane];
                    if ((iPlane < A.length ? ((foo) < 0 ? -(foo) : (foo)) : foo) > 1e-12) // if violates this == or <= constraint
                    {
                        if (debugThisFunction >= 2) System.out.println("        (violates some constraint))");
                        okaySoFar = false;
                        break;
                    }
                }
                if (okaySoFar)
                {
                    bestDot = thisDot;
                    VecMath.copyvec(bestPoint, thisPoint);
                    VecMath.copyvec(bestMultiIndex, multiIndex);
                }
            }
        }
        if (debugThisFunction >= 2) System.out.println("        ------------");
        int ret;
        if (bestDot > Double.NEGATIVE_INFINITY)
        {
            if (VecMath.normsqrd(bestPoint) >= 1e6*1e6*.99)
                ret = 1; // unbounded (hit the huge walls we added)
            else
            {
                VecMath.copyvec(x, bestPoint);
                if (y != null)
                {
                    VecMath.mxv(y, C, x);
                    VecMath.vmv(y, d, y); // y = d - C*x
                    // Exactly zero out the entries in y
                    // corresponding to the nDims planes used
                    // to compute the solution
                    for (int iDim = 0; (iDim) < (nDims); ++iDim)
                    {
                        int i = bestMultiIndex[iDim];
                        if (i < y.length)
                        {
                            do { if (!(((y[i]) < 0 ? -(y[i]) : (y[i])) < 1e-12)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1287 +"): " + "ABS(y[i]) < 1e-12" + ""); } while (false);
                            y[i] = 0.;
                        }
                        else
                        {
                            if (implicitRestrictNonNegative)
                            {
                                // it's from a non-negativity constraint we added
                                do { if (!(((x[i-y.length]) < 0 ? -(x[i-y.length]) : (x[i-y.length])) < 1e-12)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1295 +"): " + "ABS(x[i-y.length]) < 1e-12" + ""); } while (false);
                                x[i-y.length] = 0.;
                            }
                            else
                            {
                                // it's from the axis planes we added;
                                // ignore this case
                            }
                        }
                    }
                }
                ret = 0; // finite
            }
        }
        else
        {
            ret = -1; // infeasible
        }
        if (debugThisFunction >= 1) System.out.println("    Out LPSolveByBruteForce, returning " + ret);
        return ret;
    } // LPSolveByBruteForce

        // XXX unused?
        private static int choose(int n, int k)
        {
            if (n-k > k)
                k = n-k;
            if (k < 0)
                return 0;
            // n-k+1 * n-k+2 * ... * n-1 * n
            // -------------------------------
            //    1  *   2   * ... * k-1 * k
            int prod = 1;
            for (int i = 0; (i) < (k); ++i)
            {
                prod *= (n-k+i+1);
                prod /= (i+1);
            }
            return prod;
        } // choose
        private static void initMultiIndex(int n, int multiIndex[])
        {
            VecMath.fillvec(multiIndex, n);
            if (multiIndex.length > 0)
                multiIndex[0] = -1;
        }
        private static boolean incrementMultiIndex(int n, int multiIndex[])
        {
            int k = multiIndex.length;
            int ii = k-1;
            while (ii >= 0 && multiIndex[ii] >= ii + (n-k))
                ii--;
            if (ii < 0)
                return false;
            multiIndex[ii]++;
            while (++ii < k)
                multiIndex[ii] = multiIndex[ii-1]+1;
            return true;
        } // incrementMultiIndex


    /** Little test program. */
    public static void main(String args[])
    {
        if (true)
        {
            //
            // Example from the book.
            // Dummy first row and column.
            //
            double A[][] = {
                null, // dummy row for 1-based indexing
                // Objective function
                {Double.NaN, 0, 1, 1, 3, -.5}, // maximize z = x1 + x2 + 3*x3 - .5*x4
                // Less-than constraints
                {Double.NaN, 740, -1, 0, -2, 0}, // x1 + 2*x3 <= 740
                {Double.NaN, 0, 0, -2, 0, 7}, // 2*x2 - 7*x4 <= 0
                // Greater-than constraints
                {Double.NaN, .5, 0, -1, 1, -2}, // x2 - x3 + 2*x4 >= .5
                // Equality constraints
                {Double.NaN, 9,-1,-1,-1,-1}, // x1 + x2 + x3 + x4 == 9
                // Algorithm needs extra scratch row for the auxiliary objective function
                {Double.NaN, 0,0,0,0,0},
            };
            int m = A.length-3;
            int n = A[1].length-2;
            int nLE = 2;
            int nGE = 1;
            int nEQ = 1;
            do { if (!(nLE+nGE+nEQ == m)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1384 +"): " + "nLE+nGE+nEQ == m" + ""); } while (false);
            System.out.println("Input:");
            System.out.println("A" + " =\n" + VecMath.toString(A));
            System.out.println("m" + " = " + (m));
            System.out.println("n" + " = " + (n));
            System.out.println("nLE" + " = " + (nLE));
            System.out.println("nGE" + " = " + (nGE));
            System.out.println("nEQ" + " = " + (nEQ));

            int izrov[] = new int[n+1]; izrov[0] = -999;
            int iposv[] = new int[m+1]; iposv[0] = -999;

            int icase = simplx(A,
                               nLE+nGE+nEQ,
                               n,
                               nLE, nGE, nEQ,
                               izrov,
                               iposv);
            System.out.println("Output:");
            System.out.println("icase" + " = " + (icase));
            if (debugLevel >= 4)
            {
                System.out.println("A" + " =\n" + VecMath.toString(A));
            }
            if (debugLevel >= 1)
            {
                System.out.println("izrov" + " = " + VecMath.toString(izrov));
                System.out.println("iposv" + " = " + VecMath.toString(iposv));
                System.out.println("A" + " =\n" + TableauToString(A,m,n,nLE,nGE,izrov,iposv));
            }

            do { if (!(A[0] == null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1415 +"): " + "A[0] == null" + ""); } while (false);
            for (int i = 1; i < A.length; ++i)
                do { if (!(Double.isNaN(A[i][0]))) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1417 +"): " + "Double.isNaN(A[i][0])" + ""); } while (false);
            do { if (!(izrov[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1418 +"): " + "izrov[0] == -999" + ""); } while (false);
            do { if (!(iposv[0] == -999)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1419 +"): " + "iposv[0] == -999" + ""); } while (false);

            //
            // Assert the answer is right...
            //
            double expectedAnswerToTwoDecimals[] = { 0, 3.33, 4.73, .95 }; // from the book

            double answer[] = new double[n];
            for (int i = 1; i < izrov.length; ++i)
                if (izrov[i]-1 < n)
                    answer[izrov[i]-1] = 0.;
            for (int i = 1; i < iposv.length; ++i)
                if (iposv[i]-1 < n)
                    answer[iposv[i]-1] = A[i+1][1];
            System.out.println("answer" + " = " + VecMath.toString(answer));
            double answerToTwoDecimals[] = new double[n];
            for (int i = 0; (i) < (n); ++i)
            {
                if (answer[i] < 0)
                    answerToTwoDecimals[i] = -((int)(-answer[i]*100+.5+1e-12)*.01);
                else
                    answerToTwoDecimals[i] = (int)(answer[i]*100+.5+1e-12)*.01;
            }
            System.out.println("answerToTwoDecimals" + " = " + VecMath.toString(answerToTwoDecimals));
            System.out.println("expectedAnswerToTwoDecimals" + " = " + VecMath.toString(expectedAnswerToTwoDecimals));
            if (VecMath.dist(answerToTwoDecimals, expectedAnswerToTwoDecimals) > 1e-12)
            {
                System.out.println("ERROR: didn't match what book said");
                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1447 +"): " + "false" + ""); } while (false);
            }
            else
            {
                System.out.println("SUCCESS");
            }
            System.out.println("=============================================");
        }

        if (true)
        {
            System.out.println("=============================================");
            System.out.println("Finding verts of dual polyhedra...");
            //
            // See if we can locate the dual verts
            // of each regular polyhedron.
            //
            double noVerts[][] = {
            };
            double oneVert1d[][] = {
                { 1},
            };
            double segVerts1d[][] = {
                { 1},
                {-1},
            };
            double oneVert2d[][] = {
                { 1,1},
            };
            double segVerts2d[][] = {
                { 1, 1},
                {-1,-1},
            };
            double squareVerts2d[][] = {
                {-1,-1},
                {-1, 1},
                { 1,-1},
                { 1, 1},
            };
            double diamondVerts2d[][] = {
                { 1,0},
                {-1,0},
                {0, 1},
                {0,-1},
            };
            double triVerts2d[][] = {
                {1,0},
                {-.5, Math.sqrt(3.)/2},
                {-.5,-Math.sqrt(3.)/2},
            };
            double oneVert3d[][] = {
                { 1,1,1},
            };
            double segVerts3d[][] = {
                { 1, 1, 1},
                {-1,-1,-1},
            };
            double backwardsTetraMinusBackVerts[][] = {
                //{-1,-1,-1},
                {-1,1,1},
                {1,-1,1},
                {1,1,-1},
            };
            double tetraMinusFrontVerts[][] = {
                //{1,1,1},
                {1,-1,-1},
                {-1,1,-1},
                {-1,-1,1},
            };
            double tetraVerts[][] = {
                {1,1,1},
                {1,-1,-1},
                {-1,1,-1},
                {-1,-1,1},
            };
            double cubeVerts[][] = {
                {-1,-1,-1},
                { 1,-1,-1},
                {-1, 1,-1},
                { 1, 1,-1},
                {-1,-1, 1},
                { 1,-1, 1},
                {-1, 1, 1},
                { 1, 1, 1},
            };
            double octaVerts[][] = {
                {1,0,0},
                {-1,0,0},
                {0,1,0},
                {0,-1,0},
                {0,0,1},
                {0,0,-1},
            };
            double gold = (Math.sqrt(5)+1)/2;
            double icosaVerts[][] = {
                {0, 1, gold},
                {0, -1, gold},
                {0, 1, -gold},
                {0, -1, -gold},
                { 1, gold, 0},
                { 1, -gold, 0},
                {-1, gold, 0},
                {-1, -gold, 0},
                { gold, 0, 1},
                { gold, 0, -1},
                {-gold, 0, 1},
                {-gold, 0, -1},
            };
            double dodecaVerts[][] = {
                {0, 1/gold, gold},
                {0, -1/gold, gold},
                {0, 1/gold, -gold},
                {0, -1/gold, -gold},
                { 1/gold, gold, 0},
                { 1/gold, -gold, 0},
                {-1/gold, gold, 0},
                {-1/gold, -gold, 0},
                { gold, 0, 1/gold},
                { gold, 0, -1/gold},
                {-gold, 0, 1/gold},
                {-gold, 0, -1/gold},
                { 1, 1, 1},
                { 1, 1,-1},
                { 1,-1, 1},
                { 1,-1,-1},
                {-1, 1, 1},
                {-1, 1,-1},
                {-1,-1, 1},
                {-1,-1,-1},
            };
            double primalsVerts[][][] = {
                noVerts, // not sure, algorithm isn't sure either I bet
                oneVert1d, // should be finite if nonnegative, unbounded otherwise
                segVerts1d, // should be finite
                oneVert2d, // should be finite
                segVerts2d, // should be finite
                squareVerts2d, // should be finite
                diamondVerts2d, // should be finite
                triVerts2d, // should be finite
                oneVert3d, // should be finite
                segVerts3d, // should be finite
                backwardsTetraMinusBackVerts, // should be finite
                tetraMinusFrontVerts, // should be finite
                tetraVerts, // should be finite
                cubeVerts, // should be finite
                octaVerts, // should be finite
                icosaVerts, // should be finite
                dodecaVerts, // should be finite
            };
            for (int iPrimal = 0; iPrimal < primalsVerts.length; ++iPrimal)
            {
                System.out.println("=======================================================");
                System.out.println("iPrimal = "+iPrimal);
                double verts[][] = primalsVerts[iPrimal];
                int nVerts = verts.length;
                int nDims = (nVerts==0 ? 3 : verts[0].length);

                double A[][] = new double[0][nDims]; // XXX maybe should allow null?
                double b[] = new double[0]; // XXX maybe should allow null?
                double C[][] = verts;
                double d[] = new double[nVerts];

                double x[] = new double[nDims]; // solution
                double y[] = new double[nVerts]; // slack at solution
                double xByBruteForce[] = new double[nDims]; // solution
                double yByBruteForce[] = new double[nVerts]; // slack at solution

                for (int iVert = 0; iVert < nVerts; ++iVert)
                    d[iVert] = 1; // verts[iVert] dot x <= 1

                for (int iIter = 0; iIter < 2; ++iIter)
                {
                    boolean implicitRestrictNonNegative = (iIter==0?true:false); // restricted/faster then general.  restricted will often be infeasible if problem is offset in a random direction.
                    System.out.println("implicitRestrictNonNegative" + " = " + (implicitRestrictNonNegative));
                    java.util.Random generator =
                        args.length > 0 ? new java.util.Random(Integer.parseInt(args[0]))
                                        : new java.util.Random();
                    // Work around bug I'm seeing that makes
                    // the first call to nextDouble() from a seeded generator
                    // always return something very near .73 ...
                    for (int iFudge = 0; (iFudge) < (10); ++iFudge)
                        generator.nextDouble();

                    // Random offset...
                    double offset[] = new double[nDims];
                    if (true) // XXX make this a command line option?
                    {
                        for (int iDim = 0; iDim < nDims; ++iDim)
                            offset[iDim] = 2*generator.nextDouble() - 1;
                        VecMath.normalize(offset);
                        VecMath.sxv(offset, 10., offset);
                    }
                    if (false) // XXX make this a command line option?
                    {
                        VecMath.zerovec(offset);
                        offset[0] = 10;
                        // XXX should test this too! objective functions headed straight for face planes, or straight for edges, or straight for vertices
                    }

                    System.out.println("offset" + " = " + VecMath.toString(offset));


                    // So instead of C x < d,
                    // we want C (x - offset) < d
                    // i.e. C x < d + C offset
                    double offset_d[] = VecMath.mxv(C, offset);
                                        VecMath.vpv(offset_d, offset_d, d);

                    // Random objective function...
                    double q[] = new double[nDims];
                    for (int iDim = 0; iDim < nDims; ++iDim)
                        q[iDim] = 2*generator.nextDouble()-1;

                    if (false) // XXX make this a command line option?
                    {
                        VecMath.fillvec(q, -1.);
                        if (q.length > 0)
                            q[0] = -1.01; // fudge a little
                        if (q.length > 1)
                            q[1] = -1.001;
                    }

                    if (true) // XXX make this a command line option?
                    {
                        VecMath.fillvec(q, 1.);
                        if (q.length > 0)
                            q[0] = 1.01; // fudge a little
                        if (q.length > 1)
                            q[1] = 1.001;
                    }

                    int result = LPSolve(x,y,A,b,C,offset_d,q, implicitRestrictNonNegative);
                    int resultByBruteForce = LPSolveByBruteForce(xByBruteForce,yByBruteForce,A,b,C,offset_d,q, implicitRestrictNonNegative);

                    System.out.println("q" + " = " + VecMath.toString(q));
                    System.out.println("result" + " = " + (result));
                    System.out.println("x" + " = " + VecMath.toString(x));
                    System.out.println("y" + " = " + VecMath.toString(y));
                    System.out.println("resultByBruteForce" + " = " + (resultByBruteForce));
                    System.out.println("xByBruteForce" + " = " + VecMath.toString(xByBruteForce));
                    System.out.println("yByBruteForce" + " = " + VecMath.toString(yByBruteForce));
                    if (resultByBruteForce != result)
                    {
                        System.out.println("ERROR: result type didn't match!\n");
                        do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1691 +"): " + "false" + ""); } while (false);
                    }
                    else if (result == 0) // and resultByBruteForce == 0
                    {
                        // Note, one possible reason for error is if
                        // objective function is in direction of the
                        // winning face plane...
                        double dot = VecMath.dot(x,q);
                        double dotByBruteForce = VecMath.dot(xByBruteForce,q);
                        double dotError = ((dot-dotByBruteForce) < 0 ? -(dot-dotByBruteForce) : (dot-dotByBruteForce));
                        System.out.println("dot" + " = " + (dot));
                        System.out.println("dotByBruteForce" + " = " + (dotByBruteForce));
                        System.out.println("dotError" + " = " + (dotError));

                        boolean qIsAllInts = true;
                        for (int i = 0; (i) < (q.length); ++i)
                            if ((int)q[i] != q[i])
                                qIsAllInts = false;

                        double xError = VecMath.dist(x, xByBruteForce);
                        double yError = VecMath.dist(y, yByBruteForce);
                        System.out.println("xError" + " = " + (xError));
                        System.out.println("yError" + " = " + (yError));
                        System.out.println("xError+yError" + " = " + (xError+yError));
                        if (xError+yError > 1e-12)
                        {
                            if (dotError < 1e-12 && qIsAllInts)
                                System.out.println("I attribute that error to be due to the non-random objective function being normal to a face plane, since we got the dot product right");
                            else
                            {
                                System.out.println("ERROR: there was no excuse for that");
                                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1722 +"): " + "false" + ""); } while (false);
                            }
                        }
                    }
                } // for iIter
                System.out.println("SUCCESS");

                System.out.println("=======================================================");
            } // for iPrimal
            System.out.println("=============================================");
        }

        if (true)
        {
            System.out.println("=============================================");
            System.out.println("Testing L1Solve and LInfinitySolve");

            // Test L1Solve and LInfinitySolve
            // with the example from the mathematica doc...
            // http://documents.wolfram.com/v5/Built-inFunctions/AdvancedDocumentation/LinearAlgebra/7.3.html
            double A[][] ={
                {1,2},
                {5,6},
                {4.5,6},
            };
            double b[] = {5, 6, 8};

            double xInfExpected[] = {-4.4, 4.65};
            double xInf[] = new double[2];
            int resultInf = LInfinitySolve(xInf, A, b);
            System.out.println("resultInf" + " = " + (resultInf));
            System.out.println("xInf" + " = " + VecMath.toString(xInf));
            System.out.println("xInfExpected" + " = " + VecMath.toString(xInfExpected));
            do { if (!(resultInf == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1755 +"): " + "resultInf == 0" + ""); } while (false);
            if (VecMath.dist(xInf,xInfExpected) <= 1e-12)
                System.out.println("SUCCESS");
            else
            {
                System.out.println("ERROR");
                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1761 +"): " + "false" + ""); } while (false);
            }

            double x1Expected[] = {-4.5, 4.75};
            double x1[] = new double[2];
            int result1 = L1Solve(x1, A, b);
            System.out.println("result1" + " = " + (result1));
            System.out.println("x1" + " = " + VecMath.toString(x1));
            System.out.println("x1Expected" + " = " + VecMath.toString(x1Expected));
            do { if (!(result1 == 0)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1770 +"): " + "result1 == 0" + ""); } while (false);
            if (VecMath.dist(x1,x1Expected) <= 1e-12)
                System.out.println("SUCCESS");
            else
            {
                System.out.println("ERROR");
                do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/LinearProgramming.prejava"+"("+1776 +"): " + "false" + ""); } while (false);
            }

            System.out.println("=============================================");
        }
    } // main

} // class LinearProgramming
