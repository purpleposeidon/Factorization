package factorization.beauty;

import factorization.shared.FzModel;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class TileEntityBiblioGenRenderer extends TileEntitySpecialRenderer<TileEntityBiblioGen> {
    public static FzModel bookModel = new FzModel("beauty/book");
    public static FzModel test = NORELEASE.just(new FzModel("fancy_fence")); /* Remove the two stolen assets */

    @Override
    public void renderTileEntityAt(TileEntityBiblioGen gen, double x, double y, double z, float partialTicks, int destroyStage) {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5F, y + 0.5F, z + 0.5F);
        float angle = (float) Math.toDegrees(NumUtil.interp(gen.prev_angle, gen.angle, partialTicks));
        GL11.glRotatef(-angle, 0, 1, 0);
        GL11.glTranslatef(0, 4.125F / 16F, 0);
        double s = 5.0 / 4.0;
        GL11.glScaled(s, s, s);
        bookModel.draw();
        GL11.glPopMatrix();
    }
}
