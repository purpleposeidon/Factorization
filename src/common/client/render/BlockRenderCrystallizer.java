package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;

public class BlockRenderCrystallizer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        Core.profileStart("crystallizer");
        int metal = 14, wood = 8 + 16, hollow = 10 + 16;
        float width = 2F / 16F;
        float mheight = 1 - 0;
        renderCauldron(rb, BlockFactorization.cauldron_side, BlockFactorization.cauldron_top);

        float start = 7F / 16F;
        float sheight = 1 - width;
        renderPart(rb, BlockFactorization.wood, width, sheight, start, 1 - width, 1, start + width);
        Core.profileEnd();
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.CRYSTALLIZER;
    }

}
