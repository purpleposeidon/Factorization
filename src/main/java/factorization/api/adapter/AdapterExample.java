package factorization.api.adapter;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

@SuppressWarnings("unused")
class AdapterExample {
    interface ISparkly {
        InterfaceAdapter<Block, ISparkly> adapter = InterfaceAdapter.get(ISparkly.class);

        int getSparklePower(World w, BlockPos pos);
    }

    class BlockVampire extends Block implements ISparkly {
        public BlockVampire(Material shiny) {
            super(shiny);
        }

        @Override
        public int getSparklePower(World w, BlockPos pos) {
            return Integer.MAX_VALUE;
        }
    }

    void init() {
        ISparkly.adapter.register(Block.class, new ISparkly() {
            @Override
            public int getSparklePower(World w, BlockPos pos) {
                Block b = w.getBlockState(pos).getBlock();
                if (b == Blocks.diamond_block) return 100;
                if (b == Blocks.coal_block) return -10;
                return 0;
            }
        });
        final ISparkly sparkly_inventory = new ISparkly() {
            @Override
            public int getSparklePower(World w, BlockPos pos) {
                TileEntity te = w.getTileEntity(pos);
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

    public static void printSparkliness(World w, BlockPos pos) {
        ISparkly sparkly = ISparkly.adapter.cast(w.getBlockState(pos).getBlock());
        int sparklePower = 0;
        if (sparkly != null) {
            sparklePower = sparkly.getSparklePower(w, pos);
        }
        System.out.println("The sparkle power is " + sparklePower);
    }
}
