package factorization.compat.railcraft;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import factorization.compat.CompatBase;
import factorization.shared.Core;
import mods.railcraft.api.crafting.IRockCrusherRecipe;
import mods.railcraft.common.util.crafting.RockCrusherCraftingManager;
import mods.railcraft.common.util.crafting.RollingMachineCraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.List;

public class Compat_Railcraft extends CompatBase {
    public static List<IRecipe> rollingmachine_recipes;
    public static List<? extends IRockCrusherRecipe> crusher_recipes;

    @Override
    public void init(FMLInitializationEvent event) {
        rollingmachine_recipes = RollingMachineCraftingManager.getInstance().getRecipeList();
        crusher_recipes = RockCrusherCraftingManager.getInstance().getRecipes();
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "Rolling Machine|factorization.compat.railcraft.Compat_Railcraft|rollingmachine_recipes");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "Rock Crusher|factorization.compat.railcraft.Compat_Railcraft|crusher_recipes");
    }
}
