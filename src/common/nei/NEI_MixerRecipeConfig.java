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
import factorization.client.gui.GuiSlag;
import factorization.common.Core;
import factorization.common.TileEntityMixer;
import factorization.common.TileEntityMixer.MixRecipe;
import factorization.common.TileEntitySlagFurnace;
import factorization.common.TileEntitySlagFurnace.SlagRecipes;
import factorization.common.TileEntitySlagFurnace.SmeltingResult;

public class NEI_MixerRecipeConfig extends TemplateRecipeHandler implements IConfigureNEI {
    @Override
    public void loadConfig() {
        API.registerRecipeHandler(this);
    }

    @Override
    public String getName() {
        return "factorization mixer recipes";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        //XXX NOTE: This is probably a lame implementation of this function.
        outerloop: for (MixRecipe mr : TileEntityMixer.recipes) {
            if (result == null) {
                arecipes.add(new CachedMixerRecipe(mr));
                continue outerloop;
            }
            for (ItemStack is : mr.outputs) {
                if (result.isItemEqual(is)) {
                    arecipes.add(new CachedMixerRecipe(mr));
                    continue outerloop;
                }
            }
            for (ItemStack is : mr.inputs) {
                if (result.isItemEqual(is)) {
                    arecipes.add(new CachedMixerRecipe(mr));
                    continue outerloop;
                }
            }
        }
    }

    class CachedMixerRecipe extends CachedRecipe {
        MixRecipe mr;

        public CachedMixerRecipe(MixRecipe mr) {
            this.mr = mr;
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
            switch (mr.inputs.length) {
            case 4:
                ret.add(new PositionedStack(mr.inputs[3], w + 18, h + 18));
            case 3:
                ret.add(new PositionedStack(mr.inputs[2], w, h + 18));
            case 2:
                ret.add(new PositionedStack(mr.inputs[1], w, h));
            case 1:
                ret.add(new PositionedStack(mr.inputs[0], w + 18, h));
            default:
            } //Huh, ti mi cnino
            w = 107;
            switch (mr.outputs.length) {
            case 4:
                ret.add(new PositionedStack(mr.outputs[3], w + 18, h + 18));
            case 3:
                ret.add(new PositionedStack(mr.outputs[2], w, h + 18));
            case 2:
                ret.add(new PositionedStack(mr.outputs[1], w, h));
            case 1:
                ret.add(new PositionedStack(mr.outputs[0], w + 18, h));
            default:
            }
            return ret;
        }

    }

    @Override
    public void loadTransferRects() {
        // XXX TODO (if this is even actually necessary? What's it do?) (It might give you places to click on to bring up the recipes list.)
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiSlag.class;
    }

    @Override
    public String getRecipeName() {
        return "Mixer";
    }

    @Override
    public String getGuiTexture() {
        return Core.texture_dir + "mixer.png";
    }

    @Override
    public String getOverlayIdentifier() {
        return "mixing";
    }
}
