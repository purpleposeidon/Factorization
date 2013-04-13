package factorization.client.render;



import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.FactoryType;

public class BlockRenderLeydenJar extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        Icon glass = Block.glass.getBlockTextureFromSide(0);
        BlockRenderHelper block = BlockRenderHelper.instance;
        float inset = 1F/16F;
        float jarHeight = 13F/16F;
        float metal_height = 5F/16F;
        block.setBlockBounds(inset, 0, inset, 1 - inset, jarHeight, 1 - inset);
        block.useTextures(glass, null, glass, glass, glass, glass);
        block.render(rb, x, y, z);
        renderCauldron(rb, BlockIcons.leyden_rim, BlockIcons.leyden_metal, metal_height);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }

}
