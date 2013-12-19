package factorization.shared;

import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderDefault extends FactorizationBlockRender {

    @Override
    public void render(RenderBlocks rb) {
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
    public FactoryType getFactoryType() {
        return null;
    }

}
