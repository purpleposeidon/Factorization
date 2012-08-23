package factorization.common;

import java.util.List;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import net.minecraft.src.World;
import factorization.api.Coord;
import factorization.api.IActOnCraft;


public class ItemBattery extends Item implements IActOnCraft {
    //3 States: Empty. Enough for 1 magnet. Enough for 2 magnets.
    public ItemBattery(int id) {
        super(id);
        setItemName("battery");
        setMaxDamage(2);
        setNoRepair();
        setMaxStackSize(1);
    }
    
    public int getStorage(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        if (tag.hasKey("storage")) {
            return tag.getInteger("storage");
        }
        return TileEntityBattery.max_storage;
    }
    
    public void normalizeDamage(ItemStack is) {
        int magnet_cost = (int) (TileEntityBattery.max_storage * 0.4);
        is.setItemDamage(getStorage(is)/magnet_cost);
    }
    
    @Override
    public void addInformation(ItemStack is, List lines) {
        float fullness = TileEntityBattery.getFullness(getStorage(is));
        lines.add((int) (fullness * 100) + "% charged");
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player) {
        int d = is.getItemDamage();
        if (d > 0) {
            is.setItemDamage(d - 1);
            is.stackSize++;
        }
    }

    @Override
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float vecx, float vecy, float vecz) {
        ItemStack proxy = Core.registry.battery_item_hidden.copy();
        proxy.stackSize = is.stackSize;
        proxy.setTagCompound(is.getTagCompound());
        boolean ret = proxy.getItem().tryPlaceIntoWorld(proxy, player, w, x, y, z, side, vecx, vecy, vecz);
        is.stackSize = proxy.stackSize;
        return ret;
    }
}
