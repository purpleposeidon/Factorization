package factorization.common;

import net.minecraft.item.ItemStack;

public enum ResourceType {
    SILVERORE(0, "block/galena_ore"), SILVERBLOCK(1, "block/block/silver_block"), LEADBLOCK(2, "block/lead_block"), DARKIRONBLOCK(3, "block/dark_iron_block"), EXOMODDER(4, "exo/modder_side");

    final public int md;
    final public String texture;

    ResourceType(int metadata, String tex) {
        md = metadata;
        texture = tex;
    }

    public boolean is(int md) {
        return md == this.md;
    }

    ItemStack itemStack(String name) {
        ItemStack ret = new ItemStack(Core.registry.item_resource, 1, this.md);
        return ret;
    }
}
