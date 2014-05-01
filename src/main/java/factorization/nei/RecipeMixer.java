package factorization.nei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import factorization.crafting.GuiMixer;
import factorization.crafting.TileEntityMixer;
import factorization.crafting.TileEntityMixer.RecipeMatchInfo;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class RecipeMixer extends TemplateRecipeHandler {
    //FIXME: People keep sending invalid bug reports for the mixer because it can't really merge OD ItemStacks.
    @Override
    public void loadCraftingRecipes(ItemStack result) {
        //XXX NOTE: This is probably a lame implementation of this function.
        for (RecipeMatchInfo mr : getCache()) {
            if (result == null) {
                arecipes.add(new CachedMixerRecipe(mr));
                continue;
            }
            if (result.isItemEqual(mr.output)) {
                arecipes.add(new CachedMixerRecipe(mr));
                continue;
            }
        }
    }
    
    private ArrayList<TileEntityMixer.RecipeMatchInfo> cache;
    
    ArrayList<TileEntityMixer.RecipeMatchInfo> getCache() {
        if (cache == null) {
            cache = TileEntityMixer.getRecipes();
        }
        return cache;
    }
    
    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("fz.mixing")) {
            loadCraftingRecipes(null);
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }
    
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        //XXX NOTE: This is probably a lame implementation of this function.
        List<ItemStack> items = new ArrayList();
        outerloop: for (RecipeMatchInfo mr : getCache()) {
            if (ingredient == null) {
                arecipes.add(new CachedMixerRecipe(mr));
                continue outerloop;
            }
            for (Object o : mr.inputs) {
                items.clear();
                if (o instanceof ItemStack) {
                    items.add((ItemStack) o);
                } else if (o instanceof String) {
                    items.addAll(OreDictionary.getOres((String) o));
                } else if (o instanceof List) {
                    items.addAll((Collection<? extends ItemStack>) o);
                }
                for (ItemStack item : items) {
                    if (ingredient.isItemEqual(item)) {
                        arecipes.add(new CachedMixerRecipe(mr));
                        continue outerloop;
                    }
                }
            }
        }
    }

    class CachedMixerRecipe extends CachedRecipe {
        RecipeMatchInfo recipe;

        public CachedMixerRecipe(RecipeMatchInfo recipe) {
            this.recipe = recipe;
        }

        @Override
        public PositionedStack getResult() {
            return null; // bebna
        }

        @Override
        public PositionedStack getIngredient() {
            return null;
        }

        @Override
        public ArrayList<PositionedStack> getOtherStacks() {
            ArrayList<PositionedStack> ret = new ArrayList();
            int h = 14;
            int w = 33;
            try {
                switch (recipe.inputs.size()) {
                default: //But it's incomplete!
                case 4:
                    ret.add(new PositionedStack(recipe.inputs.get(3), w + 18, h + 18));
                    //$FALL-THROUGH$
                case 3:
                    ret.add(new PositionedStack(recipe.inputs.get(2), w, h + 18));
                    //$FALL-THROUGH$
                case 2:
                    ret.add(new PositionedStack(recipe.inputs.get(1), w, h));
                    //$FALL-THROUGH$
                case 1:
                    ret.add(new PositionedStack(recipe.inputs.get(0), w + 18, h));
                    //$FALL-THROUGH$
                case 0:
                    //$FALL-THROUGH$
                } //Huh, ti mi cnino
            } catch (Exception e) {
                e.printStackTrace();
            }
            w = 107;
            
            ArrayList<ItemStack> output = new ArrayList();
            output.add(recipe.output);
            for (Object o : recipe.inputs) {
                if (o instanceof ItemStack) {
                    ItemStack is = (ItemStack) o;
                    if (is.getItem().hasContainerItem(is)) {
                        ItemStack cnt = FzUtil.normalize(is.getItem().getContainerItem(is));
                        if (cnt != null) {
                            output.add(cnt);
                        }
                    }
                }
            }
            switch (output.size()) {
            default:
            case 4:
                ret.add(new PositionedStack(output.get(3), w + 18, h + 18));
                //$FALL-THROUGH$
            case 3:
                ret.add(new PositionedStack(output.get(2), w, h + 18));
                //$FALL-THROUGH$
            case 2:
                ret.add(new PositionedStack(output.get(1), w + 18, h));
                //$FALL-THROUGH$
            case 1:
                ret.add(new PositionedStack(output.get(0), w, h));
                //$FALL-THROUGH$
            case 0:
                //$FALL-THROUGH$
            }
            return ret;
        }

    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "fz.mixing"));
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiMixer.class;
    }

    @Override
    public String getRecipeName() {
        return "Mixer";
    }

    @Override
    public String getGuiTexture() {
        return Core.gui_nei + "mixer.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "fz.mixing";
    }
}