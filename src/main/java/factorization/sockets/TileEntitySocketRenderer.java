package factorization.sockets;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntitySocketRenderer extends TileEntitySpecialRenderer<TileEntitySocketBase> {
    @Override
    public void renderTileEntityAt(TileEntitySocketBase tes, double dx, double dy, double dz, float partial, int damage) {
        GL11.glPushMatrix();
        GL11.glTranslated(dx, dy, dz);
        tes.renderTesr(null, partial);
        GL11.glPopMatrix();
    }

}
