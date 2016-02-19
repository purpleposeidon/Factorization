package factorization.beauty;

import factorization.util.ItemUtil;
import net.minecraft.item.ItemStack;

class ShaftItemCache {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShaftItemCache cacheInfo = (ShaftItemCache) o;
        return ItemUtil.identical(log, cacheInfo.log);
    }

    @Override
    public int hashCode() {
        return ItemUtil.getItemHash(log);
    }

    final ItemStack log;

    ShaftItemCache(ItemStack stack) {
        log = ItemUtil.getPackedItem(stack, "log", TileEntityShaft.defaultLog) ;
    }
}
