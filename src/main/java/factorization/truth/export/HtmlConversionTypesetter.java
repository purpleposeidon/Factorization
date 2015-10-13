package factorization.truth.export;

import factorization.truth.AbstractTypesetter;
import factorization.truth.api.*;
import factorization.truth.word.ItemWord;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

public class HtmlConversionTypesetter extends AbstractTypesetter implements IHtmlTypesetter {
    PrintStream out;
    final String root;
    public HtmlConversionTypesetter(String domain, OutputStream out, String root) {
        super(domain);
        this.out = new PrintStream(out);
        this.root = root;
    }
    
    static String found_icon = null;

    @Override
    public void html(String text) {
        out.print(text);
    }

    public static String esc(String s) {
        return s.replace("&", "&amp;").replace(">", "&gt;");
    }

    @Override
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
        html("<span class=\"manualerror\">" + msg + "</s>");
    }

    @Override
    protected void runWord(String word) {
        html(word);
    }

    @Override
    protected void runCommand(ITypesetCommand cmd, ITokenizer tokenizer) throws TruthError {
        cmd.callHTML(this, tokenizer);
    }

    @Override
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
        html("<img class=\"" + imgType + "\" src=\"" + img(found_icon) + "\" />");
        found_icon = null;
        // TODO (and this is crazy!) render the item to a texture
        // Would be good to do this only if it isn't a standard item texture, maybe.
        // Same mechanism could probably be used for docfigures; maybe make an animated gif? Would be rad.
    }

    public String getRoot() {
        return root;
    }

    @Override
    public void write(ItemStack stack) {
        putItem(stack, null);
    }

    @Override
    public void write(ItemStack[] stacks) {
        if (stacks.length > 0) {
            putItem(stacks[0], null);
        }
    }

    @Override
    public void write(Collection<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            putItem(stack, null);
            break;
        }
    }
}
