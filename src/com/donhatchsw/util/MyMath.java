// 2 # 1 "com/donhatchsw/util/MyMath.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/MyMath.prejava"
// Author: Don Hatch (hatch@hadron.org)

package com.donhatchsw.util;

/**
*
* Robust implementations of hyperbolic and inverse hyperbolic trig functions,
* and expm1 and log1p,
* and hypot; all stuff that should be in the standard math library.
* <br>
* For explanations of the robust algorithms used, see:
* <a href="http://plunk.org/~hatch/rightway.php">http://plunk.org/~hatch/rightway.php</a>
*/
//#ifdef __java

public class MyMath
{
    private MyMath() {} // non-instantiatable

    /**
    * exp(x)-1, accurate even when x is small.
    */
   /* This was found in:
    * <a href="http://www.cs.berkeley.edu/~wkahan/Math128/Sumnfp.pdf">
    * http://www.cs.berkeley.edu/~wkahan/Math128/Sumnfp.pdf</a>
    * <br>
    * Achieves "nearly full working relative accuracy despite cancellation
    * no matter how tiny x may be".
    */
    public static double
    expm1(double x)
    {
        double u = Math.exp(x);
        if (u == 1.)
            return x;
        if (u-1. == -1.)
            return -1;
        return (u-1.)*x/Math.log(u);
    }

    /**
    *  log(1+x), accurate even when x is small.
    */
    /*
    *  Found in gsl (gnu scientific library),
    *  distributed under GPL version 2,
    *  but I'm sure it's from Kahan too.
    */
    public static double
    log1p(double x)
    {
        double u = 1.+x;
        return Math.log(u) - ((u-1.)-x)/u; // cancels errors with IEEE arithmetic
    }
    /* 
        XXX the following might be preferable...
 from http://www.hursley.ibm.com/majordomo/JSR-DECIMAL/archives/jsr-decimal.archive.0102/Author/article-6.html:

It is not unheard of for math libraries to deliberately add quantities
that might have very different magnitudes to see what sort of roundoff
occurs.  For example, take Kahan's log1p implementation (log1p is the
result of ln(1 + x) -- because of roundoff in a given precision,
having a separate function is more accurate than the obvious way to
compute this function):

log1p(x) {
  if ( (1.0 + x ) == 1.0)
    return x;
  else
    return log(1.0 +x)*x/(1.0 +x);
}

The ( (1.0 + x ) == 1.0) expression computes the sum of x and 1.0
simply to see of x gets rounded away.  In this case, computing the
exact value of (1.0 + x) could seriously effect the performance of
this function when it should be very fast.
    XXX and this, from
        http://kristopherjohnson.net/twiki/pub/Main/MathLibFunctions/alg.html

 * Note: Assuming log() return accurate answer, the following
 *       algorithm can be used to compute log1p(x) to within a few ULP:
 *
 *              u = 1+x;
 *              if(u==1.0) return x ; else
 *                         return log(u)*(x/(u-1.0));
 *
 *       See HP-15C Advanced Functions Handbook, p.193.
 *

    XXX whoa, and lots of other good stuff at:
http://kristopherjohnson.net/twiki/pub/Main/MathLibFunctions/alg.html
    */

    /** hyperbolic sine function */
    public static double
    sinh(double x)
    {
        // sinh(x) = (e^x - e^-x) / 2
        //         = (e^x - 1)(e^x + 1) / e^x / 2;
        //         = expm1(x) * (expm1(x)+2) / (expm1(x)+1) / 2
        double u = expm1(x);
        return .5 * u / (u+1) * (u+2); // ordered to avoid overflow when big
    }

    /** hyperbolic cosine function */
    public static double
    cosh(double x)
    {
        // cosh(x) = (e^x + e^-x) / 2
        // I don't think there are any cancellation issues
        // (though probably coshm1, below, is more useful).
        double e_x = Math.exp(x);
        return (e_x + 1./e_x) * .5;
    }

    /** cosh(x)-1, accurate even when x is small. */
    public static double
    coshm1(double x)
    {
        // cosh(x) - 1 = (e^x + e^-x) / 2 - 1
        //             = (e^x - 2 + e^-x) / 2
        //             = (e^2x - 2*e^x + 1) / e^x / 2
        //             = (e^x - 1)^2 / e^x / 2
        //             = expm1(x)^2 / (expm1(x)+1) / 2
        double u = expm1(x);
        return .5 * u / (u+1) * u; // ordered to avoid overflow when big
    }

    /** hyperbolic tangent function */
    public static double
    tanh(double x)
    {
        // tanh(x) = sinh(x) / cosh(x)
        //         = (e^x - e^-x) / (e^x + e^-x)
        //         = (e^2x - 1) / (e^2x + 1)
        //         = expm1(2*x) / (expm1(2*x) + 2)
        // That works great but overflows prematurely, so do it
        // this way instead:
        // tanh(x) = (e^2x - 1) / (e^2x + 1)
        //         = (e^x - 1)(e^x + 1) / ((e^x - 1)(e^x + 1) + 2)
        //         = expm1(x)(expm1(x)+1) / (expm1(x)(expm1(x)+2) + 2)
        double u = expm1(x);
        return u / (u*(u+2.)+2.) * (u+2.); // ordered to avoid overflow when big
        // XXX oops, that doesn't avoid overflow
        // XXX since u*(u+2) can overflow... can we find another formulation?
    }


