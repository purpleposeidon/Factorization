package factorization.astro;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockFactorization;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderRocketEngine extends FactorizationBlockRender {

    @Override
    public void render(RenderBlocks rb) {
        IIcon body = BlockIcons.rocket_engine_invalid;
        IIcon nozzle = BlockIcons.rocket_engine_nozzle, bottom = BlockIcons.rocket_engine_bottom_hole, top = BlockIcons.rocket_engine_top;
        final Coord here = getCoord();
        if (world_mode) { //Really, it should be...
            TileEntityRocketEngine rocket = here.getTE(TileEntityRocketEngine.class);
            if (rocket != null && rocket.lastValidationStatus) {
                body = BlockIcons.rocket_engine_valid;
            }
        }
        BlockRenderHelper block = BlockRenderHelper.instance;
        Tessellator tess = Tessellator.instance;
        {
            tess.setBrightness(block.getMixedBrightnessForBlock(w, x, y, z));
            //tess.setBrightness(this.renderMinY > 0.0D ? l : par1Blocks.getMixedBrightnessForBlock(this.blockAccess, par2, par3 - 1, par4));
            tess.setColorOpaque_F(1, 1, 1);
        }
        float d = 2F/16F;
        float height = 6F/16F;
        float b = height*3 - 3F/16F;
        //The nozzle
        for (int i = 1; i < 4; i++) {
            if (i == 1) {
                block.useTextures(bottom, nozzle, nozzle, nozzle, nozzle, nozzle);
            } else if (i == 2) {
                block.useTextures(null, nozzle, nozzle, nozzle, nozzle, nozzle);
            }
            block.setBlockBounds(d*i, (i-1)*height, d*i,
                    2 - d*i, height*(i), 2 - d*i);
            block.begin();
            block.renderRotated(tess, here);
            //block.render(rb, here);
        }
        //The body
        block.useTextures(nozzle, top, body, body, body, body);
        block.setBlockBounds(0, b, 0,
                2, 3, 2);
        block.begin();
        block.renderRotated(tess, here);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ROCKETENGINE;
    }

}
