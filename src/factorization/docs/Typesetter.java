package factorization.docs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import com.google.common.base.Splitter;

import factorization.shared.Core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.Resource;
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
    
    ArrayList<Page> process(final String text) {
        startScanning(text);
        
        Object link = null;
        int contigLines = 0;
        int contigSpaces = 0;
        boolean ignoreNextSpace = false;
        
        String word;
        while ((word = readToken()) != null) {
            if (word.equals("\n")) {
                contigLines++;
            } else if (word.trim().isEmpty()) {
                if (!ignoreNextSpace) {
                    contigSpaces++;
                }
            } else if (word.startsWith("\\")) {
                if (word.startsWith("\\link:")) {
                    link = word.substring("\\link:".length());
                } else if (word.startsWith("\\endlink")) {
                    link = null;
                } else if (word.startsWith("\\newpage")) {
                    newPage();
                } else if (word.startsWith("\\icon:")) {
                    String icon = word.substring("\\icon:".length());
                    
                } else {
                    emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + word, null);
                }
                ignoreNextSpace = true;
            } else {
                if (contigLines > 0) {
                    getCurrentPage().nl();
                    getCurrentPage().add(new Word("\t", null));
                    contigLines = 0;
                    contigSpaces = 0;
                }
                if (contigSpaces > 0) {
                    emit(" ", null);
                    contigSpaces = 0;
                }
                emit(word, link);
            }
            ignoreNextSpace = false;
        }
        return pages;
    }
    
    int width(String word) {
        return font.getStringWidth(word);
    }
    
    Word emit(String word, Object link) {
        Word w = new Word(word, link);
        Page page = getCurrentPage();
        int len = font.getStringWidth(word);
        if (len + page.lineLen > pageWidth) {
            if ((1 + page.text.size())*page.lineHeight > pageHeight) {
                page = newPage();
            } else {
                page.nl();
            }
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
