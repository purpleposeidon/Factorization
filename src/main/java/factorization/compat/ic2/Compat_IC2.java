package factorization.compat.ic2;


import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import factorization.compat.CompatBase;
import factorization.shared.Core;
import ic2.api.recipe.Recipes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Compat_IC2 extends CompatBase {
    @Override
    public void init(FMLInitializationEvent event) {
        Class cl = Recipes.class;
        for (Field field : cl.getFields()) {
            final int modifiers = field.getModifiers();
            if ((modifiers & Modifier.PUBLIC) == 0) continue;
            if ((modifiers & Modifier.STATIC) == 0) continue;
            field.setAccessible(true);
            String name = field.getName();
            FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", name + "|ic2.api.recipe.Recipes|" + name);
        }
    }
}
