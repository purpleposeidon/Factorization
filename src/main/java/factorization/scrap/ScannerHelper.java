package factorization.scrap;

import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScannerHelper {
    public static Item nextItem(Scanner in) {
        final String name = in.next();
        Item ret = DataUtil.getItemFromName(name);
        if (ret == null) throw new CompileError("No such item: " + name);
        return ret;
    }

    public static Block nextBlock(Scanner in) {
        final String name = in.next();
        final Block ret = DataUtil.getBlockFromName(name);
        if (ret == null) throw new CompileError("No such block: " + name);
        return ret;
    }

    private static final Pattern stackPattern = Pattern.compile("([a-zA-Z_09:]+)(%[\\d]+)?(#[\\d])+");
    public static ItemStack nextStack(Scanner in) {
        // domain:itemName%damageValue#stacksize
        String stackText = in.next(stackPattern);
        Matcher match = stackPattern.matcher(stackText);
        String name = match.group(1);
        int damageValue = i(match.group(2), 0);
        int stacksize = i(match.group(3), 1);
        Item it = DataUtil.getItemFromName(name);
        if (it == null) throw new CompileError("No such item: " + name);
        return new ItemStack(it, stacksize, damageValue);
    }

    public static Class nextClass(Scanner in) {
        String className = in.next();
        try {
            return ScannerHelper.class.getClassLoader().loadClass(className);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new CompileError("Class not found: " + className);
        }
    }

    private static int i(String n, int def) {
        if (StringUtils.isNullOrEmpty(n)) return def;
        return Integer.parseInt(n);
    }
}
