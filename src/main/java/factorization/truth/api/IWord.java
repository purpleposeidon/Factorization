package factorization.truth.api;

import factorization.truth.DocViewer;
import net.minecraft.client.gui.FontRenderer;

public interface IWord {
    String getLink();

    void setLink(String link);

    void setStyle(String style);

    int getWidth(FontRenderer font);

    int getPaddingAbove();

    int getPaddingBelow();

    int draw(int x, int y, boolean hover, FontRenderer font);

    void drawHover(int mouseX, int mouseY);

    boolean onClick();
}
