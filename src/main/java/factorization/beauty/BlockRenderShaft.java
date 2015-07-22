package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderShaft extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) return false;
        TileEntityShaftRenderer.shaftModel.render(BlockIcons.beauty$shaft);
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SHAFT;
    }
}
