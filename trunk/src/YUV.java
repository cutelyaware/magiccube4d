
public class YUV {
	private final static double
		U_OFF = .436,
		V_OFF = .615,
		YFRAC = 1.0f;

	// From http://en.wikipedia.org/wiki/YUV#Mathematical_derivations_and_formulas
	public static void yuv2rgb(float y, float u, float v, float[] rgb) {
		rgb[0] = 1 * y +        0 * u + 1.13983f * v;
		rgb[1] = 1 * y + -.39465f * u + -.58060f * v;
		rgb[2] = 1 * y + 2.03211f * u +        0 * v;
	}	

	public static void rgb2yuv(float r, float g, float b, float[] yuv) {
		yuv[0] = .299f    * r + .587f    * g + .114f    * b;
		yuv[1] = -.14713f * r + -.28886f * g + .436f    * b;
		yuv[2] = .615f    * r + -.51499f * g + -.10001f * b;
	}
	
	private static float[] randYUVinRGBRange(float minComponent, float maxComponent) {
		while(true) {
			float y = (float)(Math.random());// * YFRAC + 1-YFRAC);
			float u = (float)(Math.random() * (2*U_OFF) - U_OFF);
			float v = (float)(Math.random() * (2*V_OFF) - V_OFF);
			float[] rgb = new float[3];
			yuv2rgb(y, u, v, rgb);
			float r = rgb[0], g = rgb[1], b = rgb[2];
			if(
					0 <= r && r <= 1 &&
					0 <= g && g <= 1 &&
					0 <= b && b <= 1 &&
					(r>minComponent || g>minComponent || b>minComponent) && // don't want all dark components
					(r<maxComponent || g<maxComponent || b<maxComponent))   // don't want all light components
					
				return new float[] {y, u, v};
		}
	}
	
	/*
	 * Returns an array of ncolors RGB triplets such that each is as unique from the rest as possible
	 * and each color has at least one component greater than minComponent and one less than maxComponent.
	 * Use min == 1 and max == 0 to include the full RGB color range.
	 */
    static float[][] generateVisuallyDistinctRGBs(int ncolors, float minComponent, float maxComponent) {
		float[][] yuv = new float[ncolors][3];
		
		// initialize array with random colors
		for(int got=0; got<ncolors; ) {
			System.arraycopy(randYUVinRGBRange(minComponent, maxComponent), 0, yuv[got++], 0, 3);
		}
		// continually break up the worst-fit color pair until we get tired of searching
		for(int c=0; c<1000; c++) {
	    	float worst = 8888;
	    	int worstID = 0;
	    	for(int i=1; i<yuv.length; i++) {
	    		for(int j=0; j<i; j++) {
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

		float[][] rgb = new float[yuv.length][3];
		for(int i=0; i<yuv.length; i++) {
			yuv2rgb(yuv[i][0], yuv[i][1], yuv[i][2], rgb[i]);
			//System.out.println(rgb[i][0] + "\t" + rgb[i][1] + "\t" + rgb[i][2]);
		}
		
		return rgb;
    }
    
    private static float sqrdist(float[] a, float[] b) {
    	float sum = 0;
    	for(int i=0; i<a.length; i++) {
    		float diff = a[i] - b[i];
    		sum += diff * diff;
    	}
    	return sum;
    }
    
    private static int worstFit(float[][] rgb) {
    	float worst = 8888;
    	int worstID = 0;
    	for(int i=1; i<rgb.length; i++) {
    		for(int j=0; j<i; j++) {
	    		float dist = sqrdist(rgb[i], rgb[j]);
	    		if(dist < worst) {
	    			worst = dist;
	    			worstID = i;
	    		}
    		}
    	}
    	System.out.println("Worst fit " + worst + " at ID " + worstID);
    	return worstID;
    }
    
    private static float[] randYUVBetterThan(float bestDistSqrd, float minComponent, float maxComponent, float[][] in) {
    	for(int attempt=1; attempt<10000; attempt++) {
    		float[] candidate = randYUVinRGBRange(minComponent, maxComponent);
    		boolean good = true;
    		for(int i=0; i<in.length; i++)
    			if(sqrdist(candidate, in[i]) < bestDistSqrd)
    				good = false;
    		if(good)
    			return candidate;
    	}
    	return null;
    }

    
	/**
	 * Simple example program.
	 */
	public static void main(String[] args) {
		float[][] rgb = generateVisuallyDistinctRGBs(4, .8f, .3f);
		for(int i=0; i<rgb.length; i++) {
			System.out.println(rgb[i][0] + "\t" + rgb[i][1] + "\t" + rgb[i][2]);
		}
	}

}
