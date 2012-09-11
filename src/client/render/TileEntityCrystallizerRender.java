package factorization.client.render;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityCrystallizer;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;

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
            RenderEngine re = Minecraft.getMinecraft().renderEngine;
            re.bindTexture(re.getTexture(crys.growing_crystal.getItem().getTextureFile()));

            int var18 = crys.growing_crystal.getItem().getColorFromDamage(crys.growing_crystal.getItemDamage(), 0);
            float r = (float) (var18 >> 16 & 255) / 255.0F;
            float g = (float) (var18 >> 8 & 255) / 255.0F;
            float b = (float) (var18 & 255) / 255.0F;
            GL11.glColor4f(r, g, b, 1.0F);
            FactorizationBlockRender.renderItemIn2D(crys.growing_crystal.getIconIndex());
            glPopMatrix();
        }

        //render the fluid
        if (crys.solution != null) {
            glEnable(GL_BLEND);
            ItemStack sol = crys.solution;
            Tessellator tess = Tessellator.instance;
            int tex = Block.waterMoving.getBlockTextureFromSide(1);
            String texture_file = Block.waterMoving.getTextureFile();
            if (sol.getItem() == Core.registry.acid) {
                glColor4f(1F, 1F, 1F, 0.5F);
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            } else if (sol.getItem() == Item.bucketMilk) {
                float f = 0.9F;
                glColor4f(f, f, f, 0.2F);
                //glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
                //glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
                glBlendFunc(GL_SRC_COLOR, GL_SRC_ALPHA);
                texture_file = Core.texture_file_block;
                tex = 16 + 9;
            } else {
                glColor4f(1F, 1F, 1F, 1);
                glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
            }
            RenderEngine re = Minecraft.getMinecraft().renderEngine;
            re.bindTexture(re.getTexture(texture_file));
            float u = ((tex & 15) << 4) / 256.0F;
            float v = (tex & 240) / 256.0F;
            float w = 1F / 16F;
            tess.startDrawingQuads();
            tess.yOffset = 9F / 16F;
            tess.addVertexWithUV(0, 0, 0, u, v);
            tess.addVertexWithUV(0, 0, 1, u, v + w);
            tess.addVertexWithUV(1, 0, 1, u + w, v + w);
            tess.addVertexWithUV(1, 0, 0, u + w, v);
            tess.yOffset = 0;
            tess.draw();
            glDisable(GL_BLEND);
        }

        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

}
