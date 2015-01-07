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
    public int draw(DocViewer page, int x, int y, boolean hover) {
        String t = text;
        int color = DocViewer.dark_color_scheme ? 0xEEEEEE : 0x111111;
        if (getLink() != null) {
            //EnumChatFormatting col = DocViewer.dark_color_scheme ? EnumChatFormatting.AQUA : EnumChatFormatting.DARK_BLUE;
            t = /*"" + col +*/ EnumChatFormatting.UNDERLINE + text;
            if (hover) color = DocViewer.dark_color_scheme ? 0xFF0080 : 0x441111;
            else color = DocViewer.dark_color_scheme ? 0x00FFFF : 0x000080;
        }
        page.getFont().drawString(t, x, y, color); // The return value of drawString isn't helpful.
        return page.getFont().getStringWidth(text);
    }
    
    @Override
    public int getPaddingAbove() {
        return 2;
    }
    
    @Override
    public int getPaddingBelow() {
        return WordPage.TEXT_HEIGHT;
    }
}
