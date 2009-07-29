
//
// NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
// AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Contains some high-level static methods built on top of Vec_h.java
 * to support the MagicCube4D application.
 * 
 * Copyright 2005 - Superliminal Software
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
        if((af[0] == 0.0F ? 0 : 1) + (af[1] == 0.0F ? 0 : 1) + (af[2] == 0.0F ? 0 : 1) + (af[3] == 0.0F ? 0 : 1) != 1)
            _assertionFailed("(center[0]!=0 ? 1 : 0) + (center[1]!=0 ? 1 : 0) + (center[2]!=0 ? 1 : 0) + (center[3]!=0 ? 1 : 0) == 1", "Math4d.prejava", 40);
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
        } else
        if(j > 0)
        {
            ai[3][3] = -1;
            ai[2][2] = -1;
        }
    }

    private static boolean VEQVXS3(int ai[], int ai1[], int i)
    {
        return ai[0] == ai1[0] * i && ai[1] == ai1[1] * i && ai[2] == ai1[2] * i;
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
            float f1 = (float)Math.sqrt(Vec_h._NORMSQRD3(af1[i]));
            Vec_h._VDS3(af1[i], af1[i], f1);
        }

        Vec_h._TRANSPOSE3(af2, af1);
        af3[0][0] = (float)Math.cos(f);
        af3[0][1] = (float)Math.sin(f);
        Vec_h._XV2(af3[1], af3[0]);
        Vec_h._M2XM3r(af1, af3, af1);
        Vec_h._MXM3r(af1, af2, af1);
    }



    /*
     * TODO: find a better way to do this.
     */
    public static boolean get4dMatThatRotatesThese3ToThose3(
                                int these0[], int these1[], int these2[], /* INPUTS */
                                int those0[], int those1[], int those2[], int mat[][]) /* OUTPUTS */
    {
        int i, j, k, l, ii, temp[] = new int[4];
        int these[][] = new int[4][4], those[][] = new int[4][4];   /* actually [3][4] */
        int transpthese[][] = new int[4][4], transpthose[][] = new int[4][4];   /* actually [4][3] */

        Vec_h._SET4(these[0], these0);
        Vec_h._SET4(these[1], these1);
        Vec_h._SET4(these[2], these2);
        Vec_h._TRANSPOSE4(transpthese, these);
        Vec_h._SET4(those[0], those0);
        Vec_h._SET4(those[1], those1);
        Vec_h._SET4(those[2], those2);
        Vec_h._TRANSPOSE4(transpthose, those);

        Vec_h._ZEROMAT4i(mat);
        for (i = 0; i < 4; ++i) {
            for (mat[0][i] = 1; mat[0][i] >= -1; mat[0][i] -= 2) {
                if (!VEQVXS3(transpthese[0], transpthose[i], mat[0][i]))
                    continue;
                for (j = 0; j < 4; ++j) {
                    if (j == i)
                        continue;
                    for (mat[1][j] = 1; mat[1][j] >= -1; mat[1][j] -= 2) {
                        if (!VEQVXS3(transpthese[1], transpthose[j], mat[1][j]))
                            continue;
                        for (k = 0; k < 4; ++k) {
                            if (k == i || k == j)
                                continue;
                            for (mat[2][k] = 1; mat[2][k] >= -1; mat[2][k] -= 2) {
                                if (!VEQVXS3(transpthese[2],
                                    transpthose[k], mat[2][k]))
                                    continue;
                                l = 6 - i - j - k;
                                for (mat[3][l] = 1; mat[3][l] >= -1;
                                     mat[3][l] -= 2) {
                                    if (!VEQVXS3(transpthese[3],
                                        transpthose[l], mat[3][l]))
                                        continue;
                                    /*
                                     * Found a good matrix!!
                                     * Assert that it works.
                                     */
                                    for (ii = 0; ii < 3; ++ii) {
                                        Vec_h._VXM4(temp, these[ii], mat);
                                        assert(Vec_h._EQVEC4(temp, those[ii]));
                                    }
                                    return true;
                                }
                                mat[3][l] = 0;
                            }
                            mat[2][k] = 0;
                        }
                    }
                    mat[1][j] = 0;
                }
            }
            mat[0][i] = 0;
        }
        return false;
    }

}
