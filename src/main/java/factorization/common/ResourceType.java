package factorization.common;

import factorization.shared.Core;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum ResourceType implements IStringSerializable, Comparable<ResourceType> {
    COPPER_ORE(0, "resource/copper_ore"),
    EMPTY1(1, null), // Was SILVERBLOCK
    EMPTY2(2, null), // Was LEADBLOCK
    DARK_IRON_BLOCK(3, "resource/dark_iron_block"),
    EMPTY4(4, null),
    DRY(5, "ceramics/dry"),
    BISQUE(6, "ceramics/bisque"),
    COPPER_BLOCK(7, "resource/copper_block")
    ;

    final public int md;
    final public String texture;

    public static final ResourceType[] values = values();

    ResourceType(int metadata, String tex) {
        md = metadata;
        texture = tex;
    }

    public boolean is(int md) {
        return md == this.md;
    }

    ItemStack itemStack() {
        return new ItemStack(Core.registry.item_resource, 1, this.md);
    }

    IBlockState blockState() {
        return Core.registry.resource_block.getDefaultState().withProperty(BlockResource.TYPE, this);
    }

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isMetal() {
        return this == DARK_IRON_BLOCK || this == COPPER_BLOCK;
    }
}
