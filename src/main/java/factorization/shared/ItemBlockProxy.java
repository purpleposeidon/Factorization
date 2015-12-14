package factorization.shared;

import factorization.shared.Core.TabType;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemBlockProxy extends ItemFactorization {
    //TODO: Why doesn't this just extend ItemBlock?
    ItemStack proxy;
    Block theBlock;
    ItemBlock theBlockItem;

    public ItemBlockProxy(Block theBlock, ItemStack proxy, String name, TabType tabType) {
        super(name, tabType);
        this.proxy = proxy.copy();
        this.theBlock = theBlock;
        theBlockItem = (ItemBlock) DataUtil.getItem(theBlock);
    }

    public ItemBlockProxy(ItemStack proxy, String name, TabType tabType) {
        this(Block.getBlockFromItem(proxy.getItem()), proxy, name, tabType);
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float hitX, float hitY, float hitZ, IBlockState bs) {
        proxy.stackSize = stack.stackSize;
        proxy.setTagCompound(stack.getTagCompound());
        boolean ret = theBlockItem.placeBlockAt(proxy, player, world, pos, side, hitX, hitY, hitZ, bs);
        stack.stackSize = proxy.stackSize;
        return ret;
    }

    //NOTE: Copied from ItemBlock, *EXCEPT* that in the final check, I've changed the AABB check is done w/ null instead of the player.
    //Why is that even necessary...?
    //TODO: Fix this stupidity. Seems like every other release it gets messed up.
    //Ah! Might have something to do with clicking on blocks that have an incorrect return. At one point clicking on colossal blocks made it place even if you were in front...

    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        IBlockState origBS = world.getBlockState(pos);
        Block block = origBS.getBlock();

        if (!block.isReplaceable(world, pos)) {
            pos = pos.offset(side);
        }

        if (stack.stackSize == 0) {
            return false;
        } else if (!player.canPlayerEdit(pos, side, stack)) {
            return false;
        } else if (world.canBlockBePlaced(theBlock, pos, false, side, null, stack)) {
            int metadata = this.getMetadata(stack.getMetadata());
            IBlockState newBS = theBlock.onBlockPlaced(world, pos, side, hitX, hitY, hitZ, metadata, player);

            if (placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newBS)) {
                world.playSoundEffect(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, theBlock.stepSound.getPlaceSound(), (theBlock.stepSound.getVolume() + 1.0F) / 2.0F, theBlock.stepSound.getFrequency() * 0.8F);
                world.playSoundEffect((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F), (double) ((float) pos.getZ() + 0.5F), theBlock.stepSound.getPlaceSound(), (theBlock.stepSound.getVolume() + 1.0F) / 2.0F, theBlock.stepSound.getFrequency() * 0.8F);
                --stack.stackSize;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemId, CreativeTabs tab, List list) {
    } //The items will be added elsewhere
}
