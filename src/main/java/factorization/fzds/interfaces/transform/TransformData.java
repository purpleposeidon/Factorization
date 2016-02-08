package factorization.fzds.interfaces.transform;

import factorization.api.Mat;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.shared.Core;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.util.Vec3;

import java.io.IOException;

/**
 * Represents a transformation. This class comes in two modes: a nullable 'Debased', and a nonnull 'Pure'.
 * The Pure Kind can not have null fields set; doing so will cause a runtime exception.
 *
 * This class is used in several contexts:
 *      Absolute: The values represent their face value as you'd expect. Kind is Pure.
 *      Velocity: The values represent change in an Absolute value. Kind is Pure.
 *      Interpolation: The represent absolute vaues, but null values are not affected. Kind is Any.
 * @param <Kind> Indicates whether nulls are allowed.
 */
@SuppressWarnings("Convert2Diamond")
public class TransformData<Kind extends Any> implements IDataSerializable {
    private int y;

    public static TransformData<Pure> newPure() {
        TransformData<Pure> ret = new TransformData<Pure>(false);
        ret.setPos(SpaceUtil.newVec());
        ret.setRot(new Quaternion());
        ret.setOffset(SpaceUtil.newVec());
        ret.setScale(1.0);
        return ret;
    }

    public static TransformData<Debased> newDebased() {
        return new TransformData<Debased>(true);
    }

    /**  */
    public TransformData<Any> elide() {
        //noinspection unchecked
        return (TransformData<Any>) this;
    }

    private TransformData(boolean allow_holes) {
        this.allow_holes = allow_holes;
    }

    public TransformData<Kind> copy() {
        return copyW(this, new TransformData<Kind>(allow_holes));
    }

    /** Returns a Debased copy. Any null fields will remain so. */
    public TransformData<Debased> debased() {
        return copyW(this, new TransformData<Debased>(true));
    }

    /** Returns a Pure copy. Any null fields will have default values set. */
    public TransformData<Pure> purified() {
        return copyW(this, new TransformData<Pure>(false));
    }

    private static <S extends Any, D extends Any> TransformData<D> copyW(TransformData<S> src, TransformData<D> ret) {
        if (src.getPos() != null) ret.setPos(src.getPos());
        if (src.getRot() != null) ret.setRot(new Quaternion(src.getRot()));
        if (src.getOffset() != null) ret.setOffset(src.getOffset());
        if (src.getScale() != null) ret.setScale(src.getScale());
        return ret;
    }


    private final boolean allow_holes;

    private Vec3 pos;
    private Quaternion rot;
    private Vec3 offset;
    private Double scale;

    public Vec3 getPos() { return pos; }
    public Quaternion getRot() { return rot; }
    public Vec3 getOffset() { return offset; }
    public Double getScale() { return scale; }

    public TransformData<Kind> setPos(Vec3 pos) { this.pos = dirt(pos); return this; }
    public TransformData<Kind> setRot(Quaternion rot) { this.rot = dirt(rot); return this; }
    public TransformData<Kind> setOffset(Vec3 offset) { this.offset = dirt(offset); return this; }
    public TransformData<Kind> setScale(Double scale) { this.scale = dirt(scale); return this; }

    private <T> T dirt(T obj) {
        dirty = true;
        if (obj == null && !allow_holes) throw new NullPointerException();
        return obj;
    }

    private transient boolean dirty = true;

