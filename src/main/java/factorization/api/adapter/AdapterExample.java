package factorization.api.adapter;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class AdapterExample {
    public interface ISparkly {
        InterfaceAdapter<Block, ISparkly> adapter = new InterfaceAdapter<Block, ISparkly>(ISparkly.class);

        int getSparklePower(World w, int x, int y, int z);
    }

    public class BlockVampire extends Block implements ISparkly {

        public BlockVampire(Material shiny) {
            super(shiny);
        }

        @Override
        public int getSparklePower(World w, int x, int y, int z) {
            return Integer.MAX_VALUE;
        }
    }

    public void init() {
        ISparkly.adapter.register(Block.class, new ISparkly() {
            @Override
            public int getSparklePower(World w, int x, int y, int z) {
                Block b = w.getBlock(x, y, z);
                if (b == Blocks.diamond_block) return 100;
                if (b == Blocks.coal_block) return -10;
                return 0;
            }
        });
    }
}
