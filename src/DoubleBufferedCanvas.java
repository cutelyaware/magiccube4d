
import java.awt.*;

//
//NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
//AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Implements double-buffering for flicker-free animation using AWT graphics.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class DoubleBufferedCanvas extends Canvas { 
    private Image mActiveOffscreenImage = null;
    private Dimension mOffscreenSize = new Dimension(-1,-1);
    private Graphics mActiveOffscreenGraphics = null;
    private Graphics mSystemGraphics = null;
    private Image backgroundImage = null;
    private Graphics backgroundGraphics = null;
    
    public Graphics getBackgroundGraphics() {
        if (backgroundGraphics == null) {
            backgroundImage = createImage(mOffscreenSize.width, mOffscreenSize.height);
            backgroundGraphics = backgroundImage.getGraphics();
        }
        clearBackground();
        return backgroundGraphics;
    }
    public Image getBackgroundImage() {
        return backgroundImage;
    }
    private void clearBackground() {
        backgroundGraphics.setColor(Color.white);
        backgroundGraphics.fillRect(0, 0, mOffscreenSize.width, mOffscreenSize.height);
        backgroundGraphics.setColor(Color.black);
    }

    
//    DoubleBufferedCanvas() {
//        this.addComponentListener(new ComponentAdapter() {
//            public void componentResized(ComponentEvent e) {
//                repaint();
//            }
//        });
//    }
    
    /**      
     * NOTE: when extending applets:
     * this overrides update() to *not* erase the background before painting
     */
    public void update(Graphics g) {
        paint(g);
    }
    
    /**
     * Begins painting into an offscreen image image.
     * @param sysgraph is the Graphics object given to a caller's paint method.
     * @return a Graphics object initialized to draw into an offscreen image.
     */
    public Graphics startPaint (Graphics sysgraph) {
        mSystemGraphics = sysgraph;
        // Initialize if this is the first pass or the size has changed
        Dimension d = new Dimension(getBounds().width, getBounds().height);
        if ((mActiveOffscreenImage == null) ||
            (d.width != mOffscreenSize.width) ||
            (d.height != mOffscreenSize.height))
        {
            mActiveOffscreenImage = createImage(d.width, d.height);
            mActiveOffscreenGraphics = mActiveOffscreenImage.getGraphics();
            mOffscreenSize = d;
            mActiveOffscreenGraphics.setFont(getFont());
            //backgroundImage = null;
            //backgroundGraphics = null;
        }
        //mActiveOffscreenGraphics.clearRect(0, 0, mOffscreenSize.width, mOffscreenSize.height);
        return mActiveOffscreenGraphics;
    }
    
    /**
     * Gives up the graphics object previously gotten from startPaint
     * and paints the entire rendered offscreen image to the screen.
     */
    public void endPaint () {
        // Start copying the offscreen image to this canvas
        // The application will begin drawing into the other one while this happens
        mSystemGraphics.drawImage(mActiveOffscreenImage, 0, 0, null);
    }
} // end class DoubleBufferedCanvas

