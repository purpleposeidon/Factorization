package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;

public class WordPage extends AbstractPage {
    ArrayList<ArrayList<Word>> text = new ArrayList();
    Object figure;
    static int TEXT_HEIGHT = 9;
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
                word = new TextWord("    ", word.getLink());
            } else if (lineLen == 0 && tw.text.trim().isEmpty()) {
                return;
            }
        }
        text.get(text.size() - 1).add(word);
        if (font != null) {
            lineLen += word.getWidth(font);
        }
    }
    
    void nl() {
        ArrayList newLine = new ArrayList();
        newLine.add(new TextWord("", null));
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
     * @param line
     * @return the "tuple" int[] { padUp, padDown }
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
    void draw(DocViewer doc, int ox, int oy, String hoveredLink) {
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
