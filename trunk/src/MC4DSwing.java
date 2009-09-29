import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Stack;

import javax.swing.*;
import javax.swing.border.*;

/**
 * The main desktop application.
 * The main method here creates and shows an instance of this class.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class MC4DSwing extends JFrame {
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
    private PuzzleState puzzle;
    private History hist;
    private PolygonManager polymgr;
    private MC4DView view;
    
    private JPanel viewcontainer = new JPanel(new BorderLayout()); // JComponent container so we can use addHotKey
    private JPanel macroControlsContainer = new JPanel(new BorderLayout());
    private float
        twistfactor = 1,
        scale = 1;
    private JFileChooser
        logFileChooser = new JFileChooser(),
        macroFileChooser = new JFileChooser();
    private JLabel 
        twistLabel  = new JLabel(),
        statusLabel = new JLabel();
    private JProgressBar progressBar = new JProgressBar();
    
    private GenericGlue genericGlue = null; //new GenericGlue(initialPuzzle, 3, progressBar);

    private Menu apply = new Menu("Apply");
    private MenuItem // adding the accelerator keys to the strings is hokey.
        openitem    = new MenuItem("Open               Ctrl+O"),
        saveitem    = new MenuItem("Save               Ctrl+S"),
        saveasitem  = new MenuItem("Save As..."),
        quititem    = new MenuItem("Quit               Ctrl+Q"),
        resetitem   = new MenuItem("Reset                     Ctrl+R"),
        undoitem    = new MenuItem("Undo                      Ctrl+Z"),
        redoitem    = new MenuItem("Redo                      Ctrl+V"),
        cheatitem   = new MenuItem("Solve (Cheat)       Ctrl+T"),
        solveitem   = new MenuItem("Solve (For Real)  Ctrl+L");
    
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
    private void saveAs(String logfilename) {
    	if(logfilename == null) {
    		System.err.println("saveAs: null file name");
    		return;
    	}
        File file = new File(logfilename);
        String sep = System.getProperty("line.separator");
        try {
            Writer writer = new FileWriter(file);
            writer.write(
                MagicCube.MAGIC_NUMBER + " " +
                MagicCube.FILE_VERSION + " " +
                scrambleState + " " +
                hist.countTwists() + " " +
                genericGlue.genericPuzzleDescription.getSchlafliProduct() + " " +
                genericGlue.genericPuzzleDescription.getEdgeLength());
            //writer.write(sep + puzzle.toString());
            writer.write(sep + "*" + sep);
            hist.write(writer);
            writer.close();
            String filepath = file.getAbsolutePath();
            statusLabel.setText("Wrote log file " + filepath);
            PropertyManager.userprefs.setProperty("logfile", filepath);
            setTitle(MagicCube.TITLE + " - " + file.getName());
        } catch(IOException ioe) {
            statusLabel.setText("Save failed");
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
            public void doit(ActionEvent ae) {
            	// Save to the previously used log file, if any, otherwise the default.
                String fname = logFileChooser.getSelectedFile() == null ? null : logFileChooser.getSelectedFile().getAbsolutePath();
                if(fname == null)
                	fname = StaticUtils.getHomeDir() + File.separator + MagicCube.LOGFILE;
                saveAs(PropertyManager.top.getProperty("logfile", fname));
            }
        },
        saveas = new ProbableAction("Save As...") {
            public void doit(ActionEvent ae) {
                if(logFileChooser.showSaveDialog(MC4DSwing.this) == JFileChooser.APPROVE_OPTION) {
                    saveAs(logFileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        },
        undo = new ProbableAction("Undo") {
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
                    if(toUndo != null)
                        view.animate(toUndo, false);
                    else
                        statusLabel.setText("Nothing to undo.");
                }
            }
        },
        redo = new ProbableAction("Redo") {
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
                    view.animate(inv, true);
                }
                statusLabel.setText("");
            }
        },
        solve = new ProbableAction("Solve") {
            public void doit(ActionEvent ae) {
                MagicCube.TwistData[] solution;
                try { solution = puzzle.solve(); }
                catch(Error e) {
                    statusLabel.setText("no solution");
                    return;
                }
                solution = History.compress(solution, puzzle.getLength(), true);
                view.animate(solution, true);
                scrambleState = SCRAMBLE_NONE; // no user credit for automatic solutions.
                statusLabel.setText("twists to solve = " + solution.length);
            }
        },
        macro = new ProbableAction("Start/End") { // toggles macro definition start/end
            public void doit(ActionEvent ae) {
                if(macroMgr.isOpen())  { // finished with macro definition
                    String name = JOptionPane.showInputDialog("Name your macro");
                    view.setBackground(PropertyManager.getColor("background.color", MagicCube.BACKGROUND));
                    if(name == null) {
                        macroMgr.cancel();
                        statusLabel.setText("");
                    }
                    else {
                        lastMacro = macroMgr.close(name);
                        initMacroList(); // to show new macro.
                        initMacroControls(); // to show new control
                        statusLabel.setText("Defined \"" + lastMacro.getName() + "\" macro with " +
                            lastMacro.length() + " move" + (lastMacro.length()==1 ? "." : "s."));
                    }
                } else { // begin macro definition
                    macroMgr.open();
                    statusLabel.setText("Click " + Macro.MAXREFS + " reference stickers. Esc to cancel.");
                    view.setBackground(Color.white);
                }
            }
        },
        cancel = new ProbableAction("Cancel Macro Definition") { // toggles macro definition start/end
            public void doit(ActionEvent ae) {
                view.cancelAnimation(); // also stops any animation
                if( ! macroMgr.isOpen())
                    return;
                macroMgr.cancel();
                statusLabel.setText("Cancelled");
                view.setBackground(PropertyManager.getColor("background.color", MagicCube.BACKGROUND));
                applyingMacro = 0;
            }
        },
        last = new ProbableAction("Apply Last Macro") {
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
                view.setBackground(new Color(255, 170, 170));
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
        reset = new AbstractAction("Reset") {
            public void actionPerformed(ActionEvent ae) {
                cancel.doit(ae);
                genericGlue.initPuzzle(
            		genericGlue.genericPuzzleDescription.getSchlafliProduct(),
            		""+genericGlue.genericPuzzleDescription.getEdgeLength(),
            		progressBar, statusLabel, false);
                hist = new History((int)genericGlue.genericPuzzleDescription.getEdgeLength());
                scrambleState = SCRAMBLE_NONE;
                updateTwistsLabel();
                view.repaint();
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
                initMacroList(); // update controls with macro definitions just read.
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
                if(macroFileChooser.showOpenDialog(MC4DSwing.this) != JFileChooser.APPROVE_OPTION)
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

        // set accelerator keys for some actions
        StaticUtils.addHotKey(KeyEvent.VK_R, viewcontainer, "Reset", reset);
        StaticUtils.addHotKey(KeyEvent.VK_Z, viewcontainer, "Undo", undo);
        StaticUtils.addHotKey(KeyEvent.VK_V, viewcontainer, "Redo", redo);
        StaticUtils.addHotKey(KeyEvent.VK_O, viewcontainer, "Open", open);
        StaticUtils.addHotKey(KeyEvent.VK_S, viewcontainer, "Save", save);
        StaticUtils.addHotKey(KeyEvent.VK_Q, viewcontainer, "Quit", quit);
        StaticUtils.addHotKey(KeyEvent.VK_M, viewcontainer, "Macro", macro);
        StaticUtils.addHotKey(KeyEvent.VK_ESCAPE, viewcontainer, "Cancel", cancel);
        StaticUtils.addHotKey(KeyEvent.VK_A, viewcontainer, "Apply", last);
        StaticUtils.addHotKey(KeyEvent.VK_L, viewcontainer, "Real", solve);
        StaticUtils.addHotKey(KeyEvent.VK_T, viewcontainer, "Cheat", cheat);

        // unfortunately using the non-swing menus because the JMenu versions don't
        // paint on top of the non-swing puzzle view for some mysterious reason.

        openitem.addActionListener(open);
        saveitem.addActionListener(save);
        //StaticUtils.addHotKey(KeyEvent.VK_S, saveitem, "Save", save); // this is how we'd do it with swing
        saveasitem.addActionListener(saveas);
        quititem.addActionListener(quit);
        undoitem.addActionListener(undo);
        redoitem.addActionListener(redo);
        resetitem.addActionListener(reset);
        cheatitem.addActionListener(cheat);
        solveitem.addActionListener(solve);
        
        Menu filemenu = new Menu("File");
        filemenu.add(openitem);
        filemenu.addSeparator();
        filemenu.add(saveitem);
        filemenu.add(saveasitem);
        filemenu.addSeparator();
        filemenu.add(quititem);
        Menu editmenu = new Menu("Edit");
        editmenu.add(resetitem);
        editmenu.add(undoitem);
        editmenu.add(redoitem);
        editmenu.addSeparator();
        editmenu.add(cheatitem);
        editmenu.add(solveitem);
        //editmenu.addSeparator();
        //editmenu.add(prefsitem);
        Menu scramblemenu = new Menu("Scramble");

        // Scrambling
        //
        class Scrambler extends ProbableAction {
            private int scramblechens;
            public Scrambler(int scramblechens) {
                super("Scramble " + scramblechens);
                this.scramblechens = scramblechens;
            }
            public void doit(ActionEvent e) {
                reset.actionPerformed(null);
                if (genericGlue.isActive())
                {
                    genericGlue.scrambleAction(view, statusLabel, scramblechens);
                    return;
                }
                MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
                java.util.Random rand = new java.util.Random();
                for(int previous_face=-1,s=0; s<scramblechens; s++) {
                    // select a random grip that is unrelated to the last one (if any) 
                    do {
                        grip.id_within_cube = rand.nextInt(MagicCube.NGRIPS);
                        PolygonManager.fillStickerspecFromIdAndLength(grip, 3);
                    }
                    while (
                        //grip.dim != 2 || // to only use 90 degree twists
                        grip.id_within_face == 13 || // can't twist about center cubies
                        s > 0 && grip.face == previous_face || // mixing it up
                        s > 0 && grip.face == PolygonManager.oppositeFace(previous_face)); // can be same as previous
                    previous_face = grip.face; // remember for next twist
                    int slicesmask = 1<<rand.nextInt(polymgr.getLength()-1); // pick a random slice
                    puzzle.twist(grip, MagicCube.CCW, slicesmask);
                    hist.apply(grip, MagicCube.CCW, slicesmask);
                }
                hist.mark(History.MARK_SCRAMBLE_BOUNDARY);
                view.repaint();
                boolean fully = scramblechens == MagicCube.FULL_SCRAMBLE;
                scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL;
                statusLabel.setText(fully ? "Fully Scrambled" : scramblechens + " Random Twist" + (scramblechens==1?"":"s"));
            }
        }
        for(int i=1; i<=8; i++) {
            Scrambler scrambler = new Scrambler(i);
            StaticUtils.addHotKey(KeyEvent.VK_0+i, viewcontainer, "Scramble"+i, scrambler);
            scramblemenu.add(new MenuItem(""+i + "         Ctrl+"+i)).addActionListener(scrambler);
        }
        scramblemenu.addSeparator();
        Scrambler full = new Scrambler(MagicCube.FULL_SCRAMBLE);
        StaticUtils.addHotKey(KeyEvent.VK_F, viewcontainer, "Full", full);
        scramblemenu.add(new MenuItem("Full     Ctrl+F")).addActionListener(full);
        
        genericGlue = new GenericGlue(initialPuzzle, 3, progressBar);

        // Puzzle lengths
        //
        Menu puzzlemenu = new Menu("Puzzle");
        genericGlue.addItemsToPuzzleMenu(puzzlemenu, statusLabel, progressBar);

        // Macros
        //
        Menu macromenu = new Menu("Macros");
        macromenu.add(new MenuItem("Start/Stop Macro Definition  Ctrl+M")).addActionListener(macro);
        macromenu.add(new MenuItem("Cancel Macro Definition      Ctrl+Esc")).addActionListener(cancel);
        macromenu.add(new MenuItem("Apply Last Macro                   Ctrl+A")).addActionListener(last);
        macromenu.add(apply);
        macromenu.add(new MenuItem("Read Macro File...")).addActionListener(read);
        macromenu.add(new MenuItem("Write Macro File")).addActionListener(write);
        macromenu.add(new MenuItem("Write Macro File As...")).addActionListener(writeas);
        initMacroList(); // create controls for any macro definitions.
        initMacroControls(); // to show controls

        // Help
        //
        Menu helpmenu = new Menu("Help");
        helpmenu.add(new MenuItem("About...")).addActionListener(new ActionListener() {
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
        
        MenuBar menubar = new MenuBar();
        menubar.add(filemenu);
        menubar.add(editmenu);
        menubar.add(scramblemenu);
        menubar.add(puzzlemenu);
        menubar.add(macromenu);
        menubar.add(helpmenu);
        setMenuBar(menubar);
        
        JPanel statusBar = new JPanel();
        statusBar.setBackground(this.getBackground());
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
        statusBar.add(Box.createHorizontalStrut(10));
        statusBar.add(statusLabel);
        progressBar.setStringPainted(true);
        statusBar.add(progressBar);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(twistLabel);
        statusBar.add(Box.createHorizontalStrut(10));
        
        viewcontainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // layout the components

        Container contents = getContentPane();
        contents.setLayout(new BorderLayout());
        contents.add(macroControlsContainer, "East"); // I'd prefer west except AWT drop-down menus screw it up.
        contents.add(viewcontainer, "Center");
        contents.add(statusBar, "South");
        
        initPuzzle(PropertyManager.top.getProperty("logfile"));
    } // end MC4DSwing


    /**
     * Called whenever macro list in manager changes to keep "Apply" submenu up-to-date.
     */
    private void initMacroList() {
        apply.removeAll();
        Macro macros[] = macroMgr.getMacros();
        for (int i = 0; i < macros.length; ++i)
        {
            final Macro macro = macros[i];
            MenuItem applyitem = apply.add(new MenuItem(macro.getName()));
            applyitem.addActionListener(new ProbableAction(macro.getName()) {
                public void doit(ActionEvent ae) {
                    lastMacro = macro;
                    last.doit(ae);
                }
            });
        }
    }

    private void initMacroControls() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Macros", new MacroControls(macroMgr, new MacroControls.Listener() {
            public void apply(Macro macro, boolean reverse) {
                lastMacro = macro;
                final int mask = reverse ? ActionEvent.CTRL_MASK : 0;
                // A fake event so action will pick up correct direction.
                // A bit of a hack but sometimes a girl's gotta do what a girl's gotta do!
                ActionEvent ae = new ActionEvent(this, 0, "apply", mask) { public int getID() { return mask; } };
                last.doit(ae);
            }
            public void changed() {
                initMacroList();
            }
        }));
        tabs.add("Preferences", new PreferencesEditor());
        macroControlsContainer.removeAll();
        macroControlsContainer.add(tabs);
        macroControlsContainer.validate();
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
    private void initPuzzle(String logfilename) {
        scrambleState = SCRAMBLE_NONE;
        scale = PropertyManager.getFloat("scale", 1);
        double initialLength = MagicCube.DEFAULT_LENGTH;
        int iLength = (int)Math.ceil(initialLength);
        hist = new History(iLength);
        String stateStr = "";
        if(logfilename != null) { // read the log file, possibly reinitializing length and history.
            File logfile = new File(logfilename);
            if(logfile.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(logfile));
                    String firstlineStr = reader.readLine();
                    String firstline[] = firstlineStr.split(" ");
                    if(firstline.length != 6 || !MagicCube.MAGIC_NUMBER.equals(firstline[0]))
                        throw new IOException();
                    int readversion = Integer.parseInt(firstline[1]);
                    if(readversion != MagicCube.FILE_VERSION) {
                        statusLabel.setText("Incompatible log file version " + readversion);
                        return;
                    }
                    scrambleState = Integer.parseInt(firstline[2]);
                    // int numTwists = Integer.parseInt(firstline[3]);
                    String schlafli = firstline[4];
                    initialLength = Double.parseDouble(firstline[5]);
                    genericGlue.initPuzzle(schlafli, ""+initialLength, progressBar, statusLabel, false);
                    iLength = (int)Math.round(initialLength);
                    hist = new History(iLength);
                    String title = MagicCube.TITLE;
                    int c;
                    for(c=reader.read(); !(c=='*' || c==-1); c=reader.read()) ; // read past state data
                    if(hist.read(new PushbackReader(reader)))
                    	title +=  " - " + schlafli + " - " + logfile.getName();
                    else
                     	System.out.println("Error reading puzzle history");
                    setTitle(title);
                } catch (Exception e) {
                    statusLabel.setText("Failed to parse log file '" + logfilename + "'");
                }
            }
            else
                statusLabel.setText("Couldn't find log file '" + logfilename + "'");
        }
        polymgr = new PolygonManager(iLength);
        if(PropertyManager.top.getProperty("faceshrink") != null || PropertyManager.top.getProperty("stickershrink") != null)
            polymgr.setShrinkers(
                PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK), 
                PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK));
        polymgr.setEyeW(PropertyManager.getFloat("eyew", MagicCube.EYEW));
        polymgr.setTwistFactor(PropertyManager.getFloat("twistfactor", 1));
        puzzle = new PuzzleState(iLength, polymgr);
        if(stateStr.length() > 0)
            puzzle.init(stateStr);
        
        // initialize generic version state
        try {
	        for(Enumeration<MagicCube.TwistData> moves=hist.moves(); moves.hasMoreElements(); ) {
	        	MagicCube.TwistData move = moves.nextElement();
		        genericGlue.genericPuzzleDescription.applyTwistToState(
	        		genericGlue.genericPuzzleState,
	                move.grip.id_within_cube,
	                move.direction,
	                move.slicemask);
	        }
        } catch(Exception e) {
        	e.printStackTrace();
        }
        
        view = new MC4DView(puzzle, polymgr, hist, genericGlue.genericPuzzleDescription.nFaces());
        view.genericGlue = genericGlue; // make it share mine
        //genericGlue.deactivate(); // to start with normal cubes
        view.setScale(scale); // XXX added-- I think this is needed, otherwise the Property's scale doesn't get applied til I hit the scale slider! -don

        view.setBackground(PropertyManager.getColor("background.color"));
