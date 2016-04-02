package factorization.flat.render;

import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class EntityHackRender extends Render<EntityHack> {
    public EntityHackRender(RenderManager renderManager) {
        super(renderManager);
        NORELEASE.fixme("Static entity renderer?");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHack entity) {
        return Core.blockAtlas;
    }

    @Override
    public void doRender(EntityHack entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glTranslated(-entity.posX, -entity.posY, -entity.posZ);
        FlatChunkLayer layer = ((IExtraChunkData) entity.chunk).getFlatLayer();
        bindTexture(Core.blockAtlas);
        ClientRenderInfo cri = (ClientRenderInfo) layer.renderInfo;
        cri.update(entity, entity.chunk, layer, entity.slabY);
        {
            GlStateManager.disableLighting();
            // Big Table o' glPolygonOffset Tests
            //(-3, -3) // What vanilla uses. Has z-fighting when looking straight down.
            //(3, 3) // invisible, except for z-fighting
            //(0, 1) // *very* z-fighty
            //(0, -1) // *very* z-fighty
            //(1, 0) // mostly invisible
            //(-1, 0) // visible; still z-fighting
            //(-10, 0) // visible; still z-fighting
            //(-100, 0) // visible; still z-fighting
            //(0, 100) // invisible, but no z-fighting
            //(0, -100) // z-fighting at extreme angles (looking past edge + shifting)
            //(-100, -100) // Seems good! But very extreme.
            //(-10, -10) // z-fighting when looking down
            //(-50, -50) // Works. Can it be lowered?
            //(-12, -12) // Z-fighting when looking down
            //(-25, -25) // Too extreme. It peaks through blocks!
            //(-25, 0) // Still peeks, also z-fights
            //(-3, 0) // No peeking, z-fights
            //(-3, -25) // Pretty good, but there's minor peeking.
            //(-3, -8) // z-fights; still peeks
            //(-3, -4) // z-fights; still peeks
            //(-1, -25) // Seems perfect!
            //(0, -25) // z-fighting
            GlStateManager.doPolygonOffset(-1, -12);
            GlStateManager.enablePolygonOffset();
            cri.draw(entity);
            GlStateManager.disablePolygonOffset();
            GlStateManager.doPolygonOffset(0, 0);
            GlStateManager.enableLighting();
        }

        GL11.glPopMatrix();
    }

}
