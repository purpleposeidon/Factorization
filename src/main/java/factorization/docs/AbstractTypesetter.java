package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

import com.google.common.base.Strings;

import factorization.shared.NORELEASE;

public abstract class AbstractTypesetter {
    // Super-awesome typesetter version π², by neptunepink
    
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    ArrayList<String> topics = new ArrayList();
    
    protected ArrayList<AbstractPage> pages = new ArrayList();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList();
    protected ArrayList<Word> segmentStart = null;
    
    public AbstractTypesetter(FontRenderer font, int pageWidth, int pageHeight) {
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
    
    void error(String msg) {
        process(msg.replace("\\", "\\\\ "), null, "" + EnumChatFormatting.RED + EnumChatFormatting.BOLD);
    }

    String getParameter(final String cmdName, final Tokenizer tokenizer) {
        if (!tokenizer.nextToken()) {
            error("EOF looking for parameter for " + cmdName);
            return null;
        }
        if (tokenizer.type != tokenizer.type.PARAMETER) {
            error("Expected parameter for " + cmdName);
            return null;
        }
        return tokenizer.token;
    }
    
    void append(final String text) {
        process(text, null, "");
    }
    
    protected void process(final String text, final String link, final String style) {
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
                final String cmd = token.toLowerCase();
                if (cmd.equals("\\include")) {
                    String name = getParameter(cmd, tokenizer);
                    if (name == null) {
                        error("No page name specified");
                        return;
                    }
                    String subtext = DocumentationModule.readDocument(name);
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
    
    int width(String word) {
        return font.getStringWidth(word);
    }
    
    void emitWord(Word w) {
        WordPage page = getCurrentPage();
        int len = w.getWidth(font);
        if (len + page.lineLen > pageWidth) {
            page.nl();
        }
        int total_height = 0;
        for (ArrayList<Word> line : page.text) {
            total_height += page.getPad(line, true) + page.getPad(line, false);
        }
        
        if (total_height > pageHeight) {
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
    
    TextWord emit(String text, String link) {
        TextWord w = new TextWord(text, link);
        emitWord(w);
        return w;
    }
    
    WordPage current;
    
    void emptyBuffer() {
        pages.addAll(afterBuffer);
        afterBuffer.clear();
    }
    
    WordPage newPage() {
        emptyBuffer();
        current = new WordPage(font);
        pages.add(current);
        segmentStart = null;
        return current;
    }
    
    WordPage getCurrentPage() {
        if (current == null) {
            return newPage();
        }
        return current;
    }
    
    ArrayList<AbstractPage> getPages() {
        emptyBuffer();
        return pages;
    }
}
