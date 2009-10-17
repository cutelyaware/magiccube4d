package com.superliminal.magiccube4d;

/**
 * Contains implementations of vector arithmatic operations.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Don Hatch
 */
public class Vec_h
{

    private static void strip_outer_parens_2(float f)
    {
    }

    private static void strip_outer_parens_3(float f)
    {
    }

    private static void strip_outer_parens_4(float f)
    {
    }

    public static void _XV2(float af[], float af1[])
    {
        strip_outer_parens_2((af[0] = -af1[1]) - (af[1] = af1[0]));
    }

    public static void _VXS2(int ai[], int ai1[], int i)
    {
        strip_outer_parens_2((ai[0] = ai1[0] * i) - (ai[1] = ai1[1] * i));
    }

    public static void _VXS2(float af[], float af1[], float f)
    {
        strip_outer_parens_2((af[0] = af1[0] * f) - (af[1] = af1[1] * f));
    }

    public static float _VXV2(float af[], float af1[])
    {
        return af[0] * af1[1] + af[1] * -af1[0];
    }
    
    public static int _VXV2(short as[], short as1[])
    {
        return as[0] * as1[1] + as[1] * -as1[0];
    }
    
    public static void _VMV2(float af[], float af1[], float af2[])
    {
        strip_outer_parens_2((af[0] = af1[0] - af2[0]) - (af[1] = af1[1] - af2[1]));
    }
    
    public static void _VMV2(short af[], short af1[], short af2[])
    {
        strip_outer_parens_2((af[0] = (short)(af1[0] - af2[0])) - (af[1] = (short)(af1[1] - af2[1])));
    }
    
    public static void _VDS2(float af[], float af1[], float f)
    {
        strip_outer_parens_2((af[0] = af1[0] / f) - (af[1] = af1[1] / f));
    }    
    
    public static void _LERP2(float af[], float af1[], float af2[], float f)
    {
        strip_outer_parens_2((af[0] = af1[0] + f * (af2[0] - af1[0])) - (af[1] = af1[1] + f * (af2[1] - af1[1])));
    }
    
    public static float _NORMSQRD2(float af[])
    {
        return af[0] * af[0] + af[1] * af[1];
    }    

    public static void _SET3(float af[], float af1[])
    {
        strip_outer_parens_3((af[0] = af1[0]) - (af[1] = af1[1]) - (af[2] = af1[2]));
    }

    public static void _LERP3(float af[], float af1[], float af2[], float f)
    {
        strip_outer_parens_3((af[0] = af1[0] + f * (af2[0] - af1[0])) - (af[1] = af1[1] + f * (af2[1] - af1[1])) - (af[2] = af1[2] + f * (af2[2] - af1[2])));
    }

    public static void _VMV3(float af[], float af1[], float af2[])
    {
        strip_outer_parens_3((af[0] = af1[0] - af2[0]) - (af[1] = af1[1] - af2[1]) - (af[2] = af1[2] - af2[2]));
    }

    public static void _VXS3(float af[], float af1[], float f)
    {
        strip_outer_parens_3((af[0] = af1[0] * f) - (af[1] = af1[1] * f) - (af[2] = af1[2] * f));
    }

    public static void _VDS3(float af[], float af1[], float f)
    {
        strip_outer_parens_3((af[0] = af1[0] / f) - (af[1] = af1[1] / f) - (af[2] = af1[2] / f));
    }

    public static void _VXV3(float af[], float af1[], float af2[])
    {
        strip_outer_parens_3((af[0] = af1[1] * af2[2] + af1[2] * -af2[1]) - (af[1] = -(af1[0] * af2[2] + af1[2] * -af2[0])) - (af[2] = af1[0] * af2[1] + af1[1] * -af2[0]));
    }

    public static boolean _ISZEROVEC3(float af[])
    {
        return af[0] == 0.0F && af[1] == 0.0F && af[2] == 0.0F;
    }

    public static float _DOT3(float af[], float af1[])
    {
        return af[0] * af1[0] + af[1] * af1[1] + af[2] * af1[2];
    }

    public static float __DET3(float af[][])
    {
        return af[0][0] * (af[1][1] * af[2][2] + af[1][2] * -af[2][1]) + af[0][1] * -(af[1][0] * af[2][2] + af[1][2] * -af[2][0]) + af[0][2] * (af[1][0] * af[2][1] + af[1][1] * -af[2][0]);
    }

    public static float _NORMSQRD3(float af[])
    {
        return af[0] * af[0] + af[1] * af[1] + af[2] * af[2];
    } 
    
    public static float _TRACE3(float af[][]) {
        return af[0][0] + af[1][1] + af[2][2];
    }
    
    public static void _VPV3(float af[], float af1[], float af2[]) {
        for(int i=0; i<3; i++)
            af[i] = af1[i] + af2[i];
    }

