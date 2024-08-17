package com.superliminal.util;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * Title: PropertyManager
 * Description: Methods for getting property strings from cascading Properties objects.
 * The order of precedence is as follows: <code>
 *                   top
 *                   system
 *                   userprefs
 *                   vendorprops
 *                   defaults
 *               </code>
 * 
 * The root of the chain is stored in the public static "top" member which should be
 * accessed for nearly all application purposes. Applications can set values there
 * as well which will take precedence over lower-level defaults but will not be
 * persisted across sessions.
 * 
 * Property file names are assumed to be rooted in a folder named "resources"
 * expected to be found in the classpath. In that folder is expected to be
 * a property file named "defaults.prop". That file should contain default values
 * for all properties the application might request. This becomes the lowest level
 * Properties in a chain.
 * 
 * An optional vendor-supplied property file may also be provided in which
 * vendors may specify overrides for custom versions of a published sub-set of
 * the default properties such as branding logos, colors, etc. A vendor property
 * file must be specified as a URL in an environment variable with the key
 * "vendorprops". e.g. vendorprops=http://newcorp.com/analyer/resources/vendor.prop.
 * the values specified in this file will take precedence over the analyzer defaults.
 * Note that images and other file resources referred to by that file must be
 * begin with the '/' character and will then be looked for relative to the
 * directory containing the vendor property file.
 * e.g. given the vendor prop file path above and the property setting:
 * main.logo.small=/newcorp.gif
 * will resolve to:
 * newcorp.com/analyzer/resources/newcorp.gif
 * Also, quoted values will have their quotes stripped. This is in case a user
 * wants to create a path or other property that begins with '/'.
 * 
 * Another special properties object in the chain is "userprefs" which is for end
 * user preferences which are stored in a file on the user's local machine
 * in the same directory as the .jar or .class file being run.
 * Properties that are set into this object are immediately persisted to the file
 * for retrieval in the current and future sessions.
 * 
 * 
 * Copyright 2005 - Superliminal Software
 * 
 * @author Melinda Green
 */
@SuppressWarnings("serial")
public class PropertyManager extends Properties {

    public static interface PropertyListener {
        public void propertyChanged(String property, String newval);
    }
    private HashMap<PropertyListener, String[]> PropertyListeners = new HashMap<PropertyListener, String[]>();
    public void addPropertyListener(PropertyListener pl, String[] propnames) {
        PropertyListeners.put(pl, propnames);
    }
    public void addPropertyListener(PropertyListener pl, String propname) {
        PropertyListeners.put(pl, new String[]{propname});
    }
    public void addPropertyListener(PropertyListener pl) {
        PropertyListeners.put(pl, new String[0]);
    }
    public void removePropertyListener(PropertyListener pl) {
        PropertyListeners.remove(pl);
    }
    protected void firePropertyChange(String property, String newval) {
        // listeners to levels below this one may want to know in case the new value hides the lower one.
        if(defaults != null) {
            //System.out.println("defaults class: " + defaults.getClass().getCanonicalName());
            ((PropertyManager) defaults).firePropertyChange(property, newval);
        }
        // now inform listeners at this level.
        for(PropertyListener pl : PropertyListeners.keySet())
            for(String prop : PropertyListeners.get(pl))
                if(prop.equals(property))// && !get(prop).equals(newval))
                    pl.propertyChanged(property, newval);
    }
    @Override
    public synchronized Object setProperty(String key, String value) {
        Object previousValue = super.setProperty(key, value);
        //System.out.println("setting " + key + " to " + value);
        top.firePropertyChange(key, value);
        return previousValue;
    }

    // NOTE: Wants to be passed in but needs to be set during static initialization,
    // therefore it probably needs to be environment or registry variable.
    private final static String PRODUCT_NAME = "mc4d";

