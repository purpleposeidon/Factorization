package factorization.flat.api;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.FlatMod;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/** This class contains internal things. */
public abstract class FlatFace implements IDataSerializable {
    @Nullable
    public abstract IFlatModel getModel(Coord at, EnumFacing side);
    public abstract void loadModels(IModelMaker maker);

    public int getColor(Coord at, EnumFacing side) {
        return 0xFFFFFFFF;
    }

    public void onReplaced(Coord at, EnumFacing side) {
    }

    public void onPlaced(Coord at, EnumFacing side, EntityPlayer player, ItemStack is) {
    }

    public boolean isValidAt(Coord at, EnumFacing side) {
        return true;
    }

    public boolean isReplaceable(Coord at, EnumFacing side) {
        return false;
    }

    public void onNeighborBlockChanged(Coord at, EnumFacing side) {

    }

    public void onNeighborFaceChanged(Coord at, EnumFacing side) {

    }

    public void onActivate(Coord at, EnumFacing side, EntityPlayer player) {

    }

    public void onHit(Coord at, EnumFacing side, EntityPlayer player) {

    }

    protected static AxisAlignedBB getBounds(Coord at, EnumFacing side, double width, double height) {
        boolean off = false;
        if (SpaceUtil.sign(side) == -1) {
            off = true;
        }
        final double x = at.z;
        final double y = at.y;
        final double z = at.z;
        final double w = width;
        final double I = height;
        final double l = 0.5 - w;
        final double h = 0.5 + w;
        switch (side) {
            default:
            case EAST:  return move(off, side, x + 1, y + l, z + l, x+1+I, y + h, z + h);
            case UP:    return move(off, side, x + l, y + 1, z + l, x + h, y+1+I, z + h);
            case SOUTH: return move(off, side, x + l, y + l, z + 1, x + h, y + h, z+1+I);
        }
    }


    protected static AxisAlignedBB move(boolean off, EnumFacing side, double x0, double y0, double z0, double x1, double y1, double z1) {
        AxisAlignedBB ret = new AxisAlignedBB(x0, y0, z0, x1, y1, z1);
        if (off) {
            return ret.offset(side.getFrontOffsetX(), side.getFrontOffsetY(), side.getFrontOffsetZ());
        }
        return ret;
    }

    public void listSelectionBounds(Coord at, EnumFacing side, Entity player, IBoxList list) {
        list.add(getBounds(at, side, 0.5, 1.0 / 16.0));
    }

    /**
     *
     * @return The species. This method is provided as a way to avoid instanceof checks. Use with {@link Flat#nextSpeciesId()}
     */
    public int getSpecies() {
        return -1;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    /** @return a FlatFace that is alike this one, but whose fields can be safely modified. */
    public FlatFace cloneDynamic() {
        return this;
    }

    public final FlatFace dupe() {
        if (isStatic()) return this;
        return cloneDynamic();
    }
    public final boolean isStatic() {
        return staticId != 0;
    }

    public final boolean isDynamic() {
        return staticId == 0;
    }

    /** This field is internal. */
    public transient char staticId = FlatMod.DYNAMIC_SENTINEL;

    /** Like Block.isAir, but even more dangerous. */
    public boolean isNull() {
        return false;
    }
}