    public static void _VXM3(float af[], float af1[], float af2[][])
    {
        float f = af1[0];
        float f1 = af1[1];
        float f2 = af1[2];
        float af3[] = af2[0];
        float af4[] = af2[1];
        float af5[] = af2[2];
        af[0] = f * af3[0] + f1 * af4[0] + f2 * af5[0];
        af[1] = f * af3[1] + f1 * af4[1] + f2 * af5[1];
        af[2] = f * af3[2] + f1 * af4[2] + f2 * af5[2];
    }

    public static void _MXV3(float af[], float af1[][], float af2[])
    {
        strip_outer_parens_3((af[0] = af1[0][0] * af2[0] + af1[0][1] * af2[1] + af1[0][2] * af2[2]) - (af[1] = af1[1][0] * af2[0] + af1[1][1] * af2[1] + af1[1][2] * af2[2]) - (af[2] = af1[2][0] * af2[0] + af1[2][1] * af2[1] + af1[2][2] * af2[2]));
    }

    public static void _MXM3(float af[][], float af1[][], float af2[][])
    {
        strip_outer_parens_3((af[0][0] = af1[0][0] * af2[0][0] + af1[0][1] * af2[1][0] + af1[0][2] * af2[2][0]) - (af[0][1] = af1[0][0] * af2[0][1] + af1[0][1] * af2[1][1] + af1[0][2] * af2[2][1]) - (af[0][2] = af1[0][0] * af2[0][2] + af1[0][1] * af2[1][2] + af1[0][2] * af2[2][2]));
        strip_outer_parens_3((af[1][0] = af1[1][0] * af2[0][0] + af1[1][1] * af2[1][0] + af1[1][2] * af2[2][0]) - (af[1][1] = af1[1][0] * af2[0][1] + af1[1][1] * af2[1][1] + af1[1][2] * af2[2][1]) - (af[1][2] = af1[1][0] * af2[0][2] + af1[1][1] * af2[1][2] + af1[1][2] * af2[2][2]));
        strip_outer_parens_3((af[2][0] = af1[2][0] * af2[0][0] + af1[2][1] * af2[1][0] + af1[2][2] * af2[2][0]) - (af[2][1] = af1[2][0] * af2[0][1] + af1[2][1] * af2[1][1] + af1[2][2] * af2[2][1]) - (af[2][2] = af1[2][0] * af2[0][2] + af1[2][1] * af2[1][2] + af1[2][2] * af2[2][2]));
    }

    public static void _MXV4(float af[], int ai[][], float af1[])
    {
        strip_outer_parens_4((af[0] = (float)ai[0][0] * af1[0] + (float)ai[0][1] * af1[1] + (float)ai[0][2] * af1[2] + (float)ai[0][3] * af1[3]) - (af[1] = (float)ai[1][0] * af1[0] + (float)ai[1][1] * af1[1] + (float)ai[1][2] * af1[2] + (float)ai[1][3] * af1[3]) - (af[2] = (float)ai[2][0] * af1[0] + (float)ai[2][1] * af1[1] + (float)ai[2][2] * af1[2] + (float)ai[2][3] * af1[3]) - (af[3] = (float)ai[3][0] * af1[0] + (float)ai[3][1] * af1[1] + (float)ai[3][2] * af1[2] + (float)ai[3][3] * af1[3]));
    }

    public static void _MXV4(int ai[], int ai1[][], int ai2[])
    {
        strip_outer_parens_4((ai[0] = ai1[0][0] * ai2[0] + ai1[0][1] * ai2[1] + ai1[0][2] * ai2[2] + ai1[0][3] * ai2[3]) - (ai[1] = ai1[1][0] * ai2[0] + ai1[1][1] * ai2[1] + ai1[1][2] * ai2[2] + ai1[1][3] * ai2[3]) - (ai[2] = ai1[2][0] * ai2[0] + ai1[2][1] * ai2[1] + ai1[2][2] * ai2[2] + ai1[2][3] * ai2[3]) - (ai[3] = ai1[3][0] * ai2[0] + ai1[3][1] * ai2[1] + ai1[3][2] * ai2[2] + ai1[3][3] * ai2[3]));
    }

    public static void _VXM4(float af[], float af1[], float af2[][])
    {
        float f = af1[0];
        float f1 = af1[1];
        float f2 = af1[2];
        float f3 = af1[3];
        float af3[] = af2[0];
        float af4[] = af2[1];
        float af5[] = af2[2];
        float af6[] = af2[3];
        af[0] = f * af3[0] + f1 * af4[0] + f2 * af5[0] + f3 * af6[0];
        af[1] = f * af3[1] + f1 * af4[1] + f2 * af5[1] + f3 * af6[1];
        af[2] = f * af3[2] + f1 * af4[2] + f2 * af5[2] + f3 * af6[2];
        af[3] = f * af3[3] + f1 * af4[3] + f2 * af5[3] + f3 * af6[3];
    }

