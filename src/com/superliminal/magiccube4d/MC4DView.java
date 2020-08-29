package com.superliminal.magiccube4d;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

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
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DView extends Component {

    // Sticker click listener support
    public static interface StickerListener {
        public void stickerClicked(InputEvent e, MagicCube.TwistData twisted);
    }
    private Set<StickerListener> stickerListeners = new HashSet<StickerListener>();
    public void addStickerListener(StickerListener tl) {
        stickerListeners.add(tl);
    }
    public void removeStickerListener(StickerListener tl) {
        stickerListeners.remove(tl);
    }
    protected void fireStickerClickedEvent(InputEvent event, MagicCube.TwistData twist) {
        for(StickerListener sl : stickerListeners)
            sl.stickerClicked(event, twist);
    }

    /**
     * Callback used to notify completion of queued twist animations and pure callback items.
     */
    public interface ItemCompleteCallback {
        /**
         * @param twist the move associated with the callback if any, null if none.
         */
        public void onItemComplete(MagicCube.TwistData twist);
    }

    private PuzzleManager puzzleManager = null;
    private AnimationQueue animationQueue;
    public boolean isAnimating() {
        return animationQueue.isAnimating();
    }
    private int slicemask; // bitmap representing which number keys are down
    public int getSlicemask() {
        return slicemask == 0 ? 1 : slicemask;
    }
    private Color skyOverride = null; // null means use the user's preference from the PropertyManager.
    private int xOff, yOff;
    private float polys2pixelsSF = .01f; // screen transform data
    private Point lastDrag; // non-null == dragging
    private long lastDragTime; // timestamp of last drag event
    private RotationHandler rotationHandler;

    /**
     * Overrides the user's preference when not null. Set to null to revert.
     */
    public void setSkyOverride(Color so) {
        this.skyOverride = so;
        repaint();
    }

    /**
     * Appends a move and calls optional callback when finished.
     * 
     * @param move A single twist or rotation to be animated as soon as possible.
     * @param icc Optional callback to be notified of twist completion.
     * @param macroMove Whether this move is part of a sequence subject to quick moves.
     */
    public void animate(MagicCube.TwistData move, ItemCompleteCallback icc, boolean macroMove) {
        animationQueue.append(move, icc, macroMove);
        repaint();
    }
    // Helper functions that call the above.
    public void animate(MagicCube.TwistData move, ItemCompleteCallback icc) {
        animate(move, icc, false);
    }
    public void animate(MagicCube.TwistData move) {
        animate(move, null, false);
    }
    public void animate(MagicCube.TwistData moves[], ItemCompleteCallback icc, boolean macroMove) {
        for(MagicCube.TwistData move : moves)
            animate(move, icc, macroMove);
    }

    /**
     * Appends a callback to be called when when any previous item is finished, or immediately if none.
     */
    public void append(ItemCompleteCallback icc) {
        animationQueue.appendCallback(icc);
        repaint();
    }

    public void cancelAnimation() {
        animationQueue.cancelAnimation();
    }

    public void updateStickerHighlighting(boolean isControlDown) {
        Point mousePos = getMousePosition();
        if(mousePos != null && puzzleManager.updateStickerHighlighting(mousePos.x, mousePos.y, getSlicemask(), isControlDown))
            repaint();
    }
    public void updateStickerHighlighting(InputEvent e) {
        updateStickerHighlighting(e.isControlDown());
    }


    public MC4DView(PuzzleManager gg, RotationHandler rotations) {
        this.puzzleManager = gg;
        this.rotationHandler = rotations;
        this.animationQueue = new AnimationQueue(puzzleManager);
        this.setFocusable(true);
        // manage slicemask as user holds and releases number keys
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9) {
                    slicemask |= 1 << numkey - 1; // turn on the specified bit
                    updateStickerHighlighting(arg0);
                }
                if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
                    updateStickerHighlighting(arg0);
                }
            }
            @Override
            public void keyReleased(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9) {
                    slicemask &= ~(1 << numkey - 1); // turn off the specified bit
                    updateStickerHighlighting(arg0);
                }
                if(arg0.getKeyCode() == KeyEvent.VK_CONTROL) {
                    updateStickerHighlighting(arg0);
                }
            }
        });
        // Pick up mouse clicks, drags, etc.
        this.addMouseListener(new MouseAdapter() {
            private boolean wasInMotionWhenPressed = true;
            // look for and initiate twist and rotation animations
            @Override
            public void mouseClicked(MouseEvent e) {
                MC4DView.this.requestFocusInWindow(); // to start receiving key events.
                if(!puzzleManager.canMouseClick())
                    return;
                boolean isViewRotation = e.isControlDown() || SwingUtilities.isMiddleMouseButton(e);
                if(isViewRotation) {
                    // Pass it off to the puzzle manager.
                    if(puzzleManager != null) {
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
                if(grip < 0) {
                    System.out.println("missed");
                }
                else {
                    MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
                    clicked.id_within_puzzle = grip; // slamming new id. do we need to set the other members?
                    clicked.face = puzzleManager.puzzleDescription.getGrip2Face()[grip];
                    // Tell listeners about the legal twist and let them call animate() if desired.
                    int dir = (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;
                    //if(e.isShiftDown()) // Experimental control to allow double twists.
                    //    dir *= 2;
                    if(!wasInMotionWhenPressed)
                        fireStickerClickedEvent(e, new MagicCube.TwistData(clicked, dir, getSlicemask()));
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
                puzzleManager.clearStickerHighlighting();
                FPSTimer.stop();
            }
            @Override
            public void mouseReleased(MouseEvent me) {
                long timedelta = me.getWhen() - lastDragTime;
                lastDrag = null;
                if(timedelta > 0) {
                    rotationHandler.stopSpinning(); // stop any spin if last point wasn't in motion
                    repaint();
                }
                if(isInMotion())
                    puzzleManager.clearStickerHighlighting();
                else
                    puzzleManager.updateStickerHighlighting(me.getX(), me.getY(), getSlicemask(), me.isControlDown());
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                requestFocusInWindow(); // So we can get ctrl and other key events before the user clicks.
                super.mouseEntered(e);
            }
        }); // end MouseListener
        // watch for dragging gestures to rotate the 3D view
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent me) {
                if(lastDrag == null)
                    return;
                float[]
                end = new float[]{me.getX(), me.getY()},
                drag_dir = new float[2];
                Vec_h._VMV2(drag_dir, new float[]{lastDrag.x, lastDrag.y}, end);
                drag_dir[1] *= -1; // in Windows, Y is down, so invert it
                rotationHandler.mouseDragged(drag_dir[0], drag_dir[1],
                    SwingUtilities.isLeftMouseButton(me), SwingUtilities.isMiddleMouseButton(me), me.isShiftDown());
                frames = 0;
                if(PropertyManager.getBoolean(MagicCube.DEBUGGING, false))
                    FPSTimer.restart();
                lastDrag = me.getPoint();
                lastDragTime = me.getWhen();
                repaint();
            }
            @Override
            public void mouseMoved(MouseEvent me) {
                super.mouseMoved(me);
                if(puzzleManager != null) {
                    if(!isInMotion() && puzzleManager.updateStickerHighlighting(me.getX(), me.getY(), getSlicemask(), me.isControlDown()))
                        repaint();
                }
            }
        });
        // Listen for changes to factors that affect puzzle/view scaling
        //
        puzzleManager.addPuzzleListener(new PuzzleManager.PuzzleListener() {
            @Override
            public void puzzleChanged(boolean newPuzzle) {
                if(newPuzzle)
                    rotationHandler.set4dView(MagicCube.NICE_VIEW);
                updateViewFactors(); // affects puzzle size
            }
        });
        PropertyManager.top.addPropertyListener(new PropertyManager.PropertyListener() {
            @Override
            public void propertyChanged(String property, String newval) {
                updateViewFactors(); // properties listed below affect puzzle size
            }
        }, new String[]{"eyew", "faceshrink", "stickershrink"});
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { // affects xOff & yOff
                updateViewFactors();
            }
        });
    } // end MC4DView()


    private void updateViewFactors() {
        int W = getWidth(), H = getHeight(), minpix = Math.min(W, H);
        if(minpix == 0)
            return;
        xOff = ((W > H) ? (W - H) / 2 : 0) + minpix / 2;
        yOff = ((H > W) ? (H - W) / 2 : 0) + minpix / 2;
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
        for(int i = 0; i < frame.drawListSize; i++) {
            int item[] = frame.drawList[i];
            int iSticker = item[0];
            int iPolyWithinSticker = item[1];
            int poly[] = stickerInds[iSticker][iPolyWithinSticker];
            for(int vertIndex : poly) {
                float dist = Vec_h._NORMSQRD3(frame.verts[vertIndex]);
                radius3d = Math.max(dist, radius3d);
            }
        }
        radius3d = (float) Math.sqrt(radius3d);
        // This is what corrects the view scale for changes in puzzle and puzzle geometry.
        // To remove this correction, just set polys2pixelSF = minpix.
        polys2pixelsSF = minpix / (1.25f * radius3d);
        repaint(); // Needed when a puzzle is read via Ctrl-O.
    } // end updateViewFactors()


    /**
     * @param l light vector in screen space.
     * @param n plane normal vector of plane passing through origin.
     * @return matrix that takes points in 3-space to their shadows.
     * 
     *         private static float[][] getShadowMat(float[] l, float[] n) {
     *         // l /= l dot n
     *         float[] tmp = new float[l.length];
     *         Vec_h._VDS3(tmp, l, Vec_h._DOT3(l, n));
     *         l = tmp;
     *         return new float[][] {
     *         { 1-n[0]*l[0], -n[0]*l[1], -n[0]*l[2], },
     *         { -n[1]*l[0], 1-n[1]*l[1], -n[1]*l[2], },
     *         { -n[2]*l[0], -n[2]*l[1], 1-n[2]*l[2], },
     *         };
     *         }
     */

    // Quick & dirty frame timer for debugging.
    //
    private static int frames = 0, FPS = 0;
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
            || (puzzleManager != null ? puzzleManager.isAnimating() : isAnimating());
    }

    private int numPaints = 0;

    @Override
    public void paint(Graphics g) {
        frames++;
        if(animationQueue.isAnimating() && puzzleManager.iTwist == puzzleManager.nTwist) {
            // time to stop the animation
            animationQueue.finishedTwist(); // end animation
            repaint();
        }
        if(lastDrag == null && rotationHandler.continueSpin()) { // keep spinning
            repaint();
        }
        // antialiasing makes for a beautiful image but can also be expensive to draw therefore
        // we'll turn on antialiasing only when the the user allows it but keep it off when in motion.
        if(g instanceof Graphics2D) {
            boolean okToAntialias = !isInMotion() && PropertyManager.getBoolean("antialiasing", true);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                okToAntialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            // Voodoo to remove 1/2 pixel lower-right bias so that all four modes match up:
            // [antialiased,non-antialiased] x [fill,outlines].  Note that this works only
            // when rendering directly to a visible Component (not a BufferedImage).
            // For details, see Issue #138 and
            // https://stackoverflow.com/questions/7701097/java-graphics-fillpolygon-how-to-also-render-right-and-bottom-edges/63645061#answer-63645061 .
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        // paint the background
        g.setColor(skyOverride == null ? PropertyManager.getColor("sky.color", MagicCube.SKY) : skyOverride);
        g.fillRect(0, 0, getWidth(), getHeight());
        if(PropertyManager.getBoolean("ground", true)) {
            g.setColor(PropertyManager.getColor("ground.color"));
            g.fillRect(0, getHeight() * 6 / 9, getWidth(), getHeight());
        }
        // paint the puzzle
        if(puzzleManager != null && puzzleManager.puzzleDescription != null) {
            final boolean do3DStepsOnly = false;
            PipelineUtils.AnimFrame frame = puzzleManager.computeFrame(
                PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK),
                PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK),
                rotationHandler,
                PropertyManager.getFloat("eyew", MagicCube.EYEW),
                MagicCube.EYEZ * PropertyManager.getFloat("scale", 1),
                polys2pixelsSF,
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
                for(int i = 0; i < FPS; i++)
                    sb.append(' ');
                g.setColor(Color.black);
                StaticUtils.fillString(" FPS: " + FPS + sb, 0, getHeight(), Color.white, g);
            }
        }
        ++numPaints; // before looking at it
        if(PropertyManager.getBoolean(MagicCube.DEBUGGING, false)) {
            // Show e.g. "(123 paints)" in upper right of the picture.
            g.setColor(Color.black);
            String text = "(" + numPaints + " paint" + (numPaints == 1 ? "" : "s") + ")";
            FontMetrics fontMetrics = g.getFontMetrics();
            g.drawString(text,
                /* x= */getWidth() - 2 - fontMetrics.stringWidth(text),
                /* y= */g.getFontMetrics().getAscent());
        }
    } // end paint()


    /**
     * Maintains a queue of "animatable" items consisting of twists and callbacks.
     * Twists represent puzzle twists as well as pure rotations, collectively called "moves".
     * Moves can contain optional callbacks to be called when they complete their animation.
     * Callback items are called immediately upon being dequeued.
     * 
     * The getAnimating() method returns the currently animating move if any and dequeues
     * the next one if any. Queued items do not get automatically dequeued as others finish.
     * This method is required to get them going and should be called frequently.
     * 
     * The provided PuzzleManager is used to compute frame polygons. It's puzzle state is
     * never altered (IE treated as const) though it's highlighting properties are set
     * depending upon the state of the mouse buttons and keyboard keys.
     * 
     * The cancelAnimation() method cancels any current animation and clears the queue.
     */
    private static class AnimationQueue {
        private Vector<Object> queue = new Vector<Object>();
        private QueueItem animating; // non-null == animation in progress
        private PuzzleManager puzzleMgr;

        private class QueueItem {
            public MagicCube.TwistData twist;
            public ItemCompleteCallback icc;
            public boolean macroMove = false;
            public QueueItem(MagicCube.TwistData twist, ItemCompleteCallback icc, boolean macroMove) {
                this.twist = twist;
                this.icc = icc;
                this.macroMove = macroMove;
            }
        }

        public AnimationQueue(PuzzleManager pm) {
            puzzleMgr = pm;
        }

        /**
         * Returns the currently animating twist. If no twist is currently animating, it pulls new items off the animation queue.
         * Dequeued twists begin animating. Dequeued callback items are called immediately.
         * It is generally OK to call this method at any time. It can have the side effect of
         * 
         * @return The currently animating twist if any. Otherwise begins animating the next item if any and returns that. Otherwise null.
         */
        public MagicCube.TwistData getAnimating() {
            while(animating == null && !queue.isEmpty()) {
                // No twists are currently animating and there are items queued up,
                // so begin processing them until one of those things is no longer true.
                Object item = queue.remove(0);
                if(item instanceof QueueItem) { // this is an animatable item.
                    animating = (QueueItem) item;
                    int iTwistGrip = animating.twist.grip.id_within_puzzle;
                    int iSlicemask = animating.twist.slicemask;
                    int[] orders = puzzleMgr.puzzleDescription.getGripSymmetryOrders();
                    if(0 > iTwistGrip || iTwistGrip >= orders.length) {
                        System.err.println("order indexing error in MC4CView.AnimationQueue.getAnimating()");
                        continue;
                    }
                    int order = orders[iTwistGrip];
                    if(!PipelineUtils.hasValidTwist(iTwistGrip, iSlicemask, puzzleMgr.puzzleDescription))
                        continue;
                    double totalRotationAngle = 2 * Math.PI / order;
                    boolean quickly = false;
                    if(PropertyManager.getBoolean("quickmoves", false)) // use some form of quick moves
                        if(PropertyManager.getBoolean("quickmacros", false))
                            quickly = animating.macroMove;
                        else
                            quickly = true;
                    puzzleMgr.nTwist = quickly ? 1 :
                        puzzleMgr.calculateNTwists(totalRotationAngle, PropertyManager.getFloat("twistfactor", 1));
                    puzzleMgr.iTwist = 0;
                    puzzleMgr.iTwistGrip = iTwistGrip;
                    puzzleMgr.twistDir = animating.twist.direction;
                    puzzleMgr.twistSliceMask = animating.twist.slicemask;
                    break; // successfully dequeued a twist which is now animating.
                }
                if(item instanceof ItemCompleteCallback) { // Call the supplied callback and continue dequeuing.
                    ((ItemCompleteCallback) item).onItemComplete(null);
                }
            }
            return animating == null ? null : animating.twist;
        } // end getAnimating

        public boolean isAnimating() {
            return animating != null;
        }

        public void append(MagicCube.TwistData twist, ItemCompleteCallback icc, boolean macroMove) {
            queue.add(new QueueItem(twist, icc, macroMove));
            getAnimating(); // in case queue was empty this sets twist as animating
        }

        public void appendCallback(ItemCompleteCallback icc) {
            queue.add(icc);
            getAnimating(); // in case queue was empty this will call the callback.
        }

        public void finishedTwist() {
            QueueItem finished = animating;
            animating = null; // the signal that the twist is finished.
            getAnimating(); // Queue up the next item if any before calling callback which may queue up more.
            if(finished != null && finished.icc != null)
                finished.icc.onItemComplete(finished.twist);
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
        final int[] num_twists = new int[1];
        System.out.println("version " + System.getProperty("java.version"));
        JFrame frame = new StaticUtils.QuickFrame("test");
        final MC4DView view = new MC4DView(new PuzzleManager(SCHLAFLI, LENGTH, new JProgressBar()), new RotationHandler());
        view.addStickerListener(new MC4DView.StickerListener() {
            @Override
            public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
                view.animate(twisted, new ItemCompleteCallback() {
                    @Override
                    public void onItemComplete(MagicCube.TwistData twist) {
                        System.out.println("Num Twists: " + ++num_twists[0]);
                    }
                });
            }
        });
        frame.getContentPane().add(view);
        frame.setVisible(true);
    }
}
