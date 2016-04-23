package factorization.mechanics;

import factorization.shared.FzModel;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntityHingeRenderer extends TileEntitySpecialRenderer<TileEntityHinge> {
    FzModel top = new FzModel("hingeTop");

    @Override
    public void renderTileEntityAt(TileEntityHinge te, double cameraX, double cameraY, double cameraZ, float partial, int damage) {
        GL11.glPushMatrix();
        GL11.glTranslated(cameraX, cameraY, cameraZ);
        top.draw();
        GL11.glPopMatrix();
    }
}
