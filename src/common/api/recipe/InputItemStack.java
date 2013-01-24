package factorization.api.recipe;

import factorization.common.FactorizationUtil;
import net.minecraft.item.ItemStack;

public class InputItemStack implements IGenericRecipeInput {
    ItemStack me;
    
    public InputItemStack(ItemStack is) {
        this.me = is;
    }

    @Override
    public boolean matches(ItemStack is) {
        if (me.getItemDamage() == -1) {
            return is.getItem() == me.getItem();
        }
        return FactorizationUtil.identical(me, is);
    }

}
