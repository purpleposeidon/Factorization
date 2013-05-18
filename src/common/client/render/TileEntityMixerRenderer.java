package factorization.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.ItemIcons;
import factorization.common.TileEntityMixer;

public class TileEntityMixerRenderer extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityMixer mixer = (TileEntityMixer) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + 1.0F, (float) z + 1.0F);
        renderWithRotation(mixer.getRotation()*0.4F);
        GL11.glPopMatrix();
    }
    
    
    static void renderWithRotation(float rotation) {
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        //GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0.5F, -0.5F/8F, -0.5F);
        GL11.glRotatef(rotation, 0, 1, 0);

        float s = 0.60f;
        GL11.glScalef(s, s, s);
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0.45F);
        drawProp();
        GL11.glPopMatrix();
        
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorOpaque_F(1, 1, 1);
        BlockRenderHelper block = BlockRenderHelper.instance;
        Icon crank = ItemIcons.charge$crankshaft;
        block.useTextures(null, null, crank, crank, crank, crank);
        float d = 3F/8F;
        block.setBlockBoundsOffset(d, 2.5F/8F, d);
        block.begin();
        block.renderForTileEntity();
        //GL11.glTranslatef(-1, 0, 0);
        GL11.glTranslatef(-0.5F, -1.1F, -0.5F);
        Tessellator.instance.draw();
    }

    static void drawProp() {
        FactorizationBlockRender.renderIcon(Core.registry.fan.getIconFromDamage(0));
    }

}
