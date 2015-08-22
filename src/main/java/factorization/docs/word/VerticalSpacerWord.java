package factorization.docs.word;

import factorization.docs.DocViewer;
import factorization.docs.word.Word;
import net.minecraft.client.gui.FontRenderer;

public class VerticalSpacerWord extends Word {
    final int vertSize;
    
    public VerticalSpacerWord(int vertSize) {
        super(null);
        this.vertSize = vertSize;
    }

    @Override
    public int getWidth(FontRenderer font) {
        return 0;
    }

    @Override
    public int draw(DocViewer doc, int x, int y, boolean hover) {
        return 0;
    }
    
    @Override
    public int getPaddingAbove() {
        return 0;
    }
    
    @Override
    public int getPaddingBelow() {
        return vertSize;
    }

}
