package factorization.flat.render;

import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IFlatVisitor;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public final class EntityHackRender extends Render<EntityHack> {
    public EntityHackRender(RenderManager renderManager) {
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

}
