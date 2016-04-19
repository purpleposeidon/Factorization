package factorization.charge;

import factorization.shared.FzModel;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TileEntityMirrorRender extends TileEntitySpecialRenderer<TileEntityMirror> {
    FzModel face = new FzModel(new ResourceLocation("factorization:block/mirrorface"), false, FzModel.FORMAT_BLOCK);
    @Override
    public void renderTileEntityAt(TileEntityMirror mirror, double x, double y, double z, float partialTicks, int destroyStage) {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        int target = mirror.getTargetInfo();
        if (target != TileEntityMirror.NO_TARGET) {
            GL11.glRotatef(target + 90, 0, 1, 0);
            GL11.glRotatef(45, 1, 0, 0);
        }
        face.draw();
        GL11.glPopMatrix();
    }
}
