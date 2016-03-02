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
        if (SpaceUtil.sign(side) == -1) {
            // This usually won't happen.
            at = at.add(side);
            side = side.getOpposite();
        }
        final double x = at.x;
        final double y = at.y;
        final double z = at.z;
        final double w = width;
        final double I = height;
        final double l = 0.5 - w;
        final double h = 0.5 + w;
        switch (side) {
            default:
            case EAST:  return new AxisAlignedBB(x+1-I, y + l, z + l, x+1+I, y + h, z + h);
            case UP:    return new AxisAlignedBB(x + l, y+1-I, z + l, x + h, y+1+I, z + h);
            case SOUTH: return new AxisAlignedBB(x + l, y + l, z+1-I, x + h, y + h, z+1+I);
        }
    }

    public void listSelectionBounds(Coord at, EnumFacing side, Entity player, IBoxList list) {
        list.add(getBounds(at, side, 0.5 - 1.0 / 16.0, 1.0 / 64.0));
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
        return staticId != FlatMod.DYNAMIC_SENTINEL;
    }

    public final boolean isDynamic() {
        return staticId == FlatMod.DYNAMIC_SENTINEL;
    }

    /** This field is internal. */
    public transient char staticId = FlatMod.DYNAMIC_SENTINEL;

    /** Like Block.isAir, but even more dangerous. */
    public boolean isNull() {
        return false;
    }
}
