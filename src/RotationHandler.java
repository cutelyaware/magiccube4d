import com.donhatchsw.util.VecMath;


enum Snap
{
	Snap_Cell,		// Rotates cell centers.
	Snap_Smart		// Rotates vertices, edge centers, cell centers, etc. smartly (we've been calling this cubie-to-center as well)
};

class RotationSettings
{
	RotationSettings()
	{
		allowSpinDrag = true;
		snapSetting = Snap.Snap_Cell;
	}
	
	public boolean allowSpinDrag;
	public Snap snapSetting;
}

// Class which manages view rotations, both ctrl-click and continuous style.
class RotationHandler
{
	public RotationSettings settings = new RotationSettings();

	// 4D Variables.
	private double viewMat4d[][] = VecMath.identitymat( 4 );
	private double spindelta4d[][] = null;

	// 3D Variables (make these go away since Jenn controls could subsume them?)
	private SQuat viewrot = new SQuat();	// total quaternion rotation of puzzle.
	private SQuat spindelta = null;			// rotation to add for each frame while spinning. null == stopped

	public double[][] current4dView()
	{
		return viewMat4d;
	}

    public float[][] current3dView() {
        if( !settings.allowSpinDrag )
            return new float[][] {
                {1,0,0,},
                {0,1,0,},
                {0,0,1,},
            };
        return new SQuat.Matrix3(viewrot).asArray();
    }

	public void reset4dView()
	{
		VecMath.identitymat( viewMat4d );
	}
	
	// Handles updating our rotation matrices based on mouse dragging.
	// XXX - 4D dragging to come.
	public void mouseDragged( float dx, float dy )
	{
		// Old 3D code for now.
		float[] drag_dir = new float[] { dx, dy };
        float[] axis = new float[3];
        Vec_h._XV2(axis, drag_dir);
        float pixelsMoved = (float)Math.sqrt(Vec_h._NORMSQRD2(axis));
        if (pixelsMoved > .0001) { // do nothing if ended where we started
            Vec_h._VDS2(axis, axis, pixelsMoved);
            axis[2] = 0;
            float rads = pixelsMoved / 300f;
            spindelta = new SQuat(axis, rads);
            viewrot.setMult(spindelta);
            if(pixelsMoved < 2)
                spindelta = null; // drag distance not large enough to trigger autorotation
        }
	}
	
	// Returns true if our settings are such that we should continue.
	public boolean continueSpin()
	{
		if( settings.allowSpinDrag && spindelta != null )
		{
			viewrot.setMult( spindelta );
			return true;
		}
		
		return false;
	}
	
	public void stopSpinning()
	{
		spindelta4d = null;
		spindelta = null;
	}
}