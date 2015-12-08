package factorization.common;

import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.IBlockAccess;

import java.util.List;

public class BlockResource extends Block {
    enum ResourceTypes implements IStringSerializable, Comparable<ResourceTypes> {
        OLD_SILVER_ORE, SILVER_BLOCK, LEAD_BLOCK, DARK_IRON_BLOCK;

        @Override
        public String getName() {
            return name();
        }

        public boolean isMetal() {
            return this == SILVER_BLOCK || this == LEAD_BLOCK || this == DARK_IRON_BLOCK;
        }
    }

    public static final IProperty<ResourceTypes> TYPE = PropertyEnum.create("type", ResourceTypes.class);

    protected BlockResource() {
        super(Material.rock);
        setHardness(2.0F);
        setUnlocalizedName("factorization.ResourceBlock");
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, TYPE);
    }

    public void addCreativeItems(List<ItemStack> itemList) {
        itemList.add(Core.registry.silver_ore_item);
        itemList.add(Core.registry.silver_block_item);
        itemList.add(Core.registry.lead_block_item);
        itemList.add(Core.registry.dark_iron_block_item);
    }
    
    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> itemList) {
        addCreativeItems(itemList);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, ResourceTypes.values()[meta]);
    }

    @Override
    public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
        IBlockState bs = world.getBlockState(pos);
        return bs.getValue(TYPE).isMetal();
    }
}
