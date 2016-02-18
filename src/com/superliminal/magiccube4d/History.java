package com.superliminal.magiccube4d;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Maintains a sequence of twists, rotates, and marks applied to a MagicCube4D puzzle.
 * Supports undo/redo and macro moves and is able to save and restore from log files.
 * 
 * <p>
 * DESIGN
 * </p>
 * 
 * <pre>
 * - Twists and rotates are called "moves". Rotates are represented internally as twists that affect all slices (-1) but are logically considered a different kind of move.
 * - Marks are single character delimiters that can be inserted between moves like bookmarks. Note: there can be any number of marks between moves.
 * - Moves and marks are called history nodes.
 * - Macros are represented internally by a sequence of nodes bracketed by the reserved characters '[' and ']'.
 * - There is a reference to a "current" move which may be any node and can be accessed via getCurrent() and controlled with the various goToXxxx() methods. Internally, a null current refers to last.
 * - Notification of changes to the current node can be listened to.
 * </pre>
 * 
 * Copyright 2005 - Superliminal Software
 * 
 * @author Don Hatch
 * @author Melinda Green
 */
public class History {
    private static boolean DEBUG = false;
    public static void setDebugging(boolean on) {
        DEBUG = on;
    }
    public static boolean getDebugging() {
        return DEBUG;
    }

    static private void Assert(boolean condition) {
        if(!condition)
            throw new Error("Assertion failed");
    }

    // Predefined marks.
    // TODO: Move these to MagicCube.java and factor out any macro-specific functionality from History class.
    public final static char
        MARK_ANY = 0, // Matches any mark when searching.
        MARK_MACRO_OPEN = '[',
        MARK_MACRO_CLOSE = ']',
        MARK_SCRAMBLE_BOUNDARY = '|',
        MARK_SETUP_MOVES = 'S';

    private int edgeLength;

