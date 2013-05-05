package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.common.BlockFactorization;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.TileEntityRocketEngine;

public class BlockRenderRocketEngine extends FactorizationBlockRender {

    @Override
    protected
    void render(RenderBlocks rb) {
        boolean oldAo = rb.enableAO;
        rb.enableAO = false;
        Icon body = BlockIcons.rocket_engine_invalid;
        Icon nozzle = BlockIcons.rocket_engine_nozzle, bottom = BlockIcons.rocket_engine_bottom_hole, top = BlockIcons.rocket_engine_top;
        if (world_mode) { //Really, it should be...
            TileEntityRocketEngine rocket = getCoord().getTE(TileEntityRocketEngine.class);
            if (rocket != null && rocket.lastValidationStatus) {
                body = BlockIcons.rocket_engine_valid;
            }
        }
        float d = 2F/16F;
        float height = 6F/16F;
        float b = height*3 - 3F/16F;
        //The nozzle
        BlockFactorization.sideDisable = ForgeDirection.DOWN.flag;
        for (int i = 1; i < 4; i++) {
            if (i == 3) {
                BlockFactorization.sideDisable |= ForgeDirection.UP.flag;
            }
            renderPart(rb, nozzle,
                    d*i, (i-1)*height, d*i,
                    2 - d*i, height*(i), 2 - d*i);
        }
        BlockFactorization.sideDisable = -1 ^ (ForgeDirection.DOWN.flag);
        int i = 1;
        renderPart(rb, bottom,
                d*i, (i-1)*height, d*i,
                2 - d*i, height*(i), 2 - d*i);
        
        //The body
        BlockFactorization.sideDisable = ForgeDirection.DOWN.flag | ForgeDirection.UP.flag;
        renderPart(rb, body,
                0, b, 0,
                2, 3, 2);
        
        BlockFactorization.sideDisable = -1 ^ ForgeDirection.DOWN.flag;
        renderPart(rb, nozzle,
                0, b, 0,
                2, 3, 2);
        BlockFactorization.sideDisable = -1 ^ ForgeDirection.UP.flag;
        renderPart(rb, top,
                0, b, 0,
                2, 3, 2);
        
        BlockFactorization.sideDisable = 0;
        rb.enableAO = oldAo;
    }

    @Override
    protected
    FactoryType getFactoryType() {
        return FactoryType.ROCKETENGINE;
    }

}
