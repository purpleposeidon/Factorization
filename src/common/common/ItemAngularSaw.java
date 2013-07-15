package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import factorization.api.Coord;
import factorization.common.Core.TabType;

public class ItemAngularSaw extends Item { //NORELEASE: Should we lose this thing? I think the recipe's gone already for some reason... 
    static Block[] cuttableBlocks = {Block.oreDiamond, Block.oreRedstone, Block.oreLapis, Block.oreCoal, Block.oreEmerald,
        Block.stone, Block.glass, Block.glowStone };
    public ItemAngularSaw(int itemID) {
        super(itemID);
        setMaxDamage(0); //used to be 64
        setUnlocalizedName("factorization:tool/angular_saw");
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
            Core.notify(player, here, "No cut");
            return false;
        }
        int damage = 1;
        for (Coord neighbor : here.getNeighborsAdjacent()) {
            if (neighbor.isSolid()) {
                damage++;
            }
        }
        if (!Core.registry.extractEnergy(player, damage*100)) {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack is = player.inventory.getStackInSlot(i);
                if (is == null || is.getItem() != Core.registry.battery) {
                    continue;
                }
                Core.notify(player, here, "No charge");
                return true;
            }
            Core.notify(player, here, "No battery");
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
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean detailed) {
        super.addInformation(is, player, list, detailed);
        Core.brand(is, list);
    }
}
