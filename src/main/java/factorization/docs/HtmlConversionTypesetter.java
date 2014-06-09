package factorization.docs;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.Loader;

public class HtmlConversionTypesetter extends AbstractTypesetter {
    PrintStream out;
    public HtmlConversionTypesetter(OutputStream out) {
        super(null, 0, 0);
        this.out = new PrintStream(out);
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
            s("<a href=\"" + newLink + ".html\">", null);
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
            found_icon = null;
            theItem.getItem().registerIcons(new IIconRegister() {
                @Override
                public IIcon registerIcon(String iconName) {
                    if (found_icon == null) {
                        found_icon = iconName;
                    }
                    return null; // This'll break shit. Oh well. :/
                }
            });
            s("<img src=\"" + found_icon + "\" />", link);
            found_icon = null;
        } else if (cmd.equals("\\img")) {
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            s("<img src=\"" + imgName + "\" />", link);
        } else if (cmd.equals("\\imgx")) {
            int width = Integer.parseInt(getParameter(cmd, tokenizer));
            int height = Integer.parseInt(getParameter(cmd, tokenizer));
            String imgName = getParameter(cmd, tokenizer);
            if (imgName == null) {
                error("No img specified");
                return;
            }
            s(String.format("<img width=%s height=%s src=\"%s\" />", width, height, imgName), link);
        } else if (cmd.equals("\\figure")) {
            // TODO...
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
            // NOP
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
        } else {
            error("Unknown command: ");
            emit(cmd, null);
        }
    }

    void s(String s, String link) {
        out.print(s);
    }
    
    @Override
    void error(String msg) {
        s("<span class=\"mcerror\">" + msg + "</s>", null);
    }
    
    @Override
    TextWord emit(String text, String link) {
        s(text, link);
        return null;
    }
}
