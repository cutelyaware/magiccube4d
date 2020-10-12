package com.superliminal.magiccube4d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.superliminal.util.ColorButton;
import com.superliminal.util.ColorButton.ColorChangeListener;
import com.superliminal.util.Console;
import com.superliminal.util.PropControls;
import com.superliminal.util.PropControls.PropCheckBox;
import com.superliminal.util.PropControls.PropRadioButton;
import com.superliminal.util.PropControls.PropSlider;
import com.superliminal.util.PropertyManager;
import com.superliminal.util.ResourceUtils;
import com.superliminal.util.SpringUtilities;
import com.superliminal.util.StaticUtils;


/**
 * The main desktop application.
 * The main method here creates and shows an instance of this class.
 * 
 * Copyright 2005 - Superliminal Software
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DSwing extends JFrame {

    // The puzzle states regarding scrambling.
    private final static int
        SCRAMBLE_NONE = 0, // Unscrambled
        SCRAMBLE_PARTIAL = 1, // Some small number of scrambles
        SCRAMBLE_FULL = 2, // Fully scrambled
        SCRAMBLE_SOLVED = 3; // Was solved by user even if not currently solved
    private int scrambleState = SCRAMBLE_NONE;

    // Macro state
    private MacroManager macroMgr = new MacroManager(PropertyManager.top.getProperty("macrofile",
        StaticUtils.getHomeDir() + File.separator + "MC4D.macros"));
    private Macro lastMacro;

    // Top level Model and View objects in MVC pattern.
    private History hist; // Contains the list of all moves and marks.
    private MC4DView view; // 4D display of the puzzle state, animates a "move queue" and notifies as they complete.

    // View transformations
    private BoundedRangeModel viewScaleModel;
    private RotationHandler rotations = new RotationHandler(MagicCube.NICE_VIEW);

    // References to Swing components.
    private JLabel
        statusLabel = new JLabel(), // Lower left text describing latest state or info messages.
        twistLabel = new JLabel(); // Lower right count of the number of pure twists in History.
    private Color
        normalStatus = statusLabel.getForeground(),
        warningStatus = Color.red.darker();
    private JPanel mainViewContainer = new JPanel(new BorderLayout()); // Can use as addHotKey target when no MenuItem available.
    private JPanel mainTabsContainer = new JPanel(new BorderLayout());
    private JFileChooser
        logFileChooser = new JFileChooser(),
        macroFileChooser = new JFileChooser();
    private JProgressBar progressBar = new JProgressBar();

    private PuzzleManager puzzleManager = null;

    private JMenu apply = new JMenu("Apply"); // Currently unused since Macros tab added.

    private JMenuItem
        openitem = new JMenuItem("Open"),
        saveitem = new JMenuItem("Save"),
        saveasitem = new JMenuItem("Save As..."),
        quititem = new JMenuItem("Quit"),
        resetitem = new JMenuItem("Reset"),
        undoitem = new JMenuItem("Undo"),
        redoitem = new JMenuItem("Redo"),
        beginitem = new JMenuItem("Go To Beginning"),
        enditem = new JMenuItem("Go To End"),
        playitem = new JMenuItem("Play"),
        cheatitem = new JMenuItem("Solve (Cheat)"),
        solveitem = new JMenuItem("Solve (For Real)");

    private void setStatus(String text, boolean warning) {
        statusLabel.setForeground(warning ? warningStatus : normalStatus);
        statusLabel.setText(text);
    }
    private void setStatus(String text) {
        setStatus(text, false);
    }
    private void updateTwistsLabel() {
        int twists = hist.countTwists();
        twistLabel.setText("Total Twists: " + twists);
    }

    private void updateEditMenuItems() {
        saveitem.setEnabled(true);
        cheatitem.setEnabled(hist.hasPreviousMove());
        solveitem.setEnabled(!puzzleManager.isSolved());
    }

    /**
     * Cancels any macro mode, animation, audio, etc.
     */
    private void cancel() {
        boolean macro_canceled = false;
        view.cancelAnimation(); // also stops any animation
        Audio.stop(Audio.Sound.TWISTING); // TODO: Needs a stopAll() method.
        if(macroMgr.isOpen()) {
            macroMgr.cancel();
            macro_canceled = true;
        }
        setSkyAndHighlighting(null, normalHighlighter, false);
        // Remove any set-up mark. Consider also undoing any set-up moves.
        macro_canceled |= hist.removeLastMark(History.MARK_SETUP_MOVES);
        syncPuzzleStateWithHistory();
        setStatus(macro_canceled ? "Cancelled" : "");
    }

    private PuzzleManager.Highlighter normalHighlighter = new PuzzleManager.Highlighter() {
        @Override
        public boolean shouldHighlightSticker(PuzzleDescription puzzle,
            int stickerIndex, int gripIndex, int slicemask, int x, int y, boolean isControlDown)
        {
            if(isControlDown)
                return puzzleManager.canRotateToCenter(x, y, rotations);
            else
                return PipelineUtils.hasValidTwist(gripIndex, slicemask, puzzleManager.puzzleDescription);
        }
    };


    /**
     * Writes the current state of the History to a log file with the given name.
     * 
     * @param logFileName
     */
    private void saveAs(String logFileName) {
        if(logFileName == null) {
            setStatus("saveAs: null file name. File not saved.", true);
            return;
        }
        File file = new File(logFileName);
        String sep = System.getProperty("line.separator");
        try {
            Writer writer = new FileWriter(file);
            /*
             * First Line Format:
             * 0 - Magic Number
             * 1 - File Version
             * 2 - Scramble State
             * 3 - Twist Count
             * 4 - Schlafli Product
             * 5 - Edge Length
             */
            writer.write(
                MagicCube.MAGIC_NUMBER + " " +
                    MagicCube.LOG_FILE_VERSION + " " +
                    scrambleState + " " +
                    hist.countTwists() + " " +
                    puzzleManager.puzzleDescription.getSchlafliProduct() + " " +
                    puzzleManager.getPrettyLength());
            writer.write(sep);
            rotations.write(writer);
            //writer.write(sep + puzzle.toString());
            writer.write("*" + sep);
            hist.truncate(); // Don't save potential redo moves after current. Note: loses redo moves as a side effect!
            writer.write(hist.toString());
            writer.write(sep);
            writer.close();
            String filepath = file.getAbsolutePath();
            setStatus("Wrote log file " + filepath);
            PropertyManager.userprefs.setProperty("logfile", filepath);
            setTitle(MagicCube.TITLE + " - " + file.getName());
        } catch(IOException ioe) {
            setStatus("Save to '" + logFileName + "' failed. Consider using 'File > Save As' to save somewhere else.", true);
        }
    } // end saveAs()


    private static boolean isControlDown(ActionEvent e) {
        return e != null && (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK;
    }

    /**
     * The sky color and the way the stickers highlight as the user mouses over them
     * are tightly coupled with the different macro modes and modifier keys being held down.
     * This method is called when those things change.
     * 
     * @param sky The sky color for the current mode.
     * @param stickerHighlighter controls which stickers highlight in the current mode.
     * @param isControlDown Whether the user is holding down the Control key.
     */
    private void setSkyAndHighlighting(Color sky, PuzzleManager.Highlighter stickerHighlighter, boolean isControlDown) {
        view.setSkyOverride(sky);
        puzzleManager.setHighlighter(stickerHighlighter);
        view.updateStickerHighlighting(isControlDown);
    }

    private void applySequence(MagicCube.TwistData[] moves) {
        hist.mark(History.MARK_MACRO_OPEN);
        hist.apply(moves);
        hist.mark(History.MARK_MACRO_CLOSE);
        view.animate(moves, null, true);
    }


    /**
     * Like a regular AbstractAction but one which does nothing if triggered while the view is animating.
     * Users implement the "doit" method instead of the now-final actionPerformed which calls it.
     */
    private abstract class ProbableAction extends AbstractAction {
        protected ProbableAction(String name) {
            super(name);
        }
        public abstract void doit(ActionEvent ae);
        @Override
        public final void actionPerformed(ActionEvent ae) {
            if(view.isAnimating())
                return;
            doit(ae);
        }
    }

    //
    // CONTROLLERS
    //
    // Here begins a set of named actions triggered directly by UI components,
    // usually MenuItems and hot-keys.
    //


    // The first set are Actions which should only be enabled when not animating.
    //
    private ProbableAction
        open = new ProbableAction("Open") {
            @Override
            public void doit(ActionEvent ae) {
                if(logFileChooser.showOpenDialog(MC4DSwing.this) == JFileChooser.APPROVE_OPTION) {
                    String filepath = logFileChooser.getSelectedFile().getAbsolutePath();
                    PropertyManager.userprefs.setProperty("logfile", filepath);
                    initPuzzle(filepath);
                }
            }
        },
        save = new ProbableAction("Save") {
            @Override
            public void doit(ActionEvent ae) {
                // Save to the previously used log file, if any, otherwise the default.
                String fname = logFileChooser.getSelectedFile() == null ? null : logFileChooser.getSelectedFile().getAbsolutePath();
                if(fname == null)
                    fname = StaticUtils.getHomeDir() + File.separator + MagicCube.LOG_FILE;
                cancel(); // To at least strip out any macro set-up moves mark.
                saveAs(PropertyManager.top.getProperty("logfile", fname));
            }
        },
        saveas = new ProbableAction("Save As...") {
            @Override
            public void doit(ActionEvent ae) {
                if(logFileChooser.showSaveDialog(MC4DSwing.this) == JFileChooser.APPROVE_OPTION) {
                    String path = logFileChooser.getSelectedFile().getAbsolutePath();
                    if(!path.endsWith(".log")) {
                        path += ".log";
                    }
                    cancel(); // To at least strip out any macro set-up moves mark.
                    saveAs(path);
                }
            }
        },
        undo = new ProbableAction("Undo") {
            @Override
            public void doit(ActionEvent ae) {
                setStatus("");
                if(hist.atScrambleBoundary()) {
                    setStatus("Can't undo past scramble boundary.", true);
                    return;
                }
                if(macroMgr.isOpen()) {
                    // Try to remove last move from macro manager.
                    MagicCube.TwistData macroUndone = macroMgr.recording() ? macroMgr.removeTwist() : null;
                    if(macroUndone == null) { // User backed out of macro definition so cancel it.
                        macroMgr.cancel();
                        setSkyAndHighlighting(null, normalHighlighter, isControlDown(ae));
                        setStatus("Macro canceled");
                    }
                    else { // Macro move was undone. Now undo corresponding history move.
                        MagicCube.TwistData histUndone = hist.undo(); // Perform the undo.
                        // Sanity check that it was the same as from the macro manager.
                        assert (macroUndone.direction == histUndone.direction && macroUndone.slicemask == histUndone.slicemask && macroUndone.grip.id_within_puzzle == histUndone.grip.id_within_puzzle);
                        view.animate(histUndone, null, true); // Show the result.
                        setStatus("Macro twists: " + macroMgr.numTwists());
                    }
                }
                // Note: If a macro is cancelled above, this "else if" means no twist will be undone.
                // It's as if the undo action undid the macro open and another undo is needed to remove a previous move.
                // If users dislike needing an extra undo, simply turn the "else if" into "if".
                else if(hist.atMacroClose()) { // Undo a macro invocation.
                    setStatus("Undoing macro");
                    for(MagicCube.TwistData histUndone = hist.undo(); histUndone != null; histUndone = hist.undo()) {
                        view.animate(histUndone, null, true);
                        if(hist.atMacroOpen())
                            break;
                    }
                    view.append(clearStatus);// Will erase the label above.
                    assert (hist.atMacroOpen());
                    hist.goToPrevious(); // step over mark to point where macro was applied.
                }
                else { // Normal undo of a single twist.
                    if(hist.atMark(History.MARK_SETUP_MOVES)) { // Cancel macro set-up moves.
                        hist.removeAllMarks(History.MARK_SETUP_MOVES);
                        setSkyAndHighlighting(null, normalHighlighter, isControlDown(ae));
                    }
                    MagicCube.TwistData histUndone = hist.undo();
                    if(histUndone == null)
                        setStatus("Nothing to undo", true);
                    else
                        view.animate(histUndone);
                }
            }
        },
        redo = new ProbableAction("Redo") { // Undo a previous undo.
            @Override
            public void doit(ActionEvent ae) {
                setStatus("");
                if(hist.atMacroOpen()) {
                    setStatus("Redoing macro");
                    for(MagicCube.TwistData histRedone = hist.redo(); histRedone != null; histRedone = hist.redo()) {
                        view.animate(histRedone, null, true);
                        if(macroMgr.recording()) // Allows macro redo during macro creation.
                            macroMgr.addTwist(histRedone);
                        if(hist.atMacroClose())
                            break;
                    }
                    assert (hist.atMacroClose());
                    hist.goToNext(); // step past macro close mark.
                    view.append(clearStatus); // Will erase the label above.
                }
                else {
                    MagicCube.TwistData histRedone = hist.redo();
                    if(histRedone == null)
                        setStatus("Nothing to redo", true);
                    else {
                        view.animate(histRedone);
                        if(macroMgr.recording()) {
                            macroMgr.addTwist(histRedone);
                            setStatus("Macro twists: " + macroMgr.numTwists());
                        }
                    }
                }
            }
        },
        cheat = new ProbableAction("Cheat") { // Undo all with animation.
            @Override
            public void doit(ActionEvent ae) {
                view.append(undoMore);
            }
        },
        play = new ProbableAction("Play") { // Redo all with animation.
            @Override
            public void doit(ActionEvent ae) {
                view.append(redoMore);
            }
        },
        beginning = new ProbableAction("Go to Beginning") { // Undo all without animation.
            @Override
            public void doit(ActionEvent ae) {
                cancel();
                if(!hist.goBackwardsToMark(History.MARK_SCRAMBLE_BOUNDARY)) // Try to go back to a scramble mark.
                    hist.goToBeginning(); // Not found so go back all the way.
                else
                    hist.goToNext(); // Hit scramble mark which is now current, so go to next real move.
                syncPuzzleStateWithHistory();
                updateTwistsLabel();
            }
        },
        end = new ProbableAction("Go to End") { // Redo all. No animation.
            @Override
            public void doit(ActionEvent ae) {
                cancel();
                hist.goToEnd();
                syncPuzzleStateWithHistory();
            }
        },

//        solve = new ProbableAction("Solve") { // A true solve.
//            @Override
//            public void doit(ActionEvent ae) {
//                MagicCube.TwistData[] solution;
//                try { solution = puzzle.solve(); }
//                catch(Error e) {
//                    setStatus("No solution", true);
//                    return;
//                }
//                solution = History.compress(solution, (int)puzzleManager.puzzleDescription.getEdgeLength(), true);
//                view.animate(solution, applyToHistory, false);
//                scrambleState = SCRAMBLE_NONE; // no user credit for automatic solutions.
//                setStatus("Twists to solve = " + solution.length);
//            }
//        },
        macro = new ProbableAction("Start/End") { // toggles macro definition start/end
            @Override
            public void doit(ActionEvent ae) {
                if(macroMgr.isOpen()) { // finished with macro definition
                    String name = JOptionPane.showInputDialog("Name your macro");
                    if(name == null) {
                        macroMgr.cancel();
                        setStatus("");
                    }
                    else {
                        lastMacro = macroMgr.close(name);
                        initMacroMenu(); // to show new macro.
                        initTabs(); // to show new control
                        setStatus("Defined \"" + lastMacro.getName() + "\" macro with " +
                            lastMacro.length() + " move" + (lastMacro.length() == 1 ? "." : "s."));
                    }
                    setSkyAndHighlighting(null, normalHighlighter, isControlDown(ae));
                } else { // begin macro definition
                    macroMgr.open(0);
                    setStatus("Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                    setSkyAndHighlighting(Color.white, macroMgr, isControlDown(ae));
                }
            }
        },
        // Cancel is a special action not just for canceling macros as the label implies.
        // It is also used internally whenever it's important to return to the normal mode.
        cancel = new ProbableAction("Cancel Macro") {
            @Override
            public void doit(ActionEvent ae) {
                boolean macro_canceled = false;
                view.cancelAnimation(); // also stops any animation
                Audio.stop(Audio.Sound.TWISTING); // TODO: Needs a stopAll() method.
                if(macroMgr.isOpen()) {
                    macroMgr.cancel();
                    macro_canceled = true;
                }
                setSkyAndHighlighting(null, normalHighlighter, isControlDown(ae));
                // Remove any set-up mark. Consider also undoing any set-up moves.
                macro_canceled |= hist.removeLastMark(History.MARK_SETUP_MOVES);
                syncPuzzleStateWithHistory();
                setStatus(macro_canceled ? "Cancelled" : "");
            }
        },
        last = new ProbableAction("Apply Last Macro") {
            @Override
            public void doit(ActionEvent ae) {
                if(macroMgr.isOpen()) {
                    if(!macroMgr.recording()) {
                        setStatus("Finish specifying reference stickers before applying an inner macro.", true);
                        return;
                    }
                    MagicCube.Stickerspec[] refs = macroMgr.getRefs();
                    MagicCube.TwistData[] moves = lastMacro.getTwists(refs, puzzleManager.puzzleDescription);
                    if(moves == null) {
                        setStatus("Inner macro reference sticker pattern does not match outer macro.", true);
                        return;
                    }
                    if(getTwistDirection(ae) < 0)
                        Macro.reverse(moves);
                    macroMgr.addTwists(moves);
                    applySequence(moves);
                    return;
                }
                if(lastMacro == null) {
                    setStatus("No last macro to apply.", true);
                    return;
                }
                macroMgr.open(getTwistDirection(ae));
                setStatus("Applying macro '" + lastMacro.getName() + "'. Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                setSkyAndHighlighting(new Color(255, 170, 170), macroMgr, isControlDown(ae));
            }
        },
        setup = new ProbableAction("Macro Set-Up Moves") {
            @Override
            public void doit(ActionEvent ae) {
                if(macroMgr.isOpen()) {
                    setStatus("Cannot define set-up moves once a macro is already open.", true);
                    return;
                }
                // Begin recording macro set-up moves.
                hist.removeAllMarks(History.MARK_SETUP_MOVES);
                hist.mark(History.MARK_SETUP_MOVES);
                setStatus("Recording pre-macro set-up moves. Esc to cancel.");
                view.setSkyOverride(new Color(255, 255, 170));
            }
        };

    // Actions which *can* be realistically performed while animations are playing
    //
    private Action
        quit = new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        },
        reset = new AbstractAction("Reset") {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                scrambleState = SCRAMBLE_NONE; // do first to avoid issue 64 (fanfare on reset).
                cancel();
                puzzleManager.resetPuzzleState(); // also fires puzzle change event.
                setStatus("Reset");
                view.repaint();
            }
        },
        read = new AbstractAction("Read") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(macroFileChooser.showOpenDialog(MC4DSwing.this) != JFileChooser.APPROVE_OPTION)
                    return;
                File file = macroFileChooser.getSelectedFile();
                String filepath;
                try {
                    filepath = file.getCanonicalPath();
                }
                catch(IOException e) {
                    setStatus("Couldn't read macro file: " + file.getAbsolutePath(), true);
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", filepath);
                macroMgr = new MacroManager(filepath);
                initMacroMenu(); // update controls with macro definitions just read.
                initTabs(); // to show new controls
                int nread = apply.getItemCount();
                setStatus("Read " + nread + " macro" + (nread == 1 ? "" : "s") + " from " + filepath);
            }
        },
        write = new AbstractAction("Write") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    macroMgr.write();
                } catch(IOException e) {
                    setStatus("Couldn't write to macro file " + macroMgr.getFilePath(), true);
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", macroMgr.getFilePath());
                setStatus("Wrote macro file " + macroMgr.getFilePath());
            }
        },
        writeas = new AbstractAction("Write As") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(macroFileChooser.showSaveDialog(MC4DSwing.this) != JFileChooser.APPROVE_OPTION)
                    return;
                String filepath = macroFileChooser.getSelectedFile().getAbsolutePath();
                macroMgr.setFilePath(filepath);
                try {
                    macroMgr.write();
                } catch(IOException e) {
                    setStatus("Couldn't write to macro file " + macroMgr.getFilePath(), true);
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", macroMgr.getFilePath());
                setStatus("Wrote macro file " + macroMgr.getFilePath());
            }
        };


    /**
     * An ItemCompleteCallback that adds its related twist to the history.
     */
    private MC4DView.ItemCompleteCallback applyToHistory = new MC4DView.ItemCompleteCallback() {
        @Override
        public void onItemComplete(MagicCube.TwistData twist) {
            hist.apply(twist);
        }
    };

    /**
     * @return an ItemCompleteCallback that sets the status line to the given text.
     */
    private MC4DView.ItemCompleteCallback makeLabeler(final String label) {
        return new MC4DView.ItemCompleteCallback() {
            @Override
            public void onItemComplete(MagicCube.TwistData twist) {
                setStatus(label);
            }
        };
    }

    /**
     * An ItemCompleteCallback that clears the status text.
     */
    private MC4DView.ItemCompleteCallback clearStatus = makeLabeler("");

    private final MC4DView.ItemCompleteCallback redoMore = new MC4DView.ItemCompleteCallback() {
        @Override
        public void onItemComplete(MagicCube.TwistData twist) {
            assert (twist == null);
            if(!hist.hasNextMove()) {
                return; // Nothing left to redo.
            }
            redo.doit(null);
            // Queue up another. Note: we can't tell at this point whether it will have an effect.
            // The previous one might redo a whole macro or get cancelled.
            view.append(redoMore);
        }
    };

    private final MC4DView.ItemCompleteCallback undoMore = new MC4DView.ItemCompleteCallback() {
        @Override
        public void onItemComplete(MagicCube.TwistData twist) {
            assert (twist == null);
            if(!hist.hasPreviousMove()) {
                return; // Nothing left to undo.
            }
            if(hist.atScrambleBoundary()) {
                scrambleState = SCRAMBLE_NONE; // no user credit for automatic solutions.
                hist.removeAllMarks(History.MARK_SCRAMBLE_BOUNDARY);
            }
            undo.doit(null);
            // Queue up another. Note: we can't tell at this point whether it will have an effect.
            // The previous one might undo a whole macro or get cancelled.
            view.append(undoMore);
        }
    };

    /**
     * An ItemCompleteCallback that causes checking for solved and updates the puzzle state.
     */
    private final MC4DView.ItemCompleteCallback checkSolved = new MC4DView.ItemCompleteCallback() {
        @Override
        public void onItemComplete(MagicCube.TwistData twist) {
            history_listener.currentChanged();
        }
    };


    /**
     * One of the main controllers, this one listens to the view object for reports of clicks on stickers.
     * Those clicks are interpreted as twists, macro sticker selections, etc.
     * 
     * Note: Control-click rotations of the whole puzzle are not reported here because they can be
     * handled entirely between the 4D view and the puzzle manager. It's just a view thing. This method
     * handles those things that can affect the puzzle state.
     */
    private MC4DView.StickerListener stickerListener = new MC4DView.StickerListener() {
        @Override
        public void stickerClicked(InputEvent e, MagicCube.TwistData twisted) {
            if(!macroMgr.isOpen()) { // The simple case.
                setStatus("");
                view.animate(twisted, applyToHistory);
                return;
            }
            // A macro is open.
            if(macroMgr.recording()) {
                // We are in the twist capturing phase of macro creation.
                macroMgr.addTwist(twisted);
                setStatus("Macro twists: " + macroMgr.numTwists());
                view.animate(twisted, applyToHistory);
            }
            else {
                // We are in the reference sticker capturing phase of macro creation. 
                // Cache direction here because macroMgr.close() below will clear it.
                int dir = macroMgr.getApplyDirection(); // Cache here because macroMgr.close() below will clear it.
                if(!macroMgr.addRef(puzzleManager.puzzleDescription, twisted.grip))
                    setStatus("Picked reference won't determine unique orientation, please try another.", true);
                else if(macroMgr.recording()) { // true when the reference sticker added was the last one needed.
                    if(dir == 0) {
                        setStatus("Recording macro twists. Hit <ctrl>m when finished or Esc to cancel.");
                        setSkyAndHighlighting(Color.black, normalHighlighter, e.isControlDown());
                    }
                    else { // Attempt to apply the macro.
                        setSkyAndHighlighting(null, normalHighlighter, e.isControlDown());
                        MagicCube.Stickerspec[] refs = macroMgr.close();
                        MagicCube.TwistData[] moves = lastMacro.getTwists(refs, puzzleManager.puzzleDescription);
                        if(moves == null)
                            setStatus("Reference sticker pattern doesn't match macro definition.", true);
                        else {
                            // Capture any set-up moves before applying macro moves so we don't undo them too.
                            Enumeration<MagicCube.TwistData> setup_moves = hist.movesFromMark(History.MARK_SETUP_MOVES);
                            if(dir < 0)
                                Macro.reverse(moves); // User asked to apply macro in reverse.
                            setStatus("Applying macro '" + lastMacro.getName() + "'");
                            applySequence(moves);
                            if(setup_moves.hasMoreElements()) {
                                view.append(makeLabeler("Reversing set-up moves"));
                                List<MagicCube.TwistData> setup_twists = java.util.Collections.list(setup_moves);
                                MagicCube.TwistData[] reverse_setup = setup_twists.toArray(new MagicCube.TwistData[0]);
                                Macro.reverse(reverse_setup);
                                hist.removeLastMark(History.MARK_SETUP_MOVES);
                                applySequence(reverse_setup);
                            }
                            view.append(clearStatus); // Will erase the above labels.
                            view.append(checkSolved); // Will check for solved after animation completes. Issue 124 (No fanfare when macro solved)
                        }
                    }
                }
                else { // More reference stickers are required.
                    setStatus("Selected " + macroMgr.numRefs() + " of " + Macro.MAXREFS + " reference stickers"); // a little camera sound here would be great.
                    view.updateStickerHighlighting(e);
                }
            }
        } // end stickerClicked
    }; // end new MC4DView.StickerListener()


    private ImageIcon getImageIcon(String path) {
        ClassLoader cl = getClass().getClassLoader();
        return new ImageIcon(cl.getResource(path));
    }


    /**
     * A fully-functional 4D Rubik's Cube.
     */
    public MC4DSwing() {
        super(PropertyManager.top.getProperty("title", MagicCube.TITLE));
        this.setIconImage(getImageIcon("mc4d.png").getImage());
        logFileChooser.setFileFilter(new StaticUtils.ExtentionFilter("log", "Magic Cube 4D Log Files"));
        if(PropertyManager.top.getProperty("logfile") != null)
            logFileChooser.setSelectedFile(new File(PropertyManager.top.getProperty("logfile")));
        macroFileChooser.setFileFilter(new StaticUtils.ExtentionFilter("macros", "Magic Cube 4D Macro Definition Files"));
        if(PropertyManager.top.getProperty("macrofile") != null)
            macroFileChooser.setSelectedFile(new File(PropertyManager.top.getProperty("macrofile")));
        // Init lastMacro.
        Macro[] macros = macroMgr.getMacros();
        if(macros.length > 0)
            lastMacro = macros[macros.length - 1];

        // set accelerator keys for some menu actions
        StaticUtils.addHotKey(KeyEvent.VK_0, resetitem, "Reset", reset);
        StaticUtils.addHotKey(KeyEvent.VK_Z, undoitem, "Undo", undo);
        StaticUtils.addHotKey(KeyEvent.VK_V, redoitem, "Redo", redo);
        StaticUtils.addHotKey(KeyEvent.VK_Y, redoitem, "Redo", redo);
        StaticUtils.addHotKey(KeyEvent.VK_P, playitem, "Play", play);
        StaticUtils.addHotKey(KeyEvent.VK_O, openitem, "Open", open);
        StaticUtils.addHotKey(KeyEvent.VK_S, saveitem, "Save", save);
        StaticUtils.addHotKey(KeyEvent.VK_Q, quititem, "Quit", quit);
        //StaticUtils.addHotKey(KeyEvent.VK_L, solveitem, "Real", solve);
        StaticUtils.addHotKey(KeyEvent.VK_L, cheatitem, "Cheat", cheat);
        StaticUtils.addHotKey(KeyEvent.VK_LEFT, beginitem, "Begin", beginning);
        StaticUtils.addHotKey(KeyEvent.VK_RIGHT, enditem, "End", end);

        // no hotkey
        saveasitem.addActionListener(saveas);

        // for debugging
        StaticUtils.addHotKey(KeyEvent.VK_I, mainViewContainer, "Ident View", new ProbableAction("Identity View") {
            @Override
            public void doit(ActionEvent ae) {
                rotations.set4dView(null);
                view.repaint();
            }
        });

        JMenu filemenu = new JMenu("File");
        filemenu.add(openitem);
        filemenu.addSeparator();
        filemenu.add(saveitem);
        filemenu.add(saveasitem);
        filemenu.addSeparator();
        filemenu.add(quititem);
        JMenu editmenu = new JMenu("Edit");
        editmenu.add(resetitem);
        editmenu.add(undoitem);
        editmenu.add(redoitem);
        editmenu.add(beginitem);
        editmenu.add(enditem);
        editmenu.addSeparator();
        editmenu.add(cheatitem);
        editmenu.add(playitem);
        //editmenu.add(solveitem); // commented out until we reimplement true solves.
        JMenu scramblemenu = new JMenu("Scramble");

        // Scrambling
        //
        class Scrambler extends ProbableAction {
            private int scramblechenfrengensen;
            public Scrambler(int scramblechens) {
                super("Scramble " + scramblechens);
                this.scramblechenfrengensen = scramblechens;
            }

            @Override
            public void doit(ActionEvent e) {
                int maxOrder = 0;
                for(int order : puzzleManager.puzzleDescription.getGripSymmetryOrders())
                    maxOrder = Integer.max(order, maxOrder);
                if(maxOrder < 2) {
                    // Avoids issue 102 (Scrambling length-1 puzzles locks up the program).
                    setStatus("Can't scramble puzzles with edge length < 2", true);
                    return;
                }
                scrambleState = SCRAMBLE_NONE; // do first to avoid issue 62 (fanfare on scramble).
                reset.actionPerformed(e);
                int previous_face = -1;
                int totalTwistsNeededToFullyScramble = puzzleManager.twistsNeededToFullyScramble();
                int scrambleTwists = scramblechenfrengensen == -1 ? totalTwistsNeededToFullyScramble : scramblechenfrengensen;
                if(scramblechenfrengensen == -1)
                    System.out.println("Performing " + scrambleTwists + " scrambling twists");
                Random rand = new Random();
                for(int s = 0; s < scrambleTwists; s++) {
                    // select a random grip that is unrelated to the last one (if any)
                    int iGrip, iFace, order;
                    do {
                        iGrip = rand.nextInt(puzzleManager.puzzleDescription.nGrips());
                        iFace = puzzleManager.puzzleDescription.getGrip2Face()[iGrip];
                        order = puzzleManager.puzzleDescription.getGripSymmetryOrders()[iGrip];
                    } while(order < 2 || // don't use 360 degree twists
                    iFace == previous_face || // mixing it up
                    (previous_face != -1 && puzzleManager.puzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
                    previous_face = iFace;
                    int gripSlices = puzzleManager.puzzleDescription.getNumSlicesForGrip(iGrip);
                    int slicemask = 1 << rand.nextInt(gripSlices);
                    int dir = rand.nextBoolean() ? -1 : 1;
                    // apply the twist to the history.
                    MagicCube.Stickerspec ss = new MagicCube.Stickerspec();
                    ss.id_within_puzzle = iGrip; // slamming new id. do we need to set the other members?
                    ss.face = puzzleManager.puzzleDescription.getGrip2Face()[iGrip];
                    hist.apply(ss, dir, slicemask);
                    //System.out.println("Adding scramble twist grip: " + iGrip + " dir: " + dir + " slicemask: " + slicemask);
                }
                hist.mark(History.MARK_SCRAMBLE_BOUNDARY);
                syncPuzzleStateWithHistory();
                boolean fully = scramblechenfrengensen == -1;
                scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL;
                setStatus(fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen == 1 ? "" : "s"));
                updateTwistsLabel();
                view.repaint();
            }
        } // end class Scrambler
        JMenuItem scrambleItem = null;
        Scrambler scrambler = null;
        for(int i = 1; i <= 8; i++) {
            scrambler = new Scrambler(i);
            scrambleItem = new JMenuItem("" + i);
            StaticUtils.addHotKey(KeyEvent.VK_0 + i, scrambleItem, "Scramble" + i, scrambler);
            scramblemenu.add(scrambleItem);
        }
        scramblemenu.addSeparator();
        scrambler = new Scrambler(-1);
        scrambleItem = new JMenuItem("Full     ");
        StaticUtils.addHotKey(KeyEvent.VK_F, scrambleItem, "Full", scrambler);
        scramblemenu.add(scrambleItem);

        // Puzzle lengths
        //
        JMenu puzzlemenu = new JMenu("Puzzle");

        // Macros
        //
        JMenu macromenu = new JMenu("Macros");
        JMenuItem item = new JMenuItem("Start/Stop Macro Definition");
        StaticUtils.addHotKey(KeyEvent.VK_M, item, "Macro", macro);
        macromenu.add(item);
        item = new JMenuItem("Cancel Macro Definition");
        StaticUtils.addHotKey(KeyEvent.VK_ESCAPE, item, "Cancel", cancel);
        macromenu.add(item);
        item = new JMenuItem("Begin Macro Setup Moves");
        StaticUtils.addHotKey(KeyEvent.VK_B, item, "Begin", setup);
        macromenu.add(item);
        item = new JMenuItem("Apply Last Macro");
        StaticUtils.addHotKey(KeyEvent.VK_A, item, "Last", last);
        macromenu.add(item);
        macromenu.add(new JMenuItem("Read Macro File...")).addActionListener(read);
        macromenu.add(new JMenuItem("Write Macro File")).addActionListener(write);
        macromenu.add(new JMenuItem("Write Macro File As...")).addActionListener(writeas);
        initMacroMenu(); // create controls for any macro definitions.

        // Help
        //
        JMenu helpmenu = new JMenu("Help");
        helpmenu.add(new JMenuItem("Frequently Asked Questions...")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Desktop.getDesktop().browse(new URI("http://superliminal.com/cube/faq.html"));
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
        helpmenu.add(new JMenuItem("About...")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Get the minor version from our resource.
                String minorVersion = ResourceUtils.readRelativeFile("version.txt");
                if(minorVersion == null) {
                    System.err.println("Couldn't read minor version number");
                    minorVersion = "";
                }
                else
                    minorVersion = "." + minorVersion;
                JOptionPane.showMessageDialog(MC4DSwing.this,
                    "<html><center><big>" +
                        MagicCube.TITLE +
                        " Version " + MagicCube.PUZZLE_MAJOR_VERSION + minorVersion +
                        "</big><br>Copyright 2005 by Melinda Green and Don Hatch</center>" +
                        "<br>With invaluable help from Jay Berkenbilt, Roice Nelson," +
                        "and the members of the MC4D mailing list." +
                        "<br>http://superliminal.com/cube/cube.htm" +
                        "</html>");
            }
        });
        final JCheckBox debug_checkbox = new PropControls.PropCheckBox("Debugging", MagicCube.DEBUGGING, false, helpmenu, "Whether to print diagnostic information to the console");
        debug_checkbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                History.setDebugging(debug_checkbox.isSelected());
                view.repaint();
            }
        });
        helpmenu.add(new JMenuItem("Debugging Console")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Console.show("MC4D Debugging Console");
                if(Console.getLineCount() <= 1 && !debug_checkbox.isSelected())
                    System.out.println("Output text and error messages are redirected here when this window is showing. \nYou'll probably need to also turn on Help > debugging to see much.");
            }
        });
        helpmenu.add(debug_checkbox);

        JMenuBar menubar = new JMenuBar();
        menubar.add(filemenu);
        menubar.add(editmenu);
        menubar.add(scramblemenu);
        menubar.add(puzzlemenu);
        menubar.add(macromenu);
        menubar.add(helpmenu);
        setJMenuBar(menubar);

        JPanel statusBar = new JPanel();
        statusBar.setBackground(this.getBackground());
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(statusLabel);
        progressBar.setStringPainted(true);
        statusBar.add(progressBar);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(twistLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10, 25))); // height is so view won't jump as progress bar is shown/hidden

        mainViewContainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // Layout top-level components
        //
        final JSplitPane splitter = new JSplitPane(
            /* orientation= */JSplitPane.HORIZONTAL_SPLIT,
            /* continuousLayout= */true,
            /* leftComponent= */mainTabsContainer,
            /* rightComponent= */mainViewContainer);
        splitter.setOneTouchExpandable(true);
        splitter.setDividerLocation(PropertyManager.getInt("divider", 300));
        splitter.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    PropertyManager.userprefs.setProperty("divider", "" + splitter.getDividerLocation());
                }
            });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(splitter, "Center");
        contentPane.add(statusBar, "South");

        puzzleManager = new PuzzleManager(MagicCube.DEFAULT_PUZZLE, MagicCube.DEFAULT_LENGTH, progressBar);
        puzzleManager.addPuzzleListener(new PuzzleManager.PuzzleListener() {
            @Override
            public void puzzleChanged(boolean newPuzzle) {
                initTabs(); // to properly enable/disable the buttons
                progressBar.setVisible(false);
                hist.clear((int) puzzleManager.puzzleDescription.getEdgeLength());
                updateTwistsLabel();
                Color[] userColors = findColors(
                    puzzleManager.puzzleDescription.getSchlafliProduct(),
                    puzzleManager.puzzleDescription.nFaces());
                if(userColors != null)
                    puzzleManager.faceColors = userColors;
                if(view != null)
                    view.repaint();
            }
        });
        puzzleManager.setHighlighter(normalHighlighter);
        initTabs(); // to show controls
        initPuzzleMenu(puzzlemenu, statusLabel, progressBar);
        initPuzzle(PropertyManager.top.getProperty("logfile"));

        // Do this after loading initial puzzle to avoid console spam while loading long log file.
        History.setDebugging(debug_checkbox.isSelected());
    } // end MC4DSwing

    private void initPuzzleMenu(JMenu puzzlemenu, final JLabel label, final JProgressBar progressView) {
        final String[][] table = MagicCube.SUPPORTED_PUZZLES;
        for(int i = 0; i < table.length; ++i) {
            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli == null ? table[i][2] :
                schlafli + "  " + table[i][2]);
            // Puzzles with triangles have been problematic.
            boolean allowPuzzlesWithTriangles = true;
            if(!allowPuzzlesWithTriangles) {
                if(schlafli != null && schlafli.indexOf("{3") != -1 && !schlafli.equals("{3,3,3}"))
                    continue;
            }
            JMenu submenu;
            if(schlafli != null) {
                submenu = new JMenu(name);
                puzzlemenu.add(submenu);
            }
            else
                submenu = puzzlemenu;
            for(int j = 0; j < lengthStrings.length; ++j) {
                final String lengthString = lengthStrings[j];
                submenu.add(new JMenuItem(schlafli == null ? name : "   " + lengthString + "  ")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        String newSchlafli = schlafli;
                        String newLengthString = lengthString;
                        if(schlafli == null) {
                            String prompt = "Enter your invention:";
                            String initialInput = puzzleManager.puzzleDescription.getSchlafliProduct() + " " + puzzleManager.getPrettyLength();
                            while(true) {
                                String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                if(reply == null) {
                                    return; // Canceled
                                }
                                String schlafliAndLength[] = reply.trim().split("\\s+");
                                if(schlafliAndLength.length != 2) {
                                    prompt = "Can not build your invention.\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length. Try again!";
                                    initialInput = reply;
                                    continue;
                                }
                                newSchlafli = schlafliAndLength[0];
                                newLengthString = schlafliAndLength[1];
                                break; // got it
                            }
                        }
                        progressView.setVisible(true);
                        System.out.println(newSchlafli + " " + newLengthString);
                        puzzleManager.initPuzzle(newSchlafli, newLengthString, progressView, label, true);
                        hist.clear((int) Double.parseDouble(newLengthString));
                        updateTwistsLabel();
                        scrambleState = SCRAMBLE_NONE;
                        cancel(); // To at least assure we start in normal mode.
                        view.repaint();
                    }
                });
            }
        }
    } // initPuzzleMenu


    /**
     * Called whenever macro list in manager changes to keep "Apply" submenu up-to-date.
     */
    private void initMacroMenu() {
        apply.removeAll();
        Macro macros[] = macroMgr.getMacros();
        for(int i = 0; i < macros.length; ++i) {
            final Macro m = macros[i];
            JMenuItem applyitem = apply.add(new JMenuItem(m.getName()));
            applyitem.addActionListener(new ProbableAction(m.getName()) {
                @Override
                public void doit(ActionEvent ae) {
                    lastMacro = m;
                    last.doit(ae);
                }
            });
        }
    }

    // TODO: All this state is part of the hackish way to keep macro buttons properly enabled below.
    // Maybe the macro controls listening for puzzle change events or something else would be better?
    private final PreferencesEditor preferencesEditor = new PreferencesEditor();
    private final JTabbedPane tabs = new JTabbedPane();
    private final ChangeListener tabListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent ce) {
            PropertyManager.userprefs.setProperty("lasttab", "" + tabs.getSelectedIndex());
        }
    };
    private final MacroControls macroControls = new MacroControls();
    private final MacroControls.Listener macroControlsListener = new MacroControls.Listener() {
        @Override
        public void apply(Macro m, boolean reverse) {
            lastMacro = m;
            final int mask = reverse ? ActionEvent.CTRL_MASK : 0;
            // A fake event so action will pick up correct direction.
            // A bit of a hack but sometimes a girl's gotta do what a girl's gotta do!
            ActionEvent ae = new ActionEvent(this, 0, "apply", mask) {
                @Override
                public int getID() {
                    return mask;
                }
            };
            last.doit(ae);
        }
        @Override
        public void changed() {
            initMacroMenu();
        }
    };

    private static int getTwistDirection(ActionEvent ae) {
        // we'd love to say (ae.getModifiers()&ActionEvent.SHIFT_MASK)!=0 but for Swing bug 6183805, so...
        // boolean modified = ae.getModifiers() != 0 && (ae.getModifiers()&ActionEvent.CTRL_MASK)==0;
        // but that's broken for Don, so...
        boolean modified = ae.getID() == ActionEvent.CTRL_MASK;
        return modified ? -1 : 1;
        // NOTE: This may be causing a problem when using slicemasks on Windows 10 or with Swedish keyboards
        // As reported by Joel Karlsson. He found that using <alt> instead of <ctrl> is a workaround.
    }

    private void initTabs() {
        String schlafli = puzzleManager != null && puzzleManager.puzzleDescription != null ? puzzleManager.puzzleDescription.getSchlafliProduct() : null;
        macroControls.init(macroMgr, schlafli, macroControlsListener);
        if(tabs.getComponentCount() > 0) // If we've already setup the tabs, we're done.
            return;
        tabs.removeChangeListener(tabListener); // so as not to pick up tab change events due to adding tabs.
        tabs.removeAll();
        tabs.add("Preferences", preferencesEditor);
        tabs.add("Macros", macroControls);
        tabs.setSelectedIndex(PropertyManager.getInt("lasttab", 0));
        tabs.addChangeListener(tabListener);
        mainTabsContainer.removeAll();
        mainTabsContainer.add(tabs);
        mainTabsContainer.validate();
    }

    private void initPuzzle(String log) {
        scrambleState = SCRAMBLE_NONE;
        double initial_edge_length = MagicCube.DEFAULT_LENGTH;
        int int_edge_length = (int) Math.ceil(initial_edge_length);
        if(hist != null) // Stop listening to last History.
            hist.setHistoryListener(null);
        hist = new History(int_edge_length);
        if(log != null) { // read the log file, possibly reinitializing length and history.
            File logfile = new File(log);
            if(logfile.exists()) {
                boolean parsingError = false;
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(logfile));
                    String firstlineStr = reader.readLine();
                    if(firstlineStr == null) {
                        reader.close();
                        throw new IOException("Empty log file.");
                    }
                    String firstline[] = firstlineStr.split(" ");
                    /*
                     * First Line Format:
                     * 0 - Magic Number
                     * 1 - File Version
                     * 2 - Scramble State
                     * 3 - Twist Count
                     * 4 - Schlafli Product
                     * 5 - Edge Length
                     */
                    if(firstline.length != 6 || !MagicCube.MAGIC_NUMBER.equals(firstline[0])) {
                        reader.close();
                        throw new IOException("Unexpected log file format.");
                    }
                    int readversion = Integer.parseInt(firstline[1]);
                    if(readversion != MagicCube.LOG_FILE_VERSION) {
                        setStatus("Incompatible log file version " + readversion, true);
                        reader.close();
                        return;
                    }
                    scrambleState = Integer.parseInt(firstline[2]);
                    // int numTwists = Integer.parseInt(firstline[3]);
                    String schlafli = firstline[4];
                    initial_edge_length = Double.parseDouble(firstline[5]);
                    puzzleManager.initPuzzle(schlafli, "" + initial_edge_length, progressBar, statusLabel, false);
                    int_edge_length = (int) Math.round(initial_edge_length);
                    hist = new History(int_edge_length);
                    String title = MagicCube.TITLE;
                    rotations.read(reader);
                    int c;
                    for(c = reader.read(); !(c == '*' || c == -1); c = reader.read())
                        ; // read past state data
                    if(hist.read(new PushbackReader(reader)))
                        title += " - " + logfile.getName();
                    else
                        System.err.println("Error reading puzzle history");
                    setTitle(title);
                } catch(Exception e) {
                    e.printStackTrace();
                    parsingError = true;
                }
                if(parsingError)
                    setStatus("Failed to parse log file '" + log + "'", true);
                else
                    setStatus("Read log file '" + log + "'");
            }
            else
                setStatus("Couldn't find log file '" + log + "'", true);
            updateTwistsLabel();
        } // end reading log file
        syncPuzzleStateWithHistory();
        if((scrambleState == SCRAMBLE_PARTIAL || scrambleState == SCRAMBLE_FULL) && puzzleManager.isSolved())
            scrambleState = SCRAMBLE_SOLVED; // Because log files from older MC4D versions didn't have the solved state.
        view = new MC4DView(puzzleManager, rotations);
        view.addStickerListener(stickerListener);
        view.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent arg0) {
                char c = arg0.getKeyChar();
                if(c == KeyEvent.VK_ESCAPE)
                    cancel();
                //if(Character.isDigit(c)) {
                //    MagicCube.TwistData toGoto = hist.goTo(c - '0');
                //    if(toGoto != null)
                //        view.animate(toGoto);
                //    else
                //        setStatus("Nothing to goto.", true);
                //}
            }
        });
        view.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mwe) {
                if(viewScaleModel != null && mwe.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    int
                    min = viewScaleModel.getMinimum(),
                    max = viewScaleModel.getMaximum(),
                    cur = viewScaleModel.getValue(),
                    newValue = (int) (cur + (max - min) / 100f * mwe.getWheelRotation());
                    //System.out.println("whee! " + " from " +  cur + " to " + newValue + " (" + min + "," + max + ")");
                    viewScaleModel.setValue(newValue);
                }
            }
        });
        hist.setHistoryListener(history_listener);
        updateEditMenuItems();
        mainViewContainer.removeAll();
        mainViewContainer.add(view, "Center");
    } // end initPuzzle

    /**
     * Watches puzzle changes for solutions and keeps related UI in sync.
     */
    private History.HistoryListener history_listener = new History.HistoryListener() {
        @Override
        public void currentChanged() {
            updateEditMenuItems();
            updateTwistsLabel();
            if((scrambleState == SCRAMBLE_PARTIAL || scrambleState == SCRAMBLE_FULL) && puzzleManager.isSolved()) {
                int intlen = (int) puzzleManager.puzzleDescription.getEdgeLength();
                if(intlen <= 1)
                    return; // No soup for you!
                switch(scrambleState) {
                    case SCRAMBLE_PARTIAL:
                        scrambleState = SCRAMBLE_SOLVED; // Credit the user for solving.
                        // TIP: To help debug full solution handling, comment out these lines 
                        // including the break statement and then solve a single random twist. 
                        setStatus("Solved!");
                        if(PropertyManager.getBoolean(MagicCube.BLINDFOLD, false))
                            PropertyManager.userprefs.setProperty(MagicCube.BLINDFOLD, "" + false);
                        Audio.play(Audio.Sound.CORRECT); // Just a little "attaboy" sound.
                        break;
                    case SCRAMBLE_FULL:
                        scrambleState = SCRAMBLE_SOLVED; // Credit the user for solving.
                        setStatus("Solved!");
                        if(PropertyManager.getBoolean(MagicCube.BLINDFOLD, false))
                            PropertyManager.userprefs.setProperty(MagicCube.BLINDFOLD, "" + false);
                        String puzzle = puzzleManager.puzzleDescription.getSchlafliProduct() + intlen;
                        int previous_full_solves = PropertyManager.getInt("full" + puzzle, 0);
                        PropertyManager.userprefs.setProperty("full" + puzzle, "" + (previous_full_solves + 1)); // Remember solved puzzles.
                        int min_scrambles = puzzleManager.twistsNeededToFullyScramble();
                        if(previous_full_solves > 0 || min_scrambles < MagicCube.MIN_SCRAMBLE_TWISTS_FOR_FANFARE) {
                            // Only a small reward since the user has already solved this puzzle or it's too easy.
                            Audio.play(Audio.Sound.CORRECT);
                            break;
                        }
                        // A really flashy reward for difficult first-time solutions.
                        Congratulations congrats = new Congratulations(
                            "<html>" +
                                "<center><H1>You have solved the " + puzzle + "!</H1></center>" +
                                "<br>You may want to use File > Save As to archive your solution, then copy it somewhere safe." +
                                "<br>If this is a first for you or it is a record, consider submitting it via" +
                                "<br>http://superliminal.com/cube/halloffame.htm" +
                                "</html>");
                        congrats.setVisible(true);
                        congrats.start();
                        break;
                }
            } // end if(isSolved())
        }
    }; // end HistoryListener impl

    private void syncPuzzleStateWithHistory() {
        MagicCube.TwistData[] moves = hist.movesArray();
        try {
            puzzleManager.resetPuzzleStateNoEvent();
            for(MagicCube.TwistData move : moves) {
                if(move.grip.id_within_puzzle == -1) {
                    System.err.println("Bad move in MC4DSwing.syncPuzzleStateWithHistory: " + move.grip.id_within_puzzle);
                    return;
                }
                puzzleManager.puzzleDescription.applyTwistToState(
                    puzzleManager.puzzleState,
                    move.grip.id_within_puzzle,
                    move.direction,
                    move.slicemask);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Editor for user preferences.
     */
    private class PreferencesEditor extends JPanel {
        public PreferencesEditor() {
            init();
        }

        // Wrapper class that left-aligns a component when added to a BoxLayout.
        private class LeftAlignedRow extends JPanel {
            public LeftAlignedRow(JComponent comp, int indent) {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(Box.createRigidArea(new Dimension(indent, 0)));
                add(comp);
                add(Box.createHorizontalGlue());
            }
            public LeftAlignedRow(JComponent comp) {
                this(comp, 0);
            }
        }
        private void init() {
            // a component that forwards all repaint calls to the current view object.
            Component repainter = new Component() {
                @Override
                public void repaint() {
                    if(view != null)
                        view.repaint();
                }
            };
            ColorChangeListener color_repainter = new ColorChangeListener() {
                @Override
                public void colorChanged(Color newColor) {
                    view.repaint();
                }
            };
            removeAll();
            final JRadioButton ctrlRotateByFace = new PropControls.PropRadioButton("by Face", "ctrlrotbyface", true, false, repainter, "Control-click will rotate clicked face to the center");
            final JRadioButton ctrlRotateByCubie = new PropControls.PropRadioButton("by Cubie", "ctrlrotbyface", false, true, repainter, "Control-click will rotate clicked cubie to the center");
            ButtonGroup ctrlRotateGroup = new ButtonGroup();
            ctrlRotateGroup.add(ctrlRotateByFace);
            ctrlRotateGroup.add(ctrlRotateByCubie);
            JLabel ctrlClickLabel = new JLabel("Ctrl-Click Rotates:");
            JPanel rotateMode = new JPanel();
            rotateMode.setLayout(new BoxLayout(rotateMode, BoxLayout.X_AXIS));
            rotateMode.add(ctrlClickLabel);
            rotateMode.add(Box.createHorizontalGlue());
            final JCheckBox blindfoldbox = new PropCheckBox("Blindfold", MagicCube.BLINDFOLD, false, repainter, "Whether to gray out sticker colors (Ctrl+D)");
            StaticUtils.addHotKey(KeyEvent.VK_D, blindfoldbox, MagicCube.BLINDFOLD, new ProbableAction("Blind") {
                @Override
                public void doit(ActionEvent ae) {
                    boolean is_checked = PropertyManager.getBoolean(MagicCube.BLINDFOLD, false);
                    PropertyManager.userprefs.setProperty(MagicCube.BLINDFOLD, !is_checked + "");
                    view.repaint();
                }
            });
            class MyLabel extends JLabel {
                public MyLabel(String text, String tooltip) {
                    super(text);
                    setPreferredSize(new Dimension(100, super.getPreferredSize().height));
                    setToolTipText(tooltip);
                }
            }
            JPanel sliders = new JPanel(new SpringLayout());
            sliders.setBorder(new TitledBorder("Adjustments"));
            sliders.add(new MyLabel("Twist Speed", "Tip: Adjust during long Edit > Solve or Edit > Play"));
            sliders.add(new PropSlider("twistfactor", repainter, 1, .05f, 5, "Speed of twisting animation"));
            sliders.add(new MyLabel("Drag Speed", "Amount of rotation per mouse motion"));
            sliders.add(new PropSlider("dragfactor", repainter, 1, .05f, 5, "Amount of rotation per drag distance"));
            sliders.add(new MyLabel("View Scale", "Tip: Also controlled by mouse wheel"));
            JSlider scaler = new PropSlider("scale", repainter, 1, .1f, 5, "Size of puzzle in window");
            viewScaleModel = scaler.getModel();
            sliders.add(scaler);
            sliders.add(new MyLabel("Face Shrink", "Space between faces"));
            sliders.add(new PropSlider("faceshrink", repainter, MagicCube.FACESHRINK, .1f, 1.5f, "Size of faces within puzzle"));
            sliders.add(new MyLabel("Sticker Shrink", "Space between stickers within faces"));
            sliders.add(new PropSlider("stickershrink", repainter, MagicCube.STICKERSHRINK, .1f, 1.5f, "Size of stickers within faces"));
            sliders.add(new MyLabel("Eye W Scale", "Distance between puzzle and eye in the 4th dimension"));
            sliders.add(new PropSlider("eyew", repainter, MagicCube.EYEW, .75f, 4, "Focal length of 4D camera"));
            SpringUtilities.makeCompactGrid(sliders, 6, 2, 0, 0, 0, 0);
            JPanel modes = new JPanel();
            modes.setLayout(new BoxLayout(modes, BoxLayout.Y_AXIS));
            modes.add(new LeftAlignedRow(new PropCheckBox("Show Shadows", "shadows", true, repainter, null)));
            modes.add(new LeftAlignedRow(new PropCheckBox("Allow Auto-Rotation", "autorotate", true, repainter, "Whether to keep spinning if mouse released while dragging")));
            modes.add(new LeftAlignedRow(new PropCheckBox("Highlight by Cubie", "highlightbycubie", false, repainter, "Whether to highlight all stickers of hovered piece or just the hovered sticker")));
            modes.add(new LeftAlignedRow(new PropCheckBox("Allow Antialiasing", "antialiasing", true, repainter, "Whether to smooth polygon edges when still - Warning: Can be expensive on large puzzles")));
            modes.add(new LeftAlignedRow(new PropCheckBox("Mute Sound Effects", MagicCube.MUTED, false, repainter, "Whether to allow sound effects")));
            modes.add(new LeftAlignedRow(blindfoldbox));
            final PropCheckBox quick = new PropCheckBox("Quick Moves:", "quickmoves", false, repainter, "Whether to skip some or all twist animation");
            modes.add(new LeftAlignedRow(quick));
            final JRadioButton allMoves = new PropRadioButton("All Moves", "quickmacros", false, true, repainter, "No twist animations at all");
            final JRadioButton justMacros = new PropRadioButton("Just Macros", "quickmacros", true, false, repainter, "No twist animations for macro sequences");
            allMoves.setEnabled(PropertyManager.getBoolean("quickmoves", false));
            justMacros.setEnabled(PropertyManager.getBoolean("quickmoves", false));
            ButtonGroup quickGroup = new ButtonGroup();
            quickGroup.add(allMoves);
            quickGroup.add(justMacros);
            quick.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    allMoves.setEnabled(quick.isSelected());
                    justMacros.setEnabled(quick.isSelected());
                }
            });
            final int indent = 50;
            modes.add(new LeftAlignedRow(allMoves, indent));
            modes.add(new LeftAlignedRow(justMacros, indent));
            modes.add(rotateMode);
            modes.add(new LeftAlignedRow(ctrlRotateByFace, indent));
            modes.add(new LeftAlignedRow(ctrlRotateByCubie, indent));
            //modes.add(contigiousCubies); // Uncomment when we can make it work immediately and correctly.
            // color controls
            ColorButton skyColor = new ColorButton("Sky", "sky.color", MagicCube.SKY, color_repainter, true);
            ColorButton ground = new ColorButton("Ground", "ground.color", MagicCube.GROUND, color_repainter, true);
            JCheckBox drawGround = new PropCheckBox("Draw Ground", "ground", true, repainter, "Whether to draw a ground plane");
            JCheckBox drawOutlines = new PropCheckBox("Draw Outlines", "outlines", false, repainter, "Whether to draw sticker edges");
            ColorButton outlinesColor = new ColorButton("Outlines", "outlines.color", Color.BLACK, color_repainter, true);
            JPanel colors = new JPanel(new SpringLayout());
            colors.add(new JLabel());
            colors.add(skyColor);
            colors.add(drawGround);
            colors.add(ground);
            colors.add(drawOutlines);
            colors.add(outlinesColor);
            SpringUtilities.makeCompactGrid(colors, 3, 2, 0, 0, 10, 4);
            JButton reset_button = new JButton("Reset To Defaults");
            reset_button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    // TODO: make this work. Problem is syncing the view with the reset prefs
                    // without losing the user's history state. Possible if view gets all prefs
                    // from the property manager rather than via setters. Otherwise this is tricky to do right.
                    PropertyManager.userprefs.clear();
                    String fname = logFileChooser.getSelectedFile() == null ? null : logFileChooser.getSelectedFile().getAbsolutePath();
                    initPuzzle(fname);
                    scrambleState = SCRAMBLE_NONE;
                    init(); // to sync the controls with the default prefs.
                    mainViewContainer.validate();
                    view.repaint();
                }
            });
            modes.setBorder(new TitledBorder("Modes"));
            setPreferredSize(new Dimension(200, 600));
            // layout the top-level components
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(sliders);
            add(modes);
            JPanel colors_holder = new JPanel();
            colors_holder.setBorder(new TitledBorder("Colors"));
            colors_holder.setLayout(new BoxLayout(colors_holder, BoxLayout.X_AXIS));
            colors_holder.add(colors);
            colors_holder.add(Box.createHorizontalGlue());
            add(colors_holder);
            add(Box.createVerticalGlue());
            //JPanel tmp = new JPanel();
            //tmp.add(reset); // commented out until we can make this work well
            //add(tmp, "South");
        } // end init()
    } // end class PreferencesEditor


    private static Color[][] readColorLists(String fname) {
        URL furl = ResourceUtils.getResource(fname);
        if(furl == null)
            return new Color[0][];
        String contents = ResourceUtils.readFileFromURL(furl);
        //JOptionPane.showMessageDialog(null, contents);
        if(contents == null)
            return new Color[0][];
        ArrayList<Color[]> colorlines = new ArrayList<Color[]>();
        try {
            BufferedReader br = new BufferedReader(new StringReader(contents));
            for(String line = br.readLine(); line != null;) {
                StringTokenizer st = new StringTokenizer(line);
                Color[] colorlist = new Color[st.countTokens()];
                for(int i = 0; i < colorlist.length; i++) {
                    String colstr = st.nextToken();
                    colorlist[i] = PropertyManager.parseColor(colstr);
                    if(colorlist[i] == null) {
                        colorlist = null;
                        break; // bad line
                    }
                }
                if(colorlist != null)
                    colorlines.add(colorlist);
                line = br.readLine();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return colorlines.toArray(new Color[0][]);
    } // end readColorLists()

    private static String colorFilename(String schlafli)
    {
        return "facecolors" + File.separator + schlafli + ".txt";
    }

    private static Color[] findColors(String schlafli, int len)
    {
        String filename = colorFilename(schlafli);
        Color[] colors = findColors(len, filename);
        if(colors != null)
            return colors;

        return findColors(len, MagicCube.FACE_COLORS_FILE);
    }

    private static Color[] findColors(int len, String fname) {
        for(Color[] cols : readColorLists(fname)) {
            if(cols.length == len)
                return cols;
        }
        return null;
    }


    /**
     * Main entry point for the MagicCube4D application.
     * 
     * @param args may contain override arguments for any in "defaults.prop"
     *        by prefixing the keys with a '-' character. Arguments without following
     *        values are assumed to be boolean flags and will be set to "true".
     */
    public static void main(final String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("version " + System.getProperty("java.version"));
                PropertyManager.loadProps(args, PropertyManager.top);
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                final JFrame frame = new MC4DSwing();
                configureNormal(frame);
                frame.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent ce) {
                        if(frame.getExtendedState() != Frame.NORMAL)
                            return;
                        PropertyManager.userprefs.setProperty("window.width", "" + frame.getWidth());
                        PropertyManager.userprefs.setProperty("window.height", "" + frame.getHeight());
                    }
                    @Override
                    public void componentMoved(ComponentEvent ce) {
                        if(frame.getExtendedState() != Frame.NORMAL)
                            return;
                        PropertyManager.userprefs.setProperty("window.x", "" + frame.getX());
                        PropertyManager.userprefs.setProperty("window.y", "" + frame.getY());
                    }
                });
                frame.setExtendedState(PropertyManager.getInt("window.state", frame.getExtendedState()));
                frame.addWindowStateListener(new WindowStateListener() {
                    @Override
                    public void windowStateChanged(WindowEvent we) {
                        int state = frame.getExtendedState();
                        state &= ~Frame.ICONIFIED; // disallows saving in iconified state
                        PropertyManager.userprefs.setProperty("window.state", "" + state);
                        if(state == Frame.NORMAL) {
                            configureNormal(frame);
                        }
                    }
                });
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }

            void configureNormal(Frame frame) {
                frame.setSize(
                    PropertyManager.getInt("window.width", MagicCube.DEFAULT_WINDOW_WIDTH),
                    PropertyManager.getInt("window.height", MagicCube.DEFAULT_WINDOW_HEIGHT));
                frame.setLocation(
                    PropertyManager.getInt("window.x", frame.getX()),
                    PropertyManager.getInt("window.y", frame.getY()));
            }
        });
    } // end main()
}
