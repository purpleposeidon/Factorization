package factorization.flat.render;

import factorization.beauty.TileEntitySteamShaftRenderer;
import factorization.flat.FlatRayTarget;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class FlatRayTargetRender extends Render<FlatRayTarget> {
    public FlatRayTargetRender(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(FlatRayTarget entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.box == null) return;
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver.entityHit != entity) return;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        AxisAlignedBB offBox = entity.box.offset(-entity.posX, -entity.posY, -entity.posZ);
        RenderGlobal.drawSelectionBoundingBox(offBox);
        GlStateManager.disableTexture2D();
        int c = 0xB0;
        GL11.glLineWidth(4);
        RenderGlobal.drawOutlinedBoundingBox(offBox, c, c, c, 0xFF);
        GlStateManager.enableTexture2D();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(FlatRayTarget entity) {
        return Core.blockAtlas;
    }
}
