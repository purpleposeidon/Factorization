package factorization.beauty;

import factorization.algos.PureFloodfill;
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
import factorization.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntitySapTap extends TileEntityCommon implements IInventory {
    ItemStack sap = new ItemStack(Core.registry.sap, 0, 0);
    int log_count = 0, leaf_count = 0;
    long sap_rate = 0;

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
            return sap.splitStack(amount);
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
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
    public String getInventoryName() {
        return "fz.sapextractor";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 0;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
        return false;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack == null;
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
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        final long nowish = worldObj.getTotalWorldTime() + this.hashCode();
        if (0 == nowish % (20 * 60 * 30)) {
            doLogic(null);
        }
        if (sap_rate > 0 && 0 == nowish % sap_rate) {
            if (sap == null) {
                sap = new ItemStack(Core.registry.sap, 0);
            }
            if (sap.stackSize < sap.getMaxStackSize()) {
                sap.stackSize++;
            }
        }
    }

    static final int search_radius = 16;
    void doLogic(EntityPlayer player) {
        if (worldObj.isRemote) return;
        try {
            Coord level = getCoord();
            TreeRuler ruler = new TreeRuler(level.add(-search_radius, -8, -search_radius), level.add(search_radius, 32, search_radius), getCoord().add(ForgeDirection.UP));
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
            String sap_units = sap_rate <= 0 ? "∞" : FzUtil.unitify(FzUtil.unit_time, sap_rate, 2);
            new Notice(this, "factorization.factoryBlock.SAP_TAP.info",
                    "" + log_count, "" + leaf_count, "" + effective_logs, sap_units).withStyle(Style.LONG).sendTo(player);
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        doLogic(player);
    }

    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        doLogic(player);
        return super.activate(player, side);
    }

    static class TreeRuler extends PureFloodfill {
        protected TreeRuler(Coord start, Coord end, Coord origin) {
            super(start, end, origin);
        }

        @Override
        protected byte convert(Coord here) {
            final Block block = here.getBlock();
            Material mat = block.getMaterial();
            if (mat == Material.wood && block instanceof BlockLog) {
                return 1;
            } else if (mat == Material.leaves) {
                return 2;
            }
            return 0;
        }

        int logs = 0, leaves = 0;

        @Override
        protected void visit(Coord here, byte val) {
            if (val == 1) {
                logs++;
            } else if (val == 2) {
                leaves++;
            }
        }

        @Override
        protected boolean canTransition(byte here, byte next) {
            return next != 0 && (here == 1 || here == next) && here != 0;
        }
    }

    @Override
    public IIcon getIcon(ForgeDirection dir) {
        Block bup = worldObj == null ? Blocks.planks : worldObj.getBlock(xCoord, yCoord + 1, zCoord);
        int md;
        if (worldObj != null && bup.getMaterial() == Material.wood && bup instanceof BlockLog) {
            md = worldObj.getBlockMetadata(xCoord, yCoord + 1, zCoord);
        } else {
            bup = Blocks.log2;
            md = 1;
        }
        return bup.getIcon(dir.ordinal(), md);
    }

    boolean isLog(Coord c) {
        Block b = c.getBlock();
        return b.getMaterial() == Material.wood && b instanceof BlockLog;
    }

    @Override
    public boolean canPlaceAgainst(final EntityPlayer player, Coord c, int side) {
        final Coord at = getCoord();
        if (!isLog(at.add(ForgeDirection.UP))) {
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

    private class CrowdedCheck implements ICoordFunction {
        Coord at;
        TileEntity crowd = null;
        @Override
        public void handle(Coord here) {
            Chunk chunk = here.getChunk();
            for (TileEntity te : (Iterable<TileEntity>) chunk.chunkTileEntityMap.values()) {
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
}
