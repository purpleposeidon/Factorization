package factorization.truth.api;

import net.minecraft.client.gui.FontRenderer;

/**
 * (This class interface is here more for technical than to be helpful at this point in time.)
 */
public interface IWord {
    /**
     * @return the link for this word. May return null.
     */
    String getLink();

    /**
     * @param link the link to be set
     */
    void setLink(String link);

    /**
     * @param style The style to be set; this is an MC §-type style. Only used by the client typesetter.
     */
    void setStyle(String style);

    /**
     * @param font The font object being used
     * @return the width, in... whatever it is that FontRenderer.getStringWidth() returns
     */
    int getWidth(FontRenderer font);

    /**
     * @return The padding that should be above the word; TextWord uses 2.
     */
    int getPaddingAbove();

    /**
     * @return The height of the word.
     */
    int getWordHeight();

    /**
     * Draws the word
     * @param x X-position
     * @param y Y-position
     * @param hover true if the mouse is positioned over the word.
     * @param font The FontRenderer object.
     * @return The horizontal width drawn over; eg getWidth().
     */
    int draw(int x, int y, boolean hover, FontRenderer font);

    /**
     * Callback for drawing when the mouse is hovering.
     * @param mouseX X-position of the mouse.
     * @param mouseY Y-position of the mouse.
     */
    void drawHover(int mouseX, int mouseY);

    /**
     * Callback for when the mouse clicks on the word.
     * @return true if something happened
     */
    boolean onClick();

    /**
     * Convert the word to HTML.
     * @param out The output
     */
    void writeHtml(IHtmlTypesetter out);
}
