package factorization.sockets;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumFacing;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderSocketBase extends FactorizationBlockRender {
    final FactoryType forType;
    public BlockRenderSocketBase(FactoryType ft) {
        super(ft);
        forType = ft;
    }

    @Override
    public boolean render(RenderBlocks rb) {
        EnumFacing dir = EnumFacing.EAST;
        TileEntitySocketBase socket;
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.setBlockBounds(0, 0.75F, 0, 1, 1, 1);
        block.useTextures(BlockIcons.socket$face, BlockIcons.socket$face,
                BlockIcons.socket$side, BlockIcons.socket$side,
                BlockIcons.socket$side, BlockIcons.socket$side, 
                BlockIcons.socket$side, BlockIcons.socket$side);
        
        if (world_mode) {
            Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(w, x, y, z));
            socket = (TileEntitySocketBase) te;
            dir = socket.facing;
            
            block.beginWithMirroredUVs();
            block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(dir.getOpposite())));
            block.renderRotated(Tessellator.instance, x, y, z);
            socket.renderStatic(null, Tessellator.instance);
        } else {
            GL11.glPushMatrix();
            GL11.glRotatef(90, 1, 0, 0);
            block.renderForInventory(rb);
            GL11.glPopMatrix();
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return forType;
    }

}
