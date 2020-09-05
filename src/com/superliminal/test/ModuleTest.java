package com.superliminal.test;

import com.superliminal.magiccube4d.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;


public class ModuleTest
{
    public static void main(String args[])
    {
        try
        {
            testInternal(MagicCube.SUPPORTED_PUZZLES);
        } catch(Exception e)
        {
            System.out.println("Module test failed: " + e);
            e.printStackTrace();
        }
    }

    private static void testInternal(String puzzles[][]) throws IOException
    {
        Writer writer = new FileWriter("test/puzzleBuildTest.ref");
        String sep = System.getProperty("line.separator");

        for(int i = 0; i < puzzles.length; ++i)
        {
            final String schlafli = puzzles[i][0];
            if(schlafli == null || puzzles[i].length < 2)
                continue;
            String lengthStrings[] = puzzles[i][1].split(",");

            for(int j = 0; j < lengthStrings.length; ++j)
            {
                final String lengthString = lengthStrings[j];
                double len = Double.parseDouble(lengthString);

                String puzzleString = "" + schlafli + " " + len;
                System.out.println(puzzleString);
                PolytopePuzzleDescription puzzle = new PolytopePuzzleDescription(schlafli, len, null);
                writer.write("Puzzle:\t" + puzzleString + sep);
                writer.write("NumFaces:\t" + puzzle.nFaces() + sep);
                writer.write("NumCubies:\t" + puzzle.nCubies() + sep);
                writer.write("NumStickers:\t" + puzzle.nStickers() + sep);
                writer.write("NumGrips:\t" + puzzle.nGrips() + sep);
                writer.write(sep);

                /*
                 * Not part of the test output at this point,
                 * but was useful for testing.
                 * for( int s=0; s<puzzle.nStickers(); s++ )
                 * {
                 * int stickerGrips[] = PipelineUtils.getGripsForSticker( s, puzzle );
                 * if( stickerGrips.length == 1 )
                 * System.out.print( "" + stickerGrips[0] + " " );
                 * else
                 * System.out.print( "" + stickerGrips.length );
                 * }
                 * System.out.println( "" );
                 */
            }
        }

        writer.close();
    }
}
