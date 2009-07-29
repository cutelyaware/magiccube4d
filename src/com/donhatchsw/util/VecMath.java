// 2 # 1 "com/donhatchsw/util/VecMath.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/VecMath.prejava"
/*
* Copyright (c) 2005 Don Hatch Software
*/
//
// VecMath.prejava
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
// 24 # 14 "com/donhatchsw/util/VecMath.prejava" 2



/**
 * Some common operations on vectors and matrices,
 * using an array representation.
 * <p>
 * Most of the vector and matrix arithmetic operations
 * have two forms: a fast version in which the caller supplies
 * the result vector or matrix, and a more convenient but slower version
 * which returns a newly allocated result.
 */

/* not in javadoc, for now */
/*
 * XXX JAVADOC there are some parameter comments that should be converted to javadoc
 */
public final class VecMath
{
    private VecMath() {} // uninstantiatable


    /** vector plus vector */
    public static void vpv(double result[], double v0[], double v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + v1[i];
    }
    /** vector plus vector */
    public static void vpv(float result[], float v0[], float v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + v1[i];
    }
    /** vector plus vector */
    public static void vpv(int result[], int v0[], int v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + v1[i];
    }
    /** vector minus vector */
    public static void vmv(double result[], double v0[], double v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] - v1[i];
    }
    /** vector minus vector */
    public static void vmv(float result[], float v0[], float v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] - v1[i];
    }
    /** vector minus vector */
    public static void vmv(int result[], int v0[], int v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] - v1[i];
    }

    /** vector times scalar */
    public static void vxs(double result[], double v[], double s)
    {
        for (int i = (v.length)-1; (i) >= 0; --i)
            result[i] = v[i] * s;
    }
    /** vector times scalar */
    public static void vxs(float result[], float v[], float s)
    {
        for (int i = (v.length)-1; (i) >= 0; --i)
            result[i] = v[i] * s;
    }

    /** matrix times scalar */
    public static void mxs(double result[][], double M[][], double s)
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (result[i] != null)
                vxs(result[i], M[i], s);
    }
    /** matrix times scalar */
    public static void mxs(float result[][], float M[][], float s)
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (result[i] != null)
                vxs(result[i], M[i], s);
    }
    /** matrix times scalar */
    public static void mxs(double result[][][], double M[][][], double s)
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            mxs(result[i], M[i], s);
    }
    /** matrix times scalar */
    public static void mxs(double result[][][][], double M[][][][], double s)
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            mxs(result[i], M[i], s);
    }
    /** scalar times matrix */
    public static void sxm(double result[][], double s, double M[][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (result[i] != null)
                sxv(result[i], s, M[i]);
    }
    /** scalar times matrix */
    public static void sxm(double result[][][], double s, double M[][][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            sxm(result[i], s, M[i]);
    }
    /** scalar times matrix */
    public static void sxm(double result[][][][], double s, double M[][][][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            sxm(result[i], s, M[i]);
    }

    /** scalar times vector */
    public static void sxv(double result[], double s, double v[])
    {
        for (int i = (v.length)-1; (i) >= 0; --i)
            result[i] = s * v[i];
    }

    /** scalar times vector */
    public static void sxv(int result[], int s, int v[])
    {
        for (int i = (v.length)-1; (i) >= 0; --i)
            result[i] = s * v[i];
    }

    /** perp dot */
    public static void xv2(double result[/*2*/], double v[/*2*/])
    {
        result[0] = -v[1];
        result[1] = v[0];
    }
    /** determinant of matrix having the two vectors as rows */
    public static double vxv2(double v[/*2*/], double w[/*2*/])
    {
        return v[0]*w[1] - v[1]*w[0];
    }
    /** determinant of matrix having the two vectors as rows */
    public static float vxv2(float v[/*2*/], float w[/*2*/])
    {
        return v[0]*w[1] - v[1]*w[0];
    }
    /** 3-dimensional cross product */
    public static void vxv3(double v0[/*3*/], final double v1[/*3*/], final double v2[/*3*/])
    {
        v0[0] = v1[1]*v2[2] - v1[2]*v2[1];
        v0[1] = v1[2]*v2[0] - v1[0]*v2[2];
        v0[2] = v1[0]*v2[1] - v1[1]*v2[0];
    }
    /** determinant of matrix having the three vectors as rows */
    public static double vxvxv3(double v0[/*3*/], double v1[/*3*/], double v2[/*3*/])
    {
        return v0[0] * (v1[1]*v2[2] - v1[2]*v2[1])
             + v0[1] * (v1[2]*v2[0] - v1[0]*v2[2])
             + v0[2] * (v1[0]*v2[1] - v1[1]*v2[0]);
    }
    /** determinant of matrix having the three vectors as rows */
    public static float vxvxv3(float v0[/*3*/], float v1[/*3*/], float v2[/*3*/])
    {
        return v0[0] * (v1[1]*v2[2] - v1[2]*v2[1])
             + v0[1] * (v1[2]*v2[0] - v1[0]*v2[2])
             + v0[2] * (v1[0]*v2[1] - v1[1]*v2[0]);
    }
    /** n-dimensional cross product of the first n-1 n-dimensional vectors in the matrix */
    public static void crossprod(double result[/*dim*/], double vectors[/*dim-1*/][/*dim*/])
    {
        int dim = result.length;
        if (dim == 0)
            return; // not a lot of options here, don't think too hard about it
        do { if (!(vectors.length >= dim-1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+189 +"): " + "vectors.length >= dim-1" + ""); } while (false);

        if (true)
        {
            // optimize for small cases...
            switch(dim)
            {
                case 1:
                    result[0] = 1.;
                    return;
                case 2:
                    xv2(result, vectors[0]);
                    return;
                case 3:
                    // 3d cross product
                    vxv3(result, vectors[0], vectors[1]);
                    return;
            }
        }
        else
        {
            if (dim == 1 || dim == 2 || dim == 3)
                System.err.println("WARNING: doing slow crossprod for testing, dim = "+dim+"");
        }

        {
            double cofactorMatrix[][] = new double[dim-1][dim-1];
            int sign = ((dim-1) % 2 == 0 ? 1 : -1);
            for (int i = 0; (i) < (dim); ++i)
            {
                // cofactor matrix is vectors matrix with column i deleted...
                {
                    for (int j = 0; (j) < (dim-1); ++j)
                    for (int k = 0; (k) < (dim-1); ++k)
                        cofactorMatrix[j][k] = vectors[j][k>=i?k+1:k];
                }
                result[i] = detDestructive(cofactorMatrix) * sign;
                sign = -sign;
            }
        }
    } // crossprod


    /** vector plus (scalar times vector) */
    public static void vpsxv(double result[], double v0[],
                                              double s1, double v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + s1*v1[i];
    }

    /** vector plus (scalar times vector) */
    public static void vpsxv(float result[], float v0[],
                                             float s1, float v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + s1*v1[i];
    }

    /** vector plus (scalar times vector) */
    public static void vpsxv(int result[], int v0[],
                                           int s1, int v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + s1*v1[i];
    }

    /** vector plus vector minus vector */
    public static void vpvmv(double result[], double v0[], double v1[], double v2[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = v0[i] + (v1[i] - v2[i]);
    }

    /** (scalar times vector) plus (scalar times vector) */
    public static void sxvpsxv(double result[], double s0, double v0[],
                                                double s1, double v1[])
    {
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result[i] = s0*v0[i] + s1*v1[i];
    }

    /** matrix plus matrix */
    public static void mpm(Object result, Object M0, Object M1)
    {
        if (result instanceof int[])
            vpv((int[])result, (int[])M0, (int[])M1);
        else if (result instanceof double[])
            vpv((double[])result, (double[])M0, (double[])M1);
        else
        {
            Object resultArray[] = (Object[])result;
            Object M0Array[] = (Object[])M0;
            Object M1Array[] = (Object[])M1;
            for (int i = (resultArray.length)-1; (i) >= 0; --i)
                mpm(resultArray[i], M0Array[i], M1Array[i]);
        }
    } // mpm

    /** matrix minus matrix */
    public static void mmm(Object result, Object M0, Object M1)
    {
        if (result instanceof int[])
            vmv((int[])result, (int[])M0, (int[])M1);
        else if (result instanceof double[])
            vmv((double[])result, (double[])M0, (double[])M1);
        else
        {
            Object resultArray[] = (Object[])result;
            Object M0Array[] = (Object[])M0;
            Object M1Array[] = (Object[])M1;
            for (int i = (resultArray.length)-1; (i) >= 0; --i)
                mmm(resultArray[i], M0Array[i], M1Array[i]);
        }
    } // mmm

    /** add v to every row of M. */
    public static void vpm(double result[][], double v[], double M[][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (M[i] != null)
                vpv(result[i], M[i], v);
    }
    /** add v to every row of M. */
    public static void vpm(int result[][], int v[], int M[][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (M[i] != null)
                vpv(result[i], M[i], v);
    }

    // XXX this hasn't been tested
    /** add v to every row of M. M can be double[][], or double[][][], ... */
    public static void vpm(Object result, double v[], Object m)
    {
        Object resultFlat = Arrays.flatten(result, 0, Arrays.getDim(result)-1);
        Object mFlat = Arrays.flatten(result, 0, Arrays.getDim(result)-1);
        if (resultFlat instanceof double[][])
            vpm((double[][])resultFlat, v, (double[][])mFlat);
        else if (resultFlat instanceof int[][])
            vpm((int[][])resultFlat, v, (int[][])mFlat);
        else
            do { if (!(false)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+331 +"): " + "false" + ""); } while (false);
    }

    /** subtract v from every row of M */
    public static void mmv(int result[][], int M[][], int v[])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (M[i] != null)
                vmv(result[i], M[i], v);
    }
    /** subtract v from every row of M */
    public static void mmv(double result[][], double M[][], double v[])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
            if (M[i] != null)
                vmv(result[i], M[i], v);
    }
    /** subtract v from every row of M. M can be double[][], or double[][][], ... */
    public static void mmv(Object result, Object m, double v[])
    {
        double resultFlat[][] = (double[][])Arrays.flatten(result, 0, Arrays.getDim(result)-1);
        double mFlat[][] = (double[][])Arrays.flatten(m, 0, Arrays.getDim(m)-1);
        mmv(resultFlat, mFlat, v);
    }

    /** ||b-a||^2 ||c-a||^2 - (b-a)dot(c-a) */
    public static double sqrdTwiceTriangleArea(double a[],
                                               double b[],
                                               double c[])
    {
        double ab_dot_ab = 0.;
        double ab_dot_ac = 0.;
        double ac_dot_ac = 0.;
        for (int i = (a.length)-1; (i) >= 0; --i)
        {
            double ai = a[i];
            double ab = b[i] - ai;
            double ac = c[i] - ai;
            ab_dot_ab += ab*ab;
            ab_dot_ac += ab*ac;
            ac_dot_ac += ac*ac;
        }
        return ab_dot_ab*ac_dot_ac - ((ab_dot_ac)*(ab_dot_ac));
    } // sqrdTwiceTriangleArea



    /** linear interpolation between vectors */
    public static void lerp(double result[], double p0[], double p1[], double t)
    {
        for (int i = (p0.length)-1; (i) >= 0; --i)
            result[i] = p0[i] + t*(p1[i]-p0[i]);
    }
    /** barycentric interpolation between vectors */
    public static void bary(double result[],
                            double p0[],
                            double p1[], double t1,
                            double p2[], double t2)
    {
        for (int i = (p0.length)-1; (i) >= 0; --i)
            result[i] = (1.-t1-t2)*p0[i] + t1*p1[i] + t2*p2[i];
    }
    /** cubic interpolation, given boundary positions and velocities */
    public static void cerp(double result[],
                            double p0[], double p1[],
                            double v0[], double v1[],
                            double t)
    {
        /*
           We want a matrix M defining:
                p(t) = [1 t t^2 t^3] * M * [   p0   ]
                                           [   p1   ]
                                           [   v0   ]
                                           [   v1   ]
          
                v(t) = [0 1 2t 3t^2] * M * [   p0   ]
                                           [   p1   ]
                                           [   v0   ]
                                           [   v1   ]
           Satisfying boundary conditions:
                p(t0) = p0
                p(t1) = p1
                v(t0) = v0
                v(t1) = v1
           In other words,
                    [   p0   ] = [1 t0    t0^2    t0^3] * M * [   p0   ]
                    [   p1   ]   [1 t1    t1^2    t1^3]       [   p1   ]
                    [   v0   ]   [0 1   2 t0    3 t0^2]       [   v0   ]
                    [   v1   ]   [0 1   2 t1    3 t1^2]       [   v1   ]
           So M is the inverse of that matrix of T's.
           In particular, we have t0=0 and t1=1,
           so M is simply the inverse of:
                    [1 0 0 0]
                    [1 1 1 1]
                    [0 1 0 0]
                    [0 1 2 3]
           which is, according to octave,
               octave:1> inv([1 0 0 0 ; 1 1 1 1 ; 0 1 0 0 ; 0 1 2 3])
                  M = [  1   0   0   0 ]
                      [  0   0   1   0 ]
                      [ -3   3  -2  -1 ]
                      [  2  -2   1   1 ]
           So, given t,
           the the coefficient matrix for p0,p1,v0,v1
           is then [1 t t^2 t^3] * M.
        */

        double t2 = t*t;
        double coeff_p0 = (2*t - 3)*t2 + 1;
        double coeff_p1 = (-2*t + 3)*t2;
        double coeff_v0 = ((t - 2)*t + 1)*t;
        double coeff_v1 = (t - 1)*t2;

        if (false) // linear
        {
            coeff_p0 = (1-t);
            coeff_p1 = t;
            coeff_v0 = 0;
            coeff_v1 = 0;
        }

        sxv(result, coeff_p0, p0);
        vpsxv(result, result, coeff_p1, p1);
        vpsxv(result, result, coeff_v0, v0);
        vpsxv(result, result, coeff_v1, v1);
    } // cerp

    /**
     * quintic interpolation, given boundary positions, velocities, and
     * accelerations.
     */
    public static void interp5(double result[],
                               double p0[], double p1[],
                               double v0[], double v1[],
                               double a0[], double a1[],
                               double t)
    {
        /*
           Same logic as for cerp.
           M is the inverse of:
                [1  0  0  0  0  0]
                [1  1  1  1  1  1]
                [0  1  0  0  0  0]
                [0  1  2  3  4  5]
                [0  0  2  0  0  0]
                [0  0  2  6 12 20]
           So M is:
                [  1   0  0  0  0    0]
                [  0   0  1  0  0    0]
                [  0   0  0  0  .5    0]
                [-10  10 -6 -4 -1.5 .5]
                [ 15 -15  8  7  1.5 -1]
                [ -6   6 -3 -3  -.5 .5]
        */
        // XXX inefficient
        double t2 = t*t, t3 = t2*t, t4 = t3*t, t5 = t4*t;
        double tVec[] = {1,t,t2,t3,t4,t5};
        final double M[][] = {
                { 1, 0, 0, 0, 0 , 0},
                { 0, 0, 1, 0, 0 , 0},
                { 0, 0, 0, 0, .5 , 0},
                {-10, 10, -6, -4, -1.5, .5},
                { 15, -15, 8, 7, 1.5, -1},
                { -6, 6, -3, -3, -.5 , .5},
        };
        double pva[][] = {p0,p1,v0,v1,a0,a1};
        // result = tVec * M * pva
        // do it from left to right, to avoid full matrix multiply
        double coeffs[] = vxm(tVec, M); // XXX more allocation-- argh!
        vxm(result, coeffs, pva);
/*
PRINTMAT(M);
// make sure t=0 gives out p0 and t=1 give out p1...
PRINTVEC(p0);
PRINTVEC(vxmxm(new double[]{1,0,0,0,0,0},M,pva));
PRINTVEC(p1);
PRINTVEC(vxmxm(new double[]{1,1,1,1,1,1},M,pva));
*/
/*
PRINTMAT(M);
PRINTVEC(v0);
PRINTVEC(vxmxm(new double[]{0,1,0,0,0,0},M,pva));
PRINTVEC(v1);
PRINTVEC(vxmxm(new double[]{0,1,2,3,4,5},M,pva));
*/

        // XXX also, for fixed t, tVec*M is same for all pva; could precompute
        // XXX note, M*pva is same for all t on path; could precompute.
    } // interp5



    /** copy vector */
    public static void copyvec(boolean result[], boolean v[])
    {
        for (int i = (((result.length)<=(v.length)?(result.length):(v.length)))-1; (i) >= 0; --i)
            result[i] = v[i];
    }

    /** copy vector */
    public static void copyvec(int result[], int v[])
    {
        for (int i = (((result.length)<=(v.length)?(result.length):(v.length)))-1; (i) >= 0; --i)
            result[i] = v[i];
    }

    /** copy vector */
    public static void copyvec(double result[], double v[])
    {
        for (int i = (((result.length)<=(v.length)?(result.length):(v.length)))-1; (i) >= 0; --i)
            result[i] = v[i];
    }

    /** copy vector */
    public static void copyvec(float result[], float v[])
    {
        for (int i = (((result.length)<=(v.length)?(result.length):(v.length)))-1; (i) >= 0; --i)
            result[i] = v[i];
    }

    /** set column of a matrix */
    public static void setcolumn(double M[][], int iCol, double v[])
    {
        for (int iRow = (((M.length)<=(v.length)?(M.length):(v.length)))-1; (iRow) >= 0; --iRow)
            M[iRow][iCol] = v[iRow];
    }

    /** get column of a matrix */
    public static void getcolumn(double v[], double M[][], int iCol)
    {
        for (int iRow = (((v.length)<=(M.length)?(v.length):(M.length)))-1; (iRow) >= 0; --iRow)
            v[iRow] = M[iRow][iCol];
    }

    /** fill vector with a constant scalar */
    public static void fillvec(double result[], double s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = s;
    }

    /** fill vector with a constant scalar */
    public static void fillvec(int result[], int s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = s;
    }

    /** fill vector with a constant scalar */
    public static void fillvec(boolean result[], boolean s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = s;
    }

    /** fill matrix with a constant scalar */
    public static void fillmat(double result[][], double s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            fillvec(result[i], s);
    }

    /** fill matrix with a constant scalar */
    public static void fillmat(int result[][], int s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            fillvec(result[i], s);
    }

    /** fill matrix with a constant scalar */
    public static void fillmat(boolean result[][], boolean s)
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            fillvec(result[i], s);
    }

    /** fill matrix with a constant scalar */
    public static void copymat(double result[][], double m[][])
    {
        for (int i = (((result.length)<=(m.length)?(result.length):(m.length)))-1; (i) >= 0; --i)
            for (int j = (((result[i].length)<=(m[i].length)?(result[i].length):(m[i].length)))-1; (j) >= 0; --j)
                result[i][j] = m[i][j];
    }

    /** zero a matrix */
    public static void zerovec(double result[])
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = (double)0;
    }
    // XXX duplicate code
    /** zero a matrix */
    public static void zerovec(float result[])
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = (float)0;
    }
    // XXX duplicate code
    /** zero a matrix */
    public static void zerovec(int result[])
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            result[i] = (int)0;
    }
    /** zero a matrix */
    public static void zeromat(double result[][])
    {
        for (int i = (result.length)-1; (i) >= 0; --i)
            zerovec(result[i]);
    }

    /** vector dot product */
    public static double dot(double v0[], double v1[])
    {
        double result = (double)0;
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result += v0[i]*v1[i];
        return result;
    }
    /** vector dot product */
    public static float dot(float v0[], float v1[])
    {
        float result = (float)0;
        for (int i = (v0.length)-1; (i) >= 0; --i)
            result += v0[i]*v1[i];
        return result;
    }

    // XXX duplicate code below
    /** row vector times matrix */
    public static void vxm(double result[], double v[], double m[][])
    {
        do { if (!(result != v)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+663 +"): " + "result != v" + ""); } while (false);
        zerovec(result);
        int i, vLen = v.length;
        for (i = 0; (i) < (vLen); ++i)
            vpsxv(result, result, v[i], m[i]);
        // if v is short, pad it with 1's
        for (; i < m.length; ++i)
            vpv(result, result, m[i]);
    } // vxm double

    // XXX duplicate code above
    /** row vector times matrix */
    public static void vxm(float result[], float v[], float m[][])
    {
        do { if (!(result != v)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+677 +"): " + "result != v" + ""); } while (false);
        int vLen = v.length;
        zerovec(result);
        int i, j;
        for (i = 0; (i) < (vLen); ++i)
            vpsxv(result, result, v[i], m[i]);
        // if v is short, pad it with 1's
        for (; i < m.length; ++i)
            vpv(result, result, m[i]);
    } // vxm float

    // XXX duplicate code above
    /** row vector times matrix */
    public static void vxm(int result[], int v[], int m[][])
    {
        do { if (!(result != v)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+692 +"): " + "result != v" + ""); } while (false);
        int vLen = v.length;
        zerovec(result);
        int i, j;
        for (i = 0; (i) < (vLen); ++i)
            vpsxv(result, result, v[i], m[i]);
        // if v is short, pad it with 1's
        for (; i < m.length; ++i)
            vpv(result, result, m[i]);
    } // vxm int

    // XXX BLEAH! I have a fundamental contradiction here...
    // XXX I'm trying to overload mxm so that it can mean
    // XXX vectors-times-xform
    // XXX or xform-times-xform!
    // XXX Say we are multiplying 4x3 times 4x3;
    // XXX in the former case we pad the left one with 1 1 1 1;
    // XXX in the latter case we pad the left one with 0 0 0 1 !!!
    // XXX Need two separate functions!
    /** matrix times matrix */
    public static void mxm(double result[/*n*/][/*m*/],
                           double m0[/*n*/][/*dotLength*/],
                           double m1[/*dotLength*/][/*m*/])
    {


        if (((m0).length==0 ? 0 : (m0)[0].length) == m1.length)
        {
            do { if (!(result != m0 && result != m1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+720 +"): " + "result != m0 && result != m1" + ""); } while (false);
            for (int i = (result.length)-1; (i) >= 0; --i) // == m0.length
                vxm(result[i], m0[i], m1);
        }
        else
        {
            // XXX hack-- fix it up reasonably when I'm awake

            if ((m0.length == m0[0].length || m0.length == m0[0].length+1)
             && m1.length == m0[0].length+1
             && m1[0].length == m0[0].length
             && result.length == m0[0].length+1
             && result[0].length == m0[0].length)
            {
                // pad it out with 0's on the sides and 1 in the corner
                double M0[][] = identitymat(m1.length);
                for (int i = (m0.length)-1; (i) >= 0; --i)
                for (int j = (m0[i].length)-1; (j) >= 0; --j)
                    M0[i][j] = m0[i][j];
                mxm(result, M0, m1);
            }
            else
            {
             System.out.println("m0.length" + " = " + (m0.length));
             System.out.println("ROWLENGTH(m0)" + " = " + (((m0).length==0 ? 0 : (m0)[0].length)));
             System.out.println("m1.length" + " = " + (m1.length));
             System.out.println("ROWLENGTH(m1)" + " = " + (((m1).length==0 ? 0 : (m1)[0].length)));
             System.out.println("(m0.length == m0[0].length || m0.length == m0[0].length+1)" + " = " + ((m0.length == m0[0].length || m0.length == m0[0].length+1)));
             System.out.println("m1.length == m0[0].length+1" + " = " + (m1.length == m0[0].length+1));
             System.out.println("m1[0].length == m0[0].length" + " = " + (m1[0].length == m0[0].length));
             System.out.println("result.length == m0[0].length+1" + " = " + (result.length == m0[0].length+1));
             System.out.println("result[0].length == m0[0].length" + " = " + (result[0].length == m0[0].length));
                do { if (!(false)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+752 +"): " + "false" + ""); } while (false);
            }
        }
// 766 # 765 "com/donhatchsw/util/VecMath.prejava"
    } // mxm

    // XXX duplicate code above
    /** matrix times matrix */
    public static void mxm(float result[/*n*/][/*m*/],
                           float m0[/*n*/][/*dotLength*/],
                           float m1[/*dotLength*/][/*m*/])
    {
        do { if (!(result != m0 && result != m1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+773 +"): " + "result != m0 && result != m1" + ""); } while (false);
        for (int i = (result.length)-1; (i) >= 0; --i) // == m0.length
            vxm(result[i], m0[i], m1);
    } // mxm

    // XXX duplicate code above
    /** matrix times matrix */
    public static void mxm(int result[/*n*/][/*m*/],
                           int m0[/*n*/][/*dotLength*/],
                           int m1[/*dotLength*/][/*m*/])
    {
        do { if (!(result != m0 && result != m1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+784 +"): " + "result != m0 && result != m1" + ""); } while (false);
        for (int i = (result.length)-1; (i) >= 0; --i) // == m0.length
            vxm(result[i], m0[i], m1);
    } // mxm

    /** matrix times column vector */
    public static void mxv(double result[], double m[][], double v[])
    {
        do { if (!(result != v)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+792 +"): " + "result != v" + ""); } while (false);
        int nRows = m.length;
        if (nRows == 0)
            return;
        int nCols = v.length; // = m[0].length
        int i, j;
        // XXX convert to FORIDOWN if appropriate; I didn't think about it for too long
        for (i = 0; (i) < (nRows); ++i)
        {
            double m_i[] = m[i];
            double sum = 0.;
            for (j = 0; (j) < (nCols); ++j)
            {
                sum += m_i[j] * v[j];
            }
            result[i] = sum;
        }
    }

    private static void twoNormallyDistributedRandomNumbers(double result[/*2*/])
    {
        double r0, r1, r;
        do {
            r0 = 2.*Math.random() - 1.;
            r1 = 2.*Math.random() - 1.;
            r = r0*r0 + r1*r1;
        } while (r > 1. || r == 0.);
        r = Math.sqrt(-2*Math.log(r)/r);
        r0 *= r;
        r1 *= r;
        result[0] = r0;
        result[1] = r1;
    } // private twoNormallyDistributedRandomNumbers

    /** return a random vector with length <= 1. XXX currently broken */
    /* XXX this can take forever if v.length is big! like, > 7 */
    public static void random(double v[])
    {
        if (false) // really slow way if n >= 7 or so...
        {
            do
                for (int i = (v.length)-1; (i) >= 0; --i)
                    v[i] = (2.*Math.random() - 1.);
            while (normsqrd(v) > 1);
        }
        else
        {
            // From Bob Jenkins who got the idea from Steve Rayhawk...
            // http://burtleburtle.net/bob/rand/unitvec.html
            // XXX WOOPS! this gives a random vector *on* the sphere,
            // not inside it!
            double twoNormalRandoms[] = new double[2];
            for (int i = 0; (i) < (v.length); ++i)
            {
                int which = i%2;
                if (which == 0)
                    twoNormallyDistributedRandomNumbers(twoNormalRandoms);
                v[i] = twoNormalRandoms[which];
            }
            // XXX okay we are screwed... at this point,
            // XXX do we just return a unit vector?
            // XXX or return something with uniformly distributed
            // XXX angle but gaussian magnitude?
            if (false)
            {
                // unit vector
                normalize(v, v);
            }
            else
            {
                // gaussian magnitude-- do nothing
            }
        }
    }
    /** return a random vector with length <= 1. XXX currently broken */
    public static void random(double v[], java.util.Random generator)
    {
        do
            for (int i = (v.length)-1; (i) >= 0; --i)
                v[i] = (2.*generator.nextDouble() - 1.);
        while (normsqrd(v) > 1);
    }

    /** vector distance squared */
    public static double distsqrd(double v0[], double v1[])
    {
        double result = (float)0;
        for (int i = (v0.length)-1; (i) >= 0; --i) // == v1.length
        {
            double diff = v1[i]-v0[i];
            result += diff*diff;
        }
        return result;
    }
    /** vector distance squared */
    public static float distsqrd(float v0[], float v1[])
    {
        float result = (float)0;
        for (int i = (v0.length)-1; (i) >= 0; --i) // == v1.length
        {
            float diff = v1[i]-v0[i];
            result += diff*diff;
        }
        return result;
    }
    /** vector distance */
    public static double dist(double v0[], double v1[])
    {
        return Math.sqrt(distsqrd(v0, v1));
    }

    /** vector norm (length) squared */
    public static double normsqrd(double v[])
    {
        return dot(v,v);
    }
    /** vector norm (length) squared */
    public static float normsqrd(float v[])
    {
        return dot(v,v);
    }
    /** vector norm (length) */
    public static double norm(double v[])
    {
        return Math.sqrt(dot(v,v));
    }
    /** vector norm (length) */
    public static float norm(float v[])
    {
        return (float)Math.sqrt(dot(v,v)); // XXX is there a sqrtf?
    }

    /** returns index of least element */
    public static int mini(double v[])
    {
        int mini = -1;
        double min = Double.POSITIVE_INFINITY;
        for (int i = (v.length)-1; (i) >= 0; --i)
        {
            if (v[i] < min)
            {
                min = v[i];
                mini = i;
            }
        }
        return mini;
    }
    /** returns index of greatest element */
    public static int maxi(double v[])
    {
        int maxi = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = (v.length)-1; (i) >= 0; --i)
        {
            if (v[i] > max)
            {
                max = v[i];
                maxi = i;
            }
        }
        return maxi;
    }

    /** returns minimum element */
    public static double min(double v[])
    {
        double min = Double.POSITIVE_INFINITY;
        for (int i = (v.length)-1; (i) >= 0; --i)
            min = Math.min(min, v[i]);
        return min;
    }
    /** returns maximum element */
    public static double max(double v[])
    {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = (v.length)-1; (i) >= 0; --i)
            max = Math.max(max, v[i]);
        return max;
    }

    /** sum of elements */
    public static double sum(double v[])
    {
        double sum = 0.;
        for (int i = (v.length)-1; (i) >= 0; --i)
            sum += v[i];
        return sum;
    }
    /** average of elements */
    public static double average(double array[])
    {
        return sum(array) / array.length;
    }
    /** product of elements, not using log (fast) */
    public static double productNotUsingLog(double array[])
    {
        double product = 1.;
        for (int i = (array.length)-1; (i) >= 0; --i)
            product *= array[i];
        return product;
    }
    /** product of elements, using log to avoid over/underflow (slow) */
    public static double productUsingLog(double array[])
    {
        double logProd = 0.;
        for (int i = (array.length)-1; (i) >= 0; --i)
            logProd += Math.log(array[i]);
        return Math.exp(logProd / array.length);
    }
    /** geometric average of elements */
    public static double geomAverage(double array[])
    {
        double logProd = 0.;
        for (int i = (array.length)-1; (i) >= 0; --i)
            logProd += Math.log(array[i]);
        return Math.exp(logProd / array.length);
    }

    /** vector sum of vectors (array rows) */
    public static void sum(double result[], double array[][])
    {
        zerovec(result);
        for (int i = (array.length)-1; (i) >= 0; --i)
            vpv(result, result, array[i]);
    }
    /** vector average of vectors (array rows) */
    public static void average(double result[], double array[][])
    {
        sum(result, array);
        vxs(result, result, 1./array.length);
    }
    /** vector sum of indexed list into array of vectors */
    public static void sumIndexed(double result[], int inds[], double array[][])
    {
        zerovec(result);
        for (int i = (inds.length)-1; (i) >= 0; --i)
            vpv(result, result, array[inds[i]]);
    }
    /** vector sum of indexed list into array of vectors */
    public static void sumIndexed(float result[], int inds[], float array[][])
    {
        zerovec(result);
        for (int i = (inds.length)-1; (i) >= 0; --i)
            vpv(result, result, array[inds[i]]);
    }
    /** vector average of indexed list into array of vectors */
    public static void averageIndexed(double result[], int inds[], double array[][])
    {
        sumIndexed(result, inds, array);
        vxs(result, result, (double)1/(double)inds.length);
    }
    /** vector average of indexed list into array of vectors */
    public static void averageIndexed(float result[], int inds[], float array[][])
    {
        sumIndexed(result, inds, array);
        vxs(result, result, (float)1/(float)inds.length);
    }
    /** vector average of two-dimensional indexed list into array of vectors */
    public static void averageIndexed(double result[], int inds[][], double array[][])
    {
        int totalInds = 0;
        zerovec(result);
        for (int i = (inds.length)-1; (i) >= 0; --i)
        {
            int row[] = inds[i];
            for (int j = (row.length)-1; (j) >= 0; --j)
            {
                vpv(result, result, array[row[j]]);
                totalInds++;
            }
        }
        vxs(result, result, (double)1/(double)totalInds);
    }
    /** vector average of two-dimensional indexed list into array of vectors */
    public static void averageIndexed(float result[], int inds[][], float array[][])
    {
        int totalInds = 0;
        zerovec(result);
        for (int i = (inds.length)-1; (i) >= 0; --i)
        {
            int row[] = inds[i];
            for (int j = (row.length)-1; (j) >= 0; --j)
            {
                vpv(result, result, array[row[j]]);
                totalInds++;
            }
        }
        vxs(result, result, (float)1/(float)totalInds);
    }

    /**
    *  Compute the array of sums-- each element of result
    *  is the sum of the box starting at index 0,0,...0
    *  up to and including the given index.
    *  result is allowed to be the same as from.
    */
    public static void integrate(Object result, Object from)
    {
        if (result instanceof int[])
        {
            int resultArray[] = (int[])result;
            int fromArray[] = (int[])from;
            int sum = 0;
            int n = resultArray.length;
            for (int i = 0; (i) < (n); ++i)
            {
                sum += fromArray[i];
                resultArray[i] = sum;
            }
        }
        else if (result instanceof double[])
        {
            double resultArray[] = (double[])result;
            double fromArray[] = (double[])from;
            double sum = 0;
            int n = resultArray.length;
            for (int i = 0; (i) < (n); ++i)
            {
                sum += fromArray[i];
                resultArray[i] = sum;
            }
        }
        else // must be a higher-dimensonal array of int or double
        {
            Object resultArray[] = (Object[])result;
            Object fromArray[] = (Object[])from;
            int n = resultArray.length;
            for (int i = 0; (i) < (n); ++i)
                integrate(resultArray[i], fromArray[i]);
            for (int i = 0; (i) < (n-1); ++i)
                mpm(resultArray[i+1], resultArray[i], resultArray[i+1]);
        }
    } // integrate

    /**
    *  Inverse of the integrate function.
    *  result is allowed to be the same as from.
    */
    public static void differentiate(Object result, Object from)
    {
        if (result instanceof int[])
        {
            int resultArray[] = (int[])result;
            int fromArray[] = (int[])from;
            for (int i = (resultArray.length-1)-1; (i) >= 0; --i)
                resultArray[i+1] = fromArray[i+1] - fromArray[i];
            if (resultArray.length > 0)
                resultArray[0] = fromArray[0];
        }
        else if (result instanceof double[])
        {
            double resultArray[] = (double[])result;
            double fromArray[] = (double[])from;
            for (int i = (resultArray.length-1)-1; (i) >= 0; --i)
                resultArray[i+1] = fromArray[i+1] - fromArray[i];
            if (resultArray.length > 0)
                resultArray[0] = fromArray[0];
        }
        else // must be a higher-dimensional array of int or double
        {
            Object resultArray[] = (Object[])result;
            Object fromArray[] = (Object[])from;
            for (int i = (resultArray.length)-1; (i) >= 0; --i)
                differentiate(resultArray[i], fromArray[i]);
            for (int i = (resultArray.length-1)-1; (i) >= 0; --i)
                mmm(resultArray[i+1], resultArray[i+1], resultArray[i]);
        }
    } // differentiate

        /** recursive function used by bboxIndexed... */
        private static void _bboxIndexed(double result[/*2*/][],
                                         double coords[][],
                                         Object inds) // array of ints, dim >= 1
        {
            int nDims = coords[0].length;
            Class indsClass = inds.getClass();
            Class componentType = indsClass.getComponentType();
            do { if (!(componentType != null)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1169 +"): " + "componentType != null" + ""); } while (false); // inds must be an array
            if (componentType.isArray())
            {
                int n = java.lang.reflect.Array.getLength(inds);
                for (int i = (n)-1; (i) >= 0; --i)
                    _bboxIndexed(result,
                                 coords,
                                 java.lang.reflect.Array.get(inds, i));
            }
            else
            {
                int ints[] = (int[])inds;
                for (int iInt = (ints.length)-1; (iInt) >= 0; --iInt)
                for (int iDim = (nDims)-1; (iDim) >= 0; --iDim)
                {
                    double val = coords[ints[iInt]][iDim];
                    result[0][iDim] = Math.min(result[0][iDim], val);
                    result[1][iDim] = Math.max(result[1][iDim], val);
                }
            }
        } // _bboxIndexed
    /** bounding box of multidimensional indexed list into an array of points */
    public static void bboxIndexed(double result[/*2*/][],
                                   double coords[][],
                                   Object inds) // array of ints, dim >= 1
    {
        fillvec(result[0], Double.POSITIVE_INFINITY);
        fillvec(result[1], Double.NEGATIVE_INFINITY);
        _bboxIndexed(result, coords, inds);

    } // bboxIndexed

    /** bounding box of an array of points */
    public static void bbox(double result[/*2*/][], double array[][])
    {
        for (int i = (result[0].length)-1; (i) >= 0; --i)
        {
            result[0][i] = Double.POSITIVE_INFINITY;
            result[1][i] = Double.NEGATIVE_INFINITY;
        }
        for (int i = (array.length)-1; (i) >= 0; --i)
        for (int j = (array[i].length)-1; (j) >= 0; --j)
        {
            result[0][j] = Math.min(result[0][j], array[i][j]);
            result[1][j] = Math.max(result[1][j], array[i][j]);
        }
    }

    /** uniform (i.e. square, cube, etc.) bounding box of array of points */
    public static void bboxUniform(double bbox[/*2*/][], double array[][])
    {
        bbox(bbox, array);
        double bboxCenter[] = average(bbox);
        double bboxDims[] = vmv(bbox[1], bbox[0]);
        double bboxMaxDim = max(bboxDims);
        fillvec(bboxDims, bboxMaxDim);
        vpsxv(bbox[0], bboxCenter, -.5, bboxDims);
        vpsxv(bbox[1], bboxCenter, .5, bboxDims);
    }

    /** intersect two boxes */
    public static void bboxIntersect(double result[/*2*/][],
                                     double bbox0[/*2*/][],
                                     double bbox1[/*2*/][])
    {
        int n = result[0].length;
        for (int i = 0; (i) < (n); ++i)
        {
            result[0][i] = ((bbox0[0][i])>=(bbox1[0][i])?(bbox0[0][i]):(bbox1[0][i]));
            result[1][i] = ((bbox0[1][i])<=(bbox1[1][i])?(bbox0[1][i]):(bbox1[1][i]));
        }
    }

    /** bounding box of union of two boxes */
    public static void bboxUnion(double result[/*2*/][],
                                 double bbox0[/*2*/][],
                                 double bbox1[/*2*/][])
    {
        int n = result[0].length;
        for (int i = 0; (i) < (n); ++i)
        {
            result[0][i] = ((bbox0[0][i])<=(bbox1[0][i])?(bbox0[0][i]):(bbox1[0][i]));
            result[1][i] = ((bbox0[1][i])>=(bbox1[1][i])?(bbox0[1][i]):(bbox1[1][i]));
        }
    }

    /** whether bbox (including boundary) contains point, within tolerance eps */
    public static boolean closedBBoxContains(double bbox[/*2*/][],
                                             double point[],
                                             double eps)
    {
        for (int i = (point.length)-1; (i) >= 0; --i)
            if (!((bbox[0][i] - eps <=(point[i]))&&((point[i])<= bbox[1][i] + eps)))
                return false;
        return true;
    }
    /** whether bbox (exluding boundary) contains point, within tolerance eps */
    public static boolean bboxInteriorContains(double bbox[/*2*/][],
                                               double point[],
                                               double eps)
    {
        for (int i = (point.length)-1; (i) >= 0; --i)
            if (!((bbox[0][i] + eps <(point[i]))&&((point[i])< bbox[1][i] - eps)))
                return false;
        return true;
    }


    /** transpose matrix */
    public static void transpose(double result[][], double M[][])
    {
        do { if (!(result != M)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1280 +"): " + "result != M" + ""); } while (false);
        for (int i = (M.length)-1; (i) >= 0; --i)
        for (int j = (M[i].length)-1; (j) >= 0; --j)
            result[j][i] = M[i][j];
    }

    /**
     * LU decompomposition, straight out of Numerical recipes in C.
     * <p>
     * The nxn matrix A is replaced by the LU decomposition
     * of a rowwise permutation of itself.
     * indx is an output vector that records the row permutation
     * effected by the partial pivoting;
     * the function return value is +-1 depending on whether
     * the number of row interchanges was even or odd, respectively.
     * <p>
     * This function is used in combination with luBackSubstitute
     * to solve linear equations or invert a matrix.
     * <p>
     * These functions are used internally by
     * <code>invmxv</code>,
     * <code>invmxm</code>,
     * <code>vxinvm</code>,
     * and <code>mxinvm</code>
     * which provide an easier-to-use
     * interface for solving linear (and linear least-squares) systems.
     */
    public static int luDecompose(double A[/*n*/][/*n*/],
                             int n, int indx[])
        throws Exception // when singular
    {
        double vv[] = new double[n]; // stores the implicit scaling of each row.
        int d = 1; // no row interchanges yet.
        for (int i = 0; (i) < (n); ++i) // Loop over rows to get the implicit scaling information.
        {
            // max will be absolute value of largest element in row i.
            double max = 0.;
            for (int j = 0; (j) < (n); ++j)
            {
                double temp = ((A[i][j]) < 0 ? -(A[i][j]) : (A[i][j]));
                if (temp > max)
                    max = temp;
            }
            if (max == 0.)
            {
                throw new Exception("Singular matrix in routine luDecompose"); // No nonzero largest element.  // XXX should throw something else? XXX should make sure we handle this exception in all functions that call this one?
            }
            vv[i] = 1./max;
        }
        for (int j = 0; (j) < (n); ++j) // This is the loop over columns of Crout's method.
        {
            for (int i = 0; (i) < (j); ++i) // This is equation (2.3.12) except for i==j.
            {
                double sum = A[i][j];
                for (int k = 0; (k) < (i); ++k)
                    sum -= A[i][k] * A[k][j];
                A[i][j] = sum;
            }

            double max = 0.; // Initialize for the search for largest pivot element.
            int imax = -1;
            for (int i = j; i < n; i++) // This is i==j of equation (2.3.12) and i==j+1..n-1 of equation (2.3.13).
            {
                double sum = A[i][j];
                for (int k = 0; (k) < (j); ++k)
                    sum -= A[i][k] * A[k][j];
                A[i][j] = sum;
                double dum = vv[i] * ((sum) < 0 ? -(sum) : (sum));
                if (dum >= max) // Is the figure of merit for the pivot better than the best so far?
                {
                    max = dum;
                    imax = i;
                }
            }
            do { if (!(imax != -1)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1354 +"): " + "imax != -1" + ""); } while (false);
            if (j != imax) // Do we need to interchange rows?
            {
                // Yes, do so...
                double temp[];
                {temp=(A[imax]);A[imax]=(A[j]);A[j]=(temp);};
                d = -d; // ... and change the parity of d.
                vv[imax] = vv[j]; // Also interchange the scale factor.
            }
            indx[j] = imax;
            if (A[j][j] == 0.)
            {
                // If the pivot element is zero the matrix is singular
                // (at least to the precision of the algorithm).
                // For some applications on singular matrices,
                // it is desirable to substitute TINY for zero.

                A[j][j] = 1e-40;
            }
            if (j != n-1) // now, finally, divide by the pivot element.
            {
                double invAjj = 1./A[j][j];
                for (int i = j+1; i < n; i++)
                    A[i][j] *= invAjj;
            }
        }
        return d;
    } // luDecompose

    /**
     * Solves the set of n linear equations A*x = b.
     * <p>
     * Here A[0..n-1] is input,
     * not as the matrix A but rather its LU decomposition,
     * determined by the routing luDecompose.
     * indx[0..n-1] is input as the permutation vector returned by luDecompose.
     * b[0..n-1] is input as the right-hand side vector b,
     * and returns with the solution vector x.
     * A, n, and indx are not modified by this routine
     * and can be left in place for successive calls
     * with different right-hand sides b.
     * <p>
     * This routine takes into account the possibility that b will begin
     * with many zero elements, so it is efficient for use
     * in matrix inversion.
     * <p>
     * These functions are used internally by
     * <code>invmxv</code>,
     * <code>invmxm</code>,
     * <code>vxinvm</code>,
     * and <code>mxinvm</code>
     * which provide an easier-to-use
     * interface for solving linear (and linear least-squares) systems.
     */
    public static void luBackSubstitute(double A[][],
                              int n,
                              int indx[],
                              double b[])
    {
        int ii = -1;
        // when ii is set to a nonnegative value,
        // it will become the index of the first nonvanishing element
        // of b.

        // We now do the forward substitution, equation (2.3.6).
        // The only new wrinkle is to unscramble the permutation as we go.
        for (int i = 0; (i) < (n); ++i)
        {
            int ip = indx[i];
            double sum = b[ip];
            b[ip] = b[i];
            if (ii >= 0)
                for (int j = ii; j < i; j++)
                    sum -= A[i][j] * b[j];
            else if (sum != 0.)
                ii = i; // A nonzero element was encountered, so from now on we will have to do the sums in the loop above.
            b[i] = sum;

        }
        for (int i = (n)-1; (i) >= 0; --i) // Now we do the backsubstitution, equation (2.3.7).
        {
            double sum = b[i];
            for (int j = i+1; j < n;j++)
                sum -= A[i][j] * b[j];
            b[i] = sum / A[i][i]; // Store a component of the solution vector x.
        } // All done!
    } // luBackSubstitute


    /**
     * Linear equation solver and matrix inverter
     * by Gauss-Jordan elimination with full pivoting,
     * straight out of Numerical recipes in C, page 39.
     * <p>
     * a[0..n-1][0..n-1] is the input matrix.
     * b[0..n-1][0..m-1] is input containing the m
     * right-hand vectors as columns.
     * On output, a is replaced by its matrix inverse, and b is replaced
     * by the corresponding set of solution vectors (as columns).
     */
    public static void gaussj(double a[/*n*/][/*n*/], int n,
                              double b[/*n*/][/*m*/], int m)
        throws Exception // on exactly singular matrix
    {
        int indxc[], indxr[], ipiv[];
        int i,icol=-1,irow=-1,j,k,l,ll;
        double big,dum,pivinv,doubletemp;

        indxc = new int[n];
        indxr = new int[n];
        ipiv = new int[n];
        for (j = 0; (j) < (n); ++j)
            ipiv[j] = 0;
        for (i = 0; (i) < (n); ++i) {
            big = 0.;
            for (j = 0; (j) < (n); ++j) {
                if (ipiv[j] != 1) {
                    for (k = 0; (k) < (n); ++k) {
                        if (ipiv[k] == 0) {
                            if (((a[j][k]) < 0 ? -(a[j][k]) : (a[j][k])) >= big) {
                                big = ((a[j][k]) < 0 ? -(a[j][k]) : (a[j][k]));
                                irow = j;
                                icol = k;
                            }
                        } else if (ipiv[k] > 1) {
                            // XXX Error - singular matrix, 1
                            throw new Exception("Singular matrix in routine gaussj, 1"); // XXX should throw something else? XXX should make sure we handle this exception in all functions that call this one?
                        }
                    }
                }
            }
            ++ipiv[icol];
            /*
             * We now have the pivot, so interchange rows, if needed,
             * to put the pivot element on the diagonal.
             */
            if (irow != icol) {
                for (l = 0; (l) < (n); ++l) {doubletemp=(a[irow][l]);a[irow][l]=(a[icol][l]);a[icol][l]=(doubletemp);};
                for (l = 0; (l) < (m); ++l) {doubletemp=(b[irow][l]);b[irow][l]=(b[icol][l]);b[icol][l]=(doubletemp);};
            }
            indxr[i]=irow;
            indxc[i]=icol;
            if (a[icol][icol] == 0.) {
                // XXX Error - singular matrix, 2
                throw new Exception("Singular matrix in routine gaussj, 2"); // XXX should throw something else? XXX should make sure we handle this exception in all functions that call this one?
            }
            pivinv = 1.0/a[icol][icol];
            a[icol][icol] = 1.;
            for (l = 0; (l) < (n); ++l) a[icol][l] *= pivinv;
            for (l = 0; (l) < (m); ++l) b[icol][l] *= pivinv;
            for (ll = 0; (ll) < (n); ++ll) {
                if (ll != icol) {
                    dum = a[ll][icol];
                    a[ll][icol] = 0.;
                    for (l = 0; (l) < (n); ++l) a[ll][l] -= a[icol][l]*dum;
                    for (l = 0; (l) < (m); ++l) b[ll][l] -= b[icol][l]*dum;
                }
            }
        }
        /*
         * Unscramble the solution in view of the column interchanges.
         */
        for (l = n-1; l >= 0; --l) {
            if (indxr[l] != indxc[l])
                for (k = 0; (k) < (n); ++k)
                    {doubletemp=(a[k][indxr[l]]);a[k][indxr[l]]=(a[k][indxc[l]]);a[k][indxc[l]]=(doubletemp);};
        }
    } // gaussj

    /** invert matrix */
    public static void invertmat(double result[][], double M[][])
    {
        int nRows = M.length;
        int nCols = ((M).length==0 ? 0 : (M)[0].length);
        do { if (!(nRows == result.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1528 +"): " + "nRows == result.length" + ""); } while (false);
        do { if (!(nCols == ((result).length==0 ? 0 : (result)[0].length))) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1529 +"): " + "nCols == ROWLENGTH(result)" + ""); } while (false);
        if (nRows == nCols)
        {




            double temp[][] = copymat(M);
            int indx[] = new int[nRows];
            try
            {
                /*(void)*/luDecompose(temp, nRows, indx); // decompose the matrix just once.
            }
            catch (Exception e)
            {
                fillmat(result, Double.POSITIVE_INFINITY);
                return;
            }

            double col[] = new double[nRows];
            for (int j = 0; (j) < (nRows); ++j)
            {
                zerovec(col);
                col[j] = 1.;
                luBackSubstitute(temp,nRows,indx,col);
                setcolumn(result, j, col);
            }

        }
        else
        {
            int n = ((nRows)>=(nCols)?(nRows):(nCols));
            double temp[][] = identitymat(n);
            copymat(temp, M);



            invertmat(temp, temp);

            copymat(result, temp);
        }
    } // invertmat

    /** destructive matrix determinant (destroys the contents of M in the process) */
    public static double detDestructive(double M[][])
    {
        int n = M.length;
        do { if (!(n == ((M).length==0 ? 0 : (M)[0].length))) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1576 +"): " + "n == ROWLENGTH(M)" + ""); } while (false);

        // XXX duplicate code below
        if (true)
        {
            // optimize for small cases...
            switch (n)
            {
                case 0:
                    return 1.;
                case 1:
                    return M[0][0];
                case 2:
                    return vxv2(M[0], M[1]);
                case 3:
                    return vxvxv3(M[0], M[1], M[2]);
            }
        }
        else
        {
            if (((0 <=(n))&&((n)<= 3)))
                System.err.println("WARNING: doing slow detDestructive for testing, dim = "+n+"");
        }
        int indx[] = new int[n];
        try
        {
            double d = (double)luDecompose(M, n, indx); // this returns d as +-1.
            for (int j = 0; (j) < (n); ++j)
                d *= M[j][j];
            return d;
        }
        catch (Exception e)
        {
            return 0.; // XXX not sure this is really exceptional, but...
        }
    } // detDestructive
    /** matrix determinant */
    public static double det(double M[][])
    {
        int n = M.length;
        do { if (!(n == ((M).length==0 ? 0 : (M)[0].length))) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1616 +"): " + "n == ROWLENGTH(M)" + ""); } while (false);

        // XXX duplicate code above
        if (true)
        {
            // optimize for small cases...
            switch (n)
            {
                case 0:
                    return 1.;
                case 1:
                    return M[0][0];
                case 2:
                    return vxv2(M[0], M[1]);
                case 3:
                    return vxvxv3(M[0], M[1], M[2]);
            }
        }
        else
        {
            if (((0 <=(n))&&((n)<= 3)))
                System.err.println("WARNING: doing slow det for testing, dim = "+n+"");
        }

        return detDestructive(copymat(M));
    } // det


    /**
     * Matrix inverse times column vector
     * (linear equations or least-squares solver).
     * <p>
     * Given a matrix A and column vector b,
     * computes A^-1 b, i.e. the solution x
     * to the system of linear equations A x = b.
     * <p>
     * If the system is overdetermined by dimensions
     * (i.e. A has more rows than columns),
     * then the solution x will be found
     * that minimizes ||A x - b||;
     * that is, x that solves A^T A x = A^T b,
     * that is, x = (A^T A)^-1 A^T b.
     */
    public static void invmxv(double result[], double A[][], double b[])
    {
        if (result.length == 0)
            return; // avoid dangerous assertions

        if (false) // XXX get rid
        {
            System.out.println("result.length" + " = " + (result.length));
            System.out.println("A.length" + " = " + (A.length));
            System.out.println("ROWLENGTH(A)" + " = " + (((A).length==0 ? 0 : (A)[0].length)));
            System.out.println("b.length" + " = " + (b.length));
        }

        int n = b.length;
        do { if (!(n == A.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1673 +"): " + "n == A.length" + ""); } while (false);
        int rowlength = ((A).length==0 ? 0 : (A)[0].length);
        do { if (!(rowlength == result.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1675 +"): " + "rowlength == result.length" + ""); } while (false);
        if (rowlength < n)
        {
            // Linear least squares
            double Atranspose[][] = transpose(A);
            double bb[] = mxv(Atranspose, b); // column vector of size rowlength
            double AA[][] = mxm(Atranspose, A); // rowlength by rowlength
            invmxv(result, AA, bb); // recurse on the normal equation
            return;
        }
        do { if (!(rowlength == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1685 +"): " + "rowlength == n" + ""); } while (false); // A is square
                // XXX note, we get a different assertion above if A is 0x1, should make that more clear

        double temp[][] = copymat(A);
        int indx[] = new int[n];
        try
        {
            /*(void)*/luDecompose(temp, n, indx);
        }
        catch (Exception e)
        {
            fillvec(result, Double.POSITIVE_INFINITY);
            return;
        }
        copyvec(result, b);
        luBackSubstitute(temp, n, indx, result);

        if (normsqrd(result) < 1e20) // XXX work around the TINY thing... this is going to have to cleaned up eventually to deal with nearly singular matrices anyway
        {
            // check that it worked, i.e. that b = A * result
            do { if (!(result != b)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1705 +"): " + "result != b" + ""); } while (false); // XXX shouldn't assume this
            double bb[] = mxv(A, result);
            //PRINTMAT(A);
            //PRINTVEC(result);
            //PRINTVEC(b);
            //PRINTVEC(bb);

            for (int i = 0; (i) < (n); ++i)
            {
                //assert(1. + SQR(bb[i] - b[i]) == 1.);
                if (1. + ((bb[i] - b[i])*(bb[i] - b[i])) != 1.)
                {
                    System.out.println("\"WARNING!\"" + " = " + ("WARNING!")); // XXX lame behavior!
                    System.out.println("bb[i]" + " = " + (bb[i]));
                    System.out.println("b[i]" + " = " + (b[i]));
                    System.out.println("SQR(bb[i] - b[i])" + " = " + (((bb[i] - b[i])*(bb[i] - b[i]))));
                    System.out.println("1. + SQR(bb[i] - b[i])" + " = " + (1. + ((bb[i] - b[i])*(bb[i] - b[i]))));
                }
                do { if (!((float)(1. + ((bb[i] - b[i])*(bb[i] - b[i]))) == (float)1.)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1723 +"): " + "(float)(1. + SQR(bb[i] - b[i])) == (float)1." + ""); } while (false);
            }
        }
    } // invmxv

    /** matrix inverse times matrix */
    public static void invmxm(double result[][], double A[][], double B[][])
    {
        if (result.length == 0 || result[0].length == 0)
            return; // avoid dangerous assertions

        int n = B.length;
        do { if (!(n == A.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1735 +"): " + "n == A.length" + ""); } while (false);
        int rowlength = ((A).length==0 ? 0 : (A)[0].length);
        do { if (!(rowlength == result.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1737 +"): " + "rowlength == result.length" + ""); } while (false);
        int nCols = ((result).length==0 ? 0 : (result)[0].length);
        do { if (!(nCols == ((B).length==0 ? 0 : (B)[0].length))) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1739 +"): " + "nCols == ROWLENGTH(B)" + ""); } while (false);
        if (rowlength < n)
        {
            // Linear least squares
            double Atranspose[][] = transpose(A);
            double BB[][] = mxm(Atranspose, B);
            double AA[][] = mxm(Atranspose, A); // rowlength by rowlength
            invmxm(result, AA, BB); // recurse on the normal equation, of size rowlength
            return;
        }
        do { if (!(rowlength == n)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1749 +"): " + "rowlength == n" + ""); } while (false); // A is square

        double temp[][] = copymat(A);
        int indx[] = new int[n];
        try
        {
            /*(void)*/luDecompose(temp, n, indx);
        }
        catch (Exception e)
        {
            fillmat(result, Double.POSITIVE_INFINITY);
            return;
        }
        double col[] = new double[n];
        for (int i = 0; (i) < (nCols); ++i)
        {
            getcolumn(col, B, i);
            luBackSubstitute(temp, n, indx, col);
            setcolumn(result, i, col);
        }
    } // invmxm

    /**
     * Row vector times matrix inverse
     * (linear equations or least-squares solver).
     * <p>
     * Given a row vector b and a matrix A,
     * computes b A^-1, i.e. the solution x
     * to the system of linear equations x A = b.
     * <p>
     * If the system is overdetermined by dimensions
     * (i.e. A has more columns than rows),
     * then the solution x will be found
     * that minimizes ||x A - b||;
     * that is, x that solves x A A^T = b A^T,
     * that is, x = b A^T (A A^T)^-1.
     */
    public static void vxinvm(double result[], double b[], double A[][])
    {
        if (result.length == 0)
            return; // avoid dangerous assertions
        invmxv(result, transpose(A), b);
    } // vxinvm

    /** matrix times matrix inverse */
    public static void mxinvm(double result[][], double B[][], double A[][])
    {
        if (result.length == 0 || result[0].length == 0)
            return; // avoid dangerous assertions
        double transposeResult[][] = new double[((result).length==0 ? 0 : (result)[0].length)][result.length];
        invmxm(transposeResult, transpose(A), transpose(B));
        transpose(result, transposeResult);
    }

    /** square submatrix */
    public static void submat(double result[][], double M[][], int inds[])
    {
        for (int i = (inds.length)-1; (i) >= 0; --i)
        for (int j = (inds.length)-1; (j) >= 0; --j)
            result[i][j] = M[inds[i]][inds[j]];
    }

    /** set matrix to identity */
    public static void identitymat(double M[][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
        for (int j = (M[i].length)-1; (j) >= 0; --j)
            M[i][j] = (i==j ? 1 : 0);
    }
    /** set matrix to identity */
    public static void identitymat(float M[][])
    {
        for (int i = (M.length)-1; (i) >= 0; --i)
        for (int j = (M[i].length)-1; (j) >= 0; --j)
            M[i][j] = (i==j ? 1 : 0);
    }

    /** Finds the matrix that rotates radians in the direction from fromAxis to toAxis, when applied to a vector by multiplying the matrix on the right by the row vector on the left. */
    public static void makeRowRotMat(double M[][], int fromAxis, int toAxis, double radians)
    {
        do { if (!(fromAxis != toAxis)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1829 +"): " + "fromAxis != toAxis" + ""); } while (false); // which means dim must be >= 2
        identitymat(M);
        double s = Math.sin(radians);
        double c = Math.cos(radians);
        M[fromAxis][fromAxis] = c;
        M[fromAxis][toAxis] = s;
        M[toAxis][fromAxis] = -s;
        M[toAxis][toAxis] = c;
    }
    /** Finds the matrix that rotates radians in the direction from fromAxis to toAxis, when applied to a vector by multiplying the matrix on the right by the row vector on the left. */
    public static void makeRowRotMat(float M[][], int fromAxis, int toAxis, float radians)
    {
        do { if (!(fromAxis != toAxis)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+1841 +"): " + "fromAxis != toAxis" + ""); } while (false); // which means dim must be >= 2
        identitymat(M);
        float s = (float)Math.sin(radians);
        float c = (float)Math.cos(radians);
        M[fromAxis][fromAxis] = c;
        M[fromAxis][toAxis] = s;
        M[toAxis][fromAxis] = -s;
        M[toAxis][toAxis] = c;
    }

    /** Finds the matrix that translates by a given amount, when applied to a vector by multiplying the matrix on the right by the row vector on the left. */
    public static void makeRowTransMat(double M[][], double translate[])
    {
        identitymat(M); // non-square is okay
        int nRows = M.length;
        for (int i = (translate.length)-1; (i) >= 0; --i)
            M[nRows-1][i] = translate[i];
    }
    /** Finds the matrix that translates by minus a given amount, when applied to a vector by multiplying the matrix on the right by the row vector on the left. */
    public static void makeRowTransMatInv(double M[][], double translate[])
    {
        identitymat(M); // non-square is okay
        int nRows = M.length;
        for (int i = (translate.length)-1; (i) >= 0; --i)
            M[nRows-1][i] = -translate[i];
    }

    // XXX got bored and stopped converting from FOR to FORIDOWN here


    /** Finds the matrix that non-uniformly scales by a given amount, when applied to a vector by multiplying the matrix on the right by the row vector on the left. */
    public static void makeRowScaleMat(double M[][], double scale[])
    {
        int i, j, n = M.length, m = (n == 0 ? 0 : M[0].length);
        for (i = 0; (i) < (n); ++i)
        for (j = 0; (j) < (m); ++j)
            M[i][j] = (i==j ? i>=scale.length ? 1.
                                              : scale[i]
                            : 0);
    }

    /**
     * Calculates the angle between two unit vectors,
     * using the robust algorithm described in <a href="http://plunk.org/~hatch/rightway.php">http://plunk.org/~hatch/rightway.php</a>.
     */
    public static double angleBetweenUnitVectors(double u0[], double u1[])
    {
        // XXX can we cleverly avoid the square root?
        if (dot(u0, u1) > 0)
            return 2*Math.asin(.5*dist(u0,u1));
        else
        {
            double minusU1[] = sxv(-1.,u1);
            return Math.PI - 2*Math.asin(.5*dist(u0,minusU1));
        }
    }

    /* XXX figure out how to expose this, see Holyhedron/SynopsisStuff.prejava */
    public static double synopsis(double v[])
    {
        int n = v.length;
        for (int i = 0; (i) < (n); ++i)
            if (v[i] <= 0) // non-strict so we do it this way if on the boundary of the positive orthant
            {
                //
                // Outside the positive orthant,
                // return minus the euclidean distance
                // to the positive orthant.
                //
                double sum = 0.;
                for (; i < n; i++)
                    if (v[i] < 0.)
                        sum += v[i]*v[i];
                return -Math.sqrt(sum);
            }
        {
            //
            // Inside the positive orthant, return the "-2"-norm.
            //
            double sum = 0.;
            for (int i = 0; (i) < (n); ++i)
                sum += 1. / (v[i]*v[i]);
            return Math.sqrt(1. / sum);
            /*
                More stable to calculate it differently,
                    e.g. x / sqrt(1) instead of 1/sqrt(1/x^2)
                    e.g. x y / sqrt(x^2 + y^2) instead of 1/sqrt(1/x^2 + 1/y^2)
                    e.g. x y z / sqrt(x^2 y^2 + x^2 z^2 + y^2 z^2)  instead of 1/sqrt(1/x^2 + 1/y^2 + 1/z^2)
                    I think the following works, but I haven't tested it yet...
                double sum = 0., prod = 1.;
                FORI (i, n)
                {
                    // sum is now the sum of squares of all products of i-1 items from v[0]..v[i-1]  (0 if i=0, 1 if i=1)
                    sum = sum*(v[i]*v[i]) + prod*prod;
                    prod *= v[i];
                    // sum is now the sum of squares of all products of i items from v[0]..v[i]  (1 if i=0)
                }
                // sum is now the sum of squares of all products of n-1 items from v[0]..v[n-1] (0 if n=0, 1 if n=1)
                return sum==0. ? 0. : prod / Math.sqrt(sum);
            */
        }
    } // synopsis of vector returning double

    /**
     * Calculates the angular distance from the boundary of the positive orthant
     * to the vector:
     * positive if inside the positive orthant,
     * and negative if outside it.
     * <p>
     * This angle will be
     * in the range [-pi/2 - asin(1/sqrt(n)) .. asin(1/sqrt(n))].
     * It will be positive for vectors in the all-positive orthant,
     * and less than -pi/2 for numbers in the all-negative orthant.
     */
    public static double positiveOrthantness(double v[],
                                             boolean useSynopsis)
    {
        int n = v.length;

        // Special case for trivial cases to prevent unstable math
        if (n <= 1)
            return n == 0 ? 0. : // abitrary
                   v[0] > 0. ? Math.PI/2 :
                   v[0] < 0. ? -Math.PI/2 : 0.;

        double posSum = 0., negSum = 0.;
        double smallestMagnitudePos = Double.POSITIVE_INFINITY;
        double smallestMagnitudeNeg = Double.NEGATIVE_INFINITY;
        for (int i = 0; (i) < (n); ++i)
        {
            double vi = v[i];
            if (vi <= 0)
            {
                negSum += vi*vi;
                smallestMagnitudeNeg = ((smallestMagnitudeNeg)>=(vi)?(smallestMagnitudeNeg):(vi));
            }
            if (vi >= 0)
            {
                posSum += vi*vi;
                smallestMagnitudePos = ((smallestMagnitudePos)<=(vi)?(smallestMagnitudePos):(vi));
            }

        }
        if (negSum == 0. && posSum == 0.)
            return 0.; // arbitrary, instead of blowing up
        else if (negSum == 0.) // if inside all-positive octant
        {
            if (useSynopsis)
                return synopsis(normalize(v));
            else
                return Math.asin(smallestMagnitudePos/Math.sqrt(posSum)); // stable since argument < 1/sqrt(2) since n>=2
        }
        else if (posSum == 0.) // if inside all-negative octant
        {
            if (useSynopsis)
                return -Math.PI/2 - synopsis(normalize(sxv(-1,v)));
            else
                return -Math.PI/2 + Math.asin(smallestMagnitudeNeg/Math.sqrt(negSum)); // stable since argument < 1/sqrt(2) since n>=2
        }
        else // in intermediate octant
            return -Math.atan2(Math.sqrt(negSum), Math.sqrt(posSum));
    } // positiveOrthantness

    /**
     * Returns the matrix obtained from M by adding one identity row and column
     * at the given index.
     */
    public static double[][] insertIdentityRowAndColumn(double M[][], int ind, double zero, double one)
    {
        double[][] result = new double[M.length+1][M.length+1];
        int i, j;
        for (i = 0; (i) < (M.length); ++i)
        for (j = 0; (j) < (M.length); ++j)
            result[i>=ind?i+1:i][j>=ind?j+1:j] = M[i][j];
        for (i = 0; (i) < (result.length); ++i)
            result[i][ind] = result[ind][i] = zero;
        result[ind][ind] = one;
        return result;
    }

    /**
     * Returns the matrix obtained from M by deleting one row and colum
     * at the given index.
     */
    public static double[][] deleteRowAndColumn(double M[][], int ind)
    {
        double[][] result = new double[M.length-1][M.length-1];
        int i, j, n = result.length;
        for (i = 0; (i) < (n); ++i)
        for (j = 0; (j) < (n); ++j)
            result[i][j] = M[i>=ind?i+1:i][j>=ind?j+1:j];
        return result;
    }


    /** int vector equality test */
    public static boolean equals(int v0[], int v1[])
    {
        if (v0 == null)
            return v1 == null ? true : false;
        else if (v1 == null)
            return false;
        int n = v0.length;
        if (n != v1.length)
            return false;
        for (int i = 0; (i) < (n); ++i)
            if (v0[i] != v1[i])
                return false;
        return true;
    } // equals(int[],int[])
    /** exact double vector equality test */
    public static boolean equalsExactly(double v0[], double v1[])
    {
        if (v0 == null)
            return v1 == null ? true : false;
        else if (v1 == null)
            return false;
        int n = v0.length;
        if (n != v1.length)
            return false;
        for (int i = 0; (i) < (n); ++i)
            if (v0[i] != v1[i])
                return false;
        return true;
    } // equalsExactly(Object[],Object[])
    /** Object vector equality test using == on each element Object */
    public static boolean equalsUsingEqualsSymbol(Object v0[], Object v1[])
    {
        if (v0 == null)
            return v1 == null ? true : false;
        else if (v1 == null)
            return false;
        int n = v0.length;
        if (n != v1.length)
            return false;
        for (int i = 0; (i) < (n); ++i)
            if (v0[i] != v1[i])
                return false;
        return true;
    } // equalsUsingEqualsSymbol(Object[],Object[])

    /** Fuzzy matrix equality test; works even if eps==0. */
    public static boolean equals(double M0[][], double M1[][], double eps)
    {
        if (M0 == null && M1 == null)
            return true;
        if (M0 == null || M1 == null || M0.length != M1.length)
            return false;
        int i, n = M0.length;
        for (i = 0; (i) < (n); ++i)
        {
            if (M0[i].length != M1[i].length)
                return false;
            int j, m = M0[i].length;
            for (j = 0; (j) < (m); ++j)
                if (M0[i][j] != M1[i][j] // avoid function call in common case
                 && Math.abs(M0[i][j]-M1[i][j]) > eps)
                    return false;
        }
        return true;
    } // equals

    /** downward circular shift */
    public static void downshift(double to[][], double from[][])
    {
        double from0[] = from[0];
        int i, n = from.length;
        for (i = 0; (i) < (n-1); ++i)
            to[i] = from[i+1];
        to[i] = from0;
    } // downshift

    /** upward circular shift */
    public static void upshift(double to[][], double from[][])
    {
        int i, n = from.length;
        double fromLast[] = from[n-1];
        for (i = n-1; i > 0; --i)
            to[i] = from[i-1];
        to[i] = fromLast;
    } // upshift


        /**
         * From numerical recipes in C.
         * <p>
         * Computes all eigenvalues and eigenvectors of a real symmetric matrix.
         * On output, elements of A above the diagonal are destroyed.
         * d[0..n-1] returns the eigenvalues of A.
         * V[0..n-1][0..n-1] is a matrix whose columns contain,
         * on output, the normalized eigenvectors of A.
         * The function return value is the number of Jacobi rotations
         * that were required.
         */
        public static int jacobi(double A[][], double d[], double V[][])
        {
            int n = d.length; // == A.length == A[0].length == V.length == V[0].length

            double b[] = new double[n];
            double z[] = new double[n];
            identitymat(V);

            // initialize b and d to the diagonal of A.
            {
                int i;
                for (i = 0; (i) < (n); ++i)
                {
                    b[i] = d[i] = A[i][i];
                    z[i] = 0.; // This vector will accumulate terms of the form ta_pq as in equation (11.1.14) in the book.
                }
            }

            int nRot = 0;
            int iIter;
            for (iIter = 0; (iIter) < (50); ++iIter) // XXX ? is this appropriate? why?
            {
                double sum = 0.;
                {
                    int i, j;
                    for (i = 0; (i) < (n-1); ++i) // sum off-diagonal elements
                    for (j = i+1; j < n; ++j)
                        sum += ((A[i][j]) < 0 ? -(A[i][j]) : (A[i][j]));
                }

                if (sum == 0.)
                {
                    // The normal return, which relies on quadratic convergence
                    // to machine underflow.
                    //PRINT(iIter);
                    return nRot;
                }
                double tresh = (iIter < 3 ? .2*sum/(n*n) : 0.);
                int i, j;
                for (i = 0; (i) < (n-1); ++i)
                for (j = i+1; j < n; ++j)
                {
                    double g = 100.*((A[i][j]) < 0 ? -(A[i][j]) : (A[i][j]));
                    // After four sweeps, skip the rotation if the off-diagonal element is small.
                    if (iIter > 3 && ((d[i]) < 0 ? -(d[i]) : (d[i]))+g == ((d[i]) < 0 ? -(d[i]) : (d[i]))
                                  && ((d[j]) < 0 ? -(d[j]) : (d[j]))+g == ((d[j]) < 0 ? -(d[j]) : (d[j])))
                        A[i][j] = 0.;
                    else if (((A[i][j]) < 0 ? -(A[i][j]) : (A[i][j])) > tresh)
                    {
                        double h = d[j] - d[i];
                        double t;
                        if (((h) < 0 ? -(h) : (h))+g == ((h) < 0 ? -(h) : (h)))
                        {
                            t = A[i][j]/h; // t = 1/(2*theta)
                        }
                        else
                        {
                            double theta = .5*h/(A[i][j]); // equation 11.1.10
                            t = 1./(((theta) < 0 ? -(theta) : (theta)) + Math.sqrt(1. + theta*theta));
                            if (theta < 0.)
                                t = -t;
                        }
                        double c = 1./Math.sqrt(1. + t*t);
                        double s = t*c;
                        double tau = s/(1. + c);
                        h = t * A[i][j];
                        z[i] -= h;
                        z[j] += h;
                        d[i] -= h;
                        d[j] += h;
                        A[i][j] = 0.;
                        int k;

                        for (k = 0; (k) < (i); ++k) // Case of rotations 1 <= k <= i
                            { g = A[k][i]; h = A[k][j]; A[k][i] = g-s*(h+g*tau); A[k][j] = h+s*(g-h*tau); };
                        for (k = i+1; k < j; ++k) // Case of rotations i < k < j
                            { g = A[i][k]; h = A[k][j]; A[i][k] = g-s*(h+g*tau); A[k][j] = h+s*(g-h*tau); };
                        for (k = j+1; k < n; ++k)
                            { g = A[i][k]; h = A[j][k]; A[i][k] = g-s*(h+g*tau); A[j][k] = h+s*(g-h*tau); };
                        for (k = 0; (k) < (n); ++k)
                            { g = V[k][i]; h = V[k][j]; V[k][i] = g-s*(h+g*tau); V[k][j] = h+s*(g-h*tau); };
                        nRot++;
                    }
                } // for i,j above diagonal
                // Update d with the sum of ta_pq, and reinitialize z.
                for (i = 0; (i) < (n); ++i)
                {
                    b[i] += z[i];
                    d[i] = b[i];
                    z[i] = 0.;
                }
            } // for iIter

            // in practice, it pretty much always converges by 8 iterations
            // (for up to an 11x11 matrix)
            System.err.println("Too many iterations in routine jacobi");
            return -1;
        } // jacobi

    /** Find a matrix L such that L*transpose(L) = C. */
    public static int leftSqrtOfSymmetricMatrix(double result[][], boolean whichColumnsOfResultAreImaginary[], double C[][])
    {
        int n = C.length; // == C[0].length
        do { if (!(n == result.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+2238 +"): " + "n == result.length" + ""); } while (false);
        double eigenValues[] = new double[n];
        double eigenVectors[][] = new double[n][n]; // columns
        double temp[][] = copymat(C); // since jacobi is destructive
        int nRot = jacobi(temp, eigenValues, eigenVectors); // XXX not the most efficient when n is big (they say 10 or so)
        if (nRot < 0)
        {
            // it failed; not much we can do
            // XXX should return a more useful failure status though
            if (result != null)
                identitymat(result);
            return -2;
        }

        if (true)
        {
            // check that it worked, i.e. that
            //     C * eigenvectors = eigenVectors * diag(eigenValues)
            double diagEigenValues[][] = identitymat(n);
            int i;
            for (i = 0; (i) < (n); ++i)
                diagEigenValues[i][i] = eigenValues[i];
            double A[][] = mxm(C, eigenVectors);
            double B[][] = mxm(eigenVectors, diagEigenValues);
            //PRINTMAT(C);
            //PRINTVEC(eigenValues);
            //PRINTMAT(eigenVectors);
            //PRINTMAT(A);
            //PRINTMAT(B);
            int j;
            for (i = 0; (i) < (n); ++i)
            for (j = 0; (j) < (n); ++j)
                do { if (!(1. + ((A[i][j]-B[i][j])*(A[i][j]-B[i][j])) == 1.)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+2270 +"): " + "1. + SQR(A[i][j]-B[i][j]) == 1." + ""); } while (false);
        }

        // XXX if result is null, shouldn't do the sqrts and stuff in the
        // XXX following loop

        /*
        PRINTVEC(eigenValues);
        PRINTMAT(eigenVectors);
        */

        //PRINT(nRot);

        int positiveDefiniteStatus = 1; // positive semidefinite until proven otherwise
        int i;
        for (i = 0; (i) < (n); ++i)
        {
            double eigenValue = eigenValues[i];
            if (1.+((eigenValue)*(eigenValue)) == 1.)
                positiveDefiniteStatus = ((positiveDefiniteStatus)<=(0)?(positiveDefiniteStatus):(0)); // borderline
            else if (eigenValue < 0.)
                positiveDefiniteStatus = -1; // definitely not positive semidefinite

            double sqrtEigenValue;
            if (whichColumnsOfResultAreImaginary != null)
            {
                if ((whichColumnsOfResultAreImaginary[i] = (eigenValue < 0.)) == true)
                    sqrtEigenValue = Math.sqrt(-eigenValue);
                else
                    sqrtEigenValue = Math.sqrt(eigenValue);
            }
            else
                sqrtEigenValue = Math.sqrt(((eigenValue)>=(0.)?(eigenValue):(0.)));

            // multiply column i by corrected eigenValue...
            int j;
            for (j = 0; (j) < (n); ++j)
                eigenVectors[j][i] *= sqrtEigenValue;
        }

        copymat(result, eigenVectors);
        return positiveDefiniteStatus;
    } // leftSqrtOfSymmetricMatrix

    /**
     * From paper "The most general methodology to create a valid
     * correlation matrix for risk management and option pricing purposes"
     * by Riccardo Rebonato and Peter Jackel, October 19, 1999.
     * <p>
     * Returns -1 if it changed, 0 if on boundary (within some fuzzy tolerance),
     * 1 if it didn't change.
     * C is assumed to be symmetric and have 1's on the diagonal.
     * <p>
     * XXX maybe should just return the smallest eigenvalue before correction?
     *     then the caller can decide what to do with it.
     */
    public static int positiveSemiDefinitizeWithUnitDiagonal(double result[][], boolean whichColumnsAreImaginary[], double C[][])
    {
        double B[][] = new double[result.length][result.length];
        int positiveDefiniteStatus = leftSqrtOfSymmetricMatrix(B, whichColumnsAreImaginary, C);
        if (positiveDefiniteStatus == -2)
        {
            // failed. XXX throw an exception?
            identitymat(result);
        }
        else
        {
            if (whichColumnsAreImaginary == null)
                correlate(result, B);
        }
        return positiveDefiniteStatus;
    } // positiveSemiDefinitizeWithUnitDiagonal

    /**
     * XXX this is not used; I'm pretty sure it core dumps.
     * <p>
     * -1 = not positive semidefinite
     * <br>
     * 0 = positive semidefinite but not positive definite (within some fuzz)
     * <br>
     * 1 = positive definite
     */
    public static int positiveDefiniteness(double M[][])
    {
        return positiveSemiDefinitizeWithUnitDiagonal(null, null, M);
    }

    /** return the correlation matrix of the rows of V. */
    public static void correlate(double result[][], double V[][])
    {
        double normalizedV[][] = new double[V.length][V.length==0?0:V[0].length];
        normalizeRows(normalizedV, V);
        mxm(result, normalizedV, transpose(normalizedV));
    } // correlate

    /** sets result matrix to M with each row normalized. */
    public static void normalizeRows(double result[][], double M[][])
    {
        int i, n = M.length;
        for (i = 0; (i) < (n); ++i)
            normalize(result[i], M[i]);
    }

    /** Normalize a vector. XXX leaves 0 as 0! may be unexpected in some cases */
    public static void normalize(double result[], double v[])
    {
        double len = norm(v);
        vxs(result, v, len==(double)0 ? (double)1 : (double)1/len);
    }
    /** Normalize a vector. XXX leaves 0 as 0! may be unexpected in some cases */
    public static void normalize(float result[], float v[])
    {
        float len = norm(v);
        vxs(result, v, len==(float)0 ? (float)1 : (float)1/len);
    }

    /** Gram-Schmidt orthonormalize the first k rows of a matrix. */
    public static void gramschmidt(double result[][], double M[][], int k)
    {
        if (result != M)
            copymat(result, M);
        for (int i = 0; i < result.length; ++i)
        {
            for (int j = 0; j < i; ++j)
            {
                // result[j] is already unit length...
                // result[i] -= (result[i] dot result[j])*result[j]
                com.donhatchsw.util.VecMath.vpsxv(result[i],
                              result[i],
                              -com.donhatchsw.util.VecMath.dot(result[i],result[j]),
                              result[j]);
            }
            com.donhatchsw.util.VecMath.normalize(result[i], result[i]);
        }
    } // gramschmidt
    /** Gram-Schmidt orthonormalize a matrix. */
    public static void gramschmidt(double result[][], double M[][])
    {
        gramschmidt(result, M, ((result.length)<=(M.length)?(result.length):(M.length)));
    } // gramschmidt


    /* XXX can we make a general method that takes any kind of array? yes, @see Arrays class */
    public static String toString(boolean v[])
    {
        if (v == null)
            return "(null)";
        int i, n = v.length;
        String result = "<";
        for (i = 0; (i) < (n); ++i)
        {
            result += v[i];
            if (i < n-1)
                result += ",";
        }
        result += ">";
        return result;
    }
    public static String toString(int v[])
    {
        if (v == null)
            return "(null)";
        int i, n = v.length;
        String result = "<";
        for (i = 0; (i) < (n); ++i)
        {
            result += v[i];
            if (i < n-1)
                result += ",";
        }
        result += ">";
        return result;
    }
    public static String toString(double v[])
    {
        if (v == null)
            return "(null)";
        int i, n = v.length;
        String result = "<";
        for (i = 0; (i) < (n); ++i)
        {
            result += v[i];
            if (i < n-1)
                result += ",";
        }
        result += ">";
        return result;
    }
    public static String toString(float v[])
    {
        if (v == null)
            return "(null)";
        int i, n = v.length;
        String result = "<";
        for (i = 0; (i) < (n); ++i)
        {
            result += v[i];
            if (i < n-1)
                result += ",";
        }
        result += ">";
        return result;
    }
    public static String toString(Object v[])
    {
        if (v == null)
            return "(null)";
        int i, n = v.length;
        String result = "<";
        for (i = 0; (i) < (n); ++i)
        {
            result += v[i];
            if (i < n-1)
                result += ",";
        }
        result += ">";
        return result;
    }

    public static String toString(double m[][])
    {
        return Arrays.toStringNonCompact(m, "", "    ");
    }

    public static String toString(float m[][])
    {
        return Arrays.toStringNonCompact(m, "", "    ");
    }

    public static String toString(int m[][])
    {
        return Arrays.toStringNonCompact(m, "", "    ");
    }

    /* XXX convert this to Arrays.toString! */
    public static String toString(int m[][][], String bigSep, String littleSep)
    {
        if (m == null)
            return "(null)";
        String result = "";
        int iLayer, nLayers = m.length;
        for (iLayer = 0; (iLayer) < (nLayers); ++iLayer)
        {
            int iRow, nRows = m[iLayer].length;
            for (iRow = 0; (iRow) < (nRows); ++iRow)
            {
                result += "    [";
                int iCol, nCols = m[iLayer][iRow].length;
                for (iCol = 0; (iCol) < (nCols); ++iCol)
                {
                    result += m[iLayer][iRow][iCol];
                    if (iCol+1 < nCols)
                        result += " ";
                }
                result += "]";
                if (iRow+1 < nRows)
                    result += littleSep;
            }
            if (iLayer+1 < nLayers)
                result += bigSep;
        }
        return result;
    } // toString(int [][][])
    public static String toString(int m[][][])
    {
        return toString(m, "\n\n", "\n");
        //return toString(m, "\n", "");
    } // toString(int [][][])


    // Versions which return a newly allocated array...
    // These are more convenient but slower,
    // and should not be called in compute-intensive inner loops.
    // XXX JAVADOC GROUP

        /** vector plus vector, returning newly allocated result */
        public static double[] vpv(double v0[], double v1[])
        {
            double result[] = new double[v0.length];
            vpv(result, v0, v1);
            return result;
        }
        /** vector plus vector, returning newly allocated result */
        public static float[] vpv(float v0[], float v1[])
        {
            float result[] = new float[v0.length];
            vpv(result, v0, v1);
            return result;
        }
        /** vector plus vector, returning newly allocated result */
        public static int[] vpv(int v0[], int v1[])
        {
            int result[] = new int[v0.length];
            vpv(result, v0, v1);
            return result;
        }
        /** scalar times vector, returning newly allocated result */
        public static double[] sxv(double s, double v[])
        {
            double result[] = new double[v.length];
            sxv(result, s, v);
            return result;
        }
        /** scalar times vector, returning newly allocated result */
        public static int[] sxv(int s, int v[])
        {
            int result[] = new int[v.length];
            sxv(result, s, v);
            return result;
        }
        public static double[] normalize(double v[])
        {
            double result[] = new double[v.length];
            normalize(result, v);
            return result; // XXX should return NULL on failure?  Not sure
        }
        public static float[] normalize(float v[])
        {
            float result[] = new float[v.length];
            normalize(result, v);
            return result; // XXX should return NULL on failure?  Not sure
        }
        /** perp dot, returning newly allocated result */
        public static double[] xv2(double v[/*2*/])
        {
            double result[] = new double[2];
            xv2(result, v);
            return result;
        }
        /** vector minus vector, returning newly allocated result */
        public static double[] vmv(double v0[], double v1[])
        {
            double result[] = new double[v0.length];
            vmv(result, v0, v1);
            return result;
        }
        /** vector minus vector, returning newly allocated result */
        public static float[] vmv(float v0[], float v1[])
        {
            float result[] = new float[v0.length];
            vmv(result, v0, v1);
            return result;
        }
        /** subtract v from each row of M, returning newly allocated result */
        public static int[][] mmv(int M[][], int v[])
        {
            int result[][] = new int[M.length][v.length];
            mmv(result, M, v);
            return result;
        }
        /** subtract v from each row of M, returning newly allocated result */
        public static double[][] mmv(double M[][], double v[])
        {
            double result[][] = new double[M.length][v.length];
            mmv(result, M, v);
            return result;
        }
        /** linear interpolation between vectors, returning newly allocated result */
        public static double[] lerp(double p0[], double p1[], double t)
        {
            double result[] = new double[p0.length];
            lerp(result, p0, p1, t);
            return result;
        }
        /** barycentric interpolation between vectors, returning newly allocated result */
        public static double[] bary(double p0[],
                                    double p1[], double t1,
                                    double p2[], double t2)
        {
            double result[] = new double[p0.length];
            bary(result, p0, p1, t1, p2, t2);
            return result;
        }
        /** return a new vector of zeros */
        public static double[] zerovec(int dim)
        {
            double result[] = new double[dim];
            zerovec(result);
            return result;
        }
        /** return a new vector filled with a given scalar */
        public static double[] fillvec(int dim, double s)
        {
            double result[] = new double[dim];
            fillvec(result,s);
            return result;
        }
        /** return a new vector filled with a given scalar */
        public static int[] fillvec(int dim, int s)
        {
            int result[] = new int[dim];
            fillvec(result,s);
            return result;
        }
        /** return a new vector filled with a given scalar */
        public static boolean[] fillvec(int dim, boolean s)
        {
            boolean result[] = new boolean[dim];
            fillvec(result,s);
            return result;
        }
        /** return a new matrix filled with a given scalar */
        public static double[][] fillmat(int dim0, int dim1, double s)
        {
            double result[][] = new double[dim0][dim1];
            fillmat(result,s);
            return result;
        }
        /** return a new matrix filled with a given scalar */
        public static int[][] fillmat(int dim0, int dim1, int s)
        {
            int result[][] = new int[dim0][dim1];
            fillmat(result,s);
            return result;
        }
        /** return a new matrix filled with a given scalar */
        public static boolean[][] fillmat(int dim0, int dim1, boolean s)
        {
            boolean result[][] = new boolean[dim0][dim1];
            fillmat(result,s);
            return result;
        }
        /** vector sum, returning newly allocated result */
        public static double[] sum(double array[][])
        {
            if (array.length == 0)
                return null;
            double result[] = new double[array[0].length];
            sum(result, array);
            return result;
        }
        /** vector average, returning newly allocated result */
        public static double[] average(double array[][])
        {
            if (array.length == 0)
                return null;
            double result[] = new double[array[0].length];
            average(result, array);
            return result;
        }
        /** vector average indexed, returning newly allocated result */
        public static float[] averageIndexed(int inds[], float array[][])
        {
            if (array.length == 0)
                return null;
            float result[] = new float[array[0].length];
            averageIndexed(result, inds, array);
            return result;
        }
        /** vector average indexed, returning newly allocated result */
        public static double[] averageIndexed(int inds[][], double array[][])
        {
            if (array.length == 0)
                return null;
            double result[] = new double[array[0].length];
            averageIndexed(result, inds, array);
            return result;
        }
        /** vector average indexed, returning newly allocated result */
        public static float[] averageIndexed(int inds[][], float array[][])
        {
            if (array.length == 0)
                return null;
            float result[] = new float[array[0].length];
            averageIndexed(result, inds, array);
            return result;
        }
        /** bbox indexed, returning newly allocated result */
        public static double[/*2*/][] bboxIndexed(double array[][],
                                                  Object inds)
        {
            if (array.length == 0)
                return null; // XXX could be dangerous
            double result[][] = new double[2][array[0].length];
            bboxIndexed(result, array, inds);
            return result;
        }
        /** uniform bounding box, returning newly allocated result */
        public static double[/*2*/][] bboxUniform(double array[][])
        {
            if (array.length == 0)
                return null; // XXX could be dangerous
            double result[][] = new double[2][array[0].length];
            bboxUniform(result, array);
            return result;
        }
        /** bounding box, returning newly allocated result */
        public static double[/*2*/][] bbox(double array[][])
        {
            if (array.length == 0)
                return null; // XXX could be dangerous
            double result[][] = new double[2][array[0].length];
            bbox(result, array);
            return result;
        }
        /** bounding box intersect, returning newly allocated result */
        public static double[/*2*/][] bboxIntersect(double bbox0[/*2*/][],
                                                    double bbox1[/*2*/][])
        {
            double result[][] = new double[2][bbox0[0].length];
            bboxIntersect(result, bbox0, bbox1);
            return result;
        }
        /** bounding box union, returning newly allocated result */
        public static double[/*2*/][] bboxUnion(double bbox0[/*2*/][],
                                                double bbox1[/*2*/][])
        {
            double result[][] = new double[2][bbox0[0].length];
            bboxUnion(result, bbox0, bbox1);
            return result;
        }

        /** submatrix, returning newly allocated result */
        public static double[][] submat(double M[][], int inds[])
        {
            double result[][] = new double[inds.length][inds.length];
            submat(result, M, inds);
            return result;
        }
        /** return newly allocated identity matrix of given dimension */
        public static double[][] identitymat(int n)
        {
            double result[][] = new double[n][n];
            identitymat(result);
            return result;
        }
        /** return newly allocated identity matrix of given dimensions */
        public static double[][] identitymat(int n, int m)
        {
            double result[][] = new double[n][m];
            identitymat(result);
            return result;
        }
        /** transpose matrix, returning newly allocated result */
        public static double[][] transpose(double M[][])
        {
            double result[][] = new double[M.length==0 ? 0 : M[0].length][M.length];
            transpose(result, M);
            return result;
        }
        /** matrix copy, returning newly allocated result */
        public static double[][] copymat(double M[][])
        {
            double result[][] = new double[M.length][M.length==0 ? 0 : M[0].length];
            copymat(result, M);
            return result;
        }
        /** vector copy, returning newly allocated result */
        public static boolean[] copyvec(boolean v[])
        {
            boolean result[] = new boolean[v.length];
            copyvec(result, v);
            return result;
        }
        /** vector copy, returning newly allocated result */
        public static int[] copyvec(int v[])
        {
            int result[] = new int[v.length];
            copyvec(result, v);
            return result;
        }
        /** vector copy, returning newly allocated result */
        public static double[] copyvec(double v[])
        {
            double result[] = new double[v.length];
            copyvec(result, v);
            return result;
        }
        /** vector copy, returning newly allocated result */
        public static float[] copyvec(float v[])
        {
            float result[] = new float[v.length];
            copyvec(result, v);
            return result;
        }
        /** get matrix column, returning newly allocated result */
        public static double[] getcolumn(double M[][], int iCol)
        {
            double result[] = new double[M.length];
            getcolumn(result, M, iCol);
            return result;
        }
        /** matrix times matrix, returning newly allocated result */
        public static double[][] mxm(double m0[/*n*/][/*dotLength*/],
                                     double m1[/*dotLength*/][/*m*/])
        {

            int n = m0.length;
            int dotLength = m1.length;
            int m = (dotLength == 0 ? 0 : m1[0].length);

            {
                // XXX hack-- fix when I'm awake
                if (dotLength != ((m0).length==0 ? 0 : (m0)[0].length))
                {
                    /*
                    assumpt(dotLength == 4 && m0[0].length == 3);
                    n = 4;
                    m = 3;
                    */
                    do { if (!(dotLength == m0[0].length+1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+2870 +"): " + "dotLength == m0[0].length+1" + ""); } while (false);
                    n = dotLength;
                    m = m0[0].length;
                }
            }

            double[][] result = new double[n][m];
            mxm(result, m0, m1);
            return result;
// 2881 # 2887 "com/donhatchsw/util/VecMath.prejava"
        }
        // XXX duplicate code
        /** matrix times matrix, returning newly allocated result */
        public static float[][] mxm(float m0[/*n*/][/*dotLength*/],
                                    float m1[/*dotLength*/][/*m*/])
        {
            int n = m0.length;
            int dotLength = m1.length;
            int m = (dotLength == 0 ? 0 : m1[0].length);
            float[][] result = new float[n][m];
            mxm(result, m0, m1);
            return result;
        }
        // XXX duplicate code
        /** matrix times matrix, returning newly allocated result */
        public static int[][] mxm(int m0[/*n*/][/*dotLength*/],
                                  int m1[/*dotLength*/][/*m*/])
        {
            int n = m0.length;
            int dotLength = m1.length;
            int m = (dotLength == 0 ? 0 : m1[0].length);
            int[][] result = new int[n][m];
            mxm(result, m0, m1);
            return result;
        }

        /** (scalar times vector) plus (scalar times vector), returning newly allocated result */
        public static double[] sxvpsxv(double s0, double v0[],
                                       double s1, double v1[])
        {
            double[] result = new double[v0.length];
            sxvpsxv(result, s0, v0, s1, v1);
            return result;
        }
        /** matrix times scalar, returning newly allocated result */
        public static double[][] mxs(double M[][], double s)
        {
            double result[][] = new double[M.length][M.length==0 ? 0 : M[0].length];
            mxs(result, M, s);
            return result;
        }
        /** matrix times scalar, returning newly allocated result */
        public static float[][] mxs(float M[][], float s)
        {
            float result[][] = new float[M.length][M.length==0 ? 0 : M[0].length];
            mxs(result, M, s);
            return result;
        }
        /** scalar times matrix, returning newly allocated result */
        public static double[][] sxm(double s, double M[][])
        {
            double result[][] = new double[M.length][M.length==0 ? 0 : M[0].length];
            sxm(result, s, M);
            return result;
        }
        /** vector times scalar, returning newly allocated result */
        public static double[] vxs(double v[], double s)
        {
            double result[] = new double[v.length];
            vxs(result, v, s);
            return result;
        }
        /** vector times scalar, returning newly allocated result */
        public static float[] vxs(float v[], float s)
        {
            float result[] = new float[v.length];
            vxs(result, v, s);
            return result;
        }
        /** row vector times matrix, returning newly allocated result */
        public static double[] vxm(double v[], double M[][])
        {
            double result[] = new double[M.length == 0 ? 0 : M[0].length];
            vxm(result, v, M);
            return result;
        }
        /** row vector times matrix, returning newly allocated result */
        public static float[] vxm(float v[], float M[][])
        {
            float result[] = new float[M.length == 0 ? 0 : M[0].length];
            vxm(result, v, M);
            return result;
        }
        /** matrix times column vector, returning newly allocated result */
        public static double[] mxv(double M[][], double v[])
        {
            double result[] = new double[M.length];
            mxv(result, M, v);
            return result;
        }
        /** vector plus (scalar times vector), returning newly allocated result */
        public static double[] vpsxv(double v0[], double s1, double v1[])
        {
            double result[] = new double[v0.length];
            vpsxv(result, v0, s1, v1);
            return result;
        }
        /** vector plus (scalar times vector), returning newly allocated result */
        public static int[] vpsxv(int v0[], int s1, int v1[])
        {
            int result[] = new int[v0.length];
            vpsxv(result, v0, s1, v1);
            return result;
        }
        /** add v to every row of m, returning newly allocated result */
        public static double[][] vpm(double v[], double m[][])
        {
            double result[][] = new double[m.length][((m).length==0 ? 0 : (m)[0].length)];
            vpm(result, v, m);
            return result;
        }
        /** add v to every row of m, returning newly allocated result */
        public static int[][] vpm(int v[], int m[][])
        {
            int result[][] = new int[m.length][((m).length==0 ? 0 : (m)[0].length)];
            vpm(result, v, m);
            return result;
        }

        /** invert matrix, returning newly allocated result */
        public static double[][] invertmat(double M[][])
        {
            double result[][] = new double[M.length][M.length==0 ? 0 : M[0].length];
            invertmat(result, M);
            return result;
        }
        /** matrix inverse times column vector, returning newly allocated result */
        public static double[] invmxv(double M[][], double v[])
        {
            double result[] = new double[((M).length==0 ? 0 : (M)[0].length)];
            invmxv(result, M, v);
            return result;
        }
        /** row vector times inverse matrix, returning newly allocated result */
        public static double[] vxinvm(double v[], double M[][])
        {
            double result[] = new double[M.length];
            vxinvm(result, v, M);
            return result;
        }

        /** 3-dimensional cross product, returning newly allocated result */
        public static double[] vxv3(final double v1[/*3*/], final double v2[/*3*/])
        {
            double result[] = new double[3];
            vxv3(result, v1, v2);
            return result;
        }

        /** n-dimensional cross product of n-1 n-dimensional vectors, returning newly allocated result */
        public static double[] crossprod(double vectors[][])
        {
            double result[] = new double[vectors.length+1];
            crossprod(result, vectors);
            return result;
        }

        /** return a random vector with length <= 1, in a newly allocated result XXX currently broken */
        public static double[] random(int dim)
        {
            double result[] = new double[dim];
            random(result);
            return result;
        }
        /** return a random vector with length <= 1, in a newly allocated result XXX currently broken */
        public static double[] random(int dim, java.util.Random generator)
        {
            double result[] = new double[dim];
            random(result, generator);
            return result;
        }


        /** make row-oriented translation matrix, returning newly allocated result */
        public static double[][] makeRowTransMat(double translate[])
        {
            double result[][] = new double[translate.length+1][translate.length];
            makeRowTransMat(result, translate);
            return result;
        }

        /** make row-oriented inverse translation matrix, returning newly allocated result */
        public static double[][] makeRowTransMatInv(double translate[])
        {
            double result[][] = new double[translate.length+1][translate.length];
            makeRowTransMatInv(result, translate);
            return result;
        }

        /** make row-oriented translation matrix, returning newly allocated result */
        public static double[][] makeRowTransMat(double x, double y, double z)
        {
            return makeRowTransMat(new double[] {x, y, z});
        }

        /** make row-oriented inverse translation matrix, returning newly allocated result */
        public static double[][] makeRowTransMatInv(double x, double y, double z)
        {
            return makeRowTransMatInv(new double[] {x, y, z});
        }

        /** make row-oriented non-uniform scale matrix, returning newly allocated result */
        public static double[][] makeRowScaleMat(double scale[])
        {
            double result[][] = new double[scale.length+1][scale.length];
            makeRowScaleMat(result, scale);
            return result;
        }
        /** make row-oriented non-uniform scale matrix with tie points, returning newly allocated result */
        public static double[][] makeRowScaleMat(double scale[],
                        double tiePointIn[],
                        double tiePointOut[])
        {
            double M[][] = makeRowScaleMat(scale);
            if (tiePointIn != null)
                M = mxm(makeRowTransMatInv(tiePointIn), M); // move in to 0
            if (tiePointOut != null)
                M = mxm(M, makeRowTransMat(tiePointOut)); // move 0 to out
            return M;
        }
        /** make row-oriented non-uniform scale matrix with fixed point, returning newly allocated result */
        public static double[][] makeRowScaleMat(double scale[],
                        double fixedPoint[])
        {
            return makeRowScaleMat(scale, fixedPoint, fixedPoint);
        }

        /** make row-oriented uniform scale matrix with tie points, returning newly allocated result */
        public static double[][] makeRowScaleMat(int n, double scale,
                        double tiePointIn[],
                        double tiePointOut[])
        {
            return makeRowScaleMat(fillvec(n,scale),tiePointIn,tiePointOut); // XXX not the most efficient way to do this
        }
        /** make row-oriented uniform scale matrix with fixed point, returning newly allocated result */
        public static double[][] makeRowScaleMat(int n, double scale,
                                                 double fixedPoint[])
        {
            return makeRowScaleMat(n,scale,fixedPoint,fixedPoint);
        }
        /** make row-oriented uniform scale matrix, returning newly allocated result */
        public static double[][] makeRowScaleMat(int n, double scale)
        {
            return makeRowScaleMat(n,scale,null,null);
        }
        /** make row-oriented non-uniform scale matrix, returning newly allocated result */
        public static double[][] makeRowScaleMat(double x, double y, double z)
        {
            return makeRowScaleMat(new double[]{x,y,z},null,null);
        }




        /** make row-oriented rotation matrix, returning newly allocated result */
        public static double[][] makeRowRotMat(int n, int fromAxis, int toAxis, double radians)
        {
            double result[][] = new double[n][n];
            makeRowRotMat(result, fromAxis, toAxis, radians);
            return result;
        }
        /** make row-oriented rotation matrix, returning newly allocated result */
        public static float[][] makeRowRotMat(int n, int fromAxis, int toAxis, float radians)
        {
            float result[][] = new float[n][n];
            makeRowRotMat(result, fromAxis, toAxis, radians);
            return result;
        }

        /** make row-oriented rotation matrix with tie points, returning newly allocated result */
        public static double[][] makeRowRotMat(int n, int fromAxis, int toAxis, double radians,
                double tiePointIn[],
                double tiePointOut[])
        {
            double M[][] = makeRowRotMat(n, fromAxis, toAxis, radians);
            if (tiePointIn != null)
                M = mxm(makeRowTransMatInv(tiePointIn), M);
            if (tiePointOut != null)
                M = mxm(M, makeRowTransMat(tiePointOut));
            return M;
        }

        /** make row-oriented rotation matrix with fixed point, returning newly allocated result */
        public static double[][] makeRowRotMat(int n, int fromAxis, int toAxis, double radians, double fixedPoint[])
        {
            return makeRowRotMat(n, fromAxis, toAxis, radians, fixedPoint, fixedPoint);
        }

        /** make row-oriented rotation matrix that fixes a given subset of the n-2 axes, returning newly allocated result */
        public static double[][] makeRowRotMat(double radians, double fixedAxes[/*n-2*/][])
        {
            int nFixedAxes = fixedAxes.length;
            int n = (nFixedAxes == 0 ? 2 : fixedAxes[0].length);
            do { if (!(nFixedAxes == n-2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3180 +"): " + "nFixedAxes == n-2" + ""); } while (false);

            do { if (!(n == 3)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3182 +"): " + "n == 3" + ""); } while (false); // and so nFixedAxes == 1

            double fixedAxis[] = fixedAxes[0];

            double rotPlusZToFixedAxis[][];
            {
                rotPlusZToFixedAxis = new double[3][3];
                VecMath.copyvec(rotPlusZToFixedAxis[2], fixedAxis);
                VecMath.normalize(rotPlusZToFixedAxis[2], rotPlusZToFixedAxis[2]);
                int mini = VecMath.mini(fixedAxis);
                VecMath.zerovec(rotPlusZToFixedAxis[1]);
                rotPlusZToFixedAxis[1][mini] = 1.;

                VecMath.vxv3(rotPlusZToFixedAxis[0],
                             rotPlusZToFixedAxis[1],
                             rotPlusZToFixedAxis[2]);
                VecMath.normalize(rotPlusZToFixedAxis[0], rotPlusZToFixedAxis[0]);
                VecMath.vxv3(rotPlusZToFixedAxis[1],
                             rotPlusZToFixedAxis[2],
                             rotPlusZToFixedAxis[0]);
            }

            return mxmxm(VecMath.transpose(rotPlusZToFixedAxis),
                         makeRowRotMat(3, 0, 1, radians),
                         rotPlusZToFixedAxis);
        } // makeRowRotMat around axis

        /** make row-oriented rotation matrix that slerps from vector towards to vector */
        public static double[][] makeRowRotMatThatSlerps(double from[], double to[], double t)
        {
            int n = from.length;
            do { if (!(n == to.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3213 +"): " + "n == to.length" + ""); } while (false);
            do { if (!(n >= 2)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3214 +"): " + "n >= 2" + ""); } while (false);
            from = normalize(from);
            to = normalize(to);
            if (n == 2)
            {
                double angle = t * angleBetweenUnitVectors(from, to);
                return vxv2(from,to) < 0 ? makeRowRotMat(2, 0,1, angle)
                                         : makeRowRotMat(2, 1,0, angle);
            }
            do { if (!(n >= 3)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3223 +"): " + "n >= 3" + ""); } while (false);
            if (1. + distsqrd(from, to) == 1.) // XXX I'm suspicious of this test.  I think this is the right threshold for when to switch between the trig formula and linear interpolation when slerping vectors themselves, but does that mean it makes sense here?
            {
                return identitymat(n);
            }
            if (1. + distsqrd(from, vxs(to, -1.)) == 1.)
            {
                // Vectors are opposite each other.
                // Choose an arbitrary waypoint vector orthogonal
                // to from and to, and use that instead.
                // XXX this code is untested as far as I know
                // choose ax in which abs(from[ax]) is minimal
                int ax = 0;
                for (int i = 1; i < n; ++i) // start at 1
                    if (from[i]*from[i] < from[ax]*from[ax])
                        ax = i;
                double waypoint[] = new double[n]; // zeros
                waypoint[ax] = 1.;
                double fromAndWaypoint[][] = {from, waypoint};
                gramschmidt(fromAndWaypoint, fromAndWaypoint);
                to = waypoint;
                t *= 2.;
            }

            double mat[][] = new double[n][n];
            copyvec(mat[0], from);
            copyvec(mat[1], to);
            if (n > 3)
            {
                java.util.Random generator = new java.util.Random(3); // same thing each time XXX should try not to have to randomize at all!
                for (int i = 2; i < n-1; ++i) // all but the last
                    random(mat[i]);
            }
            crossprod(mat[n-1], mat); // last row is cross prod of other rows, guarantees positive determininant
            gramschmidt(mat, mat);

            double angle = t * angleBetweenUnitVectors(from, to);
            double matXY[][] = makeRowRotMat(n, 0, 1, angle);
            // Return the matrix that:
            //    rotates from,to to the +X,+Y axes, then
            //    rotates +X towards +Y by angle, then
            //    rotates +X,+Y back to from,to
            return mxmxm(transpose(mat),
                         matXY,
                         mat);
        } // makeRowMatThatSlerps


        /**
         * Compute a matrix M
         * such that inTiePoints * M = outTiePoints.
         * <br>
         * I.e.
         * <br>
         *      M = inTiePoints^-1 * outTiePoints.
         */
        public static double[/*dim+1*/][/*dim*/] makeRowTiePointMat(
                               double inTiePoints[][],
                               double outTiePoints[][])
        {
            do { if (!(inTiePoints.length > 0)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3283 +"): " + "inTiePoints.length > 0" + ""); } while (false); // XXX could fix this if I had an attention span, maybe

            int dim = inTiePoints[0].length;
            do { if (!(inTiePoints.length == dim+1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3286 +"): " + "inTiePoints.length == dim+1" + ""); } while (false);
            do { if (!(outTiePoints[0].length == dim)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3287 +"): " + "outTiePoints[0].length == dim" + ""); } while (false);
            do { if (!(outTiePoints.length == dim+1)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3288 +"): " + "outTiePoints.length == dim+1" + ""); } while (false);

            // Do it using padding inTiePoints to homogeneous coords,
            // so I don't have to think.
            double inPadded[][] = VecMath.fillmat(dim+1,dim+1, 1.);
            VecMath.copymat(inPadded, inTiePoints); // using the smaller of the two sizes
            double Mpadded[][] = VecMath.mxm(VecMath.invertmat(inPadded),
                                             outTiePoints);
            double M[][] = new double[dim+1][dim];
            VecMath.copymat(M, Mpadded); // using the smaller of the two sizes
            return M;
        } // makeRowTiePointMat


        /** Flattens the rows of matrix M into a single vector. */
        public static double[] flatten(double M[][])
        {
            return (double[])Arrays.flatten(M, 0, 2); // flatten 2 dimensions into 1
        }

        /**
         * Join a rectangular array of matrices into one giant matrix,
         */
        // XXX this should really be in the Arrays class, if I could
        // figure out a decent way to generalize it...
        // it's really flattening between non-adjacent dimensions...?
        // so if flatten could take an int[][] specifying
        // where to get the final dims from...
        //      transposeAndOrFlatten(mats, new int[][] {{0,2},{1,3}});
        // This is sort of like Mathematica's flatten,
        // except that the list is a lookup from result dimensions
        // to original dimensions instead of vice versa,
        // and no dups are allowed, and all dims in the input must be referenced.
        // Negative numbers could mean traverse the twos-complement
        // of the specified dimension backwards.
        // Maybe should be a flag saying how to combine more than
        // one input dimension into one output dimension:
        // flatten or diagonal?  Or maybe two functions,
        // one that flattens and one that diagonalizes?
        // Hey but what if we only want to take a particular slice(s)
        // of a dimension? Maybe have to separate that into a separate function.
        //
        // XXX try to make a more efficient in-place version?
        //     I started trying to do that, but it became very messy very fast.
        //
        public static double[][] joinmats(double mats[][][][])
        {
            double resultBigRows[][][] = new double[mats.length][][];
            for (int iBigRow = 0; (iBigRow) < (mats.length); ++iBigRow)
            {
                double transposedMatsOnBigRow[][][] = new double[mats[iBigRow].length][][];
                for (int iBigCol = 0; (iBigCol) < (mats[iBigRow].length); ++iBigCol)
                    transposedMatsOnBigRow[iBigCol] = VecMath.transpose(
                                                       mats[iBigRow][iBigCol]);
                resultBigRows[iBigRow] = VecMath.transpose(
                       (double[][])Arrays.flatten(transposedMatsOnBigRow,0,2));
            }
            return (double[][])Arrays.flatten(resultBigRows,0,2);
        } // joinmats


        /** product of three matrices, returning newly allocated result */
        public static double[][] mxmxm(double A[][], double B[][], double C[][])
        {
            return mxm(mxm(A,B),C);
        }
        /** product of three matrices, returning newly allocated result */
        public static float[][] mxmxm(float A[][], float B[][], float C[][])
        {
            return mxm(mxm(A,B),C);
        }
        /** product of four matrices, returning newly allocated result */
        public static double[][] mxmxmxm(double A[][], double B[][], double C[][], double D[][])
        {
            return mxm(mxmxm(A,B,C),D);
        }
        /** product of four matrices, returning newly allocated result */
        public static float[][] mxmxmxm(float A[][], float B[][], float C[][], float D[][])
        {
            return mxm(mxmxm(A,B,C),D);
        }
        /** row vector times matrix times matrix, returning newly allocated result */
        public static double[] vxmxm(double a[], double B[][], double C[][])
        {
            // left-to-right to avoid matrix multiplication
            return vxm(vxm(a,B),C);
        }

        /** compute the sign of a permutation, destroying the contents of perm in the process */
        public static int permutationSignDestructive(int perm[])
        {
            int sign = 1;
            int permi;
            int n = perm.length;
            for (int i = 0; (i) < (n); ++i)
            {
                permi = perm[i];
                while (permi != i)
                {
                    // swap permi with perm[permi]...
                    {
                        // order dependent-- very subtle!
                        // (I tried using SWAP but it bombed)
                        int temp = perm[permi];
                        perm[permi] = permi;
                        permi = temp;
                    }
                    sign = -sign;
                }
            }
            return sign;
        } // permutationSignDestructive

        /** tell whether the given permutation is the identity */
        public static boolean isIdentityPerm(int perm[])
        {
            for (int i = (perm.length)-1; (i) >= 0; --i)
                if (perm[i] != i)
                    return false;
            return true;
        }
        /** set the given permuation to the identity */
        public static void identityperm(int perm[])
        {
            for (int i = (perm.length)-1; (i) >= 0; --i)
                perm[i] = i;
        } // identityperm
        /** return newly allocated identity permutation of given length */
        public static int[] identityperm(int n)
        {
            int result[] = new int[n];
            identityperm(result);
            return result;
        } // identityperm
        /** compute a random permutation of given length */
        public static void randomperm(int perm[], java.util.Random generator)
        {
            identityperm(perm);
            Arrays.shuffle(perm, generator);
        } // randomperm
        /** compute a random permutation of given length, returning newly allocated result */
        public static int[] randomperm(int n, java.util.Random generator)
        {
            int result[] = new int[n];
            randomperm(result, generator);
            return result;
        } // randomperm

        /** invert permutation */
        public static void invertperm(int result[], int perm[])
        {
            do { if (!(result != perm)) throw new Error("Assumption failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3439 +"): " + "result != perm" + ""); } while (false);
            int n = ((result.length)<=(perm.length)?(result.length):(perm.length));
            for (int i = (n)-1; (i) >= 0; --i)
                result[perm[i]] = i;
        } // invertperm
        /** invert permutation, returning newly allocated result */
        public static int[] invertperm(int perm[])
        {
            int result[] = new int[perm.length];
            invertperm(result, perm);
            return result;
        } // invertperm

        /**
         * Compose lookup tables (e.g. perms).
         * <br>
         * lut0[lut1[i]] = composeluts(lut0,lut1)[i]
         */
        public static void composeluts(int result[],
                                       int lut0[],
                                       int lut1[])
        {
            int n = lut1.length;
            do { if (!(n == result.length)) throw new Error("Assertion failed at "+"com/donhatchsw/util/VecMath.prejava"+"("+3462 +"): " + "n == result.length" + ""); } while (false);
            for (int i = 0; (i) < (n); ++i)
                result[i] = lut0[lut1[i]];
        } // composeluts
        /**
         * Compose lookup tables (e.g. perms).
         * <br>
         * lut0[lut1[i]] = composeluts(lut0,lut1)[i]
         */
        public static int[] composeluts(int lut0[],
                                        int lut1[])
        {
            int result[] = new int[lut1.length];
            composeluts(result, lut0, lut1);
            return result;
        } // composeluts






    /**
     * Test some stuff...
     */
    public static void main(String args[])
    {
        if (args.length < 1)
        {
            System.err.println("Usage: VecMath <maxdim> [<expr>]");
            System.exit(1);
        }

        int mindim = 2; // XXX change this to 0
        int maxdim = Integer.parseInt(args[0]);

        for (int dim = mindim; dim <= maxdim; ++dim)
        {
            {
                double M[][] = new double[dim][dim];
                double a[] = new double[dim];
                double b[] = new double[dim];
                double x[] = new double[dim];
                for (int i = 0; (i) < (dim); ++i)
                    random(M[i]);
                random(a);


                mxv(b,M,a);
                invmxv(x,M,b);

                System.out.println("M" + " =\n" + VecMath.toString(M));
                System.out.println("a" + " = " + VecMath.toString(a));
                System.out.println("b" + " = " + VecMath.toString(b));
                System.out.println("x" + " = " + VecMath.toString(x));
            }

            if (dim >= 1)
            {
                double vectors[][] = new double[dim-1][dim];
                identitymat(vectors);
                System.out.println("vectors" + " =\n" + VecMath.toString(vectors));
                System.out.println("crossprod(vectors)" + " = " + VecMath.toString(crossprod(vectors)));
            }
        }

    } // main

} // class VecMath
