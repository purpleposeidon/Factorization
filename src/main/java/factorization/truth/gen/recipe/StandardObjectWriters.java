package factorization.truth.gen.recipe;

import factorization.api.adapter.Adapter;
import factorization.api.adapter.GenericAdapter;
import factorization.truth.api.IObjectWriter;
import factorization.truth.gen.FluidViewer;
import factorization.truth.word.ItemWord;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Array;
import java.util.*;

class StandardObjectWriters {
    private static void reg(Class<?> klass, IObjectWriter out) {
        IObjectWriter.adapter.register(klass, out);
    }

    static boolean is_setup = false;
    static void setup() {
        if (is_setup) return;
        is_setup = true;
        reg(ItemStack.class, new WriteItemStack());
        reg(Item.class, new WriteItem());
        reg(Block.class, new WriteBlock());
        reg(String.class, new WriteStringOreDictionary());
        reg(Number.class, new WriteObjectToString());
        reg(NBTBase.class, new WriteObjectToString());
        reg(FluidStack.class, new WriteFluidStack());
        reg(Fluid.class, new WriteFluid());
        reg(Collection.class, new WriteCollection());
        // IRecipe: "embedded IRecipe"; haven't seen it crop up tho
        reg(ShapedOreRecipe.class, new WriteShapedOreRecipe());
        reg(ShapedRecipes.class, new WriteShapedRecipe());
        reg(ShapelessOreRecipe.class, new WriteShapelessOreRecipe());
        reg(ShapelessRecipes.class, new WriteShapelessRecipe());
        reg(Map.Entry.class, new WriteEntry());

        IObjectWriter.adapter.register(new ArrayAdapter());
        IObjectWriter.adapter.setFallbackAdapter(new GenericAdapter<Object, IObjectWriter>(Object.class, new ReflectionWriter()));
    }

    private static class WriteItemStack implements IObjectWriter<ItemStack> {
        @Override
        public void writeObject(List out, ItemStack val, IObjectWriter<Object> generic) {
            out.add(new ItemWord(val));
        }
    }

    private static class WriteItem implements IObjectWriter<Item> {
        @Override
        public void writeObject(List out, Item val, IObjectWriter<Object> generic) {
            out.add(new ItemWord(new ItemStack(val)));
        }
    }

    private static class WriteBlock implements IObjectWriter<Block> {
        @Override
        public void writeObject(List out, Block val, IObjectWriter<Object> generic) {
            out.add(new ItemWord(new ItemStack(val)));
        }
    }

    private static class WriteStringOreDictionary /* get it? D'ya get it? ha ha ha */ implements IObjectWriter<String> {
        final HashSet<String> knownOres = new HashSet<String>();
        {
            Collections.addAll(knownOres, OreDictionary.getOreNames());
        }
        @Override
        public void writeObject(List out, String val, IObjectWriter<Object> generic) {
            if (knownOres.contains(val)) {
                List<ItemStack> ores = OreDictionary.getOres(val);
                if (!ores.isEmpty()) {
                    out.add(new ItemWord(ores.toArray(new ItemStack[ores.size()])));
                    return;
                }
            }
            out.add(new TextWord(val));
        }
    }

    private static class WriteObjectToString implements IObjectWriter<Object> {
        @Override
        public void writeObject(List out, Object val, IObjectWriter<Object> generic) {
            out.add(new TextWord(val.toString()));
        }
    }

    private static class WriteFluidStack implements IObjectWriter<FluidStack> {
        @Override
        public void writeObject(List out, FluidStack val, IObjectWriter<Object> generic) {
            out.add(FluidViewer.convert(val.getFluid()));
            out.add(new TextWord(val.getLocalizedName()));
            if (val.amount == 0) return;
            if (val.amount % 1000 == 0) {
                out.add(new TextWord(" " + (val.amount / 1000) + "B"));
            } else {
                out.add(new TextWord(" " + val.amount + "mB"));
            }
            // TODO: Link to fluid viewer
        }
    }


    private static class WriteFluid implements IObjectWriter<Fluid> {
        @Override
        public void writeObject(List out, Fluid val, IObjectWriter<Object> generic) {
            out.add(FluidViewer.convert(val));
            // TODO: Link to fluid viewer
        }
    }

