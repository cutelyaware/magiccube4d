import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;


@SuppressWarnings("serial")
public class Congratulations extends JFrame {
	final static int FPS = 15, DUR = 2;
	private JPanel body;
	private JLabel text;
	private Container contentpane;
	private Timer timer = new Timer(1000/FPS, new ActionListener() {
		private int n = 0;
		@Override
		public void actionPerformed(ActionEvent ae) {
			if(n < FPS*DUR) { // still flashing
				contentpane.setBackground(n++ %2 == 0 ? Color.yellow : Color.white);
				body.repaint();
			}
			else {
				timer.stop(); // This is important to allow garbage collection of this JFrame.
				text.setVisible(true);
				body.add(new JLabel("Thanks to http://oldtiffinianscc.co.uk/ for the trophy image."));
				contentpane.setBackground(Color.white);
				body.repaint();				
			}
		}
	});
	
	public Congratulations(String content) {
		super("Congratulations!");
		//this.setUndecorated(true);
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
		getContentPane().add(body);
		pack();            
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
            Math.max(0,screenSize.width/2  - getWidth()/2),
            Math.max(0,screenSize.height/2 - getHeight()/2));
		text.setVisible(false);
	}
	
	public void start() {
		Audio.play(Audio.Sound.FANFARE);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Congratulations.this.setVisible(false);
				super.mouseClicked(e);
			}
		});
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
					"<html>" + 
		            	"<center><H1>You may already be a wiener!" +
		                "<br><br><p><center>Click this window to close.</center></p>" +
	                "</html>");
				congrats.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				congrats.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						super.mouseClicked(e);
						System.exit(0);
					}
				});
				congrats.setVisible(true);
				congrats.start();
			}
			
		});
	}
}
