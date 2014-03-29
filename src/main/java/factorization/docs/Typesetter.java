package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import com.google.common.base.Strings;

public class Typesetter {
    // Super-awesome typesetter version π², by neptunepink
    
    final FontRenderer font;
    final int pageWidth, pageHeight;
    
    ArrayList<String> topics = new ArrayList();
    
    private ArrayList<AbstractPage> pages = new ArrayList();
    private ArrayList<AbstractPage> afterBuffer = new ArrayList();
    private ArrayList<Word> segmentStart = null;
    
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
    
    void append(final String text) {
        process(text, null, "");
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
                    ArrayList<ItemStack> items = DocumentationModule.lookup(itemName);
                    if (items == null) {
                        error(itemName + " no such item");
                        continue;
                    }
                    // NOTE: This could miss items. Hrm.
                    if (link == null) {
                        emitWord(new ItemWord(items.get(0)));
                    } else {
                        emitWord(new ItemWord(items.get(0), link));
                    }
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
                    String subtext = DocumentationModule.readDocument(name);
                    process(subtext, link, style);
                } else if (cmd.equals("\\lmp")) {
                    process("\\link{lmp}{LMP}", link, style);
                } else if (cmd.equals("\\generate")) {
                    String arg = getParameter(cmd, tokenizer);
                    String args[] = arg.split("/", 2);
                    IDocGenerator gen = DocumentationModule.generators.get(args[0]);
                    if (gen == null) {
                        error("\\generate{" + arg + "}: Not found: " + args[0]);
                        return;
                    }
                    String rest = args.length > 1 ? args[1] : "";
                    gen.process(this, rest);
                } else if (cmd.equals("\\seg")) {
                    ArrayList<ArrayList<Word>> lines = getCurrentPage().text;
                    if (!lines.isEmpty()) {
                        segmentStart = lines.get(lines.size() - 1);
                    }
                } else if (cmd.equals("\\endseg")) {
                    segmentStart = null;
                } else if (cmd.equals("\\topic")) {
                    String topic = getParameter(cmd, tokenizer);
                    if (topic == null) {
                        error("\\topic missing parameter");
                        continue;
                    }
                    append(String.format("\\newpage \\generate{recipes/for/%s}", topic));
                    //topics.add(topic);
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
            WordPage oldPage = page;
            ArrayList<Word> oldSeg = segmentStart;
            page = newPage();
            if (oldSeg != null) {
                int n = oldPage.text.lastIndexOf(oldSeg);
                ArrayList<ArrayList<Word>> got = new ArrayList();
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
