package factorization.mechanisms;

import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;

public class TileEntityAnchor extends TileEntityCommon {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ANCHOR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }
}