    public static void _VXM4(int ai[], int ai1[], int ai2[][])
    {
        strip_outer_parens_4((ai[0] = ai1[0] * ai2[0][0] + ai1[1] * ai2[1][0] + ai1[2] * ai2[2][0] + ai1[3] * ai2[3][0]) - (ai[1] = ai1[0] * ai2[0][1] + ai1[1] * ai2[1][1] + ai1[2] * ai2[2][1] + ai1[3] * ai2[3][1]) - (ai[2] = ai1[0] * ai2[0][2] + ai1[1] * ai2[1][2] + ai1[2] * ai2[2][2] + ai1[3] * ai2[3][2]) - (ai[3] = ai1[0] * ai2[0][3] + ai1[1] * ai2[1][3] + ai1[2] * ai2[2][3] + ai1[3] * ai2[3][3]));
    }

    public static void _VXM4(float af[], float af1[], int ai[][])
    {
        strip_outer_parens_4((af[0] = af1[0] * (float)ai[0][0] + af1[1] * (float)ai[1][0] + af1[2] * (float)ai[2][0] + af1[3] * (float)ai[3][0]) - (af[1] = af1[0] * (float)ai[0][1] + af1[1] * (float)ai[1][1] + af1[2] * (float)ai[2][1] + af1[3] * (float)ai[3][1]) - (af[2] = af1[0] * (float)ai[0][2] + af1[1] * (float)ai[1][2] + af1[2] * (float)ai[2][2] + af1[3] * (float)ai[3][2]) - (af[3] = af1[0] * (float)ai[0][3] + af1[1] * (float)ai[1][3] + af1[2] * (float)ai[2][3] + af1[3] * (float)ai[3][3]));
    }

    public static void _SET4(float af[], float af1[])
    {
        strip_outer_parens_4((af[0] = af1[0]) - (af[1] = af1[1]) - (af[2] = af1[2]) - (af[3] = af1[3]));
    }

    public static void _SET4(float af[], int ai[])
    {
        strip_outer_parens_4((af[0] = ai[0]) - (af[1] = ai[1]) - (af[2] = ai[2]) - (af[3] = ai[3]));
    }

    public static void _SET4(int ai[], int ai1[])
    {
        strip_outer_parens_4((ai[0] = ai1[0]) - (ai[1] = ai1[1]) - (ai[2] = ai1[2]) - (ai[3] = ai1[3]));
    }

    public static void _SET4(short aword0[], int ai[])
    {
        for(int i = 0; i < 4; i++)
            aword0[i] = (short)ai[i];

    }

    public static boolean _EQVEC4(int ai[], int ai1[])
    {
        return ai[0] == ai1[0] && ai[1] == ai1[1] && ai[2] == ai1[2] && ai[3] == ai1[3];
    }

    public static boolean _EQMAT4(int ai[][], int ai1[][])
    {
        return ai[0][0] == ai1[0][0] && ai[0][1] == ai1[0][1] && ai[0][2] == ai1[0][2] && ai[0][3] == ai1[0][3] && ai[1][0] == ai1[1][0] && ai[1][1] == ai1[1][1] && ai[1][2] == ai1[1][2] && ai[1][3] == ai1[1][3] && ai[2][0] == ai1[2][0] && ai[2][1] == ai1[2][1] && ai[2][2] == ai1[2][2] && ai[2][3] == ai1[2][3] && ai[3][0] == ai1[3][0] && ai[3][1] == ai1[3][1] && ai[3][2] == ai1[3][2] && ai[3][3] == ai1[3][3];
    }

