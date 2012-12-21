package factorization.client.render;

import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.common.TileEntityMixer;

public class TileEntityMixerRenderer extends TileEntitySolarTurbineRender {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityMixer mixer = (TileEntityMixer) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + 1.0F, (float) z + 1.0F);
        renderWithRotation(mixer.getRotation()*1.4F);
        GL11.glPopMatrix();
    }

}
