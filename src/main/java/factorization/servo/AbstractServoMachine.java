package factorization.servo;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.IEntityMessage;
import factorization.api.datahelpers.*;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public abstract class AbstractServoMachine extends Entity implements IEntityAdditionalSpawnData, IEntityMessage {
    public final MotionHandler motionHandler = new MotionHandler(this);

    public AbstractServoMachine(World world) {
        super(world);
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

    public void changeOrientation(ForgeDirection fd) {
        motionHandler.changeOrientation(fd);
    }

    public void setNextDirection(ForgeDirection direction) {
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
}
