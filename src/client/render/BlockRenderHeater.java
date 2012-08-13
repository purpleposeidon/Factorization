package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.src.RenderBlocks;
import factorization.common.FactoryType;
import factorization.common.Texture;

public class BlockRenderHeater extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        float d = 0.5F / 32F;
        GL11.glColor3f(0.1F, 0.1F, 0.1F);
        renderPart(rb, Texture.heater_element, d, d, d, 1 - d, 1 - d, 1 - d);
        renderNormalBlock(rb, getFactoryType().md);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }

}
