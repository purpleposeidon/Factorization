package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityCompressionCrafter;

public class BlockRenderCompressionCrafter extends FactorizationBlockRender {

    @Override
    protected void render(RenderBlocks rb) {
        BlockRenderHelper block = Core.registry.blockRender;
        Icon side = BlockIcons.compactSide;
        block.useTextures(
                BlockIcons.compactBack, BlockIcons.compactFace,
                side, side,
                side, side
                );
        block.setBlockBoundsOffset(0, 0, 0);
        ForgeDirection dir = ForgeDirection.UP;
        if (world_mode) {
            TileEntityCompressionCrafter cc = getCoord().getTE(TileEntityCompressionCrafter.class);
            if (cc == null) {
                return;
            }
            dir = cc.getFacing();
        }
        if (world_mode) {
            Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(w, x, y, z));
            Quaternion q = Quaternion.fromOrientation(FzOrientation.fromDirection(dir));
            block.begin();
            block.rotateMiddle(q);
            block.renderRotated(Tessellator.instance, x, y, z);
        } else {
            block.renderForInventory(rb);
        }
    }

    @Override
    protected FactoryType getFactoryType() {
        return FactoryType.COMPRESSIONCRAFTER;
    }

}
