package factorization.beauty;

import factorization.shared.Core;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class TileEntitySteamShaftRenderer extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double dx, double dy, double dz, float partial) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Core.blockAtlas);
        TileEntitySteamShaft shaft = (TileEntitySteamShaft) te;
        GL11.glPushMatrix();
        GL11.glTranslated(dx + 0.5, dy + 0.5, dz + 0.5);
        double theta = NumUtil.interp(shaft.prev_angle, shaft.angle, partial);
        GL11.glRotated(Math.toDegrees(theta), 0, 1, 0);
        GL11.glPushAttrib(GL11.GL_TRANSFORM_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        BlockRenderSteamShaft.whirligig.render(BlockIcons.beauty$whirligig);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
