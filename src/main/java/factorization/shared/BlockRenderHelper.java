package factorization.shared;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.api.VectorUV;
import factorization.common.BlockIcons;
import factorization.common.FzConfig;

public class BlockRenderHelper extends Block {
    //This class is used to make it easy (and very thread-safe) to render cubes of various sizes. It's a fake block.
    public static BlockRenderHelper instance;

    public BlockRenderHelper() {
        super(Material.grass);
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
        
        currentFace = faceCache[0];
        
        minX = maxX = currentFace[0].x;
        minY = maxY = currentFace[0].y;
        minZ = maxZ = currentFace[0].z;
        
        for (int face = 0; face < faceCache.length; face++) {
            currentFace = faceCache[face];
            for (int i = 1; i < currentFace.length; i++) {
                VectorUV vec = currentFace[i];
                minX = Math.min(minX, vec.x);
                minY = Math.min(minY, vec.y);
                minZ = Math.min(minZ, vec.z);
                maxX = Math.max(maxX, vec.x);
                maxY = Math.max(maxY, vec.y);
                maxZ = Math.max(maxZ, vec.z);
            }
        }
        setBlockBounds((float)minX, (float)minY, (float)minZ, (float)maxX, (float)maxY, (float)maxZ);
        return this;
    }
    
    public IIcon[] textures;
    private IIcon[] repetitionCache = new IIcon[6];
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper useTexture(IIcon texture) {
        textures = repetitionCache;
        for (int i = 0; i < textures.length; i++) {
            textures[i] = texture;
        }
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper useTextures(IIcon ...textures) {
        this.textures = textures;
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper setTexture(int i, IIcon texture) {
        textures = repetitionCache;
        textures[i] = texture;
        return this;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockAccess w, int x, int y, int z, int side) {
        return textures[side] != null;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int md) {
        IIcon ret;
        try {
            ret = textures[side];
        } catch (NullPointerException e) {
            textures = new IIcon[6];
            return textures[side] = BlockIcons.error;
        }
        if (ret == null) {
            return BlockIcons.error;
        }
        return textures[side];
    }
    
    public VectorUV[][] getFaceVertices() {
        return faceCache;
    }
    
    @SideOnly(Side.CLIENT)
    public void renderForTileEntity() {
        renderRotated(Tessellator.instance, 0, 0, 0);
    }
    
    @SideOnly(Side.CLIENT)
    public void renderForInventory(RenderBlocks renderblocks) {
        // This originally copied from RenderBlocks.renderBlockAsItem (near the bottom)
        IIcon texture;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        renderblocks.setRenderBoundsFromBlock(this);
        if ((texture = textures[0]) != null) {
            tessellator.setNormal(0.0F, -1F, 0.0F);
            renderblocks.renderFaceYNeg(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[1]) != null) {
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            renderblocks.renderFaceYPos(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[2]) != null) {
            tessellator.setNormal(0.0F, 0.0F, -1F);
            renderblocks.renderFaceZNeg(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[3]) != null) {
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            renderblocks.renderFaceZPos(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[4]) != null) {
            tessellator.setNormal(-1F, 0.0F, 0.0F);
            renderblocks.renderFaceXNeg(this, 0.0D, 0.0D, 0.0D, texture);
        }
        if ((texture = textures[5]) != null) {
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            renderblocks.renderFaceXPos(this, 0.0D, 0.0D, 0.0D, texture);
        }
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
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
    
    private static final byte UV_NONE = 0, UV_OLD_STYLE_ROTATED = 1, UV_NEW_STYLE_MIRRORED = 2, UV_FULLY_ROTATED_STYLE = 3;
    
    private VectorUV center = new VectorUV();
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper beginWithRotatedUVs() {
        return begin(UV_OLD_STYLE_ROTATED);
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper beginWithMirroredUVs() {
        return begin(UV_NEW_STYLE_MIRRORED);
    }
    
    @SideOnly(Side.CLIENT)
    public BlockRenderHelper beginWithHipsterUVs() {
        return begin(UV_FULLY_ROTATED_STYLE);
    }
    
    @SideOnly(Side.CLIENT)
    private BlockRenderHelper begin(byte uv_mode) {
        for (int i = 0; i < 6; i++) {
            IIcon faceIIcon = textures[i];
            if (faceIIcon == null) {
                continue;
            }
            currentFace = faceCache[i];
            faceVerts(i, uv_mode);
            for (int f = 0; f < currentFace.length; f++) {
                VectorUV vert = currentFace[f];
                vert.u = faceIIcon.getInterpolatedU(vert.u*16);
                vert.v = faceIIcon.getInterpolatedV(vert.v*16);
            }
        }
        center.x = (minX + maxX)/2;
        center.y = (minY + maxY)/2;
        center.z = (minZ + maxZ)/2;
        return this;
    }
    
    public BlockRenderHelper beginNoIIcons() {
        for (int i = 0; i < 6; i++) {
            //We'll have multiple instances of BlockRenderHelper for server & client, so that they don't interfere with eachother.
            currentFace = faceCache[i];
            faceVerts(i, UV_NONE);
        }
        return this;
    }

    
    boolean hasTexture(int f) {
        //Efficiency
        return Core.proxy.BlockRenderHelper_has_texture(this, f);
    }
    
    /**
     * Rotate around the block's 0,0,0 or something. You don't want this one.
     */
    public BlockRenderHelper rotate(Quaternion q) {
        //Apply the Quaternion to the vertices
        for (int f = 0; f < faceCache.length; f++) {
            if (!hasTexture(f)) {
                continue;
            }
            VectorUV[] face = faceCache[f];
            for (int v = 0; v < face.length; v++) {
                q.applyRotation(face[v]);
            }
        }
        q.applyRotation(center);
        return this;
    }
    
    /**
     * Rotate around the block's bounding box center of mass. You probably don't want this one.
     */
    public BlockRenderHelper rotateMiddle(Quaternion q) {
        Vec3 d = getBoundsMiddle();
        translate((float)(-d.xCoord), (float)(-d.yCoord), (float)(-d.zCoord));
        rotate(q);
        translate((float)(d.xCoord), (float)(d.yCoord), (float)(d.zCoord));
        return this;
    }
    
    /**
     * Rotate around the center of the block. Use this one!
     */
    public BlockRenderHelper rotateCenter(Quaternion q) {
        //TODO: FzOrientation variant; could let us do mad render optimizations
        float d = 0.5F;
        translate(-d, -d, -d);
        rotate(q);
        translate(d, d, d);
        return this;
    }
    
    public BlockRenderHelper translate(float dx, float dy, float dz) {
        //Move the vertices
        for (int f = 0; f < faceCache.length; f++) {
            if (!hasTexture(f)) {
                continue;
            }
            VectorUV[] face = faceCache[f];
            for (int v = 0; v < face.length; v++) {
                face[v].x += dx;
                face[v].y += dy;
                face[v].z += dz;
            }
        }
        center.x += dx;
        center.y += dy;
        center.z += dz;
        return this;
    }
    
    private int[] colors = new int[6];
    {
        resetColors();
    }
    
    public void setColor(int index, int color) {
        colors[index] = color;
    }
    
    public void setColor(int color) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = color;
        }
    }
    
    public void resetColors() {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = 0xFFFFFF;
        }
    }
    
    Quaternion A = new Quaternion(), B = new Quaternion(), C = new Quaternion();
    
    Quaternion getNormal(VectorUV a, VectorUV b, VectorUV c) {
        //Dir = (B - A) x (C - A)
        //Norm = Dir / len(Dir)
        A.loadFrom(a);
        B.loadFrom(b);
        C.loadFrom(c);
        
        A.incrScale(-1);
        B.incrAdd(A);
        C.incrAdd(A);
        
        B.incrCross(C);
        B.incrNormalize();
        return B;
    }
    
    public float alpha = 1F;
    
    @SideOnly(Side.CLIENT)
    public void renderRotated(Tessellator tess, int x, int y, int z) {
        for (int f = 0; f < faceCache.length; f++) {
            if (textures[f] == null) {
                continue;
            }
            VectorUV[] face = faceCache[f];
            float lighting = getNormalizedLighting(face, center);
            int color = colors[f];
            float color_r = (color & 0xFF0000) >> 16;
            float color_g = (color & 0x00FF00) >> 8;
            float color_b = (color & 0x0000FF);
            lighting /= 255F; /* because the colors go from 0x00 to 0xFF*/
            tess.setColorRGBA_F(lighting*color_r, lighting*color_g, lighting*color_b, alpha);
            
            
            for (int i = 0; i < face.length; i++) {
                VectorUV vert = face[i];
                tess.addVertexWithUV(vert.x + x, vert.y + y, vert.z + z, vert.u, vert.v);
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    public void renderRotatedUnshaded(Tessellator tess, int x, int y, int z) {
        for (int f = 0; f < faceCache.length; f++) {
            if (textures[f] == null) {
                continue;
            }
            VectorUV[] face = faceCache[f];
            float lighting = 1; // getNormalizedLighting(face, center);
            int color = colors[f];
            float color_r = (color & 0xFF0000) >> 16;
            float color_g = (color & 0x00FF00) >> 8;
            float color_b = (color & 0x0000FF);
            lighting /= 255F; /* because the colors go from 0x00 to 0xFF*/
            tess.setColorOpaque_F(lighting*color_r, lighting*color_g, lighting*color_b);
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
            return;
        }
        renderRotated(tess, c.x, c.y, c.z);
    }
    
    static Vec3 midCache = Vec3.createVectorHelper(0, 0, 0);
    public Vec3 getBoundsMiddle() {
        midCache.xCoord = (minX + maxX)/2;
        midCache.yCoord = (minY + maxY)/2;
        midCache.zCoord = (minZ + maxZ)/2;
        return midCache;
    }
    
    
    VectorUV[] currentFace;
    public VectorUV[][] faceCache = new VectorUV[6][4];
    {
        for (int i = 0; i < faceCache.length; i++) {
            currentFace = faceCache[i];
            for (int j = 0; j < currentFace.length; j++) {
                currentFace[j] = new VectorUV();
            }
        }
    }
    
    private void setVertexPositions(int face) {
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
    }
    
    private void setOldStyleRotatedishUVs(int face) {
        switch (face) {
        case 0: //-y
        case 1: //+y
            //Mirror these like MC does.
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = vert.z;
            }
            break;
        case 2: //-z
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = 1 - vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 3: //+z
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 4: //-x
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        case 5: //+x
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = 1 - vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        default:
            throw new RuntimeException("Invalid face number");
        }
    }
    
    private void setVanillaStyleMirroredUVs(int face) {
        switch (face) {
        case 0: //-y
        case 1: //+y
            //Mirror these like MC does.
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = vert.z;
            }
            break;
            // In 1.7, MC side faces mirror each-other as well.
        case 2: //-z
        case 3: //+z
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 4: //-x
        case 5: //+x
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        default:
            throw new RuntimeException("Invalid face number");
        }
    }
    
    private void setStyleFullyRotatedUVs(int face) {
        switch (face) {
        case 0: //-y
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = 1 - vert.z;
            }
            break;
        case 1: //+y
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = 1 - vert.x;
                vert.v = 1 - vert.z;
            }
            break;
        case 2: //-z
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = 1 - vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 3: //+z
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.x;
                vert.v = 1 - vert.y;
            }
            break;
        case 4: //-x
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        case 5: //+x
            for (int i = 0; i < currentFace.length; i++) {
                VectorUV vert = currentFace[i];
                vert.u = 1 - vert.z;
                vert.v = 1 - vert.y;
            }
            break;
        default:
            throw new RuntimeException("Invalid face number");
        }
    }
    
    private void convertUVsForIcon(int face) {
        VectorUV[] cache = currentFace;
        int WIDTH = 1;
        for (int i = 0; i < cache.length; i++) {
            VectorUV main = cache[i];
            double udelta = 0, vdelta = 0;
            int nada = 0;
            if (main.u > WIDTH) {
                udelta = main.u - WIDTH;
            } else if (main.u < 0) {
                udelta = main.u;
            } else {
                nada++;
            }
            if (main.v > WIDTH) {
                vdelta = main.v - WIDTH;
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
    }
    
    private void clipUVs() {
        for (int i = 0; i < currentFace.length; i++) {
            VectorUV vec = currentFace[i];
            vec.u = clip(vec.u);
            vec.v = clip(vec.v);
        }
    }
    
    private void faceVerts(int face, byte uv_mode) {
        setVertexPositions(face);
        if (uv_mode == UV_NONE) return;
        if (uv_mode == UV_NEW_STYLE_MIRRORED) {
            setVanillaStyleMirroredUVs(face);
        } else if (uv_mode == UV_OLD_STYLE_ROTATED) {
            setOldStyleRotatedishUVs(face);
        } else if (uv_mode == UV_FULLY_ROTATED_STYLE) {
            setStyleFullyRotatedUVs(face);
        }
        convertUVsForIcon(face);
        clipUVs();
    }
    
    static double clip(double v) {
        return Math.max(0, Math.min(1, v));
    }
    
    private void set(int i, int X, int Y, int Z) {
        currentFace[i].x = X == 0 ? minX : maxX;
        currentFace[i].y = Y == 0 ? minY : maxY;
        currentFace[i].z = Z == 0 ? minZ : maxZ;
    }
    
    static private ForgeDirection getDirectionFromVector(VectorUV here) {
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
    
    private static final float[] directionLighting = new float[] {0.5F, 1F, 0.8F, 0.8F, 0.6F, 0.6F};
    VectorUV normal_uv = new VectorUV();
    private float getNormalizedLighting(VectorUV[] vecs, VectorUV center) {
        Quaternion normal = getNormal(vecs[0], vecs[1], vecs[2]);
        normal_uv.x = normal.x;
        normal_uv.y = normal.y;
        normal_uv.z = normal.z;
        ForgeDirection dir = getDirectionFromVector(normal_uv);
        return directionLighting[dir.ordinal()];
    }
    
    public void setupBrightness(Tessellator tess, IBlockAccess w, int x, int y, int z) {
        if (w == null) return; // Is that cool?
        tess.instance.setBrightness(getMixedBrightnessForBlock(w, x, y, z));
    }
    
    public void setupBrightness(Tessellator tess, Coord c) {
        if (c.w == null) return; // Is that cool?
        tess.instance.setBrightness(getMixedBrightnessForBlock(c.w, c.x, c.y, c.z));
    }

}
