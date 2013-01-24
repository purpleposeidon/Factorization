package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.IActOnCraft;

public class ItemBattery extends ItemBlockProxy implements IActOnCraft {
    //3 States: Empty. Enough for 1 magnet. Enough for 2 magnets.
    public ItemBattery(int id) {
        super(id, Core.registry.battery_item_hidden);
        setItemName("factorization_battery");
        setMaxStackSize(1);
        setMaxDamage(0); //'2' is not the number for this.
        setNoRepair();
    }

    public int getStorage(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        if (tag.hasKey("storage")) {
            return tag.getInteger("storage");
        }
        return TileEntityBattery.max_storage;
    }

    public void setStorage(ItemStack is, int new_charge) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("storage", new_charge);
        normalizeDamage(is);
    }

    int magnet_cost = (int) (TileEntityBattery.max_storage * 0.4);

    public void normalizeDamage(ItemStack is) {
        is.setItemDamage(getStorage(is) / magnet_cost);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        if (is.getTagCompound() != null && is.getTagCompound().hasKey("storage")) {
            float fullness = TileEntityBattery.getFullness(getStorage(is));
            list.add((int) (fullness * 100) + "% charged");
        } else {
            switch (is.getItemDamage()) {
            case 0:
                list.add("Low charge");
                break;
            case 1:
                list.add("Medium charge");
                break;
            case 2:
                list.add("Full charge");
                break;
            }
        }
        Core.brand(list);
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result,
            EntityPlayer player) {
        normalizeDamage(is);
        int d = is.getItemDamage();
        if (d > 0) {
            int stor = getStorage(is);
            stor -= magnet_cost;
            setStorage(is, stor);
            normalizeDamage(is);
            is.stackSize++;
        }
        if (result.getItem() == Core.registry.battery) {
            is.stackSize--;
            result.itemID = is.itemID;
            result.stackSize = is.stackSize;
            result.setTagCompound(is.getTagCompound());
        }
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack wire = craftMatrix.getStackInSlot(i);
            if (wire != null && wire.isItemEqual(Core.registry.leadwire_item) /* no NBT okay */) {
                wire.stackSize++;
            }
        }
    }

    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public void getSubItems(int itemIndexThisIsARetardedParameterJustFYI, CreativeTabs tab, List list) {
        list.add(new ItemStack(Core.registry.battery, 1, 2));
    }
    
    @Override
    public void onCreated(ItemStack is, World w, EntityPlayer player) {
        if (is.getTagCompound() == null) {
            NBTTagCompound tag = FactorizationUtil.getTag(is);
            tag.setInteger("storage", getStorage(is));
        }
    }
    
    @Override
    public boolean getShareTag() {
        return true;
    }
}
