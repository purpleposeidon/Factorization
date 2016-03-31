package factorization.charge.enet;

import factorization.api.Coord;
import factorization.api.IMeterInfo;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.api.energy.ContextTileEntity;
import factorization.common.FactoryType;
import factorization.net.StandardMessageType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.ItemUtil;
import factorization.util.NumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.io.IOException;

public class TileEntityLeydenJar extends TileEntityCommon implements IMeterInfo, ITickable {
    int storage = 0;
    public static final int MAX_STORAGE = 64;
    transient byte last_light = -1;
    // NORELEASE: Add ChargeSparks to the leyden jar.

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.MachineDynamicLightable;
    }
    
    public double getLevel() {
        return ((double)storage) / MAX_STORAGE;
    }

    @Override
    public String getInfo() {
        return "Storage: " + (int)(getLevel()*100) + "%";
    }

    final ContextTileEntity above = new ContextTileEntity(this, EnumFacing.UP, null);
    final ContextTileEntity below = new ContextTileEntity(this, EnumFacing.DOWN, null);

    boolean suckInPower() {
        if (storage >= MAX_STORAGE) return false;
        WireLeader leader = ChargeEnetSubsys.instance.getLeader(below);
        if (leader == null) return false;
        int wires = leader.members.size();
        if (wires <= 0) return false;
        int power = leader.powerSum;
        if (power < wires) return false;
        leader.powerSum--;
        storage++;
        return true;
    }

    boolean distributePower() {
        if (storage <= 0) return false;
        WireLeader leader = ChargeEnetSubsys.instance.getLeader(above);
        if (leader == null) return false;
        int wires = leader.members.size();
        int power = leader.powerSum;
        if (power > wires * 2) return false;
        leader.powerSum++;
        storage--;
        return true;
    }

    @Override
    public void update() {
        final Coord here = getCoord();
        if (worldObj.isRemote) {
            updateLight(here);
            return;
        }
        suckInPower();
        if (here.isPowered()) {
            return;
        }
        boolean change = suckInPower() | /* non-short circuiting! */ distributePower();
        if (change) {
            updateLight(here);
            markDirty();
            updateClients();
        }
    }

    protected void updateLight(Coord here) {
        byte now_light = (byte) getDynamicLight();
        if (last_light == -1) {
            last_light = now_light;
        }
        if (Math.abs(last_light - now_light) > 1) {
            last_light = now_light;
            here.updateBlockLight();
            here.redraw();
        }
    }

    transient int last_storage = -1;
    
    void updateClients() {
        if (storage != last_storage) {
            if (NumUtil.significantChange(storage, last_storage, 0.05F)) {
                broadcastMessage(null, StandardMessageType.SetAmount, storage);
                last_storage = storage;
            }
        }
    }
    
    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == StandardMessageType.SetAmount) {
            storage = input.readInt();
            return true;
        }
        return false;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        storage = data.as(Share.VISIBLE, "store").putInt(storage);
        if (storage > MAX_STORAGE) {
            storage = MAX_STORAGE;
        }
    }

    @Override
    public void loadFromStack(ItemStack is) {
        super.loadFromStack(is);
        storage = ItemUtil.getTag(is).getInteger("storage");
        if (storage > MAX_STORAGE) {
            storage = MAX_STORAGE;
        }
    }
    
    @Override
    public int getDynamicLight() {
        return (int) (getLevel()*7);
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        int md = storage > 0 ? 1 : 0;
        ItemStack is = new ItemStack(Core.registry.leyden_jar, 1, md);
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setInteger("storage", storage);
        return is;
    }
    
    @Override
    public int getComparatorValue(EnumFacing side) {
        return (int) (getLevel()*0xF);
    }
}
