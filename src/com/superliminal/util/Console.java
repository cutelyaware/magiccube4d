package com.superliminal.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.superliminal.magiccube4d.MC4DView;
import com.superliminal.magiccube4d.MC4DView.ItemCompleteCallback;
import com.superliminal.magiccube4d.MagicCube;
import com.superliminal.magiccube4d.PuzzleManager;
import com.superliminal.magiccube4d.RotationHandler;

import com.superliminal.util.PropControls.PropCheckBox;
import com.superliminal.util.PropControls.PropRadioButton;

/**
 * Manages a singleton JFrame console. When visible, stdout and stderr streams are redirect to it.
 * When hidden, the original streams are restored.
 * 
 * @see http://stackoverflow.com/a/9776819/181535
 * 
 * @author Melinda Green
 */
public class Console {
    private final static JFrame mInstance = new JFrame("Console");
    private final static JTextArea mTextArea = new JTextArea(100, 0);
    private final static PrintStream mTextStream = new PrintStream(new TextAreaOutputStream(mTextArea));
    private final static PrintStream mOriginalOut = System.out;
    private final static PrintStream mOriginalErr = System.err;
    private static Component dependent = null; // Call its repaint method as needed.
    // a component that simply forwards all repaint calls to the current dependent object.
    private static Component mRepainter = new Component() {
        @Override
        public void repaint() {
            if(dependent != null)
                dependent.repaint();
        }
    };

    private static void setEnabledWhenPropertyIsTrue(final Component c, final String propname, final boolean dflt) {
        PropertyManager.top.addPropertyListener(new PropertyManager.PropertyListener() {
            @Override
            public void propertyChanged(String property, String newval) {
                c.setEnabled(PropertyManager.getBoolean(propname, dflt));
            }
        }, propname);
        c.setEnabled(PropertyManager.getBoolean(propname, dflt));
    }

    private static Component multiHorizontalSplitPane(final Component[] children, final int startIndex) {
      if (startIndex == children.length-1) return children[startIndex];
      return new JSplitPane(
          /*orientation= */JSplitPane.HORIZONTAL_SPLIT,
          /*continuoutLayout= */true,
          /*leftComponent= */children[startIndex],
          /*rightComponent= */multiHorizontalSplitPane(children, startIndex+1)) {{
          setResizeWeight(1./(children.length-startIndex));
          setOneTouchExpandable(true);
      }};
    }

