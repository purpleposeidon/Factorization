package factorization.docs;

import java.util.ArrayList;

import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.Loader;

public class ClientTypesetter extends AbstractTypesetter {

    public ClientTypesetter(String domain, FontRenderer font, int pageWidth, int pageHeight) {
        super(domain, font, pageWidth, pageHeight - WordPage.TEXT_HEIGHT * 2);
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
        } else if (cmd.equals("\\#") || cmd.equals("\\##")) {
            String itemName = getParameter(cmd, tokenizer);
            if (itemName == null) {
                error("No item specified");
                return;
            }
            ArrayList<ItemStack> items;
            if (cmd.equals("\\#")) {
                items = DocumentationModule.lookup(itemName);
            } else {
                String stackSizeS = getOptionalParameter(tokenizer);
                if (stackSizeS == null) stackSizeS = "1";
                String dmgS = getOptionalParameter(tokenizer);
                if (dmgS == null) dmgS = "0";
                int dmg = Integer.parseInt(dmgS);
                int stackSize = Integer.parseInt(stackSizeS);
                items = new ArrayList<ItemStack>();
                Block b = DataUtil.getBlockFromName(itemName);
                Item it = DataUtil.getItemFromName(itemName);
                if (b != null) {
                    items.add(new ItemStack(b, stackSize, dmg));
                } else if (it != null) {
                    items.add(new ItemStack(it, stackSize, dmg));
                } else {
                    error("Could not find block or item: " + itemName);
                }
            }
            if (items == null || items.isEmpty()) {
                error(itemName + " no such item");
                return;
            }
            if (items.size() == 1) {
                if (link == null) {
                    emitWord(new ItemWord(items.get(0)));
                } else {
                    emitWord(new ItemWord(items.get(0), link));
                }
            } else {
                ItemStack[] theItems = items.toArray(new ItemStack[items.size()]);
                if (link == null) {
                    emitWord(new ItemWord(theItems));
                } else {
                    emitWord(new ItemWord(theItems, link));
                }
            }
        } else if (cmd.equals("\\img") || cmd.equals("\\img%")) {
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            double scale = 1;
            if (cmd.equals("\\img%")) {
                String scaleStr = getParameter(cmd, tokenizer);
                if (scaleStr == null) {
                    error("No scale specified");
                    return;
                }
                scale = Double.parseDouble(scaleStr); // exception's fine
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
            ImgWord word = new ImgWord(rl, link);
            word.scale(scale);
            word.fitToPage(pageWidth, pageHeight);
            emitWord(word);
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
        } else if (cmd.equals("\\checkmods")) {
            String mode = getParameter(cmd, tokenizer); // all some none
            if (mode == null) {
                error("\\checkmods missing parameter");
                return;
            }
            String modList = getParameter(cmd, tokenizer); //craftguide NotEnoughItems
            if (modList == null) {
                error("\\checkmods missing parameter");
                return;
            }
            String content = getParameter(cmd, tokenizer);
            if (content == null) {
                error("\\checkmods missing parameter");
                return;
            }
            int count = 0;
            String[] mods = modList.split(" ");
            for (String modId : mods) {
                if (Loader.isModLoaded(modId)) {
                    count++;
                }
            }
            boolean good = false;
            if (mode.equalsIgnoreCase("all")) {
                good = count == mods.length;
            } else if (mode.equalsIgnoreCase("none")) {
                good = count == 0;
            } else if (mode.equalsIgnoreCase("some")) {
                good = count > 1;
            } else {
                error("\\checkmods first parameter must be 'all', 'none', or 'some', not " + mode);
                return;
            }
            String other = getParameter(cmd, tokenizer);
            if (good) {
                process(content, link, style);
            } else if (other != null) {
                process(other, link, style);
            }
        } else if (cmd.equals("\\vpad")) {
            String height_ = getParameter(cmd, tokenizer);
            if (height_ == null) {
                error("\\vpad missing parameter");
                return;
            }
            int height = Integer.parseInt(height_);
            emitWord(new VerticalSpacerWord(height));
        } else if (cmd.equals("\\url")) {
            String uriLink = getParameter(cmd, tokenizer);
            if (uriLink == null) {
                error("\\url missing parameter: uriLink");
                return;
            }
            String content = getParameter(cmd, tokenizer);
            if (content == null) {
                error("\\url missing parameter: content");
                return;
            }
            emitWord(new URIWord(content, uriLink));
        } else if (cmd.equals("\\ifhtml")) {
            String trueBranch = getParameter(cmd, tokenizer);
            String falseBranch = getParameter(cmd, tokenizer);
            process(falseBranch, link, style);
        } else {
            error("Unknown command: ");
            emit(cmd, null);
        }
    }

}
