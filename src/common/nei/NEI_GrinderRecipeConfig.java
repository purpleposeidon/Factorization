package factorization.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.GuiContainer;
import net.minecraft.src.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.client.gui.GuiGrinder;
import factorization.client.gui.GuiSlag;
import factorization.common.Core;
import factorization.common.TileEntityGrinder;
import factorization.common.TileEntityGrinder.GrinderRecipe;
import factorization.common.TileEntitySlagFurnace;
import factorization.common.TileEntitySlagFurnace.SlagRecipes;
import factorization.common.TileEntitySlagFurnace.SmeltingResult;

public class NEI_GrinderRecipeConfig extends TemplateRecipeHandler implements IConfigureNEI {
    @Override
    public void loadConfig() {
        API.registerRecipeHandler(this);
    }

    @Override
    public String getName() {
        return "factorization grinder recipes";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (GrinderRecipe gr : TileEntityGrinder.recipes) {
            if (result == null || result.isItemEqual(gr.output)) {
                arecipes.add(new CachedGrinderRecipe(gr));
            }
        }
    }
    
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        // TODO Auto-generated method stub
        super.loadUsageRecipes(ingredient);
    }

    class CachedGrinderRecipe extends CachedRecipe {
        GrinderRecipe gr;

        CachedGrinderRecipe(GrinderRecipe gr) {
            this.gr = gr;
        }

        @Override
        public PositionedStack getResult() {
            return new PositionedStack(gr.output, 111, 24);
        }

        @Override
        public PositionedStack getIngredient() {
            return new PositionedStack(gr.input, 51, 24);
        }

    }
    
    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> currenttip, int recipe) {
        if (stack == null) {
            return currenttip;
        }
        GrinderRecipe gr = ((CachedGrinderRecipe)arecipes.get(recipe)).gr;
        float prob = 0;
        if (gr.output != null && stack.isItemEqual(gr.output)) {
            prob = (stack.stackSize - 1) * 100 + gr.probability*100;
        }
        if (prob != 0) {
            currenttip.add(((int)prob) + "%"); 
        }
        return currenttip;
    }

    @Override
    public void loadTransferRects() {
        // XXX TODO (if this is even actually necessary? What's it do?) (It might give you places to click on to bring up the recipes list.)
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiGrinder.class;
    }

    @Override
    public String getRecipeName() {
        return "Grinder";
    }

    @Override
    public String getGuiTexture() {
        return Core.texture_dir + "grinder.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "grinding";
    }
}
