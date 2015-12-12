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
        IBlockState iblockstate = world.getBlockState(pos);
        Block block = iblockstate.getBlock();

        if (!block.isReplaceable(world, pos)) {
            pos = pos.offset(side);
        }

        if (stack.stackSize == 0) {
            return false;
        } else if (!player.canPlayerEdit(pos, side, stack)) {
            return false;
        } else if (world.canBlockBePlaced(theBlock, pos, false, side, null, stack)) {
            int i = this.getMetadata(stack.getMetadata());
            IBlockState iblockstate1 = theBlock.onBlockPlaced(world, pos, side, hitX, hitY, hitZ, i, player);

            if (placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, iblockstate1)) {
                world.playSoundEffect((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F), (double) ((float) pos.getZ() + 0.5F), this.block.stepSound.getPlaceSound(), (this.block.stepSound.getVolume() + 1.0F) / 2.0F, this.block.stepSound.getFrequency() * 0.8F);
                --stack.stackSize;
            }

            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        Block block = par3World.getBlock(par4, par5, par6);

        if (block == Blocks.snow_layer && (par3World.getBlockMetadata(par4, par5, par6) & 7) < 1)
        {
            par7 = 1;
        }
        else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush && !block.isReplaceable(par3World, par4, par5, par6))
        {
            if (par7 == 0)
            {
                --par5;
            }

            if (par7 == 1)
            {
                ++par5;
            }

            if (par7 == 2)
            {
                --par6;
            }

            if (par7 == 3)
            {
                ++par6;
            }

            if (par7 == 4)
            {
                --par4;
            }

            if (par7 == 5)
            {
                ++par4;
            }
        }

        if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else if (par5 == 255 && this.theBlock.getMaterial().isSolid())
        {
            return false;
        }
        else if (par3World.canPlaceEntityOnSide(this.theBlock, par4, par5, par6, false, par7, null, par1ItemStack))
        {
            int i1 = this.getMetadata(par1ItemStack.getItemDamage());
            int j1 = this.theBlock.onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, i1);

            if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, j1))
            {
                par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), this.theBlock.stepSound.func_150496_b(), (this.theBlock.stepSound.getVolume() + 1.0F) / 2.0F, this.theBlock.stepSound.getPitch() * 0.8F);
                --par1ItemStack.stackSize;
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    
    @Override
    public boolean getShareTag() {
        return true;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemId, CreativeTabs tab, List list) { } //The items will be added elsewhere
}
