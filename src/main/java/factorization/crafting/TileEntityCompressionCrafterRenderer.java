package factorization.crafting;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.util.NORELEASE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Random;

public class TileEntityCompressionCrafterRenderer extends TileEntitySpecialRenderer<TileEntityCompressionCrafter> {
    Random rand = new Random();
    
    @Override
    public void renderTileEntityAt(TileEntityCompressionCrafter cc, double x, double y, double z, float partial, int damage) {
        final float squishy = 3F/16F;
        final float extraAxialSquish = 10F/16F;
        float perc = cc.getProgressPerc();
        float p = perc*squishy;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        drawContents(partial, cc, extraAxialSquish, perc, p);
        GL11.glPopMatrix();
    }

    private void drawContents(float partial, TileEntityCompressionCrafter cc, float extraAxialSquish, float perc, float p) {
        if (!cc.isPrimaryCrafter()) {
            return;
        }
        if (cc.upperCorner == null || cc.lowerCorner == null
            || (!Minecraft.getMinecraft().gameSettings.fancyGraphics && !Core.dev_environ)) {
            return;
        }
        GL11.glTranslatef(-cc.getPos().getX(), -cc.getPos().getY(), -cc.getPos().getZ());
        GL11.glPushMatrix();
        if (perc > 0.75F) {
            float jiggle = perc - 0.75F;
            jiggle /= 32; //this gets us 1 pixel of jiggle room
            rand.setSeed((long) (((long) Integer.MAX_VALUE) * perc));
            GL11.glTranslatef((float) rand.nextGaussian() * jiggle, (float) rand.nextGaussian() * jiggle, (float) rand.nextGaussian() * jiggle);
        }
        Coord up = cc.upperCorner;
        Coord lo = cc.lowerCorner;
        float cx = (up.x + lo.x + 1) / 2F;
        float cy = (up.y + lo.y + 1) / 2F;
        float cz = (up.z + lo.z + 1) / 2F;
        float sx, sy, sz;
        EnumFacing fd = cc.craftingAxis;
        sx = sy = sz = 1 - p;
        if (fd.getFrontOffsetX() != 0) sx = 1 + perc * extraAxialSquish;
        if (fd.getFrontOffsetY() != 0) sy = 1 + perc * extraAxialSquish;
        if (fd.getFrontOffsetZ() != 0) sz = 1 + perc * extraAxialSquish;

        NORELEASE.fixme("s/getDirectionVec().getX()/getFrontOffsetX()");

        //Unfortunately, the transformed origin is equal to the world's origin.
        //So it scales towards the origin instead of the center of the compression area.
        //We need to translate some amount to make up for it.
        //Actual position: cx*sx; desired is cx. So translate cx - cx*sx

        GL11.glTranslatef(cx - cx * sx, cy - cy * sy, cz - cz * sz);

        GL11.glScalef(sx, sy, sz);
        AxisAlignedBB bounds = drawSquishingBlocks(up, lo, partial);
        GL11.glPopMatrix();


        drawObscurringBox(bounds);
    }

    FzModel shroud = new FzModel("compact_shroud");

    private void drawObscurringBox(AxisAlignedBB bounds) {
        // Note usage of "<" rather than "<="
        //GL11.glTranslated(bounds.minX - 1, bounds.minY - 1, bounds.minZ - 1);
        GL11.glTranslated(bounds.minX + 1, bounds.minY, bounds.minZ);
        int dx = 0, dy, dz;
        for (double xD = bounds.minX; xD < bounds.maxX; xD++) {
            dy = 0;
            for (double yD = bounds.minY; yD < bounds.maxY; yD++) {
                dz = 0;
                for (double zD = bounds.minZ; zD < bounds.maxZ; zD++) {
                    GL11.glTranslated(dx, dy, dz);
                    shroud.draw();
                    GL11.glTranslated(-dx, -dy, -dz);
                    dz++;
                }
                dy++;
            }
            dx++;
        }
        NORELEASE.fixme("draw the shroud model at each block; needs to be scaled down or something");
        // Shroud'll be a cube w/ textures on all sides being the front face of the CompACT model
    }

    private AxisAlignedBB drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
        double spx, spy, spz;
        spx = TileEntityRendererDispatcher.staticPlayerX;
        spy = TileEntityRendererDispatcher.staticPlayerY;
        spz = TileEntityRendererDispatcher.staticPlayerZ;
        TileEntityRendererDispatcher.staticPlayerX = TileEntityRendererDispatcher.staticPlayerY = TileEntityRendererDispatcher.staticPlayerZ = 0;
        try {
            return _drawSquishingBlocks(upperCorner, lowerCorner, partial);
        } finally {
            TileEntityRendererDispatcher.staticPlayerX = spx;
            TileEntityRendererDispatcher.staticPlayerY = spy;
            TileEntityRendererDispatcher.staticPlayerZ = spz;
        }
    }
    
    private AxisAlignedBB _drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
        Tessellator tessI = Tessellator.getInstance();
        WorldRenderer tess = tessI.getWorldRenderer();
        tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        AxisAlignedBB contentSize = null;
        bindTexture(Core.blockAtlas);
        World w = upperCorner.w;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        ArrayList<TileEntity> tileEntities = new ArrayList<TileEntity>();
        EnumWorldBlockLayer any = EnumWorldBlockLayer.SOLID;
        BlockRendererDispatcher br = Minecraft.getMinecraft().getBlockRendererDispatcher();
        for (EnumWorldBlockLayer pass : EnumWorldBlockLayer.values()) {
            for (int x = lowerCorner.x; x <= upperCorner.x; x++) {
                for (int y = lowerCorner.y; y <= upperCorner.y; y++) {
                    for (int z = lowerCorner.z; z <= upperCorner.z; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        IBlockState bs = w.getBlockState(pos);
                        Block b = bs.getBlock();
                        if (pass == any) {
                            if (b == null) {
                                // Use a full block; we want to obscure the sides of squishing blocks as well.
                                if (contentSize == null) {
                                    contentSize = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
                                } else {
                                    contentSize.union(new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1));
                                }
                            } else {
                                if (contentSize == null) {
                                    contentSize = b.getSelectedBoundingBox(w, pos);
                                } else {
                                    AxisAlignedBB extend = b.getSelectedBoundingBox(w, pos);
                                    if (extend != null) {
                                        contentSize = contentSize.union(extend);
                                    }
                                }
                            }
                            TileEntity te = w.getTileEntity(pos);
                            if (te instanceof TileEntityCompressionCrafter) continue; // Avoid strange derps
                            if (te != null) {
                                tileEntities.add(te);
                            }
                        }
                        if (b == Blocks.air || b == null) continue;
                        if (b.canRenderInLayer(pass)) {
                            ForgeHooksClient.setRenderLayer(pass);
                            br.renderBlock(bs, pos, w, tess);
                        }
                    }
                }
            }
        }

        tessI.draw();

        for (TileEntity te : tileEntities) {
            TileEntityRendererDispatcher.instance.renderTileEntity(te, partial, 0);
        }

        return contentSize;
    }

}
