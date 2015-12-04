package factorization.crafting;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockIcons.ExtendedIIcon;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;

public class TileEntityCompressionCrafterRenderer extends TileEntitySpecialRenderer {
    float textureOffset;
    ExtendedIIcon interp_side = new ExtendedIIcon(BlockIcons.compactSideSlide) {
        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedU(double d0) {
            return under.getInterpolatedU(d0);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedV(double d0) {
            return under.getInterpolatedV(d0 + 12*textureOffset);
        }
        
    };
    
    static double myRound(double x) {
        return x > 0.5 ? 1 : 0;
    }
    
    Random rand = new Random();
    
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityCompressionCrafter cc = (TileEntityCompressionCrafter) te;
        if (cc == null) {
            return;
        }
        interp_side.under = BlockIcons.compactSideSlide;
        bindTexture(Core.blockAtlas);
        final float squishy = 3F/16F;
        final float extraAxialSquish = 10F/16F;
        float perc = cc.getProgressPerc();
        float p = perc*squishy;
        
        BlockRenderHelper block = Core.registry.blockRender;
        textureOffset = p;
        
        block.useTextures(
                null, null,
                interp_side, interp_side,
                interp_side, interp_side
                );
        float d = -1F/256F;
        d = 0;
        block.setBlockBounds(0 - d, 0.5F - 1F/256F, 0 - d, 1 + d, 1F, 1 + d);
        EnumFacing facing = cc.getFacing();
        FzOrientation fo = FzOrientation.fromDirection(facing);
        Quaternion q = Quaternion.fromOrientation(fo);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        
        block.beginWithMirroredUVs();
        
        block.rotateCenter(q);
        
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(cc.getWorld(), cc.xCoord, cc.yCoord, cc.zCoord));
        GL11.glDisable(GL11.GL_LIGHTING);
        block.renderForTileEntity();
        Tessellator.instance.draw();
        
