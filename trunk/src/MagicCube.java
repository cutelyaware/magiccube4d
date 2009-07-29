import java.awt.Color;

//
//NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
//AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Contains all the global static constants shared by all Magic Cube 4D classes.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green & Don Hatch
 */
public class MagicCube {
	
	/*
	 * Constants that probably shouldn't be changed
	 */
	public final static int 
		NDIMS          = 4,
		NFACES         = 2 * NDIMS,
        GRIPS_PER_FACE = 3 * 3 * 3,
        NGRIPS         = NFACES * GRIPS_PER_FACE,
        LENGTH         = 3, // default
        MAXLENGTH      = 5, // note: 5 uses uses lots of memory 
        MAXSTICKERS    = totalStickers(MAXLENGTH),
		MAXVERTS       = MAXSTICKERS * 8,
		MAXQUADS       = MAXSTICKERS * 6;
    
	/**
	 * Axes
	 */
	public final static int
		X = 0,
		Y = 1,
		Z = 2,
		W = 3;
    
	/**
	 * Rotation directions
	 */
	public final static int 
		CCW =  1,
		CW  = -1;

    // disallows instantiation of utility class
    private MagicCube() {}

    /**
     * Indexing data for a sticker or a grip.
     */
    public final static class Stickerspec {
        public int
            coords[] = new int[NDIMS],
            face, dim, id_within_cube, id_within_face;
    }
    
	/**
	 * Geometry data for an animation frame.
	 */
	public final static class Frame {
	    public int nverts;
        public float verts[][] = new float[MAXVERTS][2]; // values range from -.5 to .5
	    public int nquads;
	    public short quads[][] = new short[MAXQUADS][4];
	    public short quadids[] = new short[MAXQUADS];
	    public float brightnesses[] = new float[MAXQUADS];
	}
    
    
    /**
     * Simple data class to hold an animation specification.
     * A queue of these could be used to set up a sequence of moves.
     */
    public static class TwistData {
        public MagicCube.Stickerspec grip;
        public int animStep=0, slicemask, direction;
        public TwistData(MagicCube.Stickerspec grip, int direction, int slicemask) {
            this.grip = grip; this.slicemask = slicemask; this.direction = direction;
        }
        public TwistData(int id_within_cube, int direction, int slicemask) {
            this.direction = direction;
            this.slicemask = slicemask;
            this.grip = new MagicCube.Stickerspec();
            grip.id_within_cube = id_within_cube;
            PolygonManager.fillStickerspecFromIdAndLength(grip, 3);       
        }
    }
    
    public static int totalStickers(int length) { return NFACES * length * length * length; }
    public static int totalQuads(int length) { return 6 * totalStickers(length); }
    
	// static functions from MagicCube.h
    public static float DTOR(float d) { return (float) (d * Math.PI / 180); }
	public static int SGN(float x) { return x<0 ? -1 : x>0 ? 1 :0; }
	public static float RTOD(float r) { return (float) (r / Math.PI * 180); }
	public static boolean BIT(int mask, int bit) { return (mask & (1<<bit)) != 0; }
    public static int HOWMANY(int x, int mod) { return (x + mod - 1) / mod; }
    public static int ROUNDUP(int x, int mod) { return HOWMANY(x,mod)*mod; }  /* only works for >= 0 */
    public static int ROUNDDOWN(int x, int mod) { return x / mod * mod; }  /* ditto */

    /*
     * Default face colors
     */
    public final static float FACE_COLORS[][] = {
        {0, 0, 1},
        {0.5f, 0, 0},
        {.4f, 1, 1},
        {1, 0, .5f},
        {.9f, .5f, 1},
        {1, .5f, 0},
        {1, 1, .5f},
        {0, 1, .5f},
    };
    public static Color faceColor(int face) { return new Color(
        MagicCube.FACE_COLORS[face][0], 
        MagicCube.FACE_COLORS[face][1], 
        MagicCube.FACE_COLORS[face][2]);
    }
    
	/*
	 * default 4d viewing parameters
	 */
	public final static float 
		FACESHRINK    = .4f,
		STICKERSHRINK = .5f,
		EYEW          = 5.2f;

	/*
	 * 3d viewing parameters
	 */
	public final static float
		TILT  =  30, // degrees
		TWIRL = -42, // degrees
		EYEZ  =  10; // should really be function of LENGTH and EYEW

    public final static float SUNVEC[] = { .82f, 1.55f, 3.3f }; // points *towards* sun. default for shading & shadows

    /*
	 * Default number of frames for a twist or rotate.
	 * Even numbers are sometimes bad.
	 */
	public final static int
		NFRAMES_90  = 11,
		NFRAMES_120 = 15,
		NFRAMES_180 = 23;

	/*
	 * Default number of twists to twist when scrambling
	 */
	public final static int FULL_SCRAMBLE = 40;

	public final static String 
        TITLE = "Magic Cube 4D", 
        LOGFILE = "MagicCube4D.log", // in user's home dir
        MAGIC_NUMBER = "MagicCube4D"; // 1st string in log file for sanity checking
    
    public final static String PUZZLE_VERSION = "3.2.3";
    public final static int FILE_VERSION = 2;
    
    public final static Color
        BACKGROUND = new Color(20,170,235),
        GROUND     = new Color(20, 130, 20);
}
