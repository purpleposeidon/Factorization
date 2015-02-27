package factorization.mechanisms;

import factorization.api.datahelpers.DataHelper;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;

import java.io.IOException;

public class TileEntityAnchor extends TileEntityCommon {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ANCHOR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    @Override
    public void putData(DataHelper data) throws IOException {

    }
}
