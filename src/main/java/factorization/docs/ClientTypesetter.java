package factorization.docs;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public class ClientTypesetter extends AbstractTypesetter {

    public ClientTypesetter(FontRenderer font, int pageWidth, int pageHeight) {
        super(font, pageWidth, pageHeight);
    }

    @Override
    protected void handleCommand(Tokenizer tokenizer, final String cmd, final String link, final String style) {
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
            if (content == null) return;
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
                return;
            }
            process(content, link, style + newStyle);
        } else if (cmd.equals("\\title")) {
            String val = getParameter(cmd, tokenizer);
            if (val == null) {
                error("No content");
                return;
            }
            process(val, link, style + EnumChatFormatting.UNDERLINE + EnumChatFormatting.BOLD);
        } else if (cmd.equals("\\h1")) {
            String val = getParameter(cmd, tokenizer);
            if (val == null) {
                error("No content");
                return;
            }
            process(val, link, style + EnumChatFormatting.BOLD);
            getCurrentPage().nl();
        } else if (cmd.equals("\\link") || cmd.equals("\\index")) {
            String newLink = getParameter(cmd, tokenizer);
            if (newLink == null) {
                error("missing destination parameter");
                return;
            }
            String content = getParameter(cmd, tokenizer);
            if (content == null) {
                error("missing content parameter");
                return;
            }
            process(content, newLink, style);
            if (cmd.equals("\\index")) {
                getCurrentPage().nl();
            }
        } else if (cmd.equals("\\#")) {
            String itemName = getParameter(cmd, tokenizer);
            if (itemName == null) {
                error("No item specified");
                return;
            }
            ArrayList<ItemStack> items = DocumentationModule.lookup(itemName);
            if (items == null) {
                error(itemName + " no such item");
                return;
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
                return;
            }
            ResourceLocation rl = new ResourceLocation(imgName);
            Minecraft mc = Minecraft.getMinecraft();
            try {
                IResource r = mc.getResourceManager().getResource(rl);
                if (r == null) {
                    error("Not found: " + imgName);
                    return;
                }
            } catch (Throwable e) {
                error(e.getMessage());
                e.printStackTrace();
                return;
            }
            emitWord(new ImgWord(rl, link));
        } else if (cmd.equals("\\imgx")) {
            int width = Integer.parseInt(getParameter(cmd, tokenizer));
            int height = Integer.parseInt(getParameter(cmd, tokenizer));
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            ResourceLocation rl = new ResourceLocation(imgName);
            Minecraft mc = Minecraft.getMinecraft();
            try {
                IResource r = mc.getResourceManager().getResource(rl);
                if (r == null) {
                    error("Not found: " + imgName);
                    return;
                }
            } catch (Throwable e) {
                error(e.getMessage());
                e.printStackTrace();
                return;
            }
            emitWord(new ImgWord(rl, link, width, height));
        } else if (cmd.equals("\\figure")) {
            DocWorld figure = null;
            try {
                String fig = getParameter(cmd, tokenizer);
                if (fig == null) return;
                figure = DocumentationModule.loadWorld(fig);
            } catch (Throwable t) {
                t.printStackTrace();
                error("figure is corrupt; see console");
                return;
            }
            if (figure == null) {
                error("figure failed to load");
                return;
            }
            pages.add(new FigurePage(figure));
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
                return;
            }
            append(String.format("\\newpage \\generate{recipes/for/%s}", topic));
            //topics.add(topic);
        } else {
            error("Unknown command: ");
            emit(cmd, null);
        }
    }

}
