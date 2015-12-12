package factorization.oreprocessing;

import factorization.shared.Core;
import factorization.util.FzUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class TileEntityCrystallizerRender extends TileEntitySpecialRenderer<TileEntityCrystallizer> {

    EntityLivingBase nobody = new EntityLivingBase(null) {
        @Override public ItemStack getHeldItem() { return null; }
        @Override public ItemStack getEquipmentInSlot(int slotIn) { return null; }
        @Override public ItemStack getCurrentArmor(int slotIn) { return null; }
        @Override public void setCurrentItemOrArmor(int slotIn, ItemStack stack) { }
        @Override public ItemStack[] getInventory() { return new ItemStack[0]; }
    };
    @Override
    public void renderTileEntityAt(TileEntityCrystallizer crys, double x, double y, double z, float partial, int damage) {
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
            Minecraft.getMinecraft().getRenderItem().renderItemModelForEntity(crys.growing_crystal, nobody, ItemCameraTransforms.TransformType.NONE);
            glPopMatrix();
        }

        //render the fluid
        if (crys.solution != null) {
            glPushAttrib(GL_COLOR_BUFFER_BIT);
            glAlphaFunc(GL_GREATER, 0.1F);
            glEnable(GL_BLEND);
            ItemStack sol = crys.solution;
            Block texSrc = Blocks.flowing_water;
            if (sol.getItem() == Core.registry.acid) {
                if (sol.getItemDamage() > 0) {
                    texSrc = Blocks.flowing_lava;
                    glColor4f(0.5F, 0.7F, 0F, 0.25F);
                } else {
                    glColor4f(1F, 0.7F, 0F, 0.5F);
                }
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            } else if (sol.getItem() == Items.milk_bucket) {
                float f = 0.9F;
                glColor4f(f, f, f, 0.9F);
                texSrc = Blocks.quartz_block;
            } else if (sol.getItem() == Items.lava_bucket) {
                texSrc = Blocks.flowing_lava;
                glColor4f(1, 1, 1, 0.5F);
            } else {
                glColor4f(1F, 1F, 1F, 1);
                glBlendFunc(GL_SRC_COLOR, GL_ONE_MINUS_SRC_ALPHA);
            }
            TextureAtlasSprite tex = FzUtil.getIcon(texSrc);
            re.bindTexture(Core.blockAtlas);
            float u0 = tex.getMinU();
            float v0 = tex.getMinV();
            float u1 = tex.getMaxU();
            float v1 = tex.getMaxV();
            Tessellator tessI = Tessellator.getInstance();
            WorldRenderer tess = tessI.getWorldRenderer();
            tess.startDrawing(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            tess.setTranslation(0, 9F/16F, 0);
            tess.tex(u0, v0).putPosition(0, 0, 0);
            tess.tex(u0, v1).putPosition(0, 0, 1);
            tess.tex(u1, v1).putPosition(1, 0, 1);
            tess.tex(u1, v0).putPosition(1, 0, 0);
            tess.setTranslation(0, 0, 0);
            tessI.draw();
            glPopAttrib();
        }

        glEnable(GL_LIGHTING);
        glPopMatrix();
    }

}
