package factorization.docs;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

public class Typesetter {
    // Super-awesome typesetter version π^π, by neptunepink
    
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    ArrayList<Page> pages = new ArrayList();
    
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
            if (Character.isAlphabetic(c) || c == ':' || c == '-') {
                continue;
            }
            String s = text.substring(scan, i);
            scan = i;
            return s;
        }
        String s =  text.substring(scan);
        scan = text.length();
        return s;
    }
    
    ArrayList<Page> process(final String text) {
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
                    getCurrentPage().add(new Word("\t", null));
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
                } else if (word.equalsIgnoreCase("\\r")) {
                    style = "";
                } else if (word.equalsIgnoreCase("\\-")) {
                    Page page = getCurrentPage();
                    page.nl();
                    page.add(new Word("        -", null));
                } else if (word.equalsIgnoreCase("\\:")) {
                    Page page = getCurrentPage();
                    page.nl();
                    page.add(new Word("  -", null));
                } else if (word.equalsIgnoreCase("\\t")) {
                    emit("\t", null);
                } else if (word.equalsIgnoreCase("\\p")) {
                    Page page = getCurrentPage();
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
                } else if (word.startsWith("\\icon:")) {
                    String icon = word.substring("\\icon:".length());
                    
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
    
    Word emit(String word, String link) {
        Word w = new Word(word, link);
        Page page = getCurrentPage();
        int len = font.getStringWidth(word);
        if (len + page.lineLen > pageWidth) {
            page.nl();
        }
        if ((2 + page.text.size())*page.lineHeight > pageHeight) {
            page = newPage();
            //page.add(new Word("\t", link));
        }
        page.add(w);
        return w;
    }
    
    Page current;
    
    Page newPage() {
        current = new Page(font);
        pages.add(current);
        return current;
    }
    
    Page getCurrentPage() {
        return current;
    }
}
