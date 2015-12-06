package factorization.colossi;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemGargantuanBlock extends ItemBlock {

    public ItemGargantuanBlock(Block block) {
        super(block);
        setMaxStackSize(32);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing dir, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (dir == null) return false;
        Coord me = new Coord(world, pos);
        Coord mate = me.add(dir);
        if (!mate.isReplacable()) return false;
        if (!world.canBlockBePlaced(block, mate.toBlockPos(), false, dir, player, stack)) return false;
        AxisAlignedBB box = mate.aabbFromRange(mate, mate.add(1, 1, 1));
        if (!world.checkNoEntityCollision(box)) return false;
        IBlockState childState = newState.withProperty(GargantuanBlock.FACE, newState.getValue(GargantuanBlock.FACE).getOpposite());
        boolean ret = super.placeBlockAt(stack, player, world, me.toBlockPos(), dir, hitX, hitY, hitZ, childState);
        if (!ret) return false;
        return super.placeBlockAt(stack, player, world, pos, dir, hitX, hitY, hitZ, newState);
    }
}