    public static void _MXM4(int ai[][], int ai1[][], int ai2[][])
    {
        strip_outer_parens_4((ai[0][0] = ai1[0][0] * ai2[0][0] + ai1[0][1] * ai2[1][0] + ai1[0][2] * ai2[2][0] + ai1[0][3] * ai2[3][0]) - (ai[0][1] = ai1[0][0] * ai2[0][1] + ai1[0][1] * ai2[1][1] + ai1[0][2] * ai2[2][1] + ai1[0][3] * ai2[3][1]) - (ai[0][2] = ai1[0][0] * ai2[0][2] + ai1[0][1] * ai2[1][2] + ai1[0][2] * ai2[2][2] + ai1[0][3] * ai2[3][2]) - (ai[0][3] = ai1[0][0] * ai2[0][3] + ai1[0][1] * ai2[1][3] + ai1[0][2] * ai2[2][3] + ai1[0][3] * ai2[3][3]));
        strip_outer_parens_4((ai[1][0] = ai1[1][0] * ai2[0][0] + ai1[1][1] * ai2[1][0] + ai1[1][2] * ai2[2][0] + ai1[1][3] * ai2[3][0]) - (ai[1][1] = ai1[1][0] * ai2[0][1] + ai1[1][1] * ai2[1][1] + ai1[1][2] * ai2[2][1] + ai1[1][3] * ai2[3][1]) - (ai[1][2] = ai1[1][0] * ai2[0][2] + ai1[1][1] * ai2[1][2] + ai1[1][2] * ai2[2][2] + ai1[1][3] * ai2[3][2]) - (ai[1][3] = ai1[1][0] * ai2[0][3] + ai1[1][1] * ai2[1][3] + ai1[1][2] * ai2[2][3] + ai1[1][3] * ai2[3][3]));
        strip_outer_parens_4((ai[2][0] = ai1[2][0] * ai2[0][0] + ai1[2][1] * ai2[1][0] + ai1[2][2] * ai2[2][0] + ai1[2][3] * ai2[3][0]) - (ai[2][1] = ai1[2][0] * ai2[0][1] + ai1[2][1] * ai2[1][1] + ai1[2][2] * ai2[2][1] + ai1[2][3] * ai2[3][1]) - (ai[2][2] = ai1[2][0] * ai2[0][2] + ai1[2][1] * ai2[1][2] + ai1[2][2] * ai2[2][2] + ai1[2][3] * ai2[3][2]) - (ai[2][3] = ai1[2][0] * ai2[0][3] + ai1[2][1] * ai2[1][3] + ai1[2][2] * ai2[2][3] + ai1[2][3] * ai2[3][3]));
        strip_outer_parens_4((ai[3][0] = ai1[3][0] * ai2[0][0] + ai1[3][1] * ai2[1][0] + ai1[3][2] * ai2[2][0] + ai1[3][3] * ai2[3][0]) - (ai[3][1] = ai1[3][0] * ai2[0][1] + ai1[3][1] * ai2[1][1] + ai1[3][2] * ai2[2][1] + ai1[3][3] * ai2[3][1]) - (ai[3][2] = ai1[3][0] * ai2[0][2] + ai1[3][1] * ai2[1][2] + ai1[3][2] * ai2[2][2] + ai1[3][3] * ai2[3][2]) - (ai[3][3] = ai1[3][0] * ai2[0][3] + ai1[3][1] * ai2[1][3] + ai1[3][2] * ai2[2][3] + ai1[3][3] * ai2[3][3]));
    }

    public static void _MXM4(float af[][], float af1[][], int ai[][])
    {
        strip_outer_parens_4((af[0][0] = af1[0][0] * (float)ai[0][0] + af1[0][1] * (float)ai[1][0] + af1[0][2] * (float)ai[2][0] + af1[0][3] * (float)ai[3][0]) - (af[0][1] = af1[0][0] * (float)ai[0][1] + af1[0][1] * (float)ai[1][1] + af1[0][2] * (float)ai[2][1] + af1[0][3] * (float)ai[3][1]) - (af[0][2] = af1[0][0] * (float)ai[0][2] + af1[0][1] * (float)ai[1][2] + af1[0][2] * (float)ai[2][2] + af1[0][3] * (float)ai[3][2]) - (af[0][3] = af1[0][0] * (float)ai[0][3] + af1[0][1] * (float)ai[1][3] + af1[0][2] * (float)ai[2][3] + af1[0][3] * (float)ai[3][3]));
        strip_outer_parens_4((af[1][0] = af1[1][0] * (float)ai[0][0] + af1[1][1] * (float)ai[1][0] + af1[1][2] * (float)ai[2][0] + af1[1][3] * (float)ai[3][0]) - (af[1][1] = af1[1][0] * (float)ai[0][1] + af1[1][1] * (float)ai[1][1] + af1[1][2] * (float)ai[2][1] + af1[1][3] * (float)ai[3][1]) - (af[1][2] = af1[1][0] * (float)ai[0][2] + af1[1][1] * (float)ai[1][2] + af1[1][2] * (float)ai[2][2] + af1[1][3] * (float)ai[3][2]) - (af[1][3] = af1[1][0] * (float)ai[0][3] + af1[1][1] * (float)ai[1][3] + af1[1][2] * (float)ai[2][3] + af1[1][3] * (float)ai[3][3]));
        strip_outer_parens_4((af[2][0] = af1[2][0] * (float)ai[0][0] + af1[2][1] * (float)ai[1][0] + af1[2][2] * (float)ai[2][0] + af1[2][3] * (float)ai[3][0]) - (af[2][1] = af1[2][0] * (float)ai[0][1] + af1[2][1] * (float)ai[1][1] + af1[2][2] * (float)ai[2][1] + af1[2][3] * (float)ai[3][1]) - (af[2][2] = af1[2][0] * (float)ai[0][2] + af1[2][1] * (float)ai[1][2] + af1[2][2] * (float)ai[2][2] + af1[2][3] * (float)ai[3][2]) - (af[2][3] = af1[2][0] * (float)ai[0][3] + af1[2][1] * (float)ai[1][3] + af1[2][2] * (float)ai[2][3] + af1[2][3] * (float)ai[3][3]));
        strip_outer_parens_4((af[3][0] = af1[3][0] * (float)ai[0][0] + af1[3][1] * (float)ai[1][0] + af1[3][2] * (float)ai[2][0] + af1[3][3] * (float)ai[3][0]) - (af[3][1] = af1[3][0] * (float)ai[0][1] + af1[3][1] * (float)ai[1][1] + af1[3][2] * (float)ai[2][1] + af1[3][3] * (float)ai[3][1]) - (af[3][2] = af1[3][0] * (float)ai[0][2] + af1[3][1] * (float)ai[1][2] + af1[3][2] * (float)ai[2][2] + af1[3][3] * (float)ai[3][2]) - (af[3][3] = af1[3][0] * (float)ai[0][3] + af1[3][1] * (float)ai[1][3] + af1[3][2] * (float)ai[2][3] + af1[3][3] * (float)ai[3][3]));
    }