    public boolean clean() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }

    private static final byte FLAG_POS = 0x1, FLAG_ROT = 0x2, FLAG_OFFSET = 0x4, FLAG_SCALE = 0x8;

    private static byte f(Object v, byte f) {
        return v == null ? 0 : f;
    }

    private byte flags() {
        return (byte) (f(pos, FLAG_POS) | f(rot, FLAG_ROT) | f(offset, FLAG_OFFSET) | f(scale, FLAG_SCALE));
    }

    private static boolean on(byte flag, byte FLAG) {
        return (flag & FLAG) != 0;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        byte flag = data.asSameShare(prefix + ":flags").putByte(flags());
        if (on(flag, FLAG_POS)) {
            if (pos == null) pos = SpaceUtil.newVec();
            pos = data.asSameShare(prefix + ":pos").putVec3(pos);
        }
        if (on(flag, FLAG_ROT)) {
            if (rot == null) rot = new Quaternion();
            rot = data.asSameShare(prefix + ":rot").putIDS(rot);
        }
        if (on(flag, FLAG_OFFSET)) {
            if (offset == null) offset = SpaceUtil.newVec();
            offset = data.asSameShare(prefix + ":offset").putVec3(offset);
        }
        if (on(flag, FLAG_SCALE)) {
            if (scale == null) scale = 0.0;
            scale = data.asSameShare(prefix + ":scale").putDouble(scale);
        }
        dirty |= data.isReader();
        if (data.isReader()) {
            rot = rot.cleanAbnormalNumbers();
        }
        return this;
    }

    public void multiply(TransformData data) {
        byte flag = (byte) (flags() & data.flags());
        if (on(flag, FLAG_POS)) {
            assert pos != null;
            assert data.pos != null;
            pos = pos.add(data.pos);
        }
        if (on(flag, FLAG_ROT)) {
            assert rot != null;
            assert data.rot != null;
            rot = rot.multiply(data.rot);
        }
        if (on(flag, FLAG_OFFSET)) {
            assert offset != null;
            assert data.offset != null;
            offset = offset.add(data.offset);
        }
        if (on(flag, FLAG_SCALE)) {
            assert scale != null;
            assert data.scale != null;
            scale += data.scale;
        }
        dirty |= flag != 0;
    }

    public static <T extends Any> TransformData<T> interpolate(TransformData<T> prev, TransformData<T> next, double partial) {
        TransformData ret;
        if (prev.allow_holes || next.allow_holes) {
            ret = newDebased();
        } else {
            ret = newPure();
        }
        return interpolate(prev, next, partial, ret);
    }

    public void interpolateBy(TransformData<Any> prev, TransformData<Any> next, double partial) {
        interpolate(prev, next, partial, this.elide());
    }

    private static <T extends Any> TransformData<T> interpolate(TransformData<T> prev, TransformData<T> next, double partial, TransformData<T> ret) {
        byte flag = prev.flags();
        assert flag == next.flags();
        if (on(flag, FLAG_POS)) {
            ret.setPos(NumUtil.interp(prev.pos, next.pos, partial));
        }
        if (on(flag, FLAG_ROT)) {
            ret.setRot(prev.rot.slerp(next.rot, partial));
        }
        if (on(flag, FLAG_OFFSET)) {
            ret.setOffset(NumUtil.interp(prev.offset, next.offset, partial));
        }
        if (on(flag, FLAG_SCALE)) {
            ret.setScale(NumUtil.interp(prev.scale, next.scale, partial));
        }
        return ret;
    }

    public TransformData<Any> difference(TransformData<Any> prev) {
        byte flag = prev.flags();
        TransformData<Any> ret = new TransformData<Any>(allow_holes || prev.allow_holes);
        if (on(flag, FLAG_POS)) {
            ret.setPos(pos.subtract(prev.pos));
        }
        if (on(flag, FLAG_ROT)) {
            Quaternion diff = rot.conjugate().incrMultiply(prev.rot);
            if (Core.dev_environ) {
                if (prev.rot.multiply(diff).getAngleBetween(rot) > 0.001) {
                    throw new AssertionError("Quat math fail");
                }
            }
            ret.setRot(diff);
        }
        if (on(flag, FLAG_OFFSET)) {
            ret.setOffset(offset.subtract(prev.offset));
        }
        if (on(flag, FLAG_SCALE)) {
            ret.scale = scale - prev.scale;
        }
        return ret;
    }

    @Override
    public String toString() {
        return "pos=" + pos + " rot=" + rot + " offset=" + offset + " scale=" + scale;
    }

    public Mat getMotion() {
        return Mat.mul(
                Mat.trans(pos),
                Mat.trans(offset),
                Mat.scale(scale),
                Mat.rotate(rot)
        );
    }

    public boolean isZero() {
        return (pos == null || SpaceUtil.isZero(pos))
                && (rot == null || rot.isZero())
                && (offset == null || SpaceUtil.isZero(offset))
                && (scale == null || scale == 0.0);
    }

    public double getGrandUnifiedDistance(TransformData that) {
        if (this == that) return 0;
        if (flags() != that.flags()) return Double.POSITIVE_INFINITY;

        double error = 0;
        if (pos != null) error = pos.distanceTo(that.pos);
        if (rot != null) error += rot.distance(that.rot);
        if (offset != null) error += offset.distanceTo(that.offset);
        if (scale != null) error += (scale - that.scale);
        return error;

    }

    public void setX(double x) {
        pos = new Vec3(x, pos.yCoord, pos.zCoord);
    }

    public void setY(double y) {
        pos = new Vec3(pos.xCoord, y, pos.zCoord);
    }

    public void setZ(double z) {
        pos = new Vec3(pos.xCoord, pos.yCoord,z);
    }

    public TransformData<Kind> setPos(double x, double y, double z) {
        return setPos(new Vec3(x, y, z));
    }

    public TransformData<Kind> addPos(Vec3 force) {
        return setPos(getPos().add(force));
    }

    public TransformData<Kind> addPos(double dx, double dy, double dz) {
        return setPos(pos.xCoord + dx, pos.yCoord + dy, pos.zCoord + dz);
    }

    public TransformData<Kind> mulPos(Vec3 vec3) {
        return setPos(SpaceUtil.componentMultiply(pos, vec3));
    }
}
