package factorization.darkiron;

import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockIcons;
import factorization.darkiron.BlockDarkIronOre.Glint;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;

public class GlintRenderer extends TileEntitySpecialRenderer {
    
    Minecraft mc = Minecraft.getMinecraft();
    Vec3 sideVec = new Vec3(0, 0, 0);

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double dx, double dy, double dz, float partial) {
        double distPacity = (dx + 0.5)*(dx + 0.5) + (dy + 0.5)*(dy + 0.5) + (dz + 0.5)*(dz + 0.5);
        if (distPacity > 64) {
            return;
        }
        distPacity = (6 - Math.sqrt(distPacity))/2;
        if (distPacity > 0.8) {
            distPacity = 0.8;
        }
        BlockDarkIronOre.Glint te = (Glint) tileEntity;
        te.lastRenderedTick = te.getWorld().getTotalWorldTime();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0, 0xF0);
        bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(dx, dy, dz);
        
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tess = Tessellator.instance;
        BlockRenderHelper block = BlockRenderHelper.instance;
        EntityPlayer player = mc.thePlayer;
        
        Vec3 lookVec = player.getLook(partial).normalize();
        
        World w = te.getWorld();
        
        tess.startDrawingQuads();
        for (EnumFacing dir : EnumFacing.VALUES) {
            sideVec.xCoord = dir.getDirectionVec().getX();
            sideVec.yCoord = dir.getDirectionVec().getY();
            sideVec.zCoord = dir.getDirectionVec().getZ();
            //lookVec .dot. sideVec = cos(angle)
            double theta = Math.acos(lookVec.dotProduct(sideVec));
            float opacity = (float) (theta/(2*Math.PI));
            opacity *= Math.min(te.age, 10)/10F;
            opacity *= distPacity;
            double maxTheta = 3;
            double minTheta = 2.8;
            float r = NumUtil.uninterp((float) minTheta, (float) maxTheta, (float) theta);
            opacity *= r;
            
            int light = w.getBlockLightValue(te.xCoord + dir.getDirectionVec().getX(), te.yCoord + dir.getDirectionVec().getY(), te.zCoord + dir.getDirectionVec().getZ());
            opacity += (light/16F)*0.2F;
            opacity = Math.min(opacity, 0.35F);
            
            block.alpha = opacity;
            block.useTexture(null);
            block.setTexture(dir.ordinal(), BlockIcons.ore_dark_iron_glint);
            float d = 1F/512F;
            float a = -d, b = 1 + d;
            block.setBlockBounds(a, a, a, b, b, b);
            block.beginWithRotatedUVs();
            block.renderForTileEntity();
        }
        tess.draw();
        block.alpha = 1;
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        
    }
    
    RenderBlocks rb;
    
    public void func_147496_a(World newWorld) {
        rb = new RenderBlocks(newWorld);
    }

}
