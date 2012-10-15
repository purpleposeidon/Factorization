package factorization.common;

import factorization.api.Coord;
import factorization.common.Core.TabType;
import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumRarity;
import net.minecraft.src.EnumToolMaterial;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ItemTool;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.World;

public class ItemAngularSaw extends Item {
    static Block[] cuttableBlocks = {Block.oreDiamond, Block.oreRedstone, Block.oreLapis, Block.oreCoal,
        Block.stone, Block.glass, Block.glowStone };
    public ItemAngularSaw(int itemID) {
        super(itemID);
        setMaxDamage(0); //used to be 64
        setIconCoord(5, 1);
        setItemName("angularSaw");
        setTextureFile(Core.texture_file_item);
        Core.tab(this, TabType.TOOLS);
        setMaxStackSize(1);
        setFull3D();
    }
    
    @Override
    public boolean canHarvestBlock(Block block) {
        for (Block b : cuttableBlocks) {
            if (b == block) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public float getStrVsBlock(ItemStack itemstack, Block block, int metadata) {
        if (canHarvestBlock(block)) {
            return 6;
        }
        return super.getStrVsBlock(itemstack, block, metadata);
    }
    
    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, int X, int Y, int Z,
            EntityPlayer player) {
        if (!player.capabilities.allowEdit) {
            return false;
        }
        Coord here = new Coord(player.worldObj, X, Y, Z);
        Block block = here.getBlock();
        if (!player.worldObj.canMineBlock(player, X, Y, Z)) {
            return true;
            //Oh, you can't actually mine here.
            //Let someone else figure it out.
        }
        if (!canHarvestBlock(block)) {
            return false;
        }
        int damage = 1;
        for (Coord neighbor : here.getNeighborsAdjacent()) {
            if (neighbor.isSolid()) {
                damage++;
            }
        }
        if (!Core.registry.extractEnergy(player, damage*100)) {
            return true;
        }
        if (!player.worldObj.isRemote) {
            int md = player.worldObj.getBlockMetadata(X, Y, Z);
            if (block.canSilkHarvest(player.worldObj, player, X, Y, Z, md)) {
                here.spawnItem(new ItemStack(block, 1, md));
            }
        }
        here.setId(0);
//		itemstack.damageItem(damage, player);
//		if (itemstack.stackSize <= 0) {
//			if (itemstack == player.getCurrentEquippedItem()) {
//				player.inventory.mainInventory[player.inventory.currentItem] = null;
//			}
//		}
        return true;
    }
    
    
}
