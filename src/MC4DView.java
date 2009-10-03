import java.awt.*;
import java.awt.event.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.Vector;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.Timer;


/**
 * Represents a viewer/controller for the Magic Cube 4D puzzle.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DView extends Component {

    public GenericGlue genericGlue = null; // caller can set this after I'm constructed

    public static interface TwistListener { public void twisted(MagicCube.TwistData twisted); }
    private Vector<TwistListener> twistListeners = new Vector<TwistListener>();
    public void addTwistListener(TwistListener tl) { twistListeners.add(tl); }
    public void removeTwistListener(TwistListener tl) { twistListeners.remove(tl); }
    protected void fireTwistEvent(MagicCube.TwistData twist) {
        for(Enumeration<TwistListener> e=twistListeners.elements(); e.hasMoreElements(); )
            e.nextElement().twisted(twist);
    }

    private PuzzleState state;
    private PolygonManager polymgr;
    private AnimationQueue animationQueue;
    public boolean isAnimating() { return animationQueue.isAnimating(); }
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
    private RotationHandler rotationHandler = new RotationHandler();
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
        rotationHandler.setAllowSpinDrag(val);
        repaint();
    }
    
    public void setSnapMode(RotationHandler.Snap snap) {
        rotationHandler.setSnapSetting(snap);
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

    /*
     * Shamelessly copied from SwingUtilities.java since that is in JDK 1.3 and we'd like to keep this to 1.2 and below.
     */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK);
    }
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
         return ((anEvent.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }

    public MC4DView(PuzzleState state, PolygonManager polymgr, History hist, int nfaces) {
        this.state = state;
        this.polymgr = polymgr;
        this.animationQueue = new AnimationQueue(hist);
        faceRGB = YUV.generateVisuallyDistinctRGBs(nfaces, .7f, .1f); //generateHSVColors(12, 10, .5f);
        this.setFocusable(true);
        
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
            	MC4DView.this.requestFocusInWindow(); // to start receiving key events.
            	
            	boolean isViewRotation = e.isControlDown() || isMiddleMouseButton(e);
                if( isViewRotation )
            	{
                	// Pass it off to the generic glue (for now,
                	// a view rotation helper will be created soon)
                	if( genericGlue != null && genericGlue.isActive() )
		            {
		                genericGlue.mouseClickedAction(e,
		                                               rotationHandler,
		                                               MC4DView.this.polymgr.getTwistFactor(),
		                                               slicemask,
		                                               MC4DView.this);
		            }
                	
                    return;
            	}

                // Pick our grip.
                int grip = GenericPipelineUtils.pickGrip(
                        e.getX(), e.getY(),
                        genericGlue.untwistedFrame,
                        genericGlue.genericPuzzleDescription);
                     
                if(grip < 0) {
                    System.out.println("missed");
                }
                else {
                	MagicCube.Stickerspec clicked = new MagicCube.Stickerspec();
                    clicked.id_within_cube = grip; // slamming new id. do we need to set the other members?
                    clicked.face = genericGlue.genericPuzzleDescription.getGrip2Face()[grip];
                    System.out.println("face: " + clicked.face);

                    // Tell listeners about the legal twist and let them call animate() if desired.
                    int dir = (isLeftMouseButton(e) || isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;
                    //if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                    //    dir *= 2;
                    fireTwistEvent( new MagicCube.TwistData( clicked, dir, slicemask ) );
                    repaint();
                }
            }
            // watch for dragging starts and stops
            public void mousePressed(MouseEvent arg0) {
                lastDrag = arg0.getPoint();
                lastDragTime = arg0.getWhen();
                rotationHandler.stopSpinning();
                FPSTimer.stop();
            }
            public void mouseReleased(MouseEvent arg0) {
                long timedelta = arg0.getWhen() - lastDragTime;
                lastDrag = null;
                if(timedelta > 0) {
                	rotationHandler.stopSpinning(); // stop any spin if last point wasn't in motion
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
                
                rotationHandler.mouseDragged( drag_dir[0], drag_dir[1] );
                frames = 0;
                if(debugging)
                	FPSTimer.restart();
                
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
            }
        });
    } // end MC4DView
    
    
    /*
     * Returns an array of nShades * nColorsPerShade RGB triplets 
     * from the upper satFrac fraction of the saturation range 0..1
     *
    private static float[][] generateHSVColors(int nShades, int nColorsPerShade, float satFrac) {
    	final double G = 1 - (1 + Math.sqrt(5))/2; // golden fraction of 1.0
    	float[][] colors = new float[nColorsPerShade*nShades][3];
    	for(int s=0; s<nShades; s++) {
		    for(int i=0; i<nColorsPerShade; i++) {
			    int cid = s*nColorsPerShade + i;
			    double x = G * cid;
			    double hue = 6 * (x - Math.floor(x));
			    double f = 1./2; // fraction of the range of saturation to use. E.G. 2/3 == .333 -> 1.
			    double sat = nShades < 2 ? 1 : ((double)s)/(nShades-1) * f + (1 - f);
			    hsv2rgb((float) hue, (float)sat, 1, colors[cid]);
		    }
    	}
	    return colors;
    }*/
    
    public static void hsv2rgb(float h, float s, float v, float[] rgb)
	{
		// H is given on [0->6] or -1. S and V are given on [0->1]. 
		// RGB are each returned on [0->1]. 
		float m, n, f;
		int i; 

		float[] hsv = new float[3];

		hsv[0] = h;
		hsv[1] = s;
		hsv[2] = v;
		System.out.println("H: " + h + " S: " + s + " V:" + v);
		if(hsv[0] == -1) {
			rgb[0] = rgb[1] = rgb[2] = hsv[2];
			return;  
		}
		i = (int)(Math.floor(hsv[0]));
		f = hsv[0] - i;
		if(i%2 == 0) f = 1 - f; // if i is even 
		m = hsv[2] * (1 - hsv[1]); 
		n = hsv[2] * (1 - hsv[1] * f); 
		switch (i) { 
			case 6: 
			case 0: rgb[0]=hsv[2]; rgb[1]=n; rgb[2]=m; break;
			case 1: rgb[0]=n; rgb[1]=hsv[2]; rgb[2]=m; break;
			case 2: rgb[0]=m; rgb[1]=hsv[2]; rgb[2]=n; break;
			case 3: rgb[0]=m; rgb[1]=n; rgb[2]=hsv[2]; break;
			case 4: rgb[0]=n; rgb[1]=m; rgb[2]=hsv[2]; break;
			case 5: rgb[0]=hsv[2]; rgb[1]=m; rgb[2]=n; break;
		} 
    }
    
   
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

    
    public static int distSqrd(int a[], int b[]) {
        int sum = 0;
        for(int i=0; i<a.length; i++) {
            int diff = a[i]-b[i];
            sum += diff * diff;
        }
        return sum;
    }       
    

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
    private static boolean debugging = PropertyManager.getBoolean("debugging", true);
    private static Timer FPSTimer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FPS = frames;
			//System.out.println("FPS = " + FPS);
			frames = 0;
		}
    });

    public void paint(Graphics g) {
    	frames++;
        updateViewFactors();

		if(animationQueue.isAnimating() && genericGlue.iTwist == genericGlue.nTwist) {
			MagicCube.TwistData animating = animationQueue.getAnimating();
			// time to apply the puzzle state change and stop the animation
			int mask = animating.slicemask==0?1:animating.slicemask;
			state.twist(animating.grip, animating.direction, mask);
			animationQueue.finishedTwist(); // end animation
			repaint();
		}
        if( rotationHandler.continueSpin() && lastDrag == null) { // keep spinning
            repaint();
        }
        
        // antialiasing makes for a beautiful image but can also be expensive to draw therefore
        // we'll turn on antialiasing only when the the user allows it but keep it off when in motion.
        if(g instanceof Graphics2D) {
            boolean okToAntialias = allowAntiAliasing && lastDrag==null && rotationHandler.getSpinDelta()==null
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
                rotationHandler,
                polymgr.getEyeW(),
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

                    int iTwistGrip = animating.twist.grip.id_within_cube;
                    int order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[iTwistGrip];
                    double totalRotationAngle = 2*Math.PI/order;                    
                    
                    genericGlue.nTwist = genericGlue.calculateNTwists( totalRotationAngle, polymgr.getTwistFactor() );
                    genericGlue.iTwist = 0;
                    genericGlue.iTwistGrip = iTwistGrip;
                    genericGlue.twistDir = animating.twist.direction;
                    genericGlue.twistSliceMask = animating.twist.slicemask;
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
        final MC4DView view = new MC4DView(new PuzzleState(length, polymgr), polymgr, hist, 6);
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
}
