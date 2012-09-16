package factorization.common;

import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

public class ItemPocketTable extends Item {

    public ItemPocketTable(int id) {
        super(id);
        setMaxStackSize(1);
        setTabToDisplayOn(CreativeTabs.tabTools);
        setFull3D();
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    public int getIconFromDamage(int damage) {
        return 4;
    }

    //
    // @Override
    // public boolean onItemUseFirst(ItemStack stack, EntityPlayer player,
    // World world, int X, int Y, int Z, int side) {
    // player.openGui(FactorizationCore.instance, FactoryType.POCKETCRAFT.gui,
    // null, 0, 0, 0);
    // return true;
    // }
    //
    // @Override
    // public boolean onItemUse(ItemStack stack,
    // EntityPlayer player, World world, int X, int Y,
    // int Z, int side) {
    // // TODO Auto-generated method stub
    // return super.onItemUse(par1ItemStack, par2EntityPlayer, par3World, par4,
    // par5,
    // par6, par7);
    // }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ItemStack save = player.inventory.getItemStack();
        if (save != null) {
            player.inventory.setItemStack(null);
        }
        //XXX TODO: Chests stay open. Man, how do I fix this?
        //player.openGui(Core.instance, FactoryType.NULLGUI.gui, null, 0, 0, 0);
        player.openGui(Core.instance, FactoryType.POCKETCRAFTGUI.gui, null, 0, 0, 0);
        if (save != null) {
            player.inventory.setItemStack(save);
            Core.proxy.updateHeldItem(player);
        }
        return stack;
    }

    @Override
    public String getItemName() {
        return "Pocket Crafting Table";
    }

    @Override
    public String getItemNameIS(ItemStack par1ItemStack) {
        return getItemName();
    }

    public ItemStack findPocket(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (i % 9 >= (9 - 3) && i > 9) {
                continue;
            }
            ItemStack is = inv.getStackInSlot(i);
            if (is == null) {
                continue;
            }
            if (is.getItem() == this) {
                return is;
            }
        }
        ItemStack mouse_item = player.inventory.getItemStack();
        if (mouse_item != null && mouse_item.getItem() == this && player.craftingInventory instanceof ContainerPocket) {
            return mouse_item;
        }
        return null;
    }

    public boolean tryOpen(EntityPlayer player) {
        ItemStack is = findPocket(player);
        if (is == null) {
            return false;
        }
        this.onItemRightClick(is, player.worldObj, player);
        return true;
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
}
