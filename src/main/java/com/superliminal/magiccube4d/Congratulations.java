package com.superliminal.magiccube4d;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;


/**
 * A big congratulatory pop-up that plays a fanfare while showing a trophy
 * and flashing animation for a while.
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class Congratulations extends JFrame {
    final static int FPS = 15, DUR = 2;
    private JPanel body;
    private JLabel text;
    private Container contentpane;
    private Timer timer = new Timer(1000 / FPS, new ActionListener() {
        private int n = 0;
        @Override
        public void actionPerformed(ActionEvent ae) {
            if(n < FPS * DUR) { // still flashing
                contentpane.setBackground(n++ % 2 == 0 ? Color.yellow : Color.white);
                body.repaint();
            }
            else {
                timer.stop(); // This is important to allow garbage collection of this JFrame.
                text.setVisible(true);
                contentpane.setBackground(Color.white);
                body.repaint();
            }
        }
    });

    public Congratulations(String content) {
        super("Congratulations!");
        contentpane = getContentPane();
        body = new JPanel();
        body.setOpaque(false);
        body.setBackground(Color.white);
        body.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(getImageIcon("congratulations.png"));
        icon.setAlignmentX(.5f);
        body.add(icon);
        JPanel tmp = new JPanel();
        text = new JLabel(content);
        text.setOpaque(false);
        tmp.add(text);
        tmp.setOpaque(false);
        body.add(tmp);
        JButton close = new JButton("Yea!");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Congratulations.this.setVisible(false);
                Congratulations.this.dispose();
            }
        });
        close.setAlignmentX(.5f);
        body.add(close);
        getContentPane().add(body);
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
            Math.max(0, screenSize.width / 2 - getWidth() / 2),
            Math.max(0, screenSize.height / 2 - getHeight() / 2));
        text.setVisible(false);
    }

    public void start() {
        Audio.play(Audio.Sound.FANFARE);
        timer.start();
    }

    private static ImageIcon getImageIcon(String path) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        return new ImageIcon(cl.getResource(path));
    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Congratulations congrats = new Congratulations(
                    "<html><center>" +
                        "<H1>You may already be a winner!</H1>" +
                        "<br><br><p>Click button to close.</p>" +
                        "</center></html>");
                congrats.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                congrats.setVisible(true);
                congrats.start();
            }

        });
    }
}
