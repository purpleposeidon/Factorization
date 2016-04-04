package factorization.charge;

import factorization.api.Coord;
import factorization.api.HeatConverters;
import factorization.api.IFurnaceHeatable;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.api.energy.*;
import factorization.common.FactoryType;
import factorization.net.StandardMessageType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileEntityHeater extends TileEntityCommon implements IWorker, ITickable {
    public int heat = 0;
    public static final int HEAT_PER_UNIT = 200; // 1600 ticks per coal; 8 smelts per coal.
    public static final int MAX_HEAT = HEAT_PER_UNIT;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    final ContextTileEntity context = new ContextTileEntity(this);
    {
        IWorker.construct(context);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        IWorker.invalidate(context);
    }

    static WorkUnit HEATING = WorkUnit.get(EnergyCategory.THERMAL, new ResourceLocation("factorization:heating"));

    @Override
    public Accepted accept(IWorkerContext context, WorkUnit unit, boolean simulate) {
        if (unit.category != EnergyCategory.ELECTRIC && unit.category != EnergyCategory.THERMAL) {
            return Accepted.NEVER;
        }
        if (unit == HEATING) return Accepted.NEVER;
        if (heat > MAX_HEAT) return Accepted.LATER;
        if (!simulate) {
            heat += HEAT_PER_UNIT;
        }
        return Accepted.NOW;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        heat = data.as(Share.VISIBLE, "heat").putInt(heat);
    }

    int last_heat = -99;

    void updateClient() {
        int delta = Math.abs(heat - last_heat);
        if (delta > 2) {
            broadcastMessage(null, StandardMessageType.SetHeat, heat);
            last_heat = heat;
        }
    }

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == StandardMessageType.SetHeat) {
            heat = input.readInt();
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
            return;
        }

        if (heat < MAX_HEAT) {
            IWorker.requestPower(context);
        }

        if (!hasHeat()) return;
        int action = 0;
        ArrayList<Coord> randomNeighbors = here.getRandomNeighborsAdjacent();
        ArrayList<TileEntityHeater> heaters = null;
        for (Coord c : randomNeighbors) {
            if (action == 0 && c.getBlock() == this.blockType && c.getTE() instanceof TileEntityHeater) {
                if (heaters == null) heaters = new ArrayList<TileEntityHeater>();
                heaters.add(c.getTE(TileEntityHeater.class));
                continue;
            }
            IFurnaceHeatable furnace = HeatConverters.convert(c.w, c.toBlockPos());
            if (furnace != null && sendHeat(furnace)) {
                action++;
                if (!hasHeat()) return;
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
            IFurnaceHeatable adj = HeatConverters.convert(c.w, c.toBlockPos());
            if (adj == null) continue;
            if (heater.sendHeat(adj)) {
                heat -= to_take;
                return;
            }
        }
    }

    boolean hasHeat() {
        return heat > 0;
    }

    boolean sendHeat(IFurnaceHeatable furnace) {
        if (heat <= 0) return false;
        if (!furnace.acceptsHeat()) return false;
        if (furnace.hasLaggyStart()) {
            boolean can_jumpstart = heat >= MAX_HEAT * 0.95;
            if (!can_jumpstart && !furnace.isStarted()) return false;
        }
        for (int i = 1; i <= 2; i++) {
            furnace.giveHeat();
            heat -= 1;
            if (!hasHeat()) break;
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
