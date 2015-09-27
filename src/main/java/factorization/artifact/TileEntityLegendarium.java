package factorization.artifact;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;
import java.util.ArrayList;

public class TileEntityLegendarium extends TileEntityCommon {
    static final int MIN_SIZE = 7; // The queue must be this size before something can be removed
    static final int POSTER_RANGE = 16;
    static final int MAX_USAGES_LEFT = 32;
    static final int MINIMUM_MAX_TOOL_DURABILITY = 64;

    private static final int WAIT_TIME = 7 * 24 * 60 * 60 * 1000;

    long last_insert_time = 0;
    ArrayList<ItemStack> queue = new ArrayList<ItemStack>();

    @Override
    public void putData(DataHelper data) throws IOException {
        last_insert_time = data.as(Share.PRIVATE, "lastInsertTime").putLong(last_insert_time);
        queue = data.as(Share.PRIVATE, "queue").putItemList(queue);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEGENDARIUM;
    }

    boolean isDroid(EntityPlayer player) {
        if (player instanceof FakePlayer) {
            new Notice(this, "factorization.legendarium.nodroidsallowed").sendToAll();
            return true;
        }
        return false;
    }

    static boolean isTool(ItemStack is) {
        if (is == null) return false;
        if (is.stackSize != 1) return false;
        if (is.getMaxStackSize() > 1) return false;
        if (!is.getItem().isRepairable()) return false;
        if (is.getMaxDamage() <= 1) return false;
        if (is.getHasSubtypes()) return false;
        return true;
    }

    static String analyzeItem(ItemStack is) {
        if (is == null) return "noitem";
        if (!isTool(is)) return "not_tool";
        if (is.getMaxDamage() < MINIMUM_MAX_TOOL_DURABILITY) return "wimpy_tool";
        if (is.getItemDamage() < is.getMaxDamage() - MAX_USAGES_LEFT) return "not_broken";
        return null;
    }

    void sound(String name) {

    }

    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        // Store an item
        if (worldObj.isRemote) return true;
        if (isDroid(player)) return false;
        ItemStack held = player.getHeldItem();
        if (held == null && canRemove()) {
            new Notice(this, "factorization.legendarium.ready").sendTo(player);
            return true;
        }

        String analysis = analyzeItem(held);
        if (analysis != null) {
            new Notice(this, "factorization.legendarium.item." + analysis).sendTo(player);
            return true;
        }

        long now = System.currentTimeMillis();
        long to_wait = last_insert_time + WAIT_TIME - now;
        if (to_wait > 0) {
            long ticks = (to_wait /* ms */ / 1000) /* seconds */ * 20 /* ticks */;
            new Notice(this, "factorization.legendarium.wait", FzUtil.unitify(FzUtil.unit_time, ticks, 2)).sendTo(player);
            return true;
        }
        last_insert_time = now;
        queue.add(held);
        player.setCurrentItemOrArmor(0, null);
        markDirty();
        sound("insert");
        return true;
    }

    boolean canRemove() {
        return queue.size() >= MIN_SIZE;
    }

    @Override
    public void click(EntityPlayer player) {
        // Remove an item
        if (isDroid(player)) return;
        if (!canRemove()) {
            new Notice(this, "factorization.legendarium.notfull").sendTo(player);
            return;
        }
        markDirty();
        sound("remove");
    }

    @Override
    public boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        //NORELEASE.fixme("1 per dimension class");
        return super.canPlaceAgainst(player, c, side);
    }
}
