package factorization.beauty;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.api.IMeterInfo;
import factorization.api.IRotationalEnergySource;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.charge.TileEntitySolarBoiler;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.*;

import java.io.IOException;
import java.util.Random;

public class TileEntitySteamShaft extends TileEntityCommon implements IFluidHandler, IRotationalEnergySource, IMeterInfo {
    FluidTank steamTank = new FluidTank(/*this,*/ TileEntitySolarBoiler.steam_stack.copy(), 800);

    public static double Z = 1.6; // exponent on velocity for determining drag
    public static int TURBINE_MASS = 1000; // force:steam = mass * acceleration --> acceleration = force / mass
    public static double BEARING_DRAG = 0.05; // v -= drag * v**Z // Was 0.001
    public static double DRAW_EFFICIENCY = 0.10; // taking energy causes a loss
    public static double FORCE_PER_STEAM = 3.0;
    // steam_force / mass = drag * velocity ** z
    // terminal velocity = (steam_force / (mass * drag)) ** (1/z)
    // kmplot: f(f,z) = (f / (20 ∙ 0.001))^(1/1.6)

    private double velocity = 0, drawable_velocity = 0;
    public double angle = Math.PI * Math.random();
    public double prev_angle = angle;
    private int take_spead = 1;
    private int take_accel = 0;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STEAM_SHAFT;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        velocity = data.as(Share.VISIBLE, "velocity").putDouble(velocity);
        drawable_velocity = data.as(Share.PRIVATE, "drawableVelocity").putDouble(drawable_velocity);
        steamTank = data.as(Share.PRIVATE, "steam").putTank(steamTank);
        take_spead = data.as(Share.PRIVATE, "lastTake").putInt(take_spead);
        if (take_spead < 0) take_spead = 1;
    }


    @Override
    public IIcon getIcon(EnumFacing dir) {
        switch (dir) {
            case UP: return BlockIcons.turbine_top;
            case DOWN: return BlockIcons.turbine_bottom;
            default: return BlockIcons.turbine_side;
        }
    }

    boolean dry = true;

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if (from == EnumFacing.DOWN) {
            if (doFill && resource.amount > 0) dry = false;
            return steamTank.fill(resource, doFill);
        }
        return 0;
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        if (from == EnumFacing.DOWN && fluid != null) {
            return fluid.getID() == TileEntitySolarBoiler.steam.getID();
        }
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        return new FluidTankInfo[] { steamTank.getInfo() };
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            prev_angle = angle;
            if (velocity != 0) {
                angle += getVelocity(EnumFacing.UP);
                emitParticles();
            }
            return;
        }
        double force = 0;
        if (worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) == 0) {
            FluidStack steam = steamTank.getFluid();
            if (steam == null) { // Has happened on occasion
                steamTank.setFluid(steam = FluidRegistry.getFluidStack("steam", 0));
            } else {
                // Pick the right flow rate so that we don't run out of steam
                // TODO: This is stupid. Do it right?
                boolean rare = worldObj.getTotalWorldTime() % 40 == 0;
                int takable = take_spead;
                if (takable < 1) takable = 1;
                if (takable > steam.amount) {
                    takable = steam.amount;
                    if (!dry) {
                        dry = true;
                        take_spead--;
                        if (take_spead < 1) {
                            take_spead = 1;
                        }
                        take_accel = -1;
                    }
                }
                if (steam.amount >= steamTank.getCapacity()) {
                    if (rare) {
                        take_spead += take_accel;
                        take_accel++;
                    }
                } else if (take_accel > 0) {
                    take_accel = 0;
                }
                force = takable * FORCE_PER_STEAM;
                steam.amount -= takable;
            }
        }
        double drag = BEARING_DRAG * Math.pow(Math.abs(velocity), Z);
        if (drag != drag) drag = 0; // Catch NaN
        double acceleration = force / TURBINE_MASS;
        velocity = velocity - drag + acceleration;
        velocity = Math.max(0, velocity);
        drawable_velocity = velocity * DRAW_EFFICIENCY;
        if (velocity > 0 && force == 0 && velocity < 0.03 && worldObj.getTotalWorldTime() % 60 == 0) {
            velocity = 0;
        }
        shareTurbineSpeed();
    }

    @SideOnly(Side.CLIENT)
    private void emitParticles() {
        int particleLevel = Minecraft.getMinecraft().gameSettings.particleSetting;
        if (particleLevel >= 2) return;
        double r = 7.0 / 16.0;
        double v = getVelocity(EnumFacing.UP) * r;
        double bottom = -3.0 / 16.0;
        double left = -4.0 / 16.0;
        double scootch_x = 3.0 / 16.0;
        double scootch_y = 3.0 / 16.0;
        Quaternion rot = Quaternion.getRotationQuaternionRadians(angle, EnumFacing.UP);
        double motFuzz = v / 20;
        Random rng = worldObj.rand;
        double threshold = velocity / (particleLevel == 1 ? 1 : 4);
        for (int side = 0; side < 4; side++) {
            for (int y = 0; y < 3; y++) {
                if (rng.nextFloat() > threshold) continue;
                Vec3 pos = new Vec3(left + scootch_x * y, bottom + scootch_y * y, r);
                Vec3 mot = new Vec3(-v + rng.nextGaussian() * motFuzz * 3, rng.nextGaussian() * motFuzz, rng.nextGaussian() * motFuzz + r * 0.125);
                rot.applyRotation(pos);
                rot.applyRotation(mot);

                EntityFXSteam steam = new EntityFXSteam(worldObj, xCoord + 0.5 + pos.xCoord, yCoord + 0.5 + pos.yCoord, zCoord + 0.5 + pos.zCoord, BlockIcons.steam);
                SpaceUtil.toEntVel(steam, mot);
                Minecraft.getMinecraft().effectRenderer.addEffect(steam);
            }
            rot.incrMultiply(Quaternion.getRotationQuaternionRadians(Math.PI / 2, EnumFacing.UP));
        }
    }

    private transient double last_sent_velocity = 0;
    public void shareTurbineSpeed() {
        if (last_sent_velocity == velocity) return;
        if (NumUtil.significantChange(last_sent_velocity, velocity, 0.10)) {
            last_sent_velocity = velocity;
            broadcastMessage(null, NetworkFactorization.MessageType.TurbineSpeed, (float) velocity);
        }
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == NetworkFactorization.MessageType.TurbineSpeed) {
            velocity = input.readFloat();
            return true;
        }
        return false;
    }

    @Override
    public boolean canConnect(EnumFacing direction) {
        return direction == EnumFacing.UP;
    }

    @Override
    public double availableEnergy(EnumFacing direction) {
        if (direction == EnumFacing.UP) return drawable_velocity;
        return 0;
    }

    @Override
    public double takeEnergy(EnumFacing direction, double maxPower) {
        double d = Math.min(drawable_velocity, maxPower);
        drawable_velocity -= d;
        velocity -= d * DRAW_EFFICIENCY;
        return d;
    }

    @Override
    public double getVelocity(EnumFacing direction) {
        if (direction == EnumFacing.UP) {
            if (velocity > MAX_SPEED) return MAX_SPEED;
            if (velocity < -MAX_SPEED) return -MAX_SPEED;
            return velocity;
        }
        return 0;
    }

    @Override
    public boolean isTileEntityInvalid() {
        return this.isInvalid();
    }

    @Override
    public String getInfo() {
        return FzUtil.toRpm(getVelocity(EnumFacing.UP))
                + "\nPower: " + (int) (velocity * 10)
                + "\nSteam: " + steamTank.getFluidAmount() + "mB"
                + (!Core.dev_environ ? "" :
                        "\nTake-speed: " + take_spead
                        + "\nLast-sync: " + last_sent_velocity
                        + "\nAccel: " + take_accel);
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }
}
