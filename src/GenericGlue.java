//
// This file is mostly throwaway--
// it is an attempt to quickly glue the good new classes:
//      GenericPuzzleDescription (interface)
//      PolytopePuzzleDescription (implements GenericPuzzleDescription)
//      GenericPipelineUtils
// onto MC4DSwing/MC4DView with as minimal impact on the existing code
// as possible, prior to Melinda getting a look at it
// and figuring out where it should really go.
//
// Functions currently in here:
//
//    - GenericGlue() constructor
//            MC4DSwing creates one of these on startup and stores it
//            in its member genericGlue;
//            it also points the MC4DView's genericGlue member
//            to the same GenericGlue object.
//    - addItemsToPuzzleMenu()
//            MC4DSwing should call this after it adds its
//            standard items to the puzzle menu.
//            This function installs callbacks that activate
//            and deactivate the genericGlue object
//            in response to the relevant PuzzleMenu items.
//    - isActive()
//            Whether this GenericGlue object is active
//            (i.e. contains a valid puzzle).
//            This will be true whenever the MC4DView's "current puzzle"
//            is a generic puzzle (rather than a standard one).
//
//    - undoAction()
//          use in place if the usual undo action when isActive() is true
//    - redoAction()
//          use in place if the usual redo action when isActive() is true
//    - cheatAction()
//          use in place if the usual cheat action when isActive() is true
//    - scrambleAction()
//          use in place if the usual cheat action when isActive() is true
//
//    - isAnimating()
//          use in place of the usual isAnimating when isActive() is true
//
//    - compute
//    - computeAndPaintFrame()
//          MC4DView calls this from its repaint() method
//          in place of its usual calls to paintFrame(),
//          if isActive() is true
//

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


public class GenericGlue
{
	public static int verboseLevel = 0; // set to something else to debug

    //
    // State.  And not much of it!
    //
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
    public int twistIsUndo; // of twist in progress, if any-- when finished, don't put on undo stack
    //
    // A cheat is in progress if cheating == true.
    //
    public boolean cheating;

    //
    // The sticker and cubie that the mouse is currently hovering over.
    //
    public int iStickerUnderMouse = -1;


    //
    // Rudimentary undo queue.
    //
    public static class HistoryNode
    {
        public int iGrip;
        public int dir;
        public int slicemask;
        public HistoryNode(int iGrip, int dir, int slicemask)
        {
            this.iGrip = iGrip;
            this.dir = dir;
            this.slicemask = slicemask;
        }
    }
    java.util.Vector<HistoryNode> undoq = new java.util.Vector<HistoryNode>(); // of HistoryNode
    int undoPartSize = 0; // undoq has undo part followed by redo part

    //
    // Two scratch Frames to use for computing and painting.
    //
    GenericPipelineUtils.Frame untwistedFrame = new GenericPipelineUtils.Frame();
    GenericPipelineUtils.Frame twistingFrame = new GenericPipelineUtils.Frame();
        { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something


    public interface Callback { public void call(); }


    public void initPuzzle(final String schlafli, final String lengthString, JProgressBar progressView, final JLabel statusLabel, boolean inBackground) {

    	statusLabel.setText("");
    	ProgressManager builder = new ProgressManager(progressView) {
            /*
             * Main task. Executed in background thread.
             */
            @Override
            public Void doInBackground() {
            	genericPuzzleDescription = buildPuzzle(schlafli, lengthString, this);
                genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());

                undoq.setSize(0);
                undoPartSize = 0;
            	return null;
    		}

            /*
             * Executed on graphics thread.
             */
			@Override
			public void done() {
				statusLabel.setText(schlafli + "  length="+lengthString);
				super.done();
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
            initPuzzle(initialSchlafli, ""+initialLength, progressView, new JLabel(), false);
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue ctor");
    }

    public boolean isActive()
    {
        return genericPuzzleDescription != null;
    }
    public void deactivate()
    {
    	genericPuzzleDescription = null;
        genericPuzzleState = null;
    }
    public boolean isAnimating()
    {
        return iRotation < nRotation
            || iTwist < nTwist;
    }

    // Call this from MC4DSwing ctor right after all
    // the other menu items are added
    public void addItemsToPuzzleMenu(Menu puzzlemenu, final JLabel statusLabel, final JProgressBar progressView)
    {
        if (verboseLevel >= 1) System.out.println("in GenericGlue.addItemsToPuzzleMenu");

        // Selecting any of the previously existing menu items
        // should have the side effect of setting
        // genericPuzzleDescription to null-- that's the indicator
        // that the glue overlay mechanism is no longer active.
        for (int i = 0; i < puzzlemenu.getItemCount(); ++i)
        {
            puzzlemenu.getItem(i).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    if (verboseLevel >= 1) System.out.println("GenericGlue: deactivating");
                    deactivate();
                }
            });
        }

