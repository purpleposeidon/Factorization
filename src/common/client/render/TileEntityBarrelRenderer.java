package factorization.client.render;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import factorization.common.Core;
import factorization.common.TileEntityBarrel;

public class TileEntityBarrelRenderer extends TileEntitySpecialRenderer {

    EntityItem entity = new EntityItem(null);
    RenderItem itemRender; // = new RenderItem();
    Random random = new Random();
    private RenderBlocks renderBlocks = new RenderBlocks();
    static boolean field_77024_a = true;
    float item_rotation = 0;
    boolean render_item, render_text;
    boolean is_obscured = false;

    public TileEntityBarrelRenderer(boolean render_item, boolean render_text) {
        this.render_item = render_item;
        this.render_text = render_text;
    }

    @Override
    public void renderTileEntityAt(TileEntity ent, double x, double y, double z, float partialTickTime) {
        Core.profileStartRender("barrel");
        doRender(ent, x, y, z, partialTickTime);
        Core.profileEndRender();
    }
    
    
    void doRender(TileEntity ent, double x, double y, double z, float partialTickTime) {
        if (render_item == false && render_text == false) {
            return;
        }
        if (!(ent instanceof TileEntityBarrel)) {
            return;
        }
        TileEntityBarrel barrel = (TileEntityBarrel) ent;
        if (barrel.item == null || barrel.getItemCount() == 0) {
            return;
        }

        double idy = 0.40;
        itemRender = (RenderItem) RenderManager.instance.getEntityClassRenderObject(EntityItem.class);
        final double d = 0.01;

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        switch (barrel.facing_direction) {
        case 3:
            item_rotation = 0;
            setupLight(barrel, 0, 1);
            if (render_item && !is_obscured)
                handleRenderItem(barrel.item, x + 0.5, y + idy, z + 1 + d, 0, 0, 2);
            if (render_text && !is_obscured)
                renderItemCount(barrel, 2, x, y, z);
            break;

        case 2:
            item_rotation = -180;
            setupLight(barrel, 0, -1);
            if (render_item && !is_obscured)
                handleRenderItem(barrel.item, x + 0.5, y + idy, z - d, 0, 0, 0);
            if (render_text && !is_obscured)
                renderItemCount(barrel, 0, x, y, z);
            break;
        case 4:
            item_rotation = -90;
            setupLight(barrel, -1, 0);
            if (render_item && !is_obscured)
                handleRenderItem(barrel.item, x - d, y + idy, z + 0.5, 0, 0, 1);
            if (render_text && !is_obscured)
                renderItemCount(barrel, 1, x, y, z);
            break;
        case 5:
        default: //Need to keep a default in case something is messed up
            item_rotation = 90;
            setupLight(barrel, 1, 0);
            if (render_item && !is_obscured)
                handleRenderItem(barrel.item, x + 1 + d, y + idy, z + 0.5, 0, 0, 3);
            if (render_text && !is_obscured)
                renderItemCount(barrel, 3, x, y, z);
            break;
        }
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    void setupLight(TileEntityBarrel barrel, int dx, int dz) {
        World w = barrel.worldObj;
        if (w.isBlockOpaqueCube(barrel.xCoord + dx, barrel.yCoord, barrel.zCoord + dz)) {
            is_obscured = true;
            return;
        }
        is_obscured = false;
        int br = w.getLightBrightnessForSkyBlocks(barrel.xCoord + dx, barrel.yCoord, barrel.zCoord + dz, 0 /* minimum */);
        int var11 = br % 65536;
        int var12 = br / 65536;
        float scale = 0.6F;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, var11*scale, var12*scale);
    }

    void renderItemCount(TileEntityBarrel barrel, int side, double x, double y, double z) {
        FontRenderer fontRender = getFontRenderer();
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + .85, z + 0.5);
        GL11.glRotatef(90 * side, 0, 1, 0);
        GL11.glTranslated(0, 0, -0.51);
        GL11.glDepthMask(false);

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
            //			if (barrel.worldObj.getWorldTime() / 40 % 2 == 0 && barrel.flamingExplosion()) {
            //				color = 0xccaaaa;
            //			}
        }
        // XXX TODO: This sometimes renders too bright. Probably some OpenGL
        // state that some other part of the render is changing. Any ideas?
        // some possibilities: +TEXTURE_2D +BLEND ±LIGHTING
        GL11.glDisable(GL11.GL_LIGHTING);
        fontRender.drawString(t, -fontRender.getStringWidth(t) / 2, 0, color);
        // Seems like using anything other than 0 for alpha fucks shit up?
        // That function is ass. I hate it.

        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    Random rand = new Random();
    RenderItem renderItem = new RenderItem();

    public void handleRenderItem(ItemStack is, double x, double y, double z, float par8,
            float par9, int side) {
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glScalef(1, 1, -1);
        GL11.glRotatef(180, 0, 0, 1);
        GL11.glRotatef(180+item_rotation, 0, 1, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0);
        float scale = 1F/32F;
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(8, 6, 0);
        GL11.glScalef(1, 1, 0.01F);
        {
            //Original call:
            //renderItem.renderItemIntoGUI(getFontRenderer(), Minecraft.getMinecraft().renderEngine, is, 0, 0);
            //However, this draws the sparkly effect, which causes problems.
            TextureManager re = Minecraft.getMinecraft().renderEngine;
            FontRenderer fr = getFontRenderer();
            if (!ForgeHooksClient.renderInventoryItem(renderBlocks, re, is, true, 0, 0, 0)) {
                //renderItem.func_82406_b(fr, re, is, 0, 0);
                renderItem.renderItemIntoGUI(fr, re, is, 0, 0);
                //renderItem.drawItemIntoGui(fr, re, is.itemID, is.getItemDamage(), is.getIconIndex(), 0, 0);
            }
        }
        GL11.glPopMatrix();
    }
    
}
