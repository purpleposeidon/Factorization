package factorization.charge;

import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.SimpleFzBlock;
import net.minecraft.util.EnumWorldBlockLayer;

public class BlockFurnaceHeater extends SimpleFzBlock {
    public BlockFurnaceHeater() {
        super(Core.registry.materialMachine, FactoryType.HEATER);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.CUTOUT;
    }
}
