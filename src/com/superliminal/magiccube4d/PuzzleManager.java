package com.superliminal.magiccube4d;

import com.donhatchsw.util.VecMath;
import com.superliminal.util.ColorUtils;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import javax.swing.*;

/**
 * Facility for creating, managing, and drawing a current puzzle.
 * Meant to be a model object shared between the UI (controller) 
 * and the rendering code (view).
 * 
 * TODO: Perhaps the drawing part should be moved to another service?
 * TODO: Hide or consider hiding the public data.
 */
public class PuzzleManager
{
	public static int verboseLevel = 0; // set to something else to debug

    public PuzzleDescription puzzleDescription = null;
    public int puzzleState[] = null;
    public Color faceColors[];
    
    public void resetPuzzleState() {
    	if(puzzleDescription != null)
    		puzzleState = VecMath.copyvec(puzzleDescription.getSticker2Face());
    }

    //
    // A rotation is currently in progress if iRotation < nRotation.
    //
    public int nRotation = 0; // total number of rotation frames in progress
    public int iRotation = 0; // number of frames done so far
    public double rotationFrom[]; // where rotation is rotating from, in 4space
    public double rotationTo[]; // where rotation is rotating to, in 4space

    //
    // A twist is currently in progress if iTwist < nTwist.
    //
    public int nTwist = 0; // total number of twist frames in progress
    public int iTwist = 0; // number of twist frames done so far
    public int iTwistGrip; // of twist in progress, if any
    public int twistDir; // of twist in progress, if any
    public int twistSliceMask; // of twist in progress, if any
    
    private int iStickerUnderMouse = -1; // The sticker that the mouse is currently hovering over.
    private boolean highlit = false; // Whether it should be highlighted.

