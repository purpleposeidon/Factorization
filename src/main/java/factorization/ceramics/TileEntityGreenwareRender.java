package factorization.ceramics;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;


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
        bindTexture(Core.blockAtlas);
        GL11.glPushMatrix();
        GL11.glTranslated(viewx, viewy, viewz);
        BlockRenderGreenware.instance.renderInInventory();
        BlockRenderGreenware.instance.setTileEntity(gw);
        BlockRenderGreenware.instance.renderDynamic(gw);
        BlockRenderGreenware.instance.clearWorldReferences();
        GL11.glPopMatrix();
        gw.lastTouched = lt;
        Core.profileEndRender();
    }

}
