package factorization.api.recipe;

import java.util.ArrayList;

import factorization.common.FactorizationUtil;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class InputOreDict implements IGenericRecipeInput {
    String dictEntry;
    public InputOreDict(String dictEntry) {
        this.dictEntry = dictEntry;
    }

    @Override
    public boolean matches(ItemStack is) {
        ArrayList<ItemStack> ores = OreDictionary.getOres(dictEntry);
        for (int i = 0; i < ores.size(); i++) {
            ItemStack ore = ores.get(i);
            if (ore.getItemDamage() == -1) {
                if (ore.getItem() == is.getItem()) {
                    return true;
                }
            }
            if (FactorizationUtil.couldMerge(is, ore)) {
                return true;
            }
        }
        return false;
    }

}
