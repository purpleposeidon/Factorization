package factorization.compat.erebus;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import factorization.compat.CompatBase;
import factorization.shared.Core;
import factorization.truth.DocumentationModule;

public class Compat_erebus extends CompatBase {
    public static Iterable<Object> compost_recipes, offering_altar_recipes, smoothie_recipes;

    private Iterable<Object> find(String className, String fieldName) {
        try {
            Class compost = ReflectionHelper.getClass(getClass().getClassLoader(), className);
            return (Iterable<Object>) ReflectionHelper.getPrivateValue(compost, null, fieldName);
        } catch (Throwable t) {
            Core.logWarning("Couldn't find erebus recipe: " + className + "." + fieldName);
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        compost_recipes = find("erebus.recipes.ComposterRegistry", "registry");
        if (compost_recipes != null) {
            FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "container.composter|factorization.compat.erebus.Compat_erebus|compost_recipes");
        }
        offering_altar_recipes = find("erebus.recipes.OfferingAltarRecipe", "list");
        if (offering_altar_recipes != null) {
            FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "tile.erebus.offeringAltar.name|factorization.compat.erebus.Compat_erebus|offering_altar_recipes");
        }
        smoothie_recipes = find("erebus.recipes.SmoothieMakerRecipe", "recipes");
        if (smoothie_recipes != null) {
            FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "tile.erebus.smoothieMaker.name|factorization.compat.erebus.Compat_erebus|smoothie_recipes");
        }
    }
}
