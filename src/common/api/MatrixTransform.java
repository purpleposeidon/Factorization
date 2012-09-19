package factorization.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;

public class MatrixTransform {
    float M[][] = new float[4][4]; //row, column
    
    public MatrixTransform() {
        reset();
    }
    
    public MatrixTransform(float[][] matrix) {
        this.M = matrix;
    }
    
    //matrix operations
    
    public void reset() {
        for (float[] f : M) {
            Arrays.fill(f, 0);
        }
        for (int i = 0; i < 4; i++) {
            M[i][i] = 1;
        }
    }
    
    static float[][] multiplyTemp = new float[4][4];
    public void multiply(MatrixTransform other) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float cellSum = 0;
                for (int n = 0; n < 4; n++) {
                    cellSum += M[row][n]*M[n][col];
                }
                multiplyTemp[row][col] = cellSum;
            }
        }
        float[][] swap = M;
        M = multiplyTemp;
        multiplyTemp = swap;
    }
    
    public static VectorUV tempApply = new VectorUV(0, 0, 0);
    public VectorUV apply(VectorUV orig) {
        float rowSum;
        int row;
        
        row = 0;
        tempApply.x =
            M[row][0]*orig.x +
            M[row][1]*orig.y +
            M[row][2]*orig.z;
        
        row = 1;
        tempApply.y =
            M[row][0]*orig.x +
            M[row][1]*orig.y +
            M[row][2]*orig.z;
        
        row = 2;
        tempApply.z =
            M[row][0]*orig.x +
            M[row][1]*orig.y +
            M[row][2]*orig.z;
        
        VectorUV ret = tempApply;
        ret.u = orig.u;
        ret.v = orig.v;
        tempApply = orig;
        return ret;
    }

    //http://www.fastgraph.com/makegames/3drotation/
    static MatrixTransform transTemp = new MatrixTransform();
    public void rotateX(float theta) {
        transTemp.reset();
        float C = (float) Math.cos(theta);
        float S = (float) Math.sin(theta);
        float s = -S;
        /* C = cos, c = -cos
         * S = sin, s = -sin
         * 1000
         * 0Cs0
         * 0SC0
         * 0001
         */
        transTemp.M[1][1] = C;
        transTemp.M[2][1] = s;
        transTemp.M[1][2] = S;
        transTemp.M[2][2] = C;
        multiply(transTemp);
    }

    public void rotateY(float theta) {
        transTemp.reset();
        float C = (float) Math.cos(theta);
        float S = (float) Math.sin(theta);
        float s = -S;
        /* C = cos, c = -cos
         * S = sin, s = -sin
         * C0S0
         * 0100
         * s0C0
         * 0001
         */
        transTemp.M[0][0] = C;
        transTemp.M[2][0] = S;
        transTemp.M[0][2] = s;
        transTemp.M[2][2] = C;
        multiply(transTemp);
    }

    public void rotateZ(float theta) {
        transTemp.reset();
        float C = (float) Math.cos(theta);
        float S = (float) Math.sin(theta);
        float s = -S;
        /* C = cos, c = -cos
         * S = sin, s = -sin
         * Cs00
         * SC00
         * 0010
         * 0001
         */
        transTemp.M[0][0] = C;
        transTemp.M[1][0] = s;
        transTemp.M[0][1] = S;
        transTemp.M[1][1] = C;
        multiply(transTemp);
    }
    
    public void rotate(float x, float y, float z, float theta) {
        //http://www.fastgraph.com/makegames/3drotation/image086.gif
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);
        float t = 1-c;
        
        float R[][] = transTemp.M;
        R[0][0] = t*x*x + c;
        R[1][0] = t*x*y - s*z;
        R[2][0] = t*x*z + s*y;
        R[0][1] = t*x*y + s*z;
        R[1][1] = t*y*y + c;
        R[2][1] = t*y*z - s*x;
        R[0][2] = t*x*z - s*y;
        R[1][2] = t*y*z + s*x;
        R[2][2] = t*z*z + c;
        
        R[3][3] = 1;
        for (int i = 0; i < 3; i++) {
            R[3][i] = 0;
            R[i][3] = 0;
        }
        multiply(transTemp);
    }
    
    public void translate(float x, float y, float z) {
        transTemp.reset();
        
        multiply(transTemp);
    }
    
    //reading & writing
    public byte[] toByteArray() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outStream);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                try {
                    out.writeFloat(M[i][j]);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return outStream.toByteArray();
    }
    
    public static MatrixTransform fromByteArray(byte[] inputByte) {
        MatrixTransform ret = new MatrixTransform();
        if (inputByte == null || inputByte.length == 0) {
            return ret;
        }
        ByteArrayInputStream inStream = new ByteArrayInputStream(inputByte);
        DataInputStream in = new DataInputStream(inStream);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                try {
                    ret.M[i][j] = in.readFloat();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        return ret;
    }
    
    public void writeToTag(NBTTagCompound tag, String name) {
        tag.setByteArray(name, toByteArray());
    }
    
    public static MatrixTransform readFromTag(NBTTagCompound tag, String name) {
        return fromByteArray(tag.getByteArray(name));
    }
    
    //object operations
    public boolean equals(MatrixTransform other) {
        return Arrays.equals(M, other.M);
    }
    
    public MatrixTransform copy() {
        return new MatrixTransform(M.clone());
    }
}
