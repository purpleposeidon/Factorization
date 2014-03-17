package factorization.docs;

import java.io.InputStream;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

public class Typesetter {
    // Super-awesome typesetter version π², by neptunepink
    
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    private ArrayList<AbstractPage> pages = new ArrayList();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList();
    
    public Typesetter(FontRenderer font, int pageWidth, int pageHeight, String text) {
        this.font = font;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        try {
            process(text, null, "");
        } catch (Throwable t) {
            error("Failed to load document.");
            t.printStackTrace();
        }
    }
    
    void error(String msg) {
        emit("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + msg, null);
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
    
    void process(final String text, final String link, final String style) {
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
                if (cmd.equals("\\p")) {
                    WordPage p = getCurrentPage();
                    p.nl();
                    if (getCurrentPage() == p) {
                        p.nl();
                    }
                } else if (cmd.equals("\\nl")) {
                    getCurrentPage().nl();
                } else if (cmd.equals("\\-")) {
                    getCurrentPage().nl();
                    emit(" - ", null);
                } else if (cmd.equals("\\") || cmd.equals("\\ ")) {
                    emit(style + " ", link);
                } else if (cmd.equalsIgnoreCase("\\\\")) {
                    emit("\\", link);
                } else if (cmd.equals("\\newpage")) {
                    newPage();
                } else if (cmd.equals("\\leftpage")) {
                    int need = 1 + (pages.size() % 2);
                    for (int i = 0; i < need; i++) {
                        newPage();
                    }
                } else if (cmd.equals("\\b") || cmd.equals("\\i") || cmd.equals("\\u") || cmd.equals("\\obf")) {
                    char mode = cmd.charAt(1);
                    String content = getParameter(cmd, tokenizer);
                    if (content == null) continue;
                    EnumChatFormatting newStyle;
                    if (mode == 'b') {
                        newStyle = EnumChatFormatting.BOLD;
                    } else if (mode == 'i') {
                        newStyle = EnumChatFormatting.ITALIC;
                    } else if (mode == 'u') {
                        newStyle = EnumChatFormatting.UNDERLINE;
                    } else if (mode == 'o') {
                        newStyle = EnumChatFormatting.OBFUSCATED;
                    } else {
                        error("Unknown style: " + cmd);
                        continue;
                    }
                    process(content, link, style + newStyle);
                } else if (cmd.equals("\\title")) {
                    String val = getParameter(cmd, tokenizer);
                    if (val == null) {
                        error("No content");
                        continue;
                    }
                    process(val, link, style + EnumChatFormatting.UNDERLINE + EnumChatFormatting.BOLD);
                } else if (cmd.equals("\\h1")) {
                    String val = getParameter(cmd, tokenizer);
                    if (val == null) {
                        error("No content");
                        continue;
                    }
                    getCurrentPage().nl();
                    process(val, link, style + EnumChatFormatting.UNDERLINE);
                } else if (cmd.equals("\\link") || cmd.equals("\\index")) {
                    String newLink = getParameter(cmd, tokenizer);
                    if (newLink == null) {
                        error("missing destination parameter");
                        continue;
                    }
                    String content = getParameter(cmd, tokenizer);
                    if (content == null) {
                        error("missing content parameter");
                        continue;
                    }
                    process(content, newLink, style);
                    if (cmd.equals("\\index")) {
                        getCurrentPage().nl();
                    }
                } else if (cmd.equals("\\#")) {
                    String itemName = getParameter(cmd, tokenizer);
                    if (itemName == null) {
                        error("No item specified");
                        continue;
                    }
                    ItemStack is = DocumentationModule.lookup(itemName);
                    if (is == null) {
                        error(itemName);
                        continue;
                    }
                    emitWord(new ItemWord(is, link));
                } else if (cmd.equals("\\img")) {
                    String imgName = getParameter(cmd, tokenizer);
                    if (imgName == null) {
                        error("No img specified");
                        continue;
                    }
                    ResourceLocation rl = new ResourceLocation(imgName);
                    Minecraft mc = Minecraft.getMinecraft();
                    try {
                        IResource r = mc.getResourceManager().getResource(rl);
                        if (r == null) {
                            error("Not found: " + imgName);
                            continue;
                        }
                    } catch (Throwable e) {
                        error(e.getMessage());
                        e.printStackTrace();
                        continue;
                    }
                    emitWord(new ImgWord(rl, link));
                } else if (cmd.equals("\\imgx")) {
                    int width = Integer.parseInt(getParameter(cmd, tokenizer));
                    int height = Integer.parseInt(getParameter(cmd, tokenizer));
                    String imgName = getParameter(cmd, tokenizer);
                    if (imgName == null) {
                        error("No img specified");
                        continue;
                    }
                    ResourceLocation rl = new ResourceLocation(imgName);
                    Minecraft mc = Minecraft.getMinecraft();
                    try {
                        IResource r = mc.getResourceManager().getResource(rl);
                        if (r == null) {
                            error("Not found: " + imgName);
                            continue;
                        }
                    } catch (Throwable e) {
                        error(e.getMessage());
                        e.printStackTrace();
                        continue;
                    }
                    emitWord(new ImgWord(rl, link, width, height));
                } else if (cmd.equals("\\figure")) {
                    DocWorld figure = null;
                    try {
                        String fig = getParameter(cmd, tokenizer);
                        if (fig == null) continue;
                        figure = DocumentationModule.loadWorld(fig);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        error("figure is corrupt; see console");
                        continue;
                    }
                    if (figure == null) {
                        error("figure failed to load");
                        continue;
                    }
                    pages.add(new FigurePage(figure));
                } else if (cmd.equals("\\include")) {
                    String name = getParameter(cmd, tokenizer);
                    if (name == null) {
                        error("No page name specified");
                        continue;
                    }
                    InputStream is = DocumentationModule.getDocumentResource(name);
                    try {
                        if (is == null) {
                            error("Not found: " + name);
                            continue;
                        }
                        String subtext = DocumentationModule.readContents(name, is);
                        process(subtext, link, style);
                    } finally {
                        Closeables.closeQuietly(is);
                    }
                } else if (cmd.equals("\\lmp")) {
                    process("\\link{lmp}{LMP}", link, style);
                } else {
                    error(cmd);
                }
                break;
            }
        }
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
