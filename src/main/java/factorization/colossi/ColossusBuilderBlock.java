package factorization.colossi;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class ColossusBuilderBlock {
    public final IBlockState bs;

    public ColossusBuilderBlock(IBlockState bs) {
        this.bs = bs;
    }
    
    public boolean matches(Coord at) {
        return at.is(bs);
    }
}