    public static void _MXM4(float af[][], float af1[][], float af2[][])
    {
        strip_outer_parens_4((af[0][0] = af1[0][0] * af2[0][0] + af1[0][1] * af2[1][0] + af1[0][2] * af2[2][0] + af1[0][3] * af2[3][0]) - (af[0][1] = af1[0][0] * af2[0][1] + af1[0][1] * af2[1][1] + af1[0][2] * af2[2][1] + af1[0][3] * af2[3][1]) - (af[0][2] = af1[0][0] * af2[0][2] + af1[0][1] * af2[1][2] + af1[0][2] * af2[2][2] + af1[0][3] * af2[3][2]) - (af[0][3] = af1[0][0] * af2[0][3] + af1[0][1] * af2[1][3] + af1[0][2] * af2[2][3] + af1[0][3] * af2[3][3]));
        strip_outer_parens_4((af[1][0] = af1[1][0] * af2[0][0] + af1[1][1] * af2[1][0] + af1[1][2] * af2[2][0] + af1[1][3] * af2[3][0]) - (af[1][1] = af1[1][0] * af2[0][1] + af1[1][1] * af2[1][1] + af1[1][2] * af2[2][1] + af1[1][3] * af2[3][1]) - (af[1][2] = af1[1][0] * af2[0][2] + af1[1][1] * af2[1][2] + af1[1][2] * af2[2][2] + af1[1][3] * af2[3][2]) - (af[1][3] = af1[1][0] * af2[0][3] + af1[1][1] * af2[1][3] + af1[1][2] * af2[2][3] + af1[1][3] * af2[3][3]));
        strip_outer_parens_4((af[2][0] = af1[2][0] * af2[0][0] + af1[2][1] * af2[1][0] + af1[2][2] * af2[2][0] + af1[2][3] * af2[3][0]) - (af[2][1] = af1[2][0] * af2[0][1] + af1[2][1] * af2[1][1] + af1[2][2] * af2[2][1] + af1[2][3] * af2[3][1]) - (af[2][2] = af1[2][0] * af2[0][2] + af1[2][1] * af2[1][2] + af1[2][2] * af2[2][2] + af1[2][3] * af2[3][2]) - (af[2][3] = af1[2][0] * af2[0][3] + af1[2][1] * af2[1][3] + af1[2][2] * af2[2][3] + af1[2][3] * af2[3][3]));
        strip_outer_parens_4((af[3][0] = af1[3][0] * af2[0][0] + af1[3][1] * af2[1][0] + af1[3][2] * af2[2][0] + af1[3][3] * af2[3][0]) - (af[3][1] = af1[3][0] * af2[0][1] + af1[3][1] * af2[1][1] + af1[3][2] * af2[2][1] + af1[3][3] * af2[3][1]) - (af[3][2] = af1[3][0] * af2[0][2] + af1[3][1] * af2[1][2] + af1[3][2] * af2[2][2] + af1[3][3] * af2[3][2]) - (af[3][3] = af1[3][0] * af2[0][3] + af1[3][1] * af2[1][3] + af1[3][2] * af2[2][3] + af1[3][3] * af2[3][3]));
    }

    public static void _ZEROMAT2(float af[][])
    {
        strip_outer_parens_2((af[0][0] = 0.0F) - (af[0][1] = 0.0F));
        strip_outer_parens_2((af[1][0] = 0.0F) - (af[1][1] = 0.0F));
    }

