package factorization.oreprocessing;

import static org.lwjgl.opengl.GL11.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

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
            FactorizationBlockRender.renderItemIcon(crys.growing_crystal.getIconIndex());
            glPopMatrix();
        }

        //render the fluid
        if (crys.solution != null) {
            glEnable(GL_BLEND);
            ItemStack sol = crys.solution;
            Tessellator tess = Tessellator.instance;
            Icon tex = Block.waterMoving.getBlockTextureFromSide(1);
            if (sol.getItem() == Core.registry.acid) {
                if (sol.getItemDamage() > 0) {
                    tex = Block.lavaMoving.getBlockTextureFromSide(0);
                    glColor4f(0.5F, 0.7F, 0F, 0.25F);
                } else {
                    glColor4f(1F, 0.7F, 0F, 0.5F);
                }
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            } else if (sol.getItem() == Item.bucketMilk) {
                float f = 0.9F;
                glColor4f(f, f, f, 0.2F);
                //glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
                //glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
                glBlendFunc(GL_SRC_COLOR, GL_SRC_ALPHA);
                //tex = 16 + 9; //Bluh!
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
            tess.yOffset = 9F / 16F;
            tess.addVertexWithUV(0, 0, 0, u0, v0);
            tess.addVertexWithUV(0, 0, 1, u0, v1);
            tess.addVertexWithUV(1, 0, 1, u1, v1);
            tess.addVertexWithUV(1, 0, 0, u1, v0);
            tess.yOffset = 0;
            tess.draw();
            glDisable(GL_BLEND);
        }

        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

}
