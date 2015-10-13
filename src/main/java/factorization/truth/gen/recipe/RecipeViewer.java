package factorization.truth.gen.recipe;

import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import factorization.shared.Core;
import factorization.truth.ClientTypesetter;
import factorization.truth.DocumentationModule;
import factorization.truth.api.*;
import factorization.truth.word.ItemWord;
import factorization.truth.word.Word;
import factorization.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.OreDictionary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class RecipeViewer implements IDocGenerator, IObjectWriter<Object> {
    HashMap<String, ArrayList<ArrayList>> recipeCategories = null;
    ArrayList<String> categoryOrder = new ArrayList<String>();
    
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
    public void process(ITypesetter out, String arg) throws TruthError {
        StandardObjectWriters.setup();
        if (recipeCategories == null || (Core.dev_environ && !Boolean.getBoolean("fz.devNoRecipeRefresh"))) {
            categoryOrder.clear();
            recipeCategories = new HashMap<String, ArrayList<ArrayList>>();
            Core.logInfo("Loading recipe list");
            loadRecipes();
            Core.logInfo("Done");
        }
        if (arg == null || arg.equalsIgnoreCase("categories") || arg.isEmpty()) {
            out.write("\\title{Recipe Categories}\n\n");
            for (String cat : categoryOrder) {
                out.write(String.format("\\link{cgi/recipes/category/%s}{\\local{%s}}\\nl", cat, cat));
            }
        } else if (arg.startsWith("category/")) {
            String cat = arg.replace("category/", "");
            if (recipeCategories.containsKey(cat)) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, null, false, cat, recipeList);
            } else {
                throw new TruthError("Category not found: " + arg);
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
                    throw new TruthError("Couldn't find item: " + arg);
                }
                //out.emitWord(new ItemWord(matching));
                out.write("\\nl");
            }
            
            for (String cat : categoryOrder) {
                ArrayList<ArrayList> recipeList = recipeCategories.get(cat);
                writeRecipes(out, matching, mustBeResult, cat, recipeList);
            }
            
        }
    }
    
    void writeRecipes(ITypesetter out, ItemStack matching, boolean mustBeResult, String categoryName, ArrayList<ArrayList> recipes) throws TruthError {
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
                            out.write("\\u{\\local{" + categoryName + "}}\n\n");
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
    
    void writeRecipe(ITypesetter out, ArrayList parts) {
        try {
            ClientTypesetter cout = (ClientTypesetter) out;
            for (Object part : parts) {
                if (part instanceof String) {
                    out.write((String) part);
                } else {
                    cout.write((Word) part);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

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
            }
        }
        if (obj instanceof Map) {
            obj = ((Map) obj).entrySet();
        }
        if (obj instanceof Iterable) {
            DocReg.registerRecipeList(key, (Iterable) obj);
        } else {
            Core.logWarning("Unable to load recipe list provided by IMC message, obtained object is neither Iterable nor Map: " + message.getStringValue());
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
        
        for (Entry<String, Iterable> entry : DocReg.customRecipes.entrySet()) {
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
    private ArrayList<ArrayList> addAll(Iterable list) {
        ArrayList<ArrayList> generated = new ArrayList<ArrayList>();
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

    static int MAX_RECURSION = 4;

    @Override
    public void writeObject(List out, Object val, IObjectWriter<Object> generic) {
        if (recursion > MAX_RECURSION) return;
        recursion++;
        try {
            addRecipe(out, val);
        } finally {
            recursion--;
        }
    }

    public void addRecipe(List out, Object obj) {
        if (recursion == 0) {
            out.add("\\seg");
        }
        if (obj instanceof IRecipe) {
            genericRecipePrefix(out, (IRecipe) obj);
        }
        int origLen = out.size();
        IObjectWriter writer = IObjectWriter.adapter.cast(obj);
        writer.writeObject(out, obj, this);
        if (out.size() == origLen) {
            out.add(obj.toString());
        }
        if (recursion == 0) {
            out.add("\\endseg");
            out.add("\\nl");
        }
    }
    
    public static Object genericRecipePrefix(List sb, IRecipe recipe) {
        ItemStack output = recipe.getRecipeOutput();
        if (output == null) return null;
        if (output.getItem() == null) return null;
        sb.add(new ItemWord(output));
        sb.add(" \\b{" + getDisplayName(output) + "}\\vpad{15}\\nl");
        return output;
    }

}
