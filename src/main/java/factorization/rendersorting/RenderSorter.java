package factorization.rendersorting;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;

public class RenderSorter implements Comparator<Object> {
    public static void dirtyTileEntity(TileEntity te) {
        if (!client) return;
        List tes = Minecraft.getMinecraft().renderGlobal.tileEntities;
        if (tes instanceof CleaningList) {
            ((CleaningList) tes).setDirty();
        }
    }

    public static void dirtyEntity(Entity ent) {
        if (!client) return;
        List ents = ent.worldObj.loadedEntityList;
        if (ents instanceof CleaningList) {
            ((CleaningList) ents).setDirty();
        }
    }

    @SubscribeEvent
    public void inject(WorldEvent.Load event) {
        World w = event.world;
        if (!w.isRemote) return;
        if (!(w instanceof WorldClient)) return;
        RenderGlobal rg = Minecraft.getMinecraft().renderGlobal;
        w.loadedEntityList = make(w.loadedEntityList, "Entities");
        rg.tileEntities = make(rg.tileEntities, "TileEntities");
    }

    @SubscribeEvent
    public void sort(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (tick++ % 20 != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        sort(mc.theWorld.loadedEntityList);
        sort(mc.renderGlobal.tileEntities);
    }

    private List make(List orig, String category) {
        if (orig instanceof CleaningList) return orig;
        if (orig.getClass() != ArrayList.class) {
            Core.logWarning("Can't sort " + category + "; it has already been changed to " + orig.getClass());
            return orig;
        }
        List ret = new CleaningList();
        ret.addAll(orig);
        return ret;
    }

    private int tick = 0;

    private void sort(List list) {
        if (!(list instanceof CleaningList)) return;
        CleaningList ls = (CleaningList) list;
        if (ls.setClean()) return;
        Collections.sort(ls, this);
    }

    @Override
    public int compare(Object a, Object b) {
        Class aClass = a.getClass();
        Class bClass = b.getClass();

        if (aClass != bClass) return aClass.getCanonicalName().compareTo(bClass.getCanonicalName());
        if (a instanceof ISortableRenderer) {
            ISortableRenderer ai = (ISortableRenderer) a;
            return ai.compareRenderer(b);
        }
        return 0;
    }

    private static boolean client = FMLCommonHandler.instance().getSide() == Side.CLIENT;

    public static int compareItemRender(ItemStack a, ItemStack b, IItemRenderer.ItemRenderType renderType) {
        // a more complex than b? -1
        // b more complex than a? +1
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return +1;
        // The enchantment effect adds another bind
        if (a.hasEffect(0)) return -1;
        if (b.hasEffect(0)) return +1;
        IItemRenderer ar = MinecraftForgeClient.getItemRenderer(a, renderType);
        IItemRenderer br = MinecraftForgeClient.getItemRenderer(b, renderType);
        if (ar == null && br == null) {
            // standard block/item render
            // (Or possibly some shenanigans have occured from the IItemRenderer.handleRenderType call)
            int as = a.getItemSpriteNumber();
            int bs = b.getItemSpriteNumber();
            if (as == bs) return 0;
            return as > bs ? -1 : +1;
        }
        // Same item renderer. They can still do arbitrary things, but there is no good sorting here.
        if (ar == br) return 0;
        if (ar != null && br != null) {
            // Different; can sort by class reasonably.
            String na = ar.getClass().getCanonicalName();
            String nb = br.getClass().getCanonicalName();
            return na.compareTo(nb);
        }
        // Move the simpler one away from the crazy renderers
        return ar == null ? +1 : -1;
    }
}
