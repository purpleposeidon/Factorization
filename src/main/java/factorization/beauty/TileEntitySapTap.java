package factorization.beauty;

import java.io.IOException;
import java.util.HashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.chunk.Chunk;

import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.charge.TileEntityCaliometricBurner;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;

public class TileEntitySapTap extends TileEntityCommon implements ISidedInventory, ITickable {
    ItemStack sap = new ItemStack(Core.registry.sap, 0, 0);
    int log_count = 0, leaf_count = 0;
    long sap_rate = 0;
    int ticks = 0;

    static {
        TileEntityCaliometricBurner.register(Core.registry.sap, 8, 0.5);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SAP_TAP;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) return sap;
        return null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot == 0) {
            if (sap.stackSize <= 0) return null;
            return sap.splitStack(amount);
        }
        return null;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        if (slot == 0) {
            ItemStack stack = sap;
            sap = null;
            return stack;

        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            if (ItemUtil.getStackSize(stack) == 0) {
                stack = new ItemStack(Core.registry.sap, 0, 0);
            }
            sap = stack;
        }
    }

    @Override
    public IChatComponent getDisplayName() {
        return new ChatComponentTranslation("fz.sapextractor");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 0;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return false;
    }

    @Override public void openInventory(EntityPlayer player) { }

    @Override public void closeInventory(EntityPlayer player) { }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack == null;
    }

    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }

    @Override
    public void clear() {
        sap = null;
        log_count = 0;
        leaf_count = 0;
        ticks = 0;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Barrel;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        sap = data.as(Share.PRIVATE, "sap").putItemStack(sap);
        log_count = data.as(Share.PRIVATE, "logCount").putInt(log_count);
        leaf_count = data.as(Share.PRIVATE, "leafCount").putInt(leaf_count);
        sap_rate = data.as(Share.PRIVATE, "sapRate").putLong(sap_rate);
        ticks = data.as(Share.PRIVATE, "ticks").putInt(ticks);
    }


    @Override
    public void tick() {
        if (worldObj.isRemote) return;
        final long nowish = worldObj.getTotalWorldTime() + this.hashCode();
        if (0 == nowish % (20 * 60 * 30)) {
            scanTree(null);
        }
        if (sap_rate > 0 && ticks++ > sap_rate) {
            ticks = 0;
            if (sap == null) {
                sap = new ItemStack(Core.registry.sap, 0);
            }
            if (sap.stackSize < sap.getMaxStackSize()) {
                sap.stackSize++;
            }
        }
    }

    static final int search_radius = 16;
    void scanTree(EntityPlayer player) {
        if (worldObj.isRemote) return;
        try {
            Coord level = getCoord();
            TreeCounter ruler = new TreeCounter(level.add(-search_radius, -8, -search_radius), level.add(search_radius, 32, search_radius), getCoord());
            ruler.calculate(1000);
            log_count = ruler.logs;
            leaf_count = ruler.leaves;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        int max_logs = leaf_count / 3;
        int effective_logs = Math.min(max_logs, log_count);
        final int real_minimum = 64 * 3;
        if (effective_logs < real_minimum) {
            effective_logs *= effective_logs / (float) real_minimum;
        }
        final long ticks_per_log = 20 * 60 * 60 * 4;
        if (effective_logs <= 0) {
            sap_rate = 0;
        } else {
            sap_rate = ticks_per_log / effective_logs;
            if (sap_rate < 10) sap_rate = 10;
        }
        if (player != null) {
            String sap_units = sap_rate <= 0 ? "∞" : FzUtil.unitTranslateTimeTicks(sap_rate, 2);
            new Notice(this, "factorization.factoryBlock.SAP_TAP.info",
                    "" + log_count, "" + leaf_count, "" + effective_logs, sap_units).withStyle(Style.LONG).sendTo(player);
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        scanTree(player);
    }

    @Override
    public boolean activate(EntityPlayer player, EnumFacing side) {
        scanTree(player);
        return super.activate(player, side);
    }

    boolean isLog(Coord c) {
        Block b = c.getBlock();
        return b.getMaterial() == Material.wood && b instanceof BlockLog;
    }

    @Override
    public boolean canPlaceAgainst(final EntityPlayer player, Coord c, EnumFacing side) {
        final Coord at = getCoord();
        if (!isLog(at.add(EnumFacing.UP))) {
            if (!worldObj.isRemote) {
                new Notice(at, "factorization.factoryBlock.SAP_TAP.belowlog").sendTo(player);
            }
            return false;
        }
        Coord min = at.add(-search_radius, -search_radius, -search_radius);
        Coord max = at.add(search_radius, search_radius, search_radius);
        CrowdedCheck cc = new CrowdedCheck();
        cc.at = at;
        Coord.iterateChunks(min, max, cc);
        if (cc.crowd != null) {
            if (!worldObj.isRemote) {
                new Notice(at, "factorization.factoryBlock.SAP_TAP.crowded").withStyle(Style.FORCE).sendTo(player);
                new Notice(cc.crowd, "factorization.factoryBlock.SAP_TAP.thecrowd").withStyle(Style.FORCE).sendTo(player);
            }
            return false;
        }
        return super.canPlaceAgainst(player, c, side);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        if (side == EnumFacing.DOWN) return new int[] { 0 };
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return false;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == 0 && sap.stackSize > 1;
    }

    private class CrowdedCheck implements ICoordFunction {
        Coord at;
        TileEntity crowd = null;
        @Override
        public void handle(Coord here) {
            Chunk chunk = here.getChunk();
            for (TileEntity te : chunk.getTileEntityMap().values()) {
                if (te.isInvalid()) continue;
                if (!(te instanceof TileEntitySapTap)) continue;
                Coord tec = new Coord(te);
                if (tec.distanceSq(at) <= search_radius) {
                    crowd = te;
                    return;
                }
            }
        }
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        if (ItemUtil.normalize(sap) != null) {
            Coord here = new Coord(this);
            InvUtil.spawnItemStack(here, sap);
        }
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        if (ItemUtil.normalize(sap) == null) return;
        InvUtil.givePlayerItem(entityplayer, sap);
        sap = new ItemStack(Core.registry.sap, 0);
    }

    private class TreeCounter {
        final Coord start, min, max;
        final FastBag<Coord> frontier = new FastBag<Coord>();
        final HashSet<Coord> visited = new HashSet<Coord>();
        int logs = 0, leaves = 0;

        public TreeCounter(Coord min, Coord max, Coord start) {
            this.start = start;
            this.min = min.copy();
            this.max = max.copy();
            Coord.sort(min, max);
            for (Coord c : start.getNeighborsAdjacent()) {
                if (kind(c) == WOOD) {
                    check(c, WOOD);
                    frontier.add(c);
                }
            }
        }

        void calculate(int n) {
            while (n-- > 0 && !frontier.isEmpty()) {
                Coord c = frontier.remove(0);
                visit(c);
            }
        }

        static final byte WOOD = 1, LEAF = 2, NON = 0;

        byte kind(Coord at) {
            final Block block = at.getBlock();
            Material mat = block.getMaterial();
            if (mat == Material.wood && block instanceof BlockLog) {
                return WOOD;
            } else if (mat == Material.leaves || mat == Material.vine) {
                return LEAF;
            }
            return NON;
        }

        boolean check(Coord at, byte kind) {
            visited.add(at);
            if (kind == WOOD) {
                logs++;
                return true;
            } else if (kind == LEAF) {
                leaves++;
                return true;
            }
            return false;
        }

        void visit(Coord at) {
            byte atKind = kind(at);
            for (Coord n : at.getNeighborsDiagonal()) {
                if (!n.inside(min, max)) continue;
                if (visited.contains(n)) continue;
                byte nKind = kind(n);
                if (atKind == LEAF && nKind != LEAF) continue;
                if (check(n, nKind)) {
                    frontier.add(n);
                }
            }
        }
    }
}
