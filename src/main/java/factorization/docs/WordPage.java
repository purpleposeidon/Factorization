package factorization.docs;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.FontRenderer;
import factorization.shared.NORELEASE;

public class WordPage extends AbstractPage {
    ArrayList<ArrayList<Word>> text = new ArrayList();
    Object figure;
    static int TEXT_HEIGHT = 9;
    int lineLen = 0;
    FontRenderer font;
    
    WordPage(FontRenderer font) {
        this.font = font;
        TEXT_HEIGHT = font.FONT_HEIGHT;
        nl();
    }
    
    void add(Word word) {
        if (word instanceof TextWord) {
            TextWord tw = (TextWord) word;
            if (tw.text == "\t") {
                word = new TextWord("    ", word.getLink());
            } else if (lineLen == 0 && tw.text.trim().isEmpty()) {
                return;
            }
        }
        text.get(text.size() - 1).add(word);
        lineLen += word.getWidth(font);
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
            int yChange = getPad(line, true) + getPad(line, false);
            if (y + yChange < relativeY) {
                y += yChange;
                continue;
            }
            y += getPad(line, true);
            int x = 0;
            for (Word word : line) {
                int width = word.getWidth(font);
                if (x <= relativeX && relativeX <= x + width) {
                    return word;
                }
                x += width;
                if (x > relativeX) break;
            }
            y += getPad(line, false);
        }
        return null;
    }
    
    int getPad(ArrayList<Word> line, boolean isAbove) {
        int padUp = 0, padDown = 0;
        for (Word word : line) {
            padUp = Math.max(word.getPaddingAbove(), padUp);
            padDown = Math.max(word.getPaddingBelow(), padDown);
        }
        NORELEASE.fixme("No mutliple returns? Idiocy.");
        if (isAbove) return padUp;
        return padDown;
    }
    
    @Override
    void draw(DocViewer doc, int ox, int oy, String hoveredLink) {
        int y = 0;
        for (ArrayList<Word> line : text) {
            int x = 0;
            y += getPad(line, true);
            for (Word word : line) {
                x += word.draw(doc, ox + x, oy + y, hoveredLink != null && hoveredLink.equals(word.getLink()));
            }
            y += getPad(line, false);
        }
    }
}
