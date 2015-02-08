package factorization.common;

import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlastedBedrock extends Block {
    protected BlastedBedrock() {
        super(Material.rock);
        setBlockUnbreakable();
        setResistance(6000000);
        setCreativeTab(Core.tabFactorization);
        setBlockName("factorization:blasted_bedrock");
        setBlockTextureName("factorization:blasted_bedrock");
    }
}
