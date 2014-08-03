package factorization.colossi;

import net.minecraft.block.Block;

public class BlockState {
    public final Block block;
    public final int md;
    
    public BlockState(Block block, int md) {
        this.block = block;
        this.md = md;
    }
}
