package factorization.client.render;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityGreenware;


public class TileEntityGreenwareRender extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double viewx, double viewy, double viewz, float partial) {
        TileEntityGreenware gw = (TileEntityGreenware) te;
        if (!gw.shouldRenderTesr) {
            return;
        }
        Core.profileStartRender("ceramics");
        
        //prevents AO flickering on & off
        int lt = gw.lastTouched;
        gw.lastTouched = 0;
        bindTextureByName("/terrain.png");
        GL11.glPushMatrix();
        GL11.glTranslated(viewx, viewy, viewz);
        BlockRenderGreenware.instance.renderInInventory();
        BlockRenderGreenware.instance.setTileEntity(gw);
        BlockRenderGreenware.instance.renderDynamic(gw);
        GL11.glPopMatrix();
        gw.lastTouched = lt;
        Core.profileEndRender();
    }

}
