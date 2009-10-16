import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ModuleTest
{
	public static void test( String puzzles[][] )
	{
		try
		{
			testInternal( puzzles );
		}
    	catch( Exception e )
    	{
    		System.out.println( "Module test failed." );
    	}
	}
	
	private static void testInternal( String puzzles[][] ) throws IOException
	{
        Writer writer = new FileWriter( "test/puzzleBuildTest.ref" ); 
        String sep = System.getProperty( "line.separator" );
		
        for( int i=0; i<puzzles.length; ++i )
        {
            final String schlafli = puzzles[i][0];
            if( schlafli == null || schlafli.indexOf("{3") != -1 )
            	continue;
            String lengthStrings[] = puzzles[i][1].split(",");
        	         
            for (int j=0; j<lengthStrings.length; ++j )
            {
            	final String lengthString = lengthStrings[j];
        		double len = Double.parseDouble( lengthString );
        		
        		PolytopePuzzleDescription puzzle = new PolytopePuzzleDescription( schlafli, len, null );
        		String puzzleString = "" + schlafli + " " + len;
        		System.out.println( puzzleString );
        		writer.write( "Puzzle:\t" + puzzleString + sep );
        		writer.write( "NumFaces:\t" + puzzle.nFaces() + sep );
        		writer.write( "NumCubies:\t" + puzzle.nCubies() + sep );
        		writer.write( "NumStickers:\t" + puzzle.nStickers() + sep );
        		writer.write( "NumGrips:\t" + puzzle.nGrips() + sep );
        		writer.write( sep );
            }
        }
        
        writer.close();
	}
}