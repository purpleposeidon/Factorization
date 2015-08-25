package factorization.beauty;

import factorization.shared.Core;
import factorization.shared.ObjectModel;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class TileEntityBiblioGenRenderer extends TileEntitySpecialRenderer {
    private static final ResourceLocation bookTexture = new ResourceLocation("factorization", "textures/model/book.png");
    private ObjectModel bookModel = new ObjectModel(Core.getResource("models/beauty/book.obj"));

    @Override
    public void renderTileEntityAt(TileEntity te, double dx, double dy, double dz, float partial) {
        TileEntityBiblioGen gen = (TileEntityBiblioGen) te;
        GL11.glPushMatrix();
        GL11.glTranslated(dx + 0.5F, dy + 0.5F, dz + 0.5F);
        float angle = (float) Math.toDegrees(NumUtil.interp(gen.prev_angle, gen.angle, partial));
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
        bookModel.render();
        GL11.glPopAttrib();
    }
}
