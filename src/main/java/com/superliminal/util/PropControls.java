package com.superliminal.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Subclasses of common Swing controls that synchronize their values with named properties.
 * 
 * Copyright 2016 - Superliminal Software
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class PropControls {
    // to disallow instantiation
    private PropControls() {
    }


    /**
     * A FloatSlider that synchronizes it's value with a given property.
     */
    public static class PropSlider extends FloatSlider {
        public PropSlider(final String propname, final Component dependent, double dflt, double min, double max, String tooltip) {
            super(SwingConstants.HORIZONTAL, PropertyManager.getFloat(propname, (float) dflt), min, max);
            setToolTipText(tooltip);
            setPreferredSize(new Dimension(200, 20));
            // Sync the property to the control
            addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    PropertyManager.userprefs.setProperty(propname, "" + (float) getFloatValue());
                    dependent.repaint();
                }
            });
            // Sync the control to the property
            PropertyManager.top.addPropertyListener(
                new PropertyManager.PropertyListener() {
                    @Override
                    public void propertyChanged(String property, String newval) {
                        String oldval = "" + PropSlider.this.getFloatValue();
                        if(oldval.equals(newval))
                            return; // no change
                        PropSlider.this.setFloatValue(Double.parseDouble(newval));
                    }
                },
                propname);
        }
    }

    /**
     * A JCheckBox that synchronizes it's value with a given property.
     */
    public static class PropCheckBox extends JCheckBox {
        public PropCheckBox(String title, final String propname, boolean dflt, final Component dependent, String tooltip) {
            super(title, PropertyManager.getBoolean(propname, dflt));
            setToolTipText(tooltip);
            // Sync the property to the control
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    PropertyManager.userprefs.setProperty(propname, "" + isSelected());
                    dependent.repaint();
                }
            });
            // Sync the control to the property
            PropertyManager.top.addPropertyListener(
                new PropertyManager.PropertyListener() {
                    @Override
                    public void propertyChanged(String property, String newval) {
                        if(("" + PropCheckBox.this.isSelected()).equals(newval))
                            return; // No change
                        PropCheckBox.this.setSelected("true".equals(newval));
                    }
                },
                propname);
        }
    }

    /**
     * A JRadioButton that synchronizes it's value with a given property.
     */
    public static class PropRadioButton extends JRadioButton {
        public PropRadioButton(String title, final String propname, boolean dflt, final boolean invert, final Component dependent, String tooltip) {
            super(title);
            setToolTipText(tooltip);
            boolean on = PropertyManager.getBoolean(propname, dflt);
            if(invert)
                on = !on;
            setSelected(on);
            // Sync the property to the control
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean is_on = isSelected();
                    if(invert)
                        is_on = !is_on;
                    PropertyManager.userprefs.setProperty(propname, "" + is_on);
                    dependent.repaint();
                }
            });

            // Sync the control to the property - TODO: make this work
//            PropertyManager.top.addPropertyListener(
//                new PropertyManager.PropertyListener() {
//                    @Override
//                    public void propertyChanged(String property, String newval) {
//                        boolean is_on = isSelected();
//                        if(invert)
//                            is_on = !is_on;
//                        if(("" + is_on).equals(newval))
//                            return; // No change
//                        PropRadioButton.this.setSelected("true".equals(newval));
//                    }
//                },
//                propname);
        }
    }

}
