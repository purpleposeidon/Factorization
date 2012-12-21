package factorization.client.render;

import static org.lwjgl.opengl.GL11.*;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.TileEntityGreenware;
import factorization.common.TileEntityGreenware.ClayState;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;


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
        
        glPushMatrix();
        glTranslated(viewx, viewy, viewz);
        
        BlockRenderGreenware.instance.renderInInventory();
        BlockRenderGreenware.instance.setTileEntity(gw);
        BlockRenderGreenware.instance.renderDynamic(gw);
        glPopMatrix();
        gw.renderedAsBlock = false;
        gw.dryTime = dryTime;
        Core.profileEndRender();
    }

}
