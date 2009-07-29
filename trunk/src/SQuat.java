//
// NOTE: FOR APPLET COMPATIBILITY THIS CLASS SHOULD NOT INCLUCE ANY POST JDK 1.2 CONSTRUCTS
// AND SHOULD NOT BE COMPILED WITH POST JDK 1.4 COMPILERS.
//

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
 *                                                                     *
 *                       SIMPLE QUATERNION LIBRARY                     *
 *                       by Melinda Green                              *
 *                       Copyright 2005 - Superliminal Software        *
 *                                                                     *
 * DESCRIPTION                                                         *
 *    Implements a quaternion class called "SQuat".                    *
 *    A quaternion represents a rotation about an axis.                *
 *    Squats can be concatenated together via the mult and setMult     *
 *    methods.                                                         *
 *                                                                     *
 *    SQuats operate in a left handed coordinate system (i.e. positive *
 *    Z is coming out of the screen). The direction of a rotation by   *
 *    a positive value follows the right-hand-rule meaning that if the *
 *    thumb of your right hand is pointing along the axis of rotation, *
 *    the rotation is in the direction that your fingers curl.         *
 *                                                                     *
 *    Simple 3D vector and matrix classes are also defined mostly as   *
 *    ways to get 3D data into and out of quaternions. Methods for     *
 *    matrix multiplication, inversion, etc. are purposely not         *
 *    implemented because they are fully covered in vec.h the vector   *
 *    macro library which will work with these vector and matrix       *
 *    classes and with any other objects that can be indexed into.     *
 *                                                                     *
 *    The GetAxis and GetRotation methods extract the current axis and *
 *    rotation in radians.  A rotation matrix can also be extracted    *
 *    via the Matrix3 constructor which takes a SQuat argument.        *
 *    A SQuat can also be initialized from a rotation matrix and       *
 *    thereby extract its axis of rotation and amount.                 *
 *                                                                     *
 *    The GetHomoAxis and GetHomoRoation methods return the raw values *
 *    stored in a SQuat which have no useful geometric interpretation  *
 *    and are probably only useful for serializing SQuats as           *
 *    effeciently as possible.                                         *
 *    When reconstructing a SQuat from homogenous values, use the      *
 *    five-value constructor and pass a true "homo-vals" flag.         *
 *    Otherwise, that flag should always be allowed to default to      *
 *    false.                                                           *
 *                                                                     *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

public class SQuat {
    private final static boolean PREMULT = true;
    private final static int 
        X = 0,
        Y = 1,
        Z = 2;
    private Vector3 axis = new Vector3();
    private float rot;
     
    private void initFromVector(Vector3 a, float r) {
        Vector3 normvec = new Vector3(a.normal());
        float half_angle = r * 0.5f;
        float sin_half_angle = (float)Math.sin(half_angle);
        rot = (float)Math.cos(half_angle);
        Vec_h._VXS3(axis.elements, normvec.elements, sin_half_angle);
    }
    
    public SQuat() {
        this(1,0,0, 0, false);
    }

    public SQuat (Vector3 a, float r, boolean homo_vals) {
        if (homo_vals)
            set(a, r);
        else
            initFromVector(a, r);
    }

    public SQuat(float x, float y, float z, float r, boolean homo_vals) {
        if (homo_vals)
            set(new Vector3(x, y, z), r);
        else
            initFromVector(new Vector3 (x, y, z), r);
    }
    
    public SQuat(float a[], float r) {
        this(a[0], a[1], a[2], r, false);
    }

    private void set(SQuat q) {
        set(q.axis, q.rot);
    }
    
    private void set(Vector3 a, float r) {
        rot = r;
        Vec_h._SET3(axis.elements, a.elements);
    }

    public SQuat(SQuat q) {
        set(q);
    }

