package factorization.charge;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.ResourceType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderMirrorStand extends FactorizationBlockRender {
    private static final int frontFace[] = {1};
    private static final int backFace[] = {0};
    private static final int sideFace[] = {2, 3, 4, 5};
    
    private static Quaternion mirrorTilt = Quaternion.getRotationQuaternionRadians(Math.toRadians(-45), 1, 0, 0);
    @Override
    public void render(RenderBlocks rb) {
        if (!world_mode) {
            return;
        }
        Core.profileStart("mirror");
        float height = 7.25F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        Icon silver = Core.registry.resource_block.getIcon(0, ResourceType.SILVERBLOCK.md);
        
        //Pole
        BlockRenderHelper block = Core.registry.blockRender;
        block.useTexture(silver);
        block.setTexture(0, null);
        block.setBlockBounds(c - radius, 0, c - radius, c + radius, height, c + radius);
        block.render(rb, x, y, z);
        
        //Base
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
        
        //Mirror
        block.setBlockBoundsOffset(2F/16F, 7.5F/16F, 2F/16F);
        //block.setBlockBoundsOffset(0, 0, 0);
        //block.setBlockBounds(0, 0, 0, 1, 1F/16F, 1);
        Icon side = BlockIcons.mirror_side;
        Icon face = BlockIcons.mirror_front;
        block.useTextures(face, face, side, side, side, side);
        
        block.begin();
        Coord here = getCoord();
        
        if (world_mode) {
            TileEntityMirror tem = here.getTE(TileEntityMirror.class);
            if (tem != null && tem.target_rotation >= 0) {
                block.translate(-0.5F, 0F, 0F);
                Quaternion trans = Quaternion.getRotationQuaternionRadians(Math.toRadians(tem.target_rotation - 90), ForgeDirection.UP);
                trans.incrMultiply(mirrorTilt);
                block.rotate(trans);
                block.translate(0.5F, -0.20F, 0.5F);
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
    public FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }
}