    public static void _ZEROMAT3(float af[][])
    {
        strip_outer_parens_3((af[0][0] = 0.0F) - (af[0][1] = 0.0F) - (af[0][2] = 0.0F));
        strip_outer_parens_3((af[1][0] = 0.0F) - (af[1][1] = 0.0F) - (af[1][2] = 0.0F));
        strip_outer_parens_3((af[2][0] = 0.0F) - (af[2][1] = 0.0F) - (af[2][2] = 0.0F));
    }

    public static void _IDENTMAT3(float af[][])
    {
        strip_outer_parens_3((af[0][0] = 0.0F) - (af[0][1] = 0.0F) - (af[0][2] = 0.0F));
        af[0][0] = 1.0F;
        strip_outer_parens_3((af[1][0] = 0.0F) - (af[1][1] = 0.0F) - (af[1][2] = 0.0F));
        af[1][1] = 1.0F;
        strip_outer_parens_3((af[2][0] = 0.0F) - (af[2][1] = 0.0F) - (af[2][2] = 0.0F));
        af[2][2] = 1.0F;
    }

    public static void _TRANSPOSE3(float af[][], float af1[][])
    {
        strip_outer_parens_3((af[0][0] = af1[0][0]) - (af[0][1] = af1[1][0]) - (af[0][2] = af1[2][0]));
        strip_outer_parens_3((af[1][0] = af1[0][1]) - (af[1][1] = af1[1][1]) - (af[1][2] = af1[2][1]));
        strip_outer_parens_3((af[2][0] = af1[0][2]) - (af[2][1] = af1[1][2]) - (af[2][2] = af1[2][2]));
    }

    public static void _SETMAT3(float af[][], float af1[][])
    {
        strip_outer_parens_3((af[0][0] = af1[0][0]) - (af[0][1] = af1[0][1]) - (af[0][2] = af1[0][2]));
        strip_outer_parens_3((af[1][0] = af1[1][0]) - (af[1][1] = af1[1][1]) - (af[1][2] = af1[1][2]));
        strip_outer_parens_3((af[2][0] = af1[2][0]) - (af[2][1] = af1[2][1]) - (af[2][2] = af1[2][2]));
    }

    public static void _ZEROMAT4(float af[][])
    {
        strip_outer_parens_4((af[0][0] = 0.0F) - (af[0][1] = 0.0F) - (af[0][2] = 0.0F) - (af[0][3] = 0.0F));
        strip_outer_parens_4((af[1][0] = 0.0F) - (af[1][1] = 0.0F) - (af[1][2] = 0.0F) - (af[1][3] = 0.0F));
        strip_outer_parens_4((af[2][0] = 0.0F) - (af[2][1] = 0.0F) - (af[2][2] = 0.0F) - (af[2][3] = 0.0F));
        strip_outer_parens_4((af[3][0] = 0.0F) - (af[3][1] = 0.0F) - (af[3][2] = 0.0F) - (af[3][3] = 0.0F));
    }

    public static void _ZEROMAT4i(int ai[][])
    {
        strip_outer_parens_4((ai[0][0] = 0) - (ai[0][1] = 0) - (ai[0][2] = 0) - (ai[0][3] = 0));
        strip_outer_parens_4((ai[1][0] = 0) - (ai[1][1] = 0) - (ai[1][2] = 0) - (ai[1][3] = 0));
        strip_outer_parens_4((ai[2][0] = 0) - (ai[2][1] = 0) - (ai[2][2] = 0) - (ai[2][3] = 0));
        strip_outer_parens_4((ai[3][0] = 0) - (ai[3][1] = 0) - (ai[3][2] = 0) - (ai[3][3] = 0));
    }

    public static void _IDENTMAT4(int ai[][])
    {
        strip_outer_parens_4((ai[0][0] = 0) - (ai[0][1] = 0) - (ai[0][2] = 0) - (ai[0][3] = 0));
        ai[0][0] = 1;
        strip_outer_parens_4((ai[1][0] = 0) - (ai[1][1] = 0) - (ai[1][2] = 0) - (ai[1][3] = 0));
        ai[1][1] = 1;
        strip_outer_parens_4((ai[2][0] = 0) - (ai[2][1] = 0) - (ai[2][2] = 0) - (ai[2][3] = 0));
        ai[2][2] = 1;
        strip_outer_parens_4((ai[3][0] = 0) - (ai[3][1] = 0) - (ai[3][2] = 0) - (ai[3][3] = 0));
        ai[3][3] = 1;
    }
    
    public static boolean _ISIDENTMAT4(int ai[][])
    {
        return ai[0][0] == 1 && ai[0][1] == 0 && ai[0][2] == 0 && ai[0][3] == 0
            && ai[1][0] == 0 && ai[1][1] == 1 && ai[1][2] == 0 && ai[1][3] == 0
            && ai[2][0] == 0 && ai[2][1] == 0 && ai[2][2] == 1 && ai[2][3] == 0
            && ai[3][0] == 0 && ai[3][1] == 0 && ai[3][2] == 0 && ai[3][3] == 1;
    }

