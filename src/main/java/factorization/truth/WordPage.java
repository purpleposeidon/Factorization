package factorization.truth;

import factorization.truth.api.AbstractPage;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;

public class WordPage extends AbstractPage {
    public ArrayList<ArrayList<Word>> text = new ArrayList<ArrayList<Word>>();
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
    
    void add(Word word) {
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
        ArrayList<Word> newLine = new ArrayList<Word>();
        newLine.add(new TextWord(""));
        text.add(newLine);
        lineLen = 0;
    }
    
    Word click(int relativeX, int relativeY) {
        int y = 0;
        for (ArrayList<Word> line : text) {
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
            for (Word word : line) {
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
    int[] getVerticalPadding(ArrayList<Word> line) {
        int padUp = 0, padDown = 0;
        for (Word word : line) {
            padUp = Math.max(word.getPaddingAbove(), padUp);
            padDown = Math.max(word.getPaddingBelow(), padDown);
        }
        return new int[] {padUp, padDown};
    }
    
    @Override
    public void draw(DocViewer doc, int ox, int oy, String hoveredLink) {
        int y = 0;
        try {
            for (ArrayList<Word> line : text) {
                int x = 0;
                int[] padding = getVerticalPadding(line);
                int paddingTop = padding[0], paddingBottom = padding[1];
                y += paddingTop;
                for (Word word : line) {
                    x += word.draw(doc, ox + x, oy + y, hoveredLink != null && hoveredLink.equals(word.getLink()));
                }
                y += paddingBottom;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
