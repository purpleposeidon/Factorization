package factorization.charge;

import factorization.shared.FzModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntityHeaterRenderer extends TileEntitySpecialRenderer<TileEntityHeater> {
    static FzModel heat = new FzModel("furnaceHeaterHeat");

    @Override
    public void renderTileEntityAt(TileEntityHeater heater, double x, double y, double z, float partial, int breaking) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GlStateManager.disableLighting();
        heat.draw(getColor(heater));
        GlStateManager.enableLighting();
        GL11.glPopMatrix();
    }

    private int getColor(TileEntityHeater heater) {
        float color = 0.1F;
        color += (heater.heat / (float) TileEntityHeater.maxHeat) * (1 - color);
        color = Math.max(color, 0);
        color = Math.min(1, color);
        int c = (int) (color * 0xFF);
        return (c << 16) | (c << 8) | c | 0xFF000000;
    }

}
