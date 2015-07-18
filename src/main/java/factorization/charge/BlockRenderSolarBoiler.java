package factorization.charge;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.TileEntityCommon;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;

public class BlockRenderSolarBoiler extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            TileEntityCommon c = getCoord().getTE(TileEntityCommon.class);
            if (c == null) {
                return false;
            }
            return renderNormalBlock(rb, c.getFactoryType().md);
        } else {
            renderNormalBlock(rb, metadata);
        }
        return false;
    }

    @Override
    public boolean renderSecondPass(RenderBlocks rb) {
        if (!world_mode) return false;
        TileEntitySolarBoiler boiler = (TileEntitySolarBoiler) te;
        float alpha = boiler.last_synced_heat;
        if (alpha <= 0) return false;
        alpha = (float) Math.log10(alpha) / 6;
        if (alpha > 0.4F) alpha = 0.4F;
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(BlockIcons.heater_heat);
        float s = -1 / 512F;
        block.setBlockBoundsOffset(s, s, s);
        block.alpha = alpha;
        block.beginWithRotatedUVs();
        block.renderRotated(Tessellator.instance, x, y, z);
        block.alpha = 1;
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOLARBOILER;
    }
}
