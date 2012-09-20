package factorization.client.render;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.VectorUV;
import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.Texture;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.ForgeDirection;

abstract public class FactorizationBlockRender implements ICoord {
    static Block metal = Block.obsidian;
    static Block glass = Block.glowStone;

    protected boolean world_mode;
    protected IBlockAccess w;
    protected int x, y, z;
    protected int metadata;
    
    private static FactorizationBlockRender renderMap[] = new FactorizationBlockRender[0xFF];
    private static FactorizationBlockRender defaultRender;
    
    public static FactorizationBlockRender getRenderer(int md) {
        FactorizationBlockRender ret = renderMap[md];
        if (ret == null) {
            return defaultRender;
        }
        return ret;
    }
    
    protected String cubeTexture = Core.texture_file_block;
    
    public FactorizationBlockRender() {
        if (getFactoryType() != null) {
            renderMap[getFactoryType().md] = this;
        } else {
            defaultRender = this;
        }
    }
    
    abstract void render(RenderBlocks rb);
    abstract FactoryType getFactoryType();
    void renderSecondPass(RenderBlocks rb) {}
    
    @Override
    public Coord getCoord() {
        if (!world_mode) return null;
        return new Coord(Minecraft.getMinecraft().theWorld, x, y, z);
    }

    public final void renderInWorld(IBlockAccess w, int wx, int wy, int wz) {
        world_mode = true;
        this.w = w;
        x = wx;
        y = wy;
        z = wz;
    }

    public final void renderInInventory() {
        world_mode = false;
        x = y = z = 0;
    }
    
    public final void setMetadata(int md) {
        metadata = md;
    }
    
    protected void renderNormalBlock(RenderBlocks rb, int md) {
//		renderPart(rb, Core.registry.factory_block.getBlockTextureFromSideAndMetadata(0, md), 0, 0, 0, 1, 1, 1);
        Block b = Core.registry.factory_rendering_block;
        b.setBlockBounds(0, 0, 0, 1, 1, 1);
        if (world_mode) {
            rb.renderStandardBlock(b, x, y, z);
        }
        else {
            Core.registry.factory_rendering_block.fake_normal_render = true;
            rb.renderBlockAsItem(b, md, 1.0F);
            Core.registry.factory_rendering_block.fake_normal_render = false;
        }
    }
    
