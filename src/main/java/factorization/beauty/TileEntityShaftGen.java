package factorization.beauty;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.IRotationalEnergySource;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class TileEntityShaftGen extends TileEntityCommon implements IChargeConductor {
    final Charge charge = new Charge(this);
    double rotor_angle;
    EnumFacing shaft_direction = EnumFacing.DOWN;
    IRotationalEnergySource shaft;
    transient double last_power;
    public static double MAX_POWER = 1024, CHARGE_PER_POWER = 180 * 4;
    boolean on;

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        if (shaftIsBroken()) return "Missing shaft";
        return "";
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        shaft_direction = SpaceUtil.getOrientation(side);
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
        charge.serialize("", data);
        rotor_angle = data.as(Share.VISIBLE, "rotorAngle").putDouble(rotor_angle);
        shaft_direction = data.as(Share.VISIBLE, "shaft_direction").putEnum(shaft_direction);
        on = data.as(Share.VISIBLE, "on").putBoolean(on);
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
    public void updateEntity() {
        if (worldObj.isRemote) return;
        charge.update();
        if (shaftIsBroken()) {
            if (worldObj.getTotalWorldTime() % 5 == 0) {
                shaft = IRotationalEnergySource.adapter.cast(getCoord().add(shaft_direction).getTE());
            }
            return;
        }
        EnumFacing shaftOutputDirection = shaft_direction.getOpposite();
        double avail = shaft.availableEnergy(shaftOutputDirection);
        double usable = Math.min(MAX_POWER, avail);
        last_power = shaft.takeEnergy(shaftOutputDirection, usable);
        int line = (int) (last_power * CHARGE_PER_POWER);
        charge.raiseValue(line);
        rotor_angle += shaft.getVelocity(shaftOutputDirection);
        boolean is_on = line > 0;
        if (is_on != on) {
            on = is_on;
            broadcastMessage(null, NetworkFactorization.MessageType.ShaftGenState, on);
        }
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == NetworkFactorization.MessageType.ShaftGenState) {
            on = input.readBoolean();
            getCoord().redraw();
            // ask for a redraw? Nah; things are getting redrawn constantly anyways
        }
        return super.handleMessageFromServer(messageType, input);
    }

    @Override
    public IIcon getIcon(EnumFacing dir) {
        return BlockIcons.beauty$shaft_gen_side;
    }
}
