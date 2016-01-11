package factorization.shared;

import factorization.common.FactoryType;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumWorldBlockLayer;

public class SimpleFzBlockCutout extends SimpleFzBlock {
    public SimpleFzBlockCutout(Material material, FactoryType factoryType) {
        super(material, factoryType);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.CUTOUT;
    }
}
