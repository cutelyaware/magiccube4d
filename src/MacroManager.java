
import java.util.Vector;
import java.io.*;

/**
 * Contains a list of macros and manages their construction, saving, storing, and serving.
 * Construction proceeds in phases.
 * <ol>
 *     <li>open a new macro,
 *     <li>add reference stickers one at a time,
 *     <li>record twists one at a time,
 *     <li>name and close the macro.
 * </ol>
 * Two public properties are exposed specifing these phases:
  * <ol>
 *     <li>"isOpen" is true between calls to open and close, false otherwise.
 *     <li>"recording" is true while open but only after all reference stickers have been specified.
 * </ol>
 * <br>
 *
 * Created Jul 15, 2006
 * @author Melinda Green
 */
public class MacroManager {
    private String filePath;
    private Vector<Macro> macros = new Vector<Macro>();
    private Macro curMacro;
    private MagicCube.Stickerspec refStickers[];
    private int nrefs;

    /**
     * Constructs a macro manager set to read and write to a given file path.
     */
    public MacroManager(String absPath) {
        filePath = absPath;
        PushbackReader reader;
        try { reader = new PushbackReader(new FileReader(absPath)); }
        catch (FileNotFoundException fnfe) {
            return; // no problem as the user may not have saved any macros yet.
        }
        // the file does exist so read the macros from it.
        Macro aMacro;
        while((aMacro = Macro.read(reader)) != null)
            macros.add(aMacro);
        try { reader.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String newPath) { filePath = newPath; }

    /**
     * Writes the contained list of macros to the current file path.
     * @throws IOException
     */
    public void write() throws IOException {
        Writer writer = new FileWriter(filePath);
        for (int i = 0; i < macros.size(); ++i)
        {
            Macro aMacro = (Macro)macros.get(i);
            aMacro.write(writer);
        }
        writer.close();
    }

    /**
     * @return true if the list of reference stickers is not complete, false otherwise.
     */
    public boolean isOpen() { return refStickers != null; }

    /**
     * @return true if the list of reference stickers is complete and macro is not yet closed, false otherwise.
     */
    public boolean recording() { return isOpen() && curMacro != null; }

    /**
     * @return the number of currently captured reference stickers for the currently open macro.
     */
    public int numRefs() { return nrefs; }

    /**
     * @return the currently contained list of macros.
     */
    public Macro[] getMacros() {
        return (Macro[])macros.toArray(new Macro[0]);
    }

    /**
     * Begins the creation or application of a macro.
     */
    public void open() {
        assert( ! isOpen());
        refStickers = new MagicCube.Stickerspec[Macro.MAXREFS];
        nrefs = 0;
    }

    /**
     * Names and saves a completed macro.
     * @return the new macro.
     */
    public Macro close(String name) {
        assert(recording());
        Macro newMacro = curMacro;
        newMacro.setName(name);
        cancel();
        macros.add(newMacro);
        return newMacro;
    }

    /**
     * Returns just the captured reference stickers and aborts the ability to define a macro.
     * This is meant to be use as a convinient way to collect reference stickers for application to
     * an existing macro.
     * @return list of all stickers added since opening.
     */
    public MagicCube.Stickerspec[] close() {
        MagicCube.Stickerspec[] captured = new MagicCube.Stickerspec[nrefs];
        System.arraycopy(refStickers, 0, captured, 0, nrefs);
        cancel();
        return captured;
    }

    /**
     * Reverts any currently open macro definition.
     */
    public void cancel() {
        curMacro = null;
        refStickers = null;
        nrefs = 0;
    }

    /**
     * Adds a reference sticker to the currently open macro.
     */
    public void addRef(MagicCube.Stickerspec sticker) {
        assert(nrefs<Macro.MAXREFS);
        refStickers[nrefs++] = sticker;
        if(nrefs == Macro.MAXREFS) {
            curMacro = new Macro(refStickers);
        }
    }

    /**
     * Adds a move to the currently recording macro.
     */
    public void addTwist(MagicCube.TwistData twisted) {
        assert(recording());
        curMacro.addMove(twisted);
    }


    public void moveMacro(Macro macro, int offset) {
        int index = macros.indexOf(macro);
        if(index < 0)
            return;
        int newpos = index + offset;
        if(0 > newpos || newpos >= macros.size())
            return;
        macros.remove(macro);
        macros.add(newpos, macro);
    }

    public void removeMacro(Macro macro) {
        macros.remove(macro);
    }

} // end class MacroManager
