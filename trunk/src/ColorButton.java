import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;

@SuppressWarnings("serial")
class ColorizedButton extends JButton {
	private Color color;

	public ColorizedButton(Color c) {
		color = c;
	}

	@Override
	public void paintComponent(Graphics g) {
		// paint original background
		super.paintComponent(g);
		// colorize complete button
		g.setColor(color);
		g.fillRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
	}
}

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
    private final static int ALPHA = 128;
	private Color color;
    private String prefKey;
    private JColorChooser tcc = new JColorChooser();
    private Color transparent(Color c) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), ALPHA); }
    
    public ColorButton(String label, final String prefKey, final Color def, final ColorChangeListener changer, final boolean continuous) {
        super(label);
        this.prefKey = prefKey;
        setColor(PropertyManager.getColor(prefKey, def));
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
        color = transparent(newColor);
        //setContrastingForeground();
        PropertyManager.userprefs.setProperty(prefKey, newColor.getRed()+"," + newColor.getGreen()+"," + newColor.getBlue());        
    }
    private void setContrastingForeground() {
        setForeground((color.getRed() + color.getGreen() + color.getBlue()) / 3 >= 128 ? Color.BLACK : Color.WHITE);
    }
    
	@Override
	public void paintComponent(Graphics g) {
		// paint original background
		super.paintComponent(g);
		// colorize complete button
		g.setColor(color);
		g.fillRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
	}
    
    public static void main(String args[]) {
        JFrame frame = new StaticUtils.QuickFrame("ColorButton Test");
        frame.getContentPane().add(new ColorButton("test", "testcolor", new Color(255, 0, 0, 64), new ColorChangeListener() {
            public void colorChanged(Color newColor) {
                System.out.println("new color " + newColor);
            }
        }, true));
        frame.setVisible(true);
    }
 
}
