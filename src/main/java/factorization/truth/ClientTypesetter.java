package factorization.truth;

import factorization.truth.api.*;
import factorization.truth.word.ItemWord;
import factorization.truth.word.TextWord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

public class ClientTypesetter extends AbstractTypesetter implements IClientTypesetter {

    final FontRenderer font;
    final int pageWidth, pageHeight;

    public ArrayList<AbstractPage> pages = new ArrayList<AbstractPage>();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList<AbstractPage>();
    public ArrayList<IWord> segmentStart = null;

    public String active_link = null, active_style = "";

    public ClientTypesetter(String domain, FontRenderer font, int pageWidth, int pageHeight) {
        super(domain);
        this.font = font;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
    }

    @Override
    public void write(IWord w) {
        if (w.getLink() == null) {
            if (active_link != null) {
                w.setLink(active_link);
            } else if (w instanceof ItemWord) {
                ((ItemWord) w).setDefaultLink();
            }
        }
        if (!StringUtils.isNullOrEmpty(active_style)) {
            w.setStyle(active_style);
        }

        WordPage page = getCurrentPage();
        int len = w.getWidth(font);
        if (len + page.lineLen > pageWidth) {
            page.nl();
        }
        int total_height = 0;
        for (ArrayList<IWord> line : page.text) {
            int[] padding = page.getVerticalPadding(line);
            int paddingTop = padding[0], paddingBottom = padding[1];
            total_height += paddingTop + paddingBottom;
        }

        if (total_height + w.getPaddingAbove() + w.getWordHeight() > pageHeight) {
            WordPage oldPage = page;
            ArrayList<IWord> oldSeg = segmentStart;
            newPage();
            page = current;
            if (oldSeg != null) {
                int n = oldPage.text.lastIndexOf(oldSeg);
                while (oldPage.text.size() > n) {
                    page.text.add(oldPage.text.remove(n));
                }
            }
        }
        page.add(w);
    }

    @Override
    public void write(String text, String link, String style) throws TruthError {
        String prev_link = active_link;
        String prev_style = active_style;
        if (link != null) {
            active_link = link;
        }
        active_style = active_style + style;
        try {
            write(text);
        } finally {
            active_link = prev_link;
            active_style = prev_style;
        }
    }

    WordPage current;

    void emptyBuffer() {
        pages.addAll(afterBuffer);
        afterBuffer.clear();
    }

    @Override
    public void newPage() {
        emptyBuffer();
        current = new WordPage(font);
        pages.add(current);
        segmentStart = null;
    }

    @Override
    public WordPage getCurrentPage() {
        if (current == null) {
            newPage();
        }
        return current;
    }

    @Override
    public void addPage(AbstractPage page) {
        pages.add(page);
    }

    @Override
    public int getPageWidth() {
        return pageWidth;
    }

    @Override
    public int getPageHeight() {
        return pageHeight;
    }

    @Override
    public ArrayList<AbstractPage> getPages() {
        emptyBuffer();
        return pages;
    }

    @Override
    public void writeErrorMessage(String msg) {
        try {
            write(msg.replace("\\", "\\\\ "), null, "" + EnumChatFormatting.RED);
        } catch (TruthError truthError) {
            truthError.printStackTrace();
            // Oh dear.
        }
    }


    @Override
    protected void runWord(String word) {
        write(new TextWord(word));
    }

    @Override
    protected void runCommand(ITypesetCommand cmd, ITokenizer tokenizer) throws TruthError {
        cmd.callClient(this, tokenizer);
    }

    @Override
    public void write(ItemStack stack) {
        write(new ItemWord(stack));
    }

    @Override
    public void write(ItemStack[] stacks) {
        write(new ItemWord(stacks));
    }

    @Override
    public void write(Collection<ItemStack> stacks) {
        write(new ItemWord(stacks));
    }
}
