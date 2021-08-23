package com.superliminal.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import com.superliminal.magiccube4d.MagicCube;


public class ColorUtils {
    private ColorUtils() {
    } // To disallow instantiation.
    private final static float
        U_OFF = .436f,
        V_OFF = .615f;
    private static final long RAND_SEED = 0;
    private static Random rand = new Random(RAND_SEED);


    /**
     * Converts an HSV color to RGB.
     * 
     * @param h hue in range [0->6] or -1. (If -1, returned RGB will be (v,v,v).
     * @param s saturation in range [0->1].
     * @param v brightness (value) in range [0->1].
     * @param rgb returned values in range [0->1].
     */
    public static void hsv2rgb(float h, float s, float v, float[] rgb) {
        float[] hsv = {h, s, v};
        System.out.println("H: " + h + " S: " + s + " V:" + v);
        if(hsv[0] == -1) {
            rgb[0] = rgb[1] = rgb[2] = hsv[2];
            return;
        }
        int i = (int) (Math.floor(hsv[0]));
        float f = hsv[0] - i;
        if(i % 2 == 0)
            f = 1 - f; // if i is even 
        float m = hsv[2] * (1 - hsv[1]);
        float n = hsv[2] * (1 - hsv[1] * f);
        switch(i) {
            case 6:
            case 0:
                rgb[0] = hsv[2];
                rgb[1] = n;
                rgb[2] = m;
                break;
            case 1:
                rgb[0] = n;
                rgb[1] = hsv[2];
                rgb[2] = m;
                break;
            case 2:
                rgb[0] = m;
                rgb[1] = hsv[2];
                rgb[2] = n;
                break;
            case 3:
                rgb[0] = m;
                rgb[1] = n;
                rgb[2] = hsv[2];
                break;
            case 4:
                rgb[0] = n;
                rgb[1] = m;
                rgb[2] = hsv[2];
                break;
            case 5:
                rgb[0] = hsv[2];
                rgb[1] = m;
                rgb[2] = n;
                break;
        }
    }


    // From http://en.wikipedia.org/wiki/YUV#Mathematical_derivations_and_formulas
    public static void yuv2rgb(float y, float u, float v, float[] rgb) {
        rgb[0] = 1 * y + 0 * u + 1.13983f * v;
        rgb[1] = 1 * y + -.39465f * u + -.58060f * v;
        rgb[2] = 1 * y + 2.03211f * u + 0 * v;
    }

    public static void rgb2yuv(float r, float g, float b, float[] yuv) {
        yuv[0] = .299f * r + .587f * g + .114f * b;
        yuv[1] = -.14713f * r + -.28886f * g + .436f * b;
        yuv[2] = .615f * r + -.51499f * g + -.10001f * b;
    }

    private static float[] randYUVinRGBRange(float minComponent, float maxComponent) {
        while(true) {
            float y = rand.nextFloat(); // * YFRAC + 1-YFRAC);
            float u = rand.nextFloat() * 2 * U_OFF - U_OFF;
            float v = rand.nextFloat() * 2 * V_OFF - V_OFF;
            float[] rgb = new float[3];
            yuv2rgb(y, u, v, rgb);
            float r = rgb[0], g = rgb[1], b = rgb[2];
            if(0 <= r && r <= 1 &&
                0 <= g && g <= 1 &&
                0 <= b && b <= 1 &&
                (r > minComponent || g > minComponent || b > minComponent) && // don't want all dark components
                (r < maxComponent || g < maxComponent || b < maxComponent)) // don't want all light components

                return new float[]{y, u, v};
        }
    }

