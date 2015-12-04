package factorization.weird;

import factorization.common.FactoryType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityMinecart;
import org.lwjgl.opengl.GL11;

public class RenderMinecartDayBarrel extends RenderMinecart {
    private static final TileEntityDayBarrelRenderer tesr = new TileEntityDayBarrelRenderer();

    static {
        tesr.func_147497_a(TileEntityRendererDispatcher.instance);
    }
    
    @Override
    protected void func_147910_a(EntityMinecart minecart, float partial, Block block, int metadata) {
        FactorizationBlockRender render = BlockRenderDayBarrel.getRenderer(FactoryType.DAYBARREL.md);

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        render.renderInWorld(minecart.worldObj, (int) minecart.posX, (int) minecart.posY, (int) minecart.posZ, 0, ((EntityMinecartDayBarrel) minecart).barrel);
        GL11.glDisable(GL11.GL_LIGHTING);

        GL11.glTranslatef(-render.x - 0.5F, -render.y - 0.5F, -render.z - 0.5F);

        Tessellator.instance.startDrawingQuads();

        render.render(RenderBlocks.getInstance());
        render.renderSecondPass(RenderBlocks.getInstance());

        Tessellator.instance.draw();

        tesr.renderTileEntityAt(render.te, render.x, render.y, render.z, 0.0F);
        GL11.glPopAttrib();
    }
}
