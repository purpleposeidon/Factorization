package factorization.beauty;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockRenderWindMill extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        ForgeDirection out = ForgeDirection.WEST;
        if (world_mode) {
            TileEntityWindMill mill = (TileEntityWindMill) te;
            out = mill.sailDirection;
        }
        BlockRenderHelper block = BlockRenderHelper.instance;
        IIcon i = BlockIcons.beauty$wind_side;
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
        block.useTextures(BlockIcons.beauty$wind_bottom, BlockIcons.beauty$wind_top, i, i, i, i);
        if (world_mode) {
            FzOrientation fzo = FzOrientation.fromDirection(out);
            block.simpleCull(fzo, w, x, y, z);
            block.beginWithRotatedUVs();
            block.rotateMiddle(Quaternion.fromOrientation(fzo));
            return block.renderRotated(Tessellator.instance, x, y, z);
        }
        block.renderForInventory(rb);
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.WIND_MILL_GEN;
    }
}
