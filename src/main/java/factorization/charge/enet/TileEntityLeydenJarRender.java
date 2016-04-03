package factorization.charge.enet;

import factorization.charge.sparkling.EntitySparkling;
import factorization.charge.sparkling.RenderSparkling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;


public class TileEntityLeydenJarRender extends TileEntitySpecialRenderer<TileEntityLeydenJar> {
    final RenderSparkling renderSparkling = new RenderSparkling(Minecraft.getMinecraft().getRenderManager());
    @Override
    public void renderTileEntityAt(TileEntityLeydenJar jar, double x, double y, double z, float partialTicks, int destroyStage) {
        if (jar.storage <= 0) return;
        if (jar.sparkling_cache == null) {
            jar.sparkling_cache = new EntitySparkling(jar.getWorld());
        }
        jar.sparkling_cache.setSurgeLevel(jar.storage, false);
        float p = jar.getWorld().getTotalWorldTime() + partialTicks;
        float height = jar.sparkling_cache.height;
        double base = 2.0 / 16.0 + height / 4;
        double wiggleRoom = 1 - base - height / 2;
        double h = (1 + Math.sin(p / 32 / height))*0.5 * wiggleRoom;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + base + h, z + 0.5);
        GL11.glRotatef(p / 4, 0, 1, 0);
        {
            GL11.glTranslated(0, height / -2, 0);
            renderSparkling.doRender(jar.sparkling_cache, 0, 0, 0, 0, 0);
        }
        GL11.glPopMatrix();
    }
}
