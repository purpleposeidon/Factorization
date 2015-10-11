package factorization.truth.gen.recipe;

import factorization.truth.api.IObjectWriter;
import factorization.truth.word.TextWord;
import net.minecraft.item.crafting.IRecipe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectionWriter implements IObjectWriter<Object> {
    int recursion = 0;

    @Override
    public void writeObject(List out, Object val, IObjectWriter<Object> generic) {
        try {
            addRecipeWithReflection(out, val, generic);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    boolean writeDirect(List out, Object val, IObjectWriter<Object> generic) {
        IObjectWriter writer = IObjectWriter.adapter.cast(val);
        if (writer != this) {
            // ItemStack/String/Number/isArray/Collection/NBTBase/Entry
            writer.writeObject(out, val, generic);
            return true;
        }
        return false;
    }

    void addRecipeWithReflection(List out, Object val, IObjectWriter<Object> generic) throws IllegalArgumentException, IllegalAccessException {
        if (recursion > 4) {
            out.add(new TextWord("…"));
            return;
        }
        recursion++;
        try {
            do_addRecipeWithReflection(out, val, generic);
        } finally {
            recursion--;
        }
    }

    void do_addRecipeWithReflection(List out, Object val, IObjectWriter<Object> generic) throws IllegalArgumentException, IllegalAccessException {
        if (writeDirect(out, val, generic)) {
            return; // ItemStack/String/Number/isArray/Collection/NBTBase/Entry
        }
        Object recipeOutput = this; // Just any ol' non-null object
        if (val instanceof IRecipe) {
            recipeOutput = RecipeViewer.genericRecipePrefix(out, (IRecipe) val);
        }
        Field[] fields = val.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().contains("$")) continue;
            int modifiers = f.getModifiers();
            if ((modifiers & Modifier.STATIC) != 0) continue;
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            Object v = f.get(val);
            if (v == recipeOutput || v == null) continue;
            ArrayList tmp = new ArrayList();
            if (writeDirect(tmp, v, generic)) {
                out.add(f.getName() + ": ");
                out.addAll(tmp);
                out.add("\\nl ");
            } else {
                addRecipeWithReflection(out, v, generic);
            }
        }
    }
}
