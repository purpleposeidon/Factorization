package factorization.ceramics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;


public class ItemRenderGlazeBucket implements IItemRenderer {

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        if (type == ItemRenderType.INVENTORY) {
            return true;
        }
        if (type == ItemRenderType.ENTITY) {
            return true;
        }
        if (type == ItemRenderType.EQUIPPED) {
            //return true;
            return false;
        }
        return false;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type == ItemRenderType.ENTITY) {
            if (helper == ItemRendererHelper.ENTITY_BOBBING) {
                return true;
            }
        }
        return false;
    }

    private static RenderItem itemRenderer = new RenderItem();
    @Override
    public void renderItem(ItemRenderType type, ItemStack is, Object... data) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tess = Tessellator.instance;
        TextureManager re = mc.renderEngine;
        ItemGlazeBucket bucket = (ItemGlazeBucket) is.getItem();
        if (type == ItemRenderType.ENTITY) {
            float s = 1F/16F;
            if (!RenderItem.renderInFrame) {
                GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            }
            GL11.glScalef(s, s, s);
            GL11.glRotatef(180, 0, 0, 1);
            GL11.glTranslatef(-8, -16, 0);
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (type == ItemRenderType.EQUIPPED) {
            Icon bi = bucket.getIconFromDamage(0);
            ItemRenderer.renderItemIn2D(tess, bi.getMinU(), bi.getMinV(), bi.getMaxU(), bi.getMaxV(), bi.getIconWidth(), bi.getIconHeight(), 0.0625F);
            float s = 1F/16F;
            GL11.glScalef(s, s, s);
        } else {
            if (type == ItemRenderType.INVENTORY) {
                RenderHelper.enableGUIStandardItemLighting();
                itemRenderer.zLevel = 0.5F;
            } else {
                itemRenderer.zLevel = 0;
            }
            itemRenderer.renderItemIntoGUI(mc.fontRenderer, re, is, 0, 0);
        }
        Icon glaze = bucket.getIcon(is, 1, null, null, 0);
        if (glaze == null) {
            glaze = Core.blockMissingIcon;
        }
        re.bindTexture(Core.blockAtlas);
        
        double lx = 4, ly = 5;
        double hx = 12, hy = 13;
        double height = hy - ly;
        float fullness = bucket.getFullness(is);
        if (fullness > 0 && fullness < 1F/16F) {
            fullness = 1F/16F;
        }
        ly += height*(1-fullness);
        tess.startDrawingQuads();
        tess.addVertexWithUV(lx, hy, 0, glaze.getInterpolatedU(lx), glaze.getInterpolatedV(hy));
        tess.addVertexWithUV(hx, hy, 0, glaze.getInterpolatedU(hx), glaze.getInterpolatedV(hy));
        tess.addVertexWithUV(hx, ly, 0, glaze.getInterpolatedU(hx), glaze.getInterpolatedV(ly));
        tess.addVertexWithUV(lx, ly, 0, glaze.getInterpolatedU(lx), glaze.getInterpolatedV(ly));
        tess.draw();
    }

}