    public static void _SETMAT4(int ai[][], int ai1[][])
    {
        strip_outer_parens_4((ai[0][0] = ai1[0][0]) - (ai[0][1] = ai1[0][1]) - (ai[0][2] = ai1[0][2]) - (ai[0][3] = ai1[0][3]));
        strip_outer_parens_4((ai[1][0] = ai1[1][0]) - (ai[1][1] = ai1[1][1]) - (ai[1][2] = ai1[1][2]) - (ai[1][3] = ai1[1][3]));
        strip_outer_parens_4((ai[2][0] = ai1[2][0]) - (ai[2][1] = ai1[2][1]) - (ai[2][2] = ai1[2][2]) - (ai[2][3] = ai1[2][3]));
        strip_outer_parens_4((ai[3][0] = ai1[3][0]) - (ai[3][1] = ai1[3][1]) - (ai[3][2] = ai1[3][2]) - (ai[3][3] = ai1[3][3]));
    }

    public static void _TRANSPOSE4(int ai[][], int ai1[][])
    {
        strip_outer_parens_4((ai[0][0] = ai1[0][0]) - (ai[0][1] = ai1[1][0]) - (ai[0][2] = ai1[2][0]) - (ai[0][3] = ai1[3][0]));
        strip_outer_parens_4((ai[1][0] = ai1[0][1]) - (ai[1][1] = ai1[1][1]) - (ai[1][2] = ai1[2][1]) - (ai[1][3] = ai1[3][1]));
        strip_outer_parens_4((ai[2][0] = ai1[0][2]) - (ai[2][1] = ai1[1][2]) - (ai[2][2] = ai1[2][2]) - (ai[2][3] = ai1[3][2]));
        strip_outer_parens_4((ai[3][0] = ai1[0][3]) - (ai[3][1] = ai1[1][3]) - (ai[3][2] = ai1[2][3]) - (ai[3][3] = ai1[3][3]));
    }

    public static void _TRANSPOSE4i(int ai[][], int ai1[][])
    {
        int ai2[][] = new int[4][4];
        _TRANSPOSE4(ai2, ai1);
        _SETMAT4(ai, ai2);
    }

    public static void _VXM4i(int ai[], int ai1[], int ai2[][])
    {
        int ai3[] = new int[4];
        _VXM4(ai3, ai1, ai2);
        _SET4(ai, ai3);
    }

    public static void _MXM4i(int ai[][], int ai1[][], int ai2[][])
    {
        int ai3[][] = new int[4][4];
        _MXM4(ai3, ai1, ai2);
        _SETMAT4(ai, ai3);
    }

    public static void _M2XM3(float af[][], float af1[][], float af2[][])
    {
        strip_outer_parens_2((af[0][0] = af1[0][0] * af2[0][0] + af1[0][1] * af2[1][0]) - (af[1][0] = af1[1][0] * af2[0][0] + af1[1][1] * af2[1][0]));
        af[2][0] = af2[2][0];
        strip_outer_parens_2((af[0][1] = af1[0][0] * af2[0][1] + af1[0][1] * af2[1][1]) - (af[1][1] = af1[1][0] * af2[0][1] + af1[1][1] * af2[1][1]));
        af[2][1] = af2[2][1];
        strip_outer_parens_2((af[0][2] = af1[0][0] * af2[0][2] + af1[0][1] * af2[1][2]) - (af[1][2] = af1[1][0] * af2[0][2] + af1[1][1] * af2[1][2]));
        af[2][2] = af2[2][2];
    }

    public static void _M4XM3(float af[][], int ai[][], float af1[][])
    {
        strip_outer_parens_3((af[0][0] = (float)ai[0][0] * af1[0][0] + (float)ai[0][1] * af1[1][0] + (float)ai[0][2] * af1[2][0]) - (af[0][1] = (float)ai[0][0] * af1[0][1] + (float)ai[0][1] * af1[1][1] + (float)ai[0][2] * af1[2][1]) - (af[0][2] = (float)ai[0][0] * af1[0][2] + (float)ai[0][1] * af1[1][2] + (float)ai[0][2] * af1[2][2]));
        af[0][3] = ai[0][3];
        strip_outer_parens_3((af[1][0] = (float)ai[1][0] * af1[0][0] + (float)ai[1][1] * af1[1][0] + (float)ai[1][2] * af1[2][0]) - (af[1][1] = (float)ai[1][0] * af1[0][1] + (float)ai[1][1] * af1[1][1] + (float)ai[1][2] * af1[2][1]) - (af[1][2] = (float)ai[1][0] * af1[0][2] + (float)ai[1][1] * af1[1][2] + (float)ai[1][2] * af1[2][2]));
        af[1][3] = ai[1][3];
        strip_outer_parens_3((af[2][0] = (float)ai[2][0] * af1[0][0] + (float)ai[2][1] * af1[1][0] + (float)ai[2][2] * af1[2][0]) - (af[2][1] = (float)ai[2][0] * af1[0][1] + (float)ai[2][1] * af1[1][1] + (float)ai[2][2] * af1[2][1]) - (af[2][2] = (float)ai[2][0] * af1[0][2] + (float)ai[2][1] * af1[1][2] + (float)ai[2][2] * af1[2][2]));
        af[2][3] = ai[2][3];
        strip_outer_parens_3((af[3][0] = (float)ai[3][0] * af1[0][0] + (float)ai[3][1] * af1[1][0] + (float)ai[3][2] * af1[2][0]) - (af[3][1] = (float)ai[3][0] * af1[0][1] + (float)ai[3][1] * af1[1][1] + (float)ai[3][2] * af1[2][1]) - (af[3][2] = (float)ai[3][0] * af1[0][2] + (float)ai[3][1] * af1[1][2] + (float)ai[3][2] * af1[2][2]));
        af[3][3] = ai[3][3];
    }

