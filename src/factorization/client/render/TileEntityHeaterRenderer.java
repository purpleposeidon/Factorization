package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.TileEntityHeater;

public class TileEntityHeaterRenderer extends TileEntitySpecialRenderer {
    static RenderBlocks rb = new RenderBlocks();

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        bindTexture(Core.blockAtlas);
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        float color = 0.1F;
        TileEntityHeater heater = (TileEntityHeater) te;
        color += (heater.heat / (float) TileEntityHeater.maxHeat) * (1 - color);
        color = Math.max(color, 0);
        color = Math.min(1, color);
        GL11.glColor4f(color, color, color, 1.0F);
        float d = 0;
        GL11.glTranslatef((float) x + d, (float) y + d, (float) z + d);
        BlockRenderHelper block = BlockRenderHelper.instance;
        float m = 1F/128F;
        block.setBlockBoundsOffset(m, m, m);
        block.useTexture(BlockIcons.heater_heat);
        block.begin();
        int brightness = (int)(color*16) << 4;
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setBrightness(brightness);
        Tessellator.instance.setColorOpaque_F(color, color, color);
        block.renderForTileEntity();
        GL11.glDisable(GL11.GL_BLEND);
        Tessellator.instance.draw();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

}
