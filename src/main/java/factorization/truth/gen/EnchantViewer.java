package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class EnchantViewer implements IDocGenerator {

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        ArrayList<ItemStack> all_items = new ArrayList<ItemStack>(); // Yikes! :o
        for (Object obj : Item.itemRegistry) {
            if (obj instanceof Item) {
                all_items.add(new ItemStack((Item) obj));
            }
        }
        for (Enchantment ench : Enchantment.enchantmentsList) {
            if (ench == null) continue;
            int min = ench.getMinLevel();
            int max = ench.getMaxLevel();
            out.write("\\seg");
            out.write("\\nl \\b{" + ench.getTranslatedName(min) + "}");
            out.write("\\nl " + ench.getClass().getSimpleName());
            if (min != max) {
                out.write("\\nl Potencies: " + min + " to " + max);
            }
            out.write("\\nl");
            
            ArrayList<ItemStack> can_enchant = new ArrayList<ItemStack>();
            for (ItemStack is : all_items) {
                if (ench.canApplyAtEnchantingTable(is)) {
                    can_enchant.add(is);
                }
            }
            listUsage(out, new ItemStack(Blocks.enchanting_table), can_enchant);
            
            ArrayList<ItemStack> can_apply = new ArrayList<ItemStack>();
            for (ItemStack is : all_items) {
                if (ench.canApply(is)) {
                    can_apply.add(is);
                }
            }
            listUsage(out, new ItemStack(Blocks.anvil), can_apply);
            
            
            out.write("\\endseg\\nl");
        }
    }
    
    void listUsage(ITypesetter out, ItemStack tool, ArrayList<ItemStack> appliesTo) throws TruthError {
        out.write("\\nl");
        out.write(tool);
        if (appliesTo.size() == 0) {
            out.write(" ➤ Nothing");
        } else {
            out.write(" ➤");
            out.write(appliesTo);
        }
        out.write("\\nl");
    }

}
