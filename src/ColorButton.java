import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;

/**
 * A JButton with a background color controlled by a pop-up JColorChooser.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class ColorButton extends JButton {
    public interface ColorChangeListener {
        public void colorChanged(Color newColor);
    }
    private String prefKey;
    private JColorChooser tcc = new JColorChooser();
    
    public ColorButton(String label, final String prefKey, final Color def, final ColorChangeListener changer, final boolean continuous) {
        super(label);
        this.prefKey = prefKey;
        setBackground(PropertyManager.getColor(prefKey, def));
        setContrastingForeground();
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                final Color oldColor = PropertyManager.getColor(prefKey, def);
                tcc.setColor(oldColor);
                JColorChooser.createDialog(ColorButton.this, "Select Color", true, tcc, 
                   new ActionListener() {
                       public void actionPerformed(ActionEvent arg0) {
                           //System.out.println("ok");
                           if( ! continuous)
                               changer.colorChanged(ColorButton.this.getBackground());
                       }
                   },
                   new ActionListener() {
                       public void actionPerformed(ActionEvent arg0) {
                           //System.out.println("cancel");
                           setColor(oldColor);
                           if(continuous)
                               changer.colorChanged(oldColor);
                       }
                   }
               ).setVisible(true);
           }
        });
        
        tcc.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Color newColor = tcc.getColor();
                //System.out.println("state changed " + newColor);
                setColor(newColor);
                if(continuous)
                    changer.colorChanged(newColor);
            }
        });        
    }
    private void setColor(Color newColor) {
        if(newColor == null)
            return;
        setBackground(newColor);
        setContrastingForeground();
        PropertyManager.userprefs.setProperty(prefKey, newColor.getRed()+"," + newColor.getGreen()+"," + newColor.getBlue());        
    }
    private void setContrastingForeground() {
        Color bg = getBackground();
        setForeground((bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 >= 128 ? Color.BLACK : Color.WHITE);
    }
    
    public static void main(String args[]) {
        JFrame frame = new StaticUtils.QuickFrame("ColorButton Test");
        frame.getContentPane().add(new ColorButton("test", "testcolor", Color.RED, new ColorChangeListener() {
            public void colorChanged(Color newColor) {
                System.out.println("new color " + newColor);
            }
        }, true));
        frame.setVisible(true);
    }
 
}
