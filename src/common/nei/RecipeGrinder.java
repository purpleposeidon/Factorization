package factorization.nei;

import java.awt.Rectangle;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.client.gui.GuiGrinder;
import factorization.common.Core;
import factorization.common.TileEntityGrinder;
import factorization.common.TileEntityGrinder.GrinderRecipe;

public class RecipeGrinder extends TemplateRecipeHandler {
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
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("fz.grinding")) {
            loadCraftingRecipes(null);
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }
    
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (GrinderRecipe gr : TileEntityGrinder.recipes) {
            if (ingredient == null) {
                arecipes.add(new CachedGrinderRecipe(gr));
            }
            for (ItemStack is : gr.getInput()) {
                if (ingredient.isItemEqual(is)) {
                    arecipes.add(new CachedGrinderRecipe(gr));
                    break;
                }
            }
        }
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
            return new PositionedStack(gr.getInput(), 51, 24);
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
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "fz.grinding"));
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
        return Core.gui_nei + "grinder.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "fz.grinding";
    }
}
