package com.superliminal.magiccube4d;

import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.superliminal.util.PropertyManager;
import com.superliminal.util.StaticUtils;


/**
 * Represents a viewer/controller for the Magic Cube 4D puzzle.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DView extends Component {

    private PuzzleManager puzzleManager = null;

    // Listener support
    public static interface StickerListener { public void stickerClicked(InputEvent e, MagicCube.TwistData twisted); }
    private Set<StickerListener> stickerListeners = new HashSet<StickerListener>();
    public void addStickerListener(StickerListener tl) { stickerListeners.add(tl); }
    public void removeStickerListener(StickerListener tl) { stickerListeners.remove(tl); }
    protected void fireStickerClickedEvent(InputEvent event, MagicCube.TwistData twist) {
        for(StickerListener sl : stickerListeners) 
        	sl.stickerClicked(event, twist);
    }

    private AnimationQueue animationQueue;
    public boolean isAnimating() { return animationQueue.isAnimating(); }
    private int slicemask; // bitmap representing which number keys are down
    public int getSlicemask() { return slicemask == 0 ? 1 : slicemask; }
    private Color skyOverride = null; // null means use the user's preference from the PropertyManager.
    private int xOff, yOff;
    private float polys2pixelsSF = .01f; // screen transform data
    private Point lastDrag; // non-null == dragging
    private long lastDragTime; // timestamp of last drag event
    private RotationHandler rotationHandler;
    
    public RotationHandler getRotations() { return rotationHandler; } // const
    
    /**
     * Overrides the user's preference when not null. Set to null to revert.
     */
    public void setSkyOverride(Color so) {
        this.skyOverride = so;
        repaint();
    }

    /**
     * Performs a move and optionally applies it to the history when finished.
     */
    public void animate(MagicCube.TwistData move, boolean applyToHist, boolean macroMove) {
        animationQueue.append(move, applyToHist, macroMove);
        repaint();
    }
    public void animate(MagicCube.TwistData move, boolean applyToHist) {
        animate(move, applyToHist, false);
    }

    /**
     * Performs a sequence of moves and optionally applies each to the history as they finished.
     */
    public void animate(MagicCube.TwistData moves[], boolean applyToHist) {
        for(int i=0; i<moves.length; i++)
            animate(moves[i], applyToHist);
    }
    
    public void animate(History hist, boolean applyToHist) {
        for(Enumeration<MagicCube.TwistData> moves=hist.moves(); moves.hasMoreElements(); )
            animate(moves.nextElement(), applyToHist);
    }

    public void append(char mark) {
        animationQueue.appendMark(mark);
    }

    public void cancelAnimation() {
        animationQueue.cancelAnimation();
    }
    
    public void setHistory(History h) {
    	this.animationQueue = new AnimationQueue(h);
    }

    public void updateStickerHighlighting( boolean isControlDown )
    {
    	Point mousePos = getMousePosition();
        if(mousePos != null && puzzleManager.updateStickerHighlighting(mousePos.x, mousePos.y, getSlicemask(), isControlDown ))
        	repaint();	
    }
    
    public void updateStickerHighlighting( InputEvent e )
    {
    	updateStickerHighlighting( e.isControlDown() );
    }
    
    public MC4DView(PuzzleManager gg, RotationHandler rotations, History hist) {
    	this.puzzleManager = gg;
        this.rotationHandler = rotations;
        this.setHistory(hist);
        this.setFocusable(true);
        
        // manage slicemask as user holds and releases number keys
        this.addKeyListener(new KeyAdapter() {
            @Override
			public void keyPressed(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9) {
                    slicemask |= 1<<numkey-1; // turn on the specified bit
                    updateStickerHighlighting( arg0 );
                }

                if( arg0.getKeyCode() == KeyEvent.VK_CONTROL ) {
                	updateStickerHighlighting( arg0 );
                }
            }
            @Override
			public void keyReleased(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9) {
                    slicemask &= ~(1<<numkey-1); // turn off the specified bit
                    updateStickerHighlighting( arg0 );
                }
                
                if( arg0.getKeyCode() == KeyEvent.VK_CONTROL ) {
                	updateStickerHighlighting( arg0 );
                }
            }
        });
        this.addMouseListener(new MouseAdapter() {
        	private boolean wasInMotionWhenPressed = true;
        	
            // look for and initiate twist and rotation animations
            @Override
			public void mouseClicked(MouseEvent e) {
            	MC4DView.this.requestFocusInWindow(); // to start receiving key events.
            	
            	if( !puzzleManager.canMouseClick() )
            		return;
            	
            	boolean isViewRotation = e.isControlDown() || SwingUtilities.isMiddleMouseButton(e);
                if( isViewRotation )
            	{
                	// Pass it off to the puzzle manager.
                	if( puzzleManager != null )
		            {
		                puzzleManager.mouseClickedAction(e,
                           rotationHandler,
                           PropertyManager.getFloat("twistfactor", 1),
                           getSlicemask(),
                           MC4DView.this);
		            }
                    return;
            	}

                // Pick our grip.
                int grip = PipelineUtils.pickGrip(
                        e.getX(), e.getY(),
                        puzzleManager.untwistedFrame,
                        puzzleManager.puzzleDescription);

                // The twist might be illegal.
                if( grip < 0 ) 
                {
                    System.out.println("missed");
                }
                else
                {
                	MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
                    clicked.id_within_puzzle = grip; // slamming new id. do we need to set the other members?
                    clicked.face = puzzleManager.puzzleDescription.getGrip2Face()[grip];
                    //System.out.println("face: " + clicked.face);
                    
                    // Tell listeners about the legal twist and let them call animate() if desired.
                    int dir = (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;
                    //if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                    //    dir *= 2;
                    if( ! wasInMotionWhenPressed)
	                    fireStickerClickedEvent(e, new MagicCube.TwistData( clicked, dir, getSlicemask()));
                    repaint();
                }
            }
            // watch for dragging starts and stops
            @Override
			public void mousePressed(MouseEvent arg0) {
            	wasInMotionWhenPressed = isInMotion();
                rotationHandler.stopSpinning();
                lastDrag = arg0.getPoint();
                lastDragTime = arg0.getWhen();
                FPSTimer.stop();
            }
            @Override
			public void mouseReleased(MouseEvent arg0) {
                long timedelta = arg0.getWhen() - lastDragTime;
                lastDrag = null;
                if(timedelta > 0) {
                	rotationHandler.stopSpinning(); // stop any spin if last point wasn't in motion
                    repaint();
                }
            }
            @Override
			public void mouseEntered(MouseEvent e) {
            	requestFocusInWindow(); // So we can get ctrl and other key events before the user clicks.
				super.mouseEntered(e);
			}
        });
        // watch for dragging gestures to rotate the 3D view
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
			public void mouseDragged(MouseEvent me) {
                if(lastDrag == null)
                    return;
                float[]
                    end = new float[] { me.getX(), me.getY() },
                    drag_dir = new float[2];
                Vec_h._VMV2(drag_dir, new float[] { lastDrag.x, lastDrag.y }, end);
                drag_dir[1] *= -1;      // in Windows, Y is down, so invert it
                
                rotationHandler.mouseDragged( drag_dir[0], drag_dir[1],
                		SwingUtilities.isLeftMouseButton(me), SwingUtilities.isMiddleMouseButton(me), me.isShiftDown() );
                frames = 0;
                if(debugging)
                	FPSTimer.restart();
                
                lastDrag = me.getPoint();
                lastDragTime = me.getWhen();
                puzzleManager.updateStickerHighlighting(me.getX(), me.getY(), getSlicemask(), me.isControlDown());
                repaint();
            }
            @Override
			public void mouseMoved(MouseEvent me) {
                super.mouseMoved(me);
                if (puzzleManager != null )
                {
                    if(puzzleManager.updateStickerHighlighting(me.getX(), me.getY(), getSlicemask(), me.isControlDown()));
                    	repaint();
                    return;
                }
            }
        });
        
        // Listen for changes to factors that affect puzzle/view scaling
        //
        puzzleManager.addPuzzleListener(new PuzzleManager.PuzzleListener() {
			@Override
			public void puzzleChanged( boolean newPuzzle ) {
				if( newPuzzle )
					rotationHandler.set4dView(MagicCube.NICE_VIEW);
				updateViewFactors(); // affects puzzle size
			}
		});
        PropertyManager.top.addPropertyListener(new PropertyManager.PropertyListener() {
			@Override
			public void propertyChanged(String property, String newval) {
				updateViewFactors(); // properties listed below affect puzzle size
			}
        }, new String[] {"eyew", "faceshrink", "stickershrink"});
        this.addComponentListener(new ComponentAdapter() {
        	public void componentResized(ComponentEvent e) { // affects xOff & yOff
        		updateViewFactors();            
            }
        });
    } // end MC4DView
    
   
    private void updateViewFactors() {
    	int 
	        W = getWidth(),
	        H = getHeight(),
        minpix = Math.min(W, H);
	    if(minpix == 0)
	        return;
        xOff = ((W>H) ? (W-H)/2 : 0) + minpix/2;
        yOff = ((H>W) ? (H-W)/2 : 0) + minpix/2;
	    
    	// Generate view-independent vertices for the current puzzle in its original 4D orientation, centered at the origin.
        final boolean do3DStepsOnly = true;
    	PipelineUtils.AnimFrame frame = puzzleManager.computeFrame(
            	PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK),
            	PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK),
    			this.rotationHandler,
                PropertyManager.getFloat("eyew", MagicCube.EYEW),
                MagicCube.EYEZ,
                1, // get coords in model coords
                0, 0, // No offset so that verts are centered.
                MagicCube.SUNVEC,
                false, // Don't let shadow polygons muck up the calculation.
                do3DStepsOnly,
                null);
    	
    	float radius3d = -1;
    	int stickerInds[][][] = puzzleManager.puzzleDescription.getStickerInds();
    	for(int i=0; i<frame.drawListSize; i++) {
    		int item[] = frame.drawList[i];
            int iSticker = item[0];
            int iPolyWithinSticker = item[1];
            int poly[] = stickerInds[iSticker][iPolyWithinSticker];
            for(int j=0; j<poly.length; j++) {
            	int vertIndex = poly[j];
        		float dist = Vec_h._NORMSQRD3(frame.verts[vertIndex]);
        		radius3d = Math.max(dist, radius3d);    	
            }
    	}
    	radius3d = (float)Math.sqrt( radius3d );
    	//System.out.println("visible radius: " + radius3d);
    	
    	// This is what corrects the view scale for changes in puzzle and puzzle geometry.
    	// To remove this correction, just set polys2pixelSF = minpix.
        polys2pixelsSF = minpix/(1.25f*radius3d);
        
        repaint(); // Needed when a puzzle is read via Ctrl-O.
    } // end updateViewFactors
    

    /**
     * @param l light vector in screen space.
     * @param n plane normal vector of plane passing through origin.
     * @return matrix that takes points in 3-space to their shadows.
     *
    private static float[][] getShadowMat(float[] l, float[] n) {
        // l /= l dot n
        float[] tmp = new float[l.length];
        Vec_h._VDS3(tmp, l, Vec_h._DOT3(l, n));
        l = tmp;
        return new float[][] {
            { 1-n[0]*l[0],  -n[0]*l[1],  -n[0]*l[2], },
            {  -n[1]*l[0], 1-n[1]*l[1],  -n[1]*l[2], },
            {  -n[2]*l[0],  -n[2]*l[1], 1-n[2]*l[2], },
        };
    }*/

    // Quick & dirty frame timer for debugging.
    //
    private static int frames = 0, FPS =0;
    private static boolean debugging = PropertyManager.getBoolean("debugging", false);
    private static Timer FPSTimer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FPS = frames;
			//System.out.println("FPS = " + FPS);
			frames = 0;
		}
    });
    
    private boolean isInMotion() {
    	return lastDrag != null 
			|| rotationHandler.isSpinning() 
			|| (puzzleManager!=null ? puzzleManager.isAnimating() : isAnimating());
    }

    @Override
	public void paint(Graphics g) {
    	frames++;

		if(animationQueue.isAnimating() && puzzleManager.iTwist == puzzleManager.nTwist) {
			animationQueue.getAnimating();
			// time to stop the animation
			animationQueue.finishedTwist(); // end animation
			repaint();
		}
        if( rotationHandler.continueSpin() && lastDrag == null) { // keep spinning
            repaint();
        }
        
        // antialiasing makes for a beautiful image but can also be expensive to draw therefore
        // we'll turn on antialiasing only when the the user allows it but keep it off when in motion.
        if(g instanceof Graphics2D) {
            boolean okToAntialias = !isInMotion() && PropertyManager.getBoolean("antialiasing", true);
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                okToAntialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        // paint the background
        g.setColor(skyOverride == null ? PropertyManager.getColor("sky.color", MagicCube.SKY) : skyOverride);
        g.fillRect(0, 0, getWidth(), getHeight());
        if(PropertyManager.getBoolean("ground", true)) {
            g.setColor(PropertyManager.getColor("ground.color"));
            g.fillRect(0, getHeight()*6/9, getWidth(), getHeight());
        }   
        
        // paint the puzzle
        if (puzzleManager != null && puzzleManager.puzzleDescription != null)
        {
        	final boolean do3DStepsOnly = false;
        	PipelineUtils.AnimFrame frame = puzzleManager.computeFrame(
                	PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK),
                	PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK),
        			rotationHandler, 
                    PropertyManager.getFloat("eyew", MagicCube.EYEW),
                    MagicCube.EYEZ,
                    polys2pixelsSF * PropertyManager.getFloat("scale", 1),
                    xOff,
                    yOff,
                    MagicCube.SUNVEC,
                    PropertyManager.getBoolean("shadows", true),
                    do3DStepsOnly,
                    this);
        	puzzleManager.paintFrame(g,
        			frame, 
                    PropertyManager.getBoolean("shadows", true), 
                    PropertyManager.getBoolean("ground", true) ? PropertyManager.getColor("ground.color") : null, 
            		PropertyManager.getBoolean("highlightbycubie", false), 
            		PropertyManager.getBoolean("outlines", false) ? PropertyManager.getColor("outlines.color") : null, 
        			PropertyManager.getFloat("twistfactor", 1));
            
            if(FPSTimer.isRunning() && rotationHandler.continueSpin() && lastDrag == null) {
            	StringBuffer sb = new StringBuffer();
            	for(int i=0; i<FPS; i++) sb.append(' ');
            	g.setColor(Color.black);
            	StaticUtils.fillString("FPS: "+FPS+sb, 0, getHeight(), Color.white, g);
            }
        }
    } // end paint


    // wants to be static
    private class AnimationQueue {
        private History queueHist;
        private Vector<Object> queue = new Vector<Object>();
        private QueueItem animating; // non-null == animation in progress
        private class QueueItem {
            public MagicCube.TwistData twist;
            public boolean applyAnimHistWhenDone = true; // whether to change history after animating
            public boolean macroMove = false;
            public QueueItem(MagicCube.TwistData twist, boolean applyAnimHistWhenDone, boolean macroMove) {
                this.twist = twist;
                this.applyAnimHistWhenDone = applyAnimHistWhenDone;
                this.macroMove = macroMove;
            }
        }

        public AnimationQueue(History hist) {
            queueHist = hist;
        }

        public MagicCube.TwistData getAnimating() {
            if(animating != null)
                return animating.twist;
            while( ! queue.isEmpty()) {
                Object item = queue.remove(0);
                if(item instanceof QueueItem) { // this is an animatable item.
                    animating = (QueueItem)item;

                    int iTwistGrip = animating.twist.grip.id_within_puzzle;
                    int iSlicemask = animating.twist.slicemask;
                    int[] orders = puzzleManager.puzzleDescription.getGripSymmetryOrders();
                    if(0 > iTwistGrip || iTwistGrip >= orders.length) {
                    	System.err.println("order indexing error in MC4CView.AnimationQueue.getAnimating()");
                    	continue;
                    }
                    int order = orders[iTwistGrip];
                    
                    if( !PipelineUtils.hasValidTwist( iTwistGrip, iSlicemask, puzzleManager.puzzleDescription ) )
                    	continue;
                    
                    double totalRotationAngle = 2*Math.PI/order;                    
                    boolean quickly = false;
                    if(PropertyManager.getBoolean("quickmoves", false)) // use some form of quick moves
                    	if(PropertyManager.getBoolean("quickmacros", false))
                    		quickly = animating.macroMove;
                    	else
                    		quickly = true;
                    puzzleManager.nTwist = quickly ? 1 : 
                    	puzzleManager.calculateNTwists( totalRotationAngle, PropertyManager.getFloat("twistfactor", 1) );
                    puzzleManager.iTwist = 0;
                    puzzleManager.iTwistGrip = iTwistGrip;
                    puzzleManager.twistDir = animating.twist.direction;
                    puzzleManager.twistSliceMask = animating.twist.slicemask;
                    break; // successfully dequeued a twist which is now animating.
                }
                if(item instanceof Character) // apply the queued mark and continue dequeuing.
                    queueHist.mark(((Character)item).charValue());
            }
            return animating == null ? null : animating.twist;
        } // end getAnimating

        public boolean isAnimating() {
            return animating != null;
        }

        public void append(MagicCube.TwistData twist, boolean applyAnimHistWhenDone, boolean macroMove) {
            queue.add(new QueueItem(twist,applyAnimHistWhenDone, macroMove));
            getAnimating(); // in case queue was empty this sets twist as animating
        }

        public void appendMark(char mark) {
            queue.add(new Character(mark));
        }

        public void finishedTwist() {
            if(animating != null && animating.applyAnimHistWhenDone)
                queueHist.apply(animating.twist);
            animating = null; // the signal that the twist is finished.
            getAnimating(); // queue up the next twist if any.
        }

        public void cancelAnimation() {
            animating = null;
            queue.removeAllElements();
        }
    } // end class AnimationQueue


    /**
     * Simple example program.
     */
    public static void main(String[] args) throws java.io.IOException {
    	final String SCHLAFLI = "{4,3,3}";
        final int LENGTH = 3;
        System.out.println("version " + System.getProperty("java.version"));
        JFrame frame = new StaticUtils.QuickFrame("test");
        final MC4DView view = new MC4DView(new PuzzleManager(SCHLAFLI, LENGTH, new JProgressBar()), new RotationHandler(), new History(LENGTH));
        view.addStickerListener(new MC4DView.StickerListener() {
            public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
                view.animate(twisted, true);
            }
        });
        frame.getContentPane().add(view);
        frame.setVisible(true);
    }
}
