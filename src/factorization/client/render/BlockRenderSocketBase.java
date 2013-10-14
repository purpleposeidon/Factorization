package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.FactoryType;
import factorization.common.TileEntitySocketBase;

public class BlockRenderSocketBase extends FactorizationBlockRender {
    final FactoryType forType;
    public BlockRenderSocketBase(FactoryType ft) {
        super(ft);
        forType = ft;
    }

    @Override
    protected void render(RenderBlocks rb) {
        ForgeDirection dir = ForgeDirection.EAST;
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
            
            block.begin();
            block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(dir.getOpposite())));
            block.renderRotated(Tessellator.instance, x, y, z);
            socket.renderStatic(Tessellator.instance);
        } else {
            GL11.glPushMatrix();
            GL11.glRotatef(90, 1, 0, 0);
            block.renderForInventory(rb);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected FactoryType getFactoryType() {
        return forType;
    }

}
