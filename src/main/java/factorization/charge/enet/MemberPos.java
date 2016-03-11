package factorization.charge.enet;

import com.google.common.base.Objects;
import factorization.api.Coord;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

class MemberPos {
    final int x, y, z;
    final byte side;

    MemberPos(int[] xs) {
        x = xs[0];
        y = xs[1];
        z = xs[2];
        side = (byte) xs[3];
    }

    MemberPos(FlatCoord fc) {
        this(fc.at, fc.side);
    }

    MemberPos(Coord at, EnumFacing side) {
        if (SpaceUtil.sign(side) == -1) {
            at = at.add(side);
            side = side.getOpposite();
        }
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
        this.side = (byte) side.ordinal();
    }

    NBTTagIntArray toArray() {
        int[] ret = new int[4];
        ret[0] = x;
        ret[1] = y;
        ret[2] = z;
        ret[3] = side;
        return new NBTTagIntArray(ret);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberPos memberPos = (MemberPos) o;
        return x == memberPos.x &&
                y == memberPos.y &&
                z == memberPos.z &&
                side == memberPos.side;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y, z, side);
    }

    Coord getCoord(World w) {
        return new Coord(w, x, y, z);
    }

    FlatFace get(FlatCoord world) {
        return Flat.get(getCoord(world.at.w), EnumFacing.getFront(side));
    }

    FlatFace get(Coord world) {
        return Flat.get(getCoord(world.w), EnumFacing.getFront(side));
    }

    public void set(World world, FlatFace face) {
        Flat.set(getCoord(world), EnumFacing.getFront(side), face);
    }

    public void set(World world, FlatFace face, byte flags) {
        Flat.setWithNotification(getCoord(world), EnumFacing.getFront(side), face, flags);
    }
}
