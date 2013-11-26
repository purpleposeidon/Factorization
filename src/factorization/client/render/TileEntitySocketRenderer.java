package factorization.client.render;

import org.lwjgl.opengl.GL11;

import factorization.common.TileEntitySocketBase;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySocketRenderer extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity tileentity, double dx, double dy, double dz, float partial) {
        if (tileentity instanceof TileEntitySocketBase) {
            TileEntitySocketBase tes = (TileEntitySocketBase) tileentity;
            GL11.glPushMatrix();
            GL11.glTranslated(dx, dy, dz);
            tes.renderTesr(null, partial);
            GL11.glPopMatrix();
        }
    }

}
