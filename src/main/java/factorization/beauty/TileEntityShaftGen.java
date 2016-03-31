package factorization.beauty;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.api.energy.ContextTileEntity;
import factorization.api.energy.IEnergyNet;
import factorization.api.energy.WorkUnit;
import factorization.charge.enet.ChargeEnetSubsys;
import factorization.common.FactoryType;
import factorization.net.StandardMessageType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.io.IOException;

public class TileEntityShaftGen extends TileEntityCommon implements ITickable, IMeterInfo {
    EnumFacing shaft_direction = EnumFacing.DOWN;
    IRotationalEnergySource shaft;
    double received_power = 0;
    public static double MAX_POWER = 1024;
    public static int UNITS_PER_POWER = 8;
    public static WorkUnit CHARGE = ChargeEnetSubsys.CHARGE;
    boolean on;

    @Override
    public String getInfo() {
        if (shaftIsBroken()) return "Missing shaft";
        if (Core.dev_environ) {
            return received_power + "/" + UNITS_PER_POWER;
        }
        return "";
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        shaft_direction = side;
        if (player.isSneaking()) {
            shaft_direction = shaft_direction.getOpposite();
            return;
        }
        Coord at = getCoord();
        EnumFacing use = null;
        for (EnumFacing dir : EnumFacing.VALUES) {
            IRotationalEnergySource res = IRotationalEnergySource.adapter.cast(at.add(dir).getTE());
            if (res == null) continue;
            if (res.canConnect(dir.getOpposite())) {
                if (use != null) return;
                use = dir;
            }
        }
        if (use == null) return;
        shaft_direction = use;
    }

    @Override
    public boolean rotate(EnumFacing axis) {
        if (axis == shaft_direction) return false;
        shaft_direction = axis;
        return true;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        shaft_direction = data.as(Share.VISIBLE, "shaft_direction").putEnum(shaft_direction);
        on = data.as(Share.VISIBLE, "on").putBoolean(on);
        received_power = data.as(Share.PRIVATE, "receivedPower").putDouble(received_power);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SHAFT_GEN;
    }


    boolean working = false;
    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        super.onNeighborTileChanged(tilex, tiley, tilez);
        if (working) return;
        working = true;
        try {
            if (shaft == null || shaft.isTileEntityInvalid()) {
                shaft = IRotationalEnergySource.adapter.cast(getCoord().adjust(shaft_direction).getTE());
            }
        } finally {
            working = false;
        }
    }

    boolean shaftIsBroken() {
        if (shaft == null) return true;
        if (shaft.isTileEntityInvalid()) {
            shaft = null;
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        if (worldObj.isRemote) return;
        if (shaftIsBroken()) {
            if (worldObj.getTotalWorldTime() % 5 == 0) {
                shaft = IRotationalEnergySource.adapter.cast(getCoord().add(shaft_direction).getTE());
            }
            return;
        }
        EnumFacing shaftOutputDirection = shaft_direction.getOpposite();
        double avail = shaft.availableEnergy(shaftOutputDirection);
        double usable = Math.min(MAX_POWER, avail);
        double got = shaft.takeEnergy(shaftOutputDirection, usable);
        received_power += got;
        if (received_power > UNITS_PER_POWER) {
            IEnergyNet.offer(new ContextTileEntity(this), CHARGE);
            received_power -= UNITS_PER_POWER;
        }
        boolean is_on = got > 0;
        if (is_on != on) {
            on = is_on;
            broadcastMessage(null, StandardMessageType.SetWorking, on);
        }
    }

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (messageType == StandardMessageType.SetWorking) {
            on = input.readBoolean();
            getCoord().redraw();
            // ask for a redraw? Nah; things are getting redrawn constantly anyways
        }
        return super.handleMessageFromServer(messageType, input);
    }
}
