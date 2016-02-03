package factorization.shared;

import factorization.util.ItemUtil;
import factorization.util.NORELEASE;
import factorization.weird.barrel.TileEntityDayBarrel;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PatreonRewards {
    private static final byte CHEAP = 0, NORMAL = 1, EXPENSIVE = 2;

    static void init() {
        // NORELEASE: Patron update
        customBarrel("neptunepink", "Forever", NORMAL, "thaumcraft:log#0", "thaumcraft:slab_wood#0"); // Greatwood
        customBarrel("neptunepink", "Forever", NORMAL, "thaumcraft:log#3", "thaumcraft:slab_wood#1"); // Silverwood
        customBarrel("SoundLogic", "May 2015", NORMAL, "Botania:livingwood#0", "Botania:livingwood#1");
        customBarrel("SoundLogic", "May 2015", NORMAL, "Botania:dreamwood#0", "Botania:dreamwood#1"); // *two* barrel recipes!? Oh well; they're a pair.
        customBarrel("mallrat208", "Feb 2015", EXPENSIVE, "factorization:ResourceBlock#3", "factorization:ResourceBlock#7");
        masquerader("asiekierka", "Dec 2015");
    }

    private static void customBarrel(String patron, String date, byte cost, String logName, String slabName) {
        masquerader(patron, date);
        ItemStack log = ItemUtil.parseBlock(logName);
        ItemStack slab = ItemUtil.parseBlock(slabName);
        if (log == null || slab == null) return;
        TileEntityDayBarrel.makeRecipe(log, slab);
    }

    private static void masquerader(String patron, String date) {
        masqueraders.add(patron);
    }

    private static ArrayList<String> masqueraders = new ArrayList<String>();

    public static List<String> getMasqueraders() {
        ArrayList<String> ret = new ArrayList<String>();
        ret.addAll(masqueraders);
        return ret;
    }
}
