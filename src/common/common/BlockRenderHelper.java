package factorization.common;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
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

    public BlockRenderHelper(int blockId, Material par2Material) {
        super(blockId, par2Material);
        blocksList[blockId] = null;
        //These three shouldn't strictly be necessary
        opaqueCubeLookup[blockId] = false;
        lightOpacity[blockId] = 0;
        canBlockGrass[blockId] = false;
    }
    
    public BlockRenderHelper setBlockBoundsOffset(float x, float y, float z) {
        setBlockBounds(x, y, z, 1 - x, 1 -y , 1 - z);
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    private Icon[] textures;
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper useTextures(Icon[] textures) {
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
            return textures[side] = BlockFactorization.error_icon;
        }
        if (ret == null) {
            return BlockFactorization.error_icon;
        }
        return textures[side];
    }
    
    @SideOnly(Side.CLIENT)
    RenderBlocks rb = new RenderBlocks();
    
    @SideOnly(Side.CLIENT)
    public void render(int x, int y, int z) {
        rb.renderStandardBlock(this, x, y, z);
    }
    
    @SideOnly(Side.CLIENT)
    public void render(Tessellator tess, int x, int y, int z) {
        Tessellator orig = Tessellator.instance;
        Tessellator.instance = tess;
        rb.renderStandardBlock(this, x, y, z);
        Tessellator.instance = orig;
    }
    
    @SideOnly(Side.CLIENT)
    public void render(Coord c) {
        rb.renderStandardBlock(this, c.x, c.y, c.z);
    }
    
    @SideOnly(Side.CLIENT)
    public void render(Tessellator tess, Coord c) {
        Tessellator orig = Tessellator.instance;
        Tessellator.instance = tess;
        rb.renderStandardBlock(this, c.x, c.y, c.z);
        Tessellator.instance = orig;
    }
    
    //We could add stuff for simple 90 degree rotations
    
    @SideOnly(Side.CLIENT)
    private static final int X = 0, Y = 1, Z = 2, U = 3, V = 4;
    @SideOnly(Side.CLIENT)
    private static float[][][] verts = new float[8][4][5]; //sides, face vertices, vertex
    
    @SideOnly(Side.CLIENT)
    private static ForgeDirection[] dirs = ForgeDirection.values();
    
    //NOTE: This code assumes that every side has an Icon.
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper begin() {
        for (int i = 0; i < 6; i++) {
            cache = fullCache[i];
            faceVerts(i);
            Icon faceIcon = textures[i];
            for (int f = 0; f < cache.length; f++) {
                VectorUV vert = cache[f];
                vert.u = faceIcon.interpolateU(vert.u);
                vert.v = faceIcon.interpolateU(vert.v);
            }
        }
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper rotate(Quaternion q) {
        //Apply the Quaternion to the vertices
        for (int f = 0; f < fullCache.length; f++) {
            VectorUV[] face = fullCache[f];
            for (int v = 0; v < face.length; v++) {
                q.applyRotation(face[v]);
            }
        }
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper translate(float dx, float dy, float dz) {
        //Move the vertices
        for (int f = 0; f < fullCache.length; f++) {
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
            VectorUV[] face = fullCache[f];
            for (int v = 0; v < face.length; v++) {
                VectorUV vert = face[v];
                tess.addVertexWithUV(vert.x, vert.y, vert.z, vert.u, vert.v);
            }
        }
    }
    
    
    VectorUV[] cache = new VectorUV[4];
    VectorUV[][] fullCache = new VectorUV[8][4];
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
            set(0, 0, 1, 1);
            set(1, 0, 1, 0);
            set(2, 0, 0, 0);
            set(3, 0, 0, 1);
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
                vert.u = 1 - vert.y;
                vert.v = vert.z;
            }
            break;
        case 5: //+x
            for (int i = 0; i < cache.length; i++) {
                VectorUV vert = cache[i];
                vert.u = 1 - vert.y;
                vert.v = 1 - vert.z;
            }
            break;
        default:
            throw new RuntimeException("Invalid face number");
        }
        //This is for clipping UVs that go over the edge I think?
        /*
        for (int i = 0; i < cache.length; i++) {
            VectorUV main = cache[i];
            float udelta = 0, vdelta = 0;
            int nada = 0;
            if (main.u > 16) {
                udelta = main.u - 16;
            } else if (main.u < 0) {
                udelta = main.u;
            } else {
                nada++;
            }
            if (main.v > 16) {
                vdelta = main.v - 16;
            } else if (main.v < 0) {
                vdelta = main.v;
            } else {
                nada++;
            }
            if (nada == 2) {
                continue;
            }
            for (int J = 0; J < cache.length; J++) {
                VectorUV other = cache[J];
                other.u -= udelta;
                other.v -= vdelta;
            }
        }
        */
    }
    
    private void set(int i, int X, int Y, int Z) {
        cache[i].x = X == 0 ? minX : maxX;
        cache[i].y = Y == 0 ? minY : maxY;
        cache[i].z = Z == 0 ? minZ : maxZ;
    }
}
