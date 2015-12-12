package factorization.charge;

import factorization.shared.Core;
import factorization.shared.FzModel;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntityHeaterRenderer extends TileEntitySpecialRenderer<TileEntityHeater> {
    static FzModel heat = new FzModel("furnaceHeaterHeat");

    @Override
    public void renderTileEntityAt(TileEntityHeater heater, double x, double y, double z, float partial, int breaking) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        bindTexture(Core.blockAtlas);
        float color = 0.1F;
        color += (heater.heat / (float) TileEntityHeater.maxHeat) * (1 - color);
        color = Math.max(color, 0);
        color = Math.min(1, color);
        GL11.glColor4f(color, color, color, 1.0F);
        heat.draw();
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopAttrib();
    }

}
