package com.superliminal.util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * 
 * @author Melinda Green & Don Hatch
 */
@SuppressWarnings("serial")
public class FloatSlider extends JSlider {
    private double curFloat, minFloat, maxFloat;
    private boolean isLogScale;

    @Override
    protected void fireStateChanged() {
        int ival = FloatSlider.super.getValue(), min = FloatSlider.super.getMinimum(), max = FloatSlider.super.getMaximum();
        double dval = transformRange(false, min, max, ival,
            isLogScale, minFloat, maxFloat);
        // It's important to finish setting the float value before users get notified 
        // about the change otherwise they'll get the old value when they ask.
        setFloatValue(dval);
        super.fireStateChanged();
    }

    /**
     * constructs a FloatSlider using a given number of slider positions.
     * 
     * @param orientation - SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
     * @param cur - real valued initial value.
     * @param min - real valued range minimum.
     * @param max - real valued range maximum.
     * @param log - log scale if true, linear otherwise.
     */
    public FloatSlider(int orientation, double cur, double min, double max, boolean log) {
        super(orientation);
        super.getModel().setRangeProperties(1, 1, 0, 100, false);
        super.setExtent(0);
        minFloat = min;
        maxFloat = max;
        curFloat = cur;
        isLogScale = log;
        setFloatValue(cur);
    }
    /**
     * uses default scale (linear).
     */
    public FloatSlider(int orientation, double cur, double min, double max) {
        this(orientation, cur, min, max, false);
    }
    /**
     * returns the closest integer in the range of the actual int extents of the base Scrollbar.
     */
    protected int rangeValue(double dval) {
        dval = clamp(dval, minFloat, maxFloat);
        int min = super.getMinimum(), max = super.getMaximum();
        //System.out.println("setting: dval="+dval+" -> ival="+ival);
        return (int) Math.round(transformRange(isLogScale, minFloat, maxFloat, dval, false, min, max));
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
        super.setValue(rangeValue(newcur));
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
        return a + (b - a) * t;
    }
    // geometric interpolation
    private static double gerp(double a, double b, double t)
    {
        return a * Math.pow(b / a, t);
    }
    // interpolate between A and B (linearly or geometrically)
    // by the fraction that x is between a and b (linearly or geometrically)
    private static double transformRange(boolean isLog, double a, double b, double x, boolean IsLog, double A, double B) {
        if(isLog)
        {
            a = Math.log(a);
            b = Math.log(b);
            x = Math.log(x);
        }
        double t = (x - a) / (b - a);
        return IsLog ? gerp(A, B, t) : lerp(A, B, t);
    }

    /**
     * simple test program for FloatSlider class.
     */
    public static void main(String args[]) {
        Frame frame = new Frame("FloatSlider example");
        final FloatSlider rslider = new FloatSlider(SwingConstants.HORIZONTAL, 10f, 1f, 100f, true);
        final Label curValue = new Label("FloatSlider value: " + rslider.getFloatValue());
        rslider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                curValue.setText("FloatSlider value: " + rslider.getFloatValue());
                curValue.repaint();
                System.out.println("user got: " + rslider.getFloatValue() + " ival: " + rslider.getValue());
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
