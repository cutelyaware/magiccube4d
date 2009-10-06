
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * Maintains the state of a MagicCube4D puzzle.
 * The state is defined as a 4D array of stickers identified by face ID.
 * I.E. the index of the face of the untwisted cube containing each sticker.
 *
 * Contains methods to perform twists on the puzzle state and a method
 * to get the current color for a sticker position on the untwisted puzzle.
 *
 * Copyright 2005 - Superliminal Software
 * @author Don Hatch
 */
public class PuzzleState {

    private int length;
    public int getLength() { return length; }
    private int nstickers;
    private int the_state[];
    private PolygonManager polymgr;  // TODO: REMOVE THIS ABOMINABLE DEPENDANCY!

    public PuzzleState(PolygonManager polymgr) {
        this(3, polymgr);
    }

    public PuzzleState(int length, PolygonManager polymgr) {
        this.polymgr = polymgr;
        reset(length);
    }


    private MagicCube.Stickerspec tmpsticker = new MagicCube.Stickerspec();

    public MagicCube.TwistData[] solve() {
        int gridLen = 2 * length + 1;  // -n to +n or shifted into zero base, 0 to 2n inclusive.
        char stickerGrid[][][][] = new char[gridLen][gridLen][gridLen][gridLen];
        for(int i=0; i<the_state.length; i++) {
            tmpsticker.id_within_puzzle = i;
            polymgr.fillStickerspecFromId(tmpsticker);
            int coords[] = tmpsticker.coords;
            for(int j=0; j<coords.length; j++)
                coords[j] += length;
            stickerGrid[coords[0]][coords[1]][coords[2]][coords[3]] = (char)('0' + the_state[i]);
        }

        //
        // Figure out the face direction
        // corresponding to each face color
        // (i.e. which direction's face has that color
        // as its center sticker).
        // Actually we only do this for the negative faces;
        // then the color for a positive face
        // is the color opposite the color of the opposite negative face.
        // If the puzzle length is even, then look at
        // the most-negative cubie rather than the face centers.
        //
        int[][] colorToDir = new int[8][4]; // zeros
        for (int ax=0; ax < 4; ++ax)
        {
            int sign = -1; // only looking at negative faces
            int coords[] = new int[4];
            for(int j=0; j<coords.length; j++)
                coords[j] = (gridLen-1)/2; // center of stickerGrid
            coords[ax] += sign*length; // center sticker of face

            // For even-length puzzles, only the negative-most
            // cubie is definitive.
            if (length%2 == 0)
                for (int j=0; j<coords.length; j++)
                    if (j != ax)
                        coords[j] -= (length-1); // negative-most cubie

            char faceChar = stickerGrid[coords[0]][coords[1]][coords[2]][coords[3]];
            assert(faceChar != 0);
            int color = faceChar - '0';
            colorToDir[color][ax] = sign;
            colorToDir[7-color][ax] = -sign;
        }

        /*{
            System.out.println("int[][] colorToDir = {");
            for (int i = 0; i < 8; ++i)
                System.out.println("    {"+colorToDir[i][0]
                                     +", "+colorToDir[i][1]
                                     +", "+colorToDir[i][2]
                                     +", "+colorToDir[i][3]+"},");
            System.out.println("};");
        }*/

        String solution = NdSolve.solve(grid2String(length,stickerGrid));
        //System.out.println(solution);
        StringTokenizer toks = new StringTokenizer(solution, " ");
        ArrayList<MagicCube.TwistData> twists = new ArrayList<MagicCube.TwistData>();
        while(toks.hasMoreElements()) {
            String tripplestr = (String)toks.nextElement();
            twists.add(triple2Twist(tripplestr.charAt(0)-'0', tripplestr.charAt(1)-'0', tripplestr.charAt(2)-'0', colorToDir));
        }
        return twists.toArray(new MagicCube.TwistData[0]);
    }

