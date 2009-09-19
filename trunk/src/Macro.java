
import java.io.*;

/**
 * Contains a sequence of moves relative to a set of reference stickers.
 * Has a method that can take other sets of reference stickers in the same pattern as the definition stickers
 * and returns a set of moves the same as would be generated had the definition moves been applied relative
 * to the given ones instead.
 */
public class Macro {
    public final static int MAXREFS = 3;
    private History moves = new History(3);
    private String name;
    private int defRefs[][] = new int[MAXREFS][MagicCube.NDIMS]; // definition reference stickers

    private Macro() {}

    /**
     * Constructs an unnamed empty macro.
     */
    public Macro(MagicCube.Stickerspec appStickers[]) { this("", appStickers); }

    /**
     * Constructs a named, empty macro with set of reference stickers.
     * @param name display name of the new macro.
     * @param defStickers is a set of "definition" reference stickers.
     */
    public Macro(String name, MagicCube.Stickerspec defStickers[]) {
        this.name = name;
        assert(defStickers.length == MAXREFS);
        int[][] refs = stickers2refs(defStickers);
        for(int i=0; i<MAXREFS; i++)
            Vec_h._SET4(this.defRefs[i], refs[i]);
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

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

    private static int[][] stickers2refs(MagicCube.Stickerspec stickers[]) {
        int refs[][] = new int[Macro.MAXREFS][MagicCube.NDIMS];
        for(int i=0; i<refs.length; i++)
            System.arraycopy(stickers[i].coords, 0, refs[i], 0, MagicCube.NDIMS);
        return refs;
    }


    /**
     * Applies a set of application reference stickers and returns a list of macro twists
     * ready to apply the macro relative to those stickers.
     * @param appStickers stickers to apply macro relative to.
     * @return array of twists in application space or null if pattern doesn't match.
     */
    public MagicCube.TwistData[] getTwists(MagicCube.Stickerspec appStickers[]) {
        int appRefs[][] = stickers2refs(appStickers);
        int app2def[][] = new int[4][4];
        if( ! Math4d.get4dMatThatRotatesThese3ToThose3(defRefs[0], defRefs[1], defRefs[2], appRefs[0], appRefs[1], appRefs[2], app2def))
            return null;
        int det = Vec_h._DET4(app2def); // MWAHAHAHA!
        int expected = length();
        MagicCube.TwistData twists[] = new MagicCube.TwistData[expected];
        moves.goToBeginning();
        for(int i=0; i<expected; i++) {
            MagicCube.TwistData move = moves.redo();
            if(move == null)
                return null;
            Vec_h._VXM4i(move.grip.coords, move.grip.coords, app2def);
            PolygonManager.fillStickerspecFromCoordsAndLength(move.grip, 3);
            twists[i] = move;
        }
        assert(moves.redo() == null);
        if(det < 0)
            reverse(twists);
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
    public void write(Writer writer) {
        MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
        try {
            writer.write("@" + name + "@(");
            for (int i = 0; i < defRefs.length; ++i) {
                Vec_h._SET4(sticker.coords, defRefs[i]);
                PolygonManager.fillStickerspecFromCoordsAndLength(sticker, 3);
                writer.write(""+sticker.face + " "+sticker.id_within_face);
                if (i + 1 < defRefs.length)
                    writer.write(" ");
            }
            writer.write(") ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        moves.write(writer);
    }


//    /**
//     * @return Twist on success, null if at end of the macro.
//     */
//    public MagicCube.TwistData getMove(int dir_in, int mat[][]) {
//        MagicCube.TwistData move = (dir_in < 0) ? moves.undo() : moves.redo();
//        if(move == null)
//            return null;
//        Vec_h._VXM4i(move.grip.coords, move.grip.coords, mat);
//        PolygonManager.fillStickerspecFromCoordsAndLength(move.grip, 3);
//        //*dir_out *= det;
//        return move;
//    }


//    private boolean get4dMatThatRotatesThese3ToMy3(int ref0[], int ref1[], int ref2[], int mat[][]) {
//        return Math4d.get4dMatThatRotatesThese3ToThose3(defRefs[0], defRefs[1], defRefs[2], ref0, ref1, ref2, mat) == 1;
//    }

    /**
     * Deserializes a macro from a text stream.
     * @param pr a text reader assumed to be positioned at the beginning of a previously saved macro.
     * @return macro represented by the text or null if invalid.
     */
    public static Macro read(PushbackReader pr) {
        Macro restored = new Macro();
        try {
            int ch;
            do ch = pr.read();
            //while(Character.isWhitespace(ch)); // isWhitespace doesn't exist in 1.4
            while(" \t\n\r\f".indexOf(ch) != -1);
            if(ch != '@') return null;
            char[] charbuf = new char[256];
            int len=0;
            do {
                ch = pr.read();
                charbuf[len++] = (char)ch;
            } while( ! (ch=='@' || ch<=0));
            if(ch != '@') return null;
            restored.name = new String(charbuf, 0, len-1);
            ch = pr.read();
            if(ch != '(') return null;
            MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
            for(int r=0; r<MAXREFS; r++) {
                sticker.face = History.readInt(pr);
                sticker.id_within_face = History.readInt(pr);
                PolygonManager.fillStickerspecFromFaceAndIdAndLength(sticker, 3);
                Vec_h._SET4(restored.defRefs[r], sticker.coords);
            }
            ch = pr.read();
            assert(ch==')');
            ch = pr.read();
            assert(ch==' ');
        } catch (IOException e) {
            e.printStackTrace();
        }
        if( ! restored.moves.read(pr)) {
        	System.out.println("Error reading macro history");
            return null;
        }
        return restored;
    }

}
