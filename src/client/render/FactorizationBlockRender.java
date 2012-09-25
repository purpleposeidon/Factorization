package factorization.client.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoord;
import factorization.api.VectorUV;
import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.Texture;
import factorization.common.WireRenderingCube;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.IWorldAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;

abstract public class FactorizationBlockRender implements ICoord {
    static Block metal = Block.obsidian;
    static Block glass = Block.glowStone;

    protected boolean world_mode, use_vertex_offset;
    protected IBlockAccess w;
    protected int x, y, z;
    protected int metadata;
    protected TileEntity te;
    
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
        if (!world_mode) {
            if (te != null) {
                return new Coord(te);
            }
            return null;
        }
        return new Coord(Minecraft.getMinecraft().theWorld, x, y, z);
    }

    public final void renderInWorld(IBlockAccess w, int wx, int wy, int wz) {
        world_mode = true;
        this.w = w;
        x = wx;
        y = wy;
        z = wz;
        use_vertex_offset = true;
        te = null;
    }

    public final void renderInInventory() {
        world_mode = false;
        x = y = z = 0;
        use_vertex_offset = true;
        te = null;
    }
    
    public final void setTileEntity(TileEntity t) {
        te = t;
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
    
    
    static private RenderBlocks rb = new RenderBlocks();


    private int getMixedBrightnessForBlock(IBlockAccess w, int x, int y, int z) {
        return w.getLightBrightnessForSkyBlocks(x, y, z, Block.lightValue[w.getBlockId(x, y, z)]);
        //Block b = Block.blocksList[w.getBlockId(x, y, z)];
        //return w.getLightBrightnessForSkyBlocks(x, y, z, b.getLightValue(w, x, y, z));
        //return par1IBlockAccess.getLightBrightnessForSkyBlocks(par2, par3, par4, getLightValue(par1IBlockAccess, par2, par3, par4));
    }
    
    private int getAoBrightness(int a, int b, int c, int d) {
        return rb.getAoBrightness(a, b, c, d);
    }
    
    private float getAmbientOcclusionLightValue(IBlockAccess w, int x, int y, int z) {
        return Block.stone.getAmbientOcclusionLightValue(w, x, y, z);
    }
    
    static private ForgeDirection getFaceDirection(VectorUV[] vecs, VectorUV center) {
        VectorUV here = vecs[0].add(vecs[2]);
        here.scale(0.5F);
        here = here.add(center);
        float x = Math.abs(here.x), y = Math.abs(here.y), z = Math.abs(here.z);
        if (x >= y && x >= z) {
            return here.x >= 0 ? ForgeDirection.WEST : ForgeDirection.EAST;
        }
        if (y >= x && y >= z) {
            return here.y >= 0 ? ForgeDirection.UP : ForgeDirection.DOWN;
        }
        if (z >= x && z >= y) {
            return here.z >= 0 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
        }
        return ForgeDirection.UP;
    }
    
    static float[] directionLighting = new float[] {0.5F, 1F, 0.8F, 0.8F, 0.6F, 0.6F};
    static private float getNormalizedLighting(VectorUV[] vecs, VectorUV center) {
        return directionLighting[getFaceDirection(vecs, center).ordinal()];
        /*
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
        return 0.8F;*/
    }

    
    private void vectorAO(RenderingCube rc, VectorUV vec, ForgeDirection face) {
        DeltaCoord outward = new DeltaCoord(face.offsetX, face.offsetY, face.offsetZ);		
        DeltaCoord corner = new DeltaCoord((int) vec.x, (int) vec.y, (int)vec.z);
        float s = 16;
        Coord here = getCoord();
        int normalAxis = -1;
        for (int i = 0; i < 3; i++) {
            int caxis = corner.get(i);
            if (Math.abs(caxis) > 8) {
                here.set(i, (int) (here.get(i) + Math.signum(caxis)));
            }
            if (Math.abs(outward.get(i)) == 1) {
                corner.set(i, outward.get(i));
                normalAxis = i;
            } else {
                float sig = Math.signum(caxis);
                corner.set(i, (int) sig);
            }
        }
         
        int mixedBrightness[] = new int[3];
        float aoLightValue[] = new float[3];
        boolean aoGrass[] = new boolean[3];
        int array_index = 1;
        for (int i = 0; i < 3; i++) {
            if (i == normalAxis) {
                //get the corner piece
                Coord pos = here.add(corner);
                {
                    mixedBrightness[0] = getMixedBrightnessForBlock(pos.w, pos.x, pos.y, pos.z);
                    aoLightValue[0] = getAmbientOcclusionLightValue(pos.w, pos.x, pos.y, pos.z);
                    aoGrass[0] = Block.canBlockGrass[pos.getId()];
                }
            } else {
                int store = corner.get(i);
                corner.set(i, 0);
                Coord pos = here.add(corner);
                {
                    mixedBrightness[array_index] = getMixedBrightnessForBlock(pos.w, pos.x, pos.y, pos.z);
                    aoLightValue[array_index] = getAmbientOcclusionLightValue(pos.w, pos.x, pos.y, pos.z);
                    aoGrass[array_index] = Block.canBlockGrass[pos.getId()];
                }
                array_index++;
                corner.set(i, store);
            }
        }
        Coord front = here.add(face);
        int here_mixed = Block.stone.getMixedBrightnessForBlock(front.w, front.x, front.y, front.z);
        float hereAmbient = getAmbientOcclusionLightValue(here.w, here.x, here.y, here.z);
        if (!aoGrass[1] && !aoGrass[2]) {
            mixedBrightness[0] = here_mixed;
            aoLightValue[0] = hereAmbient;
        }
        int brightness = getAoBrightness(mixedBrightness[0], mixedBrightness[1], mixedBrightness[2], here_mixed);
        Tessellator.instance.setBrightness(brightness);
        
        float color = aoLightValue[0] + aoLightValue[1] + aoLightValue[2] + hereAmbient;
        float scale = faceColor/3F; //So, uhm. Why does using 4 make this too dark? o_O
        Tessellator.instance.setColorOpaque_F(color*scale, color*scale, color*scale);
        vertex(rc, vec);
    }
    
    float faceColor;
    boolean isDebugVertex = false;
    
    static boolean force_inv = true;

    protected void renderCube(RenderingCube rc) {
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
            ForgeHooksClient.bindTexture(cubeTexture, 0);
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        force_inv = false;
        float delta = 1F/256F;
        float zfight = rc.corner.x * delta; //TODO: Make this a field
        //also: zfight should just depend on size.
        //and we'll handle overlapping identically sized by shifting slightly based on the cube's origin. err. transformation matrix.
        zfight *= rc.corner.y * (delta);
        zfight *= rc.corner.z * (delta);
        zfight = 1.0025F;
        
        VectorUV center = rc.trans.apply(new VectorUV(0, 0, 0));
        
        
        if ((te != null || world_mode) && !force_inv) {
            //<other stuff's supposed to go here>
            //for each vertex:
            //tess.setColorOpaque_F(color of that vertex)
            //tess.setBrightness(brightness of that vertex)
            //add vertex
            Coord here = getCoord();
            if (Minecraft.isAmbientOcclusionEnabled() && Core.renderAO) {
                for (int face = 0; face < 6; face++) {
//					if (face != 2) {
//						continue;
//					}
                    VectorUV[] vecs = rc.faceVerts(face);
                    faceColor = getNormalizedLighting(vecs, center);
                    for (int i = 0; i < vecs.length; i++) {
                        isDebugVertex = i == 1;
                        vectorAO(rc, vecs[i], ForgeDirection.values()[face]);
                    }
                }
            } else {
                for (int face = 0; face < 6; face++) {
                    VectorUV[] vecs = rc.faceVerts(face);
                    float color = getNormalizedLighting(vecs, center);
                    Tessellator.instance.setColorOpaque_F(color, color, color);
                    int cx = here.x;
                    int cy = here.y;
                    int cz = here.z;
                    cx += Math.signum((vecs[0].x + vecs[2].x)/2);
                    cy += Math.signum((vecs[0].y + vecs[2].y)/2);
                    cz += Math.signum((vecs[0].z + vecs[2].z)/2);
                    //Not factory_rendering_block, the block at cx cy cz.
                    int brightness = 0;
                    Block block = Block.stone; //Block.blocksList[here.w.getBlockId(cx, cy, cz)];
                    if (block != null) {
                        brightness = block.getMixedBrightnessForBlock(here.w, cx, cy, cz);
                    }
                    Tessellator.instance.setBrightness(brightness);
                    for (int i = 0; i < vecs.length; i++) {
                        vertex(rc, vecs[i]);
                    }
                }
            }
        } else {
            for (int face = 0; face < 6; face++) {
                VectorUV[] vecs = rc.faceVerts(face);
                float color = getNormalizedLighting(vecs, center);
                Tessellator.instance.setColorOpaque_F(color, color, color);
                
                for (int i = 0; i < vecs.length; i++) {
                    vertex(rc, vecs[i]);
                }
            }
        }
        if (!world_mode) {
            Tessellator.instance.draw();
            ForgeHooksClient.unbindTexture();
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
    
    protected void renderCube(WireRenderingCube rc) {
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
            float color = getNormalizedLighting(vecs, rc.origin);
            
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
    
    protected void vertex(RenderingCube rc, VectorUV vec) {
        //all units are in texels; center of the cube is the origin. Or, like... not the center but the texel that's (8,8,8) away from the corner is.
        //u & v are in texels
        int u = (int) vec.u;
        int v = (int) vec.v;
        //this.x = this.y = this.z = 0;
        if (use_vertex_offset) {
            Tessellator.instance.addVertexWithUV(
                    this.x + 0.5 + vec.x / 16F,
                    this.y + 0.5 + vec.y / 16F,
                    this.z + 0.5 + vec.z / 16F,
                    rc.ul + u / 256F, rc.vl + v / 256F);
        } else {
            Tessellator.instance.addVertexWithUV(
                    0.5 + vec.x / 16F,
                    0.5 + vec.y / 16F,
                    0.5 + vec.z / 16F,
                    rc.ul + u / 256F, rc.vl + v / 256F);
        }
    }
    
    protected void vertex(WireRenderingCube rc, float x, float y, float z, float u, float v) {
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
