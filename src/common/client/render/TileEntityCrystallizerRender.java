package factorization.client.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_COLOR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderBlaze;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityCrystallizer;

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
            re.bindTextureFile("/terrain.png");

            int var18 = crys.growing_crystal.getItem().getColorFromItemStack(crys.growing_crystal, 0);
            float r = (float) (var18 >> 16 & 255) / 255.0F;
            float g = (float) (var18 >> 8 & 255) / 255.0F;
            float b = (float) (var18 & 255) / 255.0F;
            GL11.glColor4f(r, g, b, 1.0F);
            FactorizationBlockRender.renderIcon(crys.growing_crystal.getIconIndex());
            glPopMatrix();
        }

        //render the fluid
        if (crys.solution != null) {
            glEnable(GL_BLEND);
            ItemStack sol = crys.solution;
            Tessellator tess = Tessellator.instance;
            Icon tex = Block.waterMoving.getBlockTextureFromSide(1);
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
                //tex = 16 + 9; //Bluh!
            } else {
                glColor4f(1F, 1F, 1F, 1);
                glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
            }
            RenderEngine re = Minecraft.getMinecraft().renderEngine;
            //XXX TODO NORELEASE: fix renderer
            re.bindTextureFile(texture_file);
            float u = ((/*tex*/ 0 & 15) << 4) / 256.0F;
            float v = (/*tex*/ 0 & 240) / 256.0F;
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
