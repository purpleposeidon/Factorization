package factorization.client.render;

import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import net.minecraft.src.RenderBlocks;

public class BlockRenderSentryDemon extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        BlockFactorization block = Core.registry.factory_block;
        int cage = block.getBlockTextureFromSideAndMetadata(0, FactoryType.SENTRYDEMON.md);
        float h = 0.99F, l = 0.01F;
        renderPart(rb, cage, h, h, h, l, l, l);
        renderPart(rb, cage, l, l, l, h, h, h);
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.SENTRYDEMON;
    }
}
