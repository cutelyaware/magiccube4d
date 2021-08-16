package com.superliminal.util;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

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
    static {
        mTextArea.setWrapStyleWord(true);
        JScrollPane scroller = new JScrollPane(mTextArea);
        int console_height = 300;
        mInstance.setBounds(0, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - console_height, 1000, console_height);
        mInstance.getContentPane().add(scroller);
        mInstance.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.setOut(mOriginalOut);
                System.setErr(mOriginalErr);
            }
        });
    }

    private Console() {}

    public static void show(String title) {
        mInstance.setTitle(title);
        System.setOut(mTextStream);
        System.setErr(mTextStream);
        mInstance.setVisible(true);
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
        public void flush() {}

        @Override
        public void close() {}

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
