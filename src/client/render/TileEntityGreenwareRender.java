package factorization.client.render;

import static org.lwjgl.opengl.GL11.*;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.RenderingCube.Vector;
import factorization.common.TileEntityGreenware;

import net.minecraft.src.RenderBlocks;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;


public class TileEntityGreenwareRender extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        Core.profileStartRender("ceramics");
        glPushMatrix();
        glTranslated(x, y, z);
        
        TileEntityGreenware greenware = (TileEntityGreenware) te;
        BlockRenderSculpture.instance.renderInInventory();
        BlockRenderSculpture.instance.renderDynamic(greenware);
        glPopMatrix();
        Core.profileEndRender();
    }

}
