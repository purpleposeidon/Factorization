package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        renderMotor(rb, 1F / 16F);
        float width = 2F / 16F;
        renderCauldron(rb, BlockIcons.cauldron_top, BlockIcons.cauldron_side);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
