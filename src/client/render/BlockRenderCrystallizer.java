package factorization.client.render;

import net.minecraft.src.Block;
import net.minecraft.src.BlockCauldron;
import net.minecraft.src.RenderBlocks;
import factorization.common.FactoryType;

public class BlockRenderCrystallizer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        int metal = 14, wood = 8 + 16;
        float width = 2F / 16F;
        float mheight = 1 - 0;
        renderPart(rb, metal, width, 0, width, 1 - width, width, 1 - width);
        renderPart(rb, metal, 0, 0, 0, width, mheight, 1);
        renderPart(rb, metal, 1 - width, 0, 0, 1, mheight, 1);
        renderPart(rb, metal, width, 0, 0, 1 - width, mheight, width);
        renderPart(rb, metal, width, 0, 1 - width, 1 - width, mheight, 1);

        float start = 7F / 16F;
        float sheight = 1 - width;
        renderPart(rb, wood, width, sheight, start, 1 - width, 1, start + width);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.CRYSTALLIZER;
    }

}
