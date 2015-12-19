package factorization.weird;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzIcons;
import factorization.util.RenderUtil;
import factorization.util.SpaceUtil;
import factorization.weird.TileEntityDayBarrel.Type;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer<TileEntityDayBarrel> {

    void doDraw(TileEntityDayBarrel barrel, ItemStack is) {
        FzOrientation bo = barrel.orientation;
        EnumFacing face = bo.facing;
        if (SpaceUtil.sign(face) == 1) {
            GL11.glTranslated(face.getDirectionVec().getX(), face.getDirectionVec().getY(), face.getDirectionVec().getZ());
        }
        GL11.glTranslated(
                0.5*(1 - Math.abs(face.getDirectionVec().getX())), 
                0.5*(1 - Math.abs(face.getDirectionVec().getY())), 
                0.5*(1 - Math.abs(face.getDirectionVec().getZ()))
                );
        
        Quaternion quat = Quaternion.fromOrientation(bo.getSwapped());
        quat.glRotate();
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslated(0.25, 0.25 - 1.0/16.0, -1.0/128.0);
        if (barrel.type == Type.HOPPING) {
            double time = barrel.getWorld().getTotalWorldTime();
            if (Math.sin(time/20) > 0) {
                double delta = Math.max(0, Math.sin(time/2)/16);
                GL11.glTranslated(0, delta, 0);
            }
        }

        
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        
        boolean hasLabel = renderItemCount(is, barrel);
        handleRenderItem(is, barrel, hasLabel);
        
        GL11.glPopAttrib();
        GL11.glEnable(GL11.GL_LIGHTING);
        
        
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
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        if (FzConfig.render_barrel_use_displaylists && barrel.type != Type.HOPPING && barrel.should_use_display_list && barrel != FactoryType.DAYBARREL.getRepresentative()) {
            if (barrel.display_list == -1) {
                RenderUtil.checkGLError("FZ -- before barrel display list update. Someone left us a mess!");
                if (barrel.display_list == -1) {
                    barrel.display_list = GLAllocation.generateDisplayLists(1);
                }
                // https://www.opengl.org/archives/resources/faq/technical/displaylist.htm 16.070
                GL11.glNewList(barrel.display_list, GL11.GL_COMPILE);
                doDraw(barrel, is);
                GL11.glEndList();
                if (RenderUtil.checkGLError("FZ -- after barrel display list; does the item have an advanced renderer?")) {
                    Core.logSevere("The item is: " + is);
                    Core.logSevere("At: " + new Coord(barrel));
                    barrel.should_use_display_list = false;
                    doDraw(barrel, is);
                } else {
                    GL11.glCallList(barrel.display_list);
                }
            } else {
                GL11.glCallList(barrel.display_list);
            }
        } else {
            doDraw(barrel, is);
        }
        
        GL11.glPopMatrix();
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
        if (barrel.type == Type.CREATIVE) {
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
        GL11.glRotatef(180, 0, 0, 1);
        final TextureAtlasSprite font = FzIcons.items$barrel_font;
        final int len = t.length();
        final double char_width = 1.0/10.0;
        final double char_height = 1.0/10.0;
        final Tessellator tessI = Tessellator.getInstance();
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
            tess.tex(u + (x + 1) * du, v + y * dv).putPosition(IX + char_width, 0, 0);
            tess.tex(u + x * du, v + y * dv).putPosition(IX, 0, 0);
            tess.tex(u + x * du, v + (y + dy) * dv).putPosition(IX, char_height, 0);
            tess.tex(u + (x + 1) * du, v + (y + dy) * dv).putPosition(IX + char_width, char_height, 0);
        }
        tessI.draw();
        tess.setTranslation(0, 0, 0);
        GL11.glRotatef(180, 0, 0, 1);
        return true;
    }

    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    public void handleRenderItem(ItemStack is, TileEntityDayBarrel barrel, boolean hasLabel) {
        if (!FzConfig.render_barrel_item) return;
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 0, 1);
        float labelD = hasLabel ? 0F : -1F/16F;

        {
            GL11.glTranslatef(0, labelD, 1F/32F);
            float scale = 1F/32F;
            GL11.glScalef(scale, scale, scale);
            GL11.glScalef(1, 1, -0.02F);
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(is, 0, 0);
        }
        GL11.glPopMatrix();
    }
}
