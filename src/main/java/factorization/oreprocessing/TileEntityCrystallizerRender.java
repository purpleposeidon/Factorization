package factorization.oreprocessing;

import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class TileEntityCrystallizerRender extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        TileEntityCrystallizer crys = (TileEntityCrystallizer) te;
        glPushMatrix();
        glTranslatef((float) x, (float) y, (float) z);

        //render a bit of string
        glColor4f(1, 1, 1, 1);
        glDisable(GL_TEXTURE_2D);
        glLineWidth(8);
        glBegin(GL_LINES);
        glVertex3f(0.5F, 15F / 16F, 0.5F);
        glVertex3f(0.5F, 0.25F, 0.5F);
        glEnd();
        glLineWidth(1);
        glEnable(GL_TEXTURE_2D);
        
        
        glDisable(GL_LIGHTING);
        //render the item, growing as it nears completion
        TextureManager re = Minecraft.getMinecraft().renderEngine;
        if (crys.growing_crystal != null && crys.progress > 0) {
            glPushMatrix();
            float s = crys.getProgress();
            if (s < 2F / 16F) {
                s = 2F / 16F;
            }
            s *= 12F / 16F;

            glTranslatef(0.5F, 3F / 16F, 0.5F);
            glScalef(s, s, s);
            glTranslatef(-0.5F, 0, 1F / 32F);
            re.bindTexture(Core.blockAtlas);

            int var18 = crys.growing_crystal.getItem().getColorFromItemStack(crys.growing_crystal, 0);
            float r = (float) (var18 >> 16 & 255) / 255.0F;
            float g = (float) (var18 >> 8 & 255) / 255.0F;
            float b = (float) (var18 & 255) / 255.0F;
            GL11.glColor4f(r, g, b, 1.0F);
            FactorizationBlockRender.renderItemIIcon(crys.growing_crystal.getIconIndex());
            glPopMatrix();
        }

        //render the fluid
        if (crys.solution != null) {
            glPushAttrib(GL_COLOR_BUFFER_BIT);
            glAlphaFunc(GL_GREATER, 0.1F);
            glEnable(GL_BLEND);
            ItemStack sol = crys.solution;
            Tessellator tess = Tessellator.instance;
            IIcon tex = Blocks.flowing_water.getBlockTextureFromSide(1);
            if (sol.getItem() == Core.registry.acid) {
                if (sol.getItemDamage() > 0) {
                    tex = Blocks.flowing_lava.getBlockTextureFromSide(0);
                    glColor4f(0.5F, 0.7F, 0F, 0.25F);
                } else {
                    glColor4f(1F, 0.7F, 0F, 0.5F);
                }
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            } else if (sol.getItem() == Items.milk_bucket) {
                float f = 0.9F;
                glColor4f(f, f, f, 0.9F);
                tex = Blocks.quartz_block.getIcon(0, 0); // Or white stained clay
            } else if (sol.getItem() == Items.lava_bucket) {
                tex = Blocks.flowing_lava.getBlockTextureFromSide(1);
                glColor4f(1, 1, 1, 0.5F);
            } else {
                glColor4f(1F, 1F, 1F, 1);
                glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
            }
            re.bindTexture(Core.blockAtlas);
            float u0 = tex.getMinU();
            float v0 = tex.getMinV();
            float u1 = tex.getMaxU();
            float v1 = tex.getMaxV();
            tess.startDrawingQuads();
            tess.setTranslation(0, 9F/16F, 0);
            tess.addVertexWithUV(0, 0, 0, u0, v0);
            tess.addVertexWithUV(0, 0, 1, u0, v1);
            tess.addVertexWithUV(1, 0, 1, u1, v1);
            tess.addVertexWithUV(1, 0, 0, u1, v0);
            tess.setTranslation(0, 0, 0);
            tess.draw();
            glPopAttrib();
        }

        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

}
