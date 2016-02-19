package factorization.weird.barrel;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.util.SpaceUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer<TileEntityDayBarrel> {

    void doDraw(TileEntityDayBarrel barrel, ItemStack is) {
        FzOrientation bo = barrel.orientation;
        EnumFacing face = bo.facing;
        if (SpaceUtil.sign(face) == 1) {
            GlStateManager.translate(face.getDirectionVec().getX(), face.getDirectionVec().getY(), face.getDirectionVec().getZ());
        }
        GlStateManager.translate(
                0.5*(1 - Math.abs(face.getDirectionVec().getX())), 
                0.5*(1 - Math.abs(face.getDirectionVec().getY())), 
                0.5*(1 - Math.abs(face.getDirectionVec().getZ()))
                );
        
        Quaternion quat = Quaternion.fromOrientation(bo.getSwapped());
        quat.glRotate();
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.translate(0.25, 0.25 - 1.0/16.0, -1.0/128.0);
        if (barrel.type.isHopping()) {
            double time = barrel.getWorld().getTotalWorldTime();
            if (Math.sin(time/20) > 0) {
                double delta = Math.max(0, Math.sin(time/2)/16);
                GlStateManager.translate(0, delta, 0);
            }
        }

        
        //GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);

        boolean hasLabel = renderItemCount(is, barrel);
        handleRenderItem(is, barrel, hasLabel);
        
        //GL11.glPopAttrib();
        GlStateManager.enableLighting();;
    }

    //Another optimization: don't render if the barrel's facing a solid block
    //(A third optimization: somehow get the SBRH to cull faces. Complicated & expensive?)
    @Override
    public void renderTileEntityAt(TileEntityDayBarrel barrel, double x, double y, double z, float partialTicks, int destroyStage) {
        ItemStack is = barrel.item;
        if (is == null || barrel.getItemCount() <= 0) {
            return;
        }
        Core.profileStart("barrel");
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        doDraw(barrel, is);

        GlStateManager.popMatrix();
        Core.profileEnd();
    }

    String getCountLabel(ItemStack item, TileEntityDayBarrel barrel) {
        int ms = item.getMaxStackSize();
        int count = barrel.getItemCount();
        if (count == 1) return "";
        String t = "";
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
                    t += "+";
                }
                t += r;
            }
        }
        if (barrel.canLose()) {
            t = "!" + t + "!";
        }
        if (barrel.type == TileEntityDayBarrel.Type.CREATIVE) {
            t = "i";
        }
        return t;
    }
    
    final String[] fontIdx = new String[] {
        "0123",
        "4567",
        "89*+",
        "i!  " // 'i' stands in for ∞, '!' stands in for '!!'
    };
    
    boolean renderItemCount(ItemStack item, TileEntityDayBarrel barrel) {
        if (!FzConfig.render_barrel_text) return false;
        final String t = getCountLabel(item, barrel);
        if (t.isEmpty()) {
            return false;
        }

        bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.disableLighting();

        final TextureAtlasSprite font = BarrelModel.font;
        final int len = t.length();
        final double char_width = 1.0/10.0;
        final double char_height = 1.0/10.0;
        final Tessellator tessI = Tessellator.getInstance(); //new Tessellator(len * 4);
        WorldRenderer tess = tessI.getWorldRenderer();
        tess.setTranslation(-char_width * len / 2 + 0.25, -char_height - 1F/32F, 0);
        tess.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX); // 3 double vertex positions + 2 double UV positions
        double du = (font.getMaxU() - font.getMinU()) / 4;
        double dv = (font.getMaxV() - font.getMinV()) / 4;
        double u = font.getMinU();
        double v = font.getMinV();
        for (int i = 0; i < len; i++) {
            char c = t.charAt(i);
            int x = 0, y = 0;
            boolean found = false;
            foundIdx: for (y = 0; y < fontIdx.length; y++) {
                String idx = fontIdx[y];
                for (x = 0; x < idx.length(); x++) {
                    if (c == idx.charAt(x)) {
                        found = true;
                        break foundIdx;
                    }
                }
            }
            if (!found) continue;
            double IX = i*char_width;
            final double dy = 1.0 - (1.0/256.0);
            tess.pos(IX + char_width, 0, 0).tex(u + (x + 1) * du, v + y * dv).endVertex();
            tess.pos(IX, 0, 0).tex(u + x * du, v + y * dv).endVertex();
            tess.pos(IX, char_height, 0).tex(u + x * du, v + (y + dy) * dv).endVertex();
            tess.pos(IX + char_width, char_height, 0).tex(u + (x + 1) * du, v + (y + dy) * dv).endVertex();
        }
        tessI.draw();
        tess.setTranslation(0, 0, 0);

        GlStateManager.enableLighting();
        GlStateManager.rotate(180, 0, 0, 1);
        return true;
    }

    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    public void handleRenderItem(ItemStack is, TileEntityDayBarrel barrel, boolean hasLabel) {
        if (!FzConfig.render_barrel_item) return;
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        float labelD = hasLabel ? 0F : -1F/16F;

        {
            GlStateManager.translate(0, labelD, 1F/16F);
            float scale = 1F/32F;
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.scale(1, 1, -0.02F);
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(is, 0, 0);
        }
        GlStateManager.popMatrix();
    }
}