        String table[][] = {
            {"{3,3,3}",  "1,1.9,2,3,4,5,6,7", "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Prism Prism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Prism Prism"},
            {"{4}x{5}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Prism Prism (alt)"},
            {"{6}x{4}",  "1,2,2.5,3,4,5,6,7", "Hexagonal Prism Prism"},
            {"{7}x{4}",  "1,2,2.5,3,4,5,6,7", "Heptagonal Prism Prism"},
            {"{8}x{4}",  "1,2,2.5,3,4,5,6,7", "Octagonal Prism Prism"},
            {"{9}x{4}",  "1,2,2.5,3,4,5,6,7", "Nonagonal Prism Prism"},
            {"{10}x{4}", "1,2,2.5,3,4,5,6,7", "Decagonal Prism Prism"},
            {"{100}x{4}","1,3",               "Onehundredagonal Prism Prism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,2.5,3,4,5,6,7", ""},
            {"{5}x{5}",  "1,2,2.5,3,4,5,6,7", ""}, // XXX 2 is ugly, has slivers
            {"{5}x{10}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{5}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{10}", "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism"},
            {"{}x{5,3}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism (alt)"},
            {"{5,3,3}",  "1,2,2.5,3",         "Hypermegaminx (BIG!)"},
            {null,       "",                  "Invent my own!"},
        };
        for (int i = 0; i < table.length; ++i)
        {

            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli==null ? table[i][2] :
                                 schlafli + "  " + table[i][2]);

            // Puzzles with triangles kind of suck so far,
            // so we might want to leave them out of the menu...
            boolean allowPuzzlesWithTriangles = true;
            //boolean allowPuzzlesWithTriangles = false;
            if (!allowPuzzlesWithTriangles)
            {
                if (schlafli != null && schlafli.indexOf("{3") != -1)
                    continue;
            }

