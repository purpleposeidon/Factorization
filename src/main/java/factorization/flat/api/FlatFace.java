package factorization.flat.api;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.FlatMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
