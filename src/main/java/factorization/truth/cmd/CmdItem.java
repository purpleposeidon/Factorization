package factorization.truth.cmd;

import factorization.truth.DocumentationModule;
import factorization.truth.api.*;
import factorization.truth.word.ItemWord;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class CmdItem implements ITypesetCommand {

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
        if (items.size() == 1) {
            return new ItemWord(items.get(0));
        } else {
            ItemStack[] theItems = items.toArray(new ItemStack[items.size()]);
            return new ItemWord(theItems);
        }
    }

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.write(getWord(out, tokenizer));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        ItemWord word = getWord(out, tokenizer);
        // NOTE: This could miss items. Hrm.
        out.putItem(word.getItem(), word.getLink());
    }
}
