package factorization.weird;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.weird.TileEntityDayBarrel.Type;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer {
    //NOTE TODO: This could be optimized by using a custom font renderer.
    //Create a font icon digits, '+', '*', '!', and '∞'; add it to both atlases.
    //This would let the barrel render with a single texture binding for standard items.
    //Another optimization: don't render if the barrel's facing a solid block
    //(A third optimization: somehow get the SBRH to cull faces. Complicated & expensive?)
    @Override
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float partial) {
        if (!(tileentity instanceof TileEntityDayBarrel)) {
            return;
        }
        TileEntityDayBarrel barrel = (TileEntityDayBarrel) tileentity;
        ItemStack is = barrel.item;
        if (is == null || barrel.getItemCount() == 0) {
            return;
        }
        //NORELEASE: Check glEnables/glDisables/pushAttrib...
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
        if (barrel.type == Type.HOPPING) {
            double time = barrel.worldObj.getTotalWorldTime();
            if (Math.sin(time/20) > 0) {
                double delta = Math.max(0, Math.sin(time/2)/16);
                GL11.glTranslated(0, delta, 0);
            }
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        renderItemCount(is, barrel);
        GL11.glEnable(GL11.GL_LIGHTING);
        //NORELEASE: diff w/ previous version!
        GL11.glEnable(GL11.GL_NORMALIZE);
        
        //These two enableStandardLighting calls *may* make it look better, but I'm guessing they're slow.
        //RenderHelper.enableStandardItemLighting();
        
        handleRenderItem(is);
        GL11.glEnable(GL11.GL_LIGHTING);
        
        
        GL11.glPopMatrix();
        //RenderHelper.enableStandardItemLighting(); //Necessary for reset; might also be able to use pusthAttr?
    }
    
    void renderItemCount(ItemStack item, TileEntityDayBarrel barrel) {
        FontRenderer fontRender = getFontRenderer();
        if (fontRender == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(-0.25, 0.125, 0);

        float scale = 0.01F;
        GL11.glScalef(scale, scale, scale);
        GL11.glRotatef(180, 0, 0, 1);

        String t = "";
        int ms = item.getMaxStackSize();
        int count = barrel.getItemCount();
        if (ms == 1 || count == ms) {
            t += count;
        } else {
            int q = count / ms;
            if (q > 0) {
                t += (count / ms) + "*" + ms;
            }
            int r = (count % ms);
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
        if (barrel.type == Type.CREATIVE) {
            t = "∞";
        }
        
        fontRender.drawString(t, -fontRender.getStringWidth(t) / 2, 0, color);
        
        GL11.glPopMatrix();
    }

    RenderItem renderItem = new RenderItem();

    public void handleRenderItem(ItemStack is) {
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GL11.glPushMatrix();
        float scale = 1F/32F;
        GL11.glScalef(scale, scale, scale);
        if (true) {
            GL11.glScalef(-1, -1, -1);
            GL11.glScalef(1, 1, 0.02F);
        } /*else {
            GL11.glScalef(-1, -1, -1);
            GL11.glScalef(1, 1, 0.02F);
        }*/
        //RenderHelper.enableGUIStandardItemLighting();
        //RenderHelper.enableStandardItemLighting();
        //RenderHelper.disableStandardItemLighting();
        //GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, RenderHelper.setColorBuffer(RenderHelper.field_82884_b.xCoord, RenderHelper.field_82884_b.yCoord, RenderHelper.field_82884_b.zCoord, 0.0D));
        //GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, RenderHelper.setColorBuffer(RenderHelper.field_82885_c.xCoord, RenderHelper.field_82885_c.yCoord, RenderHelper.field_82885_c.zCoord, 0.0D));
        
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
