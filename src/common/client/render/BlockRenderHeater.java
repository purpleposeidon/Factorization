package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.Texture;

public class BlockRenderHeater extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        float d = 0.5F / 32F;
        if (!world_mode || !Core.renderTEs) {
            float c = 0.1F;
            GL11.glColor4f(c, c, c, 1F);
            //Tessellator.instance.setColorOpaque_F(c, c, c);
            renderPart(rb, Texture.heater_element, d, d, d, 1 - d, 1 - d, 1 - d);
            GL11.glColor4f(1, 1, 1, 1);
        }
        renderNormalBlock(rb, getFactoryType().md);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }

}
