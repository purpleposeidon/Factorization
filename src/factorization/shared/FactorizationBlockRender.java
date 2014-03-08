package factorization.shared;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.VectorUV;
import factorization.charge.WireRenderingCube;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;

abstract public class FactorizationBlockRender implements ICoord {

    public boolean world_mode, use_vertex_offset;
    public IBlockAccess w;
    public int x, y, z;
    public int metadata;
    public TileEntity te;
    public ItemStack is;
    public ItemRenderType renderType;
    
    private static FactorizationBlockRender renderMap[] = new FactorizationBlockRender[0xFF];
    private static FactorizationBlockRender defaultRender;
    
    public static void setDefaultRender(FactoryType ft) {
        assert defaultRender != null;
        renderMap[ft.md] = defaultRender;
    }
    
    public static FactorizationBlockRender getRenderer(int md) {
        FactorizationBlockRender ret = renderMap[md];
        if (ret == null) {
            renderMap[md] = defaultRender;
            Core.logFine("Using default renderer for ID " + md);
            return defaultRender;
        }
        return ret;
    }
    
    public FactorizationBlockRender() {
        initialize(getFactoryType());
    }
    
    public FactorizationBlockRender(FactoryType ft) {
        initialize(ft);
    }
    
    private void initialize(FactoryType ft) {
        if (ft != null) {
            int md = ft.md;
            if (renderMap[md] != null) {
                throw new RuntimeException("Tried to overwrite a renderer");
            }
            renderMap[md] = this;
        } else {
            if (defaultRender != null) {
                throw new RuntimeException("Tried to overwrite a renderer");
            }
            defaultRender = this;
        }
    }
    
    public abstract boolean render(RenderBlocks rb);
    public abstract FactoryType getFactoryType();
    public boolean renderSecondPass(RenderBlocks rb) { return false; }
    
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

    public final void renderInWorld(IBlockAccess w, int wx, int wy, int wz, int md, TileEntity te) {
        world_mode = true;
        this.w = w;
        x = wx;
        y = wy;
        z = wz;
        use_vertex_offset = true;
        this.metadata = md;
        this.te = te;
    }

    public final void renderInInventory() {
        world_mode = false;
        x = y = z = 0;
        use_vertex_offset = true;
        te = null;
        is = ItemRenderCapture.getRenderingItem();
        renderType = ItemRenderCapture.getRenderType();
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
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        //b.setBlockBounds(0, 0, 0, 1, 1, 1);
        if (world_mode) {
            rb.renderStandardBlock(b, x, y, z);
        }
        else {
            Core.registry.factory_rendering_block.fake_normal_render = true;
            rb.renderBlockAsItem(b, md, 1.0F);
            Core.registry.factory_rendering_block.fake_normal_render = false;
        }
    }
    
    protected void renderCauldron(RenderBlocks rb, IIcon lid, IIcon metal) {
        renderCauldron(rb, lid, metal, 1);
    }
    
