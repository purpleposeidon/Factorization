package factorization.api;

import java.util.EnumSet;

import factorization.shared.FzUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.item.ItemStack;

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
    
    FzColor(String dyeName, int hex) {
        this.dyeName = dyeName;
        this.hex = hex;
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
        if (b instanceof BlockColored) {
            int md = c.getMd();
            if (md < 0 || md >= 16) {
                return null;
            }
            return cache[md];
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
            if (FzUtil.oreDictionarySimilar(color.dyeName, is)) {
                return color;
            }
        }
        return NO_COLOR;
    }
}
