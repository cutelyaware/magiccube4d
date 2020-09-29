package com.superliminal.magiccube4d;

import com.donhatchsw.util.VecMath;

/**
 * Contains some high-level static methods built on top of Vec_h.java
 * to support the MagicCube4D application.
 * 
 * Copyright 2005 - Superliminal Software
 * 
 * @author Don Hatch
 */
public class Math4d
{
    private Math4d() {} // disallows instantiation

    private static int _assertionFailed(String s, String s1, int i)
    {
        throw new RuntimeException("Assertion failed at " + s1 + "(" + i + "): " + s);
    }

    public static void get4dRotMatrix(float af[], float af1[], float f, float af2[][])
    {
        int ai[][] = new int[4][4];
        int ai1[][] = new int[4][4];
        float af3[] = new float[4];
        float af4[] = new float[4];
        float af5[][] = new float[3][3];
        float af6[][] = new float[4][4];
        if((af[0] == 0.0F ? 0 : 1) + (af[1] == 0.0F ? 0 : 1) + (af[2] == 0.0F ? 0 : 1) + (af[3] == 0.0F ? 0 : 1) != 1) {
            System.err.println("FAIL in Math4d.get4RotMatrix: " + af[0] + "," + af[1] + "," + af[2] + "," + af[3]);
            _assertionFailed("(center[0]!=0 ? 1 : 0) + (center[1]!=0 ? 1 : 0) + (center[2]!=0 ? 1 : 0) + (center[3]!=0 ? 1 : 0) == 1", "Math4d.prejava", 40);
        }
        int i;
        for(i = 0; i < 4; i++)
            if(af[i] != 0.0F)
                break;

        if(i < 0 || i >= 4)
            _assertionFailed("INRANGE(0 <=, ax, <4)", "Math4d.prejava", 44);
        byte byte0 = af[i] >= 0.0F ? ((byte) (af[i] <= 0.0F ? ((byte) (0)) : 1)) : -1;
        getCanonicalMatThatTakesAxisToMinusW(i, byte0, ai);
        Vec_h._TRANSPOSE4i(ai1, ai);
        Vec_h._VXM4(af3, af, ai);
        if(!Vec_h._ISZEROVEC3(af3) || af3[3] != -3F)
            _assertionFailed("Vec_h._ISZEROVEC3(_3d_center) && _3d_center[3] == -3", "Math4d.prejava", 50);
        Vec_h._VXM4(af4, af1, ai);
        get3dRotMatrixAboutAxis(af4, f, af5);
        Vec_h._M4XM3(af6, ai, af5);
        Vec_h._MXM4(af2, af6, ai1);
    }

    public static void get4dTwistMat(int ai[], float f, int ai1[][])
    {
        float af[] = new float[4];
        float af1[] = new float[4];
        float af2[][] = new float[4][4];
        for(int i = 0; i < 4; i++)
            if((ai[i] >= 0 ? ai[i] : -ai[i]) == 3)
                af[i] = ai[i];
            else
                af[i] = 0.0F;

        Vec_h._SET4(af1, ai);
        get4dRotMatrix(af, af1, f, af2);
        Vec_h._ROUNDMAT4(ai1, af2);
    }

    public static void getCanonicalMatThatTakesAxisToMinusW(int i, int j, int ai[][])
    {
        Vec_h._IDENTMAT4(ai);
        if(i != 3)
        {
            ai[3][3] = 0;
            ai[i][i] = 0;
            ai[3][i] = j;
            ai[i][3] = -j;
        } else if(j > 0)
        {
            ai[3][3] = -1;
            ai[2][2] = -1;
        }
    }

    private static void get3dRotMatrixAboutAxis(float af[], float f, float af1[][])
    {
        float af2[][] = new float[3][3];
        float af3[][] = new float[2][2];
        Vec_h._IDENTMAT3(af1);
        Vec_h._SET3(af1[2], af);
        if((af[0] >= 0.0F ? af[0] : -af[0]) < (af[1] >= 0.0F ? af[1] : -af[1]))
        {
            Vec_h._VXV3(af1[1], af1[2], af1[0]);
            Vec_h._VXV3(af1[0], af1[1], af1[2]);
        } else
        {
            Vec_h._VXV3(af1[0], af1[1], af1[2]);
            Vec_h._VXV3(af1[1], af1[2], af1[0]);
        }
        for(int i = 0; i < 3; i++)
        {
            float f1 = (float) Math.sqrt(Vec_h._NORMSQRD3(af1[i]));
            Vec_h._VDS3(af1[i], af1[i], f1);
        }

        Vec_h._TRANSPOSE3(af2, af1);
        af3[0][0] = (float) Math.cos(f);
        af3[0][1] = (float) Math.sin(f);
        Vec_h._XV2(af3[1], af3[0]);
        Vec_h._M2XM3r(af1, af3, af1);
        Vec_h._MXM3r(af1, af2, af1);
    }

    // Returns true if the two vector magnitudes are close.
    private static boolean magsClose(double v1[], double v2[])
    {
        double mag1 = VecMath.norm(v1);
        double mag2 = VecMath.norm(v2);
        return Math.abs(mag2 - mag1) < .01;
    }

    public static boolean get4dMatThatRotatesThese4ToThose4(
        double these0[], double these1[], double these2[], double these3[],
        double those0[], double those1[], double those2[], double those3[],
        double mat[][])
    {
        // All the inputs should be isometric relative to each other,
        // so we do a bunch of distance checks at the beginning.
        // If any of these fail, we can't do the rotation.
        if(!magsClose(these0, those0) ||
            !magsClose(these1, those1) ||
            !magsClose(these2, those2) ||
            !magsClose(these3, those3))
            return false;

        if(!magsClose(VecMath.vmv(these1, these0), VecMath.vmv(those1, those0)) ||
            !magsClose(VecMath.vmv(these2, these0), VecMath.vmv(those2, those0)) ||
            !magsClose(VecMath.vmv(these3, these0), VecMath.vmv(those3, those0)) ||
            !magsClose(VecMath.vmv(these2, these1), VecMath.vmv(those2, those1)) ||
            !magsClose(VecMath.vmv(these3, these1), VecMath.vmv(those3, those1)) ||
            !magsClose(VecMath.vmv(these3, these2), VecMath.vmv(those3, those2)))
            return false;

        //
        // Now we are going to use VecMath.makeRowTiePointMat.
        // In general, this function does not return a rotation matrix, 
        // but given the checks above and those in the MacroManager, we'll be good.
        //

        double inTiePoints[][] = new double[5][4];
        inTiePoints[0] = these0;
        inTiePoints[1] = these1;
        inTiePoints[2] = these2;
        inTiePoints[3] = these3;
        inTiePoints[4] = VecMath.zerovec(4); // Since the origin does not move during a rotation.

        double outTiePoints[][] = new double[5][4];
        outTiePoints[0] = those0;
        outTiePoints[1] = those1;
        outTiePoints[2] = those2;
        outTiePoints[3] = those3;
        outTiePoints[4] = VecMath.zerovec(4);

        double temp[][] = VecMath.makeRowTiePointMat(inTiePoints, outTiePoints);

        // Turn into a 4x4 matrix.
        VecMath.copymat(mat, temp);
        return true;
    }
}
