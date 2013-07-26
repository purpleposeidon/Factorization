package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;

public class BlockRenderDefault extends FactorizationBlockRender {

    @Override
    protected
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
    protected
    FactoryType getFactoryType() {
        return null;
    }

}
