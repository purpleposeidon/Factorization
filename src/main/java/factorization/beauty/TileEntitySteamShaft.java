package factorization.beauty;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.IMeterInfo;
import factorization.api.IShaftPowerSource;
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

public class TileEntitySteamShaft extends TileEntityCommon implements IFluidHandler, IShaftPowerSource, IMeterInfo {
    FluidTank steamTank = new FluidTank(/*this,*/ TileEntitySolarBoiler.steam_stack.copy(), 800);

    public static double Z = 1.6;
    public static int TURBINE_MASS = 20 * (10); // force:steam = mass * acceleration --> acceleration = force / mass
    public static double BEARING_DRAG = 0.001; // v -= drag * v**Z
    public static double DRAW_EFFICIENCY = 0.2; // don't let it take *all* of the power in one fell swoop
    // steam_force / mass = drag * velocity ** z
    // terminal velocity = (steam_force / (mass * drag)) ** (1/z)
    // kmplot: f(f,z) = (f / (20 ∙ 0.001))^(1/1.6)

    double velocity = 0, drawable_velocity = 0;
    public double angle = Math.PI * Math.random();
    public double prev_angle = angle;


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
        TURBINE_MASS = NORELEASE.just(1000);
        if (worldObj.isRemote) {
            prev_angle = angle;
            double displayVelocity = Display.limitVelocity(velocity);
            angle += displayVelocity;
            if (displayVelocity > 0) {
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
                force = steam.amount;
                steam.amount = 0;
            }
        }
        double drag = BEARING_DRAG * Math.pow(velocity, Z);
        double acceleration = force / TURBINE_MASS;
        if (acceleration > drag * 1.2) {
            // Slow acceleration so that it looks cool
            double avail = acceleration - drag;
            acceleration = drag + avail * 0.2;
            //new Notice(this, "accel").sendToAll();
        }
        velocity = velocity - drag + acceleration;
        velocity = Math.max(0, velocity);
        drawable_velocity = velocity * DRAW_EFFICIENCY;
        shareTurbineSpeed();
    }

    @SideOnly(Side.CLIENT)
    private void emitParticles() {
        int particleLevel = Minecraft.getMinecraft().gameSettings.particleSetting;
        if (particleLevel >= 2) return;
        double r = 7.0 / 16.0;
        double v = IShaftPowerSource.Display.limitVelocity(velocity) * r;
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
    public double availablePower(ForgeDirection direction) {
        if (direction == ForgeDirection.UP) return drawable_velocity;
        return 0;
    }

    @Override
    public double powerConsumed(ForgeDirection direction, double maxPower) {
        double d = Math.min(drawable_velocity, maxPower);
        drawable_velocity -= d;
        return d;
    }

    @Override
    public double getAngularVelocity(ForgeDirection direction) {
        if (direction == ForgeDirection.UP) return velocity;
        return 0;
    }

    @Override
    public String getInfo() {
        return "Steam: " + steamTank.getFluidAmount() + "mB\n"
                + "Turbine speed: " + (int) velocity;
    }
}
