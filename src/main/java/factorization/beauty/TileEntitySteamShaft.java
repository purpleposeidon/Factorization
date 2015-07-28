package factorization.beauty;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.algos.PIDController;
import factorization.api.IMeterInfo;
import factorization.api.IRotationalEnergySource;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.charge.TileEntitySolarBoiler;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.shared.BlockClass;
import factorization.shared.NORELEASE;
import factorization.shared.NetworkFactorization;
import factorization.shared.TileEntityCommon;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.io.IOException;
import java.util.Random;

public class TileEntitySteamShaft extends TileEntityCommon implements IFluidHandler, IRotationalEnergySource, IMeterInfo {
    FluidTank steamTank = new FluidTank(/*this,*/ TileEntitySolarBoiler.steam_stack.copy(), 800);

    public static double Z = 1.6; // exponent on velocity for determining drag
    public static int TURBINE_MASS = 1000; // force:steam = mass * acceleration --> acceleration = force / mass
    public static double BEARING_DRAG = 0.05; // v -= drag * v**Z // Was 0.001
    public static double DRAW_EFFICIENCY = 0.10; // taking energy causes a loss
    public static double FORCE_PER_STEAM = 2.0;
    // steam_force / mass = drag * velocity ** z
    // terminal velocity = (steam_force / (mass * drag)) ** (1/z)
    // kmplot: f(f,z) = (f / (20 ∙ 0.001))^(1/1.6)

