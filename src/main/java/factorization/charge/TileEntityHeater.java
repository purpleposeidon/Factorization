package factorization.charge;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityCommon;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.util.ITickable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileEntityHeater extends TileEntityCommon implements IChargeConductor, ITickable {
    Charge charge = new Charge(this);
    public byte heat = 0;
    public static final byte maxHeat = 32;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }
    
    @Override
    public IIcon getIcon(EnumFacing dir) {
        return BlockIcons.heater_spiral;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        charge.serialize("", data);
        heat = data.as(Share.VISIBLE, "heat").putByte(heat);
    }
    
    int charge2heat(int i) {
        return (int) (i / 1.5);
    }

    byte last_heat = -99;

    void updateClient() {
        int delta = Math.abs(heat - last_heat);
        if (delta > 2) {
            broadcastMessage(null, MessageType.HeaterHeat, heat);
            last_heat = heat;
        }
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.HeaterHeat) {
            heat = input.readByte();
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        if (worldObj.isRemote) {
            return;
        }
        updateClient();
        Coord here = getCoord();
        if (here.isPowered()) {
            charge.update();
            return;
        }
        long now = worldObj.getTotalWorldTime() + here.seed();
        int rate = 4;
        if (now % rate == 0) {
            int heatToRemove = maxHeat - heat;
            int avail = Math.min(heatToRemove, charge.getValue());
            if (avail > 0 && charge2heat(heatToRemove) > 0) {
                heat += charge2heat(charge.deplete(heatToRemove));
            }
        }
        charge.update();

        if (!canSendHeat()) return;
        int recurs = 0, action = 0;
        ArrayList<Coord> randomNeighbors = here.getRandomNeighborsAdjacent();
        ArrayList<TileEntityHeater> heaters = null;
        for (Coord c : randomNeighbors) {
            if (action == 0 && c.getBlock() == this.blockType && c.getTE() instanceof TileEntityHeater) {
                if (heaters == null) heaters = new ArrayList<TileEntityHeater>();
                heaters.add(c.getTE(TileEntityHeater.class));
                continue;
            }
            IFurnaceHeatable furnace = HeatConverters.convert(c.w, c.x, c.y, c.z);
            if (furnace != null && sendHeat(furnace)) {
                action++;
                if (!canSendHeat()) return;
            }
        }
        if (action == 0 && heaters != null) {
            for (TileEntityHeater heater : heaters) {
                recursiveHeat(heater);
            }
        }
    }

    void recursiveHeat(TileEntityHeater heater) {
        int to_take = 2;
        if (heat < to_take) {
            return;
        }
        for (Coord c : heater.getCoord().getRandomNeighborsAdjacent()) {
            IFurnaceHeatable adj = HeatConverters.convert(c.w, c.x, c.y, c.z);
            if (adj == null) continue;
            if (heater.sendHeat(adj)) {
                heat -= to_take;
                return;
            }
        }
    }

    boolean shouldHeat(int cookTime) {
        if (heat >= maxHeat * 0.5) {
            return true;
        }
        return cookTime > 0;
    }

    boolean canSendHeat() {
        return heat > maxHeat / 2;
    }

    boolean sendHeat(IFurnaceHeatable furnace) {
        if (!furnace.acceptsHeat()) return false;
        if (furnace.hasLaggyStart()) {
            boolean can_jumpstart = heat >= maxHeat * 0.95;
            if (!can_jumpstart && !furnace.isStarted()) return false;
        }
        boolean any = false;
        for (int i = 1; i <= 2; i++) {
            furnace.giveHeat();
            heat -= i;
            any = true;
            if (!canSendHeat()) return true;
        }
        return true;
    }

    void cookEntity(Entity ent) {
        if (worldObj.isRemote) return;
        if (ent == null) return;
        if (heat <= 8) return;
        if (ent.isBurning()) return;
        if (!ent.canBePushed()) return;
        if (!(ent instanceof EntityLivingBase)) return;
        if (getCoord().isPowered()) return;
        heat -= 8;
        ent.setFire(1);
    }

    @Override
    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List<AxisAlignedBB> list, Entity entity) {
        cookEntity(entity);
        return super.addCollisionBoxesToList(block, aabb, list, entity);
    }
}
