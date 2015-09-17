package factorization.beauty.wickedness;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.ObjectModel;
import net.minecraft.client.renderer.RenderBlocks;
import org.lwjgl.opengl.GL11;

public class BlockRenderMisanthropicGenerator extends FactorizationBlockRender {
    ObjectModel model = new ObjectModel(Core.getResource("models/beauty/misanthropic.obj"));

    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            TileEntityMisanthropicGenerator gen = (TileEntityMisanthropicGenerator) te;
            final boolean on = gen.isOverfilled();
            if (on) {
                return model.renderBrightISBRH(rb, BlockIcons.beauty$misanthropic_on, BlockRenderHelper.instance, x, y, z);
            } else {
                return model.renderISBRH(rb, BlockIcons.beauty$misanthropic, BlockRenderHelper.instance, x, y, z);
            }
        } else {
            GL11.glTranslatef(0, -0.5F, 0);
            model.render(BlockIcons.beauty$misanthropic);
            GL11.glTranslatef(0, +0.5F, 0);
            return true;
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MISANTHROPIC_GEN;
    }
}
