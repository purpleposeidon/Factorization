package factorization.client.render;

import java.util.Random;

import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import factorization.common.Core;
import factorization.common.TileEntitySolarTurbine;

public class TileEntitySolarTurbineRender extends TileEntitySpecialRenderer {
    class Spinner extends ModelBase
    {
        public ModelRenderer driveshaft;
        public Spinner() {
            int h = 4;
            this.textureHeight = 64;
            this.textureWidth = 64;
            driveshaft = new ModelRenderer(this, 32, 0);
            driveshaft.addBox(-0.5F, -h, -0.5F,
                    1, h, 1,
                    0.25F);
            driveshaft.rotationPointX = 0;
            driveshaft.rotationPointY = 1F;
            driveshaft.rotationPointZ = 0;
        }

        public void renderAll() {
            this.driveshaft.render(0.0625F);
        }
    }

    Spinner axle = new Spinner();
    static Random rand = new Random();
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntitySolarTurbine turbine = (TileEntitySolarTurbine) te;
        GL11.glPushMatrix();
        this.bindTextureByName(Core.texture_file_item);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef((float) x, (float) y + 1.0F, (float) z + 1.0F);
        GL11.glScalef(1.0F, -1.0F, -1.0F);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(turbine.fan_rotation, 0, 1, 0);
        axle.renderAll();

        float s = 0.60f;
        GL11.glScalef(s, s, s);
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0.4F);
        //GL11.glTranslatef(0, 0, -0.7F); //or rotate 270 and use this
        drawProp();

        GL11.glPopMatrix();
    }

    void drawProp() {
        FactorizationBlockRender.renderItemIn2D(Core.registry.fan.getIconFromDamage(0));
    }

}
