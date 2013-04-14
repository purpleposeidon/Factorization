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
        Icon glass = BlockIcons.leyden_glass_side;
        Icon knob = BlockIcons.leyden_knob;
        BlockRenderHelper block = BlockRenderHelper.instance;
        float inset = 1F/16F;
        float jarHeight = 13F/16F;
        float metal_height = 5F/16F;
        float knob_in = 5F/16F;
        float post_in = 7F/16F;
        float z = 1F/64F;
        
        block.setBlockBounds(inset, 0, inset, 1 - inset, jarHeight, 1 - inset);
        block.useTextures(null, BlockIcons.leyden_glass, glass, glass, glass, glass);
        renderBlock(rb, block);
        
        block.useTexture(knob);
        block.setBlockBounds(knob_in, jarHeight - z, knob_in, 1 - knob_in, 1, 1 - knob_in);
        renderBlock(rb, block);
        
        
        Icon leyden_metal = BlockIcons.leyden_metal;
        renderCauldron(rb, BlockIcons.leyden_rim, leyden_metal, metal_height);
        block.useTextures(null, null, leyden_metal, leyden_metal, leyden_metal, leyden_metal);
        block.setBlockBounds(post_in, 1F/16F, post_in, 1 - post_in, jarHeight, 1 - post_in);
        renderBlock(rb, block);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }
    
    void renderBlock(RenderBlocks rb, BlockRenderHelper block) {
        if (world_mode) {
            block.render(rb, x, y, z);
        } else {
            block.renderForInventory(rb);
        }
    }

}
