package factorization.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.TileEntityDayBarrel;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer {
    static int NORELEASE_i = 0;
    @Override
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float partial) {
        if (!(tileentity instanceof TileEntityDayBarrel)) {
            return;
        }
        TileEntityDayBarrel barrel = (TileEntityDayBarrel) tileentity;
        ItemStack is = barrel.item;
        if (is == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        
        FzOrientation bo = barrel.orientation;
        ForgeDirection face = bo.facing;
        if (face.offsetX + face.offsetY + face.offsetZ == 1) {
            GL11.glTranslated(face.offsetX, face.offsetY, face.offsetZ);
        }
        GL11.glTranslated(
                0.5*(1 - Math.abs(face.offsetX)), 
                0.5*(1 - Math.abs(face.offsetY)), 
                0.5*(1 - Math.abs(face.offsetZ))
                );
        
        Quaternion quat = Quaternion.fromOrientation(bo.getSwapped());
        quat.glRotate();
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslated(0.25, 0.25 - 1.0/16.0, -1.0/128.0);
        
        renderItemCount(barrel);
        handleRenderItem(is);
        GL11.glPopMatrix();
        
        NORELEASE_i++;
    }
    
    void renderItemCount(TileEntityDayBarrel barrel) {
        FontRenderer fontRender = getFontRenderer();
        GL11.glPushMatrix();
        GL11.glTranslated(-0.25, 0.125, 0);
        //GL11.glDepthMask(false);

        float scale = 0.01F;
        GL11.glScalef(scale, scale, scale);
        GL11.glRotatef(180, 0, 0, 1);

        String t = "";
        int ms = barrel.item.getMaxStackSize();
        if (ms == 1 || barrel.getItemCount() == ms) {
            t += barrel.getItemCount();
        } else {
            int q = barrel.getItemCount() / ms;
            if (q > 0) {
                t += (barrel.getItemCount() / ms) + "*" + ms;
            }
            int r = (barrel.getItemCount() % ms);
            if (r != 0) {
                if (q > 0) {
                    t += " + ";
                }
                t += r;
            }
        }
        int color = 0xbbbbbb;
        if (barrel.canLose()) {
            t = "!! " + t + " !!";
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        fontRender.drawString(t, -fontRender.getStringWidth(t) / 2, 0, color);
        
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    RenderItem renderItem = new RenderItem();

    public void handleRenderItem(ItemStack is) {
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 0, 1);
        //GL11.glTranslatef(-0.5F, -0.5F, 0);
        float scale = 1F/32F;
        GL11.glScalef(scale, scale, scale);
        GL11.glScalef(1, 1, -0.02F);
        {
            TextureManager re = Minecraft.getMinecraft().renderEngine;
            FontRenderer fr = getFontRenderer();
            //this draws the sparkly effect, which causes problems.
            if (is.hasEffect(0)) {
                float orig_z = renderItem.zLevel;
                renderItem.zLevel = 23;
                renderItem.renderItemAndEffectIntoGUI(fr, re, is, 0, 0);
                renderItem.zLevel = orig_z;
            } else {
                renderItem.renderItemAndEffectIntoGUI(fr, re, is, 0, 0);
            }
        }
        GL11.glPopMatrix();
    }
}
