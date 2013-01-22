package factorization.api.recipe;

import java.util.HashMap;

public class FZRecipes {
    private static HashMap<String, GenericRecipeManager> managers = new HashMap();
    public static GenericRecipeManager get(String machineName) {
        if (managers.containsKey(machineName)) {
            return managers.get(machineName);
        }
        GenericRecipeManager ret = new GenericRecipeManager();
        managers.put(machineName, ret);
        return ret;
    }
}
