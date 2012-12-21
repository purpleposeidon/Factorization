package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;

public class BlockRenderDefault extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            TileEntityCommon c = getCoord().getTE(TileEntityCommon.class);
            if (c == null) {
                return;
            }
            renderNormalBlock(rb, c.getFactoryType().md);
        } else {
            renderNormalBlock(rb, metadata);
        }
    }

    @Override
    FactoryType getFactoryType() {
        return null;
    }

}