    protected void renderCauldron(RenderBlocks rb, IIcon lid, IIcon metal, float height) {
        Tessellator tessellator = Tessellator.instance;
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.setBlockBounds(0, 0, 0, 1, height, 1);
        block.useTextures(metal, lid, metal, metal, metal, metal);
        if (world_mode) {
            block.render(rb, getCoord());
            float d = 2F/16F;
            boolean origAO = rb.enableAO;
            rb.setRenderBounds(d, d, d, 1 - d, height, 1 - d);
            block.setBlockBounds(d, d, d, 1 - d, 1 - d, 1 - d);
            rb.enableAO = false;
            rb.renderFaceXNeg(block, x + 1 - 2*d, y, z, metal);
            rb.renderFaceZNeg(block, x, y, z + 1 - 2*d, metal);
            rb.renderFaceXPos(block, x - 1 + 2*d, y, z, metal);
            rb.renderFaceZPos(block, x, y, z - 1 + 2*d, metal);
            rb.renderFaceYPos(block, x, y - height + 1*d, z, metal);
            rb.enableAO = origAO;
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
            //GL11.glDisable(GL11.GL_LIGHTING);
            block.renderForInventory(rb);
            //GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
    
    protected void renderPart(RenderBlocks rb, IIcon texture, float b1, float b2, float b3, float b4, float b5, float b6) {
        BlockFactorization block = Core.registry.factory_rendering_block;
        rb.setRenderBounds(b1, b2, b3, b4, b5, b6);
        block.setBlockBounds(b1, b2, b3, b4, b5, b6);
        if (world_mode) {
            BlockFactorization.force_texture = texture;
            rb.renderStandardBlock(block, x, y, z);
            BlockFactorization.force_texture = null;
        }
        else {
            renderPartInvTexture(rb, block, texture);
        }
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    private void renderPartInvTexture(RenderBlocks renderblocks,
            Block block, IIcon texture) {
        // This originally copied from RenderBlocks.renderBlockAsItem
        Tessellator tessellator = Tessellator.instance;

        block.setBlockBoundsForItemRender();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1F, 0.0F);
        renderblocks.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderblocks.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.setNormal(0.0F, 0.0F, -1F);
        renderblocks.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderblocks.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.setNormal(-1F, 0.0F, 0.0F);
        renderblocks.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderblocks.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }
    
    
    static private RenderBlocks rb = new RenderBlocks();

    
    static private ForgeDirection getFaceDirection(VectorUV[] vecs, VectorUV center) {
        VectorUV here = vecs[0].add(vecs[2]);
        here.scale(0.5F);
        here = here.add(center);
        double x = Math.abs(here.x), y = Math.abs(here.y), z = Math.abs(here.z);
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
    }
    
    private float interpolate(float a, float b, float scale) {
        return a*scale + b*(1 - scale);
    }

    
    protected void renderCube(WireRenderingCube rc) {
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
            //ForgeHooksClient.bindTexture(cubeTexture, 0);
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        
        float delta = 1F/256F;
        double zfight = rc.corner.x * delta;
        zfight *= rc.corner.y * (delta);
        zfight *= rc.corner.z * (delta);
        zfight = 1.0025F;
        for (int face = 0; face < 6; face++) {
            VectorUV[] vecs = rc.faceVerts(face);
            float color = directionLighting[face]; //getNormalizedLighting(vecs, rc.origin);
            
            Tessellator.instance.setColorOpaque_F(color, color, color);
            for (int i = 0; i < vecs.length; i++) {
                VectorUV vec = vecs[i];
                vertex(rc, (float)(vec.x*zfight), (float)(vec.y*zfight), (float)(vec.z*zfight), (float)(vec.u), (float)(vec.v));
            }
        }
        if (!world_mode) {
            Tessellator.instance.draw();
            //ForgeHooksClient.unbindTexture();
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }

    protected void vertex(WireRenderingCube rc, float x, float y, float z, float u, float v) {
        //all units are in texels; center of the cube is the origin. Or, like... not the center but the texel that's (8,8,8) away from the corner is.
        //u & v are in texels
        IIcon wire = BlockIcons.wire;
        Tessellator.instance.addVertexWithUV(
                this.x + 0.5 + x / 16F,
                this.y + 0.5 + y / 16F,
                this.z + 0.5 + z / 16F,
                wire.getInterpolatedU(u), wire.getInterpolatedV(v));
    }
    
    public static void renderItemIIcon(IIcon icon) {
        //Extracted from ItemRenderer.renderItem
        if (icon == null) {
            return;
        }

        
        final TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(tex.getResourceLocation(1 /* 1 = item icons */));
        //tex.bindTexture(tex.getResourceLocation(1 /* 1 = item icons */));

        Tessellator tessellator = Tessellator.instance;
        float f = icon.getMinU();
        float f1 = icon.getMaxU();
        float f2 = icon.getMinV();
        float f3 = icon.getMaxV();
        float f4 = 0.0F;
        float f5 = 0.3F;
        //GL11.glTranslatef(-f4, -f5, 0.0F);
        float f6 = 1.5F;
        //GL11.glScalef(f6, f6, f6);
        //GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
        //GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
        //GL11.glTranslatef(-0.9375F, -0.0625F, 0.0F);
        ItemRenderer.renderItemIn2D(tessellator, f1, f2, f, f3, icon.getIconWidth(), icon.getIconHeight(), 0.0625F);
    }

    public void renderMotor(RenderBlocks rb, float yoffset) {
        IIcon metal = BlockIcons.motor_texture;
        //metal = 11;
        float d = 4.0F / 16.0F;
        float yd = -d + 0.003F;
        renderPart(rb, metal, d, d + yd + yoffset, d, 1 - d, 1 - (d + 0F/16F) + yd + yoffset, 1 - d);
    }
    
    protected void renderRotatedHelper(BlockRenderHelper block) {
        Tessellator tess = Tessellator.instance;
        if (world_mode) {
            block.renderRotated(tess, x, y, z);
        } else {
            tess.startDrawingQuads();
            block.renderRotated(tess, 0, 0, 0);
            tess.draw();
        }
    }
    
    protected void renderBlock(RenderBlocks rb, BlockRenderHelper block) {
        if (world_mode) {
            block.render(rb, x, y, z);
        } else {
            block.renderForInventory(rb);
        }
    }
}
