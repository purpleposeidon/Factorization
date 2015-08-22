package factorization.compat.railcraft;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import factorization.compat.CompatBase;
import factorization.shared.Core;
import mods.railcraft.api.crafting.IBlastFurnaceRecipe;
import mods.railcraft.api.crafting.ICokeOvenRecipe;
import mods.railcraft.api.crafting.IRockCrusherRecipe;
import mods.railcraft.api.crafting.RailcraftCraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.List;

public class Compat_Railcraft extends CompatBase {
    public static List<IRecipe> rollingmachine_recipes;
    public static List<? extends IRockCrusherRecipe> crusher_recipes;
    public static List<? extends ICokeOvenRecipe> coke_oven;
    public static List<? extends IBlastFurnaceRecipe> blast_furnace;

    @Override
    public void init(FMLInitializationEvent event) {
        rollingmachine_recipes = RailcraftCraftingManager.rollingMachine.getRecipeList();
        crusher_recipes = RailcraftCraftingManager.rockCrusher.getRecipes();
        coke_oven = RailcraftCraftingManager.cokeOven.getRecipes();
        blast_furnace = RailcraftCraftingManager.blastFurnace.getRecipes();

        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "tile.railcraft.machine.alpha.rolling.machine.name|factorization.compat.railcraft.Compat_Railcraft|rollingmachine_recipes");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "tile.railcraft.machine.alpha.rock.crusher.name|factorization.compat.railcraft.Compat_Railcraft|crusher_recipes");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "railcraft.gui.coke.oven|factorization.compat.railcraft.Compat_Railcraft|coke_oven");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "railcraft.gui.blast.furnace|factorization.compat.railcraft.Compat_Railcraft|blast_furnace");
    }
}