//        for(int f=0; f<MagicCube.NFACES; f++)
//            view.setFaceColor(f, PropertyManager.getColor("face"+f+".color", MagicCube.faceColor(f)));
        Color gc = PropertyManager.getColor("ground.color", MagicCube.GROUND);
        view.setGround(PropertyManager.getBoolean("ground", true) ? gc : null);
        view.setShowShadows(PropertyManager.getBoolean("shadows", true));
        view.setQuickMoves(PropertyManager.getBoolean("quickmoves", false));
        view.allowSpinDrag(PropertyManager.getBoolean("spindrag", true));
        view.allowAntiAliasing(PropertyManager.getBoolean("antialiasing", true));
        view.setHighlightByCubie(PropertyManager.getBoolean("highlightbycubie", false));
        Color ol = PropertyManager.getColor("outlines.color", Color.BLACK);
        view.setOutlined(PropertyManager.getBoolean("outlines", false) ? ol : null);
        viewcontainer.removeAll();
        viewcontainer.add(view, "Center");
        
        hist.addHistoryListener(new History.HistoryListener() {
            public void currentChanged() {
                saveitem.setEnabled(true);
                undoitem.setEnabled(hist.countMoves(false) > 0);
                redoitem.setEnabled(hist.hasNextMove());
                cheatitem.setEnabled(hist.hasPreviousMove());
                solveitem.setEnabled(!puzzle.isSolved() && polymgr.getLength()<4);
                if (genericGlue.isActive())
                {
                    undoitem.setEnabled(true); // so I don't have to think
                    redoitem.setEnabled(true); // so I don't have to think
                    cheatitem.setEnabled(true); // so I don't have to think
                }
                updateTwistsLabel();
                if(puzzle.isSolved()) {
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

        view.addTwistListener(new MC4DView.TwistListener() {
            public void twisted(MagicCube.TwistData twisted) {
                if(macroMgr.isOpen()) {
                    if(macroMgr.recording()) {
                        macroMgr.addTwist(twisted);
                        view.animate(twisted, true);
                    } else {
                        macroMgr.addRef(twisted.grip);
                        if(macroMgr.recording()) { // true when the reference sticker added was the last one needed.
                            if(applyingMacro != 0) {
                                view.setBackground(PropertyManager.getColor("background.color", MagicCube.BACKGROUND));
                                MagicCube.Stickerspec[] refs = macroMgr.close();
                                MagicCube.TwistData[] moves = lastMacro.getTwists(refs);
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
                                view.setBackground(Color.black);
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
        });

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
//                if(Character.isDigit(c)) {
//                    MagicCube.TwistData toGoto = hist.goTo(c - '0');
//                    if(toGoto != null)
//                        view.animate(toGoto);
//                    else
//                        statusLabel.setText("Nothing to goto.");
//                }
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
        
        PreferencesEditor() {
            final FloatSlider
                twistfactorSlider = makeSlider(PropertyManager.getFloat("twistfactor", 1), .05f, 5),
                scaleSlider = makeSlider(PropertyManager.getFloat("scale", 1), .1f, 5),
                fshrinkSlider = makeSlider(PropertyManager.getFloat("faceshrink", MagicCube.FACESHRINK), .1f, 1.5f),
                sshrinkSlider = makeSlider(PropertyManager.getFloat("stickershrink", MagicCube.STICKERSHRINK), .1f, 1.5f),
                eyewScaleSlider = makeSlider(PropertyManager.getFloat("eyew", MagicCube.EYEW)/MagicCube.EYEW, .25f, 4);
            final JCheckBox contigiousCubies = new JCheckBox("Contigious Cubies", PropertyManager.getBoolean("contigiouscubies", false));
            twistfactorSlider.addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent ae) {
                        twistfactor = (float)twistfactorSlider.getFloatValue();
                        PropertyManager.userprefs.setProperty("twistfactor", ""+twistfactor);
                        view.setTwistFactor(twistfactor);
                    }
                });
            scaleSlider.addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent ae) {
                        scale = (float)scaleSlider.getFloatValue();
                        PropertyManager.userprefs.setProperty("scale", ""+scale);
                        view.setScale(scale);
                    }
                });
            sshrinkSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    int len = polymgr.getLength();
                    float newsshrink = (float)sshrinkSlider.getFloatValue();
                    float faceshrink = polymgr.getFaceShrink();
                    if(contigiousCubies.isSelected()) {
                        faceshrink = len / (len-1 + newsshrink); // s = n / (n-1 + s)
                        if( ! synchingsliders) {
                            synchingsliders = true;
                            fshrinkSlider.setFloatValue(faceshrink);
                            synchingsliders = false;
                            PropertyManager.userprefs.setProperty("faceshrink", ""+faceshrink);
                        }
                    }
                    polymgr.setShrinkers(faceshrink, newsshrink);
                    PropertyManager.userprefs.setProperty("stickershrink", ""+newsshrink);
                    view.repaint();
                }
            });
            fshrinkSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    int len = polymgr.getLength();
                    float newfaceshrink = (float)fshrinkSlider.getFloatValue();
                    float stickershrink = polymgr.getStickerShrink();
                    if(contigiousCubies.isSelected()) {
                        stickershrink = len * (1/newfaceshrink - 1) + 1; // s = n(1/f - 1) + 1
                        if( ! synchingsliders) {
                            synchingsliders = true;
                            sshrinkSlider.setFloatValue(stickershrink);
                            synchingsliders = false;
                            PropertyManager.userprefs.setProperty("stickershrink", ""+stickershrink);
                        }
                    }
                    polymgr.setShrinkers(newfaceshrink, stickershrink);
                    PropertyManager.userprefs.setProperty("faceshrink", ""+newfaceshrink);
                    view.repaint();
                }
            });
            contigiousCubies.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    PropertyManager.userprefs.setProperty("contigiouscubies", ""+contigiousCubies.isSelected());
                    view.repaint();
                }
            });
            eyewScaleSlider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent ae) {
                    float newscale = (float)eyewScaleSlider.getFloatValue();
                    float neweyew = MagicCube.EYEW * newscale;
                    polymgr.setEyeW(neweyew);
                    PropertyManager.userprefs.setProperty("eyew", ""+neweyew);
                    view.repaint();
                }
            });
            final JCheckBox showShadows = new JCheckBox("Show Shadows", PropertyManager.getBoolean("shadows", true));
            showShadows.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean shadows = showShadows.isSelected();
                    MC4DSwing.this.view.setShowShadows(shadows);
                    PropertyManager.userprefs.setProperty("shadows", ""+shadows);
                }
            });
            final JCheckBox quickMoves = new JCheckBox("Quick Moves", PropertyManager.getBoolean("quickmoves", false));
            quickMoves.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean quickmoves = quickMoves.isSelected();
                    MC4DSwing.this.view.setQuickMoves(quickmoves);
                    PropertyManager.userprefs.setProperty("quickmoves", ""+quickmoves);
                }
            });
            final JCheckBox allowSpinDrag = new JCheckBox("Allow Spin Dragging", PropertyManager.getBoolean("spindrag", true));
            allowSpinDrag.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean spindrag = allowSpinDrag.isSelected();
                    MC4DSwing.this.view.allowSpinDrag(spindrag);
                    PropertyManager.userprefs.setProperty("spindrag", ""+spindrag);
                }
            });
            final JCheckBox allowAntiAliasing = new JCheckBox("Allow Antialiasing", PropertyManager.getBoolean("antialiasing", true));
            allowAntiAliasing.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean antialiasing = allowAntiAliasing.isSelected();
                    MC4DSwing.this.view.allowAntiAliasing(antialiasing);
                    PropertyManager.userprefs.setProperty("antialiasing", ""+antialiasing);
                }
            });
            final JCheckBox highlightByCubie = new JCheckBox("Highlight by Cubie", PropertyManager.getBoolean("highlightbycubie", false));
            highlightByCubie.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean highlightbycubie = highlightByCubie.isSelected();
                    MC4DSwing.this.view.setHighlightByCubie(highlightbycubie);
                    PropertyManager.userprefs.setProperty("highlightbycubie", ""+highlightbycubie);
                }
            });
            
            JPanel sliders = new JPanel(new GridLayout(5, 2));
            sliders.add(new JLabel("Twist Speed"));
            sliders.add(twistfactorSlider);
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
            general.add(contigiousCubies);
            general.add(showShadows);
            general.add(allowSpinDrag);
            general.add(highlightByCubie);
            general.add(quickMoves);
            general.add(allowAntiAliasing);
            general.add(Box.createVerticalGlue());

            JPanel faces = new JPanel();
            faces.setLayout(new GridLayout(2, 4));
            for(int f=0; f<MagicCube.NFACES; f++) {
                String key = "face" + f + ".color";
                Color def = MagicCube.faceColor(f);
                final int n = f;
                final JButton faceButt = new ColorButton(""+f, key, def, new ColorButton.ColorChangeListener() {
                    public void colorChanged(Color newColor) {
                        view.setFaceColor(n, newColor);
                    }
                }, true);
                faces.add(faceButt);
            }

            // background controls
            JButton backgroundColor = new ColorButton("Background", "background.color", MagicCube.BACKGROUND,
                new ColorButton.ColorChangeListener() {
                    public void colorChanged(Color newColor) {
                        view.setBackground(newColor);
                    }
                }, true
            );
            final JCheckBox drawGround = new JCheckBox("Draw Ground", PropertyManager.getBoolean("ground", true));
            final JButton ground = new ColorButton("Ground", "ground.color", MagicCube.GROUND,
                new ColorButton.ColorChangeListener() {
                    public void colorChanged(Color newColor) {
                        if( ! drawGround.isSelected())
                            return;
                        view.setGround(newColor);
                    }
                }, true
            );
            drawGround.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean drawground = drawGround.isSelected();
                    view.setGround(drawground ? ground.getBackground() : null);
                    PropertyManager.userprefs.setProperty("ground", ""+drawground);
                }
            });
            
            // outlining controls
            final JCheckBox drawOutlines = new JCheckBox("Draw Outlines", PropertyManager.getBoolean("outlines", false));
            final JButton outlinesColor = new ColorButton("Outlines", "outlines.color", Color.BLACK,
                new ColorButton.ColorChangeListener() {
                    public void colorChanged(Color newColor) {
                        if( ! drawOutlines.isSelected())
                            return;
                        view.setOutlined(newColor);
                    }
                }, true
            );
            drawOutlines.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    boolean drawoutlines = drawOutlines.isSelected();
                    view.setOutlined(drawoutlines ? outlinesColor.getBackground() : null);
                    PropertyManager.userprefs.setProperty("outlines", ""+drawoutlines);
                }
            });
            
            JPanel colors = new JPanel();
            colors.add(backgroundColor);
            colors.add(Box.createVerticalStrut(15));
            JPanel tmp = new JPanel();
            tmp.add(new JLabel("Faces:  "));
            tmp.add(faces);
            colors.add(tmp);
            colors.add(Box.createVerticalStrut(15));
            tmp = new JPanel();
            tmp.add(drawGround);
            tmp.add(ground);
            colors.add(tmp);
            tmp = new JPanel();
            tmp.add(drawOutlines);
            tmp.add(outlinesColor);
            colors.add(tmp);
            colors.add(Box.createVerticalGlue());
            
            JButton clear = new JButton("Reset To Defaults");
            clear.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    PropertyManager.userprefs.clear();
                    String fname = logFileChooser.getSelectedFile() == null ? null : logFileChooser.getSelectedFile().getAbsolutePath();
                    initPuzzle(fname);
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
            tmp.add(clear);
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
                final JFrame frame = new MC4DSwing();
                frame.setSize(
                    PropertyManager.getInt("window.width",  900),
                    PropertyManager.getInt("window.height", 700));
                frame.setLocation(
                    PropertyManager.getInt("window.x", frame.getX()),
                    PropertyManager.getInt("window.y", frame.getY()));
                frame.addComponentListener(new ComponentAdapter() {
                    public void componentResized(ComponentEvent ce) {
                        PropertyManager.userprefs.setProperty("window.width",  ""+frame.getWidth());
                        PropertyManager.userprefs.setProperty("window.height", ""+frame.getHeight());
                    }
                    public void componentMoved(ComponentEvent ce) {
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
                    }
                });
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}

