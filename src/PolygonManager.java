
//
// NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
// AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Contains methods to compute sets of polygons representing a MC4D puzzle 
 * as well as methods that return information about puzzle parts under 
 * given screen points (picking).
 * 
 * Both 3D and 2D polygons can be produced. 2D polygons are produced by first
 * computing the 3D polygons which are then transformed, lighted, projected
 * into 2D and sorted from back to front. The transforms are specified by
 * both the tilt/twirl settings as well as by rotation matrices passed to the
 * 2D polygon methods.
 * 
 * Picking methods exist that return either a particular sticker hit or
 * a virtual sticker called a "grip" which represents a twist axis.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Don Hatch
 */
public class PolygonManager {
	private interface InterpFunc {
		public float func(float f);
	}
    private static InterpFunc sine_interp = new InterpFunc() {
        public float func(float x) { return (float)(Math.sin((x - .5) * Math.PI) + 1) / 2; }
    };
    
	private int length = MagicCube.DEFAULT_LENGTH;
    public int getLength() { return length; }
	private float 
		tilt  = MagicCube.DTOR(MagicCube.TILT), 
		twirl = MagicCube.DTOR(MagicCube.TWIRL),
        eyeW  = MagicCube.EYEW;
	public float getEyeW() { return eyeW; }
	private float bbox[][] = new float[2][2];
	private int n_untwisted_verts4;
	private float untwisted_verts4[][] = new float[MagicCube.MAXVERTS][4];
	private float faceshrink = MagicCube.FACESHRINK;
    public float getFaceShrink() { return faceshrink; }
    private float stickershrink = MagicCube.STICKERSHRINK;
    public float getStickerShrink() { return stickershrink; }
    private float twistfactor = 1;
    public float getTwistFactor() { return twistfactor; }
	private int  /* FIX THIS-- decide on a consistent way to define variables */
        nframes_90  = MagicCube.NFRAMES_90, 
        nframes_120 = MagicCube.NFRAMES_120,
        nframes_180 = MagicCube.NFRAMES_180;  
    /*
     * Yes, these should probably be in a data structure:
     */
	private int  n_untwisted_verts3;
	private float untwisted_verts3[][] = new float[MagicCube.MAXVERTS][3];
    private int  n_untwisted_quads3;
    private int  untwisted_quads3[][] = new int[MagicCube.MAXQUADS][4];
    private int  untwisted_stickerids3[] = new int[MagicCube.MAXSTICKERS];
    /* NOTE stickerids3 is indexed by verts/8 whereas quadids2 is indexed by stickers */

    private static boolean are_CCW(float v0[], float v1[], float v2[]) {
        return twice_triangle_area(v0,v1,v2) > 0;
    }
    @SuppressWarnings("unused")
	private static boolean are_CW( float v0[], float v1[], float v2[]) {
        return twice_triangle_area(v0,v1,v2) < 0;
    }

    /* HA! the following are reversed from the above, since we inverted Y */
    private static boolean short_are_CCW(float v0[], float v1[], float v2[]) {
        return short_twice_triangle_area(v0, v1, v2) < 0;
    }
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

    public PolygonManager(int length) {
        reset(length);
    }
    
	public PolygonManager() {
//	    tilt = DTOR(prefs.getRealProperty(M4D_TILT, TILT));
//	    twirl = DTOR(prefs.getRealProperty(M4D_TWIRL, TWIRL));
//	    faceshrink = prefs.getRealProperty(M4D_FACESHRINK, FACESHRINK);
//	    stickershrink = prefs.getRealProperty(M4D_STICKERSHRINK, STICKERSHRINK);
//	    nframes_90 = prefs.getIntProperty(M4D_NFRAMES_90, NFRAMES_90);
//	    nframes_120 = prefs.getIntProperty(M4D_NFRAMES_120, NFRAMES_120);
//	    nframes_180 = prefs.getIntProperty(M4D_NFRAMES_180, NFRAMES_180);
//
	    this(3); //prefs.getLength());
	}

	public void reset(int new_length) {
	    if (new_length != -1)
	    {
	        length = new_length;

	        //bbox[0][0] = 0;
	        n_untwisted_verts4 = getUntwistedVerts4(untwisted_verts4, faceshrink, stickershrink);
	        
	        int nv[] = new int[1], nq[] = new int[1];
	        calc3DFrameInfo(null, MagicCube.CCW,
	            0, /* slicesmask-- so everything else is ignored */
	            0f, sine_interp, n_untwisted_verts4, untwisted_verts4,
	            eyeW, // preferences.getRealProperty(M4D_EYEW, EYEW),
	            true, //!preferences.getBoolProperty(M4D_NOCULLCELLS), /* cullcells */
	            nv, untwisted_verts3,
	            nq, untwisted_quads3,
	            untwisted_stickerids3, null);
	        n_untwisted_verts3 = nv[0];
	        n_untwisted_quads3 = nq[0];
	    }
	}

