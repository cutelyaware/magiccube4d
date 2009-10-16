//
// This file is mostly throwaway--
// it is an attempt to quickly glue the good new classes:
//      PuzzleDescription (interface)
//      PolytopePuzzleDescription (implements PuzzleDescription)
//      PipelineUtils
// onto MC4DSwing/MC4DView with as minimal impact on the existing code
// as possible, prior to Melinda getting a look at it
// and figuring out where it should really go.

import com.donhatchsw.util.VecMath;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class PuzzleManager
{
	public static int verboseLevel = 0; // set to something else to debug

    public PuzzleDescription puzzleDescription = null;
    public int puzzleState[] = null;
    public float faceRGB[][];

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


    public void initPuzzle(final String schlafli, final String lengthString, JProgressBar progressView, final JLabel statusLabel, boolean inBackground, final Callback cb) {

    	statusLabel.setText("");
    	ProgressManager builder = new ProgressManager(progressView) {
            /*
             * Main task. Executed in background thread.
             */
            @Override
            public Void doInBackground() {
            	puzzleDescription = buildPuzzle(schlafli, lengthString, this);
            	if( puzzleDescription != null )
            		puzzleState = VecMath.copyvec(puzzleDescription.getSticker2Face());
                faceRGB = ColorUtils.generateVisuallyDistinctRGBs(puzzleDescription.nFaces(), .7f, .1f);
            	return null;
    		}

            /*
             * Executed on graphics thread.
             */
			@Override
			public void done() {
				statusLabel.setText(schlafli + "  length="+lengthString);
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
    // It's more noticeable here than for twists because very small angles are possible here.  
    // Really we'd like to bound the max acceleration.
    public int calculateNTwists( double totalRotationAngle, double twistFactor )
    {
    	return (int)(totalRotationAngle/(Math.PI/2) * 11 * twistFactor);
    }
    

    public boolean mouseMovedAction( MouseEvent e )
    {
    	mouseMovedX = e.getX();
    	mouseMovedY = e.getY();
    	return(updateStickerHighlighting());
    } // mouseMovedAction
    private int mouseMovedX, mouseMovedY;
    
    public interface Highlighter
    {
    	public boolean shouldHighlightSticker( PuzzleDescription puzzle, int stickerIndex, int gripIndex, int x, int y );
    }
    private Highlighter highlighter;
    public void setHighlighter(Highlighter highlighter) {
    	this.highlighter = highlighter;
    }
    
    public boolean updateStickerHighlighting()
    {
    	PipelineUtils.PickInfo pick = PipelineUtils.getAllPickInfo(
        		mouseMovedX, mouseMovedY,
        		untwistedFrame,
        		puzzleDescription );
    	int pickedSticker = pick == null ? -1 : pick.stickerIndex;
    	boolean newHighlit = true;
    	
        if( pickedSticker >= 0 && highlighter != null) {
        	// Let the supplied highlighter decide.
        	if( ! highlighter.shouldHighlightSticker( puzzleDescription, 
        			pickedSticker, pick.gripIndex, mouseMovedX, mouseMovedY) )
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

    // XXX Could maybe separate this out
    // XXX into a compute part and a paint part,
    // XXX since they seem to be doing logically separated
    // XXX things with almost entirely disjoint subsets of the parameters
    public void computeAndPaintFrame(
        // These are used by the compute part only
        float faceShrink,
        float stickerShrink,
        RotationHandler rotationHandler,
        float eyeW,
        float eyeZ, // MagicCube.EYEZ
        float scale, // whatever the fuck that means
        float pixels2polySF, // whatever the fuck that means
        int xOff,
        int yOff,

        float towardsSunVec[], // used by compute part if showShadows is true
        boolean showShadows, // used by both compute and paint parts

        // All the rest are for paint the paint part only
        Color ground,
        boolean highlightByCubie,
        Color outlineColor,
        Graphics g,
        float twistFactor,
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
        PipelineUtils.AnimFrame glueFrameToDrawInto = untwistedFrame;

        if (iTwist < nTwist)
        {
            //
            // Twist in progress (and maybe a 4d rot too at the same time)
            //
            glueFrameToDrawInto = twistingFrame;

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
            glueFrameToDrawInto,

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
            new float[][]{{scaleFudge2d*scale/pixels2polySF, 0},
                          {0, -scaleFudge2d*scale/pixels2polySF},           
                          {xOff, yOff}},
            VecMath.normalize(towardsSunVec),
            groundNormal,
            groundOffset);

        // THE COMPUTE PART ENDS HERE
        // THE PAINT PART STARTS HERE (maybe should be a separate function)

        PipelineUtils.paintFrame(
                glueFrameToDrawInto,
                puzzleDescription,
                puzzleState,
                showShadows,
                ground,
                faceRGB,
                highlit ? iStickerUnderMouse :-1,
                highlightByCubie,
                outlineColor,
                g);

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
                // XXX need to update the hovered-over sticker! I think.
                // XXX it would suffice to just call the mouseMoved callback.
            }
        }
    } // computeAndPaintFrame

} // class PuzzleManager
