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
}