    /** inverse hyperboiic sine function */
    public static double
    asinh(double x)
    {
        // asinh(x) = log(x + sqrt(x^2 + 1))
        //          = log1p(x + sqrt(x^2 + 1) - 1)
        //          = log1p(x + (sqrt(x^2+1)-1)*(sqrt(x^2+1)+1)/(sqrt(x^2+1)+1))
        //          = log1p(x + (x^2+1 - 1)/(sqrt(x^2+1)+1))
        //          = log1p(x + x^2 / (sqrt(x^2+1)+1))
        //          = log1p(x * (1 + x / (sqrt(x^2+1)+1) ))
        return log1p(x * (1. + x / (Math.sqrt(x*x+1.)+1.)));
    }

    /** inverse hyperbolic cosine function */
    public static double
    acosh(double x)
    {
        // Only defined for x >= 1.

        // acosh(x) = log(x + sqrt(x^2 - 1))
        // Use the formula given by Kahan in
        // "Branch Cuts for Complex Elementary Functions",
        // as quoted by Cleve Moler in:
        // http://www.mathworks.com/company/newsletter/clevescorner/sum98cleve.shtml
        return 2 * Math.log(Math.sqrt((x+1)*.5) + Math.sqrt((x-1)*.5));
    }

    /** inverse hyperbolic tangent function */
    public static double
    atanh(double x)
    {
        // Only defined for x < 1.

        // atanh(x) = log((1+x)/(1-x))/2
        //          = log((1 - x + 2x) / (1-x)) / 2
        //          = log(1 + 2x/(1-x)) / 2
        //          = log1p(2x/(1-x)) / 2
        return .5 * log1p(2.*x/(1.-x));
    }

    /** 1-cos(x), doesn't lose accuracy for small x */
    public static double
    cosf1(double x)
    {
        double sinHalfX = Math.sin(.5*x);
        return 2.*sinHalfX*sinHalfX;
    }
    /** acos(1-x), doesn't lose accuracy for small x */
    public static double
    acos1m(double x)
    {
        return 2.*Math.asin(Math.sqrt(.5*x));
    }

    /** sin(x)/x, but stable when small */
    public static double
    sin_over_x(double x)
    {
        //
        // It's 1 - x^2/3! + x^4/5! - ...
        // so if |x| is so small that 1-x^2/6 is indistinguishable from 1,
        // then the result will be indistinguishable from 1 too.
        //
        if (1. - x*x*(1/6.) == 1.)
        {
            //System.out.println("Ha! sin(x)/x("+x+") returning 1. instead of "+Math.sin(x)+"/"+x);
            return 1.;
        }
        else
            return Math.sin(x)/x;
    }

    /** asin(x)/x, but stable when small */
    public static double
    asin_over_x(double x)
    {
        // x + 1/6*x^3 + 3/40*x^5 + 5/112*x^7 + 35/1152*x^9 + 63/2816*x^11 + 231/13312*x^13 + ...
        if (1. + x*x*(1/3.) == 1.)
            return 1.;
        else
            return Math.asin(x)/x;
    }


    /** (1-cos(x))/x, but stable when small */
    public static double
    cosf1_over_x(double x)
    {
        //
        // It's x/2! - x^3/4! + x^5/6! - x^7/8! + ...
        // So if x is so small that x/2 - x^3/24
        // is indistinguishable from x/2,
        // then the result will be indistinguishable from .5*x.
        // This is pretty much the same as .5 - x^2/24
        // being indistinguishable from .5,
        // but maybe off by as much as 1 in the least-significant bit
        // (if x is just on the wrong side of a power of 2, I think)
        // so be conservative and test whether
        // .5 - x^2/12 is indistinguishable from .5 instead.
        // This is exactly the same as whether 1 - x^2/6
        // is indistingishable from 1.
        //
        if (1. - x*x*(1/6.) == 1.)
        {
            //System.out.println("Ha! (1-cos(x))/x("+x+") returning "+(.5*x)+"instead of "+cosf1(x)+"/"+x+" = "+(cosf1(x) / x)+"");
            return .5*x;
        }
        else
            return cosf1(x) / x;
    }

    /** sqrt(x*x + y*y) but overflow/underflow proof. */
    public static double hypot(double x, double y)
    {
        x = Math.abs(x);
        y = Math.abs(y);
        double min, max;
        if (x < y)
        {
            min = x;
            max = y;
        }
        else
        {
            min = y;
            max = x;
        }
        if (min == 0.)
            return max;
        double min_max = min/max;
        return max * Math.sqrt(1. + min_max * min_max);
    }
} // class MyMath

/*
#endif // __java

#if __cplusplus
    //
    // C++ program, for comparison to make sure I got it right
    //
    #include <math.h>
    #include <stdio.h>
    #define print(x) printf("%s = %.17g\n", #x, x)
    int main()
    {
        print(sinh(-1.5));
        print(sinh(-.5));
        print(sinh(.5));
        print(sinh(1.5));
        print(cosh(-1.5));
        print(cosh(-.5));
        print(cosh(.5));
        print(cosh(1.5));
        print(tanh(-1.5));
        print(tanh(-.5));
        print(tanh(.5));
        print(tanh(1.5));
        print(asinh(-1.5));
        print(asinh(-.5));
        print(asinh(.5));
        print(asinh(1.5));
        print(acosh(-1.5));
        print(acosh(-.5));
        print(acosh(.5));
        print(acosh(1.5));
        print(atanh(-1.5));
        print(atanh(-.5));
        print(atanh(.5));
        print(atanh(1.5));
    }
#endif // __cplusplus
*/
