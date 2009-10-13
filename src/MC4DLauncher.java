import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * A shim class whose only purpose is to launch the real main class but with VM arguments
 * that allow for a large enough heap space to handle the largest puzzles. This could be
 * a really dumb way to do this but I don't see another way using only a single executable
 * jar file. Might also fail on non Windows systems. I guess we'll see.
 * 
 * @author Melinda Green
 */
public class MC4DLauncher {
	public static void main(String args[]) throws IOException, URISyntaxException{
		String thisjar = new File(MC4DLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString();
		if(thisjar.endsWith(".jar")) {
			System.out.println("Jar: " + thisjar);
			Runtime.getRuntime().exec("java -Xms128m -Xmx512m -cp " + thisjar + " MC4DSwing");
		}
		else {
			System.out.println("Path: " + thisjar);
			MC4DSwing.main(args);
		}
	}
}