    /*
     * Returns an array of ncolors RGB triplets such that each is as unique from the rest as possible
     * and each color has at least one component greater than minComponent and one less than maxComponent.
     * Use min == 1 and max == 0 to include the full RGB color range.
     * 
     * Warning: O N^2 algorithm blows up fast for more than 100 colors.
     */
    public static Color[] generateVisuallyDistinctColors(int ncolors, float minComponent, float maxComponent) {
        rand.setSeed(RAND_SEED); // So that we get consistent results for each combination of inputs

        float[][] yuv = new float[ncolors][3];

        // initialize array with random colors
        for(int got = 0; got < ncolors;) {
            System.arraycopy(randYUVinRGBRange(minComponent, maxComponent), 0, yuv[got++], 0, 3);
        }
        // continually break up the worst-fit color pair until we get tired of searching
        for(int c = 0; c < ncolors * 1000; c++) {
            float worst = 8888;
            int worstID = 0;
            for(int i = 1; i < yuv.length; i++) {
                for(int j = 0; j < i; j++) {
                    float dist = sqrdist(yuv[i], yuv[j]);
                    if(dist < worst) {
                        worst = dist;
                        worstID = i;
                    }
                }
            }
            float[] best = randYUVBetterThan(worst, minComponent, maxComponent, yuv);
            if(best == null)
                break;
            else
                yuv[worstID] = best;
        }

        Color[] rgbs = new Color[yuv.length];
        for(int i = 0; i < yuv.length; i++) {
            float[] rgb = new float[3];
            yuv2rgb(yuv[i][0], yuv[i][1], yuv[i][2], rgb);
            rgbs[i] = new Color(rgb[0], rgb[1], rgb[2]);
            //System.out.println(rgb[i][0] + "\t" + rgb[i][1] + "\t" + rgb[i][2]);
        }

        return rgbs;
    }

    private static float sqrdist(float[] a, float[] b) {
        float sum = 0;
        for(int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    private static double worstFit(Color[] colors) {
        float worst = 8888;
        float[] a = new float[3], b = new float[3];
        for(int i = 1; i < colors.length; i++) {
            colors[i].getColorComponents(a);
            for(int j = 0; j < i; j++) {
                colors[j].getColorComponents(b);
                float dist = sqrdist(a, b);
                if(dist < worst) {
                    worst = dist;
                }
            }
        }
        return Math.sqrt(worst);
    }

    private static float[] randYUVBetterThan(float bestDistSqrd, float minComponent, float maxComponent, float[][] in) {
        for(int attempt = 1; attempt < 100 * in.length; attempt++) {
            float[] candidate = randYUVinRGBRange(minComponent, maxComponent);
            boolean good = true;
            for(int i = 0; i < in.length; i++)
                if(sqrdist(candidate, in[i]) < bestDistSqrd)
                    good = false;
            if(good)
                return candidate;
        }
        return null; // after a bunch of passes, couldn't find a candidate that beat the best.
    }
    
    private static String colorFilename(String schlafli) {
    	return "facecolors" + File.separator + schlafli + ".txt";
    }
    
    private static Color[] findColors(int len, String fname) {
        for(Color[] cols : readColorLists(fname)) {
            if(cols.length == len)
                return cols;
        }
        return null;
    }
    
    public static Color[] findColors(String schlafli, int len) {
    	String filename = colorFilename(schlafli);
    	if (!Files.exists(Paths.get(filename)))
    		filename = MagicCube.FACE_COLORS_FILE;
    	return findColors(len, filename);
    }

    private static Color[][] readColorLists(String fname) {
        URL furl = ResourceUtils.getResource(fname);
        if(furl == null)
            return new Color[0][];
        String contents = ResourceUtils.readFileFromURL(furl);
        //JOptionPane.showMessageDialog(null, contents);
        if(contents == null)
            return new Color[0][];
        ArrayList<Color[]> colorlines = new ArrayList<Color[]>();
        try {
            BufferedReader br = new BufferedReader(new StringReader(contents));
            for(String line = br.readLine(); line != null;) {
                StringTokenizer st = new StringTokenizer(line);
                Color[] colorlist = new Color[st.countTokens()];
                for(int i = 0; i < colorlist.length; i++) {
                    String colstr = st.nextToken();
                    colorlist[i] = PropertyManager.parseColor(colstr);
                    if(colorlist[i] == null) {
                        colorlist = null;
                        break; // bad line
                    }
                }
                if(colorlist != null)
                    colorlines.add(colorlist);
                line = br.readLine();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return colorlines.toArray(new Color[0][]);
    } // end readColorLists()


    /**
     * Simple example program.
     */
    public static void main(String[] args) {
        final int ncolors = 10;
        Color[] colors = generateVisuallyDistinctColors(ncolors, .8f, .3f);
        for(int i = 0; i < colors.length; i++) {
            System.out.println(colors[i].toString());
        }
        System.out.println("Worst fit color = " + worstFit(colors));
    }

}
