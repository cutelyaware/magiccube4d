import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;

/**
 * The main desktop application.
 * The main method here creates and shows an instance of this class.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DSwing extends JFrame implements MC4DView.TwistListener {
	private final String initialPuzzle = "{5}x{4}"; // "{4,3,3}"

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
    
    private JPanel viewcontainer = new JPanel(new GridLayout(1, 2)); // JComponent container so we can use addHotKey
    private JPanel macroControlsContainer = new JPanel(new BorderLayout());
    private JFileChooser
        logFileChooser = new JFileChooser(),
        macroFileChooser = new JFileChooser();
    private JLabel 
        twistLabel  = new JLabel(),
        statusLabel = new JLabel();
    private JProgressBar progressBar = new JProgressBar();
    
    private GenericGlue genericGlue = null; //new GenericGlue(initialPuzzle, 3, progressBar);

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
                genericGlue.genericPuzzleDescription.getSchlafliProduct() + " " +
                genericGlue.genericPuzzleDescription.getEdgeLength());
            writer.write(sep);
            view.getRotations().write(writer);
            //writer.write(sep + puzzle.toString());
            writer.write(sep + "*" + sep);
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
                	fname = StaticUtils.getHomeDir() + File.separator + MagicCube.LOGFILE;
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
//                solution = History.compress(solution, (int)genericGlue.genericPuzzleDescription.getEdgeLength(), true);
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
                    view.setSkyOverride(null);
                } else { // begin macro definition
                    macroMgr.open();
                    statusLabel.setText("Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                    view.setSkyOverride(Color.white);
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
                view.setSkyOverride(null);
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
                view.setSkyOverride(new Color(255, 170, 170));
            }
        };
        
    private GenericGlue.Callback viewRepainter = new GenericGlue.Callback() {
    	public void call() {
    		initMacroControls(); // to properly enable/disable the buttons
    		progressBar.setVisible(false);
    		view.repaintView(this);
    	}
    };

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
                cancel.doit(ae);
                progressBar.setVisible(true);
                final double length = genericGlue.genericPuzzleDescription.getEdgeLength();
                genericGlue.initPuzzle(
            		genericGlue.genericPuzzleDescription.getSchlafliProduct(),
            		""+length, progressBar, statusLabel, true, 
            		new GenericGlue.Callback() {
            	    	public void call() {
                            hist.clear((int)length);
                            scrambleState = SCRAMBLE_NONE;
                            updateTwistsLabel();
                            view.repaintView(this);
                            if(ae.getSource() instanceof GenericGlue.Callback)
                            {
                            	((GenericGlue.Callback)ae.getSource()).call();
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
        editmenu.add(solveitem);
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
                reset.actionPerformed(new ActionEvent(new GenericGlue.Callback(){
					@Override
					public void call() { // will be called by the reset action when it completes.
		                progressBar.setVisible(true);
		                new ProgressManager(progressBar) {
							@Override
							protected Void doInBackground() throws Exception {
				                int previous_face = -1;
				                int totalTwistsNeededToFullyScramble = 
				                		genericGlue.genericPuzzleDescription.nFaces() // needed twists is proportional to nFaces
				                		* (int)genericGlue.genericPuzzleDescription.getEdgeLength() // and to number of slices
				                		* 2; // and to a fudge factor that brings the 3^4 close to the original 40 twists.
				                int scrambleTwists = scramblechenfrengensen == -1 ? totalTwistsNeededToFullyScramble : scramblechenfrengensen;
								Random rand = new Random();
								init("Scrambling", scrambleTwists);
				                for(int s = 0; s < scrambleTwists; s++) {
				                    // select a random grip that is unrelated to the last one (if any)
				                    int iGrip, iFace, order;
				                    do {
				                        iGrip = rand.nextInt(genericGlue.genericPuzzleDescription.nGrips());
				                        iFace = genericGlue.genericPuzzleDescription.getGrip2Face()[iGrip];
				                        order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];
				                    }
				                    while (
				                        order < 2 || // don't use 360 degree twists
				                        iFace == previous_face || // mixing it up
				                        (previous_face!=-1 && genericGlue.genericPuzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
				                    previous_face = iFace;
				                    int gripSlices = genericGlue.genericPuzzleDescription.getNumSlicesForGrip(iGrip);
				                    int slicemask = 1<<rand.nextInt(gripSlices);
				                    int dir = rand.nextBoolean() ? -1 : 1;
				                    // apply the twist to the puzzle state.
				                    genericGlue.genericPuzzleDescription.applyTwistToState(
				                    		genericGlue.genericPuzzleState,
				                            iGrip,
				                            dir,
				                            slicemask);
				                    // and save it in the history.
				                    MagicCube.Stickerspec ss = new MagicCube.Stickerspec();
				                    ss.id_within_puzzle = iGrip; // slamming new id. do we need to set the other members?
				                    ss.face = genericGlue.genericPuzzleDescription.getGrip2Face()[iGrip];
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
				                view.repaintView(this);
				                boolean fully = scramblechenfrengensen == -1;
				                scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL;
				                statusLabel.setText(fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen==1?"":"s"));
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

        genericGlue = new GenericGlue(initialPuzzle, 3, progressBar);
        initMacroControls(); // to show controls
        initPuzzleMenu(puzzlemenu, statusLabel, progressBar);
        initPuzzle(PropertyManager.top.getProperty("logfile"));
    } // end MC4DSwing


    public void initPuzzleMenu(JMenu puzzlemenu, final JLabel statusLabel, final JProgressBar progressView)
    {
        String table[][] = {
            {"{3,3,3}",  "1,1.9,2,3,4,5,6,7", "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Duoprism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Duoprism"},
            {"{4}x{5}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Duoprism (alt)"},
            {"{6}x{4}",  "1,2,2.5,3,4,5,6,7", "Hexagonal Duoprism"},
            {"{7}x{4}",  "1,2,2.5,3,4,5,6,7", "Heptagonal Duoprism"},
            {"{8}x{4}",  "1,2,2.5,3,4,5,6,7", "Octagonal Duoprism"},
            {"{9}x{4}",  "1,2,2.5,3,4,5,6,7", "Nonagonal Duoprism"},
            {"{10}x{4}", "1,2,2.5,3,4,5,6,7", "Decagonal Duoprism"},
            {"{100}x{4}","1,3",               "Onehundredagonal Duoprism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,2.5,3,4,5,6,7", ""},
            {"{5}x{5}",  "1,2,2.5,3,4,5,6,7", ""}, // XXX 2 is ugly, has slivers
            {"{5}x{10}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{5}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{10}", "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism"},
            {"{}x{5,3}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism (alt)"},
            {"{5,3,3}",  "1,2,2.5,3",         "Hypermegaminx (BIG!)"},
            {null,       "",                  "Invent my own!"},
        };
        for (int i = 0; i < table.length; ++i)
        {

            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli==null ? table[i][2] :
                                 schlafli + "  " + table[i][2]);

            // Puzzles with triangles kind of suck so far,
            // so we might want to leave them out of the menu...
            boolean allowPuzzlesWithTriangles = true;
            //boolean allowPuzzlesWithTriangles = false;
            if (!allowPuzzlesWithTriangles)
            {
                if (schlafli != null && schlafli.indexOf("{3") != -1)
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
                            String initialInput = 
                            	genericGlue.genericPuzzleDescription.getSchlafliProduct() + " " +
                                genericGlue.genericPuzzleDescription.getEdgeLength();

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
                    	genericGlue.initPuzzle(newSchlafli, newLengthString, progressView, statusLabel, true, viewRepainter);
                    	hist.clear((int)Double.parseDouble(newLengthString));
                    	updateTwistsLabel();
                    	scrambleState = SCRAMBLE_NONE;
                    	view.repaintView(this);
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
        String schlafli = genericGlue != null && genericGlue.genericPuzzleDescription != null ? genericGlue.genericPuzzleDescription.getSchlafliProduct() : null;
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
    
    public void twisted(MagicCube.TwistData twisted) {
        if(macroMgr.isOpen()) {
            if(macroMgr.recording()) {
                macroMgr.addTwist(twisted);
                view.animate(twisted, true);
            } else {
                if( !macroMgr.addRef( genericGlue.genericPuzzleDescription, twisted.grip ) )
                	statusLabel.setText( "Picked reference won't determine unique orientation, please try another." );
                else if(macroMgr.recording()) { // true when the reference sticker added was the last one needed.
                    if(applyingMacro != 0) {
                        view.setSkyOverride(null);
                        MagicCube.Stickerspec[] refs = macroMgr.close();
                        MagicCube.TwistData[] moves = lastMacro.getTwists(refs,genericGlue.genericPuzzleDescription);
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
                        view.setSkyOverride(Color.black);
                    }
                }
                else statusLabel.setText(""+macroMgr.numRefs()); // a little camera sound here would be great.
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
        RotationHandler rotations = new RotationHandler();
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
                    genericGlue.initPuzzle(schlafli, ""+initialLength, progressBar, statusLabel, false, viewRepainter);
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
		        genericGlue.genericPuzzleDescription.applyTwistToState(
	        		genericGlue.genericPuzzleState,
	                move.grip.id_within_puzzle,
	                move.direction,
	                move.slicemask);
	        }
        } catch(Exception e) {
        	e.printStackTrace();
        }
        
        if(view != null) {
        	// attempt to make the old view garbage-collectible.
        	view.removeTwistListener(this);
        	view.setHistory(null);
        }
        view = new MC4DView(genericGlue, rotations, hist);

        viewcontainer.removeAll();
        viewcontainer.add(view);
        if(PropertyManager.getBoolean("stereo", true)) {
	        MC4DView rightEye = new MC4DView(genericGlue, rotations, hist);
	        view.setOther(rightEye, -.1f);
	        rightEye.setOther(view, .1f);
	        viewcontainer.add(rightEye);
        }
        
        hist.addHistoryListener(new History.HistoryListener() {
            public void currentChanged() {
                saveitem.setEnabled(true);
                undoitem.setEnabled(hist.countMoves(false) > 0);
                redoitem.setEnabled(hist.hasNextMove());
                cheatitem.setEnabled(hist.hasPreviousMove());
                solveitem.setEnabled(!genericGlue.isSolved() && genericGlue.genericPuzzleDescription.getEdgeLength()<4);
                updateTwistsLabel();
                if(genericGlue.isSolved()) {
                    switch (scrambleState) {
                        case SCRAMBLE_PARTIAL:
                            statusLabel.setText("Solved!");
                            break;
                        case SCRAMBLE_FULL:
                            // this should really be a BIG reward with sound & animation.
                            statusLabel.setText("You have solved the full 4D cube!!!");
                            break;
                    }
                    scrambleState = SCRAMBLE_NONE;
                }                
            }
        });

        view.addTwistListener(this);

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
    } // end initPuzzle

    
    /**
     * Editor for user preferences.
     */
	private class PreferencesEditor extends JPanel {
        private FloatSlider makeSlider(float cur, float min, float max) {
            FloatSlider slider = new FloatSlider(JSlider.HORIZONTAL, cur, min, max);
            try {
                slider.setPreferredSize(new Dimension(200, 20));
            } catch (NoSuchMethodError e) {System.err.println("FloatSlider: no such method setPreferredSize, whatever");}
            return slider;
        }
        private boolean synchingsliders = false;
        
        public PreferencesEditor() {
        	init();
        }
        
        private void init() {
        	removeAll();
            final FloatSlider
	            twistSpeedSlider = makeSlider(PropertyManager.getFloat("twistfactor", 1), .05f, 5),
	            dragSpeedSlider = makeSlider(PropertyManager.getFloat("dragfactor", 1), .05f, 5),
                scaleSlider = makeSlider(PropertyManager.getFloat("scale", 1), .1f, 5),
                fshrinkSlider = makeSlider(PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK), .1f, 1.5f),
                sshrinkSlider = makeSlider(PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK), .1f, 1.5f),
                eyewScaleSlider = makeSlider(PropertyManager.getFloat("eyew", MagicCube.EYEW), 1, 10);
            final JCheckBox contigiousCubies = new JCheckBox("Contigious Cubies", PropertyManager.getBoolean("contigiouscubies", false));
            twistSpeedSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    float twistfactor = (float)twistSpeedSlider.getFloatValue();
                    PropertyManager.userprefs.setProperty("twistfactor", ""+twistfactor);
                    view.repaintView(this);
                }
            });
            dragSpeedSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    float dragfactor = (float)dragSpeedSlider.getFloatValue();
                    PropertyManager.userprefs.setProperty("dragfactor", ""+dragfactor);
                    view.repaintView(this);
                }
            });
            scaleSlider.addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent ae) {
                        float scale = (float)scaleSlider.getFloatValue();
                        PropertyManager.userprefs.setProperty("scale", ""+scale);
                        view.repaintView(this);
                    }
                });
            sshrinkSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    int len = (int)genericGlue.genericPuzzleDescription.getEdgeLength();
                    float newsshrink = (float)sshrinkSlider.getFloatValue();
                    float faceshrink = PropertyManager.getFloat("faceschrink", 1);
                    if(contigiousCubies.isSelected()) {
                        faceshrink = len / (len-1 + newsshrink); // s = n / (n-1 + s)
                        if( ! synchingsliders) {
                            synchingsliders = true;
                            fshrinkSlider.setFloatValue(faceshrink);
                            synchingsliders = false;
                            PropertyManager.userprefs.setProperty("faceshrink", ""+faceshrink);
                        }
                    }
                    PropertyManager.userprefs.setProperty("faceshrink", ""+faceshrink);
                    PropertyManager.userprefs.setProperty("stickershrink", ""+newsshrink);
                    view.repaintView(this);
                }
            });
            fshrinkSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    int len = (int)genericGlue.genericPuzzleDescription.getEdgeLength();
                    float newfaceshrink = (float)fshrinkSlider.getFloatValue();
                    float stickershrink = PropertyManager.getFloat("stickershrink", 1);
                    if(contigiousCubies.isSelected()) {
                        stickershrink = len * (1/newfaceshrink - 1) + 1; // s = n(1/f - 1) + 1
                        if( ! synchingsliders) {
                            synchingsliders = true;
                            sshrinkSlider.setFloatValue(stickershrink);
                            synchingsliders = false;
                            PropertyManager.userprefs.setProperty("stickershrink", ""+stickershrink);
                        }
                    }
                    PropertyManager.userprefs.setProperty("stickershrink", ""+stickershrink);
                    PropertyManager.userprefs.setProperty("faceshrink", ""+newfaceshrink);
                    view.repaintView(this);
                }
            });
            contigiousCubies.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    PropertyManager.userprefs.setProperty("contigiouscubies", ""+contigiousCubies.isSelected());
                    view.repaintView(this);
                }
            });
            eyewScaleSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    float neweyew = (float)eyewScaleSlider.getFloatValue();
                    PropertyManager.userprefs.setProperty("eyew", ""+neweyew);
                    view.repaintView(this);
                }
            });
            final JCheckBox showShadows = new JCheckBox("Show Shadows", PropertyManager.getBoolean("shadows", true));
            showShadows.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean shadows = showShadows.isSelected();
                    PropertyManager.userprefs.setProperty("shadows", ""+shadows);
                    view.repaintView(this);
                }
            });
            final JCheckBox quickMoves = new JCheckBox("Quick Moves", PropertyManager.getBoolean("quickmoves", false));
            quickMoves.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean quickmoves = quickMoves.isSelected();
                    PropertyManager.userprefs.setProperty("quickmoves", ""+quickmoves);
                }
            });
            final JCheckBox allowAutoRotate = new JCheckBox("Allow Auto-Rotation", PropertyManager.getBoolean("autorotate", true));
            allowAutoRotate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean autorotate = allowAutoRotate.isSelected();
                    PropertyManager.userprefs.setProperty("autorotate", ""+autorotate);
                }
            });
            final JCheckBox allowAntiAliasing = new JCheckBox("Allow Antialiasing", PropertyManager.getBoolean("antialiasing", true));
            allowAntiAliasing.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean antialiasing = allowAntiAliasing.isSelected();
                    PropertyManager.userprefs.setProperty("antialiasing", ""+antialiasing);
                    view.repaintView(this);
                }
            });
            final JCheckBox highlightByCubie = new JCheckBox("Highlight by Cubie", PropertyManager.getBoolean("highlightbycubie", false));
            highlightByCubie.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean highlightbycubie = highlightByCubie.isSelected();
                    PropertyManager.userprefs.setProperty("highlightbycubie", ""+highlightbycubie);
                }
            });
            
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
            
            JPanel sliders = new JPanel(new GridLayout(6, 2));
            sliders.add(new JLabel("Twist Speed"));
            sliders.add(twistSpeedSlider);
            sliders.add(new JLabel("Drag Speed"));
            sliders.add(dragSpeedSlider);
            sliders.add(new JLabel("View Scale"));
            sliders.add(scaleSlider);
            sliders.add(new JLabel("Face Shrink"));
            sliders.add(fshrinkSlider);
            sliders.add(new JLabel("Sticker Shrink"));
            sliders.add(sshrinkSlider);
            sliders.add(new JLabel("Eye W Scale"));
            sliders.add(eyewScaleSlider);
            sliders.setMaximumSize(new Dimension(400, 20));
            JPanel general = new JPanel();
            general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));
            general.add(sliders);
            general.add(showShadows);
            general.add(allowAutoRotate);
            general.add(highlightByCubie);
            general.add(quickMoves);
            general.add(allowAntiAliasing);
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
                    view.repaintView(this);
                }
            });
            
            // outlining controls
            final JCheckBox drawOutlines = new JCheckBox("Draw Outlines", PropertyManager.getBoolean("outlines", false));
            final ColorButton outlinesColor = new ColorButton("Outlines", "outlines.color", Color.BLACK, null, true);
            drawOutlines.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean drawoutlines = drawOutlines.isSelected();
                    PropertyManager.userprefs.setProperty("outlines", ""+drawoutlines);
                    view.repaintView(this);
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
                    view.repaintView(this);
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

    private ImageIcon getImageIcon(String path) {
        ClassLoader cl = getClass().getClassLoader();
        return new ImageIcon(cl.getResource(path));
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

