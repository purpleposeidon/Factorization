package factorization.client.render;

import factorization.common.FactoryType;
import factorization.common.Texture;
import net.minecraft.src.RenderBlocks;

public class BlockRenderMirrorStand extends FactorizationBlockRender {
    @Override
    void render(RenderBlocks rb) {
        float height = 6.5F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        renderPart(rb, Texture.silver, c - radius, 0, c - radius, c + radius, height, c + radius);
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, Texture.silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }
}
