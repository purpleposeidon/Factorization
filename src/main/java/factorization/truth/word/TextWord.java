package factorization.truth.word;

import factorization.truth.DocViewer;
import factorization.truth.WordPage;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

public class TextWord extends Word {
    public final String text;
    private short width_cache = -1;

    public TextWord(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text + " ==> " + getLink();
    }

    private static final String LINK_STYLE = "" + EnumChatFormatting.UNDERLINE;
    @Override
    public void setLink(String link) {
        super.setLink(link);
        if (style.isEmpty()) {
            setStyle(LINK_STYLE);
        } else {
            setStyle(getStyle() + LINK_STYLE);
        }
    }

    @Override
    public int getWidth(FontRenderer font) {
        if (width_cache != -1) return width_cache;
        if (font == null) return 0;
        return width_cache = (short) font.getStringWidth(style + text);
    }

    @Override
    public void setStyle(String style) {
        super.setStyle(style);
        width_cache = -1;
    }

    @Override
    public int draw(DocViewer page, int x, int y, boolean hover) {
        int color = getLinkColor(page, hover);
        page.getFont().drawString(style + text, x, y, color);
        return getWidth(page.getFont()); // The return value of drawString isn't helpful.
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