            Menu submenu;
            if (schlafli != null)
            {
                submenu = new Menu(name+"    "); // XXX padding so the > doesn't clobber the end of the longest names!? lame
                puzzlemenu.add(submenu);
            }
            else
                submenu = puzzlemenu;
            for (int j = 0; j < lengthStrings.length; ++j)
            {
                final String lengthString = lengthStrings[j];
                submenu.add(new MenuItem(schlafli==null ? name : ""+lengthString)).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                    	initPuzzle(schlafli, lengthString, progressView, statusLabel, true);
                    }
                });
            }
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue.addItemsToPuzzleMenu");
    } // addItemsToPuzzleMenu
    
    public static GenericPuzzleDescription buildPuzzle(String schlafli, String lengthString, ProgressManager progressView) {
        if (schlafli == null)
        {
            String prompt = "Enter your invention:";
            String initialInput = "{4,3,3} 3";

            while (true)
            {
                String reply = JOptionPane.showInputDialog(prompt, initialInput);
                if (reply == null)
                {
                    return null; // Canceled
                }
                String schlafliAndLength[] = reply.trim().split("\\s+");
                if (schlafliAndLength.length != 2)
                {
                    prompt = "Can not build your invention.\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length. Try again!";
                    initialInput = reply;
                    continue;
                }
                schlafli = schlafliAndLength[0];
                lengthString = schlafliAndLength[1];
                break; // got it
            }
        }
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
    	return (int)(totalRotationAngle/(Math.PI/2) * MagicCube.NFRAMES_180 * twistFactor);
    }
    
    public void undoAction(Canvas view, JLabel statusLabel, float twistFactor)
    {
        GenericGlue glue = this;
        if (glue.undoPartSize > 0)
        {
            if (glue.iTwist < glue.nTwist)
            {
                // Twist already in progress.
                // XXX should add this one to a queue.
                // XXX for now, just prohibit it.
                System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                return;
            }

            GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)glue.undoq.get(--glue.undoPartSize);

            //
            // Initiate the undo twist (opposite dir from original)
            //
            int order = glue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order;
            glue.nTwist = calculateNTwists( totalRotationAngle, twistFactor );
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir = -node.dir;
            glue.twistSliceMask = node.slicemask;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to undo.");
    } // undoAction

    public void redoAction(Canvas view, JLabel statusLabel, float twistFactor)
    {
        GenericGlue glue = this;
        if (glue.undoq.size()-glue.undoPartSize > 0)
        {
            if (glue.iTwist < glue.nTwist)
            {
                // Twist already in progress.
                // XXX should add this one to a queue.
                // XXX for now, just prohibit it.
                System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                return;
            }

            GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)glue.undoq.get(glue.undoPartSize++);

            //
            // Initiate the redo twist (same dir as original)
            //
            int order = glue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order;
            glue.nTwist = calculateNTwists( totalRotationAngle, twistFactor );
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir = node.dir;
            glue.twistSliceMask = node.slicemask;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to redo.");
    } // redoAction

    public void cheatAction(Canvas view, JLabel statusLabel)
    {
        GenericGlue glue = this;
        glue.cheating = true; // each repaint will trigger another til done
        view.repaint();
        statusLabel.setText("");
    } // cheatAction

    public void scrambleAction(Canvas view, JLabel statusLabel, int scramblechenfrengensen)
    {
        GenericGlue glue = this;
        java.util.Random rand = new java.util.Random();
        int previous_face = -1;
        for(int s = 0; s < scramblechenfrengensen; s++) {
            // select a random grip that is unrelated to the last one (if any)
            int iGrip, iFace, order;
            do {
                iGrip = rand.nextInt(glue.genericPuzzleDescription.nGrips());
                iFace = glue.genericPuzzleDescription.getGrip2Face()[iGrip];
                order = glue.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];
            }
            while (
                order < 2 || // don't use trivial ones
                iFace == previous_face || // mixing it up
                (previous_face!=-1 && glue.genericPuzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
            previous_face = iFace;
            int slicemask = 1<<rand.nextInt(2); // XXX there's no getLength()! oh I think it's because I didn't think that was a generic enough concept to put in GenericPuzzleDescription, but I might have to rethink that.  for now, we just pick the first or second slice... this is fine for up to 4x, and even 5x (sort of)
            int dir = rand.nextBoolean() ? -1 : 1;

            glue.genericPuzzleDescription.applyTwistToState(
                    glue.genericPuzzleState,
                    iGrip,
                    dir,
                    slicemask);

            glue.undoq.setSize(glue.undoPartSize); // clear redo part
            glue.undoq.addElement(new GenericGlue.HistoryNode(
                                            iGrip,
                                            dir,
                                            slicemask));
            glue.undoPartSize++;

        }
        view.repaint();
        boolean fully = scramblechenfrengensen == MagicCube.FULL_SCRAMBLE;
        // scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL; XXX do we need to do this here?
        statusLabel.setText(fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen==1?"":"s"));
    } // scrambleAction





    public void mouseMovedAction(MouseEvent e,
                                 Canvas view)
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
    		Canvas view )
    {
        GenericGlue genericGlue = this;
        boolean isRotate = e.isControlDown() || isMiddleMouseButton(e);
        
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

        if (isRotate)
        {
            float nicePoint[] = GenericPipelineUtils.pickPointToRotateToCenter(
                             e.getX(), e.getY(),
                             genericGlue.untwistedFrame,
                             genericGlue.genericPuzzleDescription, 
                             rotationHandler.settings.snapSetting );

            if (nicePoint != null)
            {
                //
                // Initiate a rotation
                // that takes the nice point to the center
                // (i.e. to the -W axis)
                // 

                double nicePointD[] = new double[4];
                for (int i = 0; i < 4; ++i)
                    nicePointD[i] = (double)nicePoint[i];

                double nicePointOnScreen[] = com.donhatchsw.util.VecMath.vxm( nicePointD, rotationHandler.current4dView() );
                com.donhatchsw.util.VecMath.normalize(nicePointOnScreen, nicePointOnScreen); // if it's not already
                double minusWAxis[] = {0,0,0,-1};
                double totalRotationAngle = com.donhatchsw.util.VecMath.angleBetweenUnitVectors(
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
        } // isRotate
        else // !isRotate, it's a twist
        {
            int iGrip = GenericPipelineUtils.pickGrip(
                            e.getX(), e.getY(),
                            genericGlue.untwistedFrame,
                            genericGlue.genericPuzzleDescription);
            if (iGrip != -1)
            {
                int order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];

               
                //System.err.println("    Grip "+iGrip+"");
                //System.err.println("        order "+order);

                if (genericGlue.iTwist < genericGlue.nTwist)
                {
                    // Twist already in progress.
                    // XXX should add this one to a queue.
                    // XXX for now, just prohibit it.
                    System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                    return;
                }


                if (order <= 0)
                {
                    System.err.println("Can't twist that.\n");
                    return;
                }

              int dir = (isLeftMouseButton(e) || isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;

                //if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                //    dir *= 2;

                double totalRotationAngle = 2*Math.PI/order;
                genericGlue.nTwist = calculateNTwists( totalRotationAngle, twistFactor );
                genericGlue.iTwist = 0;
                genericGlue.iTwistGrip = iGrip;
                genericGlue.twistDir = dir;
                genericGlue.twistSliceMask = slicemask;

                //
                // Stick it in the undo queue now, instead
                // of at the end of the animation.
                // It's easier for us to do it
                // here than for the guy at the end to do it,
                // because he would have to decide to do it or not
                // depending on whether it was an undo
                // (and there's currently no mechanism for him
                // to know that).
                //
                // XXX seems like it would be better policy
                // XXX to add to the undo queue at the same time
                // XXX as the move is applied to the state...
                // XXX so we should probably apply the move
                // XXX to the state here too, which means
                // XXX we have to modify what gets passed
                // XXX to the getFrame functions (i.e.
                // XXX tell it the twist is going towards
                // XXX the current state array instead of
                // XXX away from it)
                // 
                genericGlue.undoq.setSize(genericGlue.undoPartSize); // clear redo part
                genericGlue.undoq.addElement(new GenericGlue.HistoryNode(
                                                    genericGlue.iTwistGrip,
                                                    genericGlue.twistDir,
                                                    genericGlue.twistSliceMask));
                genericGlue.undoPartSize++;

                view.repaint();
            }
            else
                System.out.println("missed");
        }
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
        Canvas view)
    {
        GenericGlue genericGlue = this;

        // steal PolygonManager's stuff-- this should be an interface but that's not allowed here apparently
        abstract class InterpFunc { public abstract float func(float f); }
        InterpFunc sine_interp = new InterpFunc() {
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
            double incmatD[][] = com.donhatchsw.util.VecMath.makeRowRotMatThatSlerps(genericGlue.rotationFrom, genericGlue.rotationTo, incFrac);
            copy = com.donhatchsw.util.VecMath.mxm(copy, incmatD);
            com.donhatchsw.util.VecMath.gramschmidt(copy, copy);

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

            fracIntoTwist = (float)interp.func((genericGlue.iTwist+1)/(float)genericGlue.nTwist);
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
        float scaleFudge3d = 1.f;
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

            com.donhatchsw.util.VecMath.mxs(viewMat4df, scaleFudge4d),
            eyeW,
            com.donhatchsw.util.VecMath.mxmxmxm(
                com.donhatchsw.util.VecMath.makeRowRotMat(3, 2, 1, (float)Math.PI/2),
                com.donhatchsw.util.VecMath.makeRowRotMat(3, 2, 0, 1.f*(float)MagicCube.TWIRL*(float)Math.PI/180.f),
                //com.donhatchsw.util.VecMath.makeRowRotMat(3, 1, 2, 1.f*(float)MagicCube.TILT*(float)Math.PI/180.f),
                com.donhatchsw.util.VecMath.makeRowRotMat(3, 1, 2, 1.f*15*(float)Math.PI/180.f),
                com.donhatchsw.util.VecMath.mxs(rotationHandler.current3dView(), scaleFudge3d)),
            eyeZ,

            new float[][]{{scaleFudge2d*scale/pixels2polySF, 0},
                          {0, -scaleFudge2d*scale/pixels2polySF},
                          {(float)xOff, (float)yOff}},
            com.donhatchsw.util.VecMath.normalize(towardsSunVec),
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
                // The move has already been recorded in the undo queue
                // (if it's a forward move and not an undo).
                genericGlue.genericPuzzleDescription.applyTwistToState(
                            genericGlue.genericPuzzleState,
                            genericGlue.iTwistGrip,
                            genericGlue.twistDir,
                            genericGlue.twistSliceMask);
                // XXX need to update the hovered-over sticker! I think.
                // XXX it would suffice to just call the mouseMoved callback... but maybe we don't want to do that after every frame in the cheat
            }
        }
        if (genericGlue.iTwist == genericGlue.nTwist
         && genericGlue.cheating)
        {
            // End of a twist that's part of a cheat
            if (genericGlue.undoPartSize == 0)
            {
                // End of the cheat.
                genericGlue.cheating = false;
            }
            else
            {
                // Initiate the next undo twist in the cheat.
                // XXX duplicate code from the undo menu item-- make a function out of this

                GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)genericGlue.undoq.get(--genericGlue.undoPartSize);

                //
                // Initiate the undo twist (opposite dir from original)
                //
                int order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
                double totalRotationAngle = 2*Math.PI/order;
                genericGlue.nTwist = calculateNTwists( totalRotationAngle, twistFactor ); 
                genericGlue.iTwist = 0;
                genericGlue.iTwistGrip = node.iGrip;
                genericGlue.twistDir = -node.dir;
                genericGlue.twistSliceMask = node.slicemask;

                view.repaint();
            }
        }
    } // computeAndPaintFrame




    /*
     * Shamelessly copied from someone who shamelessly copied it from SwingUtilities.java since that is in JDK 1.3 and we'd like to keep this to 1.2 and below.
     */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK);
    }
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
         return ((anEvent.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }


} // class GenericGlue
