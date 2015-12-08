package factorization.colossi;

import factorization.api.Coord;
import net.minecraft.block.Block;

public class ColossusBuilderBlock {
    public final Block block;
    public final int md;
    
    public ColossusBuilderBlock(Block block, int md) {
        this.block = block;
        this.md = md;
    }
    
    public boolean matches(Coord at) {
        return at.getBlock() == block && at.getMd() == md;
    }
}