	public void setShrinkers(float face_val, float sticker_val) {
	    faceshrink = face_val;
	    stickershrink = sticker_val;
	    //n_untwisted_verts4 = getUntwistedVerts4(untwisted_verts4, faceshrink, stickershrink);
        reset(length);
	}
    public void setFaceShrink(float face_val) {
        faceshrink = face_val;
        reset(length);
    }
    public void setStickerShrink(float sticker_val) {
        stickershrink = sticker_val;
        reset(length);
    }
    public void setEyeW(float eyew_val) {
        eyeW = eyew_val;
        reset(length);
    }
    public void setTwistFactor(float factor) {
        twistfactor = factor;
//        System.out.println("nframes 120: " + scaledNFrames(nframes_120, twistfactor));
//        System.out.println("nframes 180: " + scaledNFrames(nframes_180, twistfactor));
//        System.out.println("nframes 90: " + scaledNFrames(nframes_90, twistfactor));
    }


    private static class ZAndIndex implements Sort.Comparable {
        private float z;
        private int i;
        private static float tmpVert[] = new float[3];
        @SuppressWarnings("unused")
		public ZAndIndex(float qv[][], int i) {
            set(qv, i);
        }
        public ZAndIndex() {}
        public void set(float qv[][], int i) {
            this.z = quadCenter(qv);
            this.i = i;            
        }
        public int compareTo(Object obj) {
            float diff = this.z - ((ZAndIndex)obj).z;
            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
        private static float quadCenter(float qv[][]) {
            return qv[0][MagicCube.Z] + qv[2][MagicCube.Z];
        }
        // a failed experiment?
        @SuppressWarnings("unused")
		private static float nearClosestVert(float qv[][]) {
            int closest = 0;
            for(int v=1; v<4; v++)
                if(qv[v][MagicCube.Z] > qv[closest][MagicCube.Z])
                    closest = v;
            Vec_h._VMV3(tmpVert, qv[closest], qv[(closest+2)%4]);
            float len = (float)Math.sqrt(Vec_h._NORMSQRD3(tmpVert));
            Vec_h._VDS3(tmpVert, tmpVert, len*1000); // make short, standard length
            Vec_h._VPV3(tmpVert, qv[closest], tmpVert);
            return tmpVert[MagicCube.Z];
        }
    }
    
    private float tmpTV3[][] = new float[MagicCube.MAXVERTS][3]; // scratch var
    private int tempquads[][] = new int[MagicCube.MAXQUADS][4]; // scratch var
    private int tempquadids[] = new int[MagicCube.MAXQUADS]; // scratch var
    private float tempbrightnesses[] = new float[MagicCube.MAXQUADS]; // scratch var
    private ZAndIndex azandindex[];  
    
