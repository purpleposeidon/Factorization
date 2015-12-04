package factorization.weird;

import net.minecraftforge.fml.common.registry.GameRegistry;
import factorization.shared.Core;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;

import static factorization.weird.TileEntityDayBarrel.Type.*;

public class BarrelUpgradeRecipes {
    private static final ItemStack oakLog = new ItemStack(Blocks.log);
    private static final ItemStack oakPlank = new ItemStack(Blocks.wooden_slab);
    private static final ItemStack hopper = new ItemStack(Blocks.hopper);
    private static final ItemStack slime_ball = new ItemStack(Items.slime_ball);
    private static final ItemStack web = new ItemStack(Blocks.web);

    public static void addUpgradeRecipes() {
        ItemStack oakBarrel = TileEntityDayBarrel.makeBarrel(NORMAL, oakLog, oakPlank);
        oakBarrel.setItemDamage(OreDictionary.WILDCARD_VALUE);

        RecipeSorter.register("factorization:barrel_upgrade", BarrelUpgrade.class, RecipeSorter.Category.SHAPED, "");

        GameRegistry.addRecipe(new BarrelUpgrade(SILKY, 3, 3, new ItemStack[] {
                web, web, web,
                web, oakBarrel, web,
                web, web, web
        }));

        GameRegistry.addRecipe(new BarrelUpgrade(HOPPING, 1, 3, new ItemStack[] {
                hopper,
                oakBarrel,
                hopper
        }));

        GameRegistry.addRecipe(new BarrelUpgrade(STICKY, 1, 3, new ItemStack[] {
                slime_ball,
                oakBarrel,
                slime_ball
        }));
    }

    public static class BarrelUpgrade extends ShapedRecipes {
        final TileEntityDayBarrel.Type upgradeType;

        public BarrelUpgrade(TileEntityDayBarrel.Type upgrade, int width, int height, ItemStack[] inputs) {
            super(width, height, inputs, TileEntityDayBarrel.makeBarrel(upgrade, oakLog, oakPlank));
            this.upgradeType = upgrade;
        }

        ItemStack grabBarrel(InventoryCrafting container) {
            for (int i = 0; i < container.getSizeInventory(); i++) {
                ItemStack is = container.getStackInSlot(i);
                if (is == null) continue;
                if (is.getItem() != Core.registry.daybarrel) continue;
                return is;
            }
            return null;
        }

        @Override
        public boolean matches(InventoryCrafting container, World world) {
            if (!super.matches(container, world)) return false;
            ItemStack is = grabBarrel(container);
            if (is == null) return false;
            TileEntityDayBarrel rep = new TileEntityDayBarrel();
            rep.loadFromStack(is);
            return rep.type == TileEntityDayBarrel.Type.NORMAL;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting container) {
            ItemStack is = grabBarrel(container);
            if (is == null) return super.getCraftingResult(container); // Shouldn't happen?
            TileEntityDayBarrel rep = new TileEntityDayBarrel();
            rep.loadFromStack(is);
            rep.type = upgradeType;
            return rep.getPickedBlock();
        }
    }

}
