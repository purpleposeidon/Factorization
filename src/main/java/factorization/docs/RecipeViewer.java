package factorization.docs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntitySlagFurnace;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class RecipeViewer implements IDocGenerator {
    HashMap<String, ArrayList<ArrayList>> recipeCategories = null;
    ArrayList<String> categoryOrder = new ArrayList();
    
    @Override
    public void process(Typesetter out, String arg) {
        if (recipeCategories == null) {
            recipeCategories = new HashMap();
            Core.logInfo("Loading recipe list");
            loadRecipes();
            Core.logInfo("Done");
        }
        if (arg == null || arg.equalsIgnoreCase("categories") || arg.isEmpty()) {
            out.append("\\title{Recipe Categories}\n\n");
            for (String cat : categoryOrder) {
                out.append(String.format("\\link{cgi/recipes/category/%s}{%s}\\nl", cat, cat));
            }
        } else if (arg.startsWith("category/")) {
            String cat = arg.replace("category/", "");
            if (recipeCategories.containsKey(cat)) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, null, cat, recipeList);
            } else {
                out.error("Category not found: " + arg);
            }
        } else {
            ItemStack matching = null;
            if (!arg.equalsIgnoreCase("all")) {
                ArrayList<ItemStack> matchers = DocumentationModule.getNameItemCache().get(arg);
                if (!matchers.isEmpty()) {
                    matching = matchers.get(0);
                }
                if (matching == null) {
                    out.error("Couldn't find item: " + arg);
                    return;
                }
                out.emitWord(new ItemWord(matching));
                out.append("\\nl");
            }
            
            for (String cat : categoryOrder) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, matching, cat, recipeList);
            }
            
        }
    }
    
    void writeRecipes(Typesetter out, ItemStack matching, String categoryName, ArrayList<ArrayList> recipes) {
        if (matching == null) {
            for (ArrayList recipe : recipes) {
                writeRecipe(out, recipe);
            }
        } else {
            boolean first = true;
            for (ArrayList recipe : recipes) {
                if (recipeMatches(recipe, matching)) {
                    if (first) {
                        first = false;
                        if (categoryName != null) {
                            out.append("\\u{" + categoryName + "}\n\n");
                        }
                    }
                    writeRecipe(out, recipe);
                }
            }
        }
    }
    
    boolean recipeMatches(ArrayList recipe, ItemStack matching) {
        for (Object part : recipe) {
            if (part instanceof ItemWord) {
                ItemWord iw = (ItemWord) part;
                if (iw.is == null) continue;
                if (FzUtil.identical(iw.is, matching) || FzUtil.wildcardSimilar(iw.is, matching)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    void writeRecipe(Typesetter out, ArrayList parts) {
        for (Object part : parts) {
            if (part instanceof String) {
                out.append((String) part);
            } else {
                out.emitWord((Word) part);
            }
        }
    }
    
    void loadRecipes() {
        putCategory("Workbench", CraftingManager.getInstance().getRecipeList());
        putCategory("Furnace", FurnaceRecipes.smelting().getSmeltingList().entrySet());
        putCategory("Slag Furnace", TileEntitySlagFurnace.SlagRecipes.smeltingResults);
        putCategory("Lacerator", TileEntityGrinder.recipes);
        putCategory("Crystallizer", TileEntityCrystallizer.recipes);
    }
    
    void putCategory(String label, Iterable list) {
        recipeCategories.put(label, addAll(list));
        categoryOrder.add(label);
    }
    
    int recursion;
    ArrayList<ArrayList> addAll(Iterable list) {
        ArrayList<ArrayList> generated = new ArrayList();
        for (Object obj : list) {
            ArrayList entry = new ArrayList();
            recursion = 0;
            addRecipe(entry, obj);
            if (!entry.isEmpty()) {
                generated.add(entry);
            }
        }
        return generated;
    }
    
    static String getDisplayName(ItemStack is) {
        if (is == null) return "null";
        try {
            return is.getDisplayName();
        } catch (Throwable t) {
            t.printStackTrace();
            return "ERROR";
        }
    }
    
    void addRecipe(List sb, Object obj) {
        sb.add("\\seg");
        if (obj instanceof ShapedOreRecipe) {
            addShapedOreRecipe(sb, (ShapedOreRecipe) obj);
        } else if (obj instanceof ShapedRecipes) {
            addShapedRecipes(sb, (ShapedRecipes) obj);
        } else if (obj instanceof ShapelessOreRecipe) {
            addShapelessOreRecipe(sb, (ShapelessOreRecipe) obj);
        } else if (obj instanceof ShapelessRecipes) {
            addShapelessRecipes(sb, (ShapelessRecipes) obj);
        } else {
            try {
                addRecipeWithReflection(sb, obj);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        sb.add("\\endseg");
        sb.add("\n\n");
    }
    
    Object genericRecipePrefix(List sb, IRecipe recipe) {
        ItemStack output = ((IRecipe) recipe).getRecipeOutput();
        if (output == null) return null;
        sb.add(new ItemWord(output));
        sb.add(" \\b{" + getDisplayName(output) + "}\n\n");
        return output;
    }
    
    void addShapedOreRecipe(List sb, ShapedOreRecipe recipe) {
        genericRecipePrefix(sb, recipe);
        int width = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, recipe, "width");
        int height = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, recipe, "height");
        Object[] input = recipe.getInput();
        int i = 0;
        for (Object in : input) {
            if (in instanceof ItemStack || in == null) {
                ItemStack is = (ItemStack) in;
                sb.add(new ItemWord(is));
            } else if (in instanceof Iterable) {
                Iterator<Object> it = ((Iterable)in).iterator();
                if (it.hasNext()) {
                    convertObject(sb, it.next());
                } else {
                    convertObject(sb, null);
                }
            } else {
                convertObject(sb, in);
            }
            i++;
            if (i % width == 0) {
                sb.add("\n\n");
            } else {
                sb.add(" ");
            }
        }
    }
    
    void addShapedRecipes(List sb, ShapedRecipes recipe) {
        genericRecipePrefix(sb, recipe);
        int width = recipe.recipeWidth;
        for (int i = 0; i < recipe.recipeItems.length; i++) {
            sb.add(new ItemWord(recipe.recipeItems[i]));
            if ((i + 1) % width == 0) {
                sb.add("\n\n");
            } else {
                sb.add(" ");
            }
        }
    }
    
    void addShapelessOreRecipe(List sb, ShapelessOreRecipe recipe) {
        genericRecipePrefix(sb, recipe);
        sb.add("Shapeless: ");
        for (Object obj : recipe.getInput()) {
            if (obj instanceof Object[]) {
                Object[] objs = (Object[]) obj;
                if (objs.length > 0) {
                    convertObject(sb, objs[0]);
                } else {
                    convertObject(sb, null);
                }
            } else {
                convertObject(sb, obj);
            }
        }
    }
    
    void addShapelessRecipes(List sb, ShapelessRecipes recipe) {
        genericRecipePrefix(sb, recipe);
        sb.add("Shapeless: ");
        for (Object obj : recipe.recipeItems) {
            convertObject(sb, obj);
        }
    }
    
    void addRecipeWithReflection(List sb, Object recipe) throws IllegalArgumentException, IllegalAccessException {
        if (recipe instanceof ItemStack || recipe instanceof String || recipe.getClass().isArray() || recipe instanceof Collection) {
            convertObject(sb, recipe);
            return;
        }
        Object output = RecipeViewer.class; //Just something that isn't null.
        if (recipe instanceof IRecipe) {
            output = genericRecipePrefix(sb, (IRecipe) recipe);
        } else if (recipe instanceof Entry) {
            Entry ent = (Entry) recipe;
            addRecipeWithReflection(sb, ent.getKey());
            sb.add(" ==> ");
            addRecipeWithReflection(sb, ent.getValue());
            return;
        }
        Field[] fields = recipe.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            int modifiers = f.getModifiers();
            if ((modifiers & Modifier.STATIC) != 0) continue;
            Object v = f.get(recipe);
            if (v == output || v == null) continue;
            if (v instanceof String || v instanceof ItemStack || v instanceof Collection || v.getClass().isArray()) {
                sb.add(f.getName() + ": ");
                convertObject(sb, v);
                sb.add("\\nl ");
            }
        }
    }
    
    void convertObject(List sb, Object obj) {
        if (obj == null) {
            return;
        }
        if (recursion > 4) {
            return;
        }
        if (obj instanceof Item) {
            obj = new ItemStack((Item) obj);
        } else if (obj instanceof Block) {
            obj = new ItemStack((Block) obj);
        }
        
        recursion++;
        try {
            if (obj instanceof ItemStack) {
                sb.add(new ItemWord((ItemStack) obj));
            } else if (obj instanceof String) {
                sb.add(obj.toString());
            } else if (obj.getClass().isArray()) {
                Class<?> component = obj.getClass().getComponentType();
                if (component == Object.class) {
                    Object[] listy = (Object[]) obj;
                    for (Object o : listy) {
                        convertObject(sb, o);
                    }
                } else if (component == ItemStack.class) {
                    ItemStack[] listy = (ItemStack[]) obj;
                    for (ItemStack is : listy) {
                        sb.add(new ItemWord(is));
                    }
                }
            } else if (obj instanceof Collection) {
                String ret = "";
                for (Object o : (Collection) obj) {
                    convertObject(sb, o);
                }
            } else if (obj instanceof IRecipe) {
                sb.add("Embedded Recipe:\n\n");
                addRecipe(sb, obj);
            } else {
                try {
                    addRecipeWithReflection(sb, obj);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            recursion--;
        }
    }

}
