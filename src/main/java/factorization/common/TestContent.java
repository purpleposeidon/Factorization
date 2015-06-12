package factorization.common;

import factorization.api.Coord;
import factorization.api.crafting.CraftingManagerGeneric;
import factorization.api.crafting.IVexatiousCrafting;
import factorization.crafting.TileEntityMixer;
import factorization.notify.Notice;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class TestContent {
    public static void add() {
        CraftingManagerGeneric<TileEntityCrystallizer> crys = CraftingManagerGeneric.get(TileEntityCrystallizer.class);
        crys.add(new TileEntityCrystallizer.CrystalRecipe(new ItemStack(Items.gunpowder), new ItemStack(Items.gunpowder), 1, new ItemStack(Items.lava_bucket)) {
            {
                heat_amount = 1200;
                cool_time = 60;
            }
            @Override
            public boolean matches(TileEntityCrystallizer machine) {
                return super.matches(machine);
            }

            @Override
            public void onCraftingStart(TileEntityCrystallizer machine) {
                super.onCraftingStart(machine);
                new Notice(machine, "Warning! Explosions incoming!").sendToAll();
            }

            @Override
            public void onCraftingComplete(TileEntityCrystallizer machine) {
                EntityTNTPrimed tnt = new EntityTNTPrimed(machine.getWorldObj());
                machine.getCoord().setAsEntityLocation(tnt);
                tnt.worldObj.spawnEntityInWorld(tnt);
            }
        });


        CraftingManagerGeneric<TileEntityMixer> mix = CraftingManagerGeneric.get(TileEntityMixer.class);
        mix.add(new IVexatiousCrafting<TileEntityMixer>() {
            boolean is(ItemStack is, Block b) {
                if (is == null) return false;
                return DataUtil.getBlock(is) == b;
            }

            boolean is(ItemStack is, Item i) {
                if (is == null) return false;
                return is.getItem() == i;
            }

            @Override
            public boolean matches(TileEntityMixer machine) {
                ItemStack n0 = machine.getStackInSlot(0);
                ItemStack n1 = machine.getStackInSlot(1);
                boolean has_cobble = is(n0, Blocks.cobblestone) || is(n1, Blocks.cobblestone);
                boolean has_lava = is(n0, Items.lava_bucket) || is(n1, Items.lava_bucket);
                return has_cobble && has_lava;
            }

            @Override
            public void onCraftingStart(TileEntityMixer machine) {
                new Notice(machine, "Warning! Meltdown imminent!").sendToAll();
            }

            @Override
            public void onCraftingComplete(TileEntityMixer machine) {
                new Coord(machine).setId(Blocks.lava);
            }

            @Override
            public boolean isUnblocked(TileEntityMixer machine) {
                return true;
            }
        });

    }
}
