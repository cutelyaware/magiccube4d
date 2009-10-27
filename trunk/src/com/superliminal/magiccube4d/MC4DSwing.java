package com.superliminal.magiccube4d;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.superliminal.util.ColorButton;
import com.superliminal.util.FloatSlider;
import com.superliminal.util.PropertyManager;
import com.superliminal.util.StaticUtils;
import com.superliminal.util.Util;

import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;

/**
 * The main desktop application.
 * The main method here creates and shows an instance of this class.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DSwing extends JFrame implements MC4DView.StickerListener {

    private final static int
        SCRAMBLE_NONE = 0, 
        SCRAMBLE_PARTIAL = 1,
        SCRAMBLE_FULL = 2;
    private int scrambleState = SCRAMBLE_NONE;

    // macro state
    private MacroManager macroMgr = new MacroManager(PropertyManager.top.getProperty("macrofile", 
    			StaticUtils.getHomeDir() + File.separator + "MC4D.macros"));
    private Macro lastMacro;
    private int applyingMacro; // -1 == reversed, 0 == not, 1 == forward
    
    // These are the variables that will be (re)created in initPuzzle().
    private History hist;
    private MC4DView view;
    private BoundedRangeModel viewScaleModel;
    
    private RotationHandler rotations = new RotationHandler();
    
    private JPanel viewcontainer = new JPanel(new BorderLayout()); // JComponent container so we can use addHotKey
    private JPanel macroControlsContainer = new JPanel(new BorderLayout());
    private JFileChooser
        logFileChooser = new JFileChooser(),
        macroFileChooser = new JFileChooser();
    private JLabel 
        twistLabel  = new JLabel(),
        statusLabel = new JLabel();
    private JProgressBar progressBar = new JProgressBar();
    
    private PuzzleManager puzzleManager = null;

    private JMenu apply = new JMenu("Apply");
    private JMenuItem
        openitem    = new JMenuItem("Open"),
        saveitem    = new JMenuItem("Save"),
        saveasitem  = new JMenuItem("Save As..."),
        quititem    = new JMenuItem("Quit"),
        resetitem   = new JMenuItem("Reset"),
        undoitem    = new JMenuItem("Undo"),
        redoitem    = new JMenuItem("Redo"),
        cheatitem   = new JMenuItem("Solve (Cheat)"),
        solveitem   = new JMenuItem("Solve (For Real)");
    
    private void updateTwistsLabel() {
    	int twists = hist.countTwists();
        twistLabel.setText("Total Twists: " + twists);
    }
     
    PuzzleManager.Highlighter normalHighlighter = new PuzzleManager.Highlighter() {
		@Override
		public boolean shouldHighlightSticker(PuzzleDescription puzzle, int stickerIndex, int gripIndex, int slicemask, int x, int y) {
			if(view.isCtrlKeyDown())
				return puzzleManager.canRotateToCenter(x, y, rotations);
			else
				return PipelineUtils.hasValidTwist( gripIndex, slicemask, puzzleManager.puzzleDescription );
		}
    };


    /*
     * Format: 
     * 0 - Magic Number 
     * 1 - File Version 
     * 2 - Scramble State
     * 3 - Twist Count
     * 4 - Schlafli Product
     * 5 - Edge Length
     */
    private void saveAs(String log) {
    	if(log == null) {
    		System.err.println("saveAs: null file name");
    		return;
    	}
        File file = new File(log);
        String sep = System.getProperty("line.separator");
        try {
            Writer writer = new FileWriter(file);
            writer.write(
                MagicCube.MAGIC_NUMBER + " " +
                MagicCube.LOG_FILE_VERSION + " " +
                scrambleState + " " +
                hist.countTwists() + " " +
                puzzleManager.puzzleDescription.getSchlafliProduct() + " " +
                puzzleManager.getPrettyLength());
            writer.write(sep);
            view.getRotations().write(writer);
            //writer.write(sep + puzzle.toString());
            writer.write("*" + sep);
            hist.write(writer);
            writer.close();
            String filepath = file.getAbsolutePath();
            statusLabel.setText("Wrote log file " + filepath);
            PropertyManager.userprefs.setProperty("logfile", filepath);
            setTitle(MagicCube.TITLE + " - " + file.getName());
        } catch(IOException ioe) {
            statusLabel.setText("Save to '" + log + "' failed. Consider using 'File > Save As' to save somewhere else.");
        }
    }

    
    /**
     * Like a regular AbstractAction but one which is disabled if the view is animating.
     * Users implement the "doit" method instead of actionPerformed which calls it.
     */
    private abstract class ProbableAction extends AbstractAction {
        protected ProbableAction(String name) {
            super(name);
        }
        public abstract void doit(ActionEvent ae);
        public final void actionPerformed(ActionEvent ae) {
            if(view.isAnimating())
                return;
            doit(ae);
        }
    }

    // actions which are only be enabled when not animating.
    //
    private ProbableAction
        open = new ProbableAction("Open") {
            @Override
			public void doit(ActionEvent ae) {
                if(logFileChooser.showOpenDialog(MC4DSwing.this) == JFileChooser.APPROVE_OPTION) {
                    String filepath = logFileChooser.getSelectedFile().getAbsolutePath();
                    PropertyManager.userprefs.setProperty("logfile", filepath);
                    initPuzzle(filepath);
                    statusLabel.setText("Read log file " + filepath);
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
                saveAs(PropertyManager.top.getProperty("logfile", fname));
            }
        },
        saveas = new ProbableAction("Save As...") {
            @Override
			public void doit(ActionEvent ae) {
                if(logFileChooser.showSaveDialog(MC4DSwing.this) == JFileChooser.APPROVE_OPTION) {
                	String path = logFileChooser.getSelectedFile().getAbsolutePath();
                	if( !path.endsWith(".log")) {
                		path += ".log";
                	}
                    saveAs(path);
                }
            }
        },
        undo = new ProbableAction("Undo") {
            @Override
			public void doit(ActionEvent ae) {
                statusLabel.setText("");
                if(hist.atScrambleBoundary()) {
                    statusLabel.setText("Can't undo past scramble boundary.");
                    return;
                }
                if(hist.atMacroClose()) {
                    statusLabel.setText("undoing macro");
                    for(MagicCube.TwistData toUndo=hist.undo(); toUndo!=null; toUndo=hist.undo()) {
                        view.animate(toUndo, false);
                        if(hist.atMacroOpen())
                            break;
                    }
                    assert(hist.atMacroOpen());
                    hist.goToPrevious(); // step over mark to point where macro was applied.
                }
                else {
                    MagicCube.TwistData toUndo = hist.undo();
                    if(toUndo != null) {
                    	//System.out.println("Undoing grip: " + toUndo.grip + " dir: " + toUndo.direction  + " slicemask: " + toUndo.slicemask);
                        view.animate(toUndo, false);
                    }
                    else
                        statusLabel.setText("Nothing to undo.");
                }
            }
        },
        redo = new ProbableAction("Redo") {
            @Override
			public void doit(ActionEvent ae) {
                statusLabel.setText("");
                if(hist.atMacroOpen()) {
                    statusLabel.setText("redoing macro");
                    for(MagicCube.TwistData toRedo=hist.redo(); toRedo!=null; toRedo=hist.redo()) {
                        view.animate(toRedo, false);
                        if(hist.atMacroClose())
                            break;
                    }
                    assert(hist.atMacroClose());
                    hist.goToNext(); // step over mark to point where macro is completed.
                }
                else {
                    MagicCube.TwistData toRedo = hist.redo();
                    if(toRedo != null)
                        view.animate(toRedo, false);
                    else
                        statusLabel.setText("Nothing to redo.");
                }
            }
        },
        cheat = new ProbableAction("Cheat") {
            @Override
			public void doit(ActionEvent ae) {
                scrambleState = SCRAMBLE_NONE; // no user credit for automatic solutions.
                // TODO: extend compress to work with non cubes.
                //hist.compress(false); // so fewest moves are required and solution least resembles original moves.
                Stack<MagicCube.TwistData> toundo = new Stack<MagicCube.TwistData>();
                for(Enumeration<MagicCube.TwistData> moves=hist.moves(); moves.hasMoreElements(); )
                    toundo.push(moves.nextElement());
                while( ! toundo.isEmpty()) {
                    MagicCube.TwistData last = toundo.pop();
                    MagicCube.TwistData inv = new MagicCube.TwistData(last.grip, -last.direction, last.slicemask);
                    //System.out.println("Cheating grip: " + inv.grip.id_within_puzzle + " dir: " + inv.direction  + " slicemask: " + inv.slicemask);
                    view.animate(inv, true);
                }
                statusLabel.setText("");
            }
        },
        solve = new ProbableAction("Solve") {
            @Override
			public void doit(ActionEvent ae) {
//                MagicCube.TwistData[] solution;
//                try { solution = puzzle.solve(); }
//                catch(Error e) {
//                    statusLabel.setText("no solution");
//                    return;
//                }
//                solution = History.compress(solution, (int)puzzleManager.puzzleDescription.getEdgeLength(), true);
//                view.animate(solution, true);
//                scrambleState = SCRAMBLE_NONE; // no user credit for automatic solutions.
//                statusLabel.setText("twists to solve = " + solution.length);
            }
        },
        macro = new ProbableAction("Start/End") { // toggles macro definition start/end
            @Override
			public void doit(ActionEvent ae) {
                if(macroMgr.isOpen())  { // finished with macro definition
                    String name = JOptionPane.showInputDialog("Name your macro");
                    if(name == null) {
                        macroMgr.cancel();
                        statusLabel.setText("");
                    }
                    else {
                        lastMacro = macroMgr.close(name);
                        initMacroMenu(); // to show new macro.
                        initMacroControls(); // to show new control
                        statusLabel.setText("Defined \"" + lastMacro.getName() + "\" macro with " +
                            lastMacro.length() + " move" + (lastMacro.length()==1 ? "." : "s."));
                    }
                    setSkyAndHighlighting(null, normalHighlighter);
                } else { // begin macro definition
                    macroMgr.open();
                    statusLabel.setText("Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                    setSkyAndHighlighting(Color.white, macroMgr);
                }
            }
        },
        cancel = new ProbableAction("Cancel Macro Definition") {
            @Override
			public void doit(ActionEvent ae) {
                view.cancelAnimation(); // also stops any animation
                if( ! macroMgr.isOpen())
                    return;
                macroMgr.cancel();
                statusLabel.setText("Cancelled");
                setSkyAndHighlighting(null, normalHighlighter);
                applyingMacro = 0;
            }
        },
        last = new ProbableAction("Apply Last Macro") {
            @Override
			public void doit(ActionEvent ae) {
                if(macroMgr.isOpen()) {
                    System.err.println("Warning: Macro already open.");
                    return;
                }
                if(lastMacro == null) {
                    System.err.println("Warning: No last macro to apply.");
                    return;
                }
                macroMgr.open();
                // we'd love to say (ae.getModifiers()&ActionEvent.SHIFT_MASK)!=0 but for Swing bug 6183805, so...
                // boolean modified = ae.getModifiers() != 0 && (ae.getModifiers()&ActionEvent.CTRL_MASK)==0;
                // but that's broken for Don, so...
                boolean modified = ae.getID() == ActionEvent.CTRL_MASK;
                applyingMacro = modified? -1 : 1;
                statusLabel.setText("Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                setSkyAndHighlighting(new Color(255, 170, 170), macroMgr);
            }
        };

    private PuzzleManager.Callback puzzleConfigurator = new PuzzleManager.Callback() {
    	public void call() {
    		initMacroControls(); // to properly enable/disable the buttons
    		progressBar.setVisible(false);
    		Color[] userColors = findColors(puzzleManager.puzzleDescription.nFaces(), MagicCube.FACE_COLORS_FILE);
    		if(userColors != null)
    			puzzleManager.faceColors = userColors;
    		view.repaint();
    	}
    };
    
    private void setSkyAndHighlighting( Color c, PuzzleManager.Highlighter h )
    {
    	view.setSkyOverride( c );
    	puzzleManager.setHighlighter( h );
    	view.updateStickerHighlighting();
    }

    // those actions which *can* be realistically performed while animations are playing
    //
    private Action
        quit = new AbstractAction("Quit") {
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        },
        // Resets the current puzzle and calls a callback when finished 
        // if one is supplied in the action event.
        reset = new AbstractAction("Reset") {
            public void actionPerformed(final ActionEvent ae) {
                scrambleState = SCRAMBLE_NONE; // do first to avoid issue 64 (fanfare on reset).
                cancel.doit(ae);
                progressBar.setVisible(true);
                final double length = puzzleManager.puzzleDescription.getEdgeLength();
                puzzleManager.initPuzzle(
            		puzzleManager.puzzleDescription.getSchlafliProduct(),
            		""+length, progressBar, statusLabel, true, 
            		new PuzzleManager.Callback() {
            	    	public void call() {
                            hist.clear((int)length);
                            scrambleState = SCRAMBLE_NONE; // probably redundant but shouldn't hurt.
                            updateTwistsLabel();
                            view.repaint();
                            if(ae.getSource() instanceof PuzzleManager.Callback)
                            {
                            	((PuzzleManager.Callback)ae.getSource()).call();
                            }
            	    	}
            	    }
            	);
            }
        },
        read = new AbstractAction("Read") {
            public void actionPerformed(ActionEvent ae) {
                if(macroFileChooser.showOpenDialog(MC4DSwing.this) != JFileChooser.APPROVE_OPTION)
                    return;
                File file = macroFileChooser.getSelectedFile();
                String filepath;
                try { filepath = file.getCanonicalPath(); }
                catch (IOException e) {
                    statusLabel.setText("Couldn't read macro file: " + file.getAbsolutePath());
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", filepath);
                macroMgr = new MacroManager(filepath);
                initMacroMenu(); // update controls with macro definitions just read.
                initMacroControls(); // to show new controls
                int nread = apply.getItemCount();
                statusLabel.setText("Read " + nread + " macro" + (nread==1?"":"s") + " from " + filepath);
            }
        },
        write = new AbstractAction("Write") {
            public void actionPerformed(ActionEvent ae) {
                try { macroMgr.write();
                } catch (IOException e) {
                    statusLabel.setText("Couldn't write to macro file " + macroMgr.getFilePath());
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", macroMgr.getFilePath());
                statusLabel.setText("Wrote macro file " + macroMgr.getFilePath());
            }
        },
        writeas = new AbstractAction("Write As") {
            public void actionPerformed(ActionEvent ae) {
                if(macroFileChooser.showSaveDialog(MC4DSwing.this) != JFileChooser.APPROVE_OPTION)
                    return;
                String filepath = macroFileChooser.getSelectedFile().getAbsolutePath();
                macroMgr.setFilePath(filepath);
                try { macroMgr.write();
                } catch (IOException e) {
                    statusLabel.setText("Couldn't write to macro file " + macroMgr.getFilePath());
                    return;
                }
                PropertyManager.userprefs.setProperty("macrofile", macroMgr.getFilePath());
                statusLabel.setText("Wrote macro file " + macroMgr.getFilePath());
            }
        };
        
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

        // set accelerator keys for some menu actions
        StaticUtils.addHotKey(KeyEvent.VK_R, resetitem, "Reset", reset);
        StaticUtils.addHotKey(KeyEvent.VK_Z, undoitem, "Undo", undo);
        StaticUtils.addHotKey(KeyEvent.VK_V, redoitem, "Redo", redo);
        StaticUtils.addHotKey(KeyEvent.VK_O, openitem, "Open", open);
        StaticUtils.addHotKey(KeyEvent.VK_S, saveitem, "Save", save);
        StaticUtils.addHotKey(KeyEvent.VK_Q, quititem, "Quit", quit);
        StaticUtils.addHotKey(KeyEvent.VK_L, solveitem, "Real", solve);
        StaticUtils.addHotKey(KeyEvent.VK_T, cheatitem, "Cheat", cheat);
        
        // accelerator keys from some non-menu actions
        StaticUtils.addHotKey(KeyEvent.VK_M, viewcontainer, "Macro", macro);
        StaticUtils.addHotKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), viewcontainer, "Cancel", cancel);
        StaticUtils.addHotKey(KeyEvent.VK_A, viewcontainer, "Apply", last);
        
        saveasitem.addActionListener(saveas); // no hotkey

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
        editmenu.addSeparator();
        editmenu.add(cheatitem);
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
            // This becomes a little crazy but bear with me...
            // This action runs in the background using a ProgressManager to keep the progress bar updated
            // during long scrambles of big puzzles. Problem is that it needs to first reset the puzzle
            // which itself wants to run in the background. We therefore send a callback object the reset action 
            // which it calls when the reset is finished. That callback then kicks off the scrambling action.
            // Chaining of background tasks like this is risky but it seems to work well. If ever it becomes
            // suspect, try changing the reset method to not run in the background (change it's progress manager
            // flag) then unpackage the call method to perform it's work directly after the reset finishes.
            public void doit(ActionEvent e) {
                progressBar.setVisible(true);
                reset.actionPerformed(new ActionEvent(new PuzzleManager.Callback(){
					@Override
					public void call() { // will be called by the reset action when it completes.
						scrambleState = SCRAMBLE_NONE; // do first to avoid issue 62 (fanfare on scramble).
		                progressBar.setVisible(true);
		                new ProgressManager(progressBar) {
							@Override
							protected Void doInBackground() throws Exception {
				                int previous_face = -1;
				                int totalTwistsNeededToFullyScramble = 
				                		puzzleManager.puzzleDescription.nFaces() // needed twists is proportional to nFaces
				                		* (int)puzzleManager.puzzleDescription.getEdgeLength() // and to number of slices
				                		* 2; // and to a fudge factor that brings the 3^4 close to the original 40 twists.
				                int scrambleTwists = scramblechenfrengensen == -1 ? totalTwistsNeededToFullyScramble : scramblechenfrengensen;
								Random rand = new Random();
								init("Scrambling", scrambleTwists);
				                for(int s = 0; s < scrambleTwists; s++) {
				                    // select a random grip that is unrelated to the last one (if any)
				                    int iGrip, iFace, order;
				                    do {
				                        iGrip = rand.nextInt(puzzleManager.puzzleDescription.nGrips());
				                        iFace = puzzleManager.puzzleDescription.getGrip2Face()[iGrip];
				                        order = puzzleManager.puzzleDescription.getGripSymmetryOrders()[iGrip];
				                    }
				                    while (
				                        order < 2 || // don't use 360 degree twists
				                        iFace == previous_face || // mixing it up
				                        (previous_face!=-1 && puzzleManager.puzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
				                    previous_face = iFace;
				                    int gripSlices = puzzleManager.puzzleDescription.getNumSlicesForGrip(iGrip);
				                    int slicemask = 1<<rand.nextInt(gripSlices);
				                    int dir = rand.nextBoolean() ? -1 : 1;
				                    // apply the twist to the puzzle state.
				                    puzzleManager.puzzleDescription.applyTwistToState(
				                    		puzzleManager.puzzleState,
				                            iGrip,
				                            dir,
				                            slicemask);
				                    // and save it in the history.
				                    MagicCube.Stickerspec ss = new MagicCube.Stickerspec();
				                    ss.id_within_puzzle = iGrip; // slamming new id. do we need to set the other members?
				                    ss.face = puzzleManager.puzzleDescription.getGrip2Face()[iGrip];
				                    hist.apply(ss, dir, slicemask);
				                    updateProgress(s);
				                	//System.out.println("Adding scramble twist grip: " + iGrip + " dir: " + dir + " slicemask: " + slicemask);
				                }
				
								// TODO Auto-generated method stub
								return null;
							} // end doInBackground

							@Override
							public void done() {
				                hist.mark(History.MARK_SCRAMBLE_BOUNDARY);
				                view.repaint();
				                boolean fully = scramblechenfrengensen == -1;
				                scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL;
				                statusLabel.setText(fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen==1?"":"s"));
	                            updateTwistsLabel();
	                            super.done();
							}
				        }.execute();
					}
				}, 0, "callback"));
            }
        }
        JMenuItem scrambleItem = null;
        Scrambler scrambler = null;
        for(int i=1; i<=8; i++) {
            scrambler = new Scrambler(i);
            scrambleItem = new JMenuItem(""+i);
            StaticUtils.addHotKey(KeyEvent.VK_0+i, scrambleItem, "Scramble"+i, scrambler);
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
        helpmenu.add(new JMenuItem("About...")).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                JOptionPane.showMessageDialog(MC4DSwing.this, 
                	"<html><center>" + 
	                	MagicCube.TITLE + 
	                    " Version " + MagicCube.PUZZLE_VERSION + 
	                    "<br>Copyright 2005 by Melinda Green, Don Hatch" +
	                    "<br>with invaluable help from Jay Berkenbilt and Roice Nelson." +
	                    "<br>http://superliminal.com/cube/cube.htm" +
                    "</center></html>");
            }
        });
        
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
        
        viewcontainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // layout the components

        Container contents = getContentPane();
        contents.setLayout(new BorderLayout());
        contents.add(macroControlsContainer, "West"); // I'd prefer west except AWT drop-down menus screw it up.
        contents.add(viewcontainer, "Center");
        contents.add(statusBar, "South");

        puzzleManager = new PuzzleManager(MagicCube.DEFAULT_PUZZLE, MagicCube.DEFAULT_LENGTH, progressBar);
        puzzleManager.setHighlighter(normalHighlighter);
        initMacroControls(); // to show controls
        initPuzzleMenu(puzzlemenu, statusLabel, progressBar);
        initPuzzle(PropertyManager.top.getProperty("logfile"));
    } // end MC4DSwing


    public void initPuzzleMenu(JMenu puzzlemenu, final JLabel statusLabel, final JProgressBar progressView)
    {
        final String[][] table = MagicCube.SUPPORTED_PUZZLES;
        for (int i = 0; i < table.length; ++i)
        {

            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli==null ? table[i][2] :
                                 schlafli + "  " + table[i][2]);

            // Puzzles with triangles kind of suck so far,
            // so we might want to leave them out of the menu...
            boolean allowPuzzlesWithTriangles = false;
            if (!allowPuzzlesWithTriangles)
            {
                if (schlafli != null && schlafli.indexOf("{3") != -1 && !schlafli.equals( "{3,3,3}" ) )
                    continue;
            }

            JMenu submenu;
            if (schlafli != null)
            {
                submenu = new JMenu(name+"    "); // XXX padding so the > doesn't clobber the end of the longest names!? lame
                puzzlemenu.add(submenu);
            }
            else
                submenu = puzzlemenu;
            for (int j = 0; j < lengthStrings.length; ++j)
            {
                final String lengthString = lengthStrings[j];
                submenu.add(new JMenuItem(schlafli==null ? name : "   "+lengthString+"  ")).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                    	String newSchlafli = schlafli;
                    	String newLengthString = lengthString;
                        if (schlafli == null)
                        {
                            String prompt = "Enter your invention:";
                            String name = puzzleManager.puzzleDescription.getSchlafliProduct();
                            String initialInput = name + " " + puzzleManager.getPrettyLength();
                            while (true)
                            {
                                String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                if (reply == null)
                                {
                                    return; // Canceled
                                }
                                String schlafliAndLength[] = reply.trim().split("\\s+");
                                if (schlafliAndLength.length != 2)
                                {
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
                    	puzzleManager.initPuzzle(newSchlafli, newLengthString, progressView, statusLabel, true, puzzleConfigurator);
                    	hist.clear((int)Double.parseDouble(newLengthString));
                    	updateTwistsLabel();
                    	scrambleState = SCRAMBLE_NONE;
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
        for (int i = 0; i < macros.length; ++i)
        {
            final Macro macro = macros[i];
            JMenuItem applyitem = apply.add(new JMenuItem(macro.getName()));
            applyitem.addActionListener(new ProbableAction(macro.getName()) {
                public void doit(ActionEvent ae) {
                    lastMacro = macro;
                    last.doit(ae);
                }
            });
        }
    }

    private void initMacroControls() {
        final JTabbedPane tabs = new JTabbedPane();
        String schlafli = puzzleManager != null && puzzleManager.puzzleDescription != null ? puzzleManager.puzzleDescription.getSchlafliProduct() : null;
        tabs.add("Preferences", new PreferencesEditor());
        tabs.add("Macros", new MacroControls(macroMgr, schlafli, new MacroControls.Listener() {
            public void apply(Macro macro, boolean reverse) {
                lastMacro = macro;
                final int mask = reverse ? ActionEvent.CTRL_MASK : 0;
                // A fake event so action will pick up correct direction.
                // A bit of a hack but sometimes a girl's gotta do what a girl's gotta do!
                ActionEvent ae = new ActionEvent(this, 0, "apply", mask) { public int getID() { return mask; } };
                last.doit(ae);
            }
            public void changed() {
                initMacroMenu();
            }
        }));
        tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ce) {
				PropertyManager.userprefs.setProperty("lasttab", ""+tabs.getSelectedIndex());
			}
        });
        tabs.setSelectedIndex(PropertyManager.getInt("lasttab", 0));
        macroControlsContainer.removeAll();
        macroControlsContainer.add(tabs);
        macroControlsContainer.validate();
    }
    
    public void stickerClicked(MagicCube.TwistData twisted) {
        if(macroMgr.isOpen()) {
            if(macroMgr.recording()) {
                macroMgr.addTwist(twisted);
                view.animate(twisted, true);
            } else {
                if( !macroMgr.addRef( puzzleManager.puzzleDescription, twisted.grip ) )
                	statusLabel.setText( "Picked reference won't determine unique orientation, please try another." );
                else if(macroMgr.recording()) { // true when the reference sticker added was the last one needed.
                    if(applyingMacro != 0) {
                    	setSkyAndHighlighting(null, normalHighlighter);
                        MagicCube.Stickerspec[] refs = macroMgr.close();
                        MagicCube.TwistData[] moves = lastMacro.getTwists(refs,puzzleManager.puzzleDescription);
                        if(moves == null)
                            statusLabel.setText("Reference sticker pattern doesn't match macro definition.");
                        else {
                            if(applyingMacro < 0)
                                Macro.reverse(moves);
                            statusLabel.setText("Applying macro '" + lastMacro.getName() + "'");
                            hist.mark(History.MARK_MACRO_OPEN);
                            view.animate(moves, true);
                            view.append(History.MARK_MACRO_CLOSE);
                        }
                        applyingMacro = 0;
                    }
                    else {
                        statusLabel.setText("Now recording macro twists. Hit <ctrl>m when finished.");
                        setSkyAndHighlighting(Color.black, normalHighlighter);
                    }
                }
                else 
                {
                	statusLabel.setText(""+macroMgr.numRefs()); // a little camera sound here would be great.
                	view.updateStickerHighlighting();
                }
            }
        }
        else {
            statusLabel.setText("");
            view.animate(twisted, true);
        }
    }

    
    /*
     * Format: 
     * 0 - Magic Number 
     * 1 - File Version 
     * 2 - Scramble State
     * 3 - Twist Count
     * 4 - Schlafli Product
     * 5 - Edge Length
     */
    private void initPuzzle(String log) {
        scrambleState = SCRAMBLE_NONE;
        double initialLength = MagicCube.DEFAULT_LENGTH;
        int iLength = (int)Math.ceil(initialLength);
        hist = new History(iLength);
        if(log != null) { // read the log file, possibly reinitializing length and history.
            File logfile = new File(log);
            if(logfile.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(logfile));
                    String firstlineStr = reader.readLine();
                    String firstline[] = firstlineStr.split(" ");
                    if(firstline.length != 6 || !MagicCube.MAGIC_NUMBER.equals(firstline[0]))
                        throw new IOException();
                    int readversion = Integer.parseInt(firstline[1]);
                    if(readversion != MagicCube.LOG_FILE_VERSION) {
                        statusLabel.setText("Incompatible log file version " + readversion);
                        return;
                    }
                    scrambleState = Integer.parseInt(firstline[2]);
                    // int numTwists = Integer.parseInt(firstline[3]);
                    String schlafli = firstline[4];
                    initialLength = Double.parseDouble(firstline[5]);
                    puzzleManager.initPuzzle(schlafli, ""+initialLength, progressBar, statusLabel, false, puzzleConfigurator);
                    iLength = (int)Math.round(initialLength);
                    hist = new History(iLength);
                    String title = MagicCube.TITLE;
                    rotations.read(reader);
                    int c;
                    for(c=reader.read(); !(c=='*' || c==-1); c=reader.read()) ; // read past state data
                    if(hist.read(new PushbackReader(reader)))
                    	title += " - " + logfile.getName();
                    else
                     	System.out.println("Error reading puzzle history");
                    setTitle(title);
                } catch (Exception e) {
                    statusLabel.setText("Failed to parse log file '" + log + "'");
                }
            }
            else
                statusLabel.setText("Couldn't find log file '" + log + "'");
        }
        
        // initialize generic version state
        try {
	        for(Enumeration<MagicCube.TwistData> moves=hist.moves(); moves.hasMoreElements(); ) {
	        	MagicCube.TwistData move = moves.nextElement();
	        	if(move.grip.id_within_puzzle == -1) {
	        		System.err.println("Bad move in MC4DSwing.initPuzzle: " + move.grip.id_within_puzzle);
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
        
        if(view != null) {
        	// attempt to make the old view garbage-collectible.
        	view.removeStickerListener(this);
        	view.setHistory(null);
        }
        view = new MC4DView(puzzleManager, rotations, hist, puzzleManager.puzzleDescription.nFaces());

        viewcontainer.removeAll(); 
        viewcontainer.add(view, "Center");
        
        hist.addHistoryListener(new History.HistoryListener() {
            public void currentChanged() {
                saveitem.setEnabled(true);
                undoitem.setEnabled(hist.countMoves(false) > 0);
                redoitem.setEnabled(hist.hasNextMove());
                cheatitem.setEnabled(hist.hasPreviousMove());
                solveitem.setEnabled(!puzzleManager.isSolved() && puzzleManager.puzzleDescription.getEdgeLength()<4);
                updateTwistsLabel();
                if(puzzleManager.isSolved()) {
                	int intlen = (int)puzzleManager.puzzleDescription.getEdgeLength();
                	if(intlen <= 1)
                		return; // No soup for you!
                    switch (scrambleState) {
                        case SCRAMBLE_PARTIAL:
                            statusLabel.setText("Solved!");
                            Audio.play(Audio.Sound.CORRECT); // Just a little "attaboy" sound.
                            break;
                        case SCRAMBLE_FULL:
                            statusLabel.setText("Solved!");
                            // A really flashy reward.
                            Congratulations congrats = new Congratulations(
        						"<html>" + 
        			            	"<center><H1>You have solved the " +
        							puzzleManager.puzzleDescription.getSchlafliProduct() + " " + intlen + 
        							"!</H1></center>" + 
        			                "<br>You may want to use File > Save As to archive your solution." +
        			                "<br>If this is a first for you or it is a record, consider submitting it to" +
        			                "<br>http://superliminal.com/cube/halloffame.htm" +
        			                "<br><br><p><center>Click this window to close.</center></p>" +
        		                "</html>");
        					congrats.setVisible(true);
        					congrats.start();
                            break;
                    }
                    scrambleState = SCRAMBLE_NONE;
                } // end if(isSolved())
            }
        }); // end HistoryListener impl

        view.addStickerListener(this);

        Macro[] macros = macroMgr.getMacros();
        if(macros.length > 0)
            lastMacro = macros[macros.length-1];

        view.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent arg0) {
                char c = arg0.getKeyChar();
                //System.out.println(c);
                if(c == KeyEvent.VK_ESCAPE)
                    cancel.doit(null);
                //if(c == 'k')  hist.mark(History.MARK_MACRO_OPEN);
                //if(Character.isDigit(c)) {
                //    MagicCube.TwistData toGoto = hist.goTo(c - '0');
                //    if(toGoto != null)
                //        view.animate(toGoto);
                //    else
                //        statusLabel.setText("Nothing to goto.");
                //}
            }
        });
        
        view.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent mwe) {
				if (viewScaleModel != null && mwe.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					int 
						min = viewScaleModel.getMinimum(),
						max = viewScaleModel.getMaximum(),
						cur = viewScaleModel.getValue(),
						newValue = cur + (max - min) / 300 * mwe.getWheelRotation();
					//System.out.println("whee! " + " from " +  cur + " to " + newValue + " (" + min + "," + max + ")");
					viewScaleModel.setValue(newValue);
				}
			}
        });
    } // end initPuzzle
    
    
    public static class PropSlider extends FloatSlider {
		public PropSlider(final String propname, final Component dependent, double dflt, double min, double max) {
			super(JSlider.HORIZONTAL, PropertyManager.getFloat(propname, (float)dflt), min, max);
			setPreferredSize(new Dimension(200, 20));
			addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    PropertyManager.userprefs.setProperty(propname, ""+(float)getFloatValue());
                    dependent.repaint();
                }
            });
		}
    }
    
    public static class PropCheckBox extends JCheckBox {
    	public PropCheckBox(String title, final String propname, boolean dflt, final Component dependent) {
    		super(title, PropertyManager.getBoolean(propname, dflt));
    		addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					PropertyManager.userprefs.setProperty(propname, ""+isSelected());
					dependent.repaint();
				}
    		});
    	}
    }

    
    /**
     * Editor for user preferences.
     */
	private class PreferencesEditor extends JPanel {
		
        
        public PreferencesEditor() {
        	init();
        }
        
        private void init() {
        	// a component that simply forwards all repaint calls to the current view object.
        	Component repainter = new Component() {
				@Override
				public void repaint() {
					if(view != null)
						view.repaint();
				}
        	};
        	
        	removeAll();
            
            final JRadioButton 
	        	ctrlRotateByFace  = new JRadioButton("by Face"),
	        	ctrlRotateByCubie = new JRadioButton("by Cubie");
            ButtonGroup ctrlRotateGroup = new ButtonGroup();
            ctrlRotateGroup.add(ctrlRotateByFace);
            ctrlRotateGroup.add(ctrlRotateByCubie);
            if(PropertyManager.getBoolean("ctrlrotbyface", true))
            	ctrlRotateByFace.setSelected(true);
            else
            	ctrlRotateByCubie.setSelected(true);
            ctrlRotateByFace.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean byFace = ctrlRotateByFace.isSelected();
					PropertyManager.userprefs.setProperty("ctrlrotbyface", ""+byFace);
				}
            });
            ctrlRotateByCubie.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean byCubie = ctrlRotateByCubie.isSelected();
					PropertyManager.userprefs.setProperty("ctrlrotbyface", ""+!byCubie);
				}
            });
            JLabel ctrlClickLabel = new JLabel("Ctrl-Click Rotates:");
            JPanel rotateMode = new JPanel();
            rotateMode.setLayout(new BoxLayout(rotateMode, BoxLayout.X_AXIS));
            rotateMode.add(ctrlClickLabel);
            rotateMode.add(Box.createHorizontalGlue());
            ctrlClickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            final JCheckBox mute = new JCheckBox("Mute Sound Effects", PropertyManager.getBoolean("muted", false));
            mute.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean muted = mute.isSelected();
                    PropertyManager.userprefs.setProperty("muted", ""+muted);
                    Audio.setMuted(muted);
                }
            });
            
            JPanel sliders = new JPanel(new GridLayout(6, 2));
            sliders.add(new JLabel("Twist Speed"));
            sliders.add(new PropSlider("twistfactor", repainter, 1, .05f, 5));
            sliders.add(new JLabel("Drag Speed"));
            sliders.add(new PropSlider("dragfactor", repainter, 1, .05f, 5));
            sliders.add(new JLabel("View Scale"));
            JScrollBar scaler = new PropSlider("scale", repainter, 1, .1f, 5);
            viewScaleModel = scaler.getModel();
            sliders.add(scaler);
            sliders.add(new JLabel("Face Shrink"));
            sliders.add(new PropSlider("faceshrink", repainter, MagicCube.FACESHRINK, .1f, 1.5f));
            sliders.add(new JLabel("Sticker Shrink"));
            sliders.add(new PropSlider("stickershrink", repainter, MagicCube.STICKERSHRINK, .1f, 1.5f));
            sliders.add(new JLabel("Eye W Scale"));
            sliders.add(new PropSlider("eyew", repainter, MagicCube.EYEW, 1, 10));
            sliders.setMaximumSize(new Dimension(400, 20));
            JPanel general = new JPanel();
            general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));
            general.add(sliders);
            general.add(new PropCheckBox("Show Shadows", "shadows", true, repainter));
            general.add(new PropCheckBox("Allow Auto-Rotation", "autorotate", true, repainter));
            general.add(new PropCheckBox("Highlight by Cubie", "highlightbycubie", false, repainter));
            general.add(new PropCheckBox("Quick Moves", "quickmoves", false, repainter));
            general.add(new PropCheckBox("Allow Antialiasing", "antialiasing", true, repainter));
            general.add(mute);
            //general.add(contigiousCubies); // Uncomment when we can make it work immediately and correctly.
            general.add(rotateMode);
            general.add(ctrlRotateByFace);
            general.add(ctrlRotateByCubie);
            general.add(Box.createVerticalGlue());

            // background controls
            ColorButton skyColor = new ColorButton("Sky", "sky.color", MagicCube.SKY, null, true);
            final JCheckBox drawGround = new JCheckBox("Draw Ground", PropertyManager.getBoolean("ground", true));
            final ColorButton ground = new ColorButton("Ground", "ground.color", MagicCube.GROUND, null, true);
            drawGround.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean drawground = drawGround.isSelected();
                    PropertyManager.userprefs.setProperty("ground", ""+drawground);
                    view.repaint();
                }
            });
            
            // outlining controls
            final JCheckBox drawOutlines = new JCheckBox("Draw Outlines", PropertyManager.getBoolean("outlines", false));
            final ColorButton outlinesColor = new ColorButton("Outlines", "outlines.color", Color.BLACK, null, true);
            drawOutlines.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean drawoutlines = drawOutlines.isSelected();
                    PropertyManager.userprefs.setProperty("outlines", ""+drawoutlines);
                    view.repaint();
                }
            });
            
            JPanel colors = new JPanel();
            colors.add(skyColor);
            colors.add(Box.createVerticalStrut(15));
            JPanel tmp = new JPanel();
            tmp.add(drawGround);
            tmp.add(ground);
            colors.add(tmp);
            tmp = new JPanel();
            tmp.add(drawOutlines);
            tmp.add(outlinesColor);
            colors.add(tmp);
            colors.add(Box.createVerticalGlue());
            
            JButton reset = new JButton("Reset To Defaults");
            reset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                	// TODO: make this work. Problem is syncing the view with the reset prefs
                	// without losing the user's history state. Possible if view gets all prefs
                	// from the property manager rather than via setters. Otherwise this is tricky to do right.
                    PropertyManager.userprefs.clear();
                    String fname = logFileChooser.getSelectedFile() == null ? null : logFileChooser.getSelectedFile().getAbsolutePath();
                    initPuzzle(fname);
                    scrambleState = SCRAMBLE_NONE;
                    init(); // to sync the controls with the default prefs.
                    viewcontainer.validate();
                    view.repaint();
                }
            });

            general.setBorder(new TitledBorder("General"));
            colors.setBorder(new TitledBorder("Colors"));
            setPreferredSize(new Dimension(200, 600));
            
            // layout the components
            setLayout(new BorderLayout());
            add(general, "North");
            add(colors, "Center");
            tmp = new JPanel();
            //tmp.add(reset); // commented out until we can make this work well
            add(tmp, "South");
        }
    } // end class PreferencesEditor

    
    private static Color[][] readColorLists(String fname) {
    	URL furl = Util.getResource(fname);
    	if(furl == null) return new Color[0][];
    	String contents = Util.readFileFromURL(furl);
    	//JOptionPane.showMessageDialog(null, contents);
    	if(contents == null) return new Color[0][];
    	ArrayList<Color[]> colorlines = new ArrayList<Color[]>();
    	try {
			BufferedReader br = new BufferedReader(new StringReader(contents));
			for(String line = br.readLine(); line != null; ) {
				StringTokenizer st = new StringTokenizer(line);
				Color[] colorlist = new Color[st.countTokens()];
				for(int i=0; i<colorlist.length; i++) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return colorlines.toArray(new Color[0][]);
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
     * @param args may contain override arguments for any in "defaults.prop"
     * by prefixing the keys with a '-' character. Arguments without following
     * values are assumed to be boolean flags and will be set to "true".
     */
    public static void main(final String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.out.println("version " + System.getProperty("java.version"));
                PropertyManager.loadProps(args, PropertyManager.top);
                try { UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel()); } 
                catch (Exception e) { e.printStackTrace(); }
                final JFrame frame = new MC4DSwing();
                configureNormal(frame);
                frame.addComponentListener(new ComponentAdapter() {
                    public void componentResized(ComponentEvent ce) {
                    	if(frame.getExtendedState() != Frame.NORMAL)
                    		return;
                        PropertyManager.userprefs.setProperty("window.width",  ""+frame.getWidth());
                        PropertyManager.userprefs.setProperty("window.height", ""+frame.getHeight());
                    }
                    public void componentMoved(ComponentEvent ce) {
                    	if(frame.getExtendedState() != Frame.NORMAL)
                    		return;
                        PropertyManager.userprefs.setProperty("window.x", ""+frame.getX());
                        PropertyManager.userprefs.setProperty("window.y", ""+frame.getY());
                    }
                });
                frame.setExtendedState(PropertyManager.getInt("window.state",  frame.getExtendedState()));
                frame.addWindowStateListener(new WindowStateListener() {
                    public void windowStateChanged(WindowEvent we) {
                        int state = frame.getExtendedState();
                        state &= ~Frame.ICONIFIED; // disallows saving in iconified state
                        PropertyManager.userprefs.setProperty("window.state",  ""+state);
                        if(state == Frame.NORMAL){
                        	configureNormal(frame);
                        }
                    }
                });
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
            
            void configureNormal(Frame frame) {
                frame.setSize(
                    PropertyManager.getInt("window.width",  900),
                    PropertyManager.getInt("window.height", 700));
                frame.setLocation(
                    PropertyManager.getInt("window.x", frame.getX()),
                    PropertyManager.getInt("window.y", frame.getY()));
            }
        });
    }
}

