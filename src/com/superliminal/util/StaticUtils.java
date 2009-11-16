package com.superliminal.util;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;

/**
 * A collection of generally useful Swing utility methods.
 *
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
public class StaticUtils {
    // to disallow instantiation
    private StaticUtils(){}

    /**
     * Adds a control hot key to the containing window of a component.
     * In the case of buttons and menu items it also attaches the given action to the component itself.
     *
     * @param key one of the KeyEvent keyboard constants
     * @param to component to map to
     * @param actionName unique action name for the component's action map
     * @param action callback to notify when control key is pressed
     */
    public static void addHotKey(int key, JComponent to, String actionName, Action action) {
    	addHotKey(KeyStroke.getKeyStroke(key, java.awt.event.InputEvent.CTRL_MASK), to, actionName, action);
    }
    
    public static void addHotKey(KeyStroke keystroke, JComponent to, String actionName, Action action) {
        InputMap map = to.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        map.put(keystroke, actionName);
        to.getActionMap().put(actionName, action);
        if(to instanceof JMenuItem)
            ((JMenuItem)to).setAccelerator(keystroke);
        if(to instanceof AbstractButton) // includes JMenuItem
            ((AbstractButton)to).addActionListener(action);
    }

    /**
     * Finds the top-level JFrame in the component tree containing a given component.
     * @param comp leaf component to search up from
     * @return the containing JFrame or null if none
     */
    public static JFrame getTopFrame(Component comp) {
        if(comp == null)
            return null;
        while (comp.getParent() != null)
            comp = comp.getParent();
        if (comp instanceof JFrame)
            return (JFrame) comp;
        return null;
    }

    /**
     * Different platforms use different mouse gestures as pop-up triggers.
     * This class unifies them. Just implement the abstract popUp method
     * to add your handler.
     */
    public static abstract class PopperUpper extends MouseAdapter {
        // To work properly on all platforms, must check on mouse press as well as release
        @Override
		public void mousePressed(MouseEvent e)  { if(e.isPopupTrigger()) popUp(e); }
        @Override
		public void mouseReleased(MouseEvent e) { if(e.isPopupTrigger()) popUp(e); }
        protected abstract void popUp(MouseEvent e);
    }

    // simple Clipboard string routines

    public static void placeInClipboard(String str) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new StringSelection(str), null);
    }

    public static String getFromClipboard() {
        String str = null;
        try {
            str = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(
                DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * Draws the given string into the given graphics with the area behind the string
     * filled with a given background color.
     */
    public static void fillString(String str, int x, int y, Color bg, Graphics g) {
        Rectangle2D strrect = g.getFontMetrics().getStringBounds(str, null);
        Color ocolor = g.getColor();
        g.setColor(bg);
        g.fillRect((int)(x+strrect.getX()), (int)(y+strrect.getY()), (int)(strrect.getWidth()), (int)(strrect.getHeight()));
        g.setColor(ocolor);
        g.drawString(str, x, y);
    }

    /**
     * Utility class that initializes a medium sized, screen-centered, exit-on-close JFrame.
     * Mostly useful for simple example main programs.
     */
    @SuppressWarnings("serial")
	public static class QuickFrame extends JFrame {
        public QuickFrame(String title) {
            super(title);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(640, 480);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(
                Math.max(0,screenSize.width/2  - getWidth()/2),
                Math.max(0,screenSize.height/2 - getHeight()/2));
        }
    }
    
    public static String getHomeDir() {
    	return FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
    }
    
    public static String getBinDir() {
		try {
			String here = new File(StaticUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString();
			return here.endsWith(".jar") ? here.substring(0, here.lastIndexOf(File.separator)) : here;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
    }

    /**
     * Simple file filter suitable for use with JFileChooser to accept files with a given extension.
     * Example: myFilechooser.setFileFilter.setFileFilter(new ExtentionFilter("log", "MyApp Error Log Files")
     */
    public static class ExtentionFilter extends FileFilter {
        private String extention;
        private String description;
        public ExtentionFilter(String extention, String description) {
            this.extention = extention;
            this.description = description;
        }
        @Override
		public boolean accept(File f) {
            return f.isDirectory() || extention.equalsIgnoreCase(getExtension(f));
        }
        @Override
		public String getDescription() {
            return description;
        }
        public static String getExtension(File f) {
            String s = f.getName();
            int i = s.lastIndexOf('.');
            if(i>0 && i<s.length()-1)
                return s.substring(i+1).toLowerCase();
            return null;
        }
    }

    /**
     * Selection utility in the style of the JOptionPane.showXxxDialog methods.
     * Given a JTree, presents an option dialog presenting the tree allowing users to select a node.
     * @param tree is the tree to display
     * @param parent is the component to anchor the dialog to
     * @return the path of the selected tree node or null if canceled.
     */
    public static TreePath showTreeNodeChooser(JTree tree, String title, Component parent) {
        final String OK = "OK", CANCEL = "Cancel";
        final JButton ok_butt = new JButton(OK), cancel_butt = new JButton(CANCEL);
        final TreePath selected[] = new TreePath[] { tree.getLeadSelectionPath() }; // only an array so it can be final, yet mutable
        ok_butt.setEnabled(selected[0] != null);
        final JOptionPane option_pane = new JOptionPane(new JScrollPane(tree), JOptionPane.QUESTION_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new Object[]{ok_butt, cancel_butt});
        ok_butt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                option_pane.setValue(OK);
            }
        });
        cancel_butt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                option_pane.setValue(CANCEL);
                selected[0] = null;
            }
        });
        TreeSelectionListener tsl = new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selected[0] = e.getNewLeadSelectionPath();
                ok_butt.setEnabled(selected[0] != null);
            }
        };
        JDialog dialog = option_pane.createDialog(parent, title);
        tree.addTreeSelectionListener(tsl); // to start monitoring user tree selections
        dialog.setVisible(true); // present modal tree dialog to user
        tree.removeTreeSelectionListener(tsl); // don't want to clutter caller's tree with listeners
        return OK.equals(option_pane.getValue()) ? selected[0] : null;
    }

    public static void main(String[] args) {
        TreePath got = showTreeNodeChooser(new JTree(), "Select A Node", null);
        System.out.println(got);
        System.exit(0);
    }
}
