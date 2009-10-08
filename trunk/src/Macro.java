import java.io.*;

import com.donhatchsw.util.VecMath;

/**
 * Contains a sequence of moves relative to a set of reference stickers (XXX - now grips! is this ok?).
 * Has a method that can take other sets of reference stickers in the same pattern as the definition stickers
 * and returns a set of moves the same as would be generated had the definition moves been applied relative
 * to the given ones instead.
 */
public class Macro {
    public final static int MAXREFS = 3;
    private History moves = new History(3);
    private String puzzleString;	// Macros are really relative to specific puzzles, so we save this out.
    private String name;
    private MagicCube.Stickerspec defRefs[] = new MagicCube.Stickerspec[MAXREFS];

    private Macro() {}

    /**
     * Constructs an unnamed empty macro.
     */
    public Macro( String puzzleString, MagicCube.Stickerspec appStickers[] ) 
    { 
    	this( puzzleString, "", appStickers ); 
    }

    /**
     * Constructs a named, empty macro with set of reference stickers.
     * @param name display name of the new macro.
     * @param defStickers is a set of "definition" reference stickers.
     */
    public Macro( String puzzleString, String name, MagicCube.Stickerspec defStickers[] ) 
    {
    	this.puzzleString = puzzleString;
        this.name = name;
        assert(defStickers.length == MAXREFS);
        System.arraycopy( defStickers, 0, this.defRefs, 0, MAXREFS );
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public String getPuzzleString() { return puzzleString; }

    /**
     * @return the number of moves in this macro.
     */
    public int length() { return moves.countMoves(false); }

    /**
     * Adds a twist or rotation relative to the definition stickers.
     */
    public void addMove(MagicCube.TwistData move) {
        moves.apply(move);
    }

    // XXX - This is copied from PolytopePuzzleDescription.
    //		 Move to a shared location.
    private static float[] doubleToFloat(double in[])
    {
        float out[] = new float[in.length];
        for (int i = 0; i < in.length; ++i)
            out[i] = (float)in[i];
        return out;
    }
    
    private static double[] floatToDouble(float in[])
    {
        double out[] = new double[in.length];
        for (int i = 0; i < in.length; ++i)
            out[i] = (double)in[i];
        return out;
    }
    
    private double[] getGripCoords( MagicCube.Stickerspec grip, GenericPuzzleDescription puzzle )
    {
    	return floatToDouble( puzzle.getGripCoords( grip.id_within_puzzle ) );
    }
    
    /**
     * Applies a set of application reference grips and returns a list of macro twists
     * ready to apply the macro relative to those grips.
     * @param appGrips - grips to apply macro relative to.
     * @return array of twists in application space or null if pattern doesn't match.
     */
    public MagicCube.TwistData[] getTwists( MagicCube.Stickerspec appGrips[],
    	GenericPuzzleDescription puzzle ) 
    {
    	// Default Coordinates.
    	// We also use one of the faces.
    	double ref0[] = getGripCoords( defRefs[0], puzzle );
    	double ref1[] = getGripCoords( defRefs[1], puzzle );
    	double ref2[] = getGripCoords( defRefs[2], puzzle );
    	int faceIndex = puzzle.getGrip2Face()[defRefs[0].id_within_puzzle];
    	double ref3[] = floatToDouble( puzzle.getFaceCenter( faceIndex ) );
    	
    	// Clicked Coordinates.
    	double s0[] = getGripCoords( appGrips[0], puzzle );
    	double s1[] = getGripCoords( appGrips[1], puzzle );
    	double s2[] = getGripCoords( appGrips[2], puzzle );
    	faceIndex = puzzle.getGrip2Face()[appGrips[0].id_within_puzzle];
    	double s3[] = floatToDouble( puzzle.getFaceCenter( faceIndex ) );
    	
        double transform[][] = new double[4][4];
        if( !Math4d.get4dMatThatRotatesThese4ToThose4( ref0, ref1, ref2, ref3, s0, s1, s2, s3, transform ) )
        	return null;
        
        int expected = length();
        MagicCube.TwistData twists[] = new MagicCube.TwistData[expected];
        moves.goToBeginning();
        for( int i=0; i<expected; i++ ) 
        {
            MagicCube.TwistData move = moves.redo();
            if( move == null )
                return null;
            
            // Transform to new grip.
            int gripIndex = move.grip.id_within_puzzle;
            double[] gripCoords = floatToDouble( puzzle.getGripCoords( gripIndex ) );
            double[] newCoords = VecMath.vxm( gripCoords, transform );
            int newGripIndex = puzzle.getClosestGrip( doubleToFloat( newCoords ) );
            move.grip.id_within_puzzle = newGripIndex;

            twists[i] = move;
        }
        assert( moves.redo() == null );
        
        double det = VecMath.det( transform );      
        if( det < 0 )
            reverse( twists );
        
        return twists;
    }

    public static void reverse(MagicCube.TwistData[] twists) {
        // reverse move order
        for(int i=0; i<twists.length/2; i++) {
            MagicCube.TwistData tmp = twists[i];
            twists[i] = twists[twists.length-i-1];
            twists[twists.length-i-1] = tmp;
        }
        // reverse twist directions
        for(int i = 0; i < twists.length; ++i)
            twists[i].direction *= -1;
    }

    /**
     * Serializes a macro to a text writer.
     * @param writer is the text stream to write to.
     */
    public void write( Writer writer ) throws IOException
    {
        MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
        writer.write("@" + name + "@@" + puzzleString + "@(");
        for( int i=0; i<defRefs.length; ++i ) 
        {
        	sticker = defRefs[i];
            writer.write( "" + sticker.id_within_puzzle );
            if( i+1 < defRefs.length )
                writer.write(" ");
        }
        writer.write(") ");
        moves.write( writer );
    }

    private static String readStringHelper( PushbackReader pr, int ch ) throws IOException
    {
        if(ch != '@') return null;
        char[] charbuf = new char[256];
        int len=0;
        do {
            ch = pr.read();
            charbuf[len++] = (char)ch;
        } while( ! (ch=='@' || ch<=0));
        if(ch != '@') return null;
        return new String(charbuf, 0, len-1);
    }
    
    /**
     * Deserializes a macro from a text stream.
     * @param pr a text reader assumed to be positioned at the beginning of a previously saved macro.
     * @return macro represented by the text or null if invalid.
     */
    public static Macro read( BufferedReader reader ) throws IOException
    {
    	PushbackReader pr = new PushbackReader( reader );
        Macro restored = new Macro();

        int ch;
        do ch = pr.read();
        //while(Character.isWhitespace(ch)); // isWhitespace doesn't exist in 1.4
        while(" \t\n\r\f".indexOf(ch) != -1);
        
        restored.name = readStringHelper( pr, ch );
        ch = pr.read();
        restored.puzzleString = readStringHelper( pr, ch );
        if( restored.name == null || restored.puzzleString == null )
        	return null;
        
        ch = pr.read();
        if(ch != '(') return null;
        for(int r=0; r<MAXREFS; r++) {
        	MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
            sticker.id_within_puzzle = History.readInt(pr);
            restored.defRefs[r] = sticker;
        }
        ch = pr.read();
        assert(ch==')');
        ch = pr.read();
        assert(ch==' ');

        if( ! restored.moves.read(pr)) {
        	System.out.println("Error reading macro history");
            return null;
        }
        return restored;
    } // end read

} // end class Macro
