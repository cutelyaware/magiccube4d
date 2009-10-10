import java.io.IOException;

/**
 * A shim class whose only purpose is to launch the real main class but with VM arguments
 * that allow for a large enough heap space to handle the largest puzzles. This could be
 * a really dumb way to do this but I don't see another way using only a single executable
 * jar file. Might also fail on non Windows systems. I guess we'll see.
 * 
 * Note: requires that the jar file be named exactly "MagicCube4D.jar".
 * 
 * @author Melinda Green
 */
public class MC4DLauncher {
	public static void main(String args[]) throws IOException{
		Runtime.getRuntime().exec("java -Xms128m -Xmx512m -cp MagicCube4D.jar MC4DSwing");
	}
}
