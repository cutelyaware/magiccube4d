//
// This file is mostly throwaway--
// it is an attempt to quickly glue the good new classes:
//      GenericPuzzleDescription (interface)
//      PolytopePuzzleDescription (implements GenericPuzzleDescription)
//      GenericPipelineUtils
// onto MC4DSwing/MC4DView with as minimal impact on the existing code
// as possible, prior to Melinda getting a look at it
// and figuring out where it should really go.

import com.donhatchsw.util.VecMath;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


public class GenericGlue
{
	public static int verboseLevel = 0; // set to something else to debug

    public GenericPuzzleDescription genericPuzzleDescription = null;
    public int genericPuzzleState[] = null;

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
    public int iTwistGrip;     // of twist in progress, if any
    public int twistDir;      // of twist in progress, if any
    public int twistSliceMask; // of twist in progress, if any
    
    //
    // The sticker and cubie that the mouse is currently hovering over.
    //
    public int iStickerUnderMouse = -1;

    //
    // Two scratch Frames to use for computing and painting.
    //
    GenericPipelineUtils.Frame untwistedFrame = new GenericPipelineUtils.Frame();
    GenericPipelineUtils.Frame twistingFrame = new GenericPipelineUtils.Frame();
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
            	genericPuzzleDescription = buildPuzzle(schlafli, lengthString, this);
                genericPuzzleState = VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
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

