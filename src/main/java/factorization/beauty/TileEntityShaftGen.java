package factorization.beauty;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.IShaftPowerSource;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntityShaftGen extends TileEntityCommon implements IChargeConductor {
    final Charge charge = new Charge(this);
    double rotor_angle;
    ForgeDirection shaft_direction = ForgeDirection.DOWN;
    IShaftPowerSource shaft;
    transient double last_power;
    public static double MAX_POWER = 1024, CHARGE_PER_POWER = 130;

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
        charge.raiseValue((int) (last_power * CHARGE_PER_POWER));
        rotor_angle += shaft.getAngularVelocity(shaftOutputDirection);
    }
}
