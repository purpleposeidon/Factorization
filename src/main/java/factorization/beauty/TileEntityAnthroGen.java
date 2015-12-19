package factorization.beauty;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.Entity;
import net.minecraft.entity.INpc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ITickable;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.charge.TileEntityCaliometricBurner;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;

public class TileEntityAnthroGen extends TileEntityCommon implements IInventory, ICoordFunction, ITickable {
    public static int UPDATE_RATE = 20 * 60 * 7;
    public static int MIN_WANDER_DISTANCE = 12 * 12;
    public static int VILLAGER_CHECKS_PER_ENTHEAS = 8;
    public static final int CHUNK_RANGE = 1;
    ItemStack entheas = new ItemStack(Core.registry.entheas, 0, 0);
    int satisfactory_villagers = 0;
    transient boolean isLit = false;

    static {
        TileEntityCaliometricBurner.register(Core.registry.entheas, 2, 16);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Default;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        entheas = data.as(Share.PRIVATE, "entheas").putItemStack(entheas);
        satisfactory_villagers = data.as(Share.PRIVATE, "foundVillagers").putInt(satisfactory_villagers);
        isLit = data.as(Share.VISIBLE_TRANSIENT, "isLit").putBoolean(ItemUtil.stackSize(entheas) > 0);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ANTHRO_GEN;
    }

    @Override
    public void update() {
        if (worldObj.isRemote) return;
        long now = worldObj.getTotalWorldTime() + this.hashCode();
        if ((now % UPDATE_RATE) != 0) {
            int max_particle_rate = 16;
            if (now % max_particle_rate == 0 && ItemUtil.stackSize(entheas) > 0) {
                now /= max_particle_rate;
                final int entheasRate = 1 + entheas.getMaxStackSize() - entheas.stackSize;
                if (entheasRate > 0 && now % entheasRate == 0) {
                    broadcastMessage(null, NetworkFactorization.MessageType.GeneratorParticles);
                }
            }
            return;
        }
        if (entheas == null) {
            entheas = new ItemStack(Core.registry.entheas, 0, 0);
        }
        if (entheas.stackSize >= entheas.getMaxStackSize()) return;
        Coord at = getCoord();
        int d = 16 * CHUNK_RANGE;
        Coord min = at.add(-d, -d, -d);
        Coord max = at.add(d, d, d);
        Coord.iterateChunks(min, max, this);

        int add_entheas = satisfactory_villagers / VILLAGER_CHECKS_PER_ENTHEAS;
        satisfactory_villagers %= VILLAGER_CHECKS_PER_ENTHEAS;
        int oldSize = entheas.stackSize;
        entheas.stackSize += add_entheas;
        if (entheas.stackSize > entheas.getMaxStackSize()) {
            entheas.stackSize = entheas.getMaxStackSize();
        }
        if (oldSize == 0 && entheas.stackSize > 0) {
            sync();
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == NetworkFactorization.MessageType.GeneratorParticles) {
            worldObj.spawnParticle(EnumParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
            return true;
        }
        return super.handleMessageFromServer(messageType, input);
    }

    @Override
    public void handle(Coord here) {
        for (ClassInheritanceMultiMap<Entity> list : here.getChunk().getEntityLists()) {
            for (Entity ent : list) {
                if (!(ent instanceof INpc)) continue;
                if (hashEnt(ent)) {
                    satisfactory_villagers++;
                }
            }
        }
    }

    private static final String hash_key = "anthrogen_last_seen";
    boolean hashEnt(Entity ent) {
        if (ent.isRiding()) return false;
        if (!ent.onGround) return false;
        NBTTagCompound tag = ent.getEntityData();
        NBTTagCompound sub;
        int oldX, oldY, oldZ;
        if (tag.hasKey(hash_key)) {
            sub = tag.getCompoundTag(hash_key);
            oldX = sub.getInteger("x");
            oldY = sub.getInteger("y");
            oldZ = sub.getInteger("z");
        } else {
            sub = new NBTTagCompound();
            oldX = oldY = oldZ = Integer.MAX_VALUE;
        }
        int newX = (int) ent.posX;
        int newY = (int) ent.posY;
        int newZ = (int) ent.posZ;
        int dx = newX - oldX;
        int dy = newY - oldY;
        int dz = newZ - oldZ;
        int dist = dx * dx + dy * dy + dz * dz;
        if (dist < MIN_WANDER_DISTANCE) return false;
        sub.setInteger("x", newX);
        sub.setInteger("y", newY);
        sub.setInteger("z", newZ);
        tag.setTag(hash_key, sub);
        return true;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) return entheas;
        return null;
    }

    void sync() {
        final Coord at = getCoord();
        at.syncTE();
        at.sendRedraw();
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        int oldSize = ItemUtil.getStackSize(entheas);
        if (slot == 0 && entheas != null && entheas.stackSize > 0) {
            ItemStack ret = entheas.splitStack(amount);
            if (oldSize > 0 && ItemUtil.getStackSize(entheas) <= 0) {
                sync();
            }
            return ret;
        }
        return null;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
		// NORELEASE: Fix me! Come on!
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            boolean oldState = ItemUtil.getStackSize(entheas) > 0;
            entheas = stack;
            boolean newState = ItemUtil.getStackSize(entheas) > 0;
            if (oldState != newState) sync();
        }
    }

    @Override
    public IChatComponent getDisplayName() {
        return new ChatComponentTranslation("fz.AnthroGen");
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
    protected void onRemove() {
        super.onRemove();
        if (ItemUtil.normalize(entheas) == null) return;
        Coord here = new Coord(this);
        InvUtil.spawnItemStack(here, entheas);
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        if (worldObj.isRemote) return;
        if (ItemUtil.normalize(entheas) == null) return;
        InvUtil.givePlayerItem(entityplayer, entheas);
        entheas = new ItemStack(Core.registry.entheas, 0, 0);
        sync();
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return side == EnumFacing.DOWN;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public boolean isUseableByPlayer(EntityPlayer player) { return false; }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }

    @Override
    public void clear() {
        entheas = null;
        satisfactory_villagers = 0;
    }
}
