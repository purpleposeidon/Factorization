package factorization.truth.word;

import factorization.truth.DocViewer;
import factorization.truth.api.IWord;
import net.minecraft.client.gui.FontRenderer;

public abstract class Word implements IWord {
    private static final String default_style = "";
    private String hyperlink;
    protected String style = default_style;

    public void setLink(String link) {
        this.hyperlink = link;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getLink() {
        return hyperlink;
    }
    
    public abstract int getWidth(FontRenderer font);
    public abstract int draw(int x, int y, boolean hover, FontRenderer font);

    @Override
    public void drawHover(int mouseX, int mouseY) { }
    
    public int getPaddingAbove() { return 1; }
    public int getWordHeight() { return 1; }
    
    public int getLinkColor(boolean hover) {
        boolean dark = DocViewer.dark();
        int color = dark ? 0xEEEEEE : 0x111111;
        if (getLink() != null) {
            if (hover) color = dark ? 0xFF0080 : 0x441111;
            else color = dark ? 0x00FFFF : 0x000080;
        }
        return color;
    }

    public boolean onClick() {
        return false;
    }

    public String getStyle() {
        return style;
    }
}
