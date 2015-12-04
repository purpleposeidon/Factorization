package factorization.servo;

import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.IEntityMessage;
import factorization.api.datahelpers.*;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public abstract class AbstractServoMachine extends Entity implements IEntityAdditionalSpawnData, IEntityMessage {
    // Hey,. why doesn't this extend EntityFZ!?
    public final MotionHandler motionHandler = newMotionHandler();

    public AbstractServoMachine(World world) {
        super(world);
    }

    /**
     * You <b>must</b> call this method instead of using worldObj.spawnEntityInWorld!
     * (Wait, isn't there EntityInit?)
     */
    public void spawnServoMotor() {
        motionHandler.beforeSpawn();
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
    }

    void broadcast(NetworkFactorization.MessageType message_type, Object... msg) {
        FMLProxyPacket p = Core.network.entityPacket(this, message_type, msg);
        Core.network.broadcastPacket(null, getCurrentPos(), p);
    }

    public void broadcastBriefUpdate() {
        Coord a = getCurrentPos();
        Coord b = getNextPos();
        broadcast(NetworkFactorization.MessageType.servo_brief, (byte) motionHandler.orientation.ordinal(), motionHandler.speed_b,
                a.x, a.y, a.z,
                b.x, b.y, b.z,
                motionHandler.pos_progress);
    }

    public void broadcastFullUpdate() {
        try {
            ByteBuf buf = Unpooled.buffer();
            Core.network.prefixEntityPacket(buf, this, NetworkFactorization.MessageType.servo_complete);
            DataHelper data = new DataOutByteBuf(buf, Side.SERVER);
            putData(data);
            FMLProxyPacket toSend = Core.network.entityPacket(buf);
            Core.network.broadcastPacket(null, getCurrentPos(), toSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessageFromClient(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == NetworkFactorization.MessageType.servo_stopped) {
            motionHandler.stopped = input.readBoolean();
            return true;
        }
        if (messageType == NetworkFactorization.MessageType.servo_brief) {
            Coord a = getCurrentPos();
            Coord b = getNextPos();
            FzOrientation no = FzOrientation.getOrientation(input.readByte());
            setOrientation(no);
            motionHandler.speed_b = input.readByte();
            a.x = input.readInt();
            a.y = input.readInt();
            a.z = input.readInt();
            b.x = input.readInt();
            b.y = input.readInt();
            b.z = input.readInt();
            motionHandler.pos_progress = input.readFloat();
            if (motionHandler.speed_b > 0) {
                motionHandler.stopped = false;
            }
            return true;
        }
        if (messageType == NetworkFactorization.MessageType.servo_complete) {
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
        return motionHandler.pos_prev;
    }

    public Coord getNextPos() {
        return motionHandler.pos_next;
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
        return motionHandler.orientation;
    }

    public void setOrientation(FzOrientation orientation) {
        motionHandler.orientation = orientation;
    }

    public void changeOrientation(EnumFacing fd) {
        motionHandler.changeOrientation(fd);
    }

    public void setNextDirection(EnumFacing direction) {
        motionHandler.nextDirection = direction;
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
            FzOrientation orig_or = motionHandler.orientation;
            motionHandler.updateServoMotion();
            updateServoLogic();
            if (orig_speed != motionHandler.speed_b || orig_or != motionHandler.orientation) {
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
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int three) {
        // The servers sends crappy packets. TODO: Sure would be nice if it didn't send those packets...
    }

    @Override
    public void setPosition(double x, double y, double z) {
        // super.setPosition(x, y, z); //Super does some stupid shit to the bounding box. Does not mess with the chunk location or anything like that.
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        double dp = 1;
        this.boundingBox.setBounds(x, y, z, x + dp, y + dp, z + dp);
        /*
        double neg_size = -0.25;
        double pos_size = 0.75;
        double height = 2F/16F;
        double dy = 0.5;
        this.boundingBox.setBounds(x - neg_size, dy + y - height, z - neg_size, x + pos_size, dy + y + height, z + pos_size);
        */
    }

    @Override
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
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