    private static class WriteCollection implements IObjectWriter<Collection<Object>> {
        HashSet<Collection> reverseOD = new HashSet<Collection>();
        {
            for (String name : OreDictionary.getOreNames()) {
                reverseOD.add(OreDictionary.getOres(name));
            }
        }
        @Override
        public void writeObject(List out, Collection<Object> val, IObjectWriter<Object> generic) {
            if (reverseOD.contains(val)) {
                //noinspection SuspiciousToArrayCall -- We know it must contain only ItemStacks
                ItemStack[] items = val.toArray(new ItemStack[val.size()]);
                out.add(new ItemWord(items));
                return;
            }
            for (Object o : val) {
                generic.writeObject(out, o, generic);
            }
        }
    }

    static ItemStack fixMojangRecipes(ItemStack is) {
        if (is == null) return null;
        if (is.stackSize > 1) {
            is = is.copy();
            is.stackSize = 1;
        }
        return is;
    }

    static Word stackOreDictionary(Object obj) {
        if (obj == null) {
            return new ItemWord((ItemStack) null);
        } else if (obj instanceof ItemStack) {
            ItemStack is = (ItemStack) obj;
            return new ItemWord(fixMojangRecipes(is));
        } else if (obj instanceof Collection) {
            Collection<ItemStack> od = (Collection<ItemStack>) obj;
            ItemStack[] is = new ItemStack[od.size()];
            return new ItemWord(od.toArray(is));
        } else {
            return new TextWord("?" + obj.toString());
        }
    }

    private static class WriteShapedOreRecipe implements IObjectWriter<ShapedOreRecipe> {
        @Override
        public void writeObject(List out, ShapedOreRecipe val, IObjectWriter<Object> generic) {
            int width = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, val, "width");
            //int height = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, val, "height");
            Object[] input = val.getInput();
            int i = 0;
            for (Object in : input) {
                out.add(stackOreDictionary(in));
                i++;
                if (i % width == 0) {
                    out.add("\\nl");
                }
            }
        }
    }

    private static class WriteShapedRecipe implements IObjectWriter<ShapedRecipes> {
        @Override
        public void writeObject(List out, ShapedRecipes val, IObjectWriter<Object> generic) {
            int width = val.recipeWidth;
            for (int i = 0; i < val.recipeItems.length; i++) {
                out.add(new ItemWord(fixMojangRecipes(val.recipeItems[i])));
                if ((i + 1) % width == 0) {
                    out.add("\\nl");
                }
            }
        }
    }

    private static class WriteShapelessOreRecipe implements IObjectWriter<ShapelessOreRecipe> {
        @Override
        public void writeObject(List out, ShapelessOreRecipe val, IObjectWriter<Object> generic) {
            ArrayList<Object> input = val.getInput();
            if (input == null) return;
            out.add("Shapeless: ");
            for (Object obj : input) {
                out.add(stackOreDictionary(obj));
            }
        }
    }

    private static class WriteShapelessRecipe implements IObjectWriter<ShapelessRecipes> {
        @Override
        public void writeObject(List out, ShapelessRecipes val, IObjectWriter<Object> generic) {
            if (val.recipeItems == null) return;
            out.add("Shapeless: ");
            for (Object obj : val.recipeItems) {
                out.add(new ItemWord((ItemStack) obj));
            }
        }
    }

    private static class WriteEntry implements IObjectWriter<Map.Entry> {
        @Override
        public void writeObject(List out, Map.Entry val, IObjectWriter<Object> generic) {
            generic.writeObject(out, val.getKey(), generic);
            out.add(" ➤ ");
            generic.writeObject(out, val.getValue(), generic);
        }
    }

    private static class ArrayAdapter implements Adapter<Object,IObjectWriter>, IObjectWriter<Object> {
        @Override
        public IObjectWriter adapt(Object val) {
            return this;
        }

        @Override
        public boolean canAdapt(Class<?> valClass) {
            return valClass.isArray();
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public void writeObject(List out, Object val, IObjectWriter<Object> generic) {
            out.add("[");
            int len = Array.getLength(val);
            for (int i = 0; i < len; i++) {
                if (i > 0) out.add(", ");
                Object v = Array.get(val, i);
                generic.writeObject(out, v, generic);
            }
            out.add("]");
        }
    }
}
