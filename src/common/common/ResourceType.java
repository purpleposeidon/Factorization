package factorization.common;

import net.minecraft.item.ItemStack;

public enum ResourceType {
    SILVERORE(0, "resource/galena_ore"),
    SILVERBLOCK(1, "resource/silver_block"),
    LEADBLOCK(2, "resource/lead_block"),
    DARKIRONBLOCK(3, "resource/dark_iron_block"),
    DRY(5, "ceramics/dry"),
    BISQUE(6, "ceramics/bisque")
    ;

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