    public SQuat(Matrix3 m) {
        float trace = m.trace();
        if(PREMULT) {
            if (trace > 0.0)
            {
                float s = (float)Math.sqrt(trace + 1.0);
                rot = s * 0.5f;
                s = 0.5f / s;
                Vector3 a = new Vector3(
                    m.rows[2].elements[1] - m.rows[1].elements[2],
                    m.rows[0].elements[2] - m.rows[2].elements[0], 
                    m.rows[1].elements[0] - m.rows[0].elements[1]);
                Vec_h._VXS3(axis.elements, a.elements, s);
            }
            else
            {
                int         i, j, k;
                i = 0;
                if (m.rows[1].elements[1] > m.rows[0].elements[0])
                    i = 1;
                if (m.rows[2].elements[2] > m.rows[i].elements[i])
                    i = 2;
                j = (i + 1) % 3;
                k = (i + 2) % 3;
                float s = (float)Math.sqrt(m.rows[i].elements[i] - m.rows[j].elements[j] - m.rows[k].elements[k] + 1.0);
                axis.elements[i] = s * 0.5f;
                s = 0.5f / s;
                rot = s * (m.rows[k].elements[j] - m.rows[j].elements[k]);
                axis.elements[j] = s * m.rows[j].elements[i] + m.rows[i].elements[j];
                axis.elements[k] = s * m.rows[k].elements[i] + m.rows[i].elements[k];
            }
        } else {
            if (trace > 0.0)
            {
                float s = (float)Math.sqrt(trace + 1.0);
                rot = s * 0.5f;
                s = 0.5f / s;
                Vector3 a = new Vector3(
                    m.rows[1].elements[2] - m.rows[2].elements[1],
                    m.rows[2].elements[0] - m.rows[0].elements[2], 
                    m.rows[0].elements[1] - m.rows[1].elements[0]);
                Vec_h._VXS3(axis.elements, a.elements, s);
            }
            else
            {
                int i, j, k;
                i = 0;
                if (m.rows[1].elements[1] > m.rows[0].elements[0])
                    i = 1;
                if (m.rows[2].elements[2] > m.rows[i].elements[i])
                    i = 2;
                j = (i + 1) % 3;
                k = (i + 2) % 3;
                float s = (float)Math.sqrt(m.rows[i].elements[i] - m.rows[j].elements[j] - m.rows[k].elements[k] + 1.0);
                axis.elements[i] = s * 0.5f;
                s = 0.5f / s;
                rot = s * (m.rows[j].elements[k] - m.rows[k].elements[j]);
                axis.elements[j] = s * m.rows[i].elements[j] + m.rows[j].elements[i];
                axis.elements[k] = s * m.rows[i].elements[k] + m.rows[k].elements[i];
            }
        }
    }
    
    public SQuat mult(SQuat q)
    {
        Vector3 
            v1 = new Vector3(), 
            v2 = new Vector3(), 
            scaled_sum = new Vector3(),
            cross = new Vector3(),
            new_axis = new Vector3();
        Vec_h._VXS3(v1.elements, q.axis.elements, rot);
        Vec_h._VXS3(v2.elements, axis.elements, q.rot);
        Vec_h._VPV3(scaled_sum.elements, v1.elements, v2.elements);
        // q.rot*axis
        if(PREMULT)
            Vec_h._VXV3(cross.elements, axis.elements, q.axis.elements);
        else
            Vec_h._VXV3(cross.elements, q.axis.elements, axis.elements);
        Vec_h._VPV3(new_axis.elements, scaled_sum.elements, cross.elements);
        // scaled_sum + cross
        return new SQuat(
            new_axis,
            rot * q.rot - Vec_h._DOT3(axis.elements, q.axis.elements),
            true);
    }


    public void setMult(SQuat q) {
        set(mult(q));
    }

    public float getRotation() {
        return 2 * (float)Math.acos(rot);
    }

    public void getAxis(Vector3 v) {
        float theta = getRotation();
        float f = 1 / (float)Math.sin(theta * 0.5);
        Vec_h._VXS3(v.elements, axis.elements, f);
    }

    public Vector3 getAxis() {
        return new Vector3(axis);
    }

    float getHomoRotation() {
        return rot;
    }

