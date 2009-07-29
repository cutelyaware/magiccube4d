import java.net.*;
import java.io.*;

//
//NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
//AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/**
 * Contains static I/O methods for dealing with resources.
 * 
 * Copyright 2005 - Superliminal Software
 * @author Melinda Green
 */
public class Util {
    private Util() {
    }

    /**
     * @param name is either a fully qualified url path or a relative file path.
     * @return URL version of name if a fully qualified url path otherwise a url
     * relative to name and expected to be found in the classpath.
     */
    public static URL getResource(String name) {
        if(name == null)
            return null;
        URL url = null;
        try {
            if(name.indexOf(':') == -1) {
                url = Util.class.getClassLoader().getResource(name);
            }
            else
                url = new URL(name);
        }
        catch(Exception e){
            System.err.println("IconUtils.getResource: can't load resource: " + name);
        }
        return url;
    }
    
    public static String readFileFromURL(URL url) {
        StringBuffer fBuf = new StringBuffer();
        try {
            InputStream in=url.openStream (); // Open a stream to the file using the URL.
            BufferedReader dis = new BufferedReader (new InputStreamReader (in));
            String line;
            while ( (line = dis.readLine ()) != null)
                fBuf.append(line).append(System.getProperty("line.separator"));
            in.close ();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return fBuf.toString();
    }
}
