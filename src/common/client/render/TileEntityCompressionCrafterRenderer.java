package factorization.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.BlockIcons.ExtendedIcon;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.TileEntityCompressionCrafter;

public class TileEntityCompressionCrafterRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityCompressionCrafter cc = (TileEntityCompressionCrafter) te;
        if (cc == null) {
            return;
        }
        bindTexture(Core.blockAtlas);
        float p = cc.getProgressPerc();
        p *= 7F/16F;
        final float textureOffset = p;
        
        BlockRenderHelper block = Core.registry.blockRender;
        Icon interp_side = new ExtendedIcon(BlockIcons.compactSideSlide) { //NORELEASE: Pull out!
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
        
        block.useTextures(
                null, null,
                interp_side, interp_side,
                interp_side, interp_side
                );
        final float d = 1F/128F;
        block.setBlockBounds(d, d, d, 1 - d, 1 /*- p*/, 1 - d);
        ForgeDirection facing = cc.getFacing();
        FzOrientation fo = FzOrientation.fromDirection(facing);
        Quaternion q = Quaternion.fromOrientation(fo);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        block.begin();
        block.rotateMiddle(q);
        
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(cc.worldObj, cc.xCoord, cc.yCoord, cc.zCoord));
        GL11.glDisable(GL11.GL_LIGHTING);
        block.renderForTileEntity();
        Tessellator.instance.draw();
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }

}
