package factorization.colossi;

import net.minecraft.block.Block;
import factorization.api.Coord;

public class BlockState {
    public final Block block;
    public final int md;
    
    public BlockState(Block block, int md) {
        this.block = block;
        this.md = md;
    }
    
    public boolean matches(Coord at) {
        return at.getBlock() == block && at.getMd() == md;
    }
}
