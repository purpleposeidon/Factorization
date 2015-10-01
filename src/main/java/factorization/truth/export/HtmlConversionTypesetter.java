package factorization.truth.export;

import cpw.mods.fml.common.Loader;
import factorization.shared.Core;
import factorization.truth.AbstractTypesetter;
import factorization.truth.DocumentationModule;
import factorization.truth.Tokenizer;
import factorization.truth.gen.IDocGenerator;
import factorization.truth.word.ItemWord;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class HtmlConversionTypesetter extends AbstractTypesetter {
    PrintStream out;
    final String root;
    public HtmlConversionTypesetter(String domain, OutputStream out, String root) {
        super(domain, null, 0, 0);
        this.out = new PrintStream(out);
        this.root = root;
    }
    
    static String found_icon = null;
    
    @Override
    protected void handleCommand(Tokenizer tokenizer, String cmd, String link, String style) {
        if (cmd.equals("\\p")) {
            s("<br>\n", link);
        } else if (cmd.equals("\\nl")) {
            s("<br>\n", link);
        } else if (cmd.equals("\\") || cmd.equals("\\ ")) {
            s(" ", link);
        } else if (cmd.equalsIgnoreCase("\\\\")) {
            s("\\", link);
        } else if (cmd.equals("\\newpage")) {
            // NOP
        } else if (cmd.equals("\\leftpage")) {
            // NOP
        }  else if (cmd.equals("\\b") || cmd.equals("\\i") || cmd.equals("\\u") || cmd.equals("\\obf")) {
            char mode = cmd.charAt(1);
            String content = getParameter(cmd, tokenizer);
            if (content == null) return;
            String open, close;
            if (mode == 'b') {
                open = "<b>";
                close = "</b>";
            } else if (mode == 'i') {
                open = "<i>";
                close = "</i>";
            } else if (mode == 'u') {
                open = "<u>";
                close = "</u>";
            } else if (mode == 'o') {
                open = "<span class=\"mcobfuscated\">"; // Heh.
                close = "</span>";
            } else {
                error("Unknown style: " + cmd);
                return;
            }
            s(open, null);
            process(content, link, style);
            s(close, null);
        } else if (cmd.equals("\\title")) {
            s("\n\n<h1>", null);
            String val = getParameter(cmd, tokenizer);
            if (val == null) {
                error("No content");
                return;
            }
            process(val, link, style);
            s("</h1>\n", null);
        } else if (cmd.equals("\\h1")) {
            String val = getParameter(cmd, tokenizer);
            if (val == null) {
                error("No content");
                return;
            }
            s("<h2>", null);
            process(val, link, style);
            s("</h2>\n", null);
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
            String ref = newLink;
            if (!newLink.contains("://")) {
                ref = root + newLink + ".html";
            }
            s("<a href=\"" + ref + "\">", null);
            process(content, newLink, style);
            s("</a>", null);
            if (cmd.equals("\\index")) {
                s("<br>\n", null);
            }
            ExportHtml.visitLink(newLink);
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
            ItemStack theItem = items.get(0);
            putItem(theItem, link);
        } else if (cmd.equals("\\img")) {
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            s("<img src=\"" + img(imgName) + "\" />", link);
        } else if (cmd.equals("\\imgx")) {
            int width = Integer.parseInt(getParameter(cmd, tokenizer));
            int height = Integer.parseInt(getParameter(cmd, tokenizer));
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            s(String.format("<img width=%s height=%s src=\"%s\" />", width, height, img(imgName)), link);
        } else if (cmd.equals("\\figure")) {
            // Prooobably not going to implement this one ;)
            String arg = getParameter(cmd, tokenizer);
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
            // NOP
        } else if (cmd.equals("\\endseg")) {
            // NOP
        } else if (cmd.equals("\\topic")) {
            String topic = getParameter(cmd, tokenizer);
            if (topic == null) {
                error("\\topic missing parameter");
                return;
            }
            String sub = String.format("\\newpage \\generate{recipes/for/%s}", topic);
            append(sub);
        } else if (cmd.equals("\\checkmods")) {
            // Eh. Heh. Oh boy...
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
            } else if (mode.equalsIgnoreCase("some") || mode.equalsIgnoreCase("any")) {
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
        } else if (cmd.equals("\\ifhtml")) {
            String trueBranch = getParameter(cmd, tokenizer);
            String falseBranch = getParameter(cmd, tokenizer);
            process(trueBranch, link, style);
        } else if (cmd.equals("\\vpad")) {
            getParameter(cmd, tokenizer);
        } else if (cmd.equals("\\-")) {
            s("<br> •", null);
        } else if (cmd.equals("\\url")) {
            String url = getParameter(cmd, tokenizer);
            String content = getParameter(cmd, tokenizer);
            process(content, url, style);
        } else if (cmd.equals("\\local")) {
            String localizationKey = getParameter(cmd, tokenizer);
            if (localizationKey == null) {
                error("\\local missing parameter: localizationKey");
                return;
            }
            s(Core.translate(localizationKey), link);
        } else {
            error("Unknown command: ");
            emit(cmd, null);
        }
    }

    void s(String s, String link) {
        out.print(s);
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace(">", "&gt;");
    }

    String img(String img) {
        if (!img.contains(":")) {
            img = "minecraft:" + img;
        }
        String[] parts = img.split(":", 2);
        String domain = parts[0];
        String path = parts[1];
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        return root + "resources/" + domain + "/textures/" + path;
    }
    
    @Override
    public void error(String msg) {
        s("<span class=\"manualerror\">" + msg + "</s>", null);
    }
    
    @Override
    public TextWord emit(String text, String link) {
        s(esc(text), link);
        return null;
    }

    @Override
    public void emitWord(Word w) {
        // LAMELY IMPLEMENTED! :O
        if (w instanceof TextWord) {
            TextWord tw = (TextWord) w;
            emit(tw.text, tw.getLink());
        } else if (w instanceof ItemWord) {
            ItemWord iw = (ItemWord) w;
            ItemStack is = iw.getItem();
            putItem(is, iw.getLink());
        }
    }

    void putItem(ItemStack theItem, String link) {
        String imgType = null;
        IIcon iconIndex = null;
        if (theItem != null) {
            imgType = theItem.getItemSpriteNumber() == 1 ? "items" : "blocks";
            iconIndex = theItem.getIconIndex();
        }
        if (iconIndex == null) {
            imgType = "items";
            found_icon = "factorization:transparent_item";
        } else {
            found_icon = iconIndex.getIconName();
            if (!found_icon.contains(":")) {
                found_icon = "minecraft:" + found_icon;
            }
        }
        String[] parts = found_icon.split(":", 2);
        String namespace = parts[0];
        String path = parts[1];
        found_icon = namespace + ":" + imgType + "/" + path;
        s("<img class=\"" + imgType + "\" src=\"" + img(found_icon) + "\" />", link);
        found_icon = null;
        // TODO (and this is crazy!) render the item to a texture
        // Would be good to do this only if it isn't a standard item texture.
    }
}
