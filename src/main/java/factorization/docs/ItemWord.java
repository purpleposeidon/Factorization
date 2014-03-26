package factorization.docs;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ItemWord extends Word {
    ItemStack is;
    
    public ItemWord(ItemStack is) {
        super(getDefaultHyperlink(is));
        this.is = is;
    }
    
    public ItemWord(ItemStack is, String hyperlink) {
        super(hyperlink);
        this.is = is;
    }
    
    static String getDefaultHyperlink(ItemStack is) {
        if (is == null) return null;
        return "cgi/recipes/" + is.getUnlocalizedName();
    }

    @Override
    public String toString() {
        return is + " ==> " + hyperlink;
    }
    
    @Override
    public int getWidth(FontRenderer font) {
        return 16;
    }

    @Override
    public int draw(DocViewer doc, int x, int y) {
        if (is == null) return 16;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            doc.drawItem(is, x, y - 4);
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
        if (is == null) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        doc.drawItemTip(is, mouseX, mouseY);
        GL11.glPopAttrib();
    }
}
