package factorization.colossi;

import factorization.api.Coord;
import factorization.shared.Core;
import net.minecraft.block.state.IBlockState;

public class ColossusBuilderBlock {
    public final ColossalBlock.Md[] props;

    public ColossusBuilderBlock(ColossalBlock.Md... props) {
        this.props = props;
    }
    
    public boolean matches(Coord at) {
        return at.has(ColossalBlock.VARIANT, props);
    }

    public void set(Coord at, boolean notify) {
        at.set(getState(), notify);
    }

    public IBlockState getState() {
        return Core.registry.colossal_block.getDefaultState().withProperty(ColossalBlock.VARIANT, props[0]);
    }
}
