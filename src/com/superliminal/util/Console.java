package com.superliminal.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.superliminal.magiccube4d.MC4DView;
import com.superliminal.magiccube4d.MC4DView.ItemCompleteCallback;
import com.superliminal.magiccube4d.MagicCube;
import com.superliminal.magiccube4d.PuzzleManager;
import com.superliminal.magiccube4d.RotationHandler;

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
        JMenu menu = new JMenu("Don's menu");
        menubar.add(menu);
        menu.add(new JMenuItem("Click me please!"));
        Container contents = mInstance.getContentPane();
        contents.setLayout(new BorderLayout());
        contents.add(makePuzzle(), "Center"); // Just for example. Needs the scroller below but they fight for space.
//      contents.add(scroller, "South");
    }

    private static Component makePuzzle() {
        final String SCHLAFLI = "{3,3,3}";
        final String LENGTHSTRING = "3";
        final int[] num_twists = new int[1];
        System.out.println("version " + System.getProperty("java.version"));
        final MC4DView view = new MC4DView(new PuzzleManager(SCHLAFLI, LENGTHSTRING, new JProgressBar()), new RotationHandler());
        view.addStickerListener(new MC4DView.StickerListener() {
            @Override
            public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
                view.animate(twisted, new ItemCompleteCallback() {
                    @Override
                    public void onItemComplete(MagicCube.TwistData twist) {
                        System.out.println("Num Twists: " + ++num_twists[0]);
                    }
                });
                boolean is_checked = PropertyManager.getBoolean(MagicCube.BLINDFOLD, false);
                PropertyManager.userprefs.setProperty(MagicCube.BLINDFOLD, !is_checked + "");
                dependent.repaint();
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
