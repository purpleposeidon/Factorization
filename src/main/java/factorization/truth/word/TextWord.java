package factorization.truth.word;

import factorization.truth.WordPage;
import factorization.truth.api.IHtmlTypesetter;
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
    public int draw(int x, int y, boolean hover, FontRenderer font) {
        int color = getLinkColor(hover);
        font.drawString(style + text, x, y, color);
        return getWidth(font); // The return value of drawString isn't helpful.
    }

    @Override
    public void writeHtml(IHtmlTypesetter out) {
        out.html(text);
    }

    @Override
    public int getPaddingAbove() {
        return 2;
    }
    
    @Override
    public int getWordHeight() {
        return WordPage.TEXT_HEIGHT;
    }
}
