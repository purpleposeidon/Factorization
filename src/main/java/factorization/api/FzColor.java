package factorization.api;

import factorization.util.ItemUtil;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum FzColor {
    NO_COLOR(null, 0xFFFFFF),
    WHITE("dyeWhite", 0xF0F0F0),
    ORANGE("dyeOrange", 0xEB8844),
    MAGENTA("dyeMagenta", 0xC354CD),
    LIGHTBLUE("dyeLightBlue", 0x6689D3),
    YELLOW("dyeYellow", 0xDECF2A),
    LIME("dyeLime", 0x41CD34),
    PINK("dyePink", 0xD88198),
    GRAY("dyeGray", 0x434343),
    LIGHTGRAY("dyeLightGray", 0xABABAB),
    CYAN("dyeCyan", 0x287697),
    PURPLE("dyePurple", 0x7B2FBE),
    BLUE("dyeBlue", 0x253192),
    BROWN("dyeBrown", 0x51301A),
    GREEN("dyeGreen", 0x3B511A),
    RED("dyeRed", 0xB3312C),
    BLACK("dyeBlack", 0x1E1B1B);
    
    public final String dyeName;
    public final int hex; // The color values are from ItemDye
    private final List<ItemStack> ore_entries;
    
    FzColor(String dyeName, int hex) {
        this.dyeName = dyeName;
        this.hex = hex;
        if (dyeName == null) {
            ore_entries = new ArrayList<ItemStack>();
        } else {
            ore_entries = OreDictionary.getOres(dyeName);
        }
    }

    public int getR() {
        return ((this.hex & 0xFF0000) >> 16);
    }

    public int getG() {
        return ((this.hex & 0x00FF00) >> 8);
    }

    public int getB() {
        return (this.hex & 0x0000FF);
    }

    public float getRed() {
        return ((this.hex & 0xFF0000) >> 16) / 255F;
    }
    
    public float getGreen() {
        return ((this.hex & 0x00FF00) >> 8) / 255F;
    }
    
    public float getBlue() {
        return (this.hex & 0x0000FF) / 255F;
    }
    
    
    private static FzColor[] cache = FzColor.values();
    public static final FzColor[] VALID_COLORS = getValidColors();
    
    public static FzColor readColor(Coord c) {
        if (c == null || c.w == null) {
            return NO_COLOR;
        }
        Block b = c.getBlock();
        if (b == null) {
            return NO_COLOR;
        }
        IBlockState bs = c.getState();
        for (Map.Entry<IProperty, Comparable> pair : bs.getProperties().entrySet()) {
            if (pair.getKey().getValueClass() == EnumDyeColor.class) {
                EnumDyeColor dye = (EnumDyeColor) bs.getValue(pair.getKey());
                return fromVanilla(dye);
            }
        }
        return NO_COLOR;
    }
    
    private static FzColor[] getValidColors() {
        FzColor[] ret = new FzColor[cache.length - 1];
        int i = 0;
        for (FzColor color : cache) {
            if (color == NO_COLOR) continue;
            ret[i] = color;
            i++;
        }
        return ret;
    }
    
    public boolean conflictsWith(FzColor other) {
        if (this == NO_COLOR || other == NO_COLOR) return false;
        return this != other;
    }
    
    public static FzColor fromOrdinal(byte id) {
        if (id < cache.length && id >= 0) return cache[id];
        return NO_COLOR;
    }
    
    public byte toOrdinal() {
        return (byte) this.ordinal();
    }
    
    public static FzColor fromVanillaColorIndex(int id) {
        return fromOrdinal((byte) (1 + id));
    }
    
    public int toVanillaColorIndex() {
        if (this == NO_COLOR) return 0; //???
        return ordinal() - 1;
    }
    
    public static FzColor fromItem(ItemStack is) {
        if (is == null) return NO_COLOR;
        for (FzColor color : VALID_COLORS) {
            if (ItemUtil.oreDictionarySimilarEfficient(color.ore_entries, is)) return color;
        }
        return NO_COLOR;
    }

    public static FzColor fromVanilla(EnumDyeColor color) {
        if (color == null) return NO_COLOR;
        return values()[color.ordinal() + 1];
    }

    public EnumDyeColor toVanilla() {
        if (this == NO_COLOR) return null;
        return EnumDyeColor.values()[this.ordinal() - 1];
    }
}
