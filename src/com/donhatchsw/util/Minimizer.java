// 2 # 1 "com/donhatchsw/util/Minimizer.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/Minimizer.prejava"
//
// Minimizer.prejava
//

package com.donhatchsw.util;

// 12 # 1 "com/donhatchsw/util/macros.h" 1
//
// macros.h
//
// 16 # 32 "com/donhatchsw/util/macros.h"
// XXX change the following to PRINTARRAY I think
// 18 # 8 "com/donhatchsw/util/Minimizer.prejava" 2

/**
 * Nelder and Mead's Downhill Simplex method for multidimensional minimization
 * (unrelated to the simplex method of linear programming),
 * from numerical recipes in C, pp. 408-412.
 */
public class Minimizer
{
    private Minimizer() {} // uninstantatable (currently).

    /**
     * Debugging level:  0 = nothing, 1 = entry and exit from major functions, 2 = detailed debugging.
     */
    public static int verboseLevel = 1;

    /** A function that takes a vector and returns a scalar. */
    public interface VectorFunction
    {
        double apply(double v[]);
    } // class VectorFunction

    /** Find the vector v such that fun(v) is minimal, starting with an initial guess, an initial delta, and a maximum allowed number of fun calls. */
    public static double[] minimize(VectorFunction fun, double initialGuess[], double initialDelta, int maxCalls, String paramNames[])
    {
        if (verboseLevel >= 1)
            System.out.println("    In minimize");

        int n = initialGuess.length;
        if (n == 0)
        {
            if (verboseLevel >= 1)
                System.out.println("    out minimize (trivially)");
            return new double[n]; // trivial, but our algorithm assumes at least one point
        }
        double simplex[][] = new double[n+1][n];
        double vals[] = new double[n+1];

        //int nTries = 2; // XXX book says to do this, but it seems to do more harm than good
        int nTries = 1;
        int I;
        for (I = 0; (I) < (nTries); ++I)
        {
            int i;
            for (i = 0; (i) < (n+1); ++i)
            {
                VecMath.copyvec(simplex[i], initialGuess);
                if (i < n)
                    simplex[i][i] += initialDelta;
                vals[i] = fun.apply(simplex[i]);
            }
            if (verboseLevel >= 1)
                System.out.println("        initial values at simplex verts: "+VecMath.toString(vals));



            // The book says ftol should be about the machine precision.
            // Let's make it exactly that.  (Give or take a factor
            // of 2 I suppose).
            // This uses the fact that the ordering of the IEEE
            // bit representation of doubles (interpreted as longs)
            // is the same as that of the doubles themselves,
            // so the next double greater than 1. can be obtained
            // by interpreting the bits as long, adding 1,
            // and then interpreting the bits as double again.
            double ftol = Double.longBitsToDouble(Double.doubleToLongBits(1.)+1) - 1.;
            System.out.println("ftol" + " = " + (ftol));

            int nCalls = amoeba(simplex, vals, ftol, fun, maxCalls, paramNames);
            //PRINT(nCalls);
            if (nCalls >= maxCalls)
            {
                if (verboseLevel >= 1)
                    System.out.println("    out minimize (maxCalls "+maxCalls+" reached)");
                return null;
            }
            initialGuess = simplex[0]; // restart, to foil false finishes XXX what about multiple false finishes?? need to keep going as long as there is any progress!! or, make the ending condition based on simplex size, not function value difference?
        }
        if (verboseLevel >= 1)
        {
            System.out.println("        achieved "+vals[0]+"");
            dump(simplex[0], paramNames);
        }
        if (verboseLevel >= 1)
            System.out.println("    out minimize");
        return simplex[0];
    } // minimize

