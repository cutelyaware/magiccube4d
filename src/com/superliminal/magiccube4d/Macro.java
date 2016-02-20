package com.superliminal.magiccube4d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Writer;

import com.donhatchsw.util.VecMath;

/**
 * Contains a sequence of moves relative to a set of reference stickers (XXX - now grips! is this ok?).
 * Has a method that can take other sets of reference stickers in the same pattern as the definition stickers
 * and returns a set of moves the same as would be generated had the definition moves been applied relative
 * to the given ones instead.
 */
public class Macro {
    public final static int MAXREFS = 3;
    private History moves = new History(3); // TODO: Is edge-length 3 a bug? -MG
    private String puzzleString; // Macros are really relative to specific puzzles, so we save this out.
    private String name;
    private MagicCube.Stickerspec defRefs[] = new MagicCube.Stickerspec[MAXREFS];

    // We aren't actually doing anything with this yet,
    // other than saving it to the file.  The reason I 
    // went ahead and put it there is so we don't have
    // to rev the file format if we start supporting 
    // sticker defined macros.
    private boolean gripBased = true;

    private Macro() {}

    /**
     * Constructs an unnamed empty macro.
     */
    public Macro(String puzzleString, MagicCube.Stickerspec appStickers[]) {
        this(puzzleString, "", appStickers);
    }