        if (cc.isPrimaryCrafter() && cc.upperCorner != null && cc.lowerCorner != null
                && Minecraft.getMinecraft().gameSettings.fancyGraphics) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-cc.xCoord, -cc.yCoord, -cc.zCoord);
            if (perc > 0.75F) {
                float jiggle = perc - 0.75F;
                jiggle /= 32; //this gets us 1 pixel of jiggle room
                rand.setSeed((long)(((long) Integer.MAX_VALUE)*perc));
                GL11.glTranslatef((float) rand.nextGaussian()*jiggle, (float) rand.nextGaussian()*jiggle, (float) rand.nextGaussian()*jiggle);
            }
            Coord up = cc.upperCorner;
            Coord lo = cc.lowerCorner;
            float cx = (up.x + lo.x + 1)/2F;
            float cy = (up.y + lo.y + 1)/2F;
            float cz = (up.z + lo.z + 1)/2F;
            float sx, sy, sz;
            EnumFacing fd = cc.craftingAxis;
            sx = sy = sz = 1 - p;
            if (fd.getDirectionVec().getX() != 0) sx = 1 + perc*extraAxialSquish;
            if (fd.getDirectionVec().getY() != 0) sy = 1 + perc*extraAxialSquish;
            if (fd.getDirectionVec().getZ() != 0) sz = 1 + perc*extraAxialSquish;
            
            //Unfortunately, the transformed origin is equal to the world's origin.
            //So it scales towards the origin instead of the center of the compression area.
            //We need to translate some amount to make up for it.
            //Actual position: cx*sx; desired is cx. So translate cx - cx*sx
            GL11.glTranslatef(cx - cx*sx, cy - cy*sy, cz - cz*sz);
            
            GL11.glScalef(sx, sy, sz);
            drawSquishingBlocks(up, lo, partial);
            GL11.glPopMatrix();
            
            sx = sy = sz = 1;
            float s = 17F/16F;
            if (fd.getDirectionVec().getX() != 0) sx = s;
            if (fd.getDirectionVec().getY() != 0) sy = s;
            if (fd.getDirectionVec().getZ() != 0) sz = s;
            GL11.glScalef(sx, sy, sz);
            GL11.glTranslatef(lo.x - cc.xCoord, lo.y - cc.yCoord, lo.z - cc.zCoord);
            sx -= 1;
            sy -= 1;
            sz -= 1;
            sx /= -2;
            sy /= -2;
            sz /= -2;
            GL11.glTranslatef(sx, sy, sz);
            //GL11.glTranslatef(-(up.x - lo.x), -(up.y - lo.y), -(up.z - lo.z + 1));
            drawObscurringBox();
        }
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }
    
    private void drawObscurringBox() {
        //contentSize is determined by _drawSquishingBlocks
        if (contentSize == null) {
            return;
        }
        bindTexture(Core.blockAtlas);
        BlockRenderHelper block = BlockRenderHelper.instance;
        contentSize.maxX -= contentSize.minX;
        contentSize.minX = 0;
        contentSize.maxY -= contentSize.minY;
        contentSize.minY = 0;
        contentSize.maxZ -= contentSize.minZ;
        contentSize.minZ = 0;
        block.useTexture(BlockIcons.dark_iron_block);
        block.setBlockBounds(0, 0, 0, (float) contentSize.maxX, (float) contentSize.maxY, (float) contentSize.maxZ);
        block.beginWithMirroredUVs();
        Tessellator.instance.startDrawingQuads();
        block.renderForTileEntity();
        Tessellator.instance.draw();
    }
    
    private static Tessellator tess = new Tessellator();
    
    private void drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
        Tessellator real = Tessellator.instance;
        if (real == tess) {
            return; //Oh boy!
        }
        double spx, spy, spz;
        spx = TileEntityRendererDispatcher.staticPlayerX;
        spy = TileEntityRendererDispatcher.staticPlayerY;
        spz = TileEntityRendererDispatcher.staticPlayerZ;
        TileEntityRendererDispatcher.staticPlayerX = TileEntityRendererDispatcher.staticPlayerY = TileEntityRendererDispatcher.staticPlayerZ = 0;
        try {
            Tessellator.instance = tess;
            _drawSquishingBlocks(upperCorner, lowerCorner, partial);
        } finally {
            Tessellator.instance = real;
            TileEntityRendererDispatcher.staticPlayerX = spx;
            TileEntityRendererDispatcher.staticPlayerY = spy;
            TileEntityRendererDispatcher.staticPlayerZ = spz;
        }
    }
    
    private static Tessellator tesrator = new Tessellator();
    AxisAlignedBB contentSize;
    private void _drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
        contentSize = null;
        bindTexture(Core.blockAtlas);
        Tessellator.instance.startDrawingQuads();
        World w = upperCorner.w;
        RenderBlocks rb = new RenderBlocks(w);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        for (int renderPass = 0; renderPass <= 3; renderPass++) {
            if (renderPass == 3) {
                Tessellator.instance.draw();
            }
            for (int x = lowerCorner.x; x <= upperCorner.x; x++) {
                for (int y = lowerCorner.y; y <= upperCorner.y; y++) {
                    for (int z = lowerCorner.z; z <= upperCorner.z; z++) {
                        Block b = w.getBlock(x, y, z);
                        if (b == null) {
                            if (renderPass == 3) {
                                if (contentSize == null) {
                                    contentSize = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
                                } else {
                                    contentSize.minX = Math.min(contentSize.minX, x);
                                    contentSize.maxX = Math.max(contentSize.maxX, x + 1);
                                    contentSize.minY = Math.min(contentSize.minY, y);
                                    contentSize.maxY = Math.max(contentSize.maxY, y + 1);
                                    contentSize.minZ = Math.min(contentSize.minZ, z);
                                    contentSize.maxZ = Math.max(contentSize.maxZ, z + 1);
                                }
                            }
                            continue;
                        }
                        if (renderPass == 3) {
                            if (contentSize == null) {
                                contentSize = b.getSelectedBoundingBoxFromPool(w, x, y, z);
                            } else {
                                AxisAlignedBB extend = b.getSelectedBoundingBoxFromPool(w, x, y, z);
                                if (extend != null) {
                                    contentSize.minX = Math.min(contentSize.minX, extend.minX);
                                    contentSize.maxX = Math.max(contentSize.maxX, extend.maxX);
                                    contentSize.minY = Math.min(contentSize.minY, extend.minY);
                                    contentSize.maxY = Math.max(contentSize.maxY, extend.maxY);
                                    contentSize.minZ = Math.min(contentSize.minZ, extend.minZ);
                                    contentSize.maxZ = Math.max(contentSize.maxZ, extend.maxZ);
                                }
                            }
                            TileEntity te;
                            if ((te = w.getTileEntity(x, y, z)) != null) {
                                Tessellator.instance = tesrator;
                                TileEntityRendererDispatcher.instance.renderTileEntity(te, partial);
                                Tessellator.instance = tess;
                            }
                            continue;
                        }
                        if (b.canRenderInPass(renderPass)) {
                            rb.renderBlockByRenderType(b, x, y, z);
                        }
                    }
                }
            }
        }
    }

}
