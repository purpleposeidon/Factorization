package factorization.api;

import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4d;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

/**
 * This is a 4x4 immutable matrix.
 * It's presently a wrapper around the mutable javax Matrix4d, but I'm not sure if I can rely on javax being present
 * on the server. If it turns out I can't, that's OK as this is well-encapsulated.
 *
 * See Wikipedia for an introduction on matrices and/or take a course in Linear Algebra.
 */
public final class Mat {
    public static final Mat IDENTITY = newIdentity();

    public static Mat trans(Vec3 delta) {
        return trans(delta.xCoord, delta.yCoord, delta.zCoord);
    }

    public static Mat trans(double dx, double dy, double dz) {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.m03 = dx;
        ret.matrix.m13 = dy;
        ret.matrix.m23 = dz;
        return ret;
    }

    public static Mat rotate(Quaternion quat) {
        if (quat.isZero()) return IDENTITY;
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.setRotation(quat.toJavaxD());
        return ret;
    }

    public static Mat scale(double v) {
        if (v == 1) return IDENTITY;
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.setScale(v);
        return ret;
    }

    /**
     * @param values The matrices to multiply. REMINDER: The transforms apply opposite of common sense.
     * @return The product of the provided matrices, or the identity matrix if none were given.
     */
    public static Mat mul(Mat... values) {
        Mat accum = newIdentity();
        for (Mat v : values) {
            if (v == IDENTITY) continue;
            accum.matrix.mul(v.matrix);
        }
        return accum;
    }

    public Mat invert() {
        if (this == IDENTITY) return this;
        Mat ret = dupe();
        ret.matrix.invert(this.matrix);
        return ret;
    }

    public Vec3 mul(Vec3 v) {
        return new Vec3(
                matrix.m00 * v.xCoord + matrix.m01 * v.yCoord + matrix.m02 * v.zCoord + matrix.m03,
                matrix.m10 * v.xCoord + matrix.m11 * v.yCoord + matrix.m12 * v.zCoord + matrix.m13,
                matrix.m20 * v.xCoord + matrix.m21 * v.yCoord + matrix.m22 * v.zCoord + matrix.m23);
    }

    @SideOnly(Side.CLIENT)
    public void glMul() {
        // Could use ForgeHooksClient, except that our matrices are twice as cool.
        GL11.glMultMatrix(toBuffer());
    }

    @Override
    public String toString() {
        return matrix.toString();
    }

    private final Matrix4d matrix = new Matrix4d();

    private Mat dupe() {
        Mat ret = new Mat();
        ret.matrix.set(this.matrix);
        return ret;
    }

    private static final DoubleBuffer matrixBuffer;
    static {
        matrixBuffer = ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
        assert matrixBuffer.isDirect();
    }

    private DoubleBuffer toBuffer() {
        final double[] buff = matrixBuffer.array();
        int row = 3;
        while (true) {
            matrix.getColumn(row, buff);
            row--;
            if (row == 0) break;
            System.arraycopy(buff, 0, buff, row * 4, 4);
        }
        matrixBuffer.flip();
        return matrixBuffer;
    }

    private static Mat newIdentity() {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        return ret;
    }

}
