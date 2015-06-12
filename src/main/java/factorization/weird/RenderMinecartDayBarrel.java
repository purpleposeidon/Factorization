package factorization.weird;

import factorization.common.FactoryType;
import factorization.shared.FactorizationBlockRender;
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

    //public void doRender(EntityMinecart cart, double x, double y, double z, float p1, float p2) {
    //	super.doRender(cart, x, y, z, p1, p2);
    @Override
    protected void func_147910_a(EntityMinecart minecart, float partial, Block block, int metadata) {
        FactorizationBlockRender render = BlockRenderDayBarrel.getRenderer(FactoryType.DAYBARREL.md);

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        render.renderInWorld(minecart.worldObj, (int) minecart.posX, (int) minecart.posY, (int) minecart.posZ, 0, ((EntityMinecartDayBarrel) minecart).barrel);

        //int k = cart.getDisplayTileOffset();
        //GL11.glTranslatef(0.0F, - (float)k / 16.0F, 0.0F);
        //GL11.glScalef(0.66667F, 0.66667F, 0.66667F);
        //GL11.glTranslatef(0.0F, ((float) k / 16.0F), 0.0F);
        GL11.glTranslatef(-render.x - 0.5F, -render.y - 0.5F, -render.z - 0.5F);

        Tessellator.instance.startDrawingQuads();

        render.render(RenderBlocks.getInstance());
        render.renderSecondPass(RenderBlocks.getInstance());

        Tessellator.instance.draw();

        tesr.renderTileEntityAt(render.te, render.x, render.y, render.z, 0.0F);
        GL11.glPopAttrib();
    }
}
