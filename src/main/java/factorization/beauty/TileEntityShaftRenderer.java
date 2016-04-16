package factorization.beauty;

import factorization.shared.Core;
import factorization.util.NumUtil;
import factorization.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import org.lwjgl.opengl.GL11;

public class TileEntityShaftRenderer extends TileEntitySpecialRenderer<TileEntityShaft> {
    @Override
    public void renderTileEntityAt(TileEntityShaft shaft, double dx, double dy, double dz, float partial, int destroyingStage) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(dx + 0.5, dy + 0.5, dz + 0.5);
        if (shaft.axis.getDirectionVec().getX() == 1) {
            GL11.glRotatef(90, 0, 0, 1);
        } else if (shaft.axis.getDirectionVec().getZ() == 1) {
            GL11.glRotatef(90, 1, 0, 0);
        }
        double angle = Math.toDegrees(NumUtil.interp(shaft.prev_angle, shaft.angle, partial));
        GL11.glRotatef((float) angle, 0, 1, 0);
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem ri = mc.getRenderItem();
        RenderUtil.scale3(2);
        IBakedModel model = ri.getItemModelMesher().getItemModel(shaft.shaftItem);
        ri.renderItem(shaft.shaftItem, model);
        GL11.glPopMatrix();
    }
}
