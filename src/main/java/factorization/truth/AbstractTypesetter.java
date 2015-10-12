package factorization.truth;

import com.google.common.base.Strings;
import factorization.truth.api.TruthError;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Locale;

public abstract class AbstractTypesetter implements factorization.truth.api.ITypesetter {
    // Super-awesome typesetter version π², by neptunepink

    final String domain;
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    protected ArrayList<AbstractPage> pages = new ArrayList<AbstractPage>();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList<AbstractPage>();
    protected ArrayList<Word> segmentStart = null;
    
    public AbstractTypesetter(String domain, FontRenderer font, int pageWidth, int pageHeight) {
        this.domain = domain;
        this.font = font;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
    }
    
    @Override
    public void write(String text) throws TruthError {
        write(text, null, "");
    }

    public void writeErrorMessage(String msg) {
        try {
            write(msg.replace("\\", "\\\\ "), null, "" + EnumChatFormatting.RED + EnumChatFormatting.BOLD);
        } catch (TruthError truthError) {
            truthError.printStackTrace();
            // Oh dear.
        }
    }

    @Override
    public void write(final String text, final String link, final String style) throws TruthError {
        if (Strings.isNullOrEmpty(text)) return;
        final Tokenizer tokenizer = new Tokenizer(text);
        
        while (tokenizer.nextToken()) {
            final String token = tokenizer.token;
            if (token.isEmpty()) continue;
            switch (tokenizer.type) {
            default:
                throw new TruthError("Unknown tokentype: " + tokenizer.token);
            case WORD:
                write(style + token, link);
                break;
            case PARAMETER:
                write(token, link, style);
                break;
            case COMMAND:
                final String cmd = token.toLowerCase(Locale.ROOT);
                if (cmd.equals("\\include")) {
                    String name = tokenizer.getParameter("\\include{page name}");
                    if (name == null) {
                        throw new TruthError("No page name specified");
                    }
                    String subtext = DocumentationModule.readDocument(domain, name);
                    write(subtext, link, style);
                } else if (cmd.equals("\\lmp")) {
                    write("\\link{lmp}{LMP}", link, style);
                } else {
                    handleCommand(tokenizer, cmd, link, style);
                }
                break;
            }
        }
    }
    
    protected abstract void handleCommand(Tokenizer tokenizer, String cmd, String link, String style) throws TruthError;
    
    @Override
    public void write(Word w) {
        WordPage page = getCurrentPage();
        int len = w.getWidth(font);
        if (len + page.lineLen > pageWidth) {
            page.nl();
        }
        int total_height = 0;
        for (ArrayList<Word> line : page.text) {
            int[] padding = page.getVerticalPadding(line);
            int paddingTop = padding[0], paddingBottom = padding[1];
            total_height += paddingTop + paddingBottom;
        }
        
        if (total_height + w.getPaddingAbove() + w.getPaddingBelow() > pageHeight) {
            WordPage oldPage = page;
            ArrayList<Word> oldSeg = segmentStart;
            page = newPage();
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
    public void write(String text, String link) {
        write(new TextWord(text, link));
    }
    
    WordPage current;
    
    void emptyBuffer() {
        pages.addAll(afterBuffer);
        afterBuffer.clear();
    }
    
    public WordPage newPage() {
        emptyBuffer();
        current = new WordPage(font);
        pages.add(current);
        segmentStart = null;
        return current;
    }

    public WordPage getCurrentPage() {
        if (current == null) {
            return newPage();
        }
        return current;
    }

    public ArrayList<AbstractPage> getPages() {
        emptyBuffer();
        return pages;
    }
}