    public static String grid2String(int length, char[][][][] grid) {
        String newline = System.getProperty("line.separator");
        //newline = "\r"; // to hard-code mac for testing
        //newline = "\r\n"; // to hard-code windows for testing
        //newline = "\n"; // to hard-code linux for testing
        StringBuffer sb = new StringBuffer();
        for(int x=0; x<grid.length; x++) {
            for(int y=0; y<grid.length; y++) {
                for(int z=0; z<grid.length; z++) {
                    for(int w=0; w<grid.length; w++) {
                        char c = grid[x][y][z][w];
                        sb.append(c==0 ? ' ' : c);
                    }
                }
                sb.append(newline);
            }
            sb.append(newline);
        }
        sb.append(newline);
        String statestr = sb.toString();
        // crunch out blank lines
        statestr = statestr.replaceAll("(\\s*"+newline+")+", newline);
        // oh whatever, just reformat it with voodoo so it looks nice
        statestr = NdSolve.reformatPuzzleString(statestr, NdSolve.newPuzzle(length,4));
        //System.out.println("GRID:" + newline + statestr);
        return statestr;
    } // end grid2String

    private static MagicCube.TwistData triple2Twist(int face, int fromFace, int toFace, int[][] colorToDir) {
        MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
        int[] otherdir = new int[4];
        Vec_h._VXVXV4(otherdir, colorToDir[face], colorToDir[fromFace], colorToDir[toFace]);
        Vec_h._SXVPSXV(grip.coords, 3, colorToDir[face], 2, otherdir);
        PolygonManager.fillStickerspecFromCoordsAndLength(grip, 3);
        MagicCube.TwistData twist = new MagicCube.TwistData(grip, MagicCube.CCW, 1);
        return twist;
    }

    /*
	 * Set the puzzle to the pristine state
	 */
    public void reset(int new_length) {
        if(new_length != -1) {
            this.length = new_length;
            nstickers = MagicCube.totalStickers(length);
            the_state = new int[nstickers];
        }
        int nstickersperface = length * length * length;
        for(int i=0; i<nstickers; ++i)
            the_state[i] = i / nstickersperface;
    }
    public void reset() {
        reset(length);
    }

    public boolean isSolved()
    {
        int nstickersperface = length * length * length;
        for(int i=0; i<nstickers; ++i)
            if(the_state[i] != the_state[i / nstickersperface * nstickersperface])
                return false;
        return true;
    }

    public int idToColor(int id)
    {
        return the_state[id];
    }

    public void twist(MagicCube.TwistData move) {
        twist(move.grip, move.direction, move.slicemask);
    }

    public void twist(MagicCube.Stickerspec grip, int dir, int slicesmask)
    {
        int dest[] = new int[MagicCube.MAXSTICKERS];
        int nranges, ranges[][] = new int[MagicCube.MAXLENGTH][2];
        int mat[][] = new int[4][4];
        int sgn, whichax;

        /*============= this is verbatim from polymgr.c =========*/
        /*====== FIX THIS === it's a ridiculous hack =============*/
        // It just got even more ridiculous getting it to compile -- DG
        // TODO: Remove dependancy on PolygonManager -- MG
        {
            float
                total_angle = 0,
                center[] = new float[4],
                real_stickercoords[] = new float[4],
                realmat[][] = new float[4][4];

            /*
                * Note: due to run-time errors, I changed the ternary initialization
                * of total_angle into a switch statement below. - DG
                */
            switch (grip.dim)
            {
                case 0:
                    total_angle = (float)(2*Math.PI / 3 * dir);
                    break;
                case 1:
                    total_angle = (float)(2*Math.PI / 2 * dir);
                    break;
                case 2:
                    total_angle = (float)(2*Math.PI / 4 * dir);
                    break;
//	        default:
//	            abort();
            }
            /*
                * Calculate rotation center; it's the center of the face containing
                * the grip
                */
            whichax = -1;
            for (int i = 0; i < MagicCube.NDIMS; ++i)
            {
                if (Math.abs(grip.coords[i]) == 3)
                {
                    center[i] = grip.coords[i];
                    whichax = i;
                }
                else
                    center[i] = 0;
            }
            //assert(INRANGE(0 <=, whichax, <NDIMS));
            if(whichax < 0 || whichax >= MagicCube.NDIMS) {
                System.err.println(whichax + " whichax out of range 0..3");
                return;
            }
            for(int i=0; i<MagicCube.NDIMS; i++)
                real_stickercoords[i] = grip.coords[i]; // would use SET4 but for type mismatch

            Math4d.get4dRotMatrix(center, real_stickercoords, total_angle, realmat);
            for (int i = 0; i < 4; ++i)
                for (int j = 0; j < 4; ++j)
                {
                    mat[i][j] = Math.round(realmat[i][j]);
                    if(Math.abs(realmat[i][j] - mat[i][j]) > .0001) {
                        System.err.println("PuzzleState.twist: matrix copy error");
                        return;
                    }
                }
            sgn = MagicCube.SGN(center[whichax]);
            nranges = polymgr.makeRangesInt(slicesmask, sgn, ranges);
        }
        /*=========================================================*/

        for (int i = 0; i < nstickers; ++i)
        {
            MagicCube.Stickerspec tempsticker = new MagicCube.Stickerspec();
            tempsticker.id_within_puzzle = i;
            polymgr.fillStickerspecFromId(tempsticker);
            int j;
            for (j = 0; j < nranges; ++j)
            {
                if (ranges[j][0] > tempsticker.coords[whichax] ||
                    tempsticker.coords[whichax] > ranges[j][1])
                    continue;
                Vec_h._VXM4i(tempsticker.coords, tempsticker.coords, mat);
                polymgr.fillStickerspecFromCoords(tempsticker);
                //assert(INRANGE(0 <=, tempsticker.id_within_cube, <nstickers));
                if(tempsticker.id_within_puzzle < 0 || tempsticker.id_within_puzzle >= nstickers) {
                    System.err.println(tempsticker.id_within_puzzle + " out of range 0.." + nstickers);
                    break;
                }
                dest[tempsticker.id_within_puzzle] = the_state[i];
                break;
            }
            if (j == nranges)       /* it didn't fall in any of the ranges */
                dest[i] = the_state[i];
        }

        System.arraycopy(dest, 0, the_state, 0, nstickers);
    } // end twist


