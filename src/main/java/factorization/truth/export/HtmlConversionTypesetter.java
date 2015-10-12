package factorization.truth.export;

import cpw.mods.fml.common.Loader;
import factorization.shared.Core;
import factorization.truth.AbstractTypesetter;
import factorization.truth.DocumentationModule;
import factorization.truth.Tokenizer;
import factorization.truth.api.DocReg;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.TruthError;
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
    protected void handleCommand(Tokenizer tokenizer, String cmd, String link, String style) throws TruthError {
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
            String content = tokenizer.getParameter("styled text");
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
                throw new TruthError("Unknown style: " + cmd);
            }
            s(open, null);
            write(content, link, style);
            s(close, null);
        } else if (cmd.equals("\\title")) {
            s("\n\n<h1>", null);
            String val = tokenizer.getParameter("title text");
            write(val, link, style);
            s("</h1>\n", null);
        } else if (cmd.equals("\\h1")) {
            String val = tokenizer.getParameter("header text");
            s("<h2>", null);
            write(val, link, style);
            s("</h2>\n", null);
        } else if (cmd.equals("\\link") || cmd.equals("\\index")) {
            String newLink = tokenizer.getParameter("missing link destination parameter");
            String content = tokenizer.getParameter("missing link content parameter");
            String ref = newLink;
            if (!newLink.contains("://")) {
                ref = root + newLink + ".html";
            }
            s("<a href=\"" + ref + "\">", null);
            write(content, newLink, style);
            s("</a>", null);
            if (cmd.equals("\\index")) {
                s("<br>\n", null);
            }
            ExportHtml.visitLink(newLink);
        } else if (cmd.equals("\\#")) {
            String itemName = tokenizer.getParameter("No item specified");
            ArrayList<ItemStack> items = DocumentationModule.lookup(itemName);
            if (items == null) {
                throw new TruthError(itemName + " no such item");
            }
            // NOTE: This could miss items. Hrm.
            ItemStack theItem = items.get(0);
            putItem(theItem, link);
        } else if (cmd.equals("\\img")) {
            String imgName = tokenizer.getParameter("No img specified");
            s("<img src=\"" + img(imgName) + "\" />", link);
        } else if (cmd.equals("\\imgx")) {
            int width = Integer.parseInt(tokenizer.getParameter("img width"));
            int height = Integer.parseInt(tokenizer.getParameter("img height"));
            String imgName = tokenizer.getParameter("No img specified");
            s(String.format("<img width=%s height=%s src=\"%s\" />", width, height, img(imgName)), link);
        } else if (cmd.equals("\\figure")) {
            // Prooobably not going to implement this one ;)
            tokenizer.getParameter("encoded figure");
        } else if (cmd.equals("\\generate")) {
            String arg = tokenizer.getParameter("\\generate parameter");
            String args[] = arg.split("/", 2);
            IDocGenerator gen = DocReg.generators.get(args[0]);
            if (gen == null) {
                throw new TruthError("\\generate{" + arg + "}: Not found: " + args[0]);
            }
            String rest = args.length > 1 ? args[1] : "";
            gen.process(this, rest);
        } else if (cmd.equals("\\seg")) {
            // NOP
        } else if (cmd.equals("\\endseg")) {
            // NOP
        } else if (cmd.equals("\\topic")) {
            String topic = tokenizer.getParameter("topic name");
            String sub = String.format("\\newpage \\generate{recipes/for/%s}", topic);
            write(sub);
        } else if (cmd.equals("\\checkmods")) {
            // Eh. Heh. Oh boy...
            String mode = tokenizer.getParameter("mode (all|some|none)");
            String modList = tokenizer.getParameter("space separated modID list");
            String content = tokenizer.getParameter("content");
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
                throw new TruthError("\\checkmods first parameter must be 'all', 'none', or 'some', not " + mode);
            }
            String other = tokenizer.getOptionalParameter();
            if (good) {
                write(content, link, style);
            } else if (other != null) {
                write(other, link, style);
            }
        } else if (cmd.equals("\\ifhtml")) {
            String trueBranch = tokenizer.getParameter("true branch");
            tokenizer.getParameter("false branch");
            write(trueBranch, link, style);
        } else if (cmd.equals("\\vpad")) {
            tokenizer.getParameter("vpad amount");
        } else if (cmd.equals("\\-")) {
            s("<br> •", null);
        } else if (cmd.equals("\\url")) {
            String url = tokenizer.getParameter("url target");
            String content = tokenizer.getParameter("link content");
            write(content, url, style);
        } else if (cmd.equals("\\local")) {
            String localizationKey = tokenizer.getParameter("localization key");
            s(Core.translate(localizationKey), link);
        } else {
            throw new TruthError("Unknown command " + cmd);
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
    public void writeErrorMessage(String msg) {
        s("<span class=\"manualerror\">" + msg + "</s>", null);
    }
    
    @Override
    public void write(String text, String link) {
        s(esc(text), link);
    }

    @Override
    public void write(Word w) {
        // LAMELY IMPLEMENTED! :O
        if (w instanceof TextWord) {
            TextWord tw = (TextWord) w;
            write(tw.text, tw.getLink());
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
