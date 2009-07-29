import java.awt.*;
import java.awt.event.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.Vector;
import java.util.Enumeration;

//
// NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
// AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Represents a viewer/controller for the Magic Cube 4D puzzle.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
public class MC4DView extends DoubleBufferedCanvas {

    public GenericGlue genericGlue = null; // caller can set this after I'm constructed
    public float viewMat4d[][] = {{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}}; // XXX new member, similar to viewrot-- put this somewhere

    public static interface TwistListener { public void twisted(MagicCube.TwistData twisted); }
    private Vector twistListeners = new Vector();
    public void addTwistListener(TwistListener tl) { twistListeners.add(tl); }
    public void removeTwistListener(TwistListener tl) { twistListeners.remove(tl); }
    protected void fireTwistEvent(MagicCube.TwistData twist) {
        for(Enumeration e=twistListeners.elements(); e.hasMoreElements(); )
            ((TwistListener)e.nextElement()).twisted(twist);
    }

    private PuzzleState state;
    private PolygonManager polymgr;
    private final MagicCube.Frame
        untwisted_frame = new MagicCube.Frame(),
        shadow_frame = new MagicCube.Frame(),
        twisting_frame = new MagicCube.Frame(); // scratch var used during animation
    private AnimationQueue animationQueue;
    public boolean isAnimating() { return animationQueue.isAnimating(); }
    private MagicCube.Stickerspec stickerUnderMouse; // null if none or animating/spinning
    private int cubieCenter[] = new int[MagicCube.NDIMS]; // center of cubie containing above
    private int slicemask; // bitmap representing which number keys are down
    private float faceRGB[][];
    private boolean showShadows=true;
    private Color outlineColor;
    private Color
        bg     = MagicCube.BACKGROUND,
        ground = MagicCube.GROUND;
    private int xOff, yOff;
    private float pixels2polySF = .01f; // screen transform data
    private Point lastDrag; // non-null == dragging
    private long lastDragTime; // timestamp of last drag event
    private SQuat viewrot = new SQuat(); // total quaternion rotation of puzzle in view.
    private SQuat spindelta; // rotation to add for each frame while spinning. null == stopped
    private boolean allowSpinDrag = true; // whether to allow spin dragging at all
    private boolean allowAntiAliasing = true; // whether to allow antialiasing at all
    private float scale = 1;
    private boolean highlightByCubie = false;
    private boolean quickMoves = false; // whether to skip animations and perform moves in a single frame
    public void setHighlightByCubie(boolean val) { highlightByCubie = val; }
    
    public void setScale(float scale) {
        this.scale = scale;
        updateViewFactors();
        repaint();
    }

    public void setTwistFactor(float twistfactor) {
        polymgr.setTwistFactor(twistfactor);
        repaint();
    }
    
    public void setBackground(Color bg) {
        if(bg == null)
            return;
        this.bg = bg;
        repaint();
    }
    public void setGround(Color ground) {
        this.ground = ground;
        repaint();
    }
    
    public void setOutlined(Color outlines) {
        this.outlineColor = outlines;
        repaint();
    }
    
    public void setFaceColor(int face, float r, float g, float b) {
        faceRGB[face][0] = r;
        faceRGB[face][1] = g;
        faceRGB[face][2] = b;
        repaint();
    }
    public void setFaceColor(int face, Color color) {
        float rgb[] = new float[3];
        color.getRGBColorComponents(rgb);
        setFaceColor(face, rgb[0], rgb[1], rgb[2]);
    }

    public void setShowShadows(boolean val) {
        showShadows = val;
        repaint();
    }

    public void setQuickMoves(boolean val) {
        quickMoves = val;
    }

    public void allowSpinDrag(boolean val) {
        allowSpinDrag = val;
        repaint();
    }

    public void allowAntiAliasing(boolean val) {
        allowAntiAliasing = val;
        repaint();
    }
    
    public void animate(MagicCube.TwistData move, boolean applyToHist) {
        animationQueue.append(move, applyToHist);
        repaint();
    }

    public void cancelAnimation() {
        animationQueue.cancelAnimation();
    }

    public void animate(MagicCube.TwistData moves[], boolean applyToHist) {
        for(int i=0; i<moves.length; i++)
            animate(moves[i], applyToHist);
    }

