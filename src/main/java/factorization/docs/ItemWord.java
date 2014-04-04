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
    
    ItemStack getItem() {
        if (is != null) return is;
        if (entries == null) return null;
        long now = System.currentTimeMillis() / 1000;
        now %= entries.length;
        return entries[(int) now];
    }

    @Override
    public int draw(DocViewer doc, int x, int y) {
        ItemStack toDraw = getItem();
        if (toDraw == null) return 16;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            doc.drawItem(toDraw, x, y - 4);
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
