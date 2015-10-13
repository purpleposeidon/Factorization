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
    public void html(String text) {
        out.print(text);
    }

    void html(String s, String link) {
        out.print(s);
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace(">", "&gt;");
    }

    public String img(String img) {
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
        html("<span class=\"manualerror\">" + msg + "</s>", null);
    }
    
    @Override
    public void write(String text, String link) {
        html(esc(text), link);
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

    public void putItem(ItemStack theItem, String link) {
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
        html("<img class=\"" + imgType + "\" src=\"" + img(found_icon) + "\" />", link);
        found_icon = null;
        // TODO (and this is crazy!) render the item to a texture
        // Would be good to do this only if it isn't a standard item texture.
    }

    public String getRoot() {
        return root;
    }
}
