package factorization.flat.api;

import com.google.common.base.Objects;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.io.IOException;

public class FlatCoord implements IDataSerializable {
    public final Coord at;
    public final EnumFacing side;

    public FlatCoord(Coord at, EnumFacing side) {
        this(at, side, false);
    }

    public static FlatCoord unnormalized(Coord at, EnumFacing side) {
        return new FlatCoord(at, side, true);
    }

    public FlatCoord(Coord at, EnumFacing side, boolean unnormalized) {
        if (!unnormalized) {
            if (SpaceUtil.sign(side) == -1) {
                at = at.add(side);
                side = side.getOpposite();
            }
        }
        this.at = at;
        this.side = side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlatCoord flatPos = (FlatCoord) o;
        return Objects.equal(at, flatPos.at) &&
                side == flatPos.side;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(at, side);
    }

    public FlatFace get() {
        return Flat.get(at, side);
    }

    @Nullable
    public <T extends FlatFace> T get(final Class<T> tClass) {
        FlatFace ret = get();
        if (tClass.isInstance(ret)) return (T) ret;
        return null;
    }

    public void set(FlatFace face) {
        Flat.set(at, side, face);
    }

    public FlatCoord add(EnumFacing offset) {
        return new FlatCoord(at.add(offset), side);
    }

    public FlatCoord atFace(EnumFacing hand) {
        if (hand == side) return this;
        return new FlatCoord(at, hand);
    }

    public boolean exists() {
        return at.blockExists();
    }

    @Override
    public String toString() {
        return at.toString() + " ON " + side;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        Coord new_at = data.asSameShare(prefix + ":at").putIDS(at);
        EnumFacing new_side = data.asSameShare(prefix + ":side").putEnum(side);
        if (data.isReader()) {
            return new FlatCoord(new_at, new_side);
        }
        return this;
    }

    public FlatCoord flip() {
        return new FlatCoord(at.add(side), side.getOpposite(), true);
    }

    public FlatCoord copy() {
        return new FlatCoord(at.copy(), side);
    }
}
