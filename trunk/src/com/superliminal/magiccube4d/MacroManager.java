package com.superliminal.magiccube4d;

import java.util.Vector;
import java.io.*;

import com.donhatchsw.util.VecMath;

/**
 * Contains a list of macros and manages their construction, saving, storing, and serving.
 * Construction proceeds in phases.
 * <ol>
 *     <li>open a new macro,
 *     <li>add reference stickers one at a time,
 *     <li>record twists one at a time,
 *     <li>name and close the macro.
 * </ol>
 * Two public properties are exposed specifying these phases:
 * <ol>
 *     <li>"isOpen" is true between calls to open and close, false otherwise.
 *     <li>"recording" is true while open but only after all reference stickers have been specified.
 * </ol>
 * <br>
 *
 * Created July 15, 2006
 * @author Melinda Green
 */
public class MacroManager implements PuzzleManager.Highlighter {
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
        BufferedReader reader;
        try { reader = new BufferedReader(new FileReader(absPath)); }
        catch (FileNotFoundException fnfe) {
            return; // no problem as the user may not have saved any macros yet.
        }
        // the file does exist so read the macros from it.
        try 
        {
        	// Read header info.
        	// XXX - It'd be nice to get info about failures here out to the UI.
            String firstlineStr = reader.readLine();
            String firstline[] = firstlineStr.split( " " );
            if( firstline.length != 2 || !MagicCube.MAGIC_NUMBER.equals( firstline[0] ) )
                throw new IOException();
            int readversion = Integer.parseInt(firstline[1]);
            if( readversion != MagicCube.MACRO_FILE_VERSION ) 
            	throw new IOException();
            
            Macro aMacro;
            while((aMacro = Macro.read(reader)) != null)
                macros.add(aMacro);
        	reader.close(); 
        }
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
        
        writer.write(
                MagicCube.MAGIC_NUMBER + " " +
                MagicCube.MACRO_FILE_VERSION + 
                System.getProperty( "line.separator" ) );     
        
        for (int i = 0; i < macros.size(); ++i)
        {
            Macro aMacro = macros.get(i);
            aMacro.write( writer );
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
        return macros.toArray(new Macro[0]);
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
    
    @Override
	public boolean shouldHighlightSticker( PuzzleDescription puzzle, int stickerIndex, int gripIndex, int slicemask, int x, int y )
	{	
		// Macros are currently grip based, though that may change in the future.
		MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
		grip.id_within_puzzle = gripIndex;
		
		if( !refDeterminesUniqueOrientation( puzzle, grip ) )
			return false;
		
		return true;
	}
    
    private boolean colinear( double p1[], double p2[], double p3[] )
    {
    	double v1[] = VecMath.normalize( VecMath.vmv( p2, p1 ) );
    	double v2[] = VecMath.normalize( VecMath.vmv( p3, p1 ) );
    	double a = VecMath.angleBetweenUnitVectors( v1, v2 );
    	double eps = 1e-6;
    	return Math.abs( a ) < eps || Math.abs( Math.PI - a ) < eps;
    }
    
    private boolean refDeterminesUniqueOrientation( PuzzleDescription puzzle, MagicCube.Stickerspec ref )
    {
        // We need to make sure the refs will determine a unique orientation.
        // There are a number of click patterns which will fail to do so.
    	
        // Make sure the same ref isn't used twice.
        for( int i=0; i<nrefs; i++ )
        {
        	if( refStickers[i].id_within_puzzle == ref.id_within_puzzle )
        		return false;
        }
        
        // Disallow 3 refs that are colinear with each other.
        if( nrefs == 2 )
        {
        	if( colinear(
        		Macro.getMacroRefCoords( refStickers[0], puzzle ),
        		Macro.getMacroRefCoords( refStickers[1], puzzle ),
        		Macro.getMacroRefCoords( ref, puzzle ) ) )
        			return false;
        }
        
		// Disallow refs coincident with the implicit face center.
		if( nrefs > 0 )
		{
			if( VecMath.equals( 
					Macro.getMacroRefFaceCoords( refStickers[0], puzzle ), 
					Macro.getMacroRefCoords( ref, puzzle ), 1e-6 ) )
				return false;
		}
        
        // Disallow refs that are colinear with the implicit face center.
        for( int i=0; i<nrefs; i++ )
        {
        	if( colinear(
        		Macro.getMacroRefFaceCoords( refStickers[0], puzzle ),
        		Macro.getMacroRefCoords( refStickers[i], puzzle ),
        		Macro.getMacroRefCoords( ref, puzzle ) ) )
        			return false;
        }
        
        return true;
    }
    
    /**
     * Adds a reference sticker to the currently open macro.
     * Returns false if the ref won't determine a unique orientation.
     */
    public boolean addRef( PuzzleDescription puzzle, MagicCube.Stickerspec sticker ) {
        assert(nrefs<Macro.MAXREFS);
        
        if( !refDeterminesUniqueOrientation( puzzle, sticker ) )
        	return false;

        refStickers[nrefs++] = sticker;
        if(nrefs == Macro.MAXREFS) {
            curMacro = new Macro( puzzle.getFullPuzzleString(), refStickers );
        }
        
        return true;
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
