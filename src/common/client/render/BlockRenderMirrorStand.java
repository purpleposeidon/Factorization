package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityMirror;

public class BlockRenderMirrorStand extends FactorizationBlockRender {
    private static final int frontFace[] = {1};
    private static final int backFace[] = {0};
    private static final int sideFace[] = {2, 3, 4, 5};
    
    private static Quaternion mirrorTilt = Quaternion.getRotationQuaternion(Math.toRadians(-45), 1, 0, 0);
    @Override
    void render(RenderBlocks rb) {
        Core.profileStart("mirror");
        float height = 7.25F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        Icon silver = Core.registry.silver_block_item.getIconIndex();
        renderPart(rb, silver, c - radius, 0, c - radius, c + radius, height, c + radius);
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
        
        
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.setBlockBoundsOffset(6F/8F, 0.5F, 6F/8F);
        Icon side = BlockIcons.mirror_side;
        block.useTextures(BlockIcons.mirror_back, BlockIcons.mirror_front, side, side, side, side);
        
        block.begin();
        Coord here = getCoord();
        
        if (world_mode) {
            TileEntityMirror tem = here.getTE(TileEntityMirror.class);
            if (tem != null && tem.target_rotation >= 0) {
                Quaternion trans = Quaternion.getRotationQuaternion(Math.toRadians(tem.target_rotation + 90), ForgeDirection.UP);
                trans.incrMultiply(mirrorTilt);
                block.rotate(trans);
            }
        }
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
        }
        block.renderRotated(Tessellator.instance, here);
        if (!world_mode) {
            Tessellator.instance.draw();
        }
        Core.profileEnd();
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }
}
