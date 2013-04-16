package factorization.client.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.common.TileEntityLeydenJar;

public class TileEntityLeydenJarRender extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityLeydenJar jar = (TileEntityLeydenJar) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        
        if (jar.sparks != null) {
            jar.sparks.render();
        }
        
        GL11.glPopMatrix();
    }

}
