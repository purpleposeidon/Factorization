package factorization.common;

import java.util.List;

import factorization.common.Core.TabType;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemPocketTable extends Item {

    public ItemPocketTable(int id) {
        super(id);
        setMaxStackSize(1);
        Core.tab(this, TabType.TOOLS);
        setFull3D();
        setUnlocalizedName("factorization:tool/pocket_crafting_table");
    }
    
    @Override
    public void updateIcons(IconRegister reg) {
        super.updateIcons(reg);
        FactorizationTextureLoader.register(reg, ItemIcons.class);
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
    // public boolean tryPlaceIntoWorld(ItemStack stack,
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
        if (!world.isRemote) {
            //...this may be troublesome for stack saving! :O
            player.openGui(Core.instance, FactoryType.POCKETCRAFTGUI.gui, null, 0, 0, 0);
            if (save != null) {
                player.inventory.setItemStack(save);
                Core.proxy.updateHeldItem(player);
            }
        }
        return stack;
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
        if (mouse_item != null && mouse_item.getItem() == this && player.openContainer instanceof ContainerPocket) {
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
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(is, infoList);
    }
}