    /**
     * Applications should load any user-specific property overrides directly into this object
     * and then call setProperty whenever a user action needs to change one.
     */
    public final static PropertyManager userprefs;
    static {
        File propsFile = Paths.get(StaticUtils.getBinDir(), PRODUCT_NAME + ".props").toFile();

        if (!propsFile.canWrite() && !System.getProperty("os.name").startsWith("Windows")) {
            try {
                propsFile = Paths.get(System.getenv("XDG_CONFIG_HOME"), PRODUCT_NAME + ".props").toFile();
            } catch (NullPointerException | InvalidPathException e) {
                propsFile = Paths.get(System.getProperty("user.home"), ".config", PRODUCT_NAME + ".props").toFile();
            }
        }

        userprefs = new LocalProps(propsFile);
        System.out.println("Launch dir: " + StaticUtils.getBinDir());
    }

    /**
     * Stores all system properties. These take precedence over user preferences
     * but are themselves overridden by command-line settings.
     */
    private final static Properties sysprops = new PropertyManager(userprefs);

    /**
     * Applications should typically only call getProperty on this object
     * although it is OK to store program arguments and other overrides here too.
     */
    public final static PropertyManager top = new PropertyManager(sysprops);

    static {
        init();
    } // to perform static initialization

    /** users have no business sub-classing this class so a private empty constructor will forbid it. */
    private PropertyManager() {}

    public PropertyManager(Properties defaults) {
        super(defaults);
    }

    /**
     * Utility to load a properties object from a file.
     * 
     * @param prop_url points to a property file
     * @param into is the Properties object to populate.
     */
    public static void loadProps(URL prop_url, Properties into) {
        if(prop_url == null) {
            //System.err.println("PropertyManager.loadProps: passed null url");
            return;
        }
        try {
            URLConnection connection = prop_url.openConnection();
            InputStream propstream = connection.getInputStream();
            into.load(propstream);
        } catch(Exception e) {
            System.err.println("PropertyManager.init: Couldn't load property file '" + prop_url.getPath() +
                "'. Make sure this subpath can be found within the classpath.");
        }
    }

    /**
     * Utility to load a properties object from a file.
     * 
     * @param prop_file_name names a file expected to be found under ./resources
     * @param into is the Properties object to populate.
     */
    public static void loadProps(String prop_file_name, Properties into) {
        String path = "resources" + '/' + prop_file_name;
        URL propurl = PropertyManager.class.getClassLoader().getResource(path);
        loadProps(propurl, into);
    }

    /**
     * Utility to load a set of command line arguments into a Properties object.
     * All argument names are expected to begin with a minus. If followed by an argument
     * without a minus, that argument is taken as the value for the one with the minus.
     * Arguments followed directly by another flagged argument are taken as boolean arguments
     * Whose values are set to the string "true".
     * 
     * @param args typically an args array from a main method
     *        but with extracted elements possibly nulled out.
     * @param into the Properties file to load into.
     */
    public static void loadProps(String args[], Properties into) {
        for(int i = 0; i < args.length; i++) {
            if(args[i] == null)
                continue; // skip any nulled out elements (caller probably used and extracted them)
            if(args[i].startsWith("-")) {
                // Make sure there's another arg
                if((i + 1) < args.length) {
                    // Make sure it's not another flag
                    if(args[i + 1] == null || args[i + 1].startsWith("-")) {
                        // Must be a flag without a value, set to "true"
                        into.setProperty(args[i].substring(1), "true");
                    }
                    else {
                        into.setProperty(args[i].substring(1), args[i + 1]);
                        ++i; // skip to next arg pair
                    }
                } else {
                    // Must be a flag without a value at the end of the args
                    into.setProperty(args[i].substring(1), "true");
                }
            } else {
                // argument without a dash; must be malformed
                System.err.println("Invalid propertyfile argument: " + args[i]);
            }
        }
    }


