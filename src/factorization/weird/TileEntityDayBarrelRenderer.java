package factorization.weird;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.ResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.common.ItemIIcons;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.weird.TileEntityDayBarrel.Type;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer {

    void doDraw(TileEntityDayBarrel barrel, ItemStack is) {
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
        handleRenderItem(is);
        GL11.glEnable(GL11.GL_LIGHTING);
        
        
    }

    // NOTE TODO: This could be optimized by using a custom font renderer.
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
        Core.profileStart("barrel");
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        
        
        if (FzConfig.render_barrel_use_displaylists && barrel.type != Type.HOPPING && barrel.should_use_display_list && barrel != FactoryType.DAYBARREL.getRepresentative()) {
            if (barrel.display_list == -1) {
                FzUtil.checkGLError("FZ -- before barrel display list update. Someone left us a mess!");
                if (barrel.display_list == -1) {
                    barrel.display_list = GLAllocation.generateDisplayLists(1);
                }
                // https://www.opengl.org/archives/resources/faq/technical/displaylist.htm 16.070
                GL11.glNewList(barrel.display_list, GL11.GL_COMPILE);
                doDraw(barrel, is);
                GL11.glEndList();
                if (FzUtil.checkGLError("FZ -- after barrel display list; does the item have an advanced renderer?")) {
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
    
    void renderItemCount(ItemStack item, TileEntityDayBarrel barrel) {
        final String t = getCountLabel(item, barrel);
        if (t.isEmpty()) {
            return;
        }
        GL11.glRotatef(180, 0, 0, 1);
        final IIcon font;
        final Minecraft mc = Minecraft.getMinecraft();
        if (item.getItemSpriteNumber() == 1) {
            font = BlockIcons.barrel_font;
            mc.renderEngine.bindTexture(Core.blockAtlas);
        } else {
            font = ItemIIcons.barrel_font;
            mc.renderEngine.bindTexture(Core.itemAtlas);
        }
        final int len = t.length();
        final double char_width = 1.0/10.0;
        final double char_height = 1.0/10.0;
        final Tessellator tess = Tessellator.instance;
        tess.xOffset = -char_width*len/2 + 0.25;
        tess.yOffset = -char_height;
        tess.startDrawingQuads();
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
            tess.addVertexWithUV(IX + char_width, 0, 0,
                    u + (x + 1)*du, v + y*dv);
            tess.addVertexWithUV(IX, 0, 0,
                    u + x*du, v + y*dv);
            tess.addVertexWithUV(IX, char_height,
                    0, u + x*du, v + (y + dy)*dv);
            tess.addVertexWithUV(IX + char_width, char_height, 0,
                    u + (x + 1)*du, v + (y + dy)*dv);
            
        }
        tess.draw();
        tess.xOffset = tess.yOffset = tess.zOffset = 0;
        GL11.glRotatef(180, 0, 0, 1);
    }

    Tessellator voidTessellator = new Tessellator() {
        public void addVertex(double par1, double par3, double par5) {}
        public void startDrawing(int par1) {}
        public int draw() { return 0; }
    };
    
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    
    RenderItem renderItem = new RenderItem();

    
    class Intercepter extends TextureManager {
        TextureManager realGuy = Minecraft.getMinecraft().renderEngine;
        public Intercepter(ResourceManager par1ResourceManager) {
            super(par1ResourceManager);
        }
        
        public ResourceLocation getResourceLocation(int par1) {
            return realGuy.getResourceLocation(par1);
        }
        
        public void bindTexture(ResourceLocation res) {
            if (RES_ITEM_GLINT.equals(res)) {
                Tessellator.instance = voidTessellator;
            } else {
                //Minecraft.getMinecraft().renderEngine.bindTexture(res);
                realGuy.bindTexture(res);
            }
        }
    }
    TextureManager interception = null;
    
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
            if (!is.hasEffect(0)) {
                renderItem.renderItemAndEffectIntoGUI(fr, re, is, 0, 0);
            } else {
                if (interception == null) {
                    interception = new Intercepter(Minecraft.getMinecraft().getResourceManager());
                }
                Tessellator orig = Tessellator.instance;
                renderItem.renderItemAndEffectIntoGUI(fr, interception, is, 0, 0);
                Tessellator.instance = orig;
            }
        }
        GL11.glPopMatrix();
    }
}
