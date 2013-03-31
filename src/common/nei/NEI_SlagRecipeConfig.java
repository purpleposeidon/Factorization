package factorization.nei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.client.gui.GuiSlag;
import factorization.common.Core;
import factorization.common.TileEntitySlagFurnace;
import factorization.common.TileEntitySlagFurnace.SmeltingResult;

public class NEI_SlagRecipeConfig extends TemplateRecipeHandler implements IConfigureNEI {
    @Override
    public void loadConfig() {
        API.registerRecipeHandler(this);
        API.registerUsageHandler(this);
    }

    @Override
    public String getName() {
        return "factorization slag furnace recipes";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (SmeltingResult sr : TileEntitySlagFurnace.SlagRecipes.smeltingResults) {
            if (result == null || result.isItemEqual(sr.output1) || result.isItemEqual(sr.output2)) {
                arecipes.add(new CachedSlagRecipe(sr));
            }
        }
    }
    
    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("slagging")) {
            loadCraftingRecipes(null);
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }
    
    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        ItemStack ingredient;
        if (inputId.equals("slagging")) {
            ingredient = null;
        } else if (inputId.equals("item")) {
            ingredient = (ItemStack) ingredients[0];
        } else {
            return;
        } 
        for (SmeltingResult sr : TileEntitySlagFurnace.SlagRecipes.smeltingResults) {
            if (ingredient == null || ingredient.isItemEqual(sr.input)) {
                arecipes.add(new CachedSlagRecipe(sr));
            }
        }
    }

    class CachedSlagRecipe extends CachedRecipe {
        SmeltingResult sr;

        public CachedSlagRecipe(SmeltingResult rs) {
            this.sr = rs;
        }

        @Override
        public PositionedStack getResult() {
            return null; // bebna
        }

        @Override
        public PositionedStack getIngredient() {
            return new PositionedStack(sr.input, 56 - 5, 17 - 11);
        }

        @Override
        public ArrayList<PositionedStack> getOtherStacks() {
            ArrayList<PositionedStack> ret = new ArrayList();
            int h = 11;
            int w = 109;
            ret.add(new PositionedStack(sr.output2, w, h));
            ret.add(new PositionedStack(sr.output1, w, h+26));
            ItemStack f = FurnaceRecipeHandler.afuels.get((cycleticks/48) % FurnaceRecipeHandler.afuels.size()).stack.item;
            ret.add(new PositionedStack(f, 56 - 5, 42));
            return ret;
        }

    }
    
    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> currenttip, int recipe) {
        if (stack == null) {
            return currenttip;
        }
        SmeltingResult sr = ((CachedSlagRecipe)arecipes.get(recipe)).sr;
        float probability = 0, leftProb = 0, rightProb = 0;
        int c = 0;
        if (sr.output1 != null && stack.isItemEqual(sr.output1)) {
            leftProb = (stack.stackSize - 1) * 100 + sr.prob1*100;
            c++;
        }
        if (sr.output2 != null && stack.isItemEqual(sr.output2)) {
            rightProb = (stack.stackSize - 1) * 100 + sr.prob2*100;
            c++;
        }
        int prob = (int)(leftProb + rightProb);
        if (prob == 0) {
            return currenttip; 
        }
        if (c == 2) {
            currenttip.add(prob + "% (both slots)");
        } else {
            currenttip.add(prob + "%");
        }
        return currenttip;
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "slagging"));
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiSlag.class;
    }

    @Override
    public String getRecipeName() {
        return "Slag Furnace";
    }

    @Override
    public String getGuiTexture() {
        return Core.gui_dir + "slagfurnacegui.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "slagging";
    }
}
