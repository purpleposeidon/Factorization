package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.ObjectModel;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class BlockRenderAnthrogen extends FactorizationBlockRender {
    ObjectModel lit = new ObjectModel(new ResourceLocation("factorization", "models/beauty/lanternLit.obj"));
    ObjectModel unlit = new ObjectModel(new ResourceLocation("factorization", "models/beauty/lanternUnlit.obj"));

    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            ObjectModel model = ((TileEntityAnthroGen) te).isLit ? lit : unlit;
            return model.renderISBRH(rb, BlockIcons.beauty$anthrogen, Core.registry.factory_block, x, y, z);
        } else {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, -0.25F, 0);
            unlit.render(BlockIcons.beauty$anthrogen);
            GL11.glPopMatrix();
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ANTHRO_GEN;
    }
}
