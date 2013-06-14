package factorization.common.compat;

import net.minecraft.item.ItemStack;
import factorization.common.Core;

public class Compat_IC2 {
    public Compat_IC2() {
        ItemStack matter = ic2.api.item.Items.getItem("matter").copy();
        Core.registry.shapelessOreRecipe(Core.registry.glaze_base_mimicry, Core.registry.base_unreal, matter);
    }
}
