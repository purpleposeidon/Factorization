package factorization.docs;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ItemWord extends Word {
    ItemStack is = null;
    ItemStack[] entries = null;
    
    public ItemWord(ItemStack is) {
        super(getDefaultHyperlink(is));
        this.is = is;
    }
    
    public ItemWord(ItemStack[] entries) {
        super(getDefaultHyperlink(entries));
        this.entries = entries;
    }
    
    public ItemWord(ItemStack is, String hyperlink) {
        super(hyperlink);
        this.is = is;
    }
    
    static String getDefaultHyperlink(ItemStack is) {
        if (is == null) return null;
        return "cgi/recipes/" + is.getUnlocalizedName();
    }
    
    static String getDefaultHyperlink(ItemStack[] items) {
        if (items == null || items.length == 0) return null;
        if (items.length == 1) return getDefaultHyperlink(items[0]);
        return null;
    }
    
    @Override
    public String getLink() {
        return getDefaultHyperlink(getItem());
    }

    @Override
    public String toString() {
        return is + " ==> " + getLink();
    }
    
    @Override
    public int getWidth(FontRenderer font) {
        return 16;
    }
    
    @Override
    public int getPaddingAbove() {
        return (16 - WordPage.TEXT_HEIGHT) / 2;
    }
    
    @Override
    public int getPaddingBelow() {
        return WordPage.TEXT_HEIGHT + getPaddingAbove();
    }
    
    ItemStack getItem() {
        if (is != null) return is;
        if (entries == null) return null;
        long now = System.currentTimeMillis() / 1000;
        now %= entries.length;
        return entries[(int) now];
    }

    @Override
    public int draw(DocViewer doc, int x, int y, boolean hover) {
        ItemStack toDraw = getItem();
        if (toDraw == null) return 16;
        y -= 4;
        
        {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            float gray = DocViewer.dark_color_scheme ? 0.2F : 139F/0xFF;
            GL11.glColor3f(gray, gray, gray);
            
            float z = 0;
            float d = 16;
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(x + 0, y + 0, z);
            GL11.glVertex3f(x + 0, y + d, z);
            GL11.glVertex3f(x + d, y + d, z);
            GL11.glVertex3f(x + d, y + 0, z);
            GL11.glEnd();
            
            if (hover) {
                int color = getLinkColor(hover);
                byte r = (byte) ((color >> 16) & 0xFF);
                byte g = (byte) ((color >> 8) & 0xFF);
                byte b = (byte) ((color >> 0) & 0xFF);
                GL11.glColor3b(r, g, b);
                GL11.glLineWidth(1);
                
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex3f(x + 0, y + 0, z);
                GL11.glVertex3f(x + 0, y + d, z);
                GL11.glVertex3f(x + d, y + d, z);
                GL11.glVertex3f(x + d, y + 0, z);
                GL11.glEnd();
            }
            
            GL11.glColor3f(1, 1, 1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            doc.drawItem(toDraw, x, y);
        } catch (Throwable t) {
            t.printStackTrace();
            is = null;
            try {
                Tessellator.instance.draw();
            } catch (IllegalStateException e) {
                // Ignore it.
            }
        }
        GL11.glPopAttrib();
        return 16;
    }
    
    @Override
    public void drawHover(DocViewer doc, int mouseX, int mouseY) {
        ItemStack toDraw = getItem();
        if (toDraw == null) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        doc.drawItemTip(toDraw, mouseX, mouseY);
        GL11.glPopAttrib();
    }
}
