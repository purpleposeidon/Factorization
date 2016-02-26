package factorization.flat;

import factorization.api.Coord;
import factorization.api.datahelpers.IDataSerializable;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public abstract class FlatFace implements IDataSerializable {
    @SideOnly(Side.CLIENT)
    @Nullable
    public abstract IBakedModel getModel(Coord at, EnumFacing side);

    public final boolean isStatic() {
        return staticId != 0;
    }

    public final boolean isDynamic() {
        return staticId == 0;
    }

    transient char staticId = FlatFeature.DYNAMIC_SENTINEL;

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

    public void onBlockNeighborChanged(Coord at, EnumFacing side) {

    }

    public void onFaceNeighborChanged(Coord at, EnumFacing side) {

    }

    public final FlatFace dupe() {
        if (isStatic()) return this;
        return cloneDynamic();
    }

    /** @return a FlatFace that is alike this one, but whose fields can be safely modified. */
    protected FlatFace cloneDynamic() {
        return this;
    }
}