    protected void renderPart(RenderBlocks rb, int texture, float b1, float b2, float b3,
            float b4, float b5, float b6) {
        BlockFactorization block = Core.registry.factory_rendering_block;
        block.setBlockBounds(b1, b2, b3, b4, b5, b6);
        if (world_mode) {
            Texture.force_texture = texture;
            rb.renderStandardBlock(block, x, y, z);
            Texture.force_texture = -1;
        }
        else {
            renderPartInvTexture(rb, block, texture);
        }
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    private void renderPartInvTexture(RenderBlocks renderblocks,
            Block block, int texture) {
        // This originally copied from RenderBlocks.renderBlockAsItem
        Tessellator tessellator = Tessellator.instance;

        block.setBlockBoundsForItemRender();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1F, 0.0F);
        renderblocks.renderBottomFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderblocks.renderTopFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, -1F);
        renderblocks.renderEastFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderblocks.renderWestFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(-1F, 0.0F, 0.0F);
        renderblocks.renderNorthFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderblocks.renderSouthFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }
    
    static private float getNormalizedLighting(VectorUV[] vecs) {
        float x0 = vecs[0].x, y0 = vecs[0].y, z0 = vecs[0].z;
        float x1 = vecs[1].x, y1 = vecs[1].y, z1 = vecs[1].z;
        float x2 = vecs[2].x, y2 = vecs[2].y, z2 = vecs[2].z;
        
        float x3 = x1 - x0, y3 = y1 - y0, z3 = z1 - z0;
        float x4 = x1 - x2, y4 = y1 - y2, z4 = z1 - z2;
        //this.yCoord * par1Vec3.zCoord - this.zCoord * par1Vec3.yCoord
        //this.zCoord * par1Vec3.xCoord - this.xCoord * par1Vec3.zCoord
        //this.xCoord * par1Vec3.yCoord - this.yCoord * par1Vec3.xCoord
        //v4.crossProduct(v3);
        float fx = y4*z3 - z4*y3;
        float fy = z4*x3 - x4*z3;
        float fz = x4*y3 - y4*x3;
        double length = Math.sqrt(fx*fx + fy*fy + fz*fz);
        fx /= length;
        fy /= length;
        fz /= length;
        if (fy > 0) {
            return 1;
        }
        if (fy < 0) {
            return 0.5F;
        }
        float d = 0.7071067811865475F;
        if (fz < d && fz > -d) {
            return 0.6F;
        }
        return 0.8F;
    }

    protected void renderCube(RenderingCube rc) {
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
            ForgeHooksClient.bindTexture(cubeTexture, 0);
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        
        float delta = 1F/256F;
        float zfight = rc.corner.x * delta;
        zfight *= rc.corner.y * (delta);
        zfight *= rc.corner.z * (delta);
        zfight = 1.0025F;
        for (int face = 0; face < 6; face++) {
            VectorUV[] vecs = rc.faceVerts(face);
            float color = getNormalizedLighting(vecs);
            
            Tessellator.instance.setColorOpaque_F(color, color, color);
            for (int i = 0; i < vecs.length; i++) {
                VectorUV vec = vecs[i];
                vertex(rc, vec.x*zfight, vec.y*zfight, vec.z*zfight, vec.u, vec.v);
            }
        }
        if (!world_mode) {
            Tessellator.instance.draw();
            ForgeHooksClient.unbindTexture();
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
    
    protected void vertex(RenderingCube rc, float x, float y, float z, float u, float v) {
        //all units are in texels; center of the cube is the origin. Or, like... not the center but the texel that's (8,8,8) away from the corner is.
        //u & v are in texels
        u = (int) u;
        v = (int) v;
        Tessellator.instance.addVertexWithUV(
                this.x + 0.5 + x / 16F,
                this.y + 0.5 + y / 16F,
                this.z + 0.5 + z / 16F,
                rc.ul + u / 256F, rc.vl + v / 256F);
    }
    
    public static void renderItemIn2D(int icon_index) {
        //NOTE: This is *not* suited for world_mode!
        float var6 = ((float) (icon_index % 16 * 16) + 0.0F) / 256.0F;
        float var7 = ((float) (icon_index % 16 * 16) + 15.9999F) / 256.0F;
        float var8 = ((float) (icon_index / 16 * 16) + 0.0F) / 256.0F;
        float var9 = ((float) (icon_index / 16 * 16) + 15.9999F) / 256.0F;

        renderItemIn2D_DO(Tessellator.instance, var7, var8, var6, var9);
    }

    //copied from ItemRenderer.renderItemIn2D()
    static void renderItemIn2D_DO(Tessellator par1Tessellator, float par2, float par3, float par4, float par5) {
        float var6 = 1.0F;
        float var7 = 0.0625F;
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 0.0F, 1.0F);
        par1Tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, (double) par2, (double) par5);
        par1Tessellator.addVertexWithUV((double) var6, 0.0D, 0.0D, (double) par4, (double) par5);
        par1Tessellator.addVertexWithUV((double) var6, 1.0D, 0.0D, (double) par4, (double) par3);
        par1Tessellator.addVertexWithUV(0.0D, 1.0D, 0.0D, (double) par2, (double) par3);
        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 0.0F, -1.0F);
        par1Tessellator.addVertexWithUV(0.0D, 1.0D, (double) (0.0F - var7), (double) par2, (double) par3);
        par1Tessellator.addVertexWithUV((double) var6, 1.0D, (double) (0.0F - var7), (double) par4, (double) par3);
        par1Tessellator.addVertexWithUV((double) var6, 0.0D, (double) (0.0F - var7), (double) par4, (double) par5);
        par1Tessellator.addVertexWithUV(0.0D, 0.0D, (double) (0.0F - var7), (double) par2, (double) par5);
        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        int var8;
        float var9;
        float var10;
        float var11;

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par2 + (par4 - par2) * var9 - 0.001953125F;
            var11 = var6 * var9;
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, (double) (0.0F - var7), (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, 0.0D, (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, 0.0D, (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, (double) (0.0F - var7), (double) var10, (double) par3);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(1.0F, 0.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par2 + (par4 - par2) * var9 - 0.001953125F;
            var11 = var6 * var9 + 0.0625F;
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, (double) (0.0F - var7), (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, 0.0D, (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, 0.0D, (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, (double) (0.0F - var7), (double) var10, (double) par5);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 1.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par5 + (par3 - par5) * var9 - 0.001953125F;
            var11 = var6 * var9 + 0.0625F;
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, 0.0D, (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, 0.0D, (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, (double) (0.0F - var7), (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, (double) (0.0F - var7), (double) par2, (double) var10);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, -1.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par5 + (par3 - par5) * var9 - 0.001953125F;
            var11 = var6 * var9;
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, 0.0D, (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, 0.0D, (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, (double) (0.0F - var7), (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, (double) (0.0F - var7), (double) par4, (double) var10);
        }

        par1Tessellator.draw();
    }
    

    void renderMotor(RenderBlocks rb, float yoffset) {
        int lead = Core.registry.lead_block_item.getIconIndex();
        float d = 4.0F / 16.0F;
        float yd = -d + 0.003F;
        renderPart(rb, lead, d, d + yd + yoffset, d, 1 - d, 1 - (d + 0F/16F) + yd + yoffset, 1 - d);
    }
}
