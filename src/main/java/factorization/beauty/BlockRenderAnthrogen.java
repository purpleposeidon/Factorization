package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.ObjectModel;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.ResourceLocation;

public class BlockRenderAnthrogen extends FactorizationBlockRender {
    ObjectModel lit = new ObjectModel(new ResourceLocation("factorization", "models/beauty/lanternLit.obj"));
    ObjectModel unlit = new ObjectModel(new ResourceLocation("factorization", "models/beauty/lanternUnlit.obj"));

    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            ObjectModel model = ((TileEntityAnthroGen) te).isLit ? lit : unlit;
            return model.renderISBRH(rb, BlockIcons.beauty$anthrogen, Core.registry.factory_block, x, y, z);
        } else {
            lit.render(BlockIcons.beauty$anthrogen);
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ANTHRO_GEN;
    }
}
