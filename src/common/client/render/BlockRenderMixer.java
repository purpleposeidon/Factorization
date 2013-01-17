package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import factorization.common.Core;
import factorization.common.FactoryType;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        renderMotor(rb, 1F / 16F);
        int metal = 14, lead = 16 * 2 + 2,hollow = 10 + 16;
        float width = 2F / 16F;
        renderCauldron(rb, metal, hollow);
        if (!world_mode) {
            renderPart(rb, 7, width, width * 0.5F, width, 15F / 16F, 1 - width, 1 - width);
        }
    }

    @Override
    void renderSecondPass(RenderBlocks rb) {
        int water = 7;
        int color = Block.waterStill.colorMultiplier(w, x, y, z);
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        Tessellator.instance.setColorOpaque_F(r, g, b);
        rb.renderTopFace(Core.registry.factory_rendering_block, x, y - 1F / 16F, z, water);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