    /**
     * A specialized PropertyManager that loads from a given local file.
     * Properties that are set on these objects are immediately persisted to that file
     * regardless of whether it existed originally.
     * This class is meant for handling user preferences that persist across sessions.
     */
    private static class LocalProps extends PropertyManager {
        private File localPropFile;
        private boolean storeFailed = false;
        public LocalProps(File localPropFile) {
            this.localPropFile = localPropFile;
            if(!localPropFile.exists())
                return; // nothing to load
            try {
                load(new FileInputStream(localPropFile));
            } catch(IOException e) {
                System.err.println("PropertyManager.LocalProps: Could not load local prop file '" + localPropFile.getAbsolutePath() + "'");
                this.localPropFile = null;
                e.printStackTrace();
            }
        }
        /**
         * Calls super.setProperty() and then immediately attempts to store the entire contents to the user's preference file.
         */
        @Override
        public synchronized Object setProperty(String key, String value) {
            Object ret = super.setProperty(key, value);
            writeToFile();
            // Notify all interested listeners in the chain by notifying the top and letting it recurse.
            return ret;
        }
        /**
         * Calls super.clear() and then immediately attempts to store the entire contents to the user's preference file.
         */
        @Override
        public void clear() {
            super.clear();
            writeToFile();
        }
        /**
         * Calls super.remove() and then immediately attempts to store the entire contents to the user's preference file.
         */
        @Override
        public Object remove(Object key) {
            Object ret = super.remove(key);
            writeToFile();
            return ret;
        }
        /**
         * Attempts to store the entire contents to the user's preference file.
         */
        private void writeToFile() {
            if(localPropFile == null || storeFailed)
                return;
            try {
                localPropFile.getParentFile().mkdirs();
                this.store(new FileOutputStream(localPropFile), PRODUCT_NAME + " User Preferences");
            } catch(IOException | SecurityException e) {
                storeFailed = true; // so as to only give fail msg once
                if(!localPropFile.canWrite())
                    System.err.println("Can't write");
                System.err.println("PropertyManager.LocalProps: Could not store local prop file '" + localPropFile.getAbsolutePath() + "'");
                e.printStackTrace();
            }
        }
    } // end class LocalProps


    /**
     * A specialized PropertyManager that attempts to load a set of properties from a URL
     * representing a set of VAR, reseller, or customer-specific overrides for an
     * application's default property values.
     * See the ClientProp documentation above for descriptions of the subtle syntax
     * differences for file locations and quoted keys.
     */
    private static class RemoteProps extends PropertyManager {
        private String prefix;
        public RemoteProps(String fileurl, Properties def) {
            defaults = def;
            if(fileurl == null)
                return;
            URL url = null;
            try {
                url = new URL(fileurl);
            } catch(MalformedURLException e) {
                System.err.println("Couldn't open remote property file: " + fileurl);
            }
            if(url != null)
                loadProps(url, this);
            prefix = fileurl.substring(0, fileurl.lastIndexOf("/"));
        }
        /**
         * @return normal getProperty value except that for values beginning with '/',
         *         prepends url path refering to same directory as the remote property file
         *         given to the constructor. Also, quoted values will have their quotes striipped.
         *         This is in case a user wants to create a path or other property beginning with '/'.
         */
        @Override
        public String getProperty(String key) {
            // for debugging vendor props, if uncommented, this will provide a good line
            // on which to set a breakpoint:
            //if(key.equalsIgnoreCase("main.logo.small"))
            //    key = key;
            String val = (String) get(key);
            if(val == null)
                return defaults.getProperty(key);
            if(val.startsWith("/")) // path is relative to vendor prop file location
                val = prefix + val;
            if(val.startsWith("\"") && val.endsWith("\"")) // quoted strings for root paths or to contain spaces
                val = val.substring(1, val.length() - 1);
            return val;
        }
        @Override
        public String getProperty(String key, String dflt) {
            String val = getProperty(key);
            return val == null ? dflt : val;
        }
    } // end class RemoteProps


    /**
     * Static initializer to bootstrap the system.
     */
    private static void init() {
        Properties sys = System.getProperties();
        for(Enumeration<Object> e = sys.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            sysprops.setProperty(key, sys.getProperty(key));
            //System.out.println(key + " = " + sys.getProperty(key));
        }
        PropertyManager defs = new PropertyManager();
        loadProps("defaults.prop", defs);
        // load any vendor-specific property file specified.
        // note, for testing, you can set a vm environment variable via something like:
        // -Dvendorprops=file:///C|/Superliminal/MC4D/resources/vendor.prop
        // or just hardcode with a line like this:
        // vendorpropfile = "file:///C|/Superliminal/MC4D/resources/vendor.prop";
        String vendorpropfile = System.getProperty("vendorprops");
        Properties vendorprops = new RemoteProps(vendorpropfile, defs);
        userprefs.defaults = new PropertyManager(vendorprops);
    }

