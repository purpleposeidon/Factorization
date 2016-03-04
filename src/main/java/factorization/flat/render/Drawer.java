package factorization.flat.render;

import factorization.api.Coord;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IFlatVisitor;
import factorization.util.NORELEASE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

class Drawer implements IFlatVisitor {
    final Tessellator tessI = Tessellator.getInstance();
    final WorldRenderer tess = tessI.getWorldRenderer();
    final ClientRenderInfo info;
    boolean started = false;
    static final BlockModelRenderer modelRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer();

    Drawer(ClientRenderInfo info) {
        this.info = info;
    }

    @Override
    public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
        final EnumFacing opposite = side.getOpposite();
        NORELEASE.fixme("Use IBlockAccess instead");
        NORELEASE.fixme("Consider using BlockPos instead of Coord throughout the flats. Maybe later.");
        boolean oppositeObscured = at.getBlock().isVisuallyOpaque();
        boolean normalObscured;
        {
            at.adjust(side);
            normalObscured = at.getBlock().isVisuallyOpaque();
            at.adjust(opposite);
        }
        if (normalObscured && oppositeObscured) return;
        int color = face.getColor(at, side);
        IFlatModel flatModel = face.getModel(at, side);
        if (flatModel == null) return;
        IBakedModel model = flatModel.getModel(at, side);
        if (model == null) return;

        if (!normalObscured) {
            renderModel(model, at, side);
        }
        if (!oppositeObscured) {
            at.adjust(side);
            renderModel(model, at, opposite);
            at.adjust(opposite);
        }
    }

    void renderModel(IBakedModel model, Coord at, EnumFacing side) {
        if (!started) {
            tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            started = true;
        }
        modelRenderer.renderModelStandard(at.w, model, SideSelectors.get(side), at.toBlockPos(), tess, true);
    }

    public void finish() {
        if (!started) {
            info.discardList();
            return;
        }
        tess.setTranslation(0, 0, 0);
        GL11.glNewList(info.getList(), GL11.GL_COMPILE);
        tessI.draw();
        GL11.glEndList();
    }

}
