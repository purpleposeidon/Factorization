package factorization.crafting;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemFakeBlock extends ItemFactorization {
    final Block realBlock;
    public ItemFakeBlock(String name, Core.TabType tabType, Block realBlock) {
        super(name, tabType);
        this.realBlock = realBlock;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        Coord at = new Coord(world, pos);
        if (!at.isReplacable()) return false;
        at.setId(realBlock);
        return true;
    }
}
