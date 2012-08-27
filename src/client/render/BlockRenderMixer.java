package factorization.client.render;

import net.minecraft.src.RenderBlocks;
import factorization.common.FactoryType;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        renderMotor(rb, 1F / 16F);
        int metal = 14;
        float width = 2F / 16F;
        renderPart(rb, metal, width, 0, width, 1 - width, width, 1 - width);
        renderPart(rb, metal, 0, 0, 0, width, 1, 1);
        renderPart(rb, metal, 1 - width, 0, 0, 1, 1, 1);
        renderPart(rb, metal, width, 0, 0, 1 - width, 1, width);
        renderPart(rb, metal, width, 0, 1 - width, 1 - width, 1, 1);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
