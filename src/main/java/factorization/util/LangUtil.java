package factorization.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;

public class LangUtil {
    public static String getProperKey(ItemStack is) {
        String n = is.getUnlocalizedName();
        if (n == null || n.length() == 0) {
            n = "???";
        }
        return n;
    }

    public static String getTranslationKey(ItemStack is) {
        //Get the key for translating is.
        if (is == null) {
            return "<null itemstack; bug?>";
        }
        try {
            String s = is.getDisplayName();
            if (s != null && s.length() > 0) {
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String key = getProperKey(is);
        if (canTranslate(key + ".name")) {
            return key + ".name";
        }
        if (canTranslate(key)) {
            return key;
        }
        return key + ".name";
    }

    static boolean canTranslate(String str) {
        String ret = StatCollector.translateToLocal(str);
        if (ret == null || ret.length() == 0) {
            return false;
        }
        return !ret.equals(str);
    }

    public static String getTranslationKey(Item i) {
        if (i == null) {
            return "<null item; bug?>";
        }
        return i.getUnlocalizedName() + ".name";
    }

    public static String translate(String key) {
        return ("" + StatCollector.translateToLocal(key + ".name")).trim();
    }

    public static String translateThis(String key) {
        return ("" + StatCollector.translateToLocal(key)).trim();
    }

    public static String translateExact(String key) {
        if (key != null && key.startsWith("§UNIT§")) {
            // §UNIT§ TIME 2 123456789
            String[] bits = key.split(" ");
            if (bits.length != 4) {
                return key;
            }
            String unit = bits[1];
            int maxParts = Integer.parseInt(bits[2]);
            Long val = Long.parseLong(bits[3]);
            return FzUtil.unitify(unit, val, maxParts);
        }
        String ret = StatCollector.translateToLocal(key);
        //noinspection StringEquality: StatCollector will return the exact same object if translation fails
        if (ret == key) {
            return null;
        }
        return ret;
    }

    public static String tryTranslate(String key, String fallback) {
        String ret = translateExact(key);
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    public static boolean canTranslateExact(String key) {
        return translateExact(key) != null;
    }

    public static String translateWithCorrectableFormat(String key, Object... params) {
        String format = translate(key);
        String ret = String.format(format, params);
        String correctedTranslation = translateExact("factorization.replace:" + ret);
        if (correctedTranslation != null) {
            return correctedTranslation;
        }
        return ret;
    }

    public static void sendChatMessage(boolean raw, ICommandSender sender, String msg) {
        sender.addChatMessage(raw ? new ChatComponentText(msg) : new ChatComponentTranslation(msg));
    }

    public static void sendUnlocalizedChatMessage(ICommandSender sender, String format, Object... params) {
        sender.addChatMessage(new ChatComponentTranslation(format, params));
    }
}
