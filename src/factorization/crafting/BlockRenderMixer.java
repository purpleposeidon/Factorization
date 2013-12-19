package factorization.crafting;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.BlockIcons;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FactoryType;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    public void render(RenderBlocks rb) {
        if (world_mode) {
            renderMotor(rb, 1F / 16F);
        }
        renderCauldron(rb, BlockIcons.cauldron_top, BlockIcons.cauldron_side);
        if (!world_mode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-0.5F, 0.65F, 0.5F);
            TileEntityMixerRenderer.renderWithRotation(0);
            GL11.glPopMatrix();
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
