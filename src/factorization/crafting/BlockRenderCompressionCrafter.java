package factorization.crafting;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderCompressionCrafter extends FactorizationBlockRender {

    @Override
    public void render(RenderBlocks rb) {
        BlockRenderHelper block = Core.registry.blockRender;
        Icon side = BlockIcons.compactSide;
        block.useTextures(
                BlockIcons.compactBack, BlockIcons.compactFace,
                side, side,
                side, side);
        block.setBlockBoundsOffset(0, 0, 0);
        ForgeDirection dir = ForgeDirection.WEST;
        if (world_mode) {
            TileEntityCompressionCrafter cc = (TileEntityCompressionCrafter) te;
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
            
            /*
            block.useTextures(
                    BlockIcons.compactFace, null,
                    null, null,
                    null, null);
            block.begin();
            block.rotateMiddle(q);
            float d = -1F/10000F;
            block.translate(dir.offsetX*d, dir.offsetY*d, dir.offsetZ*d);
            block.renderRotated(Tessellator.instance, x + dir.offsetX, y + dir.offsetY , z + dir.offsetZ);
            */
        } else {
            block.renderForInventory(rb);
            final float d = 1F/64F;
            block.setBlockBoundsOffset(d, 0, d);
            Icon s = BlockIcons.compactSideSlide;
            block.useTextures(null, null, s, s, s, s);
            block.renderForInventory(rb);
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.COMPRESSIONCRAFTER;
    }

}
