package factorization.compat.railcraft;

import factorization.compat.CompatBase;
import factorization.truth.DocumentationModule;
import mods.railcraft.api.crafting.IBlastFurnaceRecipe;
import mods.railcraft.api.crafting.ICokeOvenRecipe;
import mods.railcraft.api.crafting.IRockCrusherRecipe;
import mods.railcraft.api.crafting.RailcraftCraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;

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

        FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "tile.railcraft.machine.alpha.rolling.machine.name|factorization.compat.railcraft.Compat_Railcraft|rollingmachine_recipes");
        //FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "tile.railcraft.machine.alpha.rock.crusher.name|factorization.compat.railcraft.Compat_Railcraft|crusher_recipes");
        FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "railcraft.gui.coke.oven|factorization.compat.railcraft.Compat_Railcraft|coke_oven");
        FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "railcraft.gui.blast.furnace|factorization.compat.railcraft.Compat_Railcraft|blast_furnace");

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("category", "tile.railcraft.machine.alpha.rock.crusher.name|factorization.compat.railcraft.Compat_Railcraft|crusher_recipes");
        tag.setTag("input", list("getInput()"));
        tag.setTag("output", list("getPossibleOuputs()"));
        FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategoryGuided", tag);
    }
}
