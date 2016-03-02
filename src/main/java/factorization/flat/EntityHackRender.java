package factorization.flat;

import factorization.api.Coord;
import factorization.beauty.TileEntityBiblioGen;
import factorization.beauty.TileEntityBiblioGenRenderer;
import factorization.beauty.TileEntitySteamShaftRenderer;
import factorization.coremodhooks.IExtraChunkData;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
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
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glTranslated(-entity.posX, -entity.posY, -entity.posZ);
        Chunk chunk = new Coord(entity).getChunk();
        FlatChunkLayer layer = ((IExtraChunkData) chunk).getFlatLayer();
        ClientRenderInfo cri = (ClientRenderInfo) layer.renderInfo;
        cri.update(chunk, layer);
        {
            GlStateManager.disableLighting();
            cri.draw();
            GlStateManager.enableLighting();
        }

        GL11.glPopMatrix();
    }

    private static class Drawer implements IFlatVisitor {
        final Tessellator tessI = Tessellator.getInstance();
        final WorldRenderer tess = tessI.getWorldRenderer();
        final ClientRenderInfo info;
        boolean started = false;
        static final BlockModelRenderer modelRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer();

        private Drawer(ClientRenderInfo info) {
            this.info = info;
        }

        @Override
        public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
            final EnumFacing opposite = side.getOpposite();
            NORELEASE.fixme("Use IBlockAccess instead");
            NORELEASE.fixme("Consider using BlockPos instead of Coord throughout the flats.");
            boolean oppositeObscured = at.isSolid();
            boolean normalObscured;
            {
                at.adjust(side);
                normalObscured = at.isSolid();
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
            modelRenderer.renderModelStandard(at.w, model, side_selectors[side.ordinal()], at.toBlockPos(), tess, true);
        }

        static class FakeBlock extends Block {
            private final EnumFacing side;
            FakeBlock(EnumFacing side) {
                super(Material.rock);
                this.side = side;
            }

            @Override
            public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
                return side == this.side;
            }
        }

        static final Block[] side_selectors = new Block[6];
        static {
            for (EnumFacing f : EnumFacing.VALUES) {
                side_selectors[f.ordinal()] = new FakeBlock(f);
            }
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

    static final class ClientRenderInfo implements IFlatRenderInfo {
        boolean dirty = true;
        boolean entitySpawned = false;

        @Override
        public void markDirty(Coord at) {
            dirty = true;
            if (!entitySpawned && at != null) {
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
                displayList = GLAllocation.generateDisplayLists(1);
            }
            return displayList;
        }

        public void draw() {
            if (displayList == -1) return;
            GL11.glCallList(displayList);
        }

        public void update(Chunk chunk, FlatChunkLayer layer) {
            if (NORELEASE.on && Core.dev_environ) dirty = true;
            if (!dirty) return;
            dirty = false;
            Drawer visitor = new Drawer(this);
            layer.iterate(chunk, visitor);
            visitor.finish();
        }
    }
}
