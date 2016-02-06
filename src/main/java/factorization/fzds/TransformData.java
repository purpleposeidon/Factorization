package factorization.fzds;

import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.util.Vec3;

import javax.annotation.Nullable;
import java.io.IOException;

public final class TransformData {
    public static TransformData createNull() {
        return new TransformData();
    }

    public static TransformData createIdentity() {
        TransformData ret = new TransformData();
        ret.pos = SpaceUtil.newVec();
        ret.rot = new Quaternion();
        ret.offset = SpaceUtil.newVec();
        ret.scale = 1.0;
        return ret;
    }


    @Nullable
    private Vec3 pos;
    @Nullable
    private Quaternion rot;
    @Nullable
    private Vec3 offset;
    @Nullable
    private Double scale;

    private transient boolean dirty = true;

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

    public void putData(DataHelper data, String prefix) throws IOException {
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
    }

    public void accelerate(TransformData data) {
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

    public static TransformData interpolate(TransformData prev, TransformData next, double partial) {
        return interpolate(prev, next, partial, TransformData.createNull());
    }

    public void interpolateThis(TransformData prev, TransformData next, double partial) {
        interpolate(prev, next, partial, this);
    }

    @SuppressWarnings("ConstantConditions")
    private static TransformData interpolate(TransformData prev, TransformData next, double partial, TransformData ret) {
        byte flag = prev.flags();
        assert flag == next.flags();
        if (on(flag, FLAG_POS)) {
            ret.pos = NumUtil.interp(prev.pos, next.pos, partial);
        }
        if (on(flag, FLAG_ROT)) {
            ret.rot = prev.rot.slerp(next.rot, partial);
        }
        if (on(flag, FLAG_OFFSET)) {
            ret.offset = NumUtil.interp(prev.offset, next.offset, partial);
        }
        if (on(flag, FLAG_SCALE)) {
            ret.scale = NumUtil.interp(prev.scale, next.scale, partial);
        }
        if (flag != 0) {
            ret.dirty = true;
        }
        return ret;
    }

    public boolean clean() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }
}
