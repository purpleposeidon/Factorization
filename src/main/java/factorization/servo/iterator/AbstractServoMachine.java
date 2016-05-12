package factorization.servo.iterator;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.*;
import factorization.api.energy.ContextEntity;
import factorization.api.energy.EnergyCategory;
import factorization.api.energy.IWorker;
import factorization.api.energy.WorkUnit;
import factorization.charge.enet.ChargeEnetSubsys;
import factorization.net.INet;
import factorization.servo.instructions.Trap;
import factorization.shared.Core;
import factorization.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public abstract class AbstractServoMachine extends Entity implements IEntityAdditionalSpawnData, INet, IWorker<ContextEntity> {
    // Hey,. why doesn't this extend EntityFZ!?
    public final MotionHandler motionHandler = newMotionHandler();

    public AbstractServoMachine(World world) {
        super(world);
    }

    /**
     * You <b>must</b> call this method instead of using worldObj.spawnEntityInWorld!
     * (Wait, isn't there EntityInit?)
     */
    public void spawnServoMotor(EnumFacing top) {
        motionHandler.beforeSpawn(top);
        worldObj.spawnEntityInWorld(this);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        try {
            putData(new DataInNBT(nbttagcompound));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        try {
            putData(new DataOutNBT(nbttagcompound));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        try {
            putData(new DataInPacket(new ByteBufInputStream(data), Side.CLIENT));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace(); //Hrm! Why? (I mean, besides the obvious.)
        }
    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        try {
            putData(new DataOutPacket(new ByteBufOutputStream(data), Side.SERVER));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace(); //Hrm! Why? (I mean, besides the obvious.)
        }
    }

    public void putData(DataHelper data) throws IOException {
        motionHandler.putData(data);
        waitingForPower = data.as(Share.PRIVATE, "waitingForPower").putBoolean(waitingForPower);
        powered_steps = data.as(Share.PRIVATE, "poweredSteps").putInt(powered_steps);
    }

    protected int powered_steps = 0;
    protected int getMaxBuffer() {
        return 64 * 2;
    }
    protected int getStepsPerUnit() {
        return 64;
    }
    protected boolean waitingForPower = false;

    @Override
    public Accepted accept(ContextEntity context, WorkUnit unit, boolean simulate) {
        if (unit.category != EnergyCategory.ELECTRIC) return Accepted.NEVER;
        if (powered_steps > getMaxBuffer()) {
            return Accepted.LATER;
        }
        if (simulate) return Accepted.NOW;
        powered_steps += getStepsPerUnit();
        if (powered_steps > getMaxBuffer()) {
            if (waitingForPower) {
                setStopped(false);
            }
            waitingForPower = false;
        }
        return Accepted.NOW;
    }

    public void waitForPower() {
        Accepted acc = accept(new ContextEntity(this), ChargeEnetSubsys.CHARGE, true);
        if (acc == Accepted.NEVER || acc == Accepted.LATER) return;
        new Trap().iteratorHit(this);
        if (!isStopped()) return;

        waitingForPower = true;
    }

    public enum ServoMessages {
        servo_brief, servo_item, servo_complete, servo_stopped;
        public static final ServoMessages[] VALUES = values();
    }

    @Override
    public Enum[] getMessages() {
        return ServoMessages.VALUES;
    }


    void broadcast(Enum message_type, Object... msg) {
        FMLProxyPacket p = Core.network.entityPacket(this, message_type, msg);
        Core.network.broadcastPacket(null, getCurrentPos(), p);
    }

    public void broadcastBriefUpdate() {
        try {
            ByteBuf buf = Unpooled.buffer();
            Core.network.prefixEntityPacket(buf, this, ServoMessages.servo_brief);
            DataHelper data = new DataOutByteBuf(buf, Side.SERVER);
            motionHandler.motionAction.serialize("", data);
            FMLProxyPacket toSend = Core.network.entityPacket(buf);
            Core.network.broadcastPacket(null, getCurrentPos(), toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastFullUpdate() {
        try {
            ByteBuf buf = Unpooled.buffer();
            Core.network.prefixEntityPacket(buf, this, ServoMessages.servo_complete);
            DataHelper data = new DataOutByteBuf(buf, Side.SERVER);
            putData(data);
            FMLProxyPacket toSend = Core.network.entityPacket(buf);
            Core.network.broadcastPacket(null, getCurrentPos(), toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (messageType == ServoMessages.servo_stopped) {
            motionHandler.stopped = input.readBoolean();
            return true;
        }
        if (messageType == ServoMessages.servo_brief) {
            try {
                DataHelper data = new DataInByteBuf(input, Side.CLIENT);
                motionHandler.motionAction.serialize("", data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (motionHandler.speed_b > 0) {
                motionHandler.stopped = false;
            }
            return true;
        }
        if (messageType == ServoMessages.servo_complete) {
            try {
                DataHelper data = new DataInByteBuf(input, Side.CLIENT);
                putData(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public Coord getCurrentPos() {
        return motionHandler.motionAction.src.at;
    }

    public Coord getNextPos() {
        return motionHandler.motionAction.dst.at;
    }

    protected abstract void markDirty();

    public abstract boolean extractAccelerationEnergy();

    public boolean extractCharge(int amount) {
        IChargeConductor wire = getCurrentPos().getTE(IChargeConductor.class);
        if (wire == null) {
            return false;
        }
        return wire.getCharge().tryTake(amount) >= amount;
    }

    public void updateSocket() { }

    public void onEnterNewBlock() { }

    public FzOrientation getOrientation() {
        return motionHandler.motionAction.srcFzo;
    }

    public void setNextDirection(EnumFacing direction) {
        motionHandler.nextDirection = direction;
        motionHandler.nextDirectionSet = true;
    }

    public void setTargetSpeed(byte newTarget) {
        motionHandler.setTargetSpeed((byte) (newTarget - 1));
    }

    public byte getTargetSpeed() {
        return (byte) (motionHandler.target_speed_index + 1);
    }

    public void penalizeSpeed() {
        motionHandler.penalizeSpeed();
    }

    public void setStopped(boolean stop) {
        motionHandler.setStopped(stop);
    }

    public boolean isStopped() {
        return motionHandler.stopped;
    }

    @Override
    public void onEntityUpdate() { // updateEntity tick
        super.onEntityUpdate();
        if (isDead) {
            return;
        }
        if (worldObj.isRemote) {
            motionHandler.updateServoMotion();
            updateServoLogic();
        } else {
            if (!getNextPos().blockExists()) return;
            byte orig_speed = motionHandler.speed_b;
            FzOrientation orig_or = getOrientation();
            motionHandler.updateServoMotion();
            updateServoLogic();
            if (orig_speed != motionHandler.speed_b || orig_or != getOrientation()) {
                broadcastBriefUpdate();
                //NOTE: Could be spammy. Speed might be too important to not send tho.
            }
        }
    }

    public abstract void updateServoLogic();

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean p_180426_10_) {
        // The servers sends crappy packets. TODO: Sure would be nice if it didn't send those packets...
    }

    @Override
    public void setPosition(double x, double y, double z) {
        // super.setPosition(pos); //Super does some stupid shit to the bounding box. Does not mess with the chunk location or anything like that.
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        double dp = 1;
        this.setEntityBoundingBox(new AxisAlignedBB(x, y, z, x + dp, y + dp, z + dp));
    }

    protected MotionHandler newMotionHandler() {
        return new MotionHandler(this);
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (damageSource instanceof EntityDamageSourceIndirect) {
            return false;
        }
        Entity src = damageSource.getSourceOfDamage();
        if (!(src instanceof EntityPlayer)) {
            return false;
        }
        if (isDead) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) src;
        if (worldObj.isRemote) return true;
        setDead();
        if (!PlayerUtil.isPlayerCreative(player)) dropItemsOnBreak();
        return true;
    }

    protected abstract void dropItemsOnBreak();
}
