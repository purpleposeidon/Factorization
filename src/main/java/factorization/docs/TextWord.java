package factorization.docs;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

public class TextWord extends Word {
    final String text;
    
    public TextWord(String text, String hyperlink) {
        super(hyperlink);
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text + " ==> " + getLink();
    }
    
    @Override
    public int getWidth(FontRenderer font) {
        return font.getStringWidth(text);
    }
    
    @Override
    public int draw(DocViewer page, int x, int y) {
        String t = text;
        if (getLink() != null) {
            EnumChatFormatting col = DocViewer.dark_color_scheme ? EnumChatFormatting.AQUA : EnumChatFormatting.DARK_BLUE;
            t = "" + col + EnumChatFormatting.UNDERLINE + text;
        }
        int color = DocViewer.dark_color_scheme ? 0xEEEEEE : 0x111111;
        page.getFont().drawString(t, x, y, color); // The return value of drawString isn't helpful.
        return page.getFont().getStringWidth(text);
    }
}
