package factorization.flat.api;

import factorization.api.Coord;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IFlatModel {
    @SideOnly(Side.CLIENT)
    IBakedModel getModel(Coord at, EnumFacing side);
}
