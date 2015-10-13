package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.DocumentationModule;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.ItemWord;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class CmdItem extends InternalCmd {

    ItemWord getWord(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String itemName = tokenizer.getParameter("No item specified");
        ArrayList<ItemStack> items;

        String stackSizeS = tokenizer.getOptionalParameter();
        if (stackSizeS == null) {
            items = DocumentationModule.lookup(itemName);
        } else {
            String dmgS = tokenizer.getOptionalParameter();
            if (dmgS == null) dmgS = "0"; // intellij says it can not be null. Yes it can?
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
        String link = out.getInfo().link;
        if (items.size() == 1) {
            if (link == null) {
                return new ItemWord(items.get(0));
            } else {
                return new ItemWord(items.get(0), link);
            }
        } else {
            ItemStack[] theItems = items.toArray(new ItemStack[items.size()]);
            if (link == null) {
                return new ItemWord(theItems);
            } else {
                return new ItemWord(theItems, link);
            }
        }
    }

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.write(getWord(out, tokenizer));
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        ItemWord word = getWord(out, tokenizer);
        // NOTE: This could miss items. Hrm.
        out.putItem(word.getItem(), word.getLink());
    }
}
