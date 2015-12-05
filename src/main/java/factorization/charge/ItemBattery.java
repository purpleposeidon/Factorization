package factorization.charge;

import factorization.api.IActOnCraft;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.util.ItemUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

public class ItemBattery extends ItemBlockProxy implements IActOnCraft {
    //3 States: Empty. Enough for 1 magnet. Enough for 2 magnets.
    public ItemBattery() {
        super(Core.registry.battery_item_hidden, "charge_battery", TabType.CHARGE);
        setMaxStackSize(1);
        setMaxDamage(0); //'2' is not the number for this.
        setNoRepair();
    }

    public int getStorage(ItemStack is) {
        NBTTagCompound tag = ItemUtil.getTag(is);
        if (tag.hasKey("storage")) {
            return tag.getInteger("storage");
        }
        return TileEntityBattery.max_storage;
    }

    public void setStorage(ItemStack is, int new_charge) {
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setInteger("storage", new_charge);
        normalizeDamage(is);
    }

    int magnet_cost = (int) (TileEntityBattery.max_storage * 0.4);

    public void normalizeDamage(ItemStack is) {
        is.setItemDamage(getStorage(is) / magnet_cost);
    }

    @Override
    public void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        final String pre = "item.factorization:charge_battery.";
        if (is.getTagCompound() != null && is.getTagCompound().hasKey("storage")) {
            float fullness = TileEntityBattery.getFullness(getStorage(is));
            String n = StatCollector.translateToLocalFormatted(pre + "perc", (int) (fullness * 100));
            list.add(n);
        } else {
            switch (is.getItemDamage()) {
            case 0:
                list.add(pre + "low");
                break;
            case 1:
                list.add(pre + "mid");
                break;
            case 2:
                list.add(pre + "full");
                break;
            }
        }
    }
    
    @Override
    public boolean hasContainerItem() {
        return true;
    }

    // NORELEASE: doesContainerItemLeaveCraftingGrid has gone missing in 1.8?

    @Override
    public ItemStack getContainerItem(ItemStack is) {
        is = is.copy();
        normalizeDamage(is);
        int d = is.getItemDamage();
        if (d > 0) {
            int stor = getStorage(is);
            stor -= magnet_cost;
            setStorage(is, stor);
            normalizeDamage(is);
        }
        return is;
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player) {
        normalizeDamage(is);
        if (result.getItem() == Core.registry.battery) {
            is.stackSize--;
            result.setItem(result.getItem());
            result.stackSize = is.stackSize;
            result.setTagCompound(is.getTagCompound());
        }
    }

    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public void onCreated(ItemStack is, World w, EntityPlayer player) {
        if (is.getTagCompound() == null) {
            NBTTagCompound tag = ItemUtil.getTag(is);
            tag.setInteger("storage", getStorage(is));
        }
    }
    
    @Override
    public boolean getShareTag() {
        return true;
    }
}
