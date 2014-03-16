package factorization.nei;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.oreprocessing.GuiGrinder;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntityGrinder.GrinderRecipe;
import factorization.shared.Core;
import factorization.shared.FzUtil;

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
        Item ingredientItem = ingredient == null ? null : ingredient.getItem();
        if (FzUtil.couldMerge(ingredient, Core.registry.socket_lacerator)) {
            ingredient = null;
        }
        if (FzUtil.couldMerge(ingredient, Core.registry.empty_socket_item)) {
            ingredient = null;
        }
        if (ingredientItem == Core.registry.motor) {
            ingredient = null;
        }
        if (ingredientItem == Core.registry.diamond_cutting_head) {
            ingredient = null;
        }
        for (GrinderRecipe gr : TileEntityGrinder.recipes) {
            if (ingredient == null) {
                arecipes.add(new CachedGrinderRecipe(gr));
                continue;
            }
            for (ItemStack is : gr.getInput()) {
                if (FzUtil.wildcardSimilar(is, ingredient)) {
                    arecipes.add(new CachedGrinderRecipe(gr));
                    break;
                }
            }
        }
    }

    static final List<PositionedStack> socketBits = Arrays.asList(
            new PositionedStack(new ItemStack(Core.registry.diamond_cutting_head), 78, 24 - 18),
            new PositionedStack(new ItemStack(Core.registry.motor), 78, 24),
            new PositionedStack(Core.registry.empty_socket_item.copy(), 78, 24 + 18)
            );
    
    class CachedGrinderRecipe extends CachedRecipe {
        GrinderRecipe gr;

        CachedGrinderRecipe(GrinderRecipe gr) {
            this.gr = gr;
        }

        @Override
        public PositionedStack getResult() {
            return new PositionedStack(gr.output, 51+18*3, 24);
        }

        @Override
        public PositionedStack getIngredient() {
            return new PositionedStack(gr.getInput(), 51, 24);
        }
        
        @Override
        public List<PositionedStack> getOtherStacks() {
            return socketBits;
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
        for (ItemStack is : gr.getInput()) {
            if (FzUtil.wildcardSimilar(is, stack)) {
                currenttip.add("In a barrel, or as a block");
                break;
            }
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
        return "Lacerator";
    }

    @Override
    public void drawForeground(int recipe) { }
    @Override
    public void drawBackground(int recipe) { }
    
    @Override
    public String getGuiTexture() {
        return "unused";
    }

    @Override
    public String getOverlayIdentifier() {
        return "fz.grinding";
    }
    
    @Override
    public List<String> handleTooltip(GuiRecipe gui, List<String> currenttip,
            int recipe) {
        return currenttip;
    }
}
