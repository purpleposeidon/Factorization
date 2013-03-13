package factorization.client.render;

import java.util.Random;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import factorization.common.Core;
import factorization.common.TileEntitySteamTurbine;

public class TileEntitySteamTurbineRender extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntitySteamTurbine turbine = (TileEntitySteamTurbine) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + 1.0F, (float) z + 1.0F);
        TileEntitySolarTurbineRender.renderWithRotation(turbine.fan_rotation);
        GL11.glPopMatrix();
    }
}