    /**
     * Helper function to retrieve integer values from the top properties.
     * 
     * @param key property name
     * @param def default value
     * @return integer value of top.getProperty(key) or default if not found or parsed.
     */
    public static int getInt(String key, int def) {
        String str = top.getProperty(key);
        try {
            return Integer.parseInt(str);
        } catch(NumberFormatException nfe) {}
        return def;
    }

    /**
     * Helper function to retrieve float values from the top properties.
     * 
     * @param key property name
     * @param def default value
     * @return float value of top.getProperty(key) or default if not found or parsed.
     */
    public static float getFloat(String key, float def) {
        try {
            return Float.parseFloat(top.getProperty(key));
        } catch(Exception e) {}
        return def;
    }

    /**
     * Helper function to retrieve boolean values from the top properties.
     * 
     * @param key property name
     * @param def default value
     * @return boolean value of top.getProperty(key) or default if not found or parsed.
     */
    public static boolean getBoolean(String key, boolean def) {
        try {
            String topval = top.getProperty(key);
            if(topval == null)
                return def;
            //return Boolean.parseBoolean(topval); // parseBoolen doesn't exist in 1.4
            return(topval != null && topval.equalsIgnoreCase("true")); // per 1.5 doc
        } catch(Exception e) {}
        return def;
    }

    /**
     * Helper function to retrieve color objects from properties with the format
     * r,g,b where each channel is a value from 0 to 255. The color can also
     * be in hexadecimal format if it begins with "#", e.g., "#aabbcc"
     * 
     * @param key the color property name
     * @return a Color object with the parsed red, green, and blue values.
     */
    public static Color getColor(String key, Color def) {
        Color parsed = parseColor(top.getProperty(key));
        return parsed != null ? parsed : def;
    }
    public static Color parseColor(String colstr) {
        if(colstr == null)
            return null;
        if(colstr.indexOf('#') >= 0)
            return Color.decode(colstr);

        StringTokenizer toc = new StringTokenizer(colstr, ",");
        int r = Integer.parseInt(toc.nextToken());
        int g = Integer.parseInt(toc.nextToken());
        int b = Integer.parseInt(toc.nextToken());
        return new Color(r, g, b);
    }
    public static Color getColor(String key) {
        return getColor(key, null);
    }

    /**
     * Utility to construct a simple http url from keys to host and port properties.
     * 
     * @return an http url string.
     */
    public static String getURL(String hostkey, String portkey) {
        return "http://" + top.getProperty(hostkey) + ":" + top.getProperty(portkey) + "/";
    }

    /**
     * Simple example program.
     */
    public static void main(String args[]) {
        // test property listening.
        PropertyManager.top.addPropertyListener(new PropertyListener() {
            @Override
            public void propertyChanged(String property, String newval) {
                System.out.println("*** Property change. " + property + " now " + newval);
            }
        }, "debugging");
        // test of setting a top-level property, e.g. a setting specified on command line.
        PropertyManager.top.setProperty("debugging", "true");
        PropertyManager.top.setProperty("debugging", "false");
        // get a bunch of application values from the property files
        System.out.println("main.background = " + getColor("main.background"));
        System.out.println("tables.header.background = " + top.getProperty("tables.header.background") + " -> " + getColor("tables.header.background"));
        System.out.println("title = " + PropertyManager.top.getProperty("main.title"));
        // get a system property
        System.out.println("os.name = " + PropertyManager.top.getProperty("os.name"));
        // get a true top level (i.e. command line) property
        System.out.println("debugging = " + PropertyManager.top.getProperty("debugging"));
        int runcount = PropertyManager.getInt("test.runcount", 0);
        System.out.println("test.runcount = " + runcount);
        PropertyManager.userprefs.setProperty("test.runcount", ++runcount + "");
        System.out.println("test.runcount now = " + PropertyManager.userprefs.getProperty("test.runcount"));
        PropertyManager.userprefs.clear(); // testing that clearing userprefs writes the file
        System.out.println("num pref keys after clear: " + PropertyManager.userprefs.size());
        PropertyManager.userprefs.setProperty("test.runcount", runcount + "");
        System.exit(0);
    }
}
