package factorization.flat;

import factorization.api.Coord;
import factorization.api.datahelpers.IDataSerializable;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class FlatFace implements IDataSerializable {
    @SideOnly(Side.CLIENT)
    public void render(WorldRenderer tess, Coord at, EnumFacing side) {

    }

    public final boolean isStatic() {
        return staticId != 0;
    }

    public final boolean isDynamic() {
        return staticId == 0;
    }

    transient char staticId = FlatFeature.DYNAMIC_SENTINEL;

    public void onReplaced(Coord at, EnumFacing side) {
    }

    public void onPlaced(Coord at, EnumFacing side) {
    }
}