    private static class HistoryNode {
        public int stickerid;
        public int dir;
        public int slicesmask;
        public char mark;
        public HistoryNode prev, next; /* doubly linked list */
        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            HistoryNode other = (HistoryNode) obj;
            if(dir != other.dir)
                return false;
            if(mark != other.mark)
                return false;
            if(slicesmask != other.slicesmask)
                return false;
            if(stickerid != other.stickerid)
                return false;
            return true;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + dir;
            result = prime * result + mark;
            result = prime * result + slicesmask;
            result = prime * result + stickerid;
            return result;
        }
    }
    private HistoryNode first, last, current; // If current == null, current is logically last.

    public History(int edgeLength) {
        this.edgeLength = edgeLength;
    }

    public Enumeration<MagicCube.TwistData> moves() {
        return new Enumeration<MagicCube.TwistData>() {
            private Queue<HistoryNode> queue = findTwists();
            @Override
            public boolean hasMoreElements() {
                return !queue.isEmpty();
            }
            @Override
            public MagicCube.TwistData nextElement() {
                HistoryNode n = queue.remove();
                return new MagicCube.TwistData(n.stickerid, n.dir, n.slicesmask);
            }
            private Queue<HistoryNode> findTwists() {
                Queue<HistoryNode> twists = new LinkedList<HistoryNode>();
                for(HistoryNode n = first; n != null && n != current; n = n.next)
                    if(n.mark == 0)
                        twists.add(n);
                return twists;
            }
        };
    }

    /**
     * @return array version of moves().
     */
    public MagicCube.TwistData[] movesArray() {
        ArrayList<MagicCube.TwistData> list = new ArrayList<MagicCube.TwistData>();
        for(Enumeration<MagicCube.TwistData> e = moves(); e.hasMoreElements();)
            list.add(e.nextElement());
        return list.toArray(new MagicCube.TwistData[0]);
    }

    /**
     * @return the most recent mark or -1 if none.
     */
    public int lastMark() {
        for(HistoryNode n = current == null ? last : current; n != null; n = n.prev)
            if(n.mark != 0)
                return n.mark;
        return -1;
    }

    /**
     * @return the next mark after current or -1 if none.
     */
    public int nextMark() {
        if(current == null)
            return -1;
        for(HistoryNode n = current.next; n != null; n = n.next)
            if(n.mark != 0)
                return n.mark;
        return -1;
    }

    /**
     * @return all twists from the given previous mark.
     */
    public Enumeration<MagicCube.TwistData> movesFromMark(final char from_mark) {
        return new Enumeration<MagicCube.TwistData>() {
            private Queue<HistoryNode> queue = findTwistsFrom(from_mark);
            @Override
            public boolean hasMoreElements() {
                return queue != null && !queue.isEmpty();
            }
            @Override
            public MagicCube.TwistData nextElement() {
                HistoryNode n = queue.remove();
                return new MagicCube.TwistData(n.stickerid, n.dir, n.slicesmask);
            }
            private Queue<HistoryNode> findTwistsFrom(char mark) {
                // Search backwards for first node m containing given mark.
                HistoryNode m = findMark(mark, true);
                if(m == null)
                    return null; // Previous mark not found.
                // Collect all non twists following mark.
                Queue<HistoryNode> twists = new LinkedList<HistoryNode>();
                for(HistoryNode n = m.next; n != null && n != null; n = n.next)
                    if(n.mark == 0)
                        twists.add(n);
                return twists;
            }
        };
    }

    private boolean deleteNode(HistoryNode node) {
        if(node == null)
            return false;
        boolean changed = false;
        if(current == node) {
            current = node.next;
            changed = true;
        }
        if(node.prev == null) {
            Assert(node == first);
            first = node.next;
        }
        else
            node.prev.next = node.next;
        if(node.next == null) {
            Assert(node == last);
            last = node.prev;
        }
        else
            node.next.prev = node.prev;
        if(changed)
            fireCurrentChanged();
        return changed;
    }

    private void insertNode(HistoryNode node_to_insert_before, int stickerid, int dir, int slicesmask) {
        insertNode(node_to_insert_before, stickerid, dir, slicesmask, (char) 0);
    }

    private void insertNode(HistoryNode node_to_insert_before, int stickerid, int dir, int slicesmask, char mark) {
        HistoryNode temp = new HistoryNode();
        temp.stickerid = stickerid;
        temp.dir = dir;
        temp.slicesmask = slicesmask;
        temp.mark = mark;
        temp.prev = (node_to_insert_before == null ? last : node_to_insert_before.prev);
        temp.next = node_to_insert_before;
        if(temp.next == null)
            last = temp;
        else
            temp.next.prev = temp;
        if(temp.prev == null)
            first = temp;
        else
            temp.prev.next = temp;
        fireCurrentChanged();
    }

    public void deleteLast() {
        deleteNode(last);
    }

    public void clear(int newLength) {
        edgeLength = newLength;
        while(first != null)
            deleteLast();
    }

    public void clear() {
        clear(edgeLength);
    }

    public void append(int stickerid, int dir, int slicesmask) {
        if(slicesmask == 0)
            slicesmask = 1; // 0 means slicemask 1 so keep them consistent so they always compare equal.
        HistoryNode node = getPrevious();
        // When a twist is the inverse of the previous one, we can sometimes turn it into an undo.
        if(node != null // There is a previous twist
            && node.stickerid == stickerid // on the same axis
            && node.slicesmask == slicesmask // affecting the same slices
            && node.dir == -dir) // but in the *opposite* direction.
        {
            undo(); // just back the move out rather than append an inverse move
            truncate(); // Required because redo should not be possible since this is not a true undo.
        }
        else
            insertNode(current, stickerid, dir, slicesmask);
    }

    /**
     * Simply calls 3-arg version with data from given move.
     */
    public void append(MagicCube.TwistData move) {
        append(move.grip.id_within_puzzle, move.direction, move.slicemask);
    }

    public void append(MagicCube.TwistData[] moves) {
        for(MagicCube.TwistData move : moves)
            append(move.grip.id_within_puzzle, move.direction, move.slicemask);
    }

    /**
     * Delete any current node and everything after it.
     */
    public void truncate() {
        while(current != null)
            deleteLast();
    }


    /**
     * Put a single move into the history.
     * This clears the history after the current point,
     * so a "redo" is impossible afterwards.
     */
    public void apply(MagicCube.Stickerspec sticker, int dir, int slicesmask) {
        truncate();
        append(sticker.id_within_puzzle, dir, slicesmask);
    }

    /**
     * Simply calls 3-arg version with data from given move.
     */
    public void apply(MagicCube.TwistData move) {
        apply(move.grip, move.direction, move.slicemask);
    }

    /**
     * Only the first "edge-length" bits of slice masks are valid
     * so for example, values like 7, 15, and -1 are all pure rotations on an edge-length 3 puzzle.
     * 
     * @return true if given slice mask affects the entire puzzle, false otherwise.
     */
    private boolean isRotate(int slicemask) {
        for(int i = 0; i < edgeLength; i++)
            if((slicemask & 1 << i) == 0)
                return false;
        return true;
    }


    public int countTwists() {
        return countMoves(true);
    }

    public int countMoves(boolean excludeRotates) {
        int result = 0;
        boolean hitscrambleboundary = false;
        for(HistoryNode cur_node = first; cur_node != null && cur_node != current; cur_node = cur_node.next)
            if(cur_node.stickerid >= 0) {
                if(!(excludeRotates && isRotate(cur_node.slicesmask)))
                    ++result;
            }
            else if(!hitscrambleboundary) {
                if(cur_node.mark == MARK_SCRAMBLE_BOUNDARY) {
                    hitscrambleboundary = true;
                    result = 0;
                }
            }
        return result;
    }

    private MagicCube.TwistData getCurrent() {
        return new MagicCube.TwistData(current.stickerid, current.dir, current.slicesmask);
    }

    public void goToBeginning() {
        current = first;
        fireCurrentChanged();
    }
    public void goToEnd() {
        current = null;
        fireCurrentChanged();
    }
    public boolean goToPrevious() {
        if(current == null)
            return false;
        current = current.prev;
        fireCurrentChanged();
        return true;
    }
    public boolean goToNext() {
        if(current == null)
            return false;
        current = current.next;
        fireCurrentChanged();
        return true;
    }


    /**
     * Back up one move in the history, returning a move
     * that would undo the last move or null if nothing to undo.
     */
    public MagicCube.TwistData undo() {
        //search backwards to the next actual move
        HistoryNode node;
        for(node = getPrevious(); node != null; node = node.prev)
            // not quite the same as getPreviousTwist()?
            if(node.stickerid != -1)
                break;
        if(node == null)
            return null;
        current = node;
        MagicCube.TwistData toundo = getCurrent();
        toundo.direction *= -1;
        fireCurrentChanged();
        return toundo;
    }

    /**
     * Go forward one move in the history, returning the move
     * to redo or null if there is nothing to redo.
     * This is only valid if a move was undone.
     */
    public MagicCube.TwistData redo() {
        if(current == null)
            return null;
        while(current != null && current.stickerid == -1)
            current = current.next;
        if(current == null)
            return null;
        MagicCube.TwistData toredo = getCurrent();
        current = current.next;
        fireCurrentChanged();
        return toredo;
    }

    /**
     * @return true if history has a previous actual twist or rotate.
     */
    public boolean hasPreviousMove() {
        for(HistoryNode node = current == null ? last : current; node != null; node = node.prev)
            if(node.stickerid != -1)
                return true;
        return false;
    }

    /**
     * @return the last actual twist or rotate.
     */
    public HistoryNode getPreviousMove() {
        for(HistoryNode node = current == null ? last : current; node != null; node = node.prev)
            if(node.stickerid != -1)
                return node;
        return null;
    }

    public boolean hasNextMove() {
        for(HistoryNode node = current; node != null; node = node.next)
            if(node.stickerid != -1)
                return true;
        return false;
    }


    /**
     * @return most recent history node whether actual move or not.
     */
    private HistoryNode getPrevious() {
        return(current != null ? current.prev : last);
    }

    /**
     * @return next history node whether actual move or not.
     * 
     *         private HistoryNode getNext() {
     *         return (current!=null ? current.next : null);
     *         }
     */

    //
    // MARK METHODS
    //

    public void mark(char mark) {
        insertNode(current, -1, 0, 0, mark);
    }

    /**
     * Deletes the first given mark at or before the current node.
     * 
     * @return true if found, false otherwise.
     */
    public boolean removeLastMark(char mark) {
        boolean deleted = deleteNode(findMark(mark, true));
        if(deleted && DEBUG)
            System.out.println(this);
        return deleted;
    }

    /**
     * Deletes the next given mark at or after the current node.
     * 
     * @return true if found, false otherwise.
     */
    public boolean removeNextMark(char mark) {
        boolean deleted = deleteNode(findMark(mark, false));
        if(deleted && DEBUG)
            System.out.println(this);
        return deleted;
    }

    /**
     * Removes all instances of the given mark or all marks if given MARK_ANY.
     */
    public void removeAllMarks(char mark) {
        boolean deleted = false;
        for(HistoryNode n = first; n != null;) {
            HistoryNode next = n.next;
            if(n.stickerid == -1 && (mark == MARK_ANY || n.mark == mark))
                deleted |= deleteNode(n);
            n = next;
        }
        if(deleted && DEBUG)
            System.out.println(this);
    }

    /**
     * Shorthand for removeAllMarks(MARK_ANY);
     */
    public void removeAllMarks() {
        removeAllMarks(MARK_ANY);
    }

    public boolean atMark(int mark) {
        if(current != null && current.stickerid == -1 && (mark == MARK_ANY || current.mark == mark))
            return true;
        //Go through all marks at the current position.
        for(HistoryNode node = (current != null ? current.prev : last); node != null && node.stickerid == -1; node = node.prev)
            if(node.stickerid == -1 && (mark == MARK_ANY || node.mark == mark))
                return true;
        return false;
    }

    public boolean atMacroOpen() {
        return atMark(MARK_MACRO_OPEN);
    }
    public boolean atMacroClose() {
        return atMark(MARK_MACRO_CLOSE);
    }
    public boolean atScrambleBoundary() {
        return atMark(MARK_SCRAMBLE_BOUNDARY);
    }

    private HistoryNode findMark(char mark, boolean backwards) {
        HistoryNode n = current == null ? last : current;
        while(n != null && n.mark != mark)
            n = backwards ? n.prev : n.next;
        return n;
    }

    public void undoToMark(char mark) {
        Assert(findMark(mark, true) != null);
        while(stepBackwardsTowardsMark(mark) != null)
            ;
    }

    private MagicCube.TwistData stepBackwardsTowardsMark(int mark) {
        // Continue searching backwards for a potential undo
        for(HistoryNode node = getPrevious(); node != null; node = node.prev)
            if(node.stickerid == -1 && node.mark == mark)
                return undo();
        return null;
    }

    private MagicCube.TwistData stepForwardsTowardsMark(int mark) {
        // Search forwards for a potential redo
        for(HistoryNode node = current; node != null; node = node.next)
            if(node.stickerid == -1 && node.mark == mark)
                return redo();
        return null;
    }

    /**
     * Executes an undo or redo.
     * If forward_first is true and we are not at the mark, then search
     * forward first and search backward only if the mark is not ahead of us.
     * Otherwise, search backward first and search forward only if
     * the mark is not behind us. Being able to choose which way to go
     * first makes it useful to have multiple marks with the same identifier.
     * 
     * @return resulting twist if successful, null if already at the mark or no such mark.
     */
    public MagicCube.TwistData stepTowardsMark(int mark, boolean forward_first) {
        if(atMark(mark))
            return null; // already at the mark
        MagicCube.TwistData status;
        if(!forward_first) {
            status = this.stepBackwardsTowardsMark(mark);
            if(status != null)
                return status;
        }
        status = this.stepForwardsTowardsMark(mark);
        if(status != null)
            return status;
        if(forward_first)
            status = this.stepBackwardsTowardsMark(mark);
        return status;
    }


    //
    // I/O METHODS
    //

    @Override
    public String toString() {
        Assert(isSane());
        int nwritten = 0;
        StringBuilder sb = new StringBuilder();
        for(HistoryNode node = first; node != null; node = node.next) {
            if(node == current)
                sb.append("c ");
            if(node.stickerid >= 0) {
                sb.append("" + node.stickerid);
                sb.append("," + node.dir);
                sb.append("," + node.slicesmask);
            }
            else
                sb.append("m" + node.mark);
            nwritten++;
            if(node.next != null) {
                if(nwritten % 10 == 0) // write a line break
                    sb.append(System.getProperty("line.separator"));
                else
                    sb.append(" ");
            }
        }
        sb.append("."); // end of history marker
        return sb.toString();
    }

    public boolean read(PushbackReader pr) {
        HistoryNode who_will_point_to_current = null;
        clear();
        try {
            while(true) {
                int c;
                while((c = pr.read()) != -1 && Character.isWhitespace(c))
                    ;
                if(c == -1)
                    return outahere(); // premature end of file
                if(c == '.')
                    break; // end of history
                if(Character.isDigit(c)) { // read a node
                    pr.unread(c);
                    int sticker = readInt(pr);
                    if(pr.read() != ',')
                        return outahere();
                    int direction = readInt(pr);
                    if(pr.read() != ',')
                        return outahere();
                    int slicesmask = readInt(pr);
                    append(sticker, direction, slicesmask);
                } else if(c == 'm') {
                    c = pr.read();
                    mark((char) c);
                } else if(c == 'c') {
                    who_will_point_to_current = last == null ? first : last.next;
                } else {
                    System.out.println("bad hist char " + c);
                    return outahere();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            return outahere();
        }
        current = who_will_point_to_current == null ? (last == null ? null : last.next) : who_will_point_to_current.next;
        fireCurrentChanged();
        return true;
    } // end read

    /**
     * Reads a simple decimal integer.
     * 
     * @param pr PushbackReader to read from.
     * @return integer value of parsed number.
     * @throws NumberFormatException
     */
    public static int readInt(PushbackReader pr) throws NumberFormatException {
        char buf[] = new char[10];
        int c, chars = 0;
        try {
            // check the first char for negative sign
            do {
                c = pr.read();
            } while(Character.isWhitespace(c)); // skip whitespace
            if(c == '-')
                buf[chars++] = '-';
            else
                pr.unread(c);
            // read the digits
            while((c = pr.read()) != -1 && Character.isDigit(c))
                // read digits
                buf[chars++] = (char) c;
            pr.unread(c);
        } catch(Exception ioe) {
            throw new NumberFormatException("Read error in History.readInt");
        }
        // convert the string to an integer
        String numstr = new String(buf, 0, chars);
        return Integer.parseInt(numstr);
    }

    private boolean outahere() {
        //System.err.println("Error reading history-- no history read");
        //Thread.dumpStack();
        clear();
        return false;
    }

    private boolean isSane() {
        boolean found_current = false;
        Assert((first == null) == (last == null));
        if(first != null) {
            for(HistoryNode node = first; node != null; node = node.next) {
                if(node.prev != null)
                    Assert(node.prev.next == node);
                else
                    Assert(first == node);
                if(node.next != null)
                    Assert(node.next.prev == node);
                else
                    Assert(last == node);
                if(node == current)
                    found_current = true;
                if(node.stickerid >= 0) {
                    //Assert(0 <= node.stickerid && node.stickerid < MagicCube.NGRIPS); // TODO: fix this
                    Assert(node.dir == MagicCube.CCW || node.dir == MagicCube.CW);
                }
            }
        }
        Assert(found_current == (current != null));
        return true;
    } // end isSane


    //
    // LISTENER SUPPORT
    //
    public static interface HistoryListener {
        public void currentChanged();
    }
    private HistoryListener historyListener;
    public void setHistoryListener(HistoryListener listener) {
        historyListener = listener;
    }
    protected void fireCurrentChanged() {
        if(DEBUG)
            System.out.println(this);
        if(historyListener != null)
            historyListener.currentChanged();
    }


    /**
     * Converts a list of twists into an equivalent and possibly shorter list.
     * 
     * @param inmoves input array of moves to compress.
     * @param len edge edgeLength of the puzzle. Note: <i>not</i> the edgeLength of the moves array.
     * @return possibly reduced list of moves that produce the same effect as the input moves.
     */
    public static MagicCube.TwistData[] compress(MagicCube.TwistData[] inmoves, int len, boolean sweepRotatesForward) {
        History hist = new History(len);
        for(int i = 0; i < inmoves.length; i++)
            hist.append(inmoves[i]);
        hist.compress(sweepRotatesForward);
        return hist.movesArray();
    }

    /**
     * Reverses both the order of the history moves and their directions.
     * 
     * Note: Also kills current, if any, just due to laziness.
     */
    public void reverse() {
        if(first == null)
            return;
        current = null; // so as to not fire change event
        HistoryNode origFirst = first;
        origFirst.dir *= -1; // the other nodes get reversed below but don't forget this one!
        int count = countMoves(false);
        for(int i = 0; i < count - 1; i++) {
            HistoryNode lastLast = last;
            deleteLast();
            insertNode(origFirst, lastLast.stickerid, -lastLast.dir, lastLast.slicesmask, lastLast.mark);
        }
    }

    /**
     * Meant to squeeze out all redundancies and filler.
     * This is usually done in preparation for a "cheat" solve.
     * 
     * <li>Truncate (i.e. delete everything past current),</li> <li>Remove non-moves (marks),</li> <li>Merge same-face twists,</li> <li>Sweeping rotates to the beginning.</li>
     * 
     * If sweepRotatesForward is set, does it in the opposite
     * direction. In this case it's assumed to not be a real history,
     * and current is not allowed to be set.
     */
    public void compress(boolean sweepRotatesForward) {
        // TODO: Uncomment the body below and fix.
    }
//
//    	//int startCount = this.countMoves(false);
//    	
//        if (sweepRotatesForward)
//        {
//            Assert(current == null);
//            reverse();
//            compress(false);
//            reverse();
//            return;
//        }
//
//        /*
//         * Truncate
//         */
//        truncate();
//
//        /*
//         * Remove all non-moves
//         */
//        for (HistoryNode node = first; node != null; node = node.next)
//            if (node.stickerid == -1)
//                deleteNode(node);
//
//        /*
//         * Traverse from end to beginning,
//         * constructing a new list of "mega-moves".
//         * Each mega-move is a set of twists on parallel slices.
//         * Sweep the current rotation towards the beginning as we go.
//         */
//        MagicCube.Stickerspec scratchGrip = new MagicCube.Stickerspec(); // scratch
//        int scratchMat[][] = new int[4][4];
//        int scratchCoords[] = new int[4];
//
//        class MegaMove {
//            public int face; // must be less than its opposite
//            public int sliceTwistMats[][][] = new int[edgeLength][4][4];
//            public MegaMove(int face, int edgeLength)
//            {
//                this.face = face;
//                this.sliceTwistMats = new int[edgeLength][4][4];
//                for (int i = 0; i < edgeLength; ++i)
//                    Vec_h._IDENTMAT4(sliceTwistMats[i]);
//            }
//            public String toString()
//            {
//                return "{face="+face+"...}";
//            }
//        };
//        int current_matrix[][] = new int[4][4];
//        Vec_h._IDENTMAT4(current_matrix);
//        LinkedList<MegaMove> megaMoves = new LinkedList<MegaMove>();
//        for (HistoryNode node = last; node != null; node = node.prev)
//        {
//            int stickerid = node.stickerid;
//            int slicesmask = node.slicesmask;
//            int dir = node.dir;
//
//            //
//            // Figure out the grip
//            //
//            MagicCube.Stickerspec grip = scratchGrip;
//            grip.id_within_puzzle = stickerid;
//            //PolygonManager.fillStickerspecFromIdAndLength(grip, 3); // XXX crashes with non cubes
//
//            //
//            // Transform the move by current_matrix,
//            // by applying current_matrix to the coords of the grip,
//            // then get the new stickerid back out of it.
//            //
//            {
//                Vec_h._VXM4i(grip.coords, grip.coords, current_matrix);
//                PolygonManager.fillStickerspecFromCoordsAndLength(grip, 3);
//                stickerid = grip.id_within_puzzle;
//            }
//
//            int face = PolygonManager.faceOfGrip(stickerid);
//            int oppositeFace = PolygonManager.oppositeFace(face);
//
//            //
//            // See if this move can be part of the first megamove.
//            // If not, insert a new megamove for it.
//            //
//            MegaMove firstMegaMove = (megaMoves.isEmpty() ? null
//                                             : megaMoves.getFirst());
//            if (firstMegaMove == null
//             || (face != firstMegaMove.face
//              && oppositeFace != firstMegaMove.face))
//            {
//                // Can't combine with the existing first megamove,
//                // so make a new one.
//                firstMegaMove = new MegaMove(Math.min(face, oppositeFace),
//                                             edgeLength);
//                megaMoves.addFirst(firstMegaMove);
//            }
//
//            //
//            // Twist this move's slices on the megamove
//            //
//            float angle = PolygonManager.getTwistTotalAngle(grip.dim, dir);
//            Math4d.get4dTwistMat(grip.coords, angle, scratchMat);
//            for (int iSlice = 0; iSlice < edgeLength; ++iSlice)
//            {
//                if (((slicesmask>>iSlice)&1) != 0)
//                {
//                    int iSliceCanonical = (oppositeFace<face ? edgeLength-1-iSlice
//                                                             : iSlice);
//                    Vec_h._MXM4i(firstMegaMove.sliceTwistMats[iSliceCanonical],
//                                 scratchMat,
//                                 firstMegaMove.sliceTwistMats[iSliceCanonical]);
//                }
//            }
//
//            //
//            // The slices now vote on what rotation to factor out
//            // of the megamove.
//            // If they all agree, then it's
//            // a pure rotation in which case the mega-move turns
//            // into a no-op, which can be removed.
//            //
//            if (true)
//            {
//                int winnerVotes = -1;
//                int winnerSlice = -1;
//                for (int iSlice = 0; iSlice < edgeLength; ++iSlice)
//                {
//                    int nSameAsISlice = 1;
//                    for (int jSlice = iSlice+1; jSlice < edgeLength; ++jSlice)
//                        if (Vec_h._EQMAT4(firstMegaMove.sliceTwistMats[jSlice],
//                                          firstMegaMove.sliceTwistMats[iSlice]))
//                            nSameAsISlice++;
//                    if (nSameAsISlice > winnerVotes)
//                    {
//                        winnerVotes = nSameAsISlice;
//                        winnerSlice = iSlice;
//                    }
//                }
//                Vec_h._MXM4i(current_matrix,
//                             current_matrix,
//                             firstMegaMove.sliceTwistMats[winnerSlice]);
//                Vec_h._TRANSPOSE4(scratchMat, firstMegaMove.sliceTwistMats[winnerSlice]); // inverse
//                for (int iSlice = 0; iSlice < edgeLength; ++iSlice)
//                    Vec_h._MXM4i(firstMegaMove.sliceTwistMats[iSlice],
//                                 scratchMat,
//                                 firstMegaMove.sliceTwistMats[iSlice]);
//                if (winnerVotes == edgeLength)
//                {
//                    megaMoves.removeFirst(); // it was a pure rotation
//                    firstMegaMove = null;
//                }
//            }
//        }
//
//        //
//        // The proper thing to do now would be to add the rotate(s)
//        // representing current_matrix
//        // to the beginning of the mega-moves, but I'm not bothering,
//        // since the only thing this function is used for anyway
//        // is the cheat-solve.
//        //
//
//        //
//        // Convert the mega-moves back into twists...
//        //
//        clear();
//        int scratchPermutation[] = new int[edgeLength];
//        while (!megaMoves.isEmpty())
//        {
//            // So we look at slices in a random order
//            randomPermutation(scratchPermutation);
//
//            MegaMove firstMegaMove = (MegaMove)megaMoves.removeLast();
//            int face = firstMegaMove.face;
//            for (int _iSlice = 0; _iSlice < edgeLength; ++_iSlice)
//            {
//                int iSlice = scratchPermutation[_iSlice];
//                int sliceTwistMat[][] = firstMegaMove.sliceTwistMats[iSlice];
//                if (!Vec_h._ISIDENTMAT4(sliceTwistMat))
//                {
//                    int slicesmask = 1<<iSlice;
//                    for (int jSlice = 0; jSlice < edgeLength; ++jSlice)
//                    {
//                        if (Vec_h._EQMAT4(firstMegaMove.sliceTwistMats[jSlice],
//                                          sliceTwistMat))
//                        {
//                            slicesmask |= 1<<jSlice;
//                            // Clear it so we don't do it again
//                            // as an iSlice later.  But do NOT clear [iSlice],
//                            // since sliceTwistMat is still pointing to it.
//                            if (jSlice != iSlice)
//                                Vec_h._IDENTMAT4(firstMegaMove.sliceTwistMats[jSlice]);
//                        }
//                    }
//
//                    /*
//                     * Figure out how to accomplish this rotation
//                     * of the slices with one or two twists on a single grip.
//                     */
//
//                    /*
//                     * Find a sticker on this face that's not
//                     * moved by the matrix; that will be the grip of the
//                     * concatenated move.
//                     */
//                    MagicCube.Stickerspec grip = scratchGrip;
//                    grip.face = face;
//                    for (grip.id_within_face = 0;
//                         grip.id_within_face < MagicCube.GRIPS_PER_FACE; // TODO: need way to know how many 
//                         grip.id_within_face++)
//                    {
//                        PolygonManager.fillStickerspecFromFaceAndIdAndLength(grip, 3);
//                        if (grip.dim >= 3)
//                            continue;
//                        int newcoords[] = scratchCoords;
//                        Vec_h._VXM4(newcoords, grip.coords, sliceTwistMat);
//                        if (Vec_h._EQVEC4(newcoords, grip.coords)) {
//                            /*
//                             * Found the right grip;
//                             * see if any of the following work:
//                             *  0 twists
//                             *  1 twist CW
//                             *  1 twist CCW
//                             *  2 twists in random direction
//                             */
//                            int testmat[][] = scratchMat;
//                            Vec_h._IDENTMAT4(testmat);
//                            if (Vec_h._EQMAT4(testmat, sliceTwistMat)) {
//                                /*
//                                 * Result is 0 twists.
//                                 */
//                                break;
//                            }
//
//                            float angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CCW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            if (Vec_h._EQMAT4(testmat, sliceTwistMat)) {
//                                /*
//                                 * Result is 1 twist CCW.
//                                 */
//                                insertNode(first, grip.id_within_puzzle, MagicCube.CCW, slicesmask);
//                                break;
//                            }
//                            angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            if (Vec_h._EQMAT4(testmat, sliceTwistMat)) {
//                                /*
//                                 * Result is 1 twist CW.
//                                 */
//                                insertNode(first, grip.id_within_puzzle, MagicCube.CW, slicesmask);
//                                break;
//                            }
//                            int dir = Math.random() > 0.5 ? MagicCube.CW : MagicCube.CCW;
//                            angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CCW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            Vec_h._MXM4i(testmat, testmat, testmat);
//                            if (Vec_h._EQMAT4(testmat, sliceTwistMat)) {
//                                // Result is 2 twists
//                                insertNode(first, grip.id_within_puzzle, dir, slicesmask);
//                                insertNode(first, grip.id_within_puzzle, dir, slicesmask);
//                                break;
//                            }
//                            Assert(false);
//                        }
//                    }
//                    Assert(grip.id_within_face < 3 * 3 * 3);
//                }
//            }
//        }
//        //int endCount = this.countMoves(false);
//        //System.out.println("compressed " + startCount+ " twist sequence to " + endCount + " (" + (startCount - endCount)*100f/startCount + "%)");
//    } // end compress
//    
//
//    private static void randomPermutation(int perm[])
//    {
//        for (int i = 0; i < perm.length; ++i)
//            perm[i] = i;
//        for (int i = perm.length-1; i >= 0; --i)
//        {
//            int j = (int)(Math.random()*(i+1)); // in [0..i]
//            if (j != i)
//            {
//                int temp = perm[i];
//                perm[i] = perm[j];
//                perm[j] = temp;
//            }
//        }
//    }
//
//    
//    public void oldcompress() {
//        // TODO: perform on the fly, as the cheat-solve is happening, otherwise long wait if the history is long.
//        HistoryNode node, nodeptr;
//        int
//            current_matrix[][] = new int[4][4],
//            incmat[][] = new int[4][4],
//            testmat[][] = new int[4][4],
//            newcoords[] = new int[4];
//        MagicCube.Stickerspec grip = new MagicCube.Stickerspec();
//        int face, dir, thisface, nextface, temp;
//
//        /*
//         * Truncate
//         */
//        truncate();
//
//        /*
//         * Remove all non-moves
//         */
//        for (nodeptr = first; nodeptr!=null; nodeptr=nodeptr.next) {
//            if (nodeptr.stickerid == -1)
//                deleteNode(nodeptr);
//        }
//
//        /*
//         * Sweep all rotates to beginning
//         */
//        Vec_h._IDENTMAT4(current_matrix);
//        for (nodeptr=last; nodeptr!=null; nodeptr=nodeptr.prev) {
//            if (isRotate(nodeptr.slicesmask)) {
//                /*
//                * It's a rotate.  Just preconcatenate it
//                * to the current matrix and remove.
//                */
//                grip.id_within_puzzle = (nodeptr).stickerid;
//                PolygonManager.fillStickerspecFromIdAndLength(grip, 3);
//                float angle = PolygonManager.getTwistTotalAngle(grip.dim, (nodeptr).dir);
//                Math4d.get4dTwistMat(grip.coords, angle, incmat);
//                Vec_h._MXM4i(current_matrix, incmat, current_matrix);
//                deleteNode(nodeptr);
//            } else {
//                /*
//                * It's a twist (some slices stay and some move).
//                * Apply the current matrix to the coords of
//                * the grip.
//                */
//                grip.id_within_puzzle = (nodeptr).stickerid;
//                PolygonManager.fillStickerspecFromIdAndLength(grip, 3);
//                Vec_h._VXM4i(grip.coords, grip.coords, current_matrix);
//                PolygonManager.fillStickerspecFromCoordsAndLength(grip, 3);
//                (nodeptr).stickerid = grip.id_within_puzzle;
//            }
//        }
//        /*
//         * The proper thing to do now would be to add the rotates
//         * to the beginning of the history, but I'm not bothering,
//         * since the only thing this function is used for anyway
//         * is the cheat-solve.
//         */
//
//        /*
//         * Put opposite-face twists in canonical order,
//         * which can put some same-face twists together for the next pass.
//         */
//        for (node = first; node!=null; ) {
//            if (node.slicesmask == 1 && node.next!=null && node.next.slicesmask == 1) {
//                thisface = PolygonManager.faceOfGrip(node.stickerid);
//                nextface = PolygonManager.faceOfGrip(node.next.stickerid);
//                if (nextface < thisface && nextface == PolygonManager.oppositeFace(thisface)) {
//                    temp=node.stickerid; node.stickerid=node.next.stickerid; node.next.stickerid=temp;     // swap
//                    temp=node.dir; node.dir=node.next.dir; node.next.dir=temp;                             // swap
//                    temp=node.slicesmask; node.slicesmask=node.next.slicesmask; node.next.slicesmask=temp; // swap
//                    // XXX wtf is the following doing?? doesn't hurt, but I don't understand what it does -Don
//                    if (node.prev != null)
//                        node = node.prev;
//                }
//                else
//                    node = node.next;
//            }
//            else
//                node = node.next;
//        }
//
//        /*
//         * Merge same-face twists
//         */
//        HistoryNode first_on_this_face, past_last_on_this_face;
//        for (first_on_this_face = first; first_on_this_face!=null; ) {
//            if (first_on_this_face.slicesmask == 1) {
//                face = PolygonManager.faceOfGrip(first_on_this_face.stickerid);
//                past_last_on_this_face = first_on_this_face.next;
//                while (past_last_on_this_face!=null &&
//                       past_last_on_this_face.slicesmask == 1 &&
//                       PolygonManager.faceOfGrip(past_last_on_this_face.stickerid) == face)
//                {
//                    past_last_on_this_face = past_last_on_this_face.next;
//                }
//
//                if (past_last_on_this_face != first_on_this_face.next) {
//                    /*
//                     * There is more than one twist on this face.
//                     * Concatenate together all the matrices of these
//                     * twists, and then figure out how to accomplish it
//                     * with one or two twists on a single grip.
//                     */
//                    Vec_h._IDENTMAT4(current_matrix);
//                    for (node=first_on_this_face; node!=past_last_on_this_face; node=node.next) {
//                        grip.id_within_puzzle = node.stickerid;
//                        PolygonManager.fillStickerspecFromIdAndLength(grip, 3);
//                        float angle = PolygonManager.getTwistTotalAngle(grip.dim, node.dir);
//                        Math4d.get4dTwistMat(grip.coords, angle, incmat);
//                        Vec_h._MXM4i(current_matrix, current_matrix, incmat);
//                    }
//
//                    /*
//                     * We now have all the information we need;
//                     * delete the twists from the history
//                     */
//                    while (first_on_this_face != past_last_on_this_face) {
//                        deleteNode(first_on_this_face);
//                        first_on_this_face = first_on_this_face.next;
//                    }
//
//                    /*
//                     * Find a sticker on this face that's not
//                     * moved by the matrix; that will be the grip of the
//                     * concatenated move.
//                     */
//                    grip.face = face;
//                    for (grip.id_within_face = 0;
//                         grip.id_within_face < MagicCube.GRIPS_PER_FACE;
//                         grip.id_within_face++)
//                    {
//                        PolygonManager.fillStickerspecFromFaceAndIdAndLength(grip, 3);
//                        if (grip.dim >= 3)
//                            continue;
//                        Vec_h._VXM4(newcoords, grip.coords, current_matrix);
//                        if (Vec_h._EQVEC4(newcoords, grip.coords)) {
//                            /*
//                             * Found the right grip;
//                             * see if any of the following work:
//                             *  0 twists
//                             *  1 twist CW
//                             *  1 twist CCW
//                             *  2 twists in random direction
//                             */
//                            Vec_h._IDENTMAT4(testmat);
//                            if (Vec_h._EQMAT4(testmat, current_matrix)) {
//                                /*
//                                 * Result is 0 twists.
//                                 */
//                                break;
//                            }
//
//                            float angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CCW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            if (Vec_h._EQMAT4(testmat, current_matrix)) {
//                                /*
//                                 * Result is 1 twist CCW.
//                                 */
//                                insertNode(past_last_on_this_face, grip.id_within_puzzle, MagicCube.CCW, 1);
//                                break;
//                            }
//                            angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            if (Vec_h._EQMAT4(testmat, current_matrix)) {
//                                /*
//                                 * Result is 1 twist CW.
//                                 */
//                                insertNode(past_last_on_this_face, grip.id_within_puzzle, MagicCube.CW, 1);
//                                break;
//                            }
//                            dir = Math.random() > 0.5 ? MagicCube.CW : MagicCube.CCW;
//                            angle = PolygonManager.getTwistTotalAngle(grip.dim, MagicCube.CCW);
//                            Math4d.get4dTwistMat(grip.coords, angle, testmat);
//                            Vec_h._MXM4i(testmat, testmat, testmat);
//                            if (Vec_h._EQMAT4(testmat, current_matrix)) {
//                                // Result is 2 twists
//                                insertNode(past_last_on_this_face, grip.id_within_puzzle, dir, 1);
//                                insertNode(past_last_on_this_face, grip.id_within_puzzle, dir, 1);
//                                break;
//                            }
//                            Assert(false);
//                        }
//                    }
//                    Assert(grip.id_within_face < 3 * 3 * 3);
//                }
//                first_on_this_face = past_last_on_this_face;
//            }
//            else
//                first_on_this_face = first_on_this_face.next;
//        }
//    }  // end oldcompress


    /**
     * Simple example test main.
     */
    public static void main(String args[]) {
        String sep = System.getProperty("line.separator");
        History hist = new History(3);
        hist.append(1, 1, -1);
        hist.append(30, -1, 2);
        hist.append(100, 1, 1);
        try {
            System.out.println("before:");
            System.out.println(hist);
            OutputStreamWriter osw = new OutputStreamWriter(System.out);

            osw.write(hist.toString() + sep);
            osw.flush();
            //hist.reverse();
            //osw.write(hist.toString() + sep);
            //osw.flush();

            FileWriter fw;
            fw = new FileWriter("test.txt");
            fw.write(hist.toString() + sep);
            fw.close();

            Reader fr = new FileReader("test.txt");
            PushbackReader pr = new PushbackReader(fr);
            hist.read(pr);
            pr.close();
            fr = pr = null;

            System.out.println("after write and read back:");
            osw.write(hist.toString() + sep);
            osw.flush();

            System.out.println("reversed:");
            hist.reverse();
            System.out.println(hist);
            osw.write(hist.toString() + sep);
            osw.flush();

            System.out.println("twice reversed:");
            hist.reverse();
            System.out.println(hist);
            osw.write(hist.toString() + sep);
            osw.flush();

            System.out.println("thrice reversed:");
            hist.reverse();
            System.out.println(hist);
            osw.write(hist.toString() + sep);
            osw.flush();
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    } // end main()

}
