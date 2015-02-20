package factorization.shared;

import factorization.util.ItemUtil;
import factorization.weird.TileEntityDayBarrel;
import net.minecraft.item.ItemStack;

public class PatreonRewards {
    private static final byte CHEAP = 0, FINE = 1, EXPENSIVE = 2;

    static void init() {
        // NORELEASE: Patron update
        customBarrel("mallrat208", "Feb 2015", EXPENSIVE, "factorization:ResourceBlock#3", "factorization:ResourceBlock#2");
    }

    private static void customBarrel(String patron, String date, byte cost, String logName, String slabName) {
        ItemStack log = ItemUtil.parseBlock(logName);
        ItemStack slab = ItemUtil.parseBlock(slabName);
        if (log == null || slab == null) return;
        TileEntityDayBarrel.makeRecipe(log, slab);
    }

    private static void masquerader(String patron, String date) {

    }

    /**
     * Don't use this from the renderer! Have the LMP set its damage value to something when put on!
     */
    public static boolean isMasquerader(String username) {
        // Or UUID or something?
        return false;
    }
}
