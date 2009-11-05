package com.superliminal.magiccube4d;

import javax.swing.*;

import com.superliminal.util.SpringUtilities;
import com.superliminal.util.StaticUtils;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Viewer/controller for a MacroManager. <br>
 *
 * Created Nov 12, 2006
 *
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MacroControls extends JPanel {
	private static final int MAX_BUTTON_WIDTH = 140;
	
    /**
     * Reports Macro activity.
     */
    public interface Listener {
        /**
         * Called when user selects a macro button.
         */
        public void apply(Macro macro, boolean reverse);
        /**
         * Called whenever the MacroManager contents have been changed.
         */
        public void changed();
    }
    private MacroManager mgr;
    private String schlafli;
    private Listener app;
    private Macro selected;
    private final static Font SYMBOL_FONT = new Font("Dialog", Font.PLAIN, 12);

    public MacroControls(final MacroManager mgr, String schlafli, final Listener app) {
        this.mgr = mgr;
        this.schlafli = schlafli;
        this.app = app;
        init(false);
    }

    private void init(boolean changed) {

        removeAll();
        final JButton
	        moveUp = new JButton("\u25B2"),
            moveDn = new JButton("\u25BC"),
            rename = new JButton("Rename"),
            delete = new JButton("Delete");
        moveUp.setToolTipText("Move Up");
        moveDn.setToolTipText("Move Down");
        moveUp.setFont(SYMBOL_FONT);
        moveDn.setFont(SYMBOL_FONT);
        final Macro macros[] = mgr.getMacros();
        if(selected==null && macros.length>0)
            selected = macros[macros.length-1];
        moveUp.setEnabled(macros.length>0 && selected != macros[0]);
        moveDn.setEnabled(macros.length>0 && selected != macros[macros.length-1]);
        rename.setEnabled(selected != null);
        delete.setEnabled(selected != null);
        ButtonGroup group = new ButtonGroup();
        JPanel grid = new JPanel(new SpringLayout());
        for (int iMacro = 0; iMacro < macros.length; ++iMacro) {
            final Macro macro = macros[iMacro];
            final JRadioButton rb = new JRadioButton();
            rb.setSelected(macro == selected);
            rb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selected = macro;
                    moveUp.setEnabled(selected != macros[0]);
                    moveDn.setEnabled(selected != macros[macros.length-1]);
                }
            });
            group.add(rb);
            grid.add(rb);
            JButton forward = new JButton(macro.getName());
            forward.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    rb.doClick();
                    app.apply(macro, false);
                }
            });
            forward.setPreferredSize(new Dimension(Math.min(forward.getPreferredSize().width, MAX_BUTTON_WIDTH), forward.getPreferredSize().height));
            String appropriatePuzzle =  macro.getPuzzleString();
            appropriatePuzzle = appropriatePuzzle.substring(0, appropriatePuzzle.indexOf(' '));
            forward.setEnabled(appropriatePuzzle.equals(schlafli));
            forward.setToolTipText(appropriatePuzzle);
            grid.add(forward);
            JButton reverse = new JButton("Reversed");
            reverse.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    rb.doClick();
                    app.apply(macro, true);
                }
            });
            reverse.setEnabled(appropriatePuzzle.equals(schlafli));
            reverse.setToolTipText(appropriatePuzzle);
            grid.add(reverse);
        }
        SpringUtilities.makeCompactGrid(grid, macros.length, 3, 0, 0, 1, 2);
        JPanel tmp = new JPanel();
        tmp.add(grid);
        JScrollPane gridScroll = new JScrollPane(tmp);

        moveUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(selected == null)
                    return;
                mgr.moveMacro(selected, -1);
                init(true);
            }
        });
        moveDn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(selected == null)
                    return;
                mgr.moveMacro(selected, 1);
                init(true);
            }
        });
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(selected == null)
                    return;
                String newname = JOptionPane.showInputDialog("Rename Macro", selected.getName());
                if(newname == null)
                    return;
                selected.setName(newname);
                init(true);
            }
        });
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(selected == null)
                    return;
                mgr.removeMacro(selected);
                selected = null;
                init(true);
            }
        });

        JPanel controls = new JPanel();
        controls.add(moveUp);
        controls.add(moveDn);
        controls.add(rename);
        controls.add(delete);

        // layout components
        setLayout(new BorderLayout());
        add(gridScroll, "Center");
        add(controls, "South");

        validate();
        repaint();

        if(changed)
            app.changed();
    } // end init

    public static void main(String[] args) {
        MacroManager mm = new MacroManager("C:\\Java\\MC4D\\MC4D.macros");
        MacroControls mc = new MacroControls(mm, "{4,3,3}", new MacroControls.Listener() {
            public void apply(Macro macro, boolean reverse) {
                System.out.println("Applying " + macro.getName() + (reverse ? " reversed" : " forward"));
            }
            public void changed() {
                System.out.println("Macros changed");
            }
        });
        JFrame frame = new StaticUtils.QuickFrame("MacroControls Test");
        frame.add(mc);
        frame.setVisible(true);
    }
} // end class MacroControls
