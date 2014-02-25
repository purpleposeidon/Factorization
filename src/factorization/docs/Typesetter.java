package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class Typesetter {
    // Super-awesome typesetter version π^π, by neptunepink
    
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    private ArrayList<AbstractPage> pages = new ArrayList();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList();
    
    public Typesetter(FontRenderer font, int pageWidth, int pageHeight, String text) {
        this.font = font;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        newPage();
        emit("\t", null);
        process(text);
    }
    
    int scan = 0;
    String text;
    void startScanning(String text) {
        this.text = text;
        scan = 0;
    }
    
    String readToken() {
        if (scan >= text.length()) return null;
        char c = text.charAt(scan);
        if (Character.isWhitespace(c)) {
            return consumeSingle();
        }
        if (c == '\\') {
            return consumeCommand();
        }
        return consumeWord();
    }
    
    String consumeSingle() {
        scan++;
        return text.substring(scan - 1, scan);
    }
    
    String consumeWord() {
        int i;
        for (i = scan + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '\\') {
                String s = text.substring(scan, i);
                scan = i;
                return s;
            }
        }
        String s =  text.substring(scan);
        scan = text.length();
        return s;
    }
    
    String consumeCommand() {
        int i;
        for (i = scan + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isAlphabetic(c) || Character.isDigit(c) || c == ':' || c == '-' || c == '_' || c == '.' || c == '/' || c == '#') {
                continue;
            }
            String s = text.substring(scan, i);
            scan = i;
            if (i < text.length() && text.charAt(i) == ' ') {
                scan++;
            }
            return s;
        }
        String s = text.substring(scan);
        scan = text.length();
        return s;
    }
    
    String scanFor(String sentinel) {
        int end = text.indexOf(sentinel, scan);
        if (end == -1) {
            return "";
        }
        String s = text.substring(scan, end);
        scan = end + sentinel.length();
        return s;
    }
    
    ArrayList<AbstractPage> process(final String text) {
        startScanning(text);
        
        String link = null;
        int contigLines = 0;
        int contigSpaces = 0;
        
        String word;
        String style = "";
        while ((word = readToken()) != null) {
            if (word.equals("\n")) {
                contigLines++;
                if (contigLines == 2) {
                    getCurrentPage().nl();
                    getCurrentPage().add(new TextWord("\t", null));
                    contigSpaces = 1;
                }
                contigSpaces++;
                if (contigSpaces == 1) {
                    emit(style + " ", link);
                }
            } else if (word.trim().isEmpty()) {
                contigSpaces++;
                if (contigSpaces == 1) {
                    emit(style + " ", link);
                }
            } else if (word.startsWith("\\")) {
                if (word.startsWith("\\link:")) {
                    link = word.substring("\\link:".length());
                } else if (word.equalsIgnoreCase("\\endlink")) {
                    link = null;
                } else if (word.equalsIgnoreCase("\\newpage")) {
                    newPage();
                    emit("\t", null);
                } else if (word.equalsIgnoreCase("\\nl")) {
                    getCurrentPage().nl();
                } else if (word.equalsIgnoreCase("\\toc")) {
                    getCurrentPage().lineHeight += 4;
                } else if (word.equalsIgnoreCase("\\b")) {
                    style += EnumChatFormatting.BOLD;
                } else if (word.equalsIgnoreCase("\\u")) {
                    style += EnumChatFormatting.UNDERLINE;
                } else if (word.equalsIgnoreCase("\\i")) {
                    style += EnumChatFormatting.ITALIC;
                } else if (word.equalsIgnoreCase("\\obf")) {
                    style += EnumChatFormatting.OBFUSCATED;
                } else if (word.equalsIgnoreCase("\\r")) {
                    style = "";
                } else if (word.equalsIgnoreCase("\\-")) {
                    WordPage page = getCurrentPage();
                    page.nl();
                    page.add(new TextWord("        -", null));
                } else if (word.equalsIgnoreCase("\\:")) {
                    WordPage page = getCurrentPage();
                    page.nl();
                    page.add(new TextWord("  -", null));
                } else if (word.equalsIgnoreCase("\\t")) {
                    emit("\t", null);
                } else if (word.equalsIgnoreCase("\\p")) {
                    WordPage page = getCurrentPage();
                    page.nl();
                    emit("\t", null);
                } else if (word.equalsIgnoreCase("\\title")) {
                    String s = "" + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE;
                    String t = "";
                    while ((word = readToken()) != null) {
                        if (word.equals("\n")) break;
                        if (t.length() == 0 && word.trim().isEmpty()) continue;
                        t += word;
                    }
                    String v = "";
                    int twidth = (pageWidth - font.getStringWidth(t))/2/font.getStringWidth(" ");
                    for (int i = 1; i < twidth; i++) {
                        v += " ";
                    }
                    getCurrentPage().text.get(0).clear();
                    emit(v + s + t, link);
                    getCurrentPage().nl();
                    emit("\t", null);
                } else if (word.startsWith("\\#")) {
                    String itemName = word.substring("\\#".length());
                    ItemStack is = DocumentationModule.lookup(itemName);
                    if (is == null) {
                        emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + itemName, null);
                        continue;
                    }
                    emitWord(new ItemWord(is, link));
                    
                } else if (word.equalsIgnoreCase("\\figure")) {
                    DocWorld figure = null;
                    try {
                        String fig = scanFor("\\endfig").trim().replace(" ", "").replace("\n", "").replace("\r", "");
                        figure = DocumentationModule.loadWorld(fig);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "[figure is corrupt; see console]", null);
                        continue;
                    }
                    if (figure == null) {
                        emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "[figure failed to load]", null);
                        continue;
                    }
                    WordPage wp = getCurrentPage();
                    if (wp.text.size() > 1) {
                        afterBuffer.add(new FigurePage(figure));
                    } else {
                        pages.add(pages.size() - 1, new FigurePage(figure));
                    }
                } else if (word.equalsIgnoreCase("\\\\")) {
                    emit("\\", link);
                } else {
                    emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + word, null);
                }
            } else {
                contigLines = 0;
                contigSpaces = 0;
                emit(style + word, link);
            }
        }
        return pages;
    }
    
    int width(String word) {
        return font.getStringWidth(word);
    }
    
    void emitWord(Word w) {
        WordPage page = getCurrentPage();
        int len = w.getWidth(font);
        if (len + page.lineLen > pageWidth) {
            page.nl();
        }
        if ((2 + page.text.size())*page.lineHeight > pageHeight) {
            page = newPage();
            //page.add(new Word("\t", link));
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
        return current;
    }
    
    WordPage getCurrentPage() {
        return current;
    }
    
    ArrayList<AbstractPage> getPages() {
        emptyBuffer();
        return pages;
    }
}
