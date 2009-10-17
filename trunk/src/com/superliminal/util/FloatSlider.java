package com.superliminal.util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JScrollBar;

/**
 * Floating point version of JSlider.
 * Replaces the original integer get/set Minimum/Maximum/Value methods with the
 * floating point versions: get/set FloatMinimum/FloatMaximum/FloatValue.
 * It is an error to call the original versions or other methods related to them.
 * Note that unlike in the base class, values set via setFloatValue(val) are allowed
 * to be outside the min/max range values. In those cases the slider thumb will clamp
 * to the appropriate slider end (just like in the base class) but the out-of-range
 * value will still be retrievable with getFloatValue().
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green & Don Hatch
 */
@SuppressWarnings("serial")
public class FloatSlider extends JScrollBar {
    private final static int DEFAULT_RANGE = 1000; // number of discrete steps
    private final static int DEFAULT_VISIBLE = 20; // pixel width of thumb
    private double curFloat, minFloat, maxFloat;
    private boolean isLogScale;

    /**
     * constructs a FloatSlider using a given number of slider positions.
     * @param orientation - Scrollbar.VERTICAL or Scrollbar.HORIZONTAL.
     * @param cur - real valued initial value.
     * @param vis - same as in Scrollbar base class.
     * @param min - real valued range minimum.
     * @param max - real valued range maximum.
     * @param log - log scale if true, linear otherwise.
     */
    public FloatSlider(int orientation, double cur, int vis, double min, double max, int res, boolean log) {
        super(orientation, 0, vis, 0, res+vis);
        isLogScale = log;
        setAll(min, max, cur);
        addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                int 
                    ival = FloatSlider.super.getValue(),
                    vis = FloatSlider.super.getVisibleAmount(),
                    min = FloatSlider.super.getMinimum(),
                    max = FloatSlider.super.getMaximum();
                double dval = transformRange(false,      min,      max-vis,  ival,
                                             isLogScale, minFloat, maxFloat);
                //System.out.println("getting: ival="+ival+" -> dval="+dval);
                setFloatValue(dval);     
            }
        });
    }
    /**
     * uses default scale (linear).
     */
    public FloatSlider(int orientation, double cur, int vis, double min, double max, int res) {
        this(orientation, cur, vis, min, max, res, false);
    }
    /**
     * uses default visible(20) and resolution(1000).
     */
    public FloatSlider(int orientation, double cur, double min, double max, boolean log) {
        this(orientation, cur, DEFAULT_VISIBLE, min, max, DEFAULT_RANGE, log);
    }
    /**
     * uses default visible(20), resolution(1000), and scale (linear).
     */
    public FloatSlider(int orientation, double cur, double min, double max) {
        this(orientation, cur, DEFAULT_VISIBLE, min, max, DEFAULT_RANGE, false);
    }

    /**
     * returns the closest integer in the range of the actual int extents of the base Scrollbar.
     */
    protected int rangeValue(double dval) {
        dval = clamp(dval, minFloat, maxFloat);
        int 
            vis = super.getVisibleAmount(),
            min = super.getMinimum(),
            max = super.getMaximum();
        //System.out.println("setting: dval="+dval+" -> ival="+ival);
        return (int)Math.round(transformRange(isLogScale, minFloat, maxFloat, dval, false, min, max-vis));
    }

    public double getFloatMinimum() {
        return minFloat;
    }
    public double getFloatMaximum() {
        return maxFloat;
    }
    public double getFloatValue() {
        return curFloat;
    }

    public void setFloatMinimum(double newmin) {
        setAll(newmin, maxFloat, getFloatValue());
    }
    public void setFloatMaximum(double newmax) {
        setAll(minFloat, newmax, getFloatValue());
    }
    public void setFloatValue(double newcur) {
        // update the model
        curFloat = newcur;
        // update the view
        super.setValues(rangeValue(newcur),
                        super.getVisibleAmount(),
                        super.getMinimum(),
                        super.getMaximum());
    }

    public void setAll(double newmin, double newmax, double newcur) {
        minFloat = newmin;
        maxFloat = newmax;
        setFloatValue(newcur);
    }

    private static double clamp(double x, double a, double b)
    {
        return x <= a ? a :
               x >= b ? b : x;
    }
    // linear interpolation
    private static double lerp(double a, double b, double t)
    {
        return a + (b-a) * t;
    }
    // geometric interpolation
    private static double gerp(double a, double b, double t)
    {
        return a * Math.pow(b/a, t);
    }
    // interpolate between A and B (linearly or geometrically)
    // by the fraction that x is between a and b (linearly or geometrically)
    private static double transformRange(boolean isLog, double a, double b, double x, boolean IsLog, double A, double B) {
        if (isLog)
        {
            a = Math.log(a);
            b = Math.log(b);
            x = Math.log(x);
        } 
        double t = (x-a) / (b-a);
        return IsLog ? gerp(A,B,t) : lerp(A,B,t);
    }

    /**
     * simple test program for FloatSlider class.
     */
    public static void main(String args[]) {
        Frame frame = new Frame("FloatSlider example");
        final FloatSlider rslider = new FloatSlider(Scrollbar.HORIZONTAL, 10f, 1f, 100f, true);
        final Label curValue = new Label("FloatSlider value: " + rslider.getFloatValue());
        rslider.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                curValue.setText("FloatSlider value: " + rslider.getFloatValue());
            }
        });
        Container mainpanel = new Panel();
        mainpanel.setLayout(new GridLayout(3, 1));
        mainpanel.add(new Label("Range: " + rslider.getFloatMinimum() + " -> " + rslider.getFloatMaximum()));
        mainpanel.add(rslider);
        mainpanel.add(curValue);
        frame.add(mainpanel);
        frame.setSize(new Dimension(800, 100));
        frame.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent we) {
                System.exit(1);
            }
        });
        frame.setVisible(true); 
    }

}