    public void append(char mark) {
        animationQueue.appendMark(mark);
    }

    /*
     * Shamelessly copied from SwingUtilities.java since that is in JDK 1.3 and we'd like to keep this to 1.2 and below.
     */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK);
    }
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
         return ((anEvent.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }

    public MC4DView(PuzzleState state, PolygonManager polymgr, History hist) {
        this.state = state;
        this.polymgr = polymgr;
        this.animationQueue = new AnimationQueue(hist);
        faceRGB = new float[MagicCube.NFACES][3];
        for(int f=0; f<MagicCube.NFACES; f++)
            System.arraycopy(MagicCube.FACE_COLORS[f], 0, faceRGB[f], 0, 3);
        polymgr.getUntwistedFrame(untwisted_frame, getViewMat(), MagicCube.SUNVEC, true); // probably unneeded initialization
        // manage slicemask as user holds and releases number keys
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9)
                    slicemask |= 1<<numkey-1; // turn on the specified bit
            }
            public void keyReleased(KeyEvent arg0) {
                int numkey = arg0.getKeyCode() - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9)
                    slicemask &= ~(1<<numkey-1); // turn off the specified bit
            }
        });
        this.addMouseListener(new MouseAdapter() {
            // look for and initiate twist and rotation animations
            public void mouseClicked(MouseEvent e) {
                if (genericGlue != null && genericGlue.isActive())
                {
                    genericGlue.mouseClickedAction(e,
                                                   viewMat4d,
                                                   MC4DView.this.polymgr.getTwistFactor(),
                                                   slicemask,
                                                   MC4DView.this);
                    return;
                }

                MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
                boolean hit = MC4DView.this.polymgr.pickGrip(
                        (e.getX()-xOff)*pixels2polySF,
                        (e.getY()-yOff)*pixels2polySF,
                        untwisted_frame, clicked);
                if(hit) {
                    boolean isRotate = e.isControlDown() || isMiddleMouseButton(e);
                    if(isRotate) {
                        if( ! PolygonManager.facetocenterToGrip(clicked.face, clicked)) {
                            System.err.println("Can't rotate that.\n");
                            return;
                        }
                    }
                    else if(clicked.id_within_face == 13) {
                        System.err.println("Can't twist that.\n");
                        return;
                    }
                    // Tell listeners about the legal twist and let them call animate() if desired.
                    int dir = (isLeftMouseButton(e) || isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;
                    //if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                    //    dir *= 2;
                    fireTwistEvent(new MagicCube.TwistData(clicked, dir, isRotate ? -1 : slicemask));
                    repaint();
                }
                else {
                    System.out.println("missed");
                }
            }
            // watch for dragging starts and stops
            public void mousePressed(MouseEvent arg0) {
                lastDrag = arg0.getPoint();
                lastDragTime = arg0.getWhen();
                spindelta = null; // always stop any spinning
            }
            public void mouseReleased(MouseEvent arg0) {
                long timedelta = arg0.getWhen() - lastDragTime;
                lastDrag = null;
                if(timedelta > 0) {
                    spindelta = null; // stop any spin if last point wasn't in motion
                    repaint();
                }
            }
        });
        // watch for dragging gestures to rotate the 3D view
        this.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent arg0) {
                if(lastDrag == null)
                    return;
                float[]
                    end = new float[] { arg0.getX(), arg0.getY() },
                    drag_dir = new float[2];
                Vec_h._VMV2(drag_dir, new float[] { lastDrag.x, lastDrag.y }, end);
                drag_dir[1] *= -1;      // in Windows, Y is down, so invert it
                float[] axis = new float[3];
                Vec_h._XV2(axis, drag_dir);
                float pixelsMoved = (float)Math.sqrt(Vec_h._NORMSQRD2(axis));
                if (pixelsMoved > .0001) { // do nothing if ended where we started
                    Vec_h._VDS2(axis, axis, pixelsMoved);
                    axis[2] = 0;
                    float rads = pixelsMoved / 300f;
                    spindelta = new SQuat(axis, rads);
                    viewrot.setMult(spindelta);
                    if(pixelsMoved < 2)
                        spindelta = null; // drag distance not large enough to trigger autorotation
                }
                lastDrag = arg0.getPoint();
                lastDragTime = arg0.getWhen();
                repaint();
            }
            public void mouseMoved(MouseEvent arg0) {
                super.mouseMoved(arg0);
                if (genericGlue != null && genericGlue.isActive())
                {
                    genericGlue.mouseMovedAction(arg0, MC4DView.this);
                    return;
                }
                MagicCube.Stickerspec sticker = new MagicCube.Stickerspec();
                int hitQuad = MC4DView.this.polymgr.pick(
                    (arg0.getX()-xOff)*pixels2polySF,
                    (arg0.getY()-yOff)*pixels2polySF,
                    untwisted_frame, sticker);
                if(hitQuad >= 0) {
                    if(stickerUnderMouse == null || stickerUnderMouse.id_within_cube != sticker.id_within_cube)
                        repaint(); // something new to highlight
                    stickerUnderMouse = sticker;
                    //System.out.println(sticker.id_within_face + " on face " + sticker.face);
                    int len = MC4DView.this.polymgr.getLength();
                    for(int d=0; d<MagicCube.NDIMS; d++)
                        cubieCenter[d] = clamp(sticker.coords[d], -(len-1), len-1);
                }
                else { // missed
                    if(stickerUnderMouse != null)
                        repaint(); // now nothing highlighted
                    stickerUnderMouse = null;
                }
            }
        });
    } // end MC4DView
    
   
    private void updateViewFactors() {
        int 
            W = getWidth(),
            H = getHeight(),
            min = W>H ? H : W;
        if(W*H == 0)
            return;
        pixels2polySF = 1f / Math.min(W, H) / scale;
        xOff = ((W>H) ? (W-H)/2 : 0) + min/2;
        yOff = ((H>W) ? (H-W)/2 : 0) + min/2;
    }
    
    private void paintFrame(MagicCube.Frame frame, boolean isShadows, Graphics g) {
        int
            xs[] = new int[4],
            ys[] = new int[4];
        Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
        for (int q = 0; q < frame.nquads; q++) {
            for (int i = 0; i < 4; i++) {
                int qi = frame.quads[q][i];
                xs[i] = (int)(xOff + frame.verts[qi][0]/pixels2polySF + .5);
                ys[i] = (int)(yOff + frame.verts[qi][1]/pixels2polySF + .5);
                //System.out.println(xs[i] + ", " + ys[i]);
            }
            int sid = frame.quadids[q]/6;
            int cs = state.idToColor(sid);
            //System.out.println(cs);
            float b = frame.brightnesses[q];
            Color stickercolor = new Color(
                b*faceRGB[cs][0],
                b*faceRGB[cs][1],
                b*faceRGB[cs][2]);
            boolean highlight = stickerUnderMouse != null && (highlightByCubie ? partOfCubie(sid) : stickerUnderMouse.id_within_cube == sid);
            if(highlight)
                stickercolor = stickercolor.brighter().brighter();
            g.setColor(isShadows ? shadowcolor : stickercolor);
            g.fillPolygon(xs, ys, 4);
            if(!isShadows && outlineColor != null) {
                g.setColor(outlineColor);
                // uncomment the following line for an alternate outlining idea -MG
                // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                g.drawPolygon(xs, ys, 4);
            }
        }        
    }
    
    public static int distSqrd(int a[], int b[]) {
        int sum = 0;
        for(int i=0; i<a.length; i++) {
            int diff = a[i]-b[i];
            sum += diff * diff;
        }
        return sum;
    }       
    

    private MagicCube.Stickerspec tmpsticker = new MagicCube.Stickerspec();
    
    private boolean partOfCubie(int sid) {
        tmpsticker.id_within_cube = sid;
        polymgr.fillStickerspecFromId(tmpsticker);
        return distSqrd(tmpsticker.coords, cubieCenter) == 1;
    }
    
    private float[][] getViewMat() {
        if( ! allowSpinDrag)
            return new float[][] {
                {1,0,0,},
                {0,1,0,},
                {0,0,1,},
            };
        return new SQuat.Matrix3(viewrot).asArray();
    }

    /**
     * @param l light vector in screen space.
     * @param n plane normal vector of plane passing through origin.
     * @return matric that takes points in 3-space to their shadows.
     */
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
    }

    private static void shift(MagicCube.Frame frame, float[] off) {
        float[] negoff = {off[0], -off[1], off[2]};
        for(int i=0; i<frame.nverts; i++)
            for(int j=0; j<2; j++)
                frame.verts[i][j] += negoff[j];
    }

    public void paint(Graphics g1) {
        updateViewFactors();
        polymgr.getUntwistedFrame(untwisted_frame, getViewMat(), MagicCube.SUNVEC, true);

        float[][] shadowview = new float[3][3];
        float[] off = new float[3];
        if(showShadows) {
            float[] planenormal = new float[] {0,1,.1f};
            float planeoffset = -.5f;
            float[] planeoffsetvector = new float[3];
            Vec_h._VXS3(planeoffsetvector, planenormal, planeoffset); // shifting *lower* on screen
            float[][] viewmat = getViewMat(), shadowmat = getShadowMat(new float[] { .82f, 1.55f, 3.3f }, planenormal);
            Vec_h._MXM3(shadowview, viewmat, shadowmat);
            // calculate off = -planeoffsetvector * shadowmat + planeoffsetvector
            Vec_h._VXM3(off, planeoffsetvector, shadowmat);
            Vec_h._VMV3(off, planeoffsetvector, off);
            polymgr.getUntwistedFrame(shadow_frame, shadowview, MagicCube.SUNVEC, false);
            shift(shadow_frame, off);
        }

        MagicCube.Frame frame = untwisted_frame;
if (!(genericGlue != null && genericGlue.isActive())) // indented funnily for right now so I don't affect the diffs from previous version -Don
        if(animationQueue.isAnimating()) {
            MagicCube.TwistData animating = animationQueue.getAnimating();
            if(!quickMoves && animating.animStep++ < polymgr.getTwistNFrames(animating)-1) { // keep twisting
                frame = twisting_frame; // calculate an in-between frame
                polymgr.getFrame(animating.grip, animating.direction,
                    animating.slicemask==0?1:animating.slicemask,
                    animating.animStep, polymgr.getTwistNFrames(animating),
                    getViewMat(), MagicCube.SUNVEC, true, frame);
                if(showShadows)
                    polymgr.getFrame(animating.grip, animating.direction,
                        animating.slicemask==0?1:animating.slicemask,
                        animating.animStep, polymgr.getTwistNFrames(animating),
                        shadowview, MagicCube.SUNVEC, false, shadow_frame);
                        shift(shadow_frame, off);
            }
            else { // time to apply the puzzle state change and stop the animation
                int mask = animating.slicemask==0?1:animating.slicemask;
                state.twist(animating.grip, animating.direction, mask);
                animationQueue.finishedTwist(); // end animation
            }
            repaint();
            //try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        if(allowSpinDrag && spindelta != null && lastDrag == null) { // keep spinning
            viewrot.setMult(spindelta);
            repaint();
        }
        Graphics g = super.startPaint(g1); // begin painting into the back buffer

        // antialiasing makes for a beautiful image but can also be expensive to draw
        // therefore we'll turn on antialiasing when the image is still and turn it off when in motion.
        // i hope that refering to Graphics2D won't cause a class-not-found exception in any applets.
        // may need to remove this or otherwise work around that problem if so.
        if(g instanceof Graphics2D) {
            boolean okToAntialias = allowAntiAliasing && lastDrag==null && spindelta==null
                                 && !(genericGlue!=null && genericGlue.isActive() ? genericGlue.isAnimating()
                                                                                  : isAnimating());
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                okToAntialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        // paint the background
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
        if(ground != null) {
            g.setColor(ground);
            g.fillRect(0, getHeight()*6/9, getWidth(), getHeight());
        }
        // paint the puzzle
        if (genericGlue != null && genericGlue.isActive())
        {
            genericGlue.computeAndPaintFrame(
              // used by compute part...
                polymgr.getFaceShrink(),
                polymgr.getStickerShrink(),
                viewMat4d, // contents of this get incremented if rotating!
                polymgr.getEyeW(),
                this.getViewMat(),
                MagicCube.EYEZ,
                scale,
                pixels2polySF,
                xOff,
                yOff,
                MagicCube.SUNVEC,

              // used by compute and paint part...
                showShadows,

              // used by paint part only...
                ground,
                faceRGB,
                highlightByCubie,
                outlineColor,
                g,
                polymgr.getTwistFactor(),
                this);
        }
        else
        {
            if(showShadows)
                paintFrame(shadow_frame, true, g);
            paintFrame(frame, false, g);
        }
        super.endPaint(); // blits the back buffer to the front
    } // end paint


    private static class AnimationQueue {
        private History queueHist;
        private Vector queue = new Vector();
        private QueueItem animating; // non-null == animation in progress
        private static class QueueItem {
            public MagicCube.TwistData twist;
            public boolean applyAnimHistWhenDone = true; // whether to change history after animating
            public QueueItem(MagicCube.TwistData twist, boolean applyAnimHistWhenDone) {
                this.twist = twist;
                this.applyAnimHistWhenDone = applyAnimHistWhenDone;
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
                if(item instanceof QueueItem) {
                    animating = (QueueItem)item;
                    break;
                }
                if(item instanceof Character)
                    queueHist.mark(((Character)item).charValue());
            }
            return animating == null ? null : animating.twist;
        }

        public boolean isAnimating() {
            return animating != null;
        }

        public void append(MagicCube.TwistData twist, boolean applyAnimHistWhenDone) {
            queue.add(new QueueItem(twist,applyAnimHistWhenDone));
            getAnimating(); // in case queue was empty this sets twist as animating
        }

        public void appendMark(char mark) {
            queue.add(new Character(mark));
        }

        public void finishedTwist() {
            if(animating != null && animating.applyAnimHistWhenDone)
                queueHist.apply(animating.twist);
            animating = null;
            getAnimating(); // queues up the next twist if any.
        }

        public void cancelAnimation() {
            animating = null;
            queue.removeAllElements();
        }
    } // end class AnimationQueue


    // Make it so we get keyboard focus on startup,
    // without having to click first.  Thanks, Melinda!
    // The state of things seems to be:
    //      - Buttons and TextFields are apparently "focus traversable"
    //        by default.  Canvases and Applets aren't,
    //        implying (not obvious) that you need to click to type
    //        when the app starts,
    //        which is almost never the desired behavior.
    //        You can change this by overriding isFocusTraversable(),
    //        which we do below.
    //      - This method is deprecated in favor of isFocusable, though
    //        better would be simply using setFocusable(boolean). I'm
    //        not doing that however because isFocusable is a 1.4 concept
    //        and we want the Applet code to be backwards compatible with
    //        1.2 if possible.
    //      - Other approaches using requestFocus():
    //        A Canvas calling requestFocus() doesn't seem to do
    //        anything.  An Applet calling requestFocus() doesn't
    //        do anything during init(), but I think it works
    //        if you put it in the mouseEntered handler or something,
    //        though this can lead to various unpredictabilities.
    //
    public boolean isFocusTraversable()
    {
        return true;
    }

    /**
     * Simple example program.
     */
    public static void main(String[] args) throws java.io.IOException {
        int length = 3;
        System.out.println("version " + System.getProperty("java.version"));
        Frame frame = new Frame("test");
        PolygonManager polymgr = new PolygonManager(length);
        final History hist = new History(length);
        final String logfilename = "mc4d.log";
        final java.io.File logfile = new java.io.File(logfilename);
        if(logfile.exists())
            hist.read(new PushbackReader(new FileReader(logfile)));
//        java.net.URL histurl = Util.getResource(logfile);
//        if(histurl == null)
//            System.out.println("couldn't read history file");
//        else
//            hist.read(new java.io.StringReader(Util.readFileFromURL(histurl)));
        final MC4DView view = new MC4DView(new PuzzleState(length, polymgr), polymgr, hist);
        view.addTwistListener(new MC4DView.TwistListener() {
            public void twisted(MagicCube.TwistData twisted) {
                view.animate(twisted, true);
            }
        });
        frame.add(view);
        frame.setBounds(100, 100, 650, 650);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                //hist.compress();
                System.out.println("writing " + hist.countTwists() + " twist" + (hist.countTwists()==1 ? "" : "s"));
                try {
                    java.io.Writer writer = new java.io.FileWriter(new java.io.File(logfilename));
                    hist.write(writer);
                    writer.close();
                } 
                catch (IOException e) { e.printStackTrace(); }
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
    
    private static int clamp(int x, int a, int b)
    {
        return x <= a ? a :
               x >= b ? b : x;
    }
}
