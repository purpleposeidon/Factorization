package factorization.client.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityGreenware;
import factorization.common.TileEntityGreenware.ClayState;


public class TileEntityGreenwareRender extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double viewx, double viewy, double viewz, float partial) {
        TileEntityGreenware gw = (TileEntityGreenware) te;
        int dryTime = gw.dryTime;
        if (!gw.canEdit()) {
            if (gw.renderedAsBlock) {
                return;
            } else if (gw.getState() == ClayState.DRY) {
                //prevents AO flickering on & off
                gw.dryTime = Integer.MAX_VALUE;
            }
        }
        Core.profileStartRender("ceramics");
        
        GL11.glPushMatrix();
        GL11.glTranslated(viewx, viewy, viewz);
        
        BlockRenderGreenware.instance.renderInInventory();
        BlockRenderGreenware.instance.setTileEntity(gw);
        BlockRenderGreenware.instance.renderDynamic(gw);
        GL11.glPopMatrix();
        gw.renderedAsBlock = false;
        gw.dryTime = dryTime;
        Core.profileEndRender();
    }

}
