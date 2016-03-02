package factorization.flat.render;

import factorization.flat.FlatRayTarget;
import factorization.shared.Core;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class FlatRayTargetRender extends Render<FlatRayTarget> {
    public FlatRayTargetRender(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(FlatRayTarget entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.box == null) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        RenderGlobal.drawSelectionBoundingBox(entity.box.offset(-entity.posX, -entity.posY, -entity.posZ));
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(FlatRayTarget entity) {
        return Core.blockAtlas;
    }
}