    /**
     * Constructs a named, empty macro with set of reference stickers.
     * 
     * @param name display name of the new macro.
     * @param defStickers is a set of "definition" reference stickers.
     */
    public Macro(String puzzleString, String name, MagicCube.Stickerspec defStickers[]) {
        this.puzzleString = puzzleString;
        this.name = name;
        assert (defStickers.length == MAXREFS);
        System.arraycopy(defStickers, 0, this.defRefs, 0, MAXREFS);
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public String getPuzzleString() {
        return puzzleString;
    }

    /**
     * @return the number of moves in this macro.
     */
    public int length() {
        return moves.countMoves(false);
    }

    /**
     * Adds a twist or rotation relative to the definition stickers.
     */
    public void addMove(MagicCube.TwistData move) {
        moves.apply(move);
    }

    /**
     * Removes the last twist or rotation if any. *
     * 
     * @return A move that would undo the last move or null if nothing to undo.
     */
    public MagicCube.TwistData removeMove() {
        return moves.undo();
    }

    public static double[] getMacroRefCoords(MagicCube.Stickerspec grip, PuzzleDescription puzzle) {
        // When/if we have sticker based macros, this method will need to be extended.
        return VecMath.floatToDouble(puzzle.getGripCoords(grip.id_within_puzzle));
    }

    public static double[] getMacroRefFaceCoords(MagicCube.Stickerspec grip, PuzzleDescription puzzle) {
        // When/if we have sticker based macros, this method will need to be extended.
        int faceIndex = puzzle.getGrip2Face()[grip.id_within_puzzle];
        return VecMath.floatToDouble(puzzle.getFaceCenter(faceIndex));
    }

    /**
     * Applies a set of application reference grips and returns a list of macro twists
     * ready to apply the macro relative to those grips.
     * 
     * @param appGrips - grips to apply macro relative to.
     * @return array of twists in application space or null if pattern doesn't match.
     */
    public MagicCube.TwistData[] getTwists(MagicCube.Stickerspec appGrips[], PuzzleDescription puzzle) {
        // Default Coordinates.
        // We also use one of the faces.
        double ref0[] = getMacroRefCoords(defRefs[0], puzzle);
        double ref1[] = getMacroRefCoords(defRefs[1], puzzle);
        double ref2[] = getMacroRefCoords(defRefs[2], puzzle);
        double ref3[] = getMacroRefFaceCoords(defRefs[0], puzzle);

        // Clicked Coordinates.
        double s0[] = getMacroRefCoords(appGrips[0], puzzle);
        double s1[] = getMacroRefCoords(appGrips[1], puzzle);
        double s2[] = getMacroRefCoords(appGrips[2], puzzle);
        double s3[] = getMacroRefFaceCoords(appGrips[0], puzzle);
        double transform[][] = new double[4][4];
        if(!Math4d.get4dMatThatRotatesThese4ToThose4(ref0, ref1, ref2, ref3, s0, s1, s2, s3, transform))
            return null;
        int expected = length();
        MagicCube.TwistData twists[] = new MagicCube.TwistData[expected];
        moves.goToBeginning();
        for(int i = 0; i < expected; i++) {
            MagicCube.TwistData move = moves.redo();
            if(move == null)
                return null;
            // Transform to new grip.
            int gripIndex = move.grip.id_within_puzzle;
            double[] gripCoords = VecMath.floatToDouble(puzzle.getGripCoords(gripIndex));
            double[] newCoords = VecMath.vxm(gripCoords, transform);
            int newGripIndex = puzzle.getClosestGrip(VecMath.doubleToFloat(newCoords));
            move.grip.id_within_puzzle = newGripIndex;
            twists[i] = move;
        }
        assert (moves.redo() == null);
        // We could disallow these, since they don't represent a true rotation of the defaults.
        // Don did this in the old puzzle though, and it's a nice shortcut.
        // Note however, this won't work for all reference sets due to the 
        // checks in get4dMatThatRotatesThese4ToThose4.
        // XXX - make this work for all macro reference sets?
        double det = VecMath.det(transform);
        if(det < 0)
            reverse(twists);
        return twists;
    } // end getTwists()

    public static void reverse(MagicCube.TwistData[] twists) {
        // reverse move order
        for(int i = 0; i < twists.length / 2; i++) {
            MagicCube.TwistData tmp = twists[i];
            twists[i] = twists[twists.length - i - 1];
            twists[twists.length - i - 1] = tmp;
        }
        // reverse twist directions
        for(int i = 0; i < twists.length; ++i)
            twists[i].direction *= -1;
    }

    /**
     * Serializes a macro to a text writer.
     * 
     * @param writer is the text stream to write to.
     */
    public void write(Writer writer) throws IOException {
        MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
        writer.write("@" + name + "@@" + puzzleString + "@(");
        if(this.gripBased)
            writer.write("g ");
        else
            writer.write("s ");
        for(int i = 0; i < defRefs.length; ++i) {
            sticker = defRefs[i];
            writer.write("" + sticker.id_within_puzzle);
            if(i + 1 < defRefs.length)
                writer.write(" ");
        }
        writer.write(") ");
        writer.write(moves.toString() + System.getProperty("line.separator"));
    }

    private static String readStringHelper(PushbackReader pr, int ch) throws IOException {
        if(ch != '@')
            return null;
        char[] charbuf = new char[256];
        int len = 0;
        do {
            ch = pr.read();
            charbuf[len++] = (char) ch;
        } while(!(ch == '@' || ch <= 0));
        if(ch != '@')
            return null;
        return new String(charbuf, 0, len - 1);
    }

    /**
     * Deserializes a macro from a text stream.
     * 
     * @param pr a text reader assumed to be positioned at the beginning of a previously saved macro.
     * @return macro represented by the text or null if invalid.
     */
    public static Macro read(BufferedReader reader) throws IOException {
        PushbackReader pr = new PushbackReader(reader);
        Macro restored = new Macro();
        int ch;
        do
            ch = pr.read();
        //while(Character.isWhitespace(ch)); // isWhitespace doesn't exist in 1.4
        while(" \t\n\r\f".indexOf(ch) != -1);
        restored.name = readStringHelper(pr, ch);
        ch = pr.read();
        restored.puzzleString = readStringHelper(pr, ch);
        if(restored.name == null || restored.puzzleString == null)
            return null;
        ch = pr.read();
        if(ch != '(')
            return null;
        // Read whether this is a grip or sticker based macro.
        ch = pr.read();
        if(ch == 'g')
            restored.gripBased = true;
        else if(ch == 's')
            restored.gripBased = false;
        else
            return null;
        ch = pr.read();
        // Read the references.
        for(int r = 0; r < MAXREFS; r++) {
            MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
            sticker.id_within_puzzle = History.readInt(pr);
            restored.defRefs[r] = sticker;
        }
        ch = pr.read();
        assert (ch == ')');
        ch = pr.read();
        assert (ch == ' ');
        if(!restored.moves.read(pr)) {
            System.out.println("Error reading macro history");
            return null;
        }
        return restored;
    } // end read

} // end class Macro