	public void calc3DTo2DFrameInfo(
	    /* INPUTS */
	    int nverts3, float verts3[][], int nquads3,
	    int quads3[][], int stickerids3[], float tilt,
	    float twirl, float eyez, 
        float sunvec[], boolean sunvec_is_in_objspace, 
        float usermat[][],
        boolean cullverts, boolean cullbackfaces, boolean zsort,
	    /* OUTPUTS */
	    int Nverts2[], float verts2[][], int Nquads2[],
	    int quads2[][], int quadids2[],
	    float brightnesses[])
	{
	    int i;
	    float
	    	twirlmat[][] = new float[3][3], 
	    	tiltmat[][] = new float[3][3], 
	    	viewingmat[][] = new float[3][3];
	    float objspace_sunvec[] = new float[3], triangle_normal[] = new float[3];
	    float tomultiplyby, norm;
	    int nverts2, nquads2;   /* tmp vars. set Nverts2 and Nquads2 to these at end */

	    /*
	     * make a viewing matrix that tilts and twirls
	     */
	    getrotmat(MagicCube.Y, twirl, twirlmat);
	    getrotmat(MagicCube.X, tilt, tiltmat);
	    Vec_h._MXM3(viewingmat, twirlmat, tiltmat);
        
        /*
         * append any given view transform.
         */
        if(usermat != null) {
            float viewcopy[][] = new float[3][3];
            Vec_h._SETMAT3(viewcopy, viewingmat);
            Vec_h._MXM3(viewingmat, viewcopy, usermat);
        }

	    /*
	     * get normalized sunvec in object space
	     * (it's more efficient to transform the light vector to object space
	     * than to transform all the face normals to world space)
	     */
	    if (sunvec!=null && brightnesses!=null) {
	        if (sunvec_is_in_objspace)
	            Vec_h._SET3(objspace_sunvec, sunvec);
	        else /* apply inverse of viewingmat to sunvec to get objspace_sunvec */
                Vec_h._MXV3(objspace_sunvec, viewingmat, sunvec);
	        norm = (float) Math.sqrt(Vec_h._NORMSQRD3(objspace_sunvec));
            Vec_h._VDS3(objspace_sunvec, objspace_sunvec, norm);
	    }

	    /*
	     * transform verts
	     */
	    for (i = 0; i < nverts3; ++i) {
            Vec_h._VXM3(tmpTV3[i], verts3[i], viewingmat);
	        tomultiplyby = (float) 1. / (eyez - tmpTV3[i][MagicCube.Z]);
            Vec_h._VXS2(verts2[i], tmpTV3[i], tomultiplyby);
	    }

	    /*
	     * Copy quads3 to quads2
	     * and stickerids3 to quadids2,
	     * discarding back faces
	     */
	    nquads2 = 0;
	    for (i = 0; i < nquads3; ++i) {
	        if (cullbackfaces && !are_CCW(verts2[quads3[i][0]],
	                                      verts2[quads3[i][1]],
	                                      verts2[quads3[i][2]]))
	        {
	            continue;
	        }
	        Vec_h._SET4(quads2[nquads2], quads3[i]);
	        quadids2[nquads2] = (stickerids3[quads3[i][0] / 8]) * 6 + (i % 6);
	        if (sunvec!=null && brightnesses!=null) {
	            get_triangle_normal(verts3[quads3[i][0]],
	                                verts3[quads3[i][1]],
	                                verts3[quads3[i][2]], triangle_normal);
	            brightnesses[nquads2] = Vec_h._DOT3(triangle_normal, objspace_sunvec);
	            if (brightnesses[nquads2] < 0)
	                brightnesses[nquads2] = 0;
	        }
	        nquads2++;
	    }

	    nverts2 = nverts3;

	    /*
	     * Zsort the quads, quadids and brighnesses, from least to greatest z
	     */
	    if (zsort) {
            if(azandindex == null || azandindex.length < nquads2) { // initialize scratch array
                azandindex = new ZAndIndex[MagicCube.totalQuads(length)];
                for(int i1 = 0; i1 < azandindex.length; i1++)
                    azandindex[i1] = new ZAndIndex();
            }
            float tmpquads[][] = new float[4][];
            for(int i1 = 0; i1 < nquads2; i1++) {
                int qi[] = quads2[i1];
                for(int j=0; j<4; j++)
                    tmpquads[j] = tmpTV3[qi[j]];
                azandindex[i1].set(tmpquads, i1);
            }
            Sort.sort(azandindex, 0, nquads2-1);
            for(int i2=0; i2<nquads2; i2++) {
                int sortid = azandindex[i2].i;
                Vec_h._SET4(tempquads[i2], quads2[sortid]);
                tempquadids[i2] = quadids2[sortid];
                tempbrightnesses[i2] = brightnesses[sortid];
            }
            for(int i3=0; i3<nquads2; i3++) {
                Vec_h._SET4(quads2[i3], tempquads[i3]);
                quadids2[i3] = tempquadids[i3];
                brightnesses[i3] = tempbrightnesses[i3];
            }
	    }
	    if (cullverts) {
	        /*
	         * FIX THIS-- should discard unused vertices here if we are
	         * planning to store the frame in a file or something.
	         */
	    }

	    Nverts2[0] = nverts2;
	    Nquads2[0] = nquads2;
	} // end calc3DTo2DFrameInfo


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

    private static int scaledNFrames(int baseframes, float sf) {
        return Math.round(baseframes * sf);
    }

    public int getTwistNFrames(MagicCube.TwistData twist) {
        float multiplier = twistfactor * Math.abs(twist.direction);
        switch (twist.grip.dim) {
    	    case 0: return scaledNFrames(nframes_120, multiplier);
    	    case 1: return scaledNFrames(nframes_180, multiplier);
    	    case 2: return scaledNFrames(nframes_90,  multiplier);
    	    default:
    	        //assert(INRANGE(0 <=, grip.dim, <2));
    	        break;
	    }
	    return scaledNFrames(nframes_90, multiplier);          /* this never happens */
	}


	/*
	 * This procedure is hopelessly confusing, but it's private and it works.
	 * Note there are two functions here that are almost identical and might be refactored.
	 */
	private int makeRangesReal(int slicesmask, int sgn, float ranges[][]) {
	    int i, nranges;
	    float temp;

	    nranges = 0;
	    for (i = 0; i < length; ++i) {
	        if (MagicCube.BIT(slicesmask, i)) {
	            if (i == 0 || !MagicCube.BIT(slicesmask, i - 1))
	                ranges[nranges++][0] = (length - 2 * i) * faceshrink;
	            ranges[nranges - 1][1] = (length - 2 * (i + 1)) * faceshrink;
	        }
	    }
	    if (MagicCube.BIT(slicesmask, 0))
	        ranges[0][0] += 2 * length; /* bigger than any vertex */
	    if (MagicCube.BIT(slicesmask, length - 1))
	        ranges[nranges - 1][1] -= 2 * length;   /* smaller than any vertex */

	    if (sgn > 0) /* then the ranges are right but in the wrong order */
	        for (i = 0; i < nranges; ++i) {
	        	temp = ranges[i][0];
	        	ranges[i][0] = ranges[nranges-1-i][1];
	        	ranges[nranges-1-i][1] = temp;
	        }
	    else                        /* the signs of the ranges are wrong */
	        for (i = 0; i < nranges; ++i)
	            Vec_h._VXS2(ranges[i], ranges[i], -1);

	    return nranges;
	} // end makeRangesReal

