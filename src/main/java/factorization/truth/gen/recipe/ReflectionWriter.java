package factorization.truth.gen.recipe;

import factorization.truth.api.IObjectWriter;
import factorization.truth.word.TextWord;
import net.minecraft.item.crafting.IRecipe;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

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

    void addRecipeWithReflection(List out, Object val, IObjectWriter<Object> generic) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
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

    void do_addRecipeWithReflection(List out, Object val, IObjectWriter<Object> generic) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (writeDirect(out, val, generic)) {
            return; // ItemStack/String/Number/isArray/Collection/NBTBase/Entry
        }
        Object recipeOutput = this; // Just any ol' non-null object
        if (val instanceof IRecipe) {
            recipeOutput = RecipeViewer.genericRecipePrefix(out, (IRecipe) val);
        }
        ArrayList<String> properties = new ArrayList<String>();
        HashSet<Object> seen = new HashSet<Object>();
        final Class<?> valClass = val.getClass();
        if ((valClass.getModifiers() & Modifier.PUBLIC) == 0) return;
        for (Method method : valClass.getMethods()) {
            if (method.getParameterTypes().length != 0) continue;
            if (method.getReturnType() == Void.TYPE) continue;
            if ((method.getModifiers() & Modifier.STATIC) != 0) continue;
            if ((method.getModifiers() & Modifier.PUBLIC) == 0) continue;
            if (method.getDeclaringClass() != valClass) continue;
            String name = method.getName();
            if ("toString".equals(name) || "hashCode".equals(name) || "clone".equals(name)) continue;
            if (name.startsWith("get")) {
                properties.add(name = name.replaceFirst("get", "").toLowerCase(Locale.ROOT));
            }
            Type[] canThrow = method.getGenericExceptionTypes();
            if (canThrow != null && canThrow.length != 0) continue;
            Object v = method.invoke(val);
            if (v == recipeOutput || v == null) continue;
            if (seen.add(v)) {
                put(out, generic, name, v);
            }
        }
        Field[] fields = valClass.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().contains("$")) continue;
            if (properties.contains(f.getName())) continue;
            if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            Object v = f.get(val);
            if (v == recipeOutput || v == null) continue;
            if (seen.add(v)) {
                put(out, generic, f.getName(), v);
            }
        }
    }

    private void put(List out, IObjectWriter<Object> generic, String name, Object v) throws IllegalAccessException, InvocationTargetException {
        //if (name.toLowerCase(Locale.ROOT).equals("output")) return;
        ArrayList tmp = new ArrayList();
        if (writeDirect(tmp, v, generic)) {
            out.add(name + ": ");
            out.addAll(tmp);
            out.add("\\nl ");
        } else {
            addRecipeWithReflection(out, v, generic);
        }
    }
}
