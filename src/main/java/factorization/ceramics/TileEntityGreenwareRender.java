package factorization.ceramics;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockPos;

import factorization.shared.Core;


public class TileEntityGreenwareRender extends TileEntitySpecialRenderer<TileEntityGreenware> {

    final BlockPos zero = new BlockPos(0, 0, 0);
    @Override
    public void renderTileEntityAt(TileEntityGreenware gw, double x, double y, double z, float partialTicks, int destroyStage) {
        if (!gw.shouldRenderTesr) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        //prevents AO flickering on & off
        int lt = gw.lastTouched;
        gw.lastTouched = 0;
        bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        BlockModelRenderer mr = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer();
        Tessellator tessI = Tessellator.getInstance();
        WorldRenderer tess = tessI.getWorldRenderer();
        tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        mr.renderModel(gw.getWorld(), gw.buildModel(), Core.registry.legacy_factory_block.getDefaultState(), zero, tess, false);
        tessI.draw();
        GL11.glPopMatrix();
        gw.lastTouched = lt;
        GL11.glPopAttrib();
    }
}
