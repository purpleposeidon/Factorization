package factorization.nei;

import static codechicken.core.gui.GuiDraw.changeTexture;
import static codechicken.core.gui.GuiDraw.drawTexturedModalRect;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.oreprocessing.GuiCrystallizer;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityCrystallizer.CrystalRecipe;
import factorization.shared.Core;

public class RecipeCrystallizer extends TemplateRecipeHandler  {
    @Override
    public void loadCraftingRecipes(ItemStack result) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (CrystalRecipe cr : TileEntityCrystallizer.recipes) {
            if (result == null || result.isItemEqual(cr.output)) {
                arecipes.add(new CachedCrystallizerRecipe(cr));
            }
        }
    }
    
    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("fz.crystallizing")) {
            loadCraftingRecipes(null);
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }
    
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (CrystalRecipe cr : TileEntityCrystallizer.recipes) {
            if (ingredient == null
                    || ingredient.isItemEqual(cr.input)
                    || ingredient.isItemEqual(cr.solution)) {
                arecipes.add(new CachedCrystallizerRecipe(cr));
            }
        }
    }

    class CachedCrystallizerRecipe extends CachedRecipe {
        CrystalRecipe cr;

        public CachedCrystallizerRecipe(CrystalRecipe cr) {
            this.cr = cr;
        }

        @Override
        public PositionedStack getResult() {
            return new PositionedStack(cr.output, 75, 29 + 15);
        }

        @Override
        public PositionedStack getIngredient() {
            return null;
        }

        @Override
        public ArrayList<PositionedStack> getOtherStacks() {
            ArrayList<PositionedStack> ret = new ArrayList();
            ret.add(new PositionedStack(cr.input, 75, 2 + 15));
            ret.add(new PositionedStack(cr.solution, 75, 58 + 15));
            ret.add(new PositionedStack(Core.registry.heater_item, 0, 75));
            return ret;
        }

    }
    
    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> currenttip, int recipe) {
        if (stack == null) {
            return currenttip;
        }
        CrystalRecipe cr = ((CachedCrystallizerRecipe)arecipes.get(recipe)).cr;
        if (stack.isItemEqual(cr.output)) {
            float prob = cr.output_count*100;
            if (prob != 100 || true) {
                currenttip.add((int)prob + "%");
            }
        }
        if (stack.isItemEqual(cr.solution)) {
            currenttip.add("Does not get used up");
        }
        if (stack.isItemEqual(cr.input)) {
            currenttip.add("Extra copies of this can be added");
            currenttip.add("to the other slots to increase yield");
        }
        if (stack.isItemEqual(Core.registry.heater_item)) {
            currenttip.add("Use this to heat the crystallizer");
        }
        return currenttip;
    }

    @Override
    public void loadTransferRects() {
        // XXX TODO (if this is even actually necessary? What's it do?) (It might give you places to click on to bring up the recipes list.)
        transferRects.add(new RecipeTransferRect(new Rectangle(35 + 63, 78, 95 - 63, 16), "fz.crystallizing"));
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiCrystallizer.class;		
    }

    @Override
    public String getRecipeName() {
        return "Crystallizer";
    }

    @Override
    public String getGuiTexture() {
        return Core.gui_nei + "crystal.png";
    }
    
    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        changeTexture(getGuiTexture());
        drawTexturedModalRect(0, 0 + 15, 5, 11, 166, 65 + 30);
    }

    @Override
    public String getOverlayIdentifier() {
        return "fz.crystallizing";
    }
    
    /*
    public void drawBackground(GuiContainerManager gui, int recipe)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
        gui.bindTexture(getGuiTexture());
        gui.drawTexturedModalRect(0, 15, 5, 11, 166, 95);
    }*/
    
    @Override
    public void drawExtras(int recipe) {
        super.drawExtras(recipe);
        //drawProgressBar(gui, 43 - 5, 89 + 4, 0, 192, 90, 16, 20*60, 0);
        
        
        //this.drawTexturedModalRect(var5 + 43, var6 + 89, 0, 192, progress, 16);
        //for (int dx : new int[] { 54, 109 }) {
        //	this.drawTexturedModalRect(var5 + dx, var6 + 75 + heat, 176, 0 + heat, 14, 13 - heat);
        //}
        //drawProgressBar(gui, x, y, tx, ty, w, h, completion, direction);
    }
    
    @Override
    public int recipiesPerPage() {
        return 1;
    }
}
