package factorization.mechanics;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntityHingeRenderer extends TileEntitySpecialRenderer<TileEntityHinge> {
    @Override
    public void renderTileEntityAt(TileEntityHinge te, double cameraX, double cameraY, double cameraZ, float partial, int damage) {
        GL11.glPushMatrix();
        GL11.glTranslated(cameraX, cameraY, cameraZ);
        te.renderTesr(partial);
        GL11.glPopMatrix();
    }
}
