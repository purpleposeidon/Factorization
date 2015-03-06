package factorization.crafting;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            renderMotor(rb, 1F / 16F);
        }
        renderCauldron(rb, BlockIcons.mixer);
        if (!world_mode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-0.5F, 0.65F, 0.5F);
            TileEntityMixerRenderer.renderWithRotation(0);
            GL11.glPopMatrix();
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
