package com.superliminal.magiccube4d;
import java.awt.Color;


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
        DEFAULT_LENGTH = 3;
	public final static String DEFAULT_PUZZLE = "{4,3,3}";
	public final static String SUPPORTED_PUZZLES[][] = {
        {"{3,3,3}",  "1,2,3,4,5,6,7,8,9",	"Simplex"},
        {"{3}x{4}",  "1,2,3,4,5,6,7",		"Triangular Duoprism"},
        {"{4,3,3}",  "1,2,3,4,5,6,7,8,9",	"Hypercube"},
        {"{5}x{4}",  "1,2,3,4,5,6,7",		"Pentagonal Duoprism"},
        {"{6}x{4}",  "1,2,3,4,5,6,7", 		"Hexagonal Duoprism"},
        {"{7}x{4}",  "1,2,3,4,5,6,7", 		"Heptagonal Duoprism"},
        {"{8}x{4}",  "1,2,3,4,5,6,7", 		"Octagonal Duoprism"},
        {"{9}x{4}",  "1,2,3,4,5,6,7",		"Nonagonal Duoprism"},
        {"{10}x{4}", "1,2,3,4,5,6,7",		"Decagonal Duoprism"},
        {"{100}x{4}","1,3",					"Onehundredagonal Duoprism"},
        {"{3}x{3}",  "1,2,3,4,5,6,7",		""},
        {"{3}x{5}",  "1,2,3,4,5,6,7",		""},
        {"{5}x{5}",  "1,2,3,4,5,6,7",		""}, // XXX look at twisting on 2
        {"{5}x{10}", "1,2,3",				""}, // XXX look at twisting on 2
        {"{10}x{10}","1,2,3",				""}, // XXX look at twisting on 2
        {"{3,3}x{}", "1,2,3,4,5,6,7",		"Tetrahedral Prism"},
        {"{5,3}x{}", "1,2,3,4,5,6,7",		"Dodecahedral Prism"},
        {"{5,3,3}",  "1,2,3",				"Hypermegaminx (BIG!)"},
        {null,       "",					"Invent my own!"},
    };
    
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
            face, dim, id_within_puzzle;
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
        public TwistData(int id_within_puzzle, int direction, int slicemask) {
            this.direction = direction;
            this.slicemask = slicemask;
            this.grip = new MagicCube.Stickerspec();
            grip.id_within_puzzle = id_within_puzzle;
        }
    }
    
	// Helper functions
    public static float DTOR(float d) { return (float) (d * Math.PI / 180); }
	public static int SGN(float x) { return x<0 ? -1 : x>0 ? 1 :0; }
	public static float RTOD(float r) { return (float) (r / Math.PI * 180); }
	public static boolean BIT(int mask, int bit) { return (mask & (1<<bit)) != 0; }
    public static int HOWMANY(int x, int mod) { return (x + mod - 1) / mod; }
    public static int ROUNDUP(int x, int mod) { return HOWMANY(x,mod)*mod; }  /* only works for >= 0 */
    public static int ROUNDDOWN(int x, int mod) { return x / mod * mod; }  /* ditto */

    
	/*
	 * 4d viewing defaults
	 */
	public final static float 
		FACESHRINK    = .4f,
		STICKERSHRINK = .5f,
		EYEW          = 5.2f;

	/*
	 * 3d viewing defaults
	 */
	public final static float
		TILT  =  30, // degrees
		TWIRL = -42, // degrees
		EYEZ  =  10; // should really be function of LENGTH and EYEW

    public final static float SUNVEC[] = { .82f, 1.55f, 3.3f }; // Default for shading & shadows points *towards* sun. 

	public final static String 
        TITLE = "Magic Cube 4D", 
        LOGFILE = "MagicCube4D.log", // in user's home directory
        MAGIC_NUMBER = "MagicCube4D"; // 1st string in log and macro files for sanity checking
    
    public final static String PUZZLE_VERSION = "4.0.0";
    public final static int LOG_FILE_VERSION = 3;
    public final static int MACRO_FILE_VERSION = 2;
    
    public final static Color
        SKY    = new Color(20,170,235),
        GROUND = new Color(20, 130, 20);
}
