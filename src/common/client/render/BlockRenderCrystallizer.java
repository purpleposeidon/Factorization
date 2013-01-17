package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.Core;
import factorization.common.FactoryType;

public class BlockRenderCrystallizer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        Core.profileStart("crystallizer");
        int metal = 14, wood = 8 + 16, hollow = 10 + 16;
        float width = 2F / 16F;
        float mheight = 1 - 0;
        renderCauldron(rb, metal, hollow);

        float start = 7F / 16F;
        float sheight = 1 - width;
        renderPart(rb, wood, width, sheight, start, 1 - width, 1, start + width);
        Core.profileEnd();
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.CRYSTALLIZER;
    }

}
