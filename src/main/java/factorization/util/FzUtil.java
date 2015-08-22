package factorization.util;

import com.google.common.collect.Multimap;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.weird.TileEntityDayBarrel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ServersideAttributeMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class FzUtil {


    public static <E extends Enum> E shiftEnum(E current, E values[], int delta) {
        int next = current.ordinal() + delta;
        if (next < 0) {
            return values[values.length - 1];
        }
        if (next >= values.length) {
            return values[0];
        }
        return values[next];
    }
    
    
    //Liquid tank handling


    public static int getWorldDimension(World world) {
        return world.provider.dimensionId;
    }

    public static World getWorld(int dimensionId) {
        return DimensionManager.getWorld(dimensionId);
    }

    @SideOnly(Side.CLIENT)
    public static void copyStringToClipboard(String text) {
        StringSelection stringselection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringselection, null);
    }
    
    public static <E> ArrayList<E> copyWithoutNull(Collection<E> orig) {
        ArrayList<E> ret = new ArrayList();
        if (orig == null) return ret;
        for (E e : orig) {
            if (e != null) ret.add(e);
        }
        return ret;
    }

    public static void closeNoisily(String msg, InputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (IOException e) {
            Core.logSevere(msg);
            e.printStackTrace();
        }
    }
    
    public static boolean stringsEqual(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public static void spawn(Entity ent) {
        if (ent == null) return;
        ent.worldObj.spawnEntityInWorld(ent);
    }

    public static double rateDamage(ItemStack is) {
        if (is == null) return 0;
        Multimap attrs = is.getItem().getAttributeModifiers(is);
        if (attrs == null) return 0;
        BaseAttributeMap test = new ServersideAttributeMap();
        test.applyAttributeModifiers(attrs);
        IAttributeInstance attr = test.getAttributeInstance(SharedMonsterAttributes.attackDamage);
        if (attr == null) return 0;
        return attr.getAttributeValue();
    }

    public static ItemStack getReifiedBarrel(Coord at) {
        if (at == null) return null;
        if (at.w == null) return null;
        TileEntityDayBarrel barrel = at.getTE(TileEntityDayBarrel.class);
        if (barrel == null) return null;
        return barrel.item;
    }

    public static String toRpm(double velocity) {
        return (int) (Math.toDegrees(velocity) * 10 / 3) + " RPM";
    }

    // Enh, really belongs in NumUtil maybe?
    // Probably UnitUtil, with the map compass stuff as well
    private static class UnitBase {
        final long ratio;
        final String unit;

        private UnitBase(long ratio, String unit) {
            this.ratio = ratio;
            this.unit = unit;
        }
    }

    public static UnitBase unit_time[] = new UnitBase[] {
            new UnitBase(1L * 20 * 60 * 60 * 24 * 365 * 1000000000, "long eons"),
            new UnitBase(1L * 20 * 60 * 60 * 24 * 365 * 1000, "long millenia"),
            new UnitBase(1L * 20 * 60 * 60 * 24 * 365 * 100, "long centuries"),
            new UnitBase(1L * 20 * 60 * 60 * 24 * 365, "long years"),
            new UnitBase(1L * 20 * 60 * 60 * 24 * 30, "long months"), // Mostly! :D
            new UnitBase(1L * 20 * 60 * 60 * 24 * 7, "long weeks"),
            new UnitBase(1L * 20 * 60 * 60 * 24, "long days"),
            new UnitBase(1L * 20 * 60 * 60, "long hours"),
            new UnitBase(1L * 20 * 60 * 20, "days"), // assuming no sleeping
            new UnitBase(1L * 20 * 60, "minutes"),
            new UnitBase(1L * 20, "seconds"),
            new UnitBase(1L, "ticks"),
    };
    public static UnitBase unit_distance_px[] = new UnitBase[] {
            new UnitBase(1L * 16 * 1000, "kilometers"),
            new UnitBase(1L * 16 * 16, "chunks"),
            new UnitBase(1L * 16, "blocks"),
            new UnitBase(1L, "pixels"),
    };

    private static UnitBase best(UnitBase[] bases, long value) {
        boolean wasAbove = false;
        for (UnitBase base : bases) {
            if (base.ratio <= value && wasAbove) {
                return base;
            } else if (base.ratio >= value) {
                wasAbove = true;
            }
        }
        return bases[bases.length - 1];
    }

    public static String unitify(UnitBase[] bases, long value, int max_len) {
        String r = "";
        while (max_len-- != 0) {
            UnitBase best = best(bases, value);
            long l = value / best.ratio;
            value -= best.ratio * l;
            if (l > 0) {
                if (!r.isEmpty()) r += " ";
                r += l + " " + best.unit;
            } else if (value == 0 && !r.isEmpty()) {
                return r;
            }
            if (best.ratio == 1 || max_len == 0) break;
        }
        return r;
    }

    public static void debugBytes(String header, byte[] d) {
        System.out.println(header + " #" + d.length);
        for (byte b : d) {
            System.out.print(" " + Integer.toString(b));
        }
        System.out.println();
    }
}