    private double velocity = 0, drawable_velocity = 0;
    public double angle = Math.PI * Math.random();
    public double prev_angle = angle;
    public static double target_fill = 0.5;
    private PIDController pidController = new PIDController(1, 1, 1).setIntegrationFail(1000);
    private int last_take = 1;

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
        pidController.putData(data, "pid");
        last_take = data.as(Share.PRIVATE, "lastTake").putInt(last_take);
    }


    @Override
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
            case UP: return BlockIcons.turbine_top;
            case DOWN: return BlockIcons.turbine_bottom;
            default: return BlockIcons.turbine_side;
        }
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (from == ForgeDirection.DOWN) {
            return steamTank.fill(resource, doFill);
        }
        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (from == ForgeDirection.DOWN && fluid != null) {
            return fluid.getID() == TileEntitySolarBoiler.steam.getID();
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] { steamTank.getInfo() };
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            prev_angle = angle;
            if (velocity != 0) {
                angle += getVelocity(ForgeDirection.UP);
                emitParticles();
            }
            return;
        }
        double force = 0;
        if (worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) == 0) {
            double steam_setpoint = steamTank.getCapacity() * NORELEASE.just(0.9); //target_fill;
            FluidStack steam = steamTank.getFluid();
            if (steam == null) { // Has happened on occasion
                steamTank.setFluid(steam = FluidRegistry.getFluidStack("steam", 0));
            } else {
                // Accelerate slowly, also balances steam consumtion with non-steady inputs
                int DT = 1; //10;
                if (NORELEASE.on) {
                    pidController.adjustPID(0.05, 0.001, 0); // Too responsive
                    pidController.adjustPID(0.05, 0, 0); // Approaches setpoint well, but doesn't adapt
                    pidController.adjustPID(0.5, 0, 0); // Too responsive
                    pidController.adjustPID(0.005, 0, 0); // Gets stuck
                    pidController.adjustPID(0.005, 0.0001, 0); // ranges between 9 & 12
                    pidController.adjustPID(0.0005, 0.00001, 0); // Gets stuck
                    pidController.adjustPID(0.0005, 0.00005, 0); // Settles on a value, but it's a bit too high
                    pidController.adjustPID(0.0005, 0.000001, 0); // Gets *really* stuck
                    pidController.adjustPID(0.0005, 0.000025, 0); // Hey! This works great with setpoint@90% w/ RC! But too slow with FZ
                    pidController.adjustPID(0.005, 0.000025, 0); // Not faster
                    pidController.adjustPID(0.0005, 0.00005, 0); // Uh, hmm
                    pidController.setIntegrationFail(10000000);
                    pidController.setIntegrationFail(10000);
                    pidController.adjustPID(0.0005, 0.005, 0); // Gets stuck at 50
                    pidController.adjustPID(0.0005, 0.05, 0); // Doesn't stabilize
                    pidController.setIntegrationFail(100000);
                    pidController.adjustPID(0.0005, 0.01, 0); // Also doesn't stabilize?
                    pidController.adjustPID(0.0005, 0.02, 0); // Same story
                    pidController.adjustPID(4, 0, 0); // Bouncy
                    pidController.adjustPID(1, 0.05, 0); // Works beautifully with entheas ;_;
                    pidController.adjustPID(0.5, 0.05, 0); // oscillates into negativity
                    pidController.adjustPID(0.5, 0, 0); // too reactive
                    pidController.adjustPID(0.5, 0.01, 0); // doesn't let it drop
                    pidController.adjustPID(0.5, 0.1, 0); // Spends a lot of time trapped in negative-land
                    pidController.adjustPID(0.5, 0.5, 0); // similar
                    pidController.adjustPID(0.5, 0.001, 0); // Blah
                    pidController.adjustPID(0.5, 0.01, 0); //
                    pidController.setIntegrationLimits(-100000, 0);
                    pidController.adjustPID(1, 0.05, 0); // Works beautifully with entheas ;_;
                    pidController.setDt(DT);
                    pidController.adjustPID(0.1, 0.005, 0); // uh negative crap
                    pidController.adjustPID(0.0005, 0.000025, 0); // slow
                    pidController.adjustPID(0.0005, 0.00025, 0); // oscillates
                    pidController.adjustPID(0.0005, 0.00005, 0); // way too slow even w/ the sIL below
                    pidController.setIntegrationLimits(-1000000, 0);
                    pidController.adjustPID(0.0005, 0.0001, 0); // Too slow to start
                    pidController.adjustPID(0.05, 0.0001, 0); // Too reactive
                    pidController.adjustPID(0.05, 0.00001, 0); // Too slow
                    pidController.adjustPID(0.05, 0.00005, 0); // Too reactive
                    pidController.adjustPID(0.0005, 0.000025, 0); // Hey! This works great with setpoint@90% w/ RC! But too slow with FZ
                    pidController.adjustPID(0.05,   0.00002, 0); // Goes lame when it gets bad
                    pidController.adjustPID(0.005,  0.00002, 0.1); //
                    pidController.adjustPID(0.005,  0.00002, 1); //
                }
                if (worldObj.getTotalWorldTime() % DT == 0) {
                    last_take = (int) -pidController.tick(steam_setpoint, steam.amount);
                }
                int takable = last_take;
                String msg = takable + " " + steam.amount + "\n" + pidController.integral + " " + pidController.previous_error;
                new Notice(getCoord().add(0, -3, 0), msg).sendToAll();
                if (takable < 0) takable = 0;
                else if (takable < 1 && steam.amount > steam_setpoint) takable = 1;
                if (takable > steam.amount) takable = steam.amount;
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
        double v = getVelocity(ForgeDirection.UP) * r;
        double bottom = -3.0 / 16.0;
        double left = -4.0 / 16.0;
        double scootch_x = 3.0 / 16.0;
        double scootch_y = 3.0 / 16.0;
        Quaternion rot = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.UP);
        double motFuzz = v / 20;
        Random rng = worldObj.rand;
        double threshold = velocity / (particleLevel == 1 ? 1 : 4);
        for (int side = 0; side < 4; side++) {
            for (int y = 0; y < 3; y++) {
                if (rng.nextFloat() > threshold) continue;
                Vec3 pos = Vec3.createVectorHelper(left + scootch_x * y, bottom + scootch_y * y, r);
                Vec3 mot = Vec3.createVectorHelper(-v + rng.nextGaussian() * motFuzz * 3, rng.nextGaussian() * motFuzz, rng.nextGaussian() * motFuzz + r * 0.125);
                rot.applyRotation(pos);
                rot.applyRotation(mot);

                EntityFXSteam steam = new EntityFXSteam(worldObj, xCoord + 0.5 + pos.xCoord, yCoord + 0.5 + pos.yCoord, zCoord + 0.5 + pos.zCoord, BlockIcons.steam);
                SpaceUtil.toEntVel(steam, mot);
                Minecraft.getMinecraft().effectRenderer.addEffect(steam);
            }
            rot.incrMultiply(Quaternion.getRotationQuaternionRadians(Math.PI / 2, ForgeDirection.UP));
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
    public boolean canConnect(ForgeDirection direction) {
        return direction == ForgeDirection.UP;
    }

    @Override
    public double availableEnergy(ForgeDirection direction) {
        if (direction == ForgeDirection.UP) return drawable_velocity;
        return 0;
    }

    @Override
    public double takeEnergy(ForgeDirection direction, double maxPower) {
        double d = Math.min(drawable_velocity, maxPower);
        drawable_velocity -= d;
        velocity -= d * DRAW_EFFICIENCY;
        return d;
    }

    @Override
    public double getVelocity(ForgeDirection direction) {
        if (direction == ForgeDirection.UP) {
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
        return (int) (Math.toDegrees(getVelocity(ForgeDirection.UP)) * 10 / 3) + " RPM\n"
                + "Power: " + (int) (velocity * 10);
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }
}
