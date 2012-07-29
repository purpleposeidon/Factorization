package factorization.client.render;

import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityMirror;

public class TileEntityMirrorRenderer extends TileEntitySpecialRenderer {
    class MirrorModel extends ModelBase {
        ModelSingleTexturedBox parts[] = new ModelSingleTexturedBox[1];
        ModelRenderer mr;

        public MirrorModel() {
            mr = new ModelRenderer(this, 0, 0);
            mr.setTextureSize(16, 16);
            parts[0] = new ModelSingleTexturedBox(mr, 1, 2,
                    -0.5F, -0.5F, -0.5F, 2, 2, 1, 1F / 16F);
        }

        public void render() {
            for (ModelSingleTexturedBox box : parts) {
                box.render(Tessellator.instance, 1F / 16F);
            }
        }
    }

    MirrorModel model = new MirrorModel();
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        GL11.glPushMatrix();
        TileEntityMirror mirror = (TileEntityMirror) te;
        float d = 0.5F - 2F / 16F - 1.35F / 32F; //....
        d = 0.5F;
        GL11.glTranslatef((float) x + d, (float) y + d, (float) z + d);
        GL11.glRotatef(mirror.rotation + 90, 0, 1, 0);
        GL11.glColor4f(1, 1, 1, 1);

        this.bindTextureByName(Core.texture_file_item);
        float s = 1.2f;
        GL11.glScalef(s, s, s);

        GL11.glTranslatef(-0.5F, -0.3F, 0.3F);
        GL11.glRotatef(-45, 1, 0, 0);
        FactorizationRender.renderItemIn2D(9);

        GL11.glPopMatrix();
    }

}
