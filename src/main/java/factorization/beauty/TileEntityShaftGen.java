package factorization.beauty;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.IShaftPowerSource;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntityShaftGen extends TileEntityCommon implements IChargeConductor {
    final Charge charge = new Charge(this);
    double rotor_angle;
    ForgeDirection shaft_direction = ForgeDirection.DOWN;
    IShaftPowerSource shaft;
    transient double last_power;
    public static double MAX_POWER = 1024, CHARGE_PER_POWER = 130;
    boolean on;

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        if (shaftIsBroken()) return "Missing shaft";
        return "Power: " + (int) last_power;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
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
            if (shaft == null || ((TileEntity) shaft).isInvalid()) {
                shaft = KineticProxy.connect(new Coord(this), shaft_direction);
            }
        } finally {
            working = false;
        }
    }

    boolean shaftIsBroken() {
        if (shaft == null) return true;
        if (((TileEntity) shaft).isInvalid()) {
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
                shaft = KineticProxy.connect(new Coord(this), ForgeDirection.DOWN);
            }
            return;
        }
        ForgeDirection shaftOutputDirection = shaft_direction.getOpposite();
        double avail = shaft.availablePower(shaftOutputDirection);
        double usable = Math.min(MAX_POWER, avail);
        last_power = shaft.powerConsumed(shaftOutputDirection, usable);
        int line = (int) (last_power * CHARGE_PER_POWER);
        charge.raiseValue(line);
        rotor_angle += shaft.getAngularVelocity(shaftOutputDirection);
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
            // ask for a redraw? Nah; things are getting redrawn constantly anyways
        }
        return super.handleMessageFromServer(messageType, input);
    }

    @Override
    public IIcon getIcon(ForgeDirection dir) {
        if (dir == ForgeDirection.DOWN) {
            return on ? BlockIcons.beauty$shaft_gen_bottom_on : BlockIcons.beauty$shaft_gen_bottom;
        } else if (dir == ForgeDirection.UP) {
            return BlockIcons.beauty$shaft_gen_top;
        } else {
            return on ? BlockIcons.beauty$shaft_gen_side_on : BlockIcons.beauty$shaft_gen_side;
        }
    }
}