    //
    // Two scratch Frames to use for computing and painting.
    //
    PipelineUtils.AnimFrame untwistedFrame = new PipelineUtils.AnimFrame();
    PipelineUtils.AnimFrame twistingFrame = new PipelineUtils.AnimFrame();
        { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something


    public interface Callback { public void call(); }
    
    static String prettyLength(double length) {
        boolean integralLength = length == (int)length;
        return integralLength ? ""+(int)length : ""+length;
    }
    
    public String getPrettyLength() {
    	return prettyLength(puzzleDescription.getEdgeLength());
    }


    public void initPuzzle(final String schlafli, final String lengthString, JProgressBar progressView, final JLabel statusLabel, boolean inBackground, final Callback cb) {
    	statusLabel.setText("");
        final String finalLengthString = " " + prettyLength(Double.parseDouble(lengthString));
    	ProgressManager builder = new ProgressManager(progressView) {
    		private boolean succeeded = false;
            /*
             * Main task. Executed in background thread.
             */
            @Override
            public Void doInBackground() {
            	PuzzleDescription newPuzzle = buildPuzzle(schlafli, finalLengthString, this);
            	if( newPuzzle != null ) {
            		succeeded = true;
                	puzzleDescription  = newPuzzle;
            		resetPuzzleState();
            		faceColors = ColorUtils.generateVisuallyDistinctColors(puzzleDescription.nFaces(), .7f, .1f);
            	}
            	return null;
    		}

            /*
             * Executed on graphics thread.
             */
			@Override
			public void done() {
				if(succeeded)
					statusLabel.setText(schlafli + "  length = " + finalLengthString);
				super.done();
				if(cb != null) 
					cb.call();
			}
    	};
    	if(inBackground)
    		builder.execute();
    	else
    		builder.run();
    }

    public PuzzleManager(String initialSchlafli, double initialLength, JProgressBar progressView )
    {
        super();
        if (verboseLevel >= 1) System.out.println("in PuzzleManager ctor");
        if (initialSchlafli != null)
        {
            initPuzzle(initialSchlafli, ""+initialLength, progressView, new JLabel(), false, null);
        }
        if (verboseLevel >= 1) System.out.println("out PuzzleManager ctor");
    }

    public boolean isAnimating()
    {
        return iRotation < nRotation
            || iTwist < nTwist;
    }
    
    // XXX - Should we move this to an interface method on the generic puzzle,
    //		 and implement inside there?
    public boolean isSolved()
    {
		 int nFaces = puzzleDescription.nFaces();
		 int faceState[] = new int[nFaces];
		 VecMath.fillvec( faceState, -1 );
		
		 // Cycle through all the stickers.
		 for( int s=0; s<puzzleState.length; s++ )
		 {
			 int faceIndex = puzzleDescription.getSticker2Face()[s];
			 if( faceState[faceIndex] == -1 )
			 {
				 faceState[faceIndex] = puzzleState[s];
				 continue;
			 }

			 // Check for multiple colors on a single face.
			 if( puzzleState[s] != faceState[faceIndex] )
				 return false;
		 }
		
		 // Our faceState vector should have no -1s in it.
		 // Perhaps we should assert this.
		 //System.out.println( "Pristine Puzzle" );
		 return true;
    }

    private static PuzzleDescription buildPuzzle(String schlafli, String lengthString, ProgressManager progressView) {
        double len;
        try { len = Double.parseDouble(lengthString); }
        catch (java.lang.NumberFormatException e)
        {
            System.err.println(lengthString+ " is not a number");
            return null;
        }

        PuzzleDescription newPuzzle = null;
        try
        {
            newPuzzle = new PolytopePuzzleDescription(schlafli, len, progressView);
        }
        catch (Throwable t)
        {
            //t.printStacktrace();
            String explanation = t.toString();
            // yes, this is lame... AND the user
            // can't even cut and paste it to mail it to me
            if (explanation.equals("java.lang.Error: Assertion failed"))
            {
                java.io.StringWriter sw = new java.io.StringWriter();
                t.printStackTrace(new java.io.PrintWriter(sw));
                explanation = "\n" + sw.toString();
            }
            t.printStackTrace();
            System.out.println("Something went very wrong when trying to build your invention \""+schlafli+"  "+lengthString+"\":\n"+explanation);
//            JOptionPane.showMessageDialog(null,
//                "Something went very wrong when trying to build your invention \""+schlafli+"  "+lengthString+"\":\n"+explanation,
//                "Your Invention Sucks",
//                JOptionPane.ERROR_MESSAGE);
            return null;
        }

        int nDims = newPuzzle.nDims();
        if (nDims != 4)
        {
            JOptionPane.showMessageDialog(null,
                "Re: Your invention \""+schlafli+"  "+lengthString+"\"\n"+
                "\n"+
                "That is a truly BRILLIANT "+nDims+"-dimensional invention.\n"+
                "It has:\n"+
                "        "+newPuzzle.nFaces()+" faces\n"+
                "        "+newPuzzle.nStickers()+" stickers\n"+
                "        "+newPuzzle.nCubies()+" visible cubie"+(newPuzzle.nCubies()==1?"":"s")+"\n"+
                "        "+newPuzzle.nVerts()+" sticker vertices\n"+
                "However, we are only accepting 4-dimensional inventions at this time.",
                "Invention Rejection Form Letter",
                JOptionPane.ERROR_MESSAGE);
            // XXX Lame, should try to get back in the loop and prompt again instead
            return null;
        }
        return newPuzzle;
    }

    // XXX unscientific rounding-- and it's too fast for small angles!
    // Really we'd like to bound the max acceleration.
    public int calculateNTwists( double totalRotationAngle, double twistFactor )
    {
    	int nTwists = (int)(totalRotationAngle/(Math.PI/2) * 11 * twistFactor);
    	
    	// We should always do at least one,
    	// or small angle twists can get missed!
    	if( nTwists == 0 )
    		return 1;
    	else
    		return nTwists;
    }
    

    /**
     * Application-supplied callback that can be set to specify when a hovered sticker should be highlighted
     * based on application context.
     */
    public interface Highlighter
    {
    	public boolean shouldHighlightSticker( PuzzleDescription puzzle, 
    			int stickerIndex, int gripIndex, int slicemask, int x, int y, boolean isControlDown );
    }
    public void setHighlighter(Highlighter highlighter) {
    	this.highlighter = highlighter;
    }
    private Highlighter highlighter;
    
    public boolean updateStickerHighlighting(int mouseX, int mouseY, int slicemask, boolean isControlDown )
    {
    	PipelineUtils.PickInfo pick = PipelineUtils.getAllPickInfo(
        		mouseX, mouseY,
        		untwistedFrame,
        		puzzleDescription );
    	int pickedSticker = pick == null ? -1 : pick.stickerIndex;
    	boolean newHighlit = true;
    	
        if( pickedSticker >= 0 && highlighter != null) {
        	// Let the supplied highlighter decide.
        	if( ! highlighter.shouldHighlightSticker( puzzleDescription, 
        			pickedSticker, pick.gripIndex, slicemask, mouseX, mouseY, isControlDown ) )
        	{
        		newHighlit = false;
        	}
        }
        
        boolean changed = pickedSticker != iStickerUnderMouse || newHighlit != highlit;
        if(pickedSticker >= 0 && changed && newHighlit) {
        	Audio.play(Audio.Sound.HIGHLIGHT); // hovering over a newly highlighted sticker
        }
        iStickerUnderMouse = pickedSticker;
        highlit = newHighlit;
        return changed;
    }
    
    // Checks whether or not it is ok to handle a mouse click.
    // All the logic of this is really in the highlighters.
    public boolean canMouseClick()
    {
    	return highlit;
    }
    
    // scratch space for speed. note: not threadsafe.
    private static ViewRotationInfo scratch = new ViewRotationInfo();
    
    public boolean canRotateToCenter( int x, int y, RotationHandler rotationHandler ) 
    {
    	return getViewRotationInfo(x, y, rotationHandler, scratch);
    }

    private static class ViewRotationInfo
    {
    	public double totalRotationAngle;
    	public double[] nicePointOnScreen;
    	public double[] minusWAxis;
    }
    
    private boolean getViewRotationInfo( int x, int y, RotationHandler rotationHandler, ViewRotationInfo info )
    {
        float nicePoint[] = PipelineUtils.pickPointToRotateToCenter(
        	x, y, this.untwistedFrame, this.puzzleDescription, RotationHandler.getSnapSetting() );
        
        if( nicePoint == null )
        	return false;
    	
        //
        // Initiate a rotation
        // that takes the nice point to the center
        // (i.e. to the -W axis)
        // 

        double nicePointD[] = new double[4];
        for (int i = 0; i < 4; ++i)
            nicePointD[i] = nicePoint[i];

        double nicePointOnScreen[] = VecMath.vxm( nicePointD, rotationHandler.current4dView() );
        VecMath.normalize(nicePointOnScreen, nicePointOnScreen); // if it's not already
        double minusWAxis[] = {0,0,0,-1};
        double totalRotationAngle = VecMath.angleBetweenUnitVectors(
                            nicePointOnScreen,
                            minusWAxis);
        
        info.nicePointOnScreen = nicePointOnScreen;
        info.minusWAxis = minusWAxis;
        info.totalRotationAngle = totalRotationAngle;
        
        // Does this do anything?
        return totalRotationAngle > 1e-6;
    }
    
    public void mouseClickedAction( MouseEvent e,
    		RotationHandler rotationHandler,
    		float twistFactor,
    		int slicemask,
    		Component view )
    {        
        /* Uncomment to debug the pick
        {
            int hit[] = PipelineUtils.pick(e.getX(), e.getY(), untwistedFrame, puzzleDescription);
            if (hit != null)
            {
                int iSticker = hit[0];
                int iFace = puzzleDescription.getSticker2Face()[iSticker];
                int iCubie = puzzleDescription.getSticker2Cubie()[iSticker];
                System.err.println("    Hit sticker "+iSticker+"(polygon "+hit[1]+")");
                System.err.println("        face "+iFace);
                System.err.println("        cubie "+iCubie);
            }
        } */

        ViewRotationInfo rotInfo = new ViewRotationInfo();
        if( !getViewRotationInfo( e.getX(), e.getY(), rotationHandler, rotInfo ) )
        {
        	 System.out.println( "missed or invalid" );
        	 return;
        }

        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        nRotation = calculateNTwists( rotInfo.totalRotationAngle, twistFactor );
        // XXX ARGH! we'd like the speed to vary as the user changes the slider,
        // XXX but the above essentially locks in the speed for this rotation
        iRotation = 0; // we are iRotation frames into nRotation
        rotationFrom = rightClick ? rotInfo.minusWAxis : rotInfo.nicePointOnScreen;
        rotationTo = rightClick ? rotInfo.nicePointOnScreen : rotInfo.minusWAxis;
        view.repaint();

        if (iRotation == nRotation)
        {
            // Already in the center
            System.err.println("Can't rotate that.\n");
        }

    	Audio.loop(Audio.Sound.TWISTING);            
    }


    public PipelineUtils.AnimFrame computeFrame(
        float faceShrink,
        float stickerShrink,
        RotationHandler rotationHandler,
        float eyeW,
        float eyeZ,
        float viewScale,
        float pixels2polySF, // scale factor that maps distances in view space to polygon space
        int xOff,
        int yOff,
        float towardsSunVec[], // used if showShadows is true
        boolean showShadows,
        Component view)
    {
        // steal PolygonManager's stuff-- this should be an interface but that's not allowed here apparently
        abstract class InterpFunc { public abstract float func(float f); }
        InterpFunc sine_interp = new InterpFunc() {
            @Override
			public float func(float x) { return (float)(Math.sin((x - .5) * Math.PI) + 1) / 2; }
        };
        /*
        InterpFunc linear_interp = new InterpFunc() {
            public float func(float x) { return x; }
        };*/
        InterpFunc interp = sine_interp;
        //InterpFunc interp = linear_interp;

        double[][] viewMat4d = rotationHandler.current4dView();
        if (iRotation < nRotation)
        {
            //
            // 4d rotation in progress
            //
            double copy[][] = new double[4][4];
            for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
            	copy[i][j] = viewMat4d[i][j];

            double incFrac = interp.func((iRotation+1)/(float)nRotation)
                           - interp.func(iRotation/(float)nRotation);
            double incmatD[][] = VecMath.makeRowRotMatThatSlerps(rotationFrom, rotationTo, incFrac);
            copy = VecMath.mxm(copy, incmatD);
            VecMath.gramschmidt(copy, copy);

            for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                viewMat4d[i][j] = copy[i][j];
            
            //System.out.println("    "+iRotation+"/"+nRotation+" -> "+(iRotation+1)+"/"+nRotation+"");
            iRotation++;
            if(iRotation == nRotation) {
            	Audio.stop(Audio.Sound.TWISTING);
            	Audio.play(Audio.Sound.SNAP);
            }
            view.repaint(); // make sure we keep drawing while there's more to do
        }

        int iGripOfTwist = -1;
        int itwistDir = 0;
        int islicemask = 0;
        float fracIntoTwist = 0.f;
        PipelineUtils.AnimFrame frameToDrawInto = untwistedFrame;

        if (iTwist < nTwist)
        {
            //
            // Twist in progress (and maybe a 4d rot too at the same time)
            //
            frameToDrawInto = twistingFrame;

            iGripOfTwist = iTwistGrip;
            itwistDir = twistDir;
            islicemask = twistSliceMask;

            fracIntoTwist = interp.func((iTwist+1)/(float)nTwist);
            //System.out.println("    "+iTwist+"/"+nTwist+" -> "+(iTwist+1)+"/"+nTwist+"");

            view.repaint(); // make sure we keep drawing while there's more to do
        }

        // old params... but I don't think it was doing it right
        //float[] groundNormal = showShadows ? new float[] {0,1,.1f} : null;
        //float groundOffset = -1.f;

        // XXX why is this a bit diff from old?  well I don't think it was being done right for one thing
        float[] groundNormal = showShadows ? new float[] {0,1,.05f} : null;
        float groundOffset = -1.f;

        // XXX I don't seem to be quite the same as the original... unless I correct it here
        float scaleFudge4d = 1.f;
        //float scaleFudge3d = 1.f;
        float scaleFudge2d = 4.7f;

        float viewMat4df[][] = new float[4][4];
        for( int i=0; i<4; ++i )
        for( int j=0; j<4; ++j )
        	viewMat4df[i][j] = (float)viewMat4d[i][j];
        
        // XXX probably doing this more than necessary... when it's a rest frame that hasn't changed
        PipelineUtils.computeFrame(
            frameToDrawInto,

            puzzleDescription,
            faceShrink,
            stickerShrink,

            iGripOfTwist,
            itwistDir,
            islicemask,
            fracIntoTwist,

            VecMath.mxs(viewMat4df, scaleFudge4d),
            eyeW,
            eyeZ,
            new float[][]{{scaleFudge2d*viewScale/pixels2polySF, 0},
                          {0, -scaleFudge2d*viewScale/pixels2polySF},           
                          {xOff, yOff}},
            VecMath.normalize(towardsSunVec),
            groundNormal,
            groundOffset);
        
        return frameToDrawInto;
    } // end computeFrame
    

	public void paintFrame(
		Graphics g,
		PipelineUtils.AnimFrame frame,
		boolean showShadows,
        Color ground,
        boolean highlightByCubie,
        Color outlineColor,
        float twistFactor)
    {
        PipelineUtils.paintFrame(
        		g,
                frame,
                puzzleDescription,
                puzzleState,
                showShadows,
                ground,
                faceColors,
                highlit ? iStickerUnderMouse :-1,
                highlightByCubie,
                outlineColor);

        if (iTwist < nTwist)
        {
        	if(iTwist == 0) {
        		Audio.loop(Audio.Sound.TWISTING);
        	}
            iTwist++;
            if (iTwist == nTwist)
            {
                // End of twist animation-- apply the twist to the state.
                puzzleDescription.applyTwistToState(
                            puzzleState,
                            iTwistGrip,
                            twistDir,
                            twistSliceMask);
            	Audio.stop(Audio.Sound.TWISTING);
        		Audio.play(Audio.Sound.SNAP);
            }
        }
    } // end paintFrame
    

	private Random rand = new Random();
	
	public int getRandomGrip()
	{
		// Get a random sticker.
		int iSticker, iGrip;
		iSticker = rand.nextInt(puzzleDescription.nStickers());
		
		// Get the grip(s) for this sticker and pick a random one of those.
        int grips[] = PipelineUtils.getGripsForSticker(iSticker, puzzleDescription);
        if( grips.length == 1 )
        	iGrip = grips[0];
        else
        {
        	int idx = rand.nextInt( grips.length );
        	iGrip = grips[idx];
        }
        
        return iGrip;
	}
	
	
	//
	// Everything from here down is just for testing/example purposes.
	// though the scramble methods are public since they could be useful.
	// 
	
    public void scramble(int nTwists) {
    	if(puzzleDescription == null)
    		return;
    	resetPuzzleState();
        int previous_face = -1;
        int[] grip2face = puzzleDescription.getGrip2Face();
        int[] orders = puzzleDescription.getGripSymmetryOrders();
        int[] face2opposite = puzzleDescription.getFace2OppositeFace();
    	for(int s = 0; s < nTwists; s++) {
            // select a random grip that is unrelated to the last one (if any)
            int iGrip, iFace, order;
            do {
                iGrip = getRandomGrip();
                iFace = grip2face[iGrip];
                order = orders[iGrip];
            }
            while (
                order < 2 || // don't use 360 degree twists
                iFace == previous_face || // mixing it up
                (previous_face!=-1 && face2opposite[previous_face] == iFace));
            previous_face = iFace;
            int gripSlices = puzzleDescription.getNumSlicesForGrip(iGrip);
            int slicemask = 1<<rand.nextInt(gripSlices);
            int dir = rand.nextBoolean() ? -1 : 1;
            // apply the twist to the puzzle state.
            puzzleDescription.applyTwistToState(puzzleState, iGrip, dir, slicemask);
        }
    }
    
    public void scrambleFully() {
    	if(puzzleDescription == null)
    		return;
        int totalTwistsNeededToFullyScramble = 
        		puzzleDescription.nFaces() // needed twists is proportional to nFaces
        		* (int)puzzleDescription.getEdgeLength() // and to number of slices
        		* 2; // and to a fudge factor that brings the 3^4 close to the original 40 twists.
        scramble(totalTwistsNeededToFullyScramble + rand.nextInt(10)); // add a few random twists so parity is unpredictable.
    }
    
    public static void main(String[] args) {
    	PuzzleManager puzzleManager = new PuzzleManager("{3,3,3}", 2, new JProgressBar());
    	long start = System.currentTimeMillis();
        int tries = 1000000, solved = 0;
        for(int i=0; i<tries; i++) {
        	puzzleManager.scrambleFully();
        	if(puzzleManager.isSolved()) {
        		solved++;
        		System.out.println(""+solved + " so far out of " + i);
        	}
        }
        System.out.println(solved == 0 ? "none in " + tries + " scrambles are solved" : ""+solved + " in " + tries + "(1 in " + tries/(float)solved + ") scrambles are solved.");
        System.out.println("Total seconds: " + (System.currentTimeMillis() - start)/1000);
    }

} // class PuzzleManager
