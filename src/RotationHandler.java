import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;

import com.donhatchsw.util.VecMath;


// Class which manages view rotations, both ctrl-click and continuous style.
public class RotationHandler
{
	RotationHandler()
	{
		reset4dView();
	}
	
	public enum Snap
	{
		Snap_Cell,		// Rotates cell centers.
		Snap_Smart		// Rotates vertices, edge centers, cell centers, etc. smartly.
						// (we've been calling this cubie-to-center, even though that's not always the case)
	}
	
	// Modes
	private boolean allowSpinDrag = true;
	private Snap snapSetting = Snap.Snap_Cell;
	private float sensitivity = 0.5f;	// Scaled this so decent values between 0 and 1.
	public void setAllowSpinDrag(boolean allowSpinDrag) { this.allowSpinDrag = allowSpinDrag; }
	public boolean getAllowsSpinDrag() { return allowSpinDrag; }
	public void setSnapSetting(Snap snapSetting) { this.snapSetting = snapSetting; }
	public Snap getSnapSetting() { return snapSetting; }
	public void setSensitivity( float sensitivity ) { this.sensitivity = sensitivity; }

	// Persistence.
	public void write( Writer writer ) throws IOException
	{
		for(int i=0; i<4; i++) {
			for(int j=0; j<4; j++) {
				writer.write(""+viewMat4d[i][j]);
				writer.write(j==3 ? System.getProperty("line.separator") : " ");
			}
		}
	}
	
	public void read( BufferedReader reader ) throws IOException 
	{
		for(int i=0; i<4; i++) {
			String line = reader.readLine();
			String numberStrings[] = line.split(" ");
			for(int j=0; j<4; j++) {
				viewMat4d[i][j] = Double.parseDouble(numberStrings[j]);
			}
		}
	}
	
	// 4D Variables.
	private double spinDelta[][];
	private double viewMat4d[][] = VecMath.identitymat( 4 );

	public double[][] current4dView()
	{
		return viewMat4d;
	}

	public void reset4dView()
	{
		// Put us in a nice-looking start position.
		this.viewMat4d = new double[][] {
			{.732,-.196,.653,0},
			{.681,.187,-.707,0},
			{.016,.963,.270,0},
			{0,0,0,1} };
		VecMath.gramschmidt( viewMat4d, viewMat4d );
	}
	
	// Handles updating our rotation matrices based on mouse dragging.
	public void mouseDragged( float dx, float dy,
		boolean left, boolean middle, boolean shift )
	{
		// Calc how many pixels we moved.
		float[] drag_dir = new float[] { dx, dy };
        float[] axis = new float[3];
        Vec_h._XV2( axis, drag_dir );
        float pixelsMoved = (float)Math.sqrt(Vec_h._NORMSQRD2(axis));
        
        // Do nothing if we ended where we started.
        if( pixelsMoved <= .0001 ) 
        	return;
		
        spinDelta = VecMath.zeromat( 4 );

		if( left && !shift )
		{
			spinDelta[0][2] += dx;
			spinDelta[2][0] -= dx;
	
			spinDelta[1][2] += dy;
			spinDelta[2][1] -= dy;
		}
		
		if( left && shift )
		{
	        spinDelta[0][3] -= dx;
	        spinDelta[3][0] += dx;
	
	        spinDelta[1][3] -= dy;
	        spinDelta[3][1] += dy;
		}
        
		boolean right = !left && !middle;
		if( right )
		{
			spinDelta[0][1] += dx;
			spinDelta[1][0] -= dx;
	
			spinDelta[3][2] -= dy;
			spinDelta[2][3] += dy;
		}

		// Handle the sensitivity.
        VecMath.mxs( spinDelta, spinDelta, .005 * this.sensitivity );
		
		applySpinDelta();
        if( pixelsMoved < 2 )
        	spinDelta = null;
	}
	
	private void applySpinDelta()
	{
		assert( spinDelta != null );
		if( spinDelta == null )
			return;
		
        double delta[][] = VecMath.identitymat(4);
        VecMath.mpm( delta, delta, spinDelta );
        viewMat4d = VecMath.mxm( viewMat4d, delta );
        VecMath.gramschmidt( viewMat4d, viewMat4d );
	}
	
	// Returns true if our settings are such that we should continue.
	public boolean continueSpin()
	{
		if( allowSpinDrag && spinDelta != null )
		{
			applySpinDelta();
			return true;
		}
		
		return false;
	}
	
	public boolean isSpinning() 
	{ 
		return spinDelta == null; 
	}
	
	public void stopSpinning()
	{
		spinDelta = null;
	}
}