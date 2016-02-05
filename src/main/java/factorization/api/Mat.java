package factorization.api;

import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

/**
 * This is a 4x4 immutable matrix.
 * It's presently a wrapper around the mutable javax Matrix4d, but I'm not sure if I can rely on javax being present
 * on the server. If it turns out I can't, that's OK as this is well-encapsulated.
 */
public final class Mat {
    public static Mat identity() {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        return ret;
    }

    public static Mat trans(Vec3 delta) {
        return trans(delta.xCoord, delta.yCoord, delta.zCoord);
    }

    public static Mat trans(double dx, double dy, double dz) {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.setTranslation(new Vector3d(dx, dy, dz));
        return ret;
    }

    public static Mat rotate(Quaternion quat) {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.setRotation(quat.toJavaxD());
        return ret;
    }

    public static Mat scale(double v) {
        Mat ret = new Mat();
        ret.matrix.setIdentity();
        ret.matrix.setScale(v);
        return ret;
    }

    public static Mat mul(Mat... values) {
        Mat accum = identity();
        for (Mat v : values) {
            accum.matrix.mul(v.matrix);
        }
        return accum;
    }


    private final Matrix4d matrix = new Matrix4d();

    private Mat dupe() {
        Mat ret = new Mat();
        ret.matrix.set(this.matrix);
        return ret;
    }

    public Mat invert() {
        Mat ret = dupe();
        ret.matrix.invert(this.matrix);
        return ret;
    }

    public Mat transpose() {
        Mat ret = new Mat();
        ret.matrix.transpose(this.matrix);
        return ret;
    }

    public Vec3 mul(Vec3 v) {
        return new Vec3(
                matrix.m00 * v.xCoord + matrix.m01 * v.yCoord + matrix.m02 * v.zCoord + matrix.m03,
                matrix.m10 * v.xCoord + matrix.m11 * v.yCoord + matrix.m12 * v.zCoord + matrix.m13,
                matrix.m20 * v.xCoord + matrix.m21 * v.yCoord + matrix.m22 * v.zCoord + matrix.m23);
    }

    public Vector3d applyTo(Vector3d val) {
        Matrix4d buffer = new Matrix4d();
        buffer.setTranslation(val);
        buffer.mul(matrix, buffer);
        Vector3d ret = new Vector3d();
        buffer.get(ret);
        return ret;
    }

    public Vec3 applyTo(Vec3 val) {
        Vector3d vaxBuffer = SpaceUtil.toJavax(val);
        Matrix4d matBuffer = new Matrix4d();
        matBuffer.setTranslation(vaxBuffer);
        matBuffer.mul(matrix, matBuffer);
        matBuffer.get(vaxBuffer);
        return new Vec3(vaxBuffer.getX(), vaxBuffer.getY(), vaxBuffer.getZ());
    }

    @SideOnly(Side.CLIENT)
    public void multiplyGl() {
        // Could use ForgeHooksClient, but our matrices are twice as good.
        GL11.glMultMatrix(toBuffer());
    }

    @SideOnly(Side.CLIENT)
    private DoubleBuffer toBuffer() {
        final DoubleBuffer matrixBuffer = ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
        assert matrixBuffer.isDirect();
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

    @Override
    public String toString() {
        return matrix.toString();
    }
}
