package factorization.mechanisms;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class TileEntityHingeRenderer extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity tileentity, double cameraX, double cameraY, double cameraZ, float partial) {
        TileEntityHinge te = (TileEntityHinge) tileentity;
        GL11.glPushMatrix();
        GL11.glTranslated(cameraX, cameraY, cameraZ);
        te.renderTesr(partial);
        GL11.glPopMatrix();
    }
}
