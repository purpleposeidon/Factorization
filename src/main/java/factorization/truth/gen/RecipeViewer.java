package factorization.truth.gen;

import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.truth.*;
import factorization.truth.word.ItemWord;
import factorization.truth.word.TextWord;
import factorization.truth.word.Word;
import factorization.shared.Core;
import factorization.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

public class RecipeViewer implements IDocGenerator {
    HashMap<String, ArrayList<ArrayList>> recipeCategories = null;
    ArrayList<String> categoryOrder = new ArrayList();
    HashMap<List<ItemStack>, String> reverseOD = new HashMap<List<ItemStack>, String>();
    
    /** USAGE
     * recipe/
     * recipe/categories
     * 		Lists available recipe categories
     * 
     * recipe/category/CATEGORY
     * 		Lists recipes of that category
     * 
     * recipe/all
     * 		Lists all recipes
     * 
     * recipe/itemName
     * 		Lists recipes that use the itemName.
     * 
     * recipe/for/itemName
     * 		Lists recipes whose output is the itemName
     */
    @Override
    public void process(AbstractTypesetter out, String arg) {
        if (recipeCategories == null || Core.dev_environ) {
            categoryOrder.clear();
            recipeCategories = new HashMap();
            reverseOD.clear();
            Core.logInfo("Loading recipe list");
            setupReverseOD();
            loadRecipes();
            Core.logInfo("Done");
        }
        if (arg == null || arg.equalsIgnoreCase("categories") || arg.isEmpty()) {
            out.append("\\title{Recipe Categories}\n\n");
            for (String cat : categoryOrder) {
                out.append(String.format("\\link{cgi/recipes/category/%s}{\\local{%s}}\\nl", cat, cat));
            }
        } else if (arg.startsWith("category/")) {
            String cat = arg.replace("category/", "");
            if (recipeCategories.containsKey(cat)) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, null, false, cat, recipeList);
            } else {
                out.error("Category not found: " + arg);
            }
        } else {
            ItemStack matching = null;
            boolean mustBeResult = false;
            if (!arg.equalsIgnoreCase("all")) {
                if (arg.startsWith("for/")) {
                    mustBeResult = true;
                    arg = arg.replace("for/", "");
                }
                ArrayList<ItemStack> matchers = DocumentationModule.getNameItemCache().get(arg);
                if (matchers != null && !matchers.isEmpty()) {
                    matching = matchers.get(0);
                }
                if (matching == null) {
                    out.error("Couldn't find item: " + arg);
                    return;
                }
                //out.emitWord(new ItemWord(matching));
                out.append("\\nl");
            }
            
            for (String cat : categoryOrder) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, matching, mustBeResult, cat, recipeList);
            }
            
        }
    }
    
    void writeRecipes(AbstractTypesetter out, ItemStack matching, boolean mustBeResult, String categoryName, ArrayList<ArrayList> recipes) {
        if (matching == null) {
            for (ArrayList recipe : recipes) {
                writeRecipe(out, recipe);
            }
        } else {
            boolean first = true;
            for (ArrayList recipe : recipes) {
                if (recipeMatches(recipe, matching, mustBeResult)) {
                    if (first) {
                        first = false;
                        if (categoryName != null) {
                            out.append("\\u{\\local{" + categoryName + "}}\n\n");
                        }
                    }
                    writeRecipe(out, recipe);
                }
            }
        }
    }
    
    boolean recipeMatches(ArrayList recipe, ItemStack matching, boolean mustBeResult) {
        for (Object part : recipe) {
            if (part instanceof ItemWord) {
                ItemWord iw = (ItemWord) part;
                if (iw.is != null) {
                    if (ItemUtil.identical(iw.is, matching) || ItemUtil.wildcardSimilar(iw.is, matching)) {
                        return true;
                    }
                }
                if (iw.entries != null) {
                    for (ItemStack is : iw.entries) {
                        if (ItemUtil.identical(is, matching) || ItemUtil.wildcardSimilar(is, matching)) {
                            return true;
                        }
                    }
                }
                if (mustBeResult) {
                    return false;
                }
            }
        }
        return false;
    }
    
    void writeRecipe(AbstractTypesetter out, ArrayList parts) {
        try {
            for (Object part : parts) {
                if (part instanceof String) {
                    out.append((String) part);
                } else {
                    out.emitWord((Word) part);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    static TreeMap<String, Iterable> customRecipes = new TreeMap<String, Iterable>();
    static TreeMap<String, Map> customRecipesMap = new TreeMap<String, Map>();
    public static void handleImc(IMCMessage message) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if (!message.key.equals("AddRecipeCategory")) return;
        String[] cmd = message.getStringValue().split("\\|");
        String key = cmd[0];
        String className = cmd[1];
        String fieldName = cmd[2];
        Class kl = RecipeViewer.class.getClassLoader().loadClass(className);
        Field field = kl.getField(fieldName);
        Object obj = field.get(null);
        if (!(obj instanceof Iterable)) {
            String[] getter_names = new String[] {"getRecipes", "recipes", "allRecipes", "getAllRecipes"};
            Object found = null;
            Class cl = obj.getClass();
            for (String name : getter_names) {
                try {
                    Method getter = cl.getMethod(name);
                    found = getter.invoke(obj);
                    if (found != null) break;
                } catch (Throwable ignored) {

                }
            }
            if (found != null) {
                obj = found;
            } else {
                return;
            }
        }
        if (obj instanceof Map) {
            obj = ((Map) obj).entrySet();
        }
        if (obj instanceof Iterable) {
            customRecipes.put(key, (Iterable) obj);
        } else {
            Core.logWarning("Unable to load recipe list provided by IMC message, obtained object is neither Iterable nor Map: " + message.getStringValue());
        }
    }

    void setupReverseOD() {
        for (String name : OreDictionary.getOreNames()) {
            List<ItemStack> entries = OreDictionary.getOres(name);
            reverseOD.put(entries, name);
        }
    }

    void loadRecipes() {
        putCategory("Workbench", CraftingManager.getInstance().getRecipeList());
        putCategory("Furnace", FurnaceRecipes.smelting().getSmeltingList().entrySet());
        HashMap ores = new HashMap();
        for (String name : OreDictionary.getOreNames()) {
            ores.put("\"" + name + "\"", OreDictionary.getOres(name));
        }
        putCategory("Ore Dictionary", ores.entrySet());
        
        for (Entry<String, Iterable> entry : customRecipes.entrySet()) {
            putCategory(entry.getKey(), entry.getValue());
        }
    }
    
    void putCategory(String label, Iterable list) {
        try {
            recipeCategories.put(label, addAll(list));
            categoryOrder.add(label);
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
        if (obj instanceof IRecipe) {
            genericRecipePrefix(sb, (IRecipe) obj);
        }
        int origLen = sb.size();
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
        if (sb.size() == origLen) {
            sb.add(obj.toString());
        }
        sb.add("\\endseg");
        sb.add("\\nl");
    }
    
    Object genericRecipePrefix(List sb, IRecipe recipe) {
        ItemStack output = ((IRecipe) recipe).getRecipeOutput();
        if (output == null) return null;
        if (output.getItem() == null) return null;
        sb.add(new ItemWord(output));
        sb.add(" \\b{" + getDisplayName(output) + "}\\vpad{15}\\nl");
        return output;
    }
    
    static ItemStack fixMojangRecipes(ItemStack is) {
        if (is == null) return null;
        if (is.stackSize > 1) {
            is = is.copy();
            is.stackSize = 1;
        }
        return is;
    }
    
    void addShapedOreRecipe(List sb, ShapedOreRecipe recipe) {
        int width = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, recipe, "width");
        //int height = ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, recipe, "height");
        Object[] input = recipe.getInput();
        int i = 0;
        for (Object in : input) {
            if (in instanceof ItemStack || in == null) {
                ItemStack is = (ItemStack) in;
                sb.add(new ItemWord(fixMojangRecipes(is)));
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
                sb.add("\\nl");
            } else {
                //sb.add(" ");
            }
        }
    }
    
    void addShapedRecipes(List sb, ShapedRecipes recipe) {
        int width = recipe.recipeWidth;
        for (int i = 0; i < recipe.recipeItems.length; i++) {
            sb.add(new ItemWord(fixMojangRecipes(recipe.recipeItems[i])));
            if ((i + 1) % width == 0) {
                sb.add("\\nl");
            } else {
                //sb.add(" ");
            }
        }
    }
    
    void addShapelessOreRecipe(List sb, ShapelessOreRecipe recipe) {
        ArrayList<Object> input = recipe.getInput();
        if (input == null) return;
        sb.add("Shapeless: ");
        for (Object obj : input) {
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
        if (recipe.recipeItems == null) return;
        sb.add("Shapeless: ");
        for (Object obj : recipe.recipeItems) {
            convertObject(sb, obj);
        }
    }
    
    void addRecipeWithReflection(List sb, Object recipe) throws IllegalArgumentException, IllegalAccessException {
        if (recipe instanceof ItemStack || recipe instanceof String || recipe instanceof Number || recipe.getClass().isArray() || recipe instanceof Collection || recipe instanceof NBTBase) {
            convertObject(sb, recipe);
            return;
        }
        Object output = RecipeViewer.class; //Just something that isn't null.
        if (recipe instanceof IRecipe) {
            output = genericRecipePrefix(sb, (IRecipe) recipe);
        } else if (recipe instanceof Entry) {
            Entry ent = (Entry) recipe;
            addRecipeWithReflection(sb, ent.getKey());
            sb.add(" ➤ ");
            addRecipeWithReflection(sb, ent.getValue());
            return;
        }
        Field[] fields = recipe.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().contains("$")) continue;
            int modifiers = f.getModifiers();
            if ((modifiers & Modifier.STATIC) != 0) continue;
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            Object v = f.get(recipe);
            if (v == output || v == null) continue;
            if (recursion < 2 || v instanceof String || v instanceof ItemStack || v instanceof Collection || v.getClass().isArray() || v instanceof NBTBase) {
                sb.add(f.getName() + ": ");
                convertObject(sb, v);
                sb.add("\\nl ");
            }
        }
    }
    
    HashSet<String> knownOres = new HashSet();
    {
        for (String s : OreDictionary.getOreNames()) knownOres.add(s);
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
        } else if (obj instanceof String) {
            String name = (String) obj;
            if (knownOres.contains(name)) {
                obj = OreDictionary.getOres(name);
            }
        }
        
        recursion++;
        try {
            if (obj instanceof ItemStack) {
                sb.add(new ItemWord((ItemStack) obj));
            } else if (obj instanceof String || obj instanceof Number || obj instanceof NBTBase) {
                sb.add(obj.toString());
            } else if (obj instanceof FluidStack) {
                FluidStack fs = (FluidStack) obj;
                sb.add(FluidViewer.convert(fs.getFluid()));
                sb.add(new TextWord("" + fs.amount, null)); // TODO: Link to fluid viewer
            } else if (obj.getClass().isArray()) {
                Class<?> component = obj.getClass().getComponentType();
                if (component == ItemStack.class) {
                    ItemStack[] array = (ItemStack[]) obj;
                    if (array == null || array.length == 0) {
                        sb.add(new TextWord("<Empty>", null));
                    } else {
                        sb.add(new ItemWord(array));
                    }
                } else {
                    if (component == Object.class) {
                        Object[] listy = (Object[]) obj;
                        for (Object o : listy) {
                            if (o == null) {
                                sb.add("-");
                            } else {
                                convertObject(sb, o);
                            }
                        }
                    } else if (component == ItemStack.class) {
                        ItemStack[] listy = (ItemStack[]) obj;
                        for (ItemStack is : listy) {
                            if (is == null) {
                                sb.add("-");
                            } else {
                                sb.add(new ItemWord(is));
                            }
                        }
                    }
                }
            } else if (obj instanceof Collection) {
                boolean probableOreDictionary = false;
                if (obj instanceof List) {
                    probableOreDictionary = reverseOD.containsKey(obj);
                }
                if (probableOreDictionary) {
                    boolean bad = false;
                    for (Object o : (Collection) obj) {
                        if (!(o instanceof ItemStack)) {
                            bad = true;
                            break;
                        }
                    }
                    if (!bad) {
                        Collection<ItemStack> col = (Collection<ItemStack>) obj;
                        ItemStack[] items = col.toArray(new ItemStack[col.size()]);
                        sb.add(new ItemWord(items));
                        return;
                    }
                }
                for (Object o : (Collection) obj) {
                    convertObject(sb, o);
                }
            } else if (obj instanceof IRecipe) {
                sb.add("Embedded Recipe:\n\n");
                ArrayList sub = new ArrayList();
                addRecipe(sub, obj);
                sb.addAll(sub);
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
