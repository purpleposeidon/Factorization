package factorization.truth;

import cpw.mods.fml.common.Loader;
import factorization.truth.api.DocReg;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.TruthError;
import factorization.truth.word.*;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

public class ClientTypesetter extends AbstractTypesetter {

    public ClientTypesetter(String domain, FontRenderer font, int pageWidth, int pageHeight) {
        super(domain, font, pageWidth, pageHeight - WordPage.TEXT_HEIGHT * 2);
    }

    @Override
    protected void handleCommand(Tokenizer tokenizer, final String cmd, final String link, final String style) throws TruthError {
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
            write(" - ", null);
        } else if (cmd.equals("\\") || cmd.equals("\\ ")) {
            write(style + " ", link);
        } else if (cmd.equalsIgnoreCase("\\\\")) {
            write("\\", link);
        } else if (cmd.equals("\\newpage")) {
            newPage();
        } else if (cmd.equals("\\leftpage")) {
            int need = 1 + (pages.size() % 2);
            for (int i = 0; i < need; i++) {
                newPage();
            }
        } else if (cmd.equals("\\b") || cmd.equals("\\i") || cmd.equals("\\u") || cmd.equals("\\obf")) {
            char mode = cmd.charAt(1);
            String content = tokenizer.getParameter("paramter content");
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
                throw new TruthError("Unknown style: " + cmd);
            }
            write(content, link, style + newStyle);
        } else if (cmd.equals("\\title")) {
            String val = tokenizer.getParameter("No content");
            write(val, link, style + EnumChatFormatting.UNDERLINE + EnumChatFormatting.BOLD);
        } else if (cmd.equals("\\h1")) {
            String val = tokenizer.getParameter("No content");
            write(val, link, style + EnumChatFormatting.BOLD);
            getCurrentPage().nl();
        } else if (cmd.equals("\\link") || cmd.equals("\\index")) {
            String newLink = tokenizer.getParameter("missing destination parameter");
            String content = tokenizer.getParameter("missing content parameter");
            write(content, newLink, style);
            if (cmd.equals("\\index")) {
                getCurrentPage().nl();
            }
        } else if (cmd.equals("\\#") || cmd.equals("\\##")) {
            String itemName = tokenizer.getParameter("No item specified");
            ArrayList<ItemStack> items;
            if (cmd.equals("\\#")) {
                items = DocumentationModule.lookup(itemName);
            } else {
                String stackSizeS = tokenizer.getOptionalParameter();
                if (stackSizeS == null) stackSizeS = "1";
                String dmgS = tokenizer.getOptionalParameter();
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
                    throw new TruthError("Could not find block or item: " + itemName);
                }
            }
            if (items == null || items.isEmpty()) {
                throw new TruthError(itemName + " no such item");
            }
            if (items.size() == 1) {
                if (link == null) {
                    write(new ItemWord(items.get(0)));
                } else {
                    write(new ItemWord(items.get(0), link));
                }
            } else {
                ItemStack[] theItems = items.toArray(new ItemStack[items.size()]);
                if (link == null) {
                    write(new ItemWord(theItems));
                } else {
                    write(new ItemWord(theItems, link));
                }
            }
        } else if (cmd.equals("\\img") || cmd.equals("\\img%")) {
            String imgName = tokenizer.getParameter("No img specified");
            double scale = 1;
            if (cmd.equals("\\img%")) {
                String scaleStr = tokenizer.getParameter("No scale specified");
                scale = Double.parseDouble(scaleStr); // exception's fine
            }
            ResourceLocation rl = new ResourceLocation(imgName);
            Minecraft mc = Minecraft.getMinecraft();
            try {
                IResource r = mc.getResourceManager().getResource(rl);
                if (r == null) {
                    throw new TruthError("Not found: " + imgName);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new TruthError(e.getMessage());
            }
            ImgWord word = new ImgWord(rl, link);
            word.scale(scale);
            word.fitToPage(pageWidth, pageHeight);
            write(word);
        } else if (cmd.equals("\\imgx")) {
            int width = Integer.parseInt(tokenizer.getParameter("image width"));
            int height = Integer.parseInt(tokenizer.getParameter("image height"));
            String imgName = tokenizer.getParameter("No img specified");
            ResourceLocation rl = new ResourceLocation(imgName);
            Minecraft mc = Minecraft.getMinecraft();
            try {
                IResource r = mc.getResourceManager().getResource(rl);
                if (r == null) {
                    throw new TruthError("Not found: " + imgName);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new TruthError(e.getMessage());
            }
            write(new ImgWord(rl, link, width, height));
        } else if (cmd.equals("\\figure")) {
            DocWorld figure = null;
            try {
                String fig = tokenizer.getParameter("Encoded document figure");
                figure = DocumentationModule.loadWorld(fig);
            } catch (Throwable t) {
                if (t instanceof TruthError) throw (TruthError) t;
                t.printStackTrace();
                throw new TruthError("figure is corrupt; see console");
            }
            if (figure == null) {
                throw new TruthError("figure failed to load");
            }
            pages.add(new FigurePage(figure));
        } else if (cmd.equals("\\generate")) {
            String arg = tokenizer.getParameter("\\generate path");
            String args[] = arg.split("/", 2);
            IDocGenerator gen = DocReg.generators.get(args[0]);
            if (gen == null) {
                throw new TruthError("\\generate{" + arg + "}: Not found: " + args[0]);
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
            String topic = tokenizer.getParameter("\\topic missing parameter");
            write(String.format("\\newpage \\generate{recipes/for/%s}", topic));
            //topics.add(topic);
        } else if (cmd.equals("\\checkmods")) {
            String mode = tokenizer.getParameter("\\checkmods mod mode: all|none|any"); // all some none
            String modList = tokenizer.getParameter("\\checkmods list of mods"); //craftguide NotEnoughItems
            String content = tokenizer.getParameter("\\checkmods when mods installed");
            String other = tokenizer.getParameter("\\checkmods when mods not installed");
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
            } else if (mode.equalsIgnoreCase("any")) {
                good = count > 1;
            } else {
                throw new TruthError("\\checkmods first parameter must be 'all', 'none', or 'any', not '" + mode + "'");
            }
            if (good) {
                write(content, link, style);
            } else if (other != null) {
                write(other, link, style);
            }
        } else if (cmd.equals("\\vpad")) {
            int height = Integer.parseInt(tokenizer.getParameter("\\vpad height"));
            write(new VerticalSpacerWord(height));
        } else if (cmd.equals("\\url")) {
            String uriLink = tokenizer.getParameter("\\url missing parameter: uriLink");
            String content = tokenizer.getParameter("\\url missing parameter: content");
            write(new URIWord(content, uriLink));
        } else if (cmd.equals("\\ifhtml")) {
            String trueBranch = tokenizer.getParameter("\\ifhtml missing parameter: trueBranch");
            String falseBranch = tokenizer.getParameter("\\ifhtml missing parameter: falseBranch");
            write(falseBranch, link, style);
        } else if (cmd.equals("\\local")) {
            String localizationKey = tokenizer.getParameter("\\local missing parameter: localizationKey");
            write(new LocalizedWord(localizationKey, link));
        } else {
            throw new TruthError("Unknown command: " + cmd);
        }
    }

}
