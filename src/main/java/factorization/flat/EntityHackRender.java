package factorization.flat;

import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.shared.Core;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public final class EntityHackRender extends Render<EntityHack> {
    protected EntityHackRender(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHack entity) {
        return Core.blockAtlas;
    }

    @Override
    public void doRender(EntityHack entity, double x, double y, double z, float entityYaw, float partialTicks) {
        Chunk chunk = new Coord(entity).getChunk();
        FlatChunkLayer layer = ((IExtraChunkData) chunk).getFlatLayer();
        ClientRenderInfo cri = (ClientRenderInfo) layer.renderInfo;
        cri.update(chunk, layer);
        cri.draw();
    }

    private static class Drawer implements IFlatVisitor {
        final Tessellator tessI = Tessellator.getInstance();
        final WorldRenderer tess = tessI.getWorldRenderer();
        final ClientRenderInfo info;
        boolean started = false;

        private Drawer(ClientRenderInfo info) {
            this.info = info;
        }

        @Override
        public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
            boolean normalObscured = at.isSolid();
            boolean oppositeObscured;
            {
                at.add(side);
                oppositeObscured = at.isSolid();
                at.add(side.getOpposite());
            }
            if (normalObscured && oppositeObscured) return;
            int color = face.getColor(at, side);
            IFlatModel flatModel = face.getModel(at, side);
            if (flatModel == null) return;
            IBakedModel model = flatModel.getModel(at, side);
            if (model == null) return;
            if (!started) {
                tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            }
            tess.setTranslation(at.x & 0xF, at.y, at.z & 0xF);
            for (BakedQuad quad : model.getGeneralQuads()) {
                LightUtil.renderQuadColor(tess, quad, color);
            }
            if (!normalObscured) {
                for (BakedQuad quad : model.getFaceQuads(side)) {
                    LightUtil.renderQuadColor(tess, quad, color);
                }
            }
            if (!oppositeObscured) {
                for (BakedQuad quad : model.getFaceQuads(side)) {
                    LightUtil.renderQuadColor(tess, quad, color);
                }
            }
        }

        public void finish() {
            if (!started) {
                info.discardList();
                return;
            }
            GL11.glNewList(info.getList(), GL11.GL_COMPILE);
            tessI.draw();
            GL11.glEndList();
        }
    }

    static final class ClientRenderInfo implements IFlatRenderInfo {
        boolean dirty = true;
        boolean entitySpawned = false;

        @Override
        public void markDirty(Coord at) {
            dirty = true;
            if (!entitySpawned) {
                entitySpawned = true;
                EntityHack hack = new EntityHack(at.getChunk());
                at.w.spawnEntityInWorld(hack);
            }
        }

        int displayList = -1;

        void discardList() {
            if (displayList == -1) return;
            GLAllocation.deleteDisplayLists(displayList);
        }

        @Override
        public void discard() {
            discardList();
        }

        int getList() {
            if (displayList == -1) {
                displayList = GLAllocation.generateDisplayLists(-1);
            }
            return displayList;
        }

        public void draw() {
            if (displayList == -1) return;
            GL11.glCallList(displayList);
        }

        public void update(Chunk chunk, FlatChunkLayer layer) {
            if (dirty) return;
            dirty = false;
            Drawer visitor = new Drawer(this);
            layer.iterate(chunk, visitor);
            visitor.finish();
        }
    }
}