    public GenericGlue(String initialSchlafli, double initialLength, JProgressBar progressView)
    {
        super();
        if (verboseLevel >= 1) System.out.println("in GenericGlue ctor");
        if (initialSchlafli != null)
        {
            initPuzzle(initialSchlafli, ""+initialLength, progressView, new JLabel(), false, null);
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue ctor");
    }

    public boolean isAnimating()
    {
        return iRotation < nRotation
            || iTwist < nTwist;
    }
    
    
    public boolean isSolved() {
    	// TODO: implement me.
    	return false;
    }

    
    private static GenericPuzzleDescription buildPuzzle(String schlafli, String lengthString, ProgressManager progressView) {
        double len;
        try { len = Double.parseDouble(lengthString); }
        catch (java.lang.NumberFormatException e)
        {
            System.err.println(lengthString+ " is not a number");
            return null;
        }

        GenericPuzzleDescription newPuzzle = null;
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
    

    public void mouseMovedAction(MouseEvent e,
                                 Component view)
    {
        GenericGlue genericGlue = this;
        int pickedSticker = GenericPipelineUtils.pickSticker(
                                    e.getX(), e.getY(),
                                    genericGlue.untwistedFrame,
                                    genericGlue.genericPuzzleDescription);
        //System.out.println("    hover sticker "+genericGlue.iStickerUnderMouse+" -> "+pickedSticker+"");
        if (pickedSticker != genericGlue.iStickerUnderMouse)
            view.repaint(); // highlight changed (or turned on or off)
        genericGlue.iStickerUnderMouse = pickedSticker;
    } // mouseMovedAction


    public void mouseClickedAction( MouseEvent e,
    		RotationHandler rotationHandler,
    		float twistFactor,
    		int slicemask,
    		Component view )
    {
        GenericGlue genericGlue = this;
        
        /* Uncomment to debug the pick
        {
            int hit[] = GenericPipelineUtils.pick(e.getX(), e.getY(),
                                                  genericGlue.untwistedFrame,
                                                  genericGlue.genericPuzzleDescription);
            if (hit != null)
            {
                int iSticker = hit[0];
                int iFace = genericGlue.genericPuzzleDescription.getSticker2Face()[iSticker];
                int iCubie = genericGlue.genericPuzzleDescription.getSticker2Cubie()[iSticker];
                System.err.println("    Hit sticker "+iSticker+"(polygon "+hit[1]+")");
                System.err.println("        face "+iFace);
                System.err.println("        cubie "+iCubie);
            }
        } */

        float nicePoint[] = GenericPipelineUtils.pickPointToRotateToCenter(
                         e.getX(), e.getY(),
                         genericGlue.untwistedFrame,
                         genericGlue.genericPuzzleDescription, 
                         rotationHandler.getSnapSetting() );

        if (nicePoint != null)
        {
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

            genericGlue.nRotation = calculateNTwists( totalRotationAngle, twistFactor );
            // XXX ARGH! we'd like the speed to vary as the user changes the slider,
            // XXX but the above essentially locks in the speed for this rotation
            genericGlue.iRotation = 0; // we are iRotation frames into nRotation
            genericGlue.rotationFrom = nicePointOnScreen;
            genericGlue.rotationTo = minusWAxis;
            view.repaint();

            if (genericGlue.iRotation == genericGlue.nRotation)
            {
                // Already in the center
                System.err.println("Can't rotate that.\n");
            }
        }
        else
            System.out.println("missed");
            
    } // mouseClickedAction


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
        float faceRGB[][],
        boolean highlightByCubie,
        Color outlineColor,
        Graphics g,
        float twistFactor,
        Component view)
    {
        GenericGlue genericGlue = this;

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
        if (genericGlue.iRotation < genericGlue.nRotation)
        {
            //
            // 4d rotation in progress
            //
            double copy[][] = new double[4][4];
            for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
            	copy[i][j] = viewMat4d[i][j];

            double incFrac = interp.func((genericGlue.iRotation+1)/(float)genericGlue.nRotation)
                           - interp.func(genericGlue.iRotation/(float)genericGlue.nRotation);
            double incmatD[][] = VecMath.makeRowRotMatThatSlerps(genericGlue.rotationFrom, genericGlue.rotationTo, incFrac);
            copy = VecMath.mxm(copy, incmatD);
            VecMath.gramschmidt(copy, copy);

            for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                viewMat4d[i][j] = copy[i][j];
            
            //System.out.println("    "+genericGlue.iRotation+"/"+genericGlue.nRotation+" -> "+(genericGlue.iRotation+1)+"/"+genericGlue.nRotation+"");
            genericGlue.iRotation++;
            view.repaint(); // make sure we keep drawing while there's more to do
        }

        int iGripOfTwist = -1;
        int twistDir = 0;
        int slicemask = 0;
        float fracIntoTwist = 0.f;
        GenericPipelineUtils.Frame glueFrameToDrawInto = genericGlue.untwistedFrame;

        if (genericGlue.iTwist < genericGlue.nTwist)
        {
            //
            // Twist in progress (and maybe a 4d rot too at the same time)
            //
            glueFrameToDrawInto = genericGlue.twistingFrame;

            iGripOfTwist = genericGlue.iTwistGrip;
            twistDir = genericGlue.twistDir;
            slicemask = genericGlue.twistSliceMask;

            fracIntoTwist = interp.func((genericGlue.iTwist+1)/(float)genericGlue.nTwist);
            //System.out.println("    "+genericGlue.iTwist+"/"+genericGlue.nTwist+" -> "+(genericGlue.iTwist+1)+"/"+genericGlue.nTwist+"");

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
        GenericPipelineUtils.computeFrame(
            glueFrameToDrawInto,

            genericGlue.genericPuzzleDescription,
            faceShrink,
            stickerShrink,

            iGripOfTwist,
              twistDir,
              slicemask,
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

        GenericPipelineUtils.paintFrame(
                glueFrameToDrawInto,
                genericGlue.genericPuzzleDescription,
                genericGlue.genericPuzzleState,
                showShadows,
                ground,
                faceRGB,
                genericGlue.iStickerUnderMouse,
                highlightByCubie,
                outlineColor,
                g);

        if (genericGlue.iTwist < genericGlue.nTwist)
        {
            genericGlue.iTwist++;
            if (genericGlue.iTwist == genericGlue.nTwist)
            {
                // End of twist animation-- apply the twist to the state.
                genericGlue.genericPuzzleDescription.applyTwistToState(
                            genericGlue.genericPuzzleState,
                            genericGlue.iTwistGrip,
                            genericGlue.twistDir,
                            genericGlue.twistSliceMask);
                // XXX need to update the hovered-over sticker! I think.
                // XXX it would suffice to just call the mouseMoved callback.
            }
        }
    } // computeAndPaintFrame

} // class GenericGlue
