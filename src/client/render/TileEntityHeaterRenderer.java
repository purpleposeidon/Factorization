package factorization.client.render;

import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.Texture;
import factorization.common.TileEntityHeater;

public class TileEntityHeaterRenderer extends TileEntitySpecialRenderer {
    ModelElement element = new ModelElement();

    class ModelElement extends ModelBase {
        ModelSingleTexturedBox box;
        ModelRenderer model;

        public ModelElement() {
            this.textureHeight = this.textureWidth = 16;
            model = new ModelRenderer(this, 0, 0);
            box = new ModelSingleTexturedBox(model, Texture.heater_element, 0, 0, 0, 0, 1, 1, 1, 1.0F);
        }

        public void renderAll() {
            box.render(Tessellator.instance, 1F / 16F);
        }
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        Core.profileStartRender("heater");
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        this.bindTextureByName(Core.texture_file_block);
        //GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        float color = 0.1F;
        TileEntityHeater heater = (TileEntityHeater) te;
        color += (heater.heat / (float) heater.maxHeat) * (1 - color);
        GL11.glColor4f(color, color, color, 1.0F);
        float d = 0.5F - 2F / 16F - 1.35F / 32F; //....
        GL11.glTranslatef((float) x + d, (float) y + d, (float) z + d);
        float scale = 5F + 5 / 16F;
        scale -= 10F / 128F;
        GL11.glScalef(scale, scale, scale);
        element.renderAll();
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_LIGHTING);
        Core.profileEndRender();
    }

}
