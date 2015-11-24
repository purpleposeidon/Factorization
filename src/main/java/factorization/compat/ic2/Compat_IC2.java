package factorization.compat.ic2;


import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import factorization.api.IRotationalEnergySource;
import factorization.compat.CompatBase;
import factorization.shared.Core;
import factorization.truth.DocumentationModule;
import factorization.truth.api.IObjectWriter;
import factorization.truth.word.ItemWord;
import ic2.api.recipe.Recipes;
import ic2.core.AdvRecipe;
import ic2.core.AdvShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Compat_IC2 extends CompatBase {
    List<String> handled = new ArrayList<String>();

    @Override
    public void init(FMLInitializationEvent event) {
        IRotationalEnergySource.adapter.register(new RotationalEnergySourceAdapter());

        NBTTagCompound tag;
        {
            tag = standardIc2Recipe("blockcutter");
            tag.setTag("catalyst", list("getValue().metadata.'hardness'#Blade Tier"));
            imc(tag);
        }
        {
            tag = standardIc2Recipe("centrifuge");
            tag.setTag("catalyst", list("getValue().metadata.'minHeat'#Heat"));
            imc(tag);
        }

        Class cl = Recipes.class;
        for (Field field : cl.getFields()) {
            final int modifiers = field.getModifiers();
            if ((modifiers & Modifier.PUBLIC) == 0) continue;
            if ((modifiers & Modifier.STATIC) == 0) continue;
            field.setAccessible(true);
            String name = field.getName();
            if (handled.contains(name)) continue;
            FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategory", "fzdoc.ic2.recipe." + name + "|ic2.api.recipe.Recipes|" + name);
        }
        IObjectWriter.adapter.register(AdvRecipe.class, new WriteShapedRecipe());
        IObjectWriter.adapter.register(AdvShapelessRecipe.class, new WriteShapelessRecipe());
    }

    NBTTagCompound standardIc2Recipe(String name) {
        handled.add(name);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("category", "fzdoc.ic2.recipe." + name + "|ic2.api.recipe.Recipes|" + name);
        tag.setTag("input", list("getKey().getInputs()#Input"));
        tag.setTag("output", list("getValue().items"));
        return tag;
    }

    void imc(NBTTagCompound tag) {
        FMLInterModComms.sendMessage(DocumentationModule.modid, "AddRecipeCategoryGuided", tag);
    }

    private static class WriteShapedRecipe implements IObjectWriter<AdvRecipe> {
        @Override
        public void writeObject(List out, AdvRecipe val, IObjectWriter<Object> generic) {
            int mask = val.masks[0];
            int m = 0;
            for (int i = 0; i < 9; i++) {
                if ((mask & 1 << 8 - i) == 0) {
                    out.add(new ItemWord((ItemStack) null));
                } else {
                    out.add(new ItemWord(AdvRecipe.expand(val.input[m++])));
                }
                if ((i + 1) % 3 == 0) {
                    out.add("\\nl");
                }
            }
        }
    }

    private static class WriteShapelessRecipe implements IObjectWriter<AdvShapelessRecipe> {
        @Override
        public void writeObject(List out, AdvShapelessRecipe val, IObjectWriter<Object> generic) {
            out.add("Shapeless: ");
            for (Object obj : val.input) {
                out.add(new ItemWord(AdvRecipe.expand(obj)));
            }
        }
    }
}
