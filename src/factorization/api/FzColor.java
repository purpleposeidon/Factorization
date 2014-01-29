package factorization.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;

public enum FzColor {
    WHITE,
    ORANGE,
    MAGENTA,
    LIGHTBLUE,
    YELLOW,
    LIME,
    PINK,
    GRAY,
    LIGHTGRAY,
    CYAN,
    PURPLE,
    BLUE,
    BROWN,
    GREEN,
    RED,
    BLACK;
    
    private static FzColor[] cache = FzColor.values();
    
    public static FzColor readColor(Coord c) {
        if (c == null || c.w == null) {
            return BLACK;
        }
        Block b = c.getBlock();
        if (b == null) {
            return BLACK;
        }
        if (b instanceof BlockColored) {
            int md = c.getMd();
            if (md < 0 || md >= 16) {
                return BLACK;
            }
            return cache[md];
        }
        return BLACK;
    }
}
