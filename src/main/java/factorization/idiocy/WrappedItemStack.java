package factorization.idiocy;

import net.minecraft.item.ItemStack;

public class WrappedItemStack implements Comparable<WrappedItemStack> {
    final ItemStack stack;

    public WrappedItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int compareTo(WrappedItemStack o) {
        return hashCode() - o.hashCode();
    }
}
