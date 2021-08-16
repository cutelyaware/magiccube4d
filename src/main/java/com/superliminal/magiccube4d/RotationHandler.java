package com.superliminal.magiccube4d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

import com.donhatchsw.util.VecMath;
import com.superliminal.util.PropertyManager;


// Class which manages view rotations, both ctrl-click and continuous style.
public class RotationHandler {
    public RotationHandler() {
        set4dView(null);
    }
    public RotationHandler(double[][] initialMatrix) {
        set4dView(initialMatrix);
    }

    public enum Snap {
        Snap_Cell, // Rotates cell centers.
        Snap_Smart // Rotates vertices, edge centers, cell centers, etc. smartly.
                   // (we've been calling this cubie-to-center, even though that's not always the case)
    }

    public static Snap getSnapSetting() {
        return PropertyManager.getBoolean("ctrlrotbyface", true) ? Snap.Snap_Cell : Snap.Snap_Smart;
    }

    // Persistence.
    public void write(Writer writer) throws IOException {
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                writer.write("" + viewMat4d[i][j]);
                writer.write(j == 3 ? System.getProperty("line.separator") : " ");
            }
        }
    }

    public void read(BufferedReader reader) throws IOException {
        for(int i = 0; i < 4; i++) {
            String line = reader.readLine();
            String numberStrings[] = line.split(" ");
            for(int j = 0; j < 4; j++) {
                viewMat4d[i][j] = Double.parseDouble(numberStrings[j]);
            }
        }
    }

    // 4D Variables.
    private double spinDelta[][];
    private double viewMat4d[][] = VecMath.identitymat(4);

    public double[][] current4dView() {
        return viewMat4d;
    }

    public void set4dView(double[][] mat) {
        if(mat == null)
            mat = new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
            };
        VecMath.copymat(viewMat4d, mat);
        VecMath.gramschmidt(viewMat4d, viewMat4d);
    }

    // Handles updating our rotation matrices based on mouse dragging.
    public void mouseDragged(float dx, float dy,
        boolean left, boolean middle, boolean shift)
    {
        // Calc how many pixels we moved.
        float[] drag_dir = new float[]{dx, dy};
        float[] axis = new float[3];
        Vec_h._XV2(axis, drag_dir);
        float pixelsMoved = (float) Math.sqrt(Vec_h._NORMSQRD2(axis));

        // Do nothing if we ended where we started.
        if(pixelsMoved <= .0001)
            return;

        spinDelta = VecMath.zeromat(4);

        if(left && !shift) {
            spinDelta[0][2] += dx;
            spinDelta[2][0] -= dx;

            spinDelta[1][2] += dy;
            spinDelta[2][1] -= dy;
        }

        if(left && shift) {
            spinDelta[0][3] -= dx;
            spinDelta[3][0] += dx;

            spinDelta[1][3] -= dy;
            spinDelta[3][1] += dy;
        }

        boolean right = !left && !middle;
        if(right) {
            spinDelta[0][1] += dx;
            spinDelta[1][0] -= dx;

            spinDelta[3][2] -= dy;
            spinDelta[2][3] += dy;
        }

        // Handle the sensitivity.
        VecMath.mxs(spinDelta, spinDelta, .005 * PropertyManager.getFloat("dragfactor", 1));

        applySpinDelta();
        if(pixelsMoved < 2)
            stopSpinning();
    }

    private void applySpinDelta() {
        assert (spinDelta != null);
        if(spinDelta == null)
            return;

        double delta[][] = VecMath.identitymat(4);
        VecMath.mpm(delta, delta, spinDelta);
        VecMath.gramschmidt(delta, delta);
        viewMat4d = VecMath.mxm(viewMat4d, delta);
        VecMath.gramschmidt(viewMat4d, viewMat4d);
    }

    // Advances auto-rotation (if any) and returns true if our settings are such that we should continue.
    public boolean continueSpin() {
        if(PropertyManager.getBoolean("autorotate", true) && spinDelta != null) {
            applySpinDelta();
            return true;
        }

        return false;
    }

    public boolean isSpinning() {
        return spinDelta != null;
    }

    public void stopSpinning() {
        spinDelta = null;
    }
}