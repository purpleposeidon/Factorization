package factorization.common;

import factorization.api.Coord;
import factorization.common.Core.TabType;
import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Enchantment;
import net.minecraft.src.EnchantmentHelper;
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
    static Block[] oreBlocks = {Block.oreDiamond, Block.oreRedstone, Block.oreLapis, Block.oreCoal};
    public ItemAngularSaw(int itemID) {
        super(itemID);
        setMaxDamage(64);
        setIconCoord(5, 1);
        setItemName("angularSaw");
        setTextureFile(Core.texture_file_item);
        Core.tab(this, TabType.TOOLS);
        setMaxStackSize(1);
        setFull3D();
    }
    
    @Override
    public boolean canHarvestBlock(Block block) {
        for (Block b : oreBlocks) {
            if (b == block) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void onCreated(ItemStack is, World w, EntityPlayer player) {
        super.onCreated(is, w, player);
        applyEnchant(is);
    }
    
    public static ItemStack applyEnchant(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        NBTTagList ench = new NBTTagList();
        NBTTagCompound silk = new NBTTagCompound();
        silk.setShort("id", (short) Enchantment.silkTouch.effectId);
        silk.setShort("lvl", (short) 1);
        ench.appendTag(silk);
        tag.setTag("ench", ench);
        return is;
    }
    
    public static ItemStack removeEnchant(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setTag("ench", new NBTTagList());
        return is;
    }
    
    @Override
    public boolean hasEffect(ItemStack par1ItemStack) {
        return false;
    }
    
    @Override
    public EnumRarity getRarity(ItemStack par1ItemStack) {
        return EnumRarity.common;
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
        Coord here = new Coord(player.worldObj, X, Y, Z);
        Block block = here.getBlock();
        if (!player.worldObj.canMineBlock(player, X, Y, Z)) {
            return true;
            //Oh, you can't actually mine here.
            //Let someone else figure it out.
        }
        if (!canHarvestBlock(block)) {
            removeEnchant(itemstack);
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
        itemstack.damageItem(damage, player);
        here.setId(0);
        if (!player.worldObj.isRemote) {
            here.spawnItem(new ItemStack(block));
        }
        if (itemstack.stackSize <= 0) {
            if (itemstack == player.getCurrentEquippedItem()) {
                player.inventory.mainInventory[player.inventory.currentItem] = null;
            }
        }
        return true;
    }
    
    
}
