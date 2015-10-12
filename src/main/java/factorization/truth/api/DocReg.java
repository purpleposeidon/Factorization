package factorization.truth.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class DocReg {

    public static String default_lookup_domain = "factorization";
    public static String default_recipe_domain = "factorization";

    public static void registerGenerator(String name, IDocGenerator gen) {
        generators.put(name, gen);
    }

    public static void registerManwich(IManwich manwichItem) {
        manwiches.add(manwichItem);
    }

    /**
     * @param machineName A localization key for the name of the crafting machine
     * @param recipeList A list of recipes.
     *
     * This method can also be invoked via IMC.
     * Send to mod: "factorization"
     * MessageType: "AddRecipeCategory"
     * Format: "category localization key|reference.to.classContainingRecipes|nameOfStaticFieldIterable"
     * The field's value must either be Iterable or a Map, or it must be an
     * object with a 'getRecipes' method returning Iterable or Map.
     */
    public static void registerRecipeList(String machineName, Iterable recipeList) {
        customRecipes.put(machineName, recipeList);
    }

    public static void registerCommand(String name, ITypesetCommand cmd) {
        commands.put(name, cmd);
    }

    /**
     * This object is able to open the book. It may be null, particularly on servers.
     */
    public static IDocModule module;


    // For internal use only.
    public static final HashMap<String, IDocGenerator> generators = new HashMap<String, IDocGenerator>();
    public static final ArrayList<String> indexed_domains = new ArrayList<String>();
    public static final ArrayList<IManwich> manwiches = new ArrayList<IManwich>();
    public static final TreeMap<String, Iterable> customRecipes = new TreeMap<String, Iterable>();
    public static final HashMap<String, ITypesetCommand> commands = new HashMap<String, ITypesetCommand>();
}
