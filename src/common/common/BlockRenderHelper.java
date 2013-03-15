package factorization.common;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.api.VectorUV;

public class BlockRenderHelper extends Block {
    //This class is used to make it easy (and very thread-safe) to render cubes of various sizes. It's a fake block.
    public static BlockRenderHelper instance;

    public BlockRenderHelper() {
        super(Core.factory_block_id, Material.grass);
        blocksList[blockID] = null;
        //These three shouldn't strictly be necessary
        opaqueCubeLookup[blockID] = false;
        lightOpacity[blockID] = 0;
        canBlockGrass[blockID] = false;
        if (instance == null) {
            instance = this;
        }
    }
    
    public BlockRenderHelper setBlockBoundsOffset(float x, float y, float z) {
        setBlockBounds(x, y, z, 1 - x, 1 -y , 1 - z);
        return this;
    }
    
    public BlockRenderHelper setBlockBoundsBasedOnRotation() {
        double minX, minY, minZ;
        double maxX, maxY, maxZ;
        
        minX = maxX = cache[0].x;
        minY = maxY = cache[0].y;
        minZ = maxZ = cache[0].z;
        
        for (int i = 1; i < cache.length; i++) {
            VectorUV vec = cache[i];
            minX = Math.min(minX, cache[i].x);
            minY = Math.min(minY, cache[i].y);
            minZ = Math.min(minZ, cache[i].z);
            maxX = Math.min(maxX, cache[i].x);
            maxY = Math.min(maxY, cache[i].y);
            maxZ = Math.min(maxZ, cache[i].z);
        }
        setBlockBounds((float)minX, (float)minY, (float)minZ, (float)maxX, (float)maxY, (float)maxZ);
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public Icon[] textures;
    
    @SideOnly(Side.CLIENT)
    private Icon[] repetitionCache = new Icon[6];
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper useTexture(Icon texture) {
        textures = repetitionCache;
        for (int i = 0; i < textures.length; i++) {
            textures[i] = texture;
        }
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper useTextures(Icon ...textures) {
        this.textures = textures;
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockAccess w, int x, int y, int z, int side) {
        return textures[side] != null;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public Icon getBlockTextureFromSideAndMetadata(int side, int md) {
        Icon ret;
        try {
            ret = textures[side];
        } catch (NullPointerException e) {
            textures = new Icon[6];
            return textures[side] = BlockIcons.error;
        }
        if (ret == null) {
            return BlockIcons.error;
        }
        return textures[side];
    }
    
    
    @SideOnly(Side.CLIENT)
    public void renderForTileEntity() {
        renderRotated(Tessellator.instance, 0, 0, 0);
    }
    
    @SideOnly(Side.CLIENT)
    public void renderForInventory(RenderBlocks renderblocks) {
        // This originally copied from RenderBlocks.renderBlockAsItem
        Tessellator tessellator = Tessellator.instance;
        Icon texture;
        int i = 0;
        
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        renderblocks.setRenderBoundsFromBlock(this);
        tessellator.startDrawingQuads();
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(0.0F, -1F, 0.0F);
            renderblocks.renderBottomFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            renderblocks.renderTopFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(0.0F, 0.0F, -1F);
            renderblocks.renderEastFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            renderblocks.renderWestFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(-1F, 0.0F, 0.0F);
            renderblocks.renderNorthFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[i++]) != null) {
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            renderblocks.renderSouthFace(this, 0.0D, 0.0D, 0.0D, texture);
        }
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        /*RenderHelper.enableGUIStandardItemLighting();
        rb.renderBlockAsItem(this, 0, 0);*/
        /*Tessellator.instance.startDrawingQuads();
        begin();
        renderRotated(Tessellator.instance, 0, 0, 0);
        Tessellator.instance.draw();*/
    }
    
    @SideOnly(Side.CLIENT)
    public void render(RenderBlocks rb, int x, int y, int z) {
        rb.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        rb.renderStandardBlock(this, x, y, z);
    }
    
    @SideOnly(Side.CLIENT)
    public void render(RenderBlocks rb, Coord c) {
        rb.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        rb.renderStandardBlock(this, c.x, c.y, c.z);
    }
    
    //We could add stuff for simple 90 degree rotations
    
    @SideOnly(Side.CLIENT)
    private static final int X = 0, Y = 1, Z = 2, U = 3, V = 4;
    @SideOnly(Side.CLIENT)
    private static float[][][] verts = new float[8][4][5]; //sides, face vertices, vertex
    
    @SideOnly(Side.CLIENT)
    private static ForgeDirection[] dirs = ForgeDirection.values();
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper begin() {
        for (int i = 0; i < 6; i++) {
            Icon faceIcon = textures[i];
            if (faceIcon == null) {
                continue;
            }
            cache = fullCache[i];
            faceVerts(i);
            for (int f = 0; f < cache.length; f++) {
                VectorUV vert = cache[f];
                //System.out.println(vert.u + ", " + vert.v);
                vert.u = faceIcon.interpolateU(vert.u*16);
                vert.v = faceIcon.interpolateV(vert.v*16);
            }
        }
        return this;
    }
    
    public BlockRenderHelper beginNoIcons() {
        for (int i = 0; i < 6; i++) {
            cache = fullCache[i]; //fullCache isn't static; we'll have two instances for server thread & client thread
            faceVerts(i);
        }
        return this;
    }

    
    boolean hasTexture(int f) {
        //Efficiency
        return Core.proxy.BlockRenderHelper_has_texture(this, f);
    }
    
    public BlockRenderHelper rotate(Quaternion q) {
        //Apply the Quaternion to the vertices
        for (int f = 0; f < fullCache.length; f++) {
            if (!hasTexture(f)) {
                continue;
            }
            VectorUV[] face = fullCache[f];
            for (int v = 0; v < face.length; v++) {
                q.applyRotation(face[v]);
            }
        }
        return this;
    }
    
    public BlockRenderHelper translate(float dx, float dy, float dz) {
        //Move the vertices
        for (int f = 0; f < fullCache.length; f++) {
            if (!hasTexture(f)) {
                continue;
            }
            VectorUV[] face = fullCache[f];
            for (int v = 0; v < face.length; v++) {
                face[v].x += dx;
                face[v].y += dy;
                face[v].z += dz;
            }
        }
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public void renderRotated(Tessellator tess, int x, int y, int z) {
        for (int f = 0; f < fullCache.length; f++) {
            if (textures[f] == null) {
                continue;
            }
            VectorUV[] face = fullCache[f];
            for (int i = 0; i < face.length; i++) {
                VectorUV vert = face[i];
                tess.addVertexWithUV(vert.x + x, vert.y + y, vert.z + z, vert.u, vert.v);
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    public void renderRotated(Tessellator tess, Coord c) {
        if (c == null) {
            renderRotated(tess, 0, 0, 0);
        }
        for (int f = 0; f < fullCache.length; f++) {
            if (textures[f] == null) {
                continue;
            }
            VectorUV[] face = fullCache[f];
            for (int v = 0; v < face.length; v++) {
                VectorUV vert = face[v];
                tess.addVertexWithUV(vert.x + c.x, vert.y + c.y, vert.z + c.z, vert.u, vert.v);
            }
        }
    }
    
    
    VectorUV[] cache;
    VectorUV[][] fullCache = new VectorUV[6][4];
    {
        for (int i = 0; i < fullCache.length; i++) {
            cache = fullCache[i];
            for (int j = 0; j < cache.length; j++) {
                cache[j] = new VectorUV();
            }
        }
    }
    private void faceVerts(int face) {
        int c = 8;
        //Sets up vertex positions
        switch (face) {
        case 0: //-y
            set(0, 1, 0, 1);
            set(1, 0, 0, 1);
            set(2, 0, 0, 0);
            set(3, 1, 0, 0);
            break;
        case 1: //+y
            set(0, 1, 1, 0);
            set(1, 0, 1, 0);
            set(2, 0, 1, 1);
            set(3, 1, 1, 1);
            break;
        case 2: //-z
            set(0, 1, 1, 0);
            set(1, 1, 0, 0);
            set(2, 0, 0, 0);
            set(3, 0, 1, 0);
            break;
        case 3: //+z
            set(0, 1, 1, 1);
            set(1, 0, 1, 1);
            set(2, 0, 0, 1);
            set(3, 1, 0, 1);
            break;
        case 4: //-x
            set(0, 0, 0, 1);
            set(1, 0, 1, 1);
            set(2, 0, 1, 0);
            set(3, 0, 0, 0);
            break;
        case 5: //+x
            set(0, 1, 1, 1);
            set(1, 1, 0, 1);
            set(2, 1, 0, 0);
            set(3, 1, 1, 0);
            break;
        }
        //Sets up UV data
        switch (face) {
        case 0: //-y
        case 1: //+y
            //Mirror these like MC does.
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = vert.x;
                vert.v = vert.z;
            }
            break;
        case 2: //-z
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = 1 - vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 3: //+z
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 4: //-x
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        case 5: //+x
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = 1 - vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        default:
            throw new RuntimeException("Invalid face number");
        }
        //This is for clipping UVs that go over the edge I think?
        for (int i = 0; i < cache.length; i++) {
            VectorUV vec = cache[i];
            vec.u = Math.max(0, Math.min(1, vec.u));
            vec.v = Math.max(0, Math.min(1, vec.v));
        }
    }
    
    private void set(int i, int X, int Y, int Z) {
        cache[i].x = X == 0 ? minX : maxX;
        cache[i].y = Y == 0 ? minY : maxY;
        cache[i].z = Z == 0 ? minZ : maxZ;
    }
}
