package factorization.truth;

import factorization.truth.api.AbstractPage;
import factorization.truth.api.IWord;
import factorization.truth.word.TextWord;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;

public class WordPage extends AbstractPage {
    public ArrayList<ArrayList<IWord>> text = new ArrayList<ArrayList<IWord>>();
    public static int TEXT_HEIGHT = 9;
    int lineLen = 0;
    FontRenderer font;
    
    WordPage(FontRenderer font) {
        this.font = font;
        if (font != null) {
            TEXT_HEIGHT = font.FONT_HEIGHT;
        }
        nl();
    }
    
    void add(IWord word) {
        if (word instanceof TextWord) {
            TextWord tw = (TextWord) word;
            if (tw.text.equals("\t")) {
                word = new TextWord("    ");
                word.setLink(tw.getLink());
                word.setStyle(tw.getStyle());
            } else if (lineLen == 0 && tw.text.trim().isEmpty()) {
                return;
            }
        }
        text.get(text.size() - 1).add(word);
        if (font != null) {
            lineLen += word.getWidth(font);
        }
    }
    
    public void nl() {
        ArrayList<IWord> newLine = new ArrayList<IWord>();
        newLine.add(new TextWord(""));
        text.add(newLine);
        lineLen = 0;
    }
    
    IWord click(int relativeX, int relativeY) {
        int y = 0;
        for (ArrayList<IWord> line : text) {
            if (y > relativeY) break;
            int[] padding = getVerticalPadding(line);
            int paddingTop = padding[0], paddingBottom = padding[1];
            int yChange = paddingTop + paddingBottom;
            if (y + yChange < relativeY) {
                y += yChange;
                continue;
            }
            y += paddingTop;
            int x = 0;
            for (IWord word : line) {
                int width = word.getWidth(font);
                if (x <= relativeX && relativeX <= x + width) {
                    return word;
                }
                x += width;
                if (x > relativeX) break;
            }
            y += paddingBottom;
        }
        return null;
    }

    /**
     * Return the padding on a line.
     * @param line the line to get the padding of
     * @return the "tuple" int[] { padUp, padDown }
     * TODO: Custom ArrayList that keeps track of the padding
     */
    int[] getVerticalPadding(ArrayList<IWord> line) {
        int padUp = 0, padDown = 0;
        for (IWord word : line) {
            padUp = Math.max(word.getPaddingAbove(), padUp);
            padDown = Math.max(word.getWordHeight(), padDown);
        }
        return new int[] {padUp, padDown};
    }
    
    @Override
    public void draw(DocViewer doc, int ox, int oy, String hoveredLink) {
        int y = 0;
        try {
            for (ArrayList<IWord> line : text) {
                int x = 0;
                int[] padding = getVerticalPadding(line);
                int paddingTop = padding[0], paddingBottom = padding[1];
                y += paddingTop;
                for (IWord word : line) {
                    boolean hover = hoveredLink != null && hoveredLink.equals(word.getLink());
                    x += word.draw(ox + x, oy + y, hover, font);
                }
                y += paddingBottom;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
