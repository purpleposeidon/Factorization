package factorization.api.adapter;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@SuppressWarnings("unused")
class AdapterExample {
    interface ISparkly {
        InterfaceAdapter<Block, ISparkly> adapter = InterfaceAdapter.get(ISparkly.class);

        int getSparklePower(World w, int x, int y, int z);
    }

    class BlockVampire extends Block implements ISparkly {
        public BlockVampire(Material shiny) {
            super(shiny);
        }

        @Override
        public int getSparklePower(World w, int x, int y, int z) {
            return Integer.MAX_VALUE;
        }
    }

    void init() {
        ISparkly.adapter.register(Block.class, new ISparkly() {
            @Override
            public int getSparklePower(World w, int x, int y, int z) {
                Block b = w.getBlock(x, y, z);
                if (b == Blocks.diamond_block) return 100;
                if (b == Blocks.coal_block) return -10;
                return 0;
            }
        });
        final ISparkly sparkly_inventory = new ISparkly() {
            @Override
            public int getSparklePower(World w, int x, int y, int z) {
                TileEntity te = w.getTileEntity(x, y, z);
                if (te instanceof IInventory) {
                    IInventory inv = (IInventory) te;
                    ItemStack first = inv.getStackInSlot(0);
                    if (first == null) return 0;
                    return first.getItem() == Items.nether_star ? 10000 : 0;
                }
                return 0;
            }
        };
        ISparkly.adapter.register(new Adapter<Block, ISparkly>() {
            @Override
            public ISparkly adapt(final Block val) {
                return sparkly_inventory;
            }

            @Override
            public boolean canAdapt(Class<?> valClass) {
                return valClass.isAssignableFrom(ITileEntityProvider.class);
            }

            @Override
            public int priority() {
                return 0;
            }
        });
    }

    public static void printSparkliness(World w, int x, int y, int z) {
        ISparkly sparkly = ISparkly.adapter.cast(w.getBlock(x, y, z));
        int sparklePower = 0;
        if (sparkly != null) {
            sparklePower = sparkly.getSparklePower(w, x, y, z);
        }
        System.out.println("The sparkle power is " + sparklePower);
    }
}
