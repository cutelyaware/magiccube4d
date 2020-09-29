// 2 # 1 "com/donhatchsw/util/MyPanel.prejava"
// 3 # 1 "<built-in>"
// 4 # 1 "<command line>"
// 5 # 1 "com/donhatchsw/util/MyPanel.prejava"
//
// MyPanel.java
//

package com.donhatchsw.util;

import java.awt.*;

/**
 * A convenient way to make a java.awt.Panel with a GridBagLayout.
 * <p>
 * Invoke it like this:
 * <pre>
 *      new MyPanel(new Object[][][] {
 *          { // first row...
 *              {new Label("01")},
 *              {new Label("02"), anUnusualGridBagConstraints},
 *              {new Label("03"), anotherUnusualGridBagConstraints},
 *          },
 *          { // second row...
 *              {new Label("11")},
 *              {new Label("12")},
 *              {new Label("13")},
 *          }
 *      });
 * </pre>
 * For a more colorful example, see main() in the source code.
 * <p>
 * NOTE: The gridx and gridy fields of any passed-in constraints
 * are considered to be scratch and may get modified.
 * <p>
 * NOTE for indenting: a label with string "" (no spaces)works on Linux and Windows,
 * but doesn't indent at all on the Mac.
 * The string "   " (three spaces) works better,
 * but unfortunately is overkill on Linux and possibly Windows.
 * <p>
 * XXX JAVADOC There are also Row and Col classes; see the source file.  They are probably even simpler to use.
 */
public class MyPanel extends Panel
{
    public MyPanel(Object cells[][][/*2 or 1*/])
    {
        this.setLayout(new GridBagLayout());

        GridBagConstraints defaultConstraints = new GridBagConstraints();
        defaultConstraints.anchor = GridBagConstraints.WEST;

        int i;
        for (i = 0; i < cells.length; ++i)
        {
            int j;
            for (j = 0; j < cells[i].length; ++j)
            {
                if (cells[i][j] != null
                 && cells[i][j].length > 0
                 && cells[i][j][0] != null)
                {
                    Component component =
                        (cells[i][j][0] instanceof String
                            ? new Label((String)cells[i][j][0])
                            : (Component)cells[i][j][0]);
                    GridBagConstraints constraints =
                        (cells[i][j].length > 1
                      && cells[i][j][1] != null
                            ? (GridBagConstraints)cells[i][j][1]
                            : defaultConstraints);
                    constraints.gridy = i;
                    constraints.gridx = j;
                    this.add(component, constraints);
                }
            }
        }
    }

    public MyPanel(String sideLabel, Component component, GridBagConstraints c)
    {
        this(new Object[][][] {
            {
                {sideLabel}, // first item in row
                {component, c}, // second item in row
            }
        });
    }
    public MyPanel(String sideLabel, Object cells[][][], GridBagConstraints c)
    {
        this(sideLabel, new MyPanel(cells), c);
    }
    public MyPanel(String topLabel, String sideLabel, Object cells[][][], GridBagConstraints c)
    {
        this(new Object[][][] {
            {{topLabel}}, // first row
            {{new MyPanel(sideLabel, cells, c), c}} // second row
        });
    }


    public MyPanel(String sideLabel, Component component)
    {
        this(sideLabel, component, (GridBagConstraints)null);
    }
    public MyPanel(String sideLabel, Object cells[][][])
    {
        this(sideLabel, cells, (GridBagConstraints)null);
    }
    public MyPanel(String topLabel, String sideLabel, Object cells[][][])
    {
        this(topLabel, sideLabel, cells, (GridBagConstraints)null);
    }


    /**
        Test program, creates the following panel
        in a single statement:
        <pre>
        +----------------------------+
        |I:                          |
        | -------------------------- |
        |  |A:                       |
        |  | ---------------------   |
        |  |   |1: | aa | b | c      |
        |  |   |-----------------    |
        |  |   |2: | d | eee | f     |
        |  |-----------------------  |
        |  |B:                       |
        |  | ---------------------   |
        |  |   |1: | aa              |
        |  |   |-----------------    |
        |  |   |2: | a | b           |
        |----------------------------|
        |II:                         |
        | -------------------------- |
        |  |M:                       |
        |  |-----------------------  |
        |  |  |m00|m01               |
        |  |  |---+---------------   |
        |  |  |m10|m11               |
        +----------------------------+
        </pre>
    */

    public static void main(String args[])
    {
        GridBagConstraints constraintsBOTH = new GridBagConstraints();
        constraintsBOTH.fill = GridBagConstraints.BOTH;

        MyPanel myPanel = new Col(new Object[][]{
            {new Col("I:"," ", new Object[][] {
                {new Col("A:"," ", new Object[][] {
                    {new Row("1:", new Object[][] {
                        {new Button("aa")},
                        {new Button("b")},
                        {new Button("c")},
                    })},
                    {new Row("2:", new Object[][] {
                        {new Button("d")},
                        {new Button("eee")},
                        {new Button("f")},
                    })},
                })},
                {new Col("B:"," ", new Object[][] {
                    {new Row("1:", new Object[][] {
                        {new Button("aa")},
                    })},
                    {new Row("2:", new Object[][] {
                        {new Button("a")},
                        {new Button("b")},
                    })},
                })},
            })},
            {new Col("II:", " ", new Object[][] {
                {new MyPanel("M:"," ", new Object[][][] {
                   {
                       {new Button("m00"),constraintsBOTH},
                       {new Button("m01"),constraintsBOTH},
                   },
                   {
                       {new Button("0"),constraintsBOTH},
                       {new Button("m11"),constraintsBOTH},
                   },
                })},
            })},
        });

        Frame frame = new Frame("MyPanel Test");
        frame.add(myPanel);
        frame.pack();
        frame.show();
    } // main

} // class MyPanel


/**
 * Okay, experimenting with new layout idea...
 * It might be like PsStackLayout, not sure.
 *
 * XXX JAVADOC GROUP
 * XXX Row and Col need to be moved out into their own files and made public
 */
class Col
    extends MyPanel
{
    public Col(Object[][/*2*/] componentsAndConstraints)
    {
        // expand each item into a singleton row containing only it
        super(expandIntoSingletons(componentsAndConstraints));
    }
    public Col(String topLabel, String sideLabel, Object[][/*2*/] componentsAndConstraints)
    {
        super(topLabel, sideLabel, expandIntoSingletons(componentsAndConstraints));
    }
        private static Object[][][/*2*/] expandIntoSingletons(Object[][/*2*/] c2)
        {
            int n = c2.length;
            Object[][][] c3 = new Object[n][1][];
            for (int i = 0; i < n; ++i)
                c3[i][0] = c2[i];
            return c3;
        }
} // class Col

class Row
    extends MyPanel
{
    public Row(Object[][/*2*/] componentsAndConstraints)
    {
        // expand into a grid with only one row
        super(new Object[][][] {componentsAndConstraints});
    }
    public Row(String sideLabel, Object[][/*2*/] componentsAndConstraints)
    {
        super(sideLabel, new Object[][][] {componentsAndConstraints});
    }
} // class Row
