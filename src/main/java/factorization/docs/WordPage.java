package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;

public class WordPage extends AbstractPage {
    ArrayList<ArrayList<Word>> text = new ArrayList();
    Object figure;
    int lineHeight;
    int lineLen = 0;
    FontRenderer font;
    
    WordPage(FontRenderer font) {
        this.font = font;
        lineHeight = font.FONT_HEIGHT + 2;
        nl();
    }
    
    void add(Word word) {
        if (word instanceof TextWord) {
            TextWord tw = (TextWord) word;
            if (tw.text == "\t") {
                word = new TextWord("    ", word.hyperlink);
            } else if (lineLen == 0 && tw.text.trim().isEmpty()) {
                return;
            }
        }
        text.get(text.size() - 1).add(word);
        lineLen += word.getWidth(font);
    }
    
    void nl() {
        if (!text.isEmpty()) {
            ArrayList<Word> last = text.get(text.size() - 1);
            while (!last.isEmpty()) {
                Word l = last.get(last.size() - 1);
                if (l instanceof TextWord) {
                    TextWord tw = (TextWord) l;
                    if (tw.text.trim().length() == 0) {
                        last.remove(last.size() - 1);
                    }
                }
                break;
            }
        }
        text.add(new ArrayList());
        lineLen = 0;
    }
    
    Word click(int relativeX, int relativeY) {
        int y = 0;
        for (ArrayList<Word> line : text) {
            y += lineHeight;
            if (y < relativeY || line.isEmpty()) continue;
            int x = 0;
            if ("    ".equals(line.get(0))) {
                y += 20; //NORELEASE: wat
            }
            for (Word word : line) {
                int width = word.getWidth(font);
                if (x <= relativeX && relativeX <= x + width) return word;
                x += width;
                if (x > relativeX) break;
            }
            break;
        }
        return null;
    }
    
    @Override
    void draw(DocViewer doc, int ox, int oy) {
        int y = 0;
        for (ArrayList<Word> line : text) {
            int x = 0;
            for (Word word : line) {
                x += word.draw(doc, ox + x, oy + y);
            }
            y += lineHeight;
        }
    }
}