    public String toString() {
        int i, nstickersperface = length * length * length;
        StringBuffer buf = new StringBuffer();
        for (i=0; i<nstickers; ++i) {
            buf.append(the_state[i]);
            if ((i + 1) % nstickersperface == 0)
                buf.append(System.getProperty("line.separator"));
        }
        return buf.toString();
    }

    /**
     * The inverse of toString.
     */
    public void init(String stateStr) {
        int idWithinCube = 0;
        for (int i=0; i<stateStr.length(); ++i) {
            int ch = stateStr.charAt(i);
            if(Character.isDigit(ch))
                the_state[idWithinCube++] = ch - '0';
        }
    }

    @SuppressWarnings("unused")
	private boolean isSane(int state[])
    {
        int i, counts[] = new int[MagicCube.NFACES];

        for (i = 0; i < MagicCube.NFACES; ++i)
            counts[i] = 0;
        for (i = 0; i < nstickers; ++i)
            counts[state[i]]++;
        for (i = 0; i < MagicCube.NFACES; ++i)
            if (counts[i] != length * length * length)
                return false;
        return true;
    }

    /*
      * Return 1 on success, 0 on error.
      * FIX THIS-- decide whether we want to also print an error message.
      */
//	int PuzzleState::read(FILE *fp)
//	{
//	    int         i;
//	    int         temp_state[MAXSTICKERS];
//
//	    for (i = 0; i < nstickers; ++i)
//	    {
//	        if (fscanf(fp, "%1d", &temp_state[i]) != 1)
//	            return 0;
//	    }
//
//	    if (!isSane(temp_state))
//	    {
//	        /* fprintf(stderr, "That puzzle state was insane!\n"); */
//	        return 0;
//	    }
//	    else
//	    {
//	        for (i = 0; i < nstickers; ++i)
//	            the_state[i] = temp_state[i];
//	        return 1;
//	    }
//	}


    public static void main(String[] args) {
        PolygonManager polymgr = new PolygonManager();
        PuzzleState puzzle = new PuzzleState(polymgr);
        MagicCube.Stickerspec tempsticker = new MagicCube.Stickerspec();
        tempsticker.id_within_puzzle = 5;
        polymgr.fillStickerspecFromId(tempsticker);
        puzzle.twist(tempsticker, MagicCube.CW, 1);
        System.out.println(puzzle);
    }

}