    /**
     * return value is number of calls made to fun
     */
    private static int amoeba(double simplex[/*n+1*/][/*n*/],
                              double vals[/*n+1*/], // values on simplex XXX JAVADOC
                              double ftol,
                              VectorFunction fun,
                              int maxCalls,
                              String paramNames[])
    {
        int nextDump = 0;
        if (verboseLevel >= 1)
            System.out.println("        in amoeba");
        int n = simplex.length-1;
        int nCalls = 0;
        double psum[] = VecMath.sum(simplex);
        while (true)
        {
            if (verboseLevel >= 1)
                System.out.println(""+vals[0]+"    top of loop, nCalls = "+nCalls+"/"+maxCalls+"");
            if (verboseLevel >= 0)
            {
                if (nCalls >= nextDump)
                {
                    int iParam;
                    System.out.println("nCalls = "+nCalls+"/"+maxCalls+", params:");
                    dump(simplex[0], paramNames);
                    System.out.println("-> "+vals[0]);
                    nextDump += 1000;
                }
            }


            //
            // First we must determine which point is the highest (worst),
            // next-highest, and lowest (best), by looping over the points
            // in the simplex.
            //
            int ilo, ihi, inhi;
            {
                if (vals[0] <= vals[1])
                {
                    ilo = inhi = 0;
                    ihi = 1;
                }
                else
                {
                    ilo = inhi = 1;
                    ihi = 0;
                }
                int i;
                for (i = 2; i < n+1; ++i)
                {
                    if (vals[i] < vals[ilo])
                        ilo = i;
                    if (vals[i] > vals[ihi])
                    {
                        inhi = ihi;
                        ihi = i;
                    }
                    else if (vals[i] > vals[inhi])
                        inhi = i;
                }
            }
            if (verboseLevel >= 2)
                System.out.println("                ilo="+ilo+" ihi="+ihi+" inh="+inhi+"");

            double rtol = 2.*Math.abs(vals[ihi]-vals[ilo])/(Math.abs(vals[ihi])+Math.abs(vals[ilo])+TINY);
            // Compute the fractional range from highest to lowest
            // and return if satisfactory.
            if (rtol < ftol)
            {
                // if returning, put best point and value in slot 0.
                double dtemp, ptemp[];
                {dtemp=(vals[0]);vals[0]=(vals[ilo]);vals[ilo]=(dtemp);};
                {ptemp=(simplex[0]);simplex[0]=(simplex[ilo]);simplex[ilo]=(ptemp);};
                if (verboseLevel >= 1)
                    System.out.println("                reached tolerance ("+rtol+" < "+ftol+"");
                break;
            }
            if (nCalls >= maxCalls)
            {
                if (verboseLevel >= 1)
                    System.out.println("                max calls reached ("+nCalls+" >= "+maxCalls+"");
                break;
            }
            //
            // Begin a new iteration.
            // First extrapolate by a factor -1
            // through the face of the simplex across from the high point,
            // i.e., reflect the simplex from the high point.
            //
            if (verboseLevel >= 1)
                System.out.println("                            Reflecting through face across from high point");
            double ytry = amotry(simplex, vals, psum, fun, ihi, -1.);
            nCalls++;
            if (ytry <= vals[ilo]) // XXX <= ?
            {
                // Gives a result better than the best point,
                // so try an additional extrapolation by a factor 2.
                if (verboseLevel >= 1)
                    System.out.println("                            Better than the best point, so try additional extrapolation by a factor 2.");
                ytry = amotry(simplex, vals, psum, fun, ihi, 2.);
                nCalls++;
            }
            else if (ytry >= vals[inhi])
            {
                // The reflected point is worse than the second-highest,
                // so look for an intermediate lower point,
                // i.e. do a one-dimensional contraction.
                if (verboseLevel >= 1)
                    System.out.println("                            Worse than second-highest, so look for an intermediate lower point.");
                double ysave = vals[ihi];
                ytry = amotry(simplex, vals, psum, fun, ihi, .5);
                nCalls++;
                if (ytry >= ysave)
                {
                    // Can't seem to get rid of that high point.
                    // Better contract around the lowest (best) point.
                    if (verboseLevel >= 1)
                        System.out.println("                Can't seem to get rid of that high point.  Better contract around the lowest (best) point.");
                    int i;
                    for (i = 0; (i) < (n+1); ++i)
                    {
                        if (i != ilo)
                        {
                            VecMath.lerp(simplex[i], simplex[ilo], simplex[i], .5);
                            vals[i] = fun.apply(simplex[i]);
                        }
                    }
                    nCalls += n;
                    VecMath.sum(psum, simplex); // XXX probably a simpler function of existing
                }
            }
            else
            {
                if (verboseLevel >= 1)
                    System.out.println("                            Not better than best, but not worse than second-highest");
            }
        } // while (true)
        if (verboseLevel >= 1)
            System.out.println("        out amoeba");
        return nCalls;
    } // amoeba

    private static double amotry(double simplex[/*n+1*/][/*n*/],
                                 double vals[/*n+1*/], // values on simplex XXX JAVADOC
                                 double psum[/*n*/],
                                 VectorFunction fun,
                                 int ihi,
                                 double fac)
    {
        if (verboseLevel >= 2)
            System.out.println("                    in amotry");
        int n = simplex.length-1;
        double ptry[] = new double[n]; // XXX should avoid this allocation if possible

        double fac0 = (1.-fac)/n;
        double fac1 = fac0-fac;
        VecMath.sxvpsxv(ptry, fac0, psum, -fac1, simplex[ihi]);
        // VecMath.lerp(ptry, pavg, simplex[ihi], (fac(n+1)-1)/n);

        double ytry = fun.apply(ptry);
        if (ytry < vals[ihi])
        {
            vals[ihi] = ytry;
            VecMath.vmv(psum, psum, simplex[ihi]);
            simplex[ihi] = ptry;
            VecMath.vpv(psum, psum, simplex[ihi]);
        }
        if (verboseLevel >= 2)
            System.out.println("                    out amotry");
        return ytry;
    } // amotry


    private static void dump(double params[], String names[])
    {
        int i;
        System.out.println("{");
        for (i = 0; (i) < (params.length); ++i)
            System.out.println("    "+params[i]+",    // "+(names==null ? "" : names[i]));
        System.out.println("}");
    } // dump

    /** From the book. */
    public static final double TINY = 1.e-10; // XXX from book; see if it makes sense for double
} // class Minimizer
