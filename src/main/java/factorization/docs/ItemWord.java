package factorization.docs;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class ItemWord extends Word {
    final ItemStack is;
    
    public ItemWord(ItemStack is, String hyperlink) {
        super(hyperlink);
        this.is = is;
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
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        doc.drawItem(is, x, y - 4);
        GL11.glPopAttrib();
        return 16;
    }
}
