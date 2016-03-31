package factorization.charge.sparkling;

import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.util.NumUtil;
import factorization.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderSparkling extends Render<EntitySparkling> {
    public RenderSparkling(RenderManager renderManager) {
        super(renderManager);
    }

    static final FzModel model = new FzModel(new ResourceLocation("factorization:item/charge/sparkle"), true, FzModel.FORMAT_ITEM);

    @Override
    protected ResourceLocation getEntityTexture(EntitySparkling entity) {
        return Core.blockAtlas;
    }

    @Override
    public void doRender(EntitySparkling entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        RenderUtil.scale3(entity.width);
        GL11.glTranslated(0, 0.5, 0);

        double s = 16;
        double jitterX = s * NumUtil.interp(entity.posX, entity.prevPosX, partialTicks);
        double dx = (jitterX - Math.floor(jitterX)) / s;
        double jitterY = s * NumUtil.interp(entity.posY, entity.prevPosY, partialTicks);
        double dy = (jitterY - Math.floor(jitterY)) / s;
        double jitterZ = s * NumUtil.interp(entity.posZ, entity.prevPosZ, partialTicks);
        double dz = (jitterZ - Math.floor(jitterZ)) / s;
        GL11.glTranslated(dx, dy, dz);

        drawModel();
        GL11.glRotated(90, 1, 0, 0);
        drawModel();
        GL11.glRotated(90, 0, 1, 0);
        drawModel();

        GL11.glPopMatrix();
    }

    void drawModel() {
        GL11.glTranslated(-0.5, -0.5, -0.5);
        model.draw();
        GL11.glTranslated(0.5, 0.5, 0.5);
    }
}