    /**
     * I'm not even sure what this does but it's currently used by PuzzleState.java
     * so I'm making it purposely package protected for now and hope the dependancy
     * can be removed and this method made private.
     */
	int makeRangesInt(int slicesmask, int sgn, int ranges[][]) {
	    int i, nranges;
	    int temp;

	    nranges = 0;
	    for (i = 0; i < length; ++i) {
	        if (MagicCube.BIT(slicesmask, i)) { 
	        	if (i == 0 || !MagicCube.BIT(slicesmask, i - 1))
	                ranges[nranges++][0] = (length - 2 * i);
	            ranges[nranges - 1][1] = (length - 2 * (i + 1));
	        }
	    }
	    if (MagicCube.BIT(slicesmask, 0))
	        ranges[0][0] += 2 * length; /* bigger than any vertex */
	    if (MagicCube.BIT(slicesmask, length - 1))
	        ranges[nranges - 1][1] -= 2 * length;   /* smaller than any vertex */

	    if (sgn > 0) /* then the ranges are right but in the wrong order */
	        for (i = 0; i < nranges; ++i) {
	            temp = ranges[i][0];
	        	ranges[i][0] = ranges[nranges - 1 - i][1];
	        	ranges[nranges - 1 - i][1] = temp;
	        }
	    else  /* the signs of the ranges are wrong */
	        for (i = 0; i < nranges; ++i)
	            Vec_h._VXS2(ranges[i], ranges[i], -1);

	    return nranges;
	} // end makeRangesInt

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

	public static int oppositeFace(int f) {
	    return MagicCube.NFACES - 1 - f;
	}

	public static int faceOfGrip(int id) {
	    MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
	    grip.id_within_cube = id;
	    fillStickerspecFromIdAndLength(grip, 3);
	    return grip.face;
	}

