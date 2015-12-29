package factorization.weird.barrel;

import factorization.idiocy.WrappedItemStack;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.List;

public class BarrelModel implements ISmartBlockModel {
    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        Block use = null;
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState bs = (IExtendedBlockState) state;
            WrappedItemStack x = bs.getValue(BlockBarrel.BARREL_LOG);
            use = DataUtil.getBlock(x.stack);
        }
        if (use == null) use = Blocks.stone;
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(use.getDefaultState());
    }

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing face) {
        return null;
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return null;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.planks.getDefaultState());
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }
}
