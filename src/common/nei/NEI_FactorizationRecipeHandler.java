package factorization.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class NEI_FactorizationRecipeHandler implements IConfigureNEI {

    @Override
    public void loadConfig() {
        put(new RecipeCrystallizer());
        put(new RecipeGrinder());
        put(new RecipeMixer());
        put(new RecipeSlagFurnace());
    }
    
    void put(TemplateRecipeHandler it) {
        API.registerRecipeHandler(it);
        API.registerUsageHandler(it);
    }

    @Override
    public String getName() {
        return "FZ Recipes";
    }

    @Override
    public String getVersion() {
        return "1";
    }

}
