package factorization.charge;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;


public class TileEntityLeydenJarRender extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial, int breaking) {
        TileEntityLeydenJar jar = (TileEntityLeydenJar) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);

        // NORELEASE: Gonna need sparks!

        GL11.glPopMatrix();
    }

}
