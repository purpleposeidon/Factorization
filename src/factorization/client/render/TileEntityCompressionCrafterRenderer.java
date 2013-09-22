package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockIcons.ExtendedIcon;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.TileEntityCompressionCrafter;

public class TileEntityCompressionCrafterRenderer extends TileEntitySpecialRenderer {
    float textureOffset;
    ExtendedIcon interp_side = new ExtendedIcon(BlockIcons.compactSideSlide) {
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
    
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityCompressionCrafter cc = (TileEntityCompressionCrafter) te;
        if (cc == null) {
            return;
        }
        interp_side.under = BlockIcons.compactSideSlide;
        bindTexture(Core.blockAtlas);
        final float squishy = 2F/16F;
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
        ForgeDirection facing = cc.getFacing();
        FzOrientation fo = FzOrientation.fromDirection(facing);
        Quaternion q = Quaternion.fromOrientation(fo);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        
        block.begin();
        
        block.rotateCenter(q);
        
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(cc.worldObj, cc.xCoord, cc.yCoord, cc.zCoord));
        GL11.glDisable(GL11.GL_LIGHTING);
        block.renderForTileEntity();
        Tessellator.instance.draw();
        
        GL11.glPopMatrix();
        
        if (cc.isPrimaryCrafter() && cc.upperCorner != null && cc.lowerCorner != null
                && Minecraft.getMinecraft().gameSettings.fancyGraphics && Core.dev_environ /* NORELEASE */) {
            //Also: A config option?
            //Maybe just only if on fancy.
            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, (float) z);
            GL11.glTranslated(0.5, 0.5, 0.5);
            GL11.glScalef(1 - p, 1 + p, 1 - p);
            GL11.glTranslated(-0.5, -0.5, -0.5);
            GL11.glTranslatef(-cc.xCoord, -cc.yCoord, -cc.zCoord);
            float dx = cc.upperCorner.x - cc.lowerCorner.x;
            float dy = cc.upperCorner.y - cc.lowerCorner.y;
            float dz = cc.upperCorner.z - cc.lowerCorner.z;
            dx *= p;
            dy *= p;
            dz *= p;
            //GL11.glTranslatef(dx, -dy, -dz);
            float dtX = dx - dx*p;
            float dtY = dy - dy*p;
            float dtZ = dz - dz*p;
            GL11.glTranslatef(0, 0, 0);
            drawSquishingBlocks(cc.upperCorner, cc.lowerCorner, partial);
            GL11.glPopMatrix();
        }
        
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private static Tessellator tess = new Tessellator();
    
    private void drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
        Tessellator real = Tessellator.instance;
        if (real == tess) {
            return; //Oh boy!
        }
        double spx, spy, spz;
        spx = TileEntityRenderer.staticPlayerX;
        spy = TileEntityRenderer.staticPlayerY;
        spz = TileEntityRenderer.staticPlayerZ;
        TileEntityRenderer.staticPlayerX = TileEntityRenderer.staticPlayerY = TileEntityRenderer.staticPlayerZ = 0;
        try {
            Tessellator.instance = tess;
            _drawSquishingBlocks(upperCorner, lowerCorner, partial);
        } finally {
            Tessellator.instance = real;
            TileEntityRenderer.staticPlayerX = spx;
            TileEntityRenderer.staticPlayerY = spy;
            TileEntityRenderer.staticPlayerZ = spz;
        }
    }
    
    private static Tessellator tesrator = new Tessellator();
    private void _drawSquishingBlocks(Coord upperCorner, Coord lowerCorner, float partial) {
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
                        Block b = Block.blocksList[w.getBlockId(x, y, z)];
                        if (b == null) {
                            continue;
                        }
                        if (renderPass == 3) {
                            TileEntity te;
                            if ((te = w.getBlockTileEntity(x, y, z)) != null) {
                                Tessellator.instance = tesrator;
                                TileEntityRenderer.instance.renderTileEntity(te, partial);
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
