package factorization.weird;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.common.ItemIcons;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.util.RenderUtil;
import factorization.util.SpaceUtil;
import factorization.weird.TileEntityDayBarrel.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

public class TileEntityDayBarrelRenderer extends TileEntitySpecialRenderer {

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
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float partial) {
        if (!(tileentity instanceof TileEntityDayBarrel)) {
            return;
        }
        TileEntityDayBarrel barrel = (TileEntityDayBarrel) tileentity;
        ItemStack is = barrel.item;
        if (is == null || barrel.getItemCount() <= 0) {
            return;
        }
        Core.profileStart("barrel");
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        if (FzConfig.render_barrel_use_displaylists && barrel.type != Type.HOPPING && barrel.should_use_display_list && barrel != FactoryType.DAYBARREL.getRepresentative()) {
            if (barrel.display_list == -1) {
                Item item = is.getItem();
                boolean crazyItem = item.hasEffect(is, 0) && item.requiresMultipleRenderPasses();
                if (!crazyItem) {
                    crazyItem = itemHasCustomRender(is);
                }
                if (crazyItem) {
                    // FIXME: If a potion-barrel draws before a nether-star barrel, shit goes wonky
                    // There may be other situations where it pops up.
                    barrel.should_use_display_list = false;
                    doDraw(barrel, is);
                    return;
                }
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

    private boolean itemHasCustomRender(ItemStack item) {
        return MinecraftForgeClient.getItemRenderer(item, INVENTORY) != null;
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
        final IIcon font;
        final Minecraft mc = Minecraft.getMinecraft();
        if (item.getItemSpriteNumber() == 1) {
            font = BlockIcons.barrel_font;
            mc.renderEngine.bindTexture(Core.blockAtlas);
        } else {
            font = ItemIcons.barrel_font;
            mc.renderEngine.bindTexture(Core.itemAtlas);
        }
        final int len = t.length();
        final double char_width = 1.0/10.0;
        final double char_height = 1.0/10.0;
        final Tessellator tess = Tessellator.instance;
        tess.setTranslation(-char_width * len / 2 + 0.25, -char_height - 1F/32F, 0);
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
        tess.setTranslation(0, 0, 0);
        GL11.glRotatef(180, 0, 0, 1);
        return true;
    }

    Tessellator voidTessellator = new Tessellator() {
        public void addVertex(double par1, double par3, double par5) {}
        public void startDrawing(int par1) {}
        public int draw() { return 0; }
    };
    
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    
    final RenderItem renderItem = new RenderItem();
    
    class Intercepter extends TextureManager {
        final TextureManager realGuy = Minecraft.getMinecraft().renderEngine;
        public Intercepter(IResourceManager par1ResourceManager) {
            super(par1ResourceManager);
        }
        
        public ResourceLocation getResourceLocation(int par1) {
            return realGuy.getResourceLocation(par1);
        }
        
        public void bindTexture(ResourceLocation res) {
            if (RES_ITEM_GLINT.equals(res)) {
                Tessellator.instance = voidTessellator;
            } else {
                realGuy.bindTexture(res);
            }
        }
    }
    TextureManager interception = null;
    EntityItem entityitem;
    
    public void handleRenderItem(ItemStack is, TileEntityDayBarrel barrel, boolean hasLabel) {
        if (!FzConfig.render_barrel_item) return;
        //Got problems? Consider looking at ForgeHooksClient.renderInventoryItem, that might be better than this here.
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 0, 1);
        float labelD = hasLabel ? 0F : -1F/16F;
        boolean useEntityRenderer = is.getItem().requiresMultipleRenderPasses();
        boolean useInterceptionRenderer = is.hasEffect(0);
        if (FzConfig.render_barrel_force_entity_render) useEntityRenderer = true;
        if (FzConfig.render_barrel_force_no_intercept) useInterceptionRenderer = false;

        if (useEntityRenderer) {
            GL11.glRotatef(180, 1, 0, 0);
            GL11.glTranslatef(-12F/16F, -6.75F/16F - labelD, 0);
            if (entityitem == null) {
                entityitem = new EntityItem(null, 0, 0, 0, NetworkFactorization.EMPTY_ITEMSTACK.copy());
            }
            entityitem.setEntityItemStack(is);
            entityitem.hoverStart = 0.0F;
            GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean fancy = gs.fancyGraphics;
            gs.fancyGraphics = false;
            RenderItem.renderInFrame = true;
            RenderManager.instance.renderEntityWithPosYaw(entityitem, 1.0D, 0.0D, 0.0D, 0.0F, 0.0F);
            RenderItem.renderInFrame = false;
            gs.fancyGraphics = fancy;
        } else {
            GL11.glTranslatef(0, labelD, 1F/32F);
            float scale = 1F/32F;
            GL11.glScalef(scale, scale, scale);
            GL11.glScalef(1, 1, -0.02F);
            TextureManager re = Minecraft.getMinecraft().renderEngine;
            FontRenderer fr = func_147498_b();
            if (useInterceptionRenderer) {
                if (interception == null) {
                    interception = new Intercepter(Minecraft.getMinecraft().getResourceManager());
                }
                Tessellator orig = Tessellator.instance;
                renderItem.renderItemAndEffectIntoGUI(fr, interception, is, 0, 0);
                Tessellator.instance = orig;
            } else {
                renderItem.renderItemAndEffectIntoGUI(fr, re, is, 0, 0);
            }
        }
        GL11.glPopMatrix();
    }
}
