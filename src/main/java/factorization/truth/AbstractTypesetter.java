package factorization.truth;

import com.google.common.base.Strings;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Locale;

public abstract class AbstractTypesetter {
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
    
    public void processText(String text) {
        try {
            process(text, null, "");
        } catch (Throwable t) {
            error("Failed to load document.");
            t.printStackTrace();
        }
    }
    
    public void error(String msg) {
        process(msg.replace("\\", "\\\\ "), null, "" + EnumChatFormatting.RED + EnumChatFormatting.BOLD);
    }

    public String getParameter(final String cmdName, final Tokenizer tokenizer) {
        if (!tokenizer.nextToken()) {
            error("EOF looking for parameter for " + cmdName);
            return null;
        }
        if (tokenizer.type != Tokenizer.TokenType.PARAMETER) {
            error("Expected parameter for " + cmdName);
            return null;
        }
        return tokenizer.token;
    }

    public String getOptionalParameter(final Tokenizer tokenizer) {
        if (!tokenizer.nextToken()) return null;
        if (tokenizer.type != Tokenizer.TokenType.PARAMETER) {
            tokenizer.prevToken();
            return null;
        }
        return tokenizer.token;
    }
    
    public void append(final String text) {
        process(text, null, "");
    }
    
    public void process(final String text, final String link, final String style) {
        if (Strings.isNullOrEmpty(text)) return;
        final Tokenizer tokenizer = new Tokenizer(text);
        
        while (tokenizer.nextToken()) {
            final String token = tokenizer.token;
            if (token.isEmpty()) {
                break;
            }
            switch (tokenizer.type) {
            default:
                error(tokenizer.token);
                break;
            case WORD:
                emit(style + token, link);
                break;
            case PARAMETER:
                process(token, link, style);
                break;
            case COMMAND:
                final String cmd = token.toLowerCase(Locale.ROOT);
                if (cmd.equals("\\include")) {
                    String name = getParameter(cmd, tokenizer);
                    if (name == null) {
                        error("No page name specified");
                        return;
                    }
                    String subtext = DocumentationModule.readDocument(domain, name);
                    process(subtext, link, style);
                } else if (cmd.equals("\\lmp")) {
                    process("\\link{lmp}{LMP}", link, style);
                } else {
                    handleCommand(tokenizer, cmd, link, style);
                }
                break;
            }
        }
    }
    
    protected abstract void handleCommand(Tokenizer tokenizer, String cmd, String link, String style);
    
    public void emitWord(Word w) {
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
    
    public TextWord emit(String text, String link) {
        TextWord w = new TextWord(text, link);
        emitWord(w);
        return w;
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
