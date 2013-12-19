package factorization.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockDarkIronOre;
import factorization.common.BlockDarkIronOre.Glint;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.shared.Core;

public class GlintRenderer extends TileEntitySpecialRenderer {
    
    Minecraft mc = Minecraft.getMinecraft();
    Vec3 sideVec = Vec3.createVectorHelper(0, 0, 0);

    @Override
    public void renderTileEntityAt(TileEntity xte, double dx, double dy, double dz, float partial) {
        double distPacity = (dx + 0.5)*(dx + 0.5) + (dy + 0.5)*(dy + 0.5) + (dz + 0.5)*(dz + 0.5);
        if (distPacity > 6*6) {
            return;
        }
        if (distPacity > 4*4) {
            distPacity = (6 - Math.sqrt(distPacity))/2;
        } else {
            distPacity = 1;
        }
        BlockDarkIronOre.Glint te = (Glint) xte;
        te.lastRenderedTick = te.worldObj.getTotalWorldTime();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0, 0xF0);
        bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(dx + 0.5F, dy + 0.5F, dz + 0.5F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tess = Tessellator.instance;
        BlockRenderHelper block = BlockRenderHelper.instance;
        RenderBlocks rb = mc.renderGlobal.globalRenderBlocks;
        EntityPlayer player = mc.thePlayer;
        
        Vec3 lookVec = player.getLook(partial).normalize();
        
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            sideVec.xCoord = dir.offsetX;
            sideVec.yCoord = dir.offsetY;
            sideVec.zCoord = dir.offsetZ;
            //lookVec .dot. sideVec = cos(angle)
            double theta = Math.acos(lookVec.dotProduct(sideVec));
            float opacity = (float) (theta/(2*Math.PI));
            opacity *= Math.min(te.age, 10)/10F;
            opacity *= distPacity;
            //if (opacity > 0.35F) opacity = 0.35F;
            opacity *= 0.7F;
            GL11.glColor4f(1, 1, 1, opacity);
            block.useTexture(null);
            block.setTexture(dir.ordinal(), BlockIcons.ore_dark_iron_glint);
            float d = 1F/512F;
            float a = -d, b = 1 + d;
            block.setBlockBounds(a, a, a, b, b, b);
            block.renderForInventory(rb);
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopMatrix();
    }

}
