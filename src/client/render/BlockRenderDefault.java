package factorization.client.render;

import net.minecraft.src.RenderBlocks;
import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;

public class BlockRenderDefault extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        TileEntityCommon c = getCoord().getTE(TileEntityCommon.class);
        if (c == null) {
            return;
        }
        renderNormalBlock(rb, c.getFactoryType().md);
    }

    @Override
    FactoryType getFactoryType() {
        return null;
    }

}
