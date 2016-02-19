package factorization.shared;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemDynamic extends ItemBlock {
    public ItemDynamic(Block block) {
        super(block);
        //Why do we not have setHasSubtypes(true)? 1.8 rendering is too scary to mess w/ this.
        setMaxDamage(0);
        setNoRepair();
    }

    @Override
    public boolean getShareTag() {
        return true; // (Not actually needed now.)
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) return false;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            tec.onPlacedBy(player, stack, side, hitX, hitY, hitZ);
        }
        return true;
    }
}
