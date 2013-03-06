package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.FzIcon;
import factorization.common.TileEntityMixer;

public class BlockRenderMixer extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        renderMotor(rb, 1F / 16F);
        float width = 2F / 16F;
        renderCauldron(rb, BlockFactorization.cauldron_top, BlockFactorization.cauldron_side);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIXER;
    }

}
