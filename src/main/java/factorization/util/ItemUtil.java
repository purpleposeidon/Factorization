package factorization.util;

import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations on ItemStack.
 */
public final class ItemUtil {
    //ItemStack handling
    public static final int WILDCARD_DAMAGE = OreDictionary.WILDCARD_VALUE;

    public static ItemStack makeWildcard(Item item) {
        return new ItemStack(item, 1, WILDCARD_DAMAGE);
    }

    public static ItemStack makeWildcard(Block item) {
        return new ItemStack(item, 1, WILDCARD_DAMAGE);
    }

    public static boolean isWildcard(ItemStack is, boolean ifNull) {
        if (is == null) return ifNull;
        return is.getItemDamage() == WILDCARD_DAMAGE;
    }

    /**
     * return if the two itemstacks are identical, excepting stacksize
     */
    public static boolean identical(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }
        return couldMerge(a, b);
    }

    /**
     * Compare includes NBT and damage value; ignores stack size
     */
    public static boolean couldMerge(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return true;
        }
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage() && sameItemTags(a, b);
    }

    public static boolean sameItemTags(ItemStack a, ItemStack b) {
        if (a.stackTagCompound == null || b.stackTagCompound == null) {
            return a.stackTagCompound == b.stackTagCompound;
        }
        return a.stackTagCompound.equals(b.stackTagCompound);
    }

    /**
     * Compare includes damage value; ignores stack size and NBT
     */
    public static boolean similar(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage();
    }

    /**
     * Compare two items; ignores NBT; ignores damage on tools
     */
    public static boolean swordSimilar(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getItem() != b.getItem()) return false;
        if (a.getItem().isDamageable()) return true;
        return a.getItemDamage() == b.getItemDamage();
    }

    /**
     * Compare only itemIDs and damage value, taking into account that a damage value of -1 matches any
     */
    public static boolean wildcardSimilar(ItemStack template, ItemStack stranger) {
        if (template == null || stranger == null) {
            return template == stranger;
        }
        if (template.getItemDamage() == WILDCARD_DAMAGE) {
            return template.getItem() == stranger.getItem();
        }
        return similar(template, stranger);
    }

    public static boolean oreDictionarySimilar(Object template, ItemStack stranger) {
        if (template instanceof String) {
            ArrayList<ItemStack> ores = OreDictionary.getOres((String) template);
            for (int i = 0; i < ores.size(); i++) {
                if (wildcardSimilar(ores.get(i), stranger)) {
                    return true;
                }
            }
            return false;
        } else if (template instanceof List) {
            for (Object o : (List)template) {
                if (oreDictionarySimilar(o, stranger)) {
                    return true;
                }
            }
            return false;
        } else {
            return wildcardSimilar((ItemStack) template, stranger);
        }
    }

    public static int stackSize(ItemStack is) {
        return (is == null) ? 0 : is.stackSize;
    }

    public static ItemStack normalDecr(ItemStack is) {
        is.stackSize--;
        return is.stackSize <= 0 ? null : is;
    }

    /** Makes sure there is an NBT tag on the ItemStack, and then returns it. */
    public static NBTTagCompound getTag(ItemStack is) {
        NBTTagCompound ret = is.getTagCompound();
        if (ret == null) {
            ret = new NBTTagCompound();
            is.setTagCompound(ret);
        }
        return ret;
    }

    public static long getItemHash(ItemStack is) {
        if (is == null) {
            return Long.MIN_VALUE;
        }
        long ih = DataUtil.getId(is);
        long md = is.getItemDamage();
        long tg = 0;
        if (is.hasTagCompound()) {
            tg = is.getTagCompound().hashCode();
        }
        return (ih << 48) + (md << 32) + tg + is.stackSize*100;
    }

    public static String getCustomItemName(ItemStack is) {
        if (is != null && is.hasDisplayName()) {
            return is.getDisplayName();
        }
        return null;
    }

    public static boolean itemCanFire(World w, ItemStack is, int tickDelay) {
        NBTTagCompound tag = getTag(is);
        long t = tag.getLong("lf");
        if (t > w.getTotalWorldTime()) {
            tag.setLong("lf", w.getTotalWorldTime());
            return true;
        }
        if (t + tickDelay > w.getTotalWorldTime()) {
            return false;
        }
        tag.setLong("lf", w.getTotalWorldTime());
        return true;
    }

    public static ItemStack normalize(ItemStack is) {
        if (is == null || is.stackSize <= 0) {
            return null;
        }
        return is;
    }

    public static int getStackSize(ItemStack is) {
        if (is == null) {
            return 0;
        }
        return is.stackSize;
    }

    public static int getFreeSpace(ItemStack is, int stackLimit) {
        int max = Math.min(is.getMaxStackSize(), stackLimit);
        return Math.max(0, max - is.stackSize);
    }

    public static ItemStack nameItemStack(ItemStack is, String name) {
        is = is.copy();
        is.setStackDisplayName(name);
        return is;
    }

    public static List<ItemStack> getSubItems(ItemStack is) {
        ArrayList<ItemStack> out = new ArrayList();
        is.getItem().getSubItems(is.getItem(), is.getItem().getCreativeTab(), out);
        return out;
    }

    public static ItemStack parseBlock(String name) {
        short md = WILDCARD_DAMAGE;
        if (name.contains("#")) {
            String[] split = name.split("#");
            md = Short.parseShort(split[1]);
            name = split[0];
        }
        Item item = DataUtil.getItemFromName(name);
        if (item == null) return null;
        return new ItemStack(item, 1, md);
    }

    public static ItemStack getFirstOre(String oreName) {
        List<ItemStack> ores = OreDictionary.getOres(oreName);
        if (ores == null || ores.isEmpty()) return null;
        return ores.get(0);
    }
}
