package factorization.truth.gen;

import java.util.ArrayList;

import factorization.truth.AbstractTypesetter;
import factorization.truth.word.ItemWord;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EnchantViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        ArrayList<ItemStack> all_items = new ArrayList(); // Yikes! :o
        for (Object obj : Item.itemRegistry) {
            if (obj instanceof Item) {
                all_items.add(new ItemStack((Item) obj));
            }
        }
        for (Enchantment ench : Enchantment.enchantmentsList) {
            if (ench == null) continue;
            int min = ench.getMinLevel();
            int max = ench.getMaxLevel();
            out.append("\\seg");
            out.append("\\nl \\b{" + ench.getTranslatedName(min) + "}");
            out.append("\\nl " + ench.getClass().getSimpleName());
            if (min != max) {
                out.append("\\nl Potencies: " + min + " to " + max);
            }
            out.append("\\nl");
            
            ArrayList<ItemStack> can_enchant = new ArrayList();
            for (ItemStack is : all_items) {
                if (ench.canApplyAtEnchantingTable(is)) {
                    can_enchant.add(is);
                }
            }
            listUsage(out, new ItemStack(Blocks.enchanting_table), can_enchant);
            
            ArrayList<ItemStack> can_apply = new ArrayList();
            for (ItemStack is : all_items) {
                if (ench.canApply(is)) {
                    can_apply.add(is);
                }
            }
            listUsage(out, new ItemStack(Blocks.anvil), can_apply);
            
            
            out.append("\\endseg\\nl");
        }
    }
    
    void listUsage(AbstractTypesetter out, ItemStack tool, ArrayList<ItemStack> appliesTo) {
        out.append("\\nl");
        out.emitWord(new ItemWord(tool));
        if (appliesTo.size() == 0) {
            out.append(" ➤ Nothing");
        } else {
            out.append(" ➤");
            out.emitWord(new ItemWord(appliesTo.toArray(new ItemStack[0])));
        }
        out.append("\\nl");
    }

}
