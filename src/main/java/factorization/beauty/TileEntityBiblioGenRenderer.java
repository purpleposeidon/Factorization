package factorization.beauty;

import factorization.shared.FzModel;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class TileEntityBiblioGenRenderer extends TileEntitySpecialRenderer {
    private static final ResourceLocation bookTexture = new ResourceLocation("factorization", "textures/model/book.png");
    private FzModel bookModel = new FzModel("beauty/book");

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
        TileEntityBiblioGen gen = (TileEntityBiblioGen) te;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5F, y + 0.5F, z + 0.5F);
        float angle = (float) Math.toDegrees(NumUtil.interp(gen.prev_angle, gen.angle, partialTicks));
        GL11.glRotatef(-angle, 0, 1, 0);
        GL11.glTranslatef(0, 4.125F / 16F, 0);
        drawBook();
        GL11.glPopMatrix();
    }

    void drawBook() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(bookTexture);
        double s = 5.0 / 4.0;
        GL11.glScaled(s, s, s);
        GL11.glPushAttrib(GL11.GL_TRANSFORM_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        bookModel.draw();
        GL11.glPopAttrib();
    }
}