    private static class JRow extends JPanel {{ setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); }};
    private static class JCol extends JPanel {{ setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
        public void addLeftJustified(final int indentPixels, final Component c) {
            add(new JRow() {{
                if (indentPixels != 0) add(Box.createHorizontalStrut(indentPixels));
                add(c);
                add(Box.createHorizontalGlue());
            }});
        }
    };
    private static class ExpertControlPanel extends JCol {
        public ExpertControlPanel() {
            addLeftJustified(0, new JCol() {{  // extra level is necessary to prevent spreading out of controls when stretched vertically
                setBorder(new TitledBorder("EXPERIMENTAL NEW CONTROLS"));
                // TODO: investigate why all the views are getting repainted before the controls get enabled/disabled!  that's not friendly!
                addLeftJustified(0, new PropCheckBox("Allow Antialiasing", "antialiasing", true, mRepainter, "Whether to smooth polygon edges when still - Warning: Can be expensive on large puzzles"));
                final JRadioButton whenstill = new PropRadioButton("when still", "antialiasingmeansalways", /*dflt=*/false, /*invert=*/true, /*dependent=*/null, "Antialias only when no animation is in progress - Warning: Can interfere with interaction on large puzzles");
                final JRadioButton always = new PropRadioButton("always (NOT HOOKED UP YET: AWAITING CODE REVIEW)", "antialiasingmeansalways", /*dflt=*/false, /*invert=*/false, /*dependent=*/null, "Antialias every frame - Warning: can be very slow on large puzzles");
                setEnabledWhenPropertyIsTrue(whenstill, "antialiasing", true);
                setEnabledWhenPropertyIsTrue(always, "antialiasing", true);
                new ButtonGroup() {{
                    add(whenstill);
                    add(always);
                }};
                addLeftJustified(30, whenstill);
                addLeftJustified(30, always);
                addLeftJustified(0, new PropCheckBox("Use back buffer (NOT HOOKED UP YET: AWAITING CODE REVIEW)", "useyetanotherbackbuffer", false, mRepainter, "Whether to use yet another back buffer.  Seems to speed up antialiasing tremendously on some platforms."));
                addLeftJustified(0, new JButton("Just repaint the main view") {{
                    addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            dependent.repaint();
                        }
                    });
                }});
                addLeftJustified(0, new JButton("undock") {{
                  setFont(getFont().deriveFont(10.f));  // assumes default is 12
                  setBorder(javax.swing.BorderFactory.createEmptyBorder());
                  addActionListener(new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent ae) {
                        toggleDocking();
                        setLabel(parentForRedocking != null ? "redock" : "undock");
                      }
                  });
                }});
                setMaximumSize(getPreferredSize());  // necessary to prevent spreading out of controls when stretched vertically
            }});
        }
        private void toggleDocking() {
            if (parentForRedocking == null)
            {
                // Undock.
                Container parent = ExpertControlPanel.this.getParent();
                parent.remove(ExpertControlPanel.this);
                parent.revalidate();
                parent.repaint();
                parentForRedocking = parent;

                JFrame frame = new JFrame("Expert Control Panel");
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        toggleDocking();
                    }
                });
                frame.getContentPane().add(ExpertControlPanel.this);
                frame.pack();
                frame.setVisible(true);
            }
            else
            {
                // Redock.
                Container frame = ExpertControlPanel.this.getParent();
                while (!(frame instanceof JFrame))
                {
                    frame = frame.getParent();
                }

                parentForRedocking.add(ExpertControlPanel.this);
                parentForRedocking.revalidate();
                parentForRedocking.repaint();

                parentForRedocking = null;

                ((JFrame)frame).dispose();
            }
        }

        private Container parentForRedocking = null;
    };  // class ExpertControlPanel

    static {
        mTextArea.setWrapStyleWord(true);
        JScrollPane scroller = new JScrollPane(mTextArea);
        int console_height = 500;
        mInstance.setBounds(0, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - console_height, 1000, console_height);
        mInstance.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.setOut(mOriginalOut);
                System.setErr(mOriginalErr);
            }
        });
        JMenuBar menubar = new JMenuBar();
        mInstance.setJMenuBar(menubar);
        JMenu menu = new JMenu("Experimental new puzzle menu");
        menubar.add(menu);
        menu.add(new JMenuItem("Click me please!"));

        mInstance.getContentPane().add(new JSplitPane(
            /*orientation= */JSplitPane.VERTICAL_SPLIT,
            /*continuoutLayout= */true,
            /*topComponent= */new JSplitPane(
                /*orientation= */JSplitPane.HORIZONTAL_SPLIT,
                /*continuousLayout= */true,
                /*leftComponent= */new ExpertControlPanel(),
                /*rightComponent= */new JCol() {{
                    add(new JLabel("Some gratuitous puzzles:"));
                    add(multiHorizontalSplitPane(
                        new Component[] {
                            makePuzzle("{3,3}x{}", "3(4)"),
                            makePuzzle("{4,3}x{}", "3"),
                            makePuzzle("{3,4}x{}", "3(4)"),
                            makePuzzle("{5,3}x{}", "3(2.5)"),
                            makePuzzle("{3,5}x{}", "3(4)"),
                        }, 0)); }}
                ) {{ setOneTouchExpandable(true); }},
            /*bottomComponent= */scroller) {{ setOneTouchExpandable(true); }});
    }

    private static Component makePuzzle(String schlafli, String lengthString) {
        final int[] num_twists = new int[1];
        System.out.println("version " + System.getProperty("java.version"));
        final MC4DView view = new MC4DView(new PuzzleManager(schlafli, lengthString, new JProgressBar()), new RotationHandler()) {{
            // TODO: MC4DView should listen to all of these!!
            for (final String propname : new String[] {MagicCube.DEBUGGING,
                                                       MagicCube.BLINDFOLD,
                                                       "antialiasing",
                                                       "shadows",
                                                       "useyetanotherbackbuffer",
                                                      }) {
                PropertyManager.top.addPropertyListener(new PropertyManager.PropertyListener() {
                    @Override
                    public void propertyChanged(String property, String newval) {
                        repaint();
                    }
                }, propname);
            };
        }};
        view.addStickerListener(new MC4DView.StickerListener() {
            @Override
            public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
                view.animate(twisted, new ItemCompleteCallback() {
                    @Override
                    public void onItemComplete(MagicCube.TwistData twist) {
                        System.out.println("Num Twists: " + ++num_twists[0]);
                    }
                });
                if (false)  // this is good example code but it's surprising so don't do it
                {
                    boolean is_checked = PropertyManager.getBoolean(MagicCube.BLINDFOLD, false);
                    PropertyManager.userprefs.setProperty(MagicCube.BLINDFOLD, !is_checked + "");
                    dependent.repaint();
                }
            }
        });
        return view;
    }

    private Console() {
    }

    public static void show(String title, Component dep) {
        dependent = dep;
        mInstance.setTitle(title);
        System.setOut(mTextStream);
        System.setErr(mTextStream);
        mInstance.setVisible(true);
    }

    public static void show(String title) {
        show(title, null);
    }

    public static int getLineCount() {
        return mTextArea.getLineCount();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Console.show("Console Test");
                System.out.println("Standard out and error messages are redirected here while this window is showing. Closing it reverts them.");
            }
        });
        for(int i = 0; i < 15; i++) {
            System.out.println("Time " + i);
            try {
                if(i == 3)
                    throw new Exception("Exception test");
                Thread.sleep(1000);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }


    public static class TextAreaOutputStream extends OutputStream {
        private final JTextArea textArea;
        private final StringBuilder sb = new StringBuilder();

        public TextAreaOutputStream(final JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public void write(int b) throws IOException {
            if(b == '\r')
                return;
            if(b == '\n') {
                final String text = sb.toString() + "\n";
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        textArea.append(text);
                    }
                });
                sb.setLength(0);
                return;
            }
            sb.append((char) b);
        }
    }

}
