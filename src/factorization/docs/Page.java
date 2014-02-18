package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;

public class Page {
    ArrayList<ArrayList<Object>> text = new ArrayList();
    Object figure;
    int lineHeight;
    int lineLen = 0;
    FontRenderer font;
    
    Page(FontRenderer font) {
        this.font = font;
        lineHeight = font.FONT_HEIGHT + 2;
        nl();
        add(new Word("\t", null));
    }
    
    void add(Word word) {
        if (word.text == "\t") {
            word = new Word("    ", word.hyperlink);
        } else if (lineLen == 0 && word.text.trim().isEmpty()) {
            return;
        }
        text.get(text.size() - 1).add(word);
        lineLen += font.getStringWidth(word.text);
    }
    
    void nl() {
        text.add(new ArrayList());
        lineLen = 0;
    }
    
    Word click(int relativeX, int relativeY) {
        int y = -lineHeight;
        for (ArrayList<Object> line : text) {
            y += lineHeight;
            if (y < relativeY) continue;
            int x = 0;
            if ("    ".equals(line.get(0))) {
                y += 20;
            }
            for (Object obj : line) {
                if (obj instanceof Word) {
                    Word word = (Word) obj;
                    int width = font.getStringWidth(word.text);
                    if (x <= relativeX && relativeX <= x + width) return word;
                    x += width;
                    if (x > relativeX) break;
                } else if (obj instanceof ItemStack) {
                    x += lineHeight;
                }
            }
            break;
        }
        return null;
    }
    
    void draw() {
        int y = 0;
        for (ArrayList<Object> line : text) {
            int x = 0;
            for (Object obj : line) {
                if (obj instanceof Word) {
                    Word word = (Word) obj;
                    font.drawString(word.text, x, y, 0xFFFFFF);
                    x += font.getStringWidth(word.text);
                } else if (obj instanceof ItemStack) {
                    
                }
            }
            y += lineHeight;
        }
    }
}
