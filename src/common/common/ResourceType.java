package factorization.common;

import net.minecraft.item.ItemStack;

public enum ResourceType {
    SILVERORE(0), SILVERBLOCK(1), LEADBLOCK(2), DARKIRONBLOCK(3), EXOMODDER(4);

    final public int md;

    ResourceType(int metadata) {
        md = metadata;
    }

    public boolean is(int md) {
        return md == this.md;
    }

    ItemStack itemStack(String name) {
        ItemStack ret = new ItemStack(Core.registry.item_resource, 1, this.md);
        Core.proxy.addName(ret, name);
        return ret;
    }
}
