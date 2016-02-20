package factorization.beauty;

import factorization.shared.Core;
import factorization.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

class ShaftItemCache {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShaftItemCache other = (ShaftItemCache) o;
        return sheared == other.sheared && ItemUtil.identical(log, other.log);
    }

    @Override
    public int hashCode() {
        return ItemUtil.getItemHash(log) + (sheared ? 13 : 31);
    }

    final ItemStack log;
    final boolean sheared;

    ShaftItemCache(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null) {
            log = TileEntityShaft.defaultLog;
            sheared = false;
            return;
        }
        log = ItemUtil.getPackedItem(stack, "log", TileEntityShaft.defaultLog) ;
        sheared = stack.getTagCompound().getBoolean("sheared");
    }

    ShaftItemCache(ItemStack log, boolean sheared) {
        this.log = log;
        this.sheared = sheared;
    }

    public ItemStack pack() {
        ItemStack ret = new ItemStack(Core.registry.shaft);
        ItemUtil.packItem(ret, "log", log);
        NBTTagCompound tag = ItemUtil.getTag(ret);
        tag.setBoolean("sheared", sheared);
        return ret;
    }
}