    void getHomoAxis(Vector3 v) {
        Vec_h._SET3(v.elements, axis.elements);
    }
    
    
    /*
     * A simple array of three floats that can be indexed into.
     */
    public static class Vector3 {
        private float elements[] = new float[3];
        
        public float[] asArray() {
            return new float[] {
                elements[X],
                elements[Y],
                elements[Z],
            };
        }
        
        public Vector3() { }
        public Vector3 (float x, float y, float z) {
            elements[X] = x;
            elements[Y] = y;
            elements[Z] = z;
        }
        public Vector3(Vector3 from) {
            Vec_h._SET3(elements, from.elements);
        }

        public float length() {
            return (float)Math.sqrt(Vec_h._NORMSQRD3(elements));
        }

        public Vector3 normal() {
            Vector3 norm = new Vector3(this);
            float len = length();
            Vec_h._VDS3(norm.elements, norm.elements, len);
            return norm;
        }
    } // end class Vector3
    

    /**
     * A simple matrix class represented by 3 row vectors.
     */
    public static class Matrix3 {
        private Vector3 rows[] = new Vector3[] {
            new Vector3(1,0,0),
            new Vector3(0,1,0),
            new Vector3(0,0,1),
        };
        
        public float[][] asArray() {
            return new float[][] {
                rows[X].asArray(),
                rows[Y].asArray(),
                rows[Z].asArray(),
            };
        }
        
        public Matrix3 () { }
    
        public Matrix3(Matrix3 from) {
            for(int i=0; i<rows.length; i++)
                Vec_h._SET3(rows[i].elements, from.rows[i].elements);
        }
    
        public Matrix3(Vector3 x, Vector3 y, Vector3 z) {
            rows[X] = x;
            rows[Y] = y;
            rows[Z] = z;
        }
    
        public Matrix3(SQuat q) {
            float W = q.getHomoRotation();
            Vector3 axis = new Vector3();
            q.getHomoAxis(axis);
    
            float      XX = axis.elements[X] + axis.elements[X];
            float      YY = axis.elements[Y] + axis.elements[Y];
            float      ZZ = axis.elements[Z] + axis.elements[Z];
    
            float      WXX = W * XX;
            float      XXX = axis.elements[X] * XX;
    
            float      WYY = W * YY;
            float      XYY = axis.elements[X] * YY;
            float      YYY = axis.elements[Y] * YY;
    
            float      WZZ = W * ZZ;
            float      XZZ = axis.elements[X] * ZZ;
            float      YZZ = axis.elements[Y] * ZZ;
            float      ZZZ = axis.elements[Z] * ZZ;
    
            if(PREMULT) {
                rows[X].elements[X] = 1f - (YYY + ZZZ);
                rows[X].elements[Y] = XYY - WZZ;
                rows[X].elements[Z] = XZZ + WYY;
        
                rows[Y].elements[X] = XYY + WZZ;
                rows[Y].elements[Y] = 1f - (XXX + ZZZ);
                rows[Y].elements[Z] = YZZ - WXX;
        
                rows[Z].elements[X] = XZZ - WYY;
                rows[Z].elements[Y] = YZZ + WXX;
                rows[Z].elements[Z] = 1f - (XXX + YYY);
            } else {
                rows[X].elements[X] = 1f - (YYY + ZZZ);
                rows[Y].elements[X] = XYY - WZZ;
                rows[Z].elements[X] = XZZ + WYY;
        
                rows[X].elements[Y] = XYY + WZZ;
                rows[Y].elements[Y] = 1f - (XXX + ZZZ);
                rows[Z].elements[Y] = YZZ - WXX;
        
                rows[X].elements[Z] = XZZ - WYY;
                rows[Y].elements[Z] = YZZ + WXX;
                rows[Z].elements[Z] = 1f - (XXX + YYY);
            }
        }
        
        public float trace() {
            return 
                rows[X].elements[X] + 
                rows[Y].elements[Y] + 
                rows[Z].elements[Z];
        }
    } // end class Matrix3
    
} // end class SQuat
