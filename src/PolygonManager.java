

/**
 * Copyright 2005 - Superliminal Software
 * @author Don Hatch
 */
public class PolygonManager {

	// To disallow instantiation of this (now) static utility class.
    private PolygonManager() {}

//    private static boolean are_CCW(float v0[], float v1[], float v2[]) {
//        return twice_triangle_area(v0,v1,v2) > 0;
//    }
    @SuppressWarnings("unused")
	private static boolean are_CW( float v0[], float v1[], float v2[]) {
        return twice_triangle_area(v0,v1,v2) < 0;
    }

//    /* HA! the following are reversed from the above, since we inverted Y */
//    private static boolean short_are_CCW(float v0[], float v1[], float v2[]) {
//        return short_twice_triangle_area(v0, v1, v2) < 0;
//    }
    @SuppressWarnings("unused")
	private static boolean short_are_CW( float v0[], float v1[], float v2[]) {
        return short_twice_triangle_area(v0, v1, v2) > 0;
    }
    
    private static float tmpTWAf1[] = new float[2], tmpTWAf2[] = new float[2]; // scratch vars
    private static float twice_triangle_area(float v0[], float v1[], float v2[]) {
        //float tmpTNf1[] = new float[2], tmpTNf2[] = new float[2];
        Vec_h._VMV2(tmpTWAf1, v1, v0);
        Vec_h._VMV2(tmpTWAf2, v2, v0);
        return Vec_h._VXV2(tmpTWAf1, tmpTWAf2);
    }
    private static float tmpTWAs1[] = new float[2], tmpTWAs2[] = new float[2]; // scratch vars
    private static float short_twice_triangle_area(float v0[], float v1[], float v2[]) {
        //short tmpTNf1[] = new short[2], tmpTNf2[] = new short[2];
        Vec_h._VMV2(tmpTWAs1, v1, v0);
        Vec_h._VMV2(tmpTWAs2, v2, v0);
        return Vec_h._VXV2(tmpTWAs1, tmpTWAs2);
    }
    private static float tmpTNf1[] = new float[3], tmpTNf2[] = new float[3]; // scratch vars
	public static void get_triangle_normal(float v0[], float v1[], float v2[], float nrml[]) {
	    //float tmpTNf1[] = new float[3], tmpTNf2[] = new float[3];
	    Vec_h._VMV3(tmpTNf1, v1, v0);
	    Vec_h._VMV3(tmpTNf2, v2, v0);
	    Vec_h._VXV3(nrml, tmpTNf1, tmpTNf2);
	    /* normalize */
	    float tomultiplyby = 1 / (float)Math.sqrt(Vec_h._NORMSQRD3(nrml));
	    Vec_h._VXS3(nrml, nrml, tomultiplyby);
	}


	public static void getrotmat(int ax, float angle, float mat3[][]) {
	    float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
	    int fromax = (ax + 1) % 3, toax = (ax + 2) % 3;

	    Vec_h._ZEROMAT3(mat3);
	    mat3[ax][ax] = 1;
	    mat3[fromax][fromax] = c;
	    mat3[fromax][toax] = s;
	    mat3[toax][fromax] = -s;
	    mat3[toax][(ax + 2) % 3] = c;
	}
    

	public static float getTwistTotalAngle(int dim, int dir) {
	    switch (dim) {
    	    case 0: return (float)(2 * Math.PI / 3 * dir); // vertex; 120 degrees
    	    case 1: return (float)(2 * Math.PI / 2 * dir); // vertex; 180 degrees
    	    case 2: return (float)(2 * Math.PI / 4 * dir); // vertex; 90 degrees
    	    default:
    //	        assert(INRANGE(0 <=, dim, <2));
    	        break;
	    }
	    return (float)(2 * Math.PI);              /* this never happens */
	}

	/*
	 * The following is pretty arbitrary... there might be a more natural way.
	 */
	private final static int facetoaxis[] = { 
		MagicCube.W, 
		MagicCube.Z, 
		MagicCube.Y, 
		MagicCube.X, 
		MagicCube.X, 
		MagicCube.Y,
		MagicCube.Z, 
		MagicCube.W,
    };
    private final static int facetosign[] = {
        -1, -1, -1, -1, 
         1,  1,  1,  1, 
    };
    private final static int axis_and_sign_to_face[][] = { 
        {3, 4}, 
        {2, 5}, 
        {1, 6}, 
        {0, 7}, 
    };
    private static int FACETOAXIS(int f) { return facetoaxis[f]; }
    private static int FACETOSIGN(int f) { return facetosign[f]; }
    private static int AXIS_AND_SIGN_TO_FACE(int a, int s) { return axis_and_sign_to_face[a][(s+1)/2]; }

	public static int faceOfGrip(int id) {
	    MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
	    grip.id_within_puzzle = id;
	    fillStickerspecFromIdAndLength(grip, 3);
	    return grip.face;
	}

	public static void fillStickerspecFromIdAndLength(MagicCube.Stickerspec sticker, int length) {
	    int i, ax, sgn, id, face0coords[] = new int[4], mat[][] = new int[4][4];
	    sticker.id_within_face = sticker.id_within_puzzle % intpow(length, MagicCube.NDIMS - 1);
	    sticker.face = sticker.id_within_puzzle / intpow(length, MagicCube.NDIMS - 1);
	    ax = FACETOAXIS(sticker.face);
	    sgn = FACETOSIGN(sticker.face);
	    Math4d.getCanonicalMatThatTakesAxisToMinusW(ax, sgn, mat);
        Vec_h._TRANSPOSE4i(mat, mat);      /* want to go in the opposite direction */
	    id = sticker.id_within_face;
	    face0coords[MagicCube.X] = -(length - 1) + 2 * (id % length);
	    face0coords[MagicCube.Y] = -(length - 1) + 2 * ((id / length) % length);
	    face0coords[MagicCube.Z] = -(length - 1) + 2 * ((id / length / length) % length);
	    face0coords[MagicCube.W] = -length;
	    Vec_h._VXM4(sticker.coords, face0coords, mat);
//	    assert(sticker.coords[ax] == sgn * length);

	    /* sticker.depth = 0; */
	    sticker.dim = MagicCube.NDIMS - 1;
	    for (i = 0; i < MagicCube.NDIMS; ++i) {
	        if (Math.abs(sticker.coords[i]) == length - 1)
	            sticker.dim--;
	        /* sticker.depth = MAX(sticker.depth,
	           length-1-ABS(sticker.coords[i])); */
	    }
	}

    public static void fillStickerspecFromFaceAndIdAndLength(MagicCube.Stickerspec sticker, int length) {
	    int face = sticker.face;
	    int id_within_face = sticker.id_within_face;
	    sticker.id_within_puzzle = face * length * length * length + id_within_face;
	    fillStickerspecFromIdAndLength(sticker, length);
//	    assert(sticker.face == face);
//	    assert(sticker.id_within_face == id_within_face);
	}

	public static void fillStickerspecFromCoordsAndLength(MagicCube.Stickerspec sticker, int length) {
	    int i, ax = 0, sgn, mat[][] = new int[4][4], newcoords[] = new int[4];

	    sticker.dim = MagicCube.NDIMS - 1;
	    /* sticker.depth = 0; */
	    for (i = 0; i < MagicCube.NDIMS; ++i) {
	        if (Math.abs(sticker.coords[i]) == length)
	            ax = i;
	        else if (Math.abs(sticker.coords[i]) == length - 1)
	            sticker.dim--;
	        /* sticker.depth = MAX(sticker.depth,
	           length-1-ABS(sticker.coords[i])); */
	    }
//	    assert(INRANGE(X <=, ax, <=W));
	    sgn = MagicCube.SGN(sticker.coords[ax]);
	    sticker.face = AXIS_AND_SIGN_TO_FACE(ax, sgn);
	    Math4d.getCanonicalMatThatTakesAxisToMinusW(ax, sgn, mat);
	    Vec_h._VXM4(newcoords, sticker.coords, mat);
//	    assert(newcoords[MagicCube.W] == -length);

	    sticker.id_within_face = 0;
	    for (i = MagicCube.NDIMS - 2; i >= 0; --i)
	    {
	        sticker.id_within_face *= length;
	        sticker.id_within_face += (newcoords[i] + length - 1) / 2;
	    }
//	    assert(INRANGE(0 <=, sticker.id_within_face, <intpow(length, NDIMS - 1)));
	    sticker.id_within_puzzle = sticker.face * intpow(length, MagicCube.NDIMS - 1) + sticker.id_within_face;
	}

    public static int intpow(int i, int j) {
        return j != 0 ? intpow(i, j - 1) * i : 1;
    }
    
}
