package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.shared.Core;
import factorization.shared.ObjectModel;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class TileEntityShaftRenderer extends TileEntitySpecialRenderer {
    public static ObjectModel shaftModel = new ObjectModel(new ResourceLocation("factorization", "models/shaft.obj"));

    @Override
    public void renderTileEntityAt(TileEntity te, double dx, double dy, double dz, float partial) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPushAttrib(GL11.GL_TRANSFORM_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        Minecraft.getMinecraft().getTextureManager().bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(dx + 0.5, dy + 0.5, dz + 0.5);
        TileEntityShaft shaft = (TileEntityShaft) te;
        if (shaft.axis.getDirectionVec().getX() == 1) {
            GL11.glRotatef(90, 0, 0, 1);
        } else if (shaft.axis.getDirectionVec().getZ() == 1) {
            GL11.glRotatef(90, 1, 0, 0);
        }
        double angle = Math.toDegrees(NumUtil.interp(shaft.prev_angle, shaft.angle, partial));
        GL11.glRotatef((float) angle, 0, 1, 0);
        shaftModel.render(BlockIcons.beauty$shaft);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
