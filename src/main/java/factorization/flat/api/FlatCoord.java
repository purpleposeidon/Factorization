package factorization.flat.api;

import com.google.common.base.Objects;
import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;

public class FlatCoord {
    public final Coord at;
    public final EnumFacing side;

    public FlatCoord(Coord at, EnumFacing side) {
        if (SpaceUtil.sign(side) == -1) {
            at = at.add(side);
            side = side.getOpposite();
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
}