    public static void _M2XM3r(float af[][], float af1[][], float af2[][])
    {
        float af3[][] = new float[3][3];
        _M2XM3(af3, af1, af2);
        _SETMAT3(af, af3);
    }

    public static void _MXM3r(float af[][], float af1[][], float af2[][])
    {
        float af3[][] = new float[3][3];
        _MXM3(af3, af1, af2);
        _SETMAT3(af, af3);
    }

    public static void _ROUNDMAT4(int ai[][], float af[][])
    {
        for(int i = 0; i < 4; i++)
        {
            for(int j = 0; j < 4; j++)
                ai[i][j] = Math.round(af[i][j]);

        }

    }

    public static void _SXVPSXV(int[] result, int s0, int[] v0, int s1, int[] v1) {
        for(int i=0; i<v0.length; i++)
            result[i] = s0*v0[i] + s1*v1[i];
    }

    public static void _VXVXV4(int[] result, int[]a, int[]b, int[]c) {
        result[0] = -((a)[1] * ((b)[2] * ((c)[3]) + (b)[3] * -((c)[2])) + (a)[2] * -((b)[1] * ((c)[3]) + (b)[3] * -((c)[1])) + (a)[3] * ((b)[1] * ((c)[2]) + (b)[2] * -((c)[1])));
        result[1] =  ((a)[0] * ((b)[2] * ((c)[3]) + (b)[3] * -((c)[2])) + (a)[2] * -((b)[0] * ((c)[3]) + (b)[3] * -((c)[0])) + (a)[3] * ((b)[0] * ((c)[2]) + (b)[2] * -((c)[0])));
        result[2] = -((a)[0] * ((b)[1] * ((c)[3]) + (b)[3] * -((c)[1])) + (a)[1] * -((b)[0] * ((c)[3]) + (b)[3] * -((c)[0])) + (a)[3] * ((b)[0] * ((c)[1]) + (b)[1] * -((c)[0])));
        result[3] =  ((a)[0] * ((b)[1] * ((c)[2]) + (b)[2] * -((c)[1])) + (a)[1] * -((b)[0] * ((c)[2]) + (b)[2] * -((c)[0])) + (a)[2] * ((b)[0] * ((c)[1]) + (b)[1] * -((c)[0])));
    }

    public static int _DET4(int M[][])
    {
        // This is machine-generated
        // (from the C preprocessor output from vec.h)
        // so they are probably right.
        return M[0][0] * (M[1][1] * (M[2][2] * M[3][3] - M[2][3] * M[3][2])
                            - M[1][2] * (M[2][1] * M[3][3] - M[2][3] * M[3][1])
                            + M[1][3] * (M[2][1] * M[3][2] - M[2][2] * M[3][1]))
                 - M[0][1] * (M[1][0] * (M[2][2] * M[3][3] - M[2][3] * M[3][2])
                            - M[1][2] * (M[2][0] * M[3][3] - M[2][3] * M[3][0])
                            + M[1][3] * (M[2][0] * M[3][2] - M[2][2] * M[3][0]))
                 + M[0][2] * (M[1][0] * (M[2][1] * M[3][3] - M[2][3] * M[3][1])
                            - M[1][1] * (M[2][0] * M[3][3] - M[2][3] * M[3][0])
                            + M[1][3] * (M[2][0] * M[3][1] - M[2][1] * M[3][0]))
                 - M[0][3] * (M[1][0] * (M[2][1] * M[3][2] - M[2][2] * M[3][1])
                            - M[1][1] * (M[2][0] * M[3][2] - M[2][2] * M[3][0])
                            + M[1][2] * (M[2][0] * M[3][1] - M[2][1] * M[3][0]));
    }

    private Vec_h(){}
}