	public static void fillStickerspecFromIdAndLength(MagicCube.Stickerspec sticker, int length) {
	    int i, ax, sgn, id, face0coords[] = new int[4], mat[][] = new int[4][4];
	    sticker.id_within_face = sticker.id_within_cube % intpow(length, MagicCube.NDIMS - 1);
	    sticker.face = sticker.id_within_cube / intpow(length, MagicCube.NDIMS - 1);
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

    public void fillStickerspecFromId(MagicCube.Stickerspec sticker) {
	    fillStickerspecFromIdAndLength(sticker, length);
	}

    public static void fillStickerspecFromFaceAndIdAndLength(MagicCube.Stickerspec sticker, int length) {
	    int face = sticker.face;
	    int id_within_face = sticker.id_within_face;
	    sticker.id_within_cube = face * length * length * length + id_within_face;
	    fillStickerspecFromIdAndLength(sticker, length);
//	    assert(sticker.face == face);
//	    assert(sticker.id_within_face == id_within_face);
	}

    public void fillStickerspecFromFaceAndId(MagicCube.Stickerspec sticker) {
	    fillStickerspecFromFaceAndIdAndLength(sticker, length);
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
	    sticker.id_within_cube = sticker.face * intpow(length, MagicCube.NDIMS - 1) + sticker.id_within_face;
	}

    public void fillStickerspecFromCoords(MagicCube.Stickerspec sticker) {
	    fillStickerspecFromCoordsAndLength(sticker, length);
	}


	/*
	 * Get a grip for which a CCW twist is the same transformation
	 * as the given face-to-center rotation.
	 * @return true if this is a legal rotation, false otherwise.
	 */
	public static boolean facetocenterToGrip(int facetocenter, MagicCube.Stickerspec grip) {
	    int
            ax  = FACETOAXIS(facetocenter),
            sgn = FACETOSIGN(facetocenter);
	    if (!(MagicCube.X <= ax && ax <=MagicCube.Z ))
	        return false;

	    grip.coords[ax] = 0;
	    grip.coords[(ax + 1) % 3] = 3; // axis of face on which sticker lies
	    grip.coords[(ax + 2) % 3] = sgn * -(3 - 1); // it's magic
	    grip.coords[MagicCube.W] = 0;

	    fillStickerspecFromCoordsAndLength(grip, 3);

	    return true;
	}

    private float tmpC2DFIVerts3[][] = new float[MagicCube.MAXVERTS][3]; // scratch var
    private int tmpC2DFIQuads3[][] = new int[MagicCube.MAXQUADS][4]; // scratch var
    private int tmpC2DFISids3[] = new int[MagicCube.MAXSTICKERS]; // scratch var
    
	public void calc2DFrameInfo(
	    /* INPUTS */
	    MagicCube.Stickerspec sticker, int dir,
	    int slicesmask, float frac, InterpFunc interp,
	    int nverts4, float untwisted_verts4[][],
	    float eyew, boolean cullcells, float tilt, float twirl,
	    float eyez, float sunvec[], boolean sunvec_is_in_objspace,
        float usermat[][],
        boolean cullverts, boolean cullbackfaces, boolean zsort,
	    /* OUTPUTS */
	    int Nverts2[], float verts2[][], int Nquads2[],
	    int quads2[][], int quadids[],
	    float brightnesses[])  /* tmpGFBrightnesses not calculated if NULL */
	{
	    int nverts3[] = new int[1], nquads3[] = new int[1]; // array of 1 so we can pass as ref

        if (slicesmask == 0) {
            int i;
            /* FIX THIS-- this copying shouldn't be necessary */
            nverts3[0] = n_untwisted_verts3;
            for (i = 0; i < nverts3[0]; ++i)
                Vec_h._SET3(tmpC2DFIVerts3[i], untwisted_verts3[i]);
            for (i = 0; i < nverts3[0] / 8; ++i)
                tmpC2DFISids3[i] = untwisted_stickerids3[i];
            nquads3[0] = n_untwisted_quads3;
            for (i = 0; i < nquads3[0]; ++i)
                Vec_h._SET4(tmpC2DFIQuads3[i], untwisted_quads3[i]);
        } else 
        calc3DFrameInfo(sticker, dir, slicesmask, frac, interp,
            nverts4, untwisted_verts4, eyew, cullcells,
            nverts3, tmpC2DFIVerts3, nquads3, tmpC2DFIQuads3,
            tmpC2DFISids3, null);

	    calc3DTo2DFrameInfo(nverts3[0], tmpC2DFIVerts3, nquads3[0], tmpC2DFIQuads3, tmpC2DFISids3,
	        tilt, twirl, eyez,
	        sunvec, sunvec_is_in_objspace,
            usermat,
	        cullverts, cullbackfaces, zsort,
	        Nverts2, verts2, Nquads2, quads2, quadids,
	        brightnesses);
	} // end calc2DFrameInfo
    

	/**
     * High level grip selection.
	 * "grip" is equal to the sticker id if length=3; 
	 * otherwise it's equal to the stickerid on the 3x3x3x3 puzzle that corresponds to the twist.
	 * @return true if landed on a sticker, false otherwise.
	 */
	public boolean pickGrip(float x, float y, MagicCube.Frame frame, MagicCube.Stickerspec grip) {
	    int hitQuad = pick(x, y, frame, grip);
        if(hitQuad < 0)
            return false;
        if (length == 2) {
            int ax, sgn, face0coords[] = new int[4], M[][] = new int[4][4];
            ax = frame.quadids[hitQuad] % 3;
            sgn = (frame.quadids[hitQuad] % 6) < 3 ? -1 : 1;
            /* invent a nonexistent sticker at the appropriate axis */
            face0coords[ax] = sgn * (length - 1);
            face0coords[(ax + 1) % 3] = 0;
            face0coords[(ax + 2) % 3] = 0;
            face0coords[3] = -length;
            Math4d.getCanonicalMatThatTakesAxisToMinusW(FACETOAXIS(grip.face), FACETOSIGN(grip.face), M);
            /* apply inv of that matrix to face0coords */
            Vec_h._MXV4(grip.coords, M, face0coords);
            grip.dim = 2;
        }

        /*
         * Now we know the coordinates of the sticker.
         * "Straighten it out" to the coords of the appropriate grip.
         * I.e. change everything < length-1 to 0,
         * change length-1 to 2 and change length to 3.
         */
        for(int j=0; j<4; ++j) {
            if (Math.abs(grip.coords[j]) < length - 1)
                grip.coords[j] = 0;
            else
                grip.coords[j] = MagicCube.SGN(grip.coords[j]) *
                    (Math.abs(grip.coords[j]) - length + 3);
        }
        fillStickerspecFromCoordsAndLength(grip, 3);
        return true;
	} // end pickGrip

    
    /**
     * Low level sticker selection.
     * Fills the given stickerspec with data of sticker under the given point if any.
     * @return index of sticker quad in frame if landed on a sticker, -1 otherwise
     */
	public int pick(float x, float y, MagicCube.Frame frame, MagicCube.Stickerspec sticker) {
	    boolean checking_bbox = true; //!preferences.getBoolProperty(M4D_DONT_CHECK_BBOX);
	    float verts[][] = frame.verts;
	    short quads[][] = frame.quads;
	    int i, j;
	    float thispoint[] = { x, y };

	    for (i = frame.nquads - 1; i >= 0; --i) {
	        if (checking_bbox) {
	            for (j = 0; j < 4; ++j)
	                if (x <= verts[quads[i][j]][0])
	                    break;
	            if (j == 4)
	                continue;
	            for (j = 0; j < 4; ++j)
	                if (x >= verts[quads[i][j]][0])
	                    break;
	            if (j == 4)
	                continue;
	            for (j = 0; j < 4; ++j)
	                if (y <= verts[quads[i][j]][1])
	                    break;
	            if (j == 4)
	                continue;
	            for (j = 0; j < 4; ++j)
	                if (y >= verts[quads[i][j]][1])
	                    break;
	            if (j == 4)
	                continue;
	        }

	        for (j = 0; j < 4; ++j)
	            if (!short_are_CCW(verts[quads[i][j]], verts[quads[i][(j + 1) % 4]], thispoint))
	                break;
	        if (j == 4) { /* they were all CCW, so we hit a quad */
	            sticker.id_within_cube = frame.quadids[i] / 6;
	            fillStickerspecFromId(sticker);
	            return i;
	        }
	    }
	    return -1;
	} // end pick

	public void getUntwistedFrame(MagicCube.Frame frame, float usermat[][], float[] sunvec, boolean cullbackfaces) {
        MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
	    sticker.id_within_cube = 0;
	    fillStickerspecFromId(sticker);
	    getFrame(sticker, MagicCube.CCW, 0, 0, 1, usermat, sunvec, cullbackfaces, frame);
	}

    private float tmpGFVerts[][] = new float[MagicCube.MAXVERTS][2]; // scratch var
    private int tmpGFQuads[][] = new int[MagicCube.MAXQUADS][4]; // scratch var
    private int tmpGFQuadids[] = new int[MagicCube.MAXQUADS]; // scratch var
    private float tmpGFBrightnesses[] = new float[MagicCube.MAXQUADS]; // scratch var
    
    /**
     * Produces the geometry for a particular frame of animation.
     * 
     * @param sticker identifies the control sticker being acted upon. E.G. being twisted about.
     * @param dir CW or CCW
     * @param slicesmask identifies which slice or slices being twisted.
     * @param seqno frame number in the range 0 -> outof-1.
     * @param outof total number of frames in animation sequence.
     * @param usermat is an optional viewing matrix to be applied before 2D projection.
     * @param frame container for the resulting data.
     */
	public void getFrame(MagicCube.Stickerspec sticker, int dir, int slicesmask, 
            int seqno, int outof, float usermat[][], float sunvec[], boolean cullbackfaces, MagicCube.Frame frame)
	{
	    /*
	     * The frame calculation routines use different types from the
	     * struct frame, so we must translate
	     */
	    int nverts, nquads;

        int nv[] = new int[1], nq[] = new int[1];
	    calc2DFrameInfo(sticker, dir, slicesmask, (float) seqno / outof,
            sine_interp, n_untwisted_verts4, untwisted_verts4,
            eyeW, //preferences.getRealProperty(M4D_EYEW, EYEW),
            true, //!preferences.getBoolProperty(M4D_NOCULLCELLS), /* cullcells */
            tilt, twirl,
            MagicCube.EYEZ, //preferences.getRealProperty(M4D_EYEZ, EYEZ),
            sunvec, 
            false, /* sunvec_is_in_objspace */
            usermat,
            true, //preferences.getBoolProperty(M4D_CULLVERTS), /* cullverts */
            cullbackfaces, //!preferences.getBoolProperty(M4D_NOCULLFACES), /* cullbackfaces */
            true, //!preferences.getBoolProperty(M4D_NOZSORT), /* zsort */
            nv, tmpGFVerts, nq, tmpGFQuads,
            tmpGFQuadids, tmpGFBrightnesses);
        nverts = nv[0];
        nquads = nq[0];
	    frame.nverts = nverts;
	    if (bbox[0][0] == 0) { // floating point compare?
	        /*
	         * It is assumed that the first time this function is called,
	         * it is with the untwisted frame.  The bounding box
	         * of this frame is the one that will always be used.
	         */
	        float ctr[] = new float[2], rad;
	        Vec_h._ZEROMAT2(bbox);
	        for (int i = 0; i < nverts; ++i) {
	            bbox[0][0] = Math.min(bbox[0][0], tmpGFVerts[i][0]);
	            bbox[0][1] = Math.min(bbox[0][1], tmpGFVerts[i][1]);
	            bbox[1][0] = Math.max(bbox[1][0], tmpGFVerts[i][0]);
	            bbox[1][1] = Math.max(bbox[1][1], tmpGFVerts[i][1]);
	        }
	        Vec_h._LERP2(ctr, bbox[0], bbox[1], .5f);
	        rad = Math.max(bbox[1][0] - ctr[0], bbox[1][1] - ctr[1]);
	        /* make bbox a square twice or so as big as it was */
	        bbox[0][0] = (float)(ctr[0] - 1.2 * rad);
	        bbox[0][1] = (float)(ctr[1] - 1.2 * rad);
	        bbox[1][0] = (float)(ctr[0] + 1.2 * rad);
	        bbox[1][1] = (float)(ctr[1] + 1.2 * rad);
	    }
	    for (int i = 0; i < nverts; ++i) {
            frame.verts[i][0] = (tmpGFVerts[i][0] - bbox[0][0]) / (bbox[1][0] - bbox[0][0]) -.5f;
            frame.verts[i][1] = (bbox[1][1] - tmpGFVerts[i][1]) / (bbox[1][1] - bbox[0][1]) - .5f;
//            float normval = (tmpGFVerts[i][0] - bbox[0][0]) / (bbox[1][0] - bbox[0][0]) -.5f;
//	        frame.verts[i][0] = (short)(normval * Short.MAX_VALUE * 2);
//            normval = (bbox[1][1] - tmpGFVerts[i][1]) / (bbox[1][1] - bbox[0][1]) - .5f;
//	        frame.verts[i][1] = (short)(normval * Short.MAX_VALUE * 2);
	    }
	    frame.nquads = nquads;
	    for (int i = 0; i < nquads; ++i) {
	        Vec_h._SET4(frame.quads[i], tmpGFQuads[i]);
	        frame.quadids[i] = (short)tmpGFQuadids[i];
	        frame.brightnesses[i] = tmpGFBrightnesses[i];
	    }
	} // end getFrame


    private float ranges[][] = new float[MagicCube.MAXLENGTH][2]; // scratch var
    private float toverts4[][] = new float[MagicCube.MAXVERTS][4]; // scratch var
    
	public void calc3DFrameInfo(
	    /* INPUTS */
	    MagicCube.Stickerspec grip, int dir,
	    int slicesmask, float frac, InterpFunc interp,
	    int nverts4, float uv4[][], float eyew,
	    boolean cullcells,
	    /* OUTPUTS */
	    int Nverts3[], float verts3[][],
	    int Nquads3[], int quads3[][],
	    int stickerids3[], boolean stickers_inverted[])
	{
	    int i, j, k, whichax=0, sgn, nranges=0, nverts3, nquads3;
	    float
            total_angle,
	    	center[] = new float[4], 
	    	real_stickercoords[] = new float[4], 
	    	mat[][] = new float[4][4],
	    	tomultiplyby, orientationmat[][] = new float[3][3];
	    /*
	     * NOTE: Don't change the order here.
	     * the miserable little hack in polymgr_pick_grip
	     * depends on the assumption that cubequads[i] is on axis i%3
	     * in direction (i<3 ? -1 : 1).
	     */
	    final int  cubequads[][] = {
	        {0, 4, 6, 2},
	        {0, 1, 5, 4},
	        {0, 2, 3, 1},
	        {7, 5, 1, 3},
	        {7, 3, 2, 6},
	        {7, 6, 4, 5},
	    };

	    if (0 == nverts4 || null == uv4) {
	        nverts4 = n_untwisted_verts4;
	        uv4 = untwisted_verts4;
	    }


	    if (slicesmask != 0) {
	        /*
	         * Calculate rotation center; 
             * It's the center of the face containing the grip.
	         */
	        whichax = -1;
	        for (i = 0; i < MagicCube.NDIMS; ++i) {
	            if (Math.abs(grip.coords[i]) == 3) {
	                center[i] = grip.coords[i];
	                whichax = i;
	            }
	            else
	                center[i] = 0;
	        }
//	        assert(INRANGE(0 <=, whichax, <NDIMS));

	        total_angle = getTwistTotalAngle(grip.dim, dir);
            Vec_h._SET4(real_stickercoords, grip.coords);

	        Math4d.get4dRotMatrix(center, real_stickercoords, interp.func(frac) * total_angle, mat);
	        sgn = MagicCube.SGN(center[whichax]);
	        nranges = makeRangesReal(slicesmask, sgn, ranges);
	    }

	    /*
	     * Transform the vertices
	     */
	    nverts3 = 0;
	    for (i = 0; i < nverts4; ++i) {
	        if (i % 8 == 0)
	            stickerids3[nverts3 / 8] = i / 8;
            if (slicesmask == 0)
                Vec_h._SET4(toverts4[i], untwisted_verts4[i]);
            else {
                /*
                * Transform the vertex in 4-space
                */
                for (j = 0; j < nranges; ++j) {
                    if (ranges[j][0] <= untwisted_verts4[i][whichax] && untwisted_verts4[i][whichax] <= ranges[j][1]) {
                        Vec_h._VXM4(toverts4[i], untwisted_verts4[i], mat);
                        break;
                    }
                }
                if (j == nranges)
                    Vec_h._SET4(toverts4[i], untwisted_verts4[i]);
            }

	        /*
	         * W-divide to get 3d coords
	         */
	        tomultiplyby = 1 / (eyew - toverts4[i][3]);
            Vec_h._VXS3(verts3[nverts3], toverts4[i], tomultiplyby);
	        nverts3++;
	        if ((cullcells || stickers_inverted!=null) && i % 8 == 4) {
	            /* got enough verts to determine whether to cull */
	            j = (nverts3 / 8) * 8;  /* round down to start of sticker */
                Vec_h._VMV3(orientationmat[0], verts3[j + 1], verts3[j]);
                Vec_h._VMV3(orientationmat[1], verts3[j + 2], verts3[j]);
                Vec_h._VMV3(orientationmat[2], verts3[j + 4], verts3[j]);
	            boolean is_inverted = Vec_h.__DET3(orientationmat) <= 0;
	            if (stickers_inverted!=null)
	                stickers_inverted[i / 8] = is_inverted;
	            if (cullcells && is_inverted) {
	                i = MagicCube.ROUNDUP(i, 8) - 1;  /* skip rest of this sticker's verts */
	                nverts3 = j;
	            }
	        }
	    }

	    /*
	     * We now have nverts3 and verts3;
	     * Now set quads.
	     */
	    nquads3 = 0;
	    for (i = 0; i < nverts3 / 8; ++i)   // for each sticker
	        for (j = 0; j < 6; ++j)
	        {                       // for each sticker face
	            for (k = 0; k < 4; ++k) // for each face vertex
	                quads3[nquads3][k] = 8 * i + cubequads[j][k];
	            nquads3++;
	        }

	    Nverts3[0] = nverts3;
	    Nquads3[0] = nquads3;
	} // end calc3DFrameInfo


    private float sticker_centers_3d[][][][] = new float[MagicCube.MAXLENGTH][MagicCube.MAXLENGTH][MagicCube.MAXLENGTH][MagicCube.NDIMS - 1];
    private float face0verts[][][][][][][] = new float[MagicCube.MAXLENGTH][MagicCube.MAXLENGTH][MagicCube.MAXLENGTH][2][2][2][MagicCube.NDIMS];
    
	private int getUntwistedVerts4(float verts4[][], float faceshrink, float stickershrink) {
	    int i, j, k, ii, jj, kk, f;
	    int mat[][] = new int[4][4];
	    int nverts4;

	    for (k = 0; k < length; ++k)
	        for (j = 0; j < length; ++j)
	            for (i = 0; i < length; ++i) {
	                sticker_centers_3d[i][j][k][MagicCube.X] = -length + 2 * i + 1;
	                sticker_centers_3d[i][j][k][MagicCube.Y] = -length + 2 * j + 1;
	                sticker_centers_3d[i][j][k][MagicCube.Z] = -length + 2 * k + 1;
	                for (kk = 0; kk < 2; ++kk)
	                    for (jj = 0; jj < 2; ++jj)
	                        for (ii = 0; ii < 2; ++ii) {
	                            face0verts[i][j][k][ii][jj][kk][MagicCube.X] = -length + 2 * (i + ii);
	                            face0verts[i][j][k][ii][jj][kk][MagicCube.Y] = -length + 2 * (j + jj);
	                            face0verts[i][j][k][ii][jj][kk][MagicCube.Z] = -length + 2 * (k + kk);
	                            face0verts[i][j][k][ii][jj][kk][MagicCube.W] = -length;
	                            Vec_h._LERP3(
                                    face0verts[i][j][k][ii][jj][kk],
	                                sticker_centers_3d[i][j][k],
	                                face0verts[i][j][k][ii][jj][kk], stickershrink);
                                Vec_h._VXS3(
                                    face0verts[i][j][k][ii][jj][kk],
	                                face0verts[i][j][k][ii][jj][kk], faceshrink);
	                        }
	            }

	    nverts4 = 0;
	    for (f = 0; f < MagicCube.NFACES; ++f) {
	        Math4d.getCanonicalMatThatTakesAxisToMinusW(FACETOAXIS(f), FACETOSIGN(f), mat);
	        for (k = 0; k < length; ++k)
	            for (j = 0; j < length; ++j)
	                for (i = 0; i < length; ++i)
	                    for (kk = 0; kk < 2; ++kk)
	                        for (jj = 0; jj < 2; ++jj)
	                            for (ii = 0; ii < 2; ++ii) {
	                                //assert(nverts4 < MagicCube.MAXVERTS);
	                                /*
	                                 * want to take -W to the axis, so we apply the inverse,
	                                 * i.e. apply the transpose, i.e. multiply by the matrix on
	                                 * the left
	                                 */
                                    Vec_h._MXV4(verts4[nverts4++], mat, face0verts[i][j][k][ii][jj][kk]);
	                            }
	    }
	    return nverts4;
	} // end getUntwistedVerts4

	public void incTilt(float inc) {
	    tilt += inc;
	    /* restrict to range [-pi/2, pi/2] */
	    if (tilt > Math.PI / 2)
	        tilt = (float)Math.PI / 2;
	    if (tilt < -Math.PI / 2)
	        tilt = (float)-Math.PI / 2;
	    // printf("Tilt = %g degrees\n", RTOD(tilt));
	}

	public void incTwirl(float inc) {
	    twirl += inc;
	    // printf("Twirl = %g degrees\n", RTOD(twirl));
	}	
    
    public static int intpow(int i, int j) {
        return j != 0 ? intpow(i, j - 1) * i : 1;
    }
    
    
    public static void main(String args[]) {
        new PolygonManager();
    }
    
}
