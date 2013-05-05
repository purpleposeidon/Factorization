package factorization.common.servo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.IEntityMessage;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataInPacket;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.DataOutPacket;
import factorization.api.datahelpers.Share;
import factorization.common.Core;

public class ServoMotor extends Entity implements IEntityAdditionalSpawnData,
        ICoord, IEntityMessage {
    Controller controller = new Controller();

    boolean dampenVelocity;

    Coord pos;
    ForgeDirection direction = ForgeDirection.UNKNOWN,
            nextDirection = ForgeDirection.UNKNOWN;
    double speed;
    double accumulated_motion;
    double percent_complete;

    static final double maxSpeed = 0.1, slowedSpeed = maxSpeed / 20,
            minSpeed = slowedSpeed / 10;

    private static class MessageType {
        static final short motor_speed = 100, motor_direction = 101,
                motor_position = 102;
    }

    public ServoMotor(World world) {
        super(world);
        setSize(1, 1);
        double d = 0.5;
        boundingBox.minX = -d;
        boundingBox.minY = -d;
        boundingBox.minZ = -d;
        boundingBox.maxX = +d;
        boundingBox.maxY = +d;
        boundingBox.maxZ = +d;
        pos = new Coord(world, 0, 0, 0);
    }

    @Override
    protected void entityInit() {
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
    public void readSpawnData(ByteArrayDataInput data) {
        try {
            putData(new DataInPacket(data, Side.CLIENT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeSpawnData(ByteArrayDataOutput data) {
        try {
            putData(new DataOutPacket(data, Side.SERVER));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void putData(DataHelper data) throws IOException {
        data.as(Share.VISIBLE, "controller");
        controller.putData(data);
        byte d = (byte) direction.ordinal();
        d = data.as(Share.VISIBLE, "direction").putByte(d);
        direction = ForgeDirection.getOrientation(d);
        speed = data.as(Share.VISIBLE, "speed").putDouble(speed);
        accumulated_motion = data.as(Share.VISIBLE, "accumulated_motion").putDouble(accumulated_motion);
        percent_complete = data.as(Share.VISIBLE, "percent_complete").putDouble(percent_complete);
        pos = data.as(Share.VISIBLE, "pos").put(pos);
    }

    boolean validPosition(Coord c) {
        return c.getTE(TileEntityServoRail.class) != null;
    }

    boolean validDirection(ForgeDirection dir) {
        if (dir.offsetX + dir.offsetY + dir.offsetZ == -1) {
            return validPosition(getCoord());
        } else {
            return validPosition(getCoord().add(dir));
        }
    }

    void checkDirection() {
        if (validPosition(getCoord())) {
            return;
        }
        if (validDirection(direction)) {
            return;
        }
        if (validDirection(nextDirection)) {
            swapDirections();
            return;
        }
        speed = 0;
        direction = ForgeDirection.UNKNOWN;
    }

    void swapDirections() {
        ForgeDirection shwahpah = direction;
        direction = nextDirection;
        nextDirection = shwahpah;
    }

    @Override
    public void onEntityUpdate() {
        if (ticksExisted == 1) {
            checkDirection();
        }
        super.onEntityUpdate();
        if (worldObj.isRemote) {
            doLogic();
        } else {
            boolean first = ticksExisted == 1;
            double old_speed = speed;
            ForgeDirection old_dir = direction;
            ForgeDirection old_next_dir = nextDirection;
            boolean at_stop = old_speed == 0 || old_dir == ForgeDirection.UNKNOWN;
            doLogic();
            if (old_speed != speed || first) {
                speedChanged();
            }
            if (old_dir != direction || old_next_dir != nextDirection || first) {
                directionChanged();
            }
            broadcast(MessageType.motor_position, (float) posX, (float) posY, (float) posZ); //NORELEASE
        }
        setPosition(posX, posY, posZ);
    }

    void doLogic() {
        double x = posX, y = posY, z = posZ;
        worldObj.spawnParticle("reddust", x, y, z, 0, 0, 0);

        if (!worldObj.isRemote) {
            if (dampenVelocity || hasSignal(Signal.STOP_MOTOR) || hasSignal(Signal.SLOW_MOTOR)) {
                speed = speed * 2 / 3;
                if (speed < slowedSpeed) {
                    dampenVelocity = false;
                    if (hasSignal(Signal.STOP_MOTOR)) {
                        if (speed < minSpeed) {
                            speed = 0;
                            if (direction != ForgeDirection.UNKNOWN) {
                                nextDirection = direction;
                                direction = ForgeDirection.UNKNOWN;
                            }
                        }
                    }
                }
            } else if (direction != ForgeDirection.UNKNOWN && speed < maxSpeed) {
                accelerate(); // NORELEASE
                /*
                 * long now = worldObj.getTotalWorldTime(); if (now % 10 == 0) {
                 * IChargeConductor conductor =
                 * here.getTE(IChargeConductor.class); if (conductor != null) {
                 * int drain = 4; if (conductor.getCharge().tryTake(drain) >=
                 * drain) { accelerate(); } } }
                 */
            }
        }
        if (controller != null) {
            controller.doUpdate(this);
        }
        if (speed <= 0 || direction == ForgeDirection.UNKNOWN) {
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (percent_complete >= 1) {
            percent_complete = 0;
            onEnterNewBlock();
            if (pickNextDirection()) {
                // We're relying on the speed being < 1 here.
                moveMotor();
            }
        }
    }

    static ThreadLocal<ArrayList<ForgeDirection>> direction_cache = new ThreadLocal<ArrayList<ForgeDirection>>();

    static ArrayList<ForgeDirection> getDirs() {
        ArrayList<ForgeDirection> ret = direction_cache.get();
        if (ret == null) {
            ret = new ArrayList(6);
            for (int i = 0; i < 6; i++) {
                ret.add(ForgeDirection.getOrientation(i));
            }
            direction_cache.set(ret);
        }
        return ret;
    }

    boolean pickNextDirection() {
        ForgeDirection opposite = direction.getOpposite();
        if (direction != ForgeDirection.UNKNOWN && validPosition(getCoord().add(direction))) {
            return true;
        }
        if (nextDirection != ForgeDirection.UNKNOWN && validDirection(nextDirection)) {
            swapDirections();
            return true;
        }
        ArrayList<ForgeDirection> dirs = getDirs();
        Collections.shuffle(dirs);
        for (int i = 0; i < 6; i++) {
            ForgeDirection d = dirs.get(i);
            if (d == opposite || d == direction || d == nextDirection) {
                continue;
            }
            if (validDirection(d)) {
                nextDirection = direction;
                direction = d;
                return true;
            }
        }
        direction = nextDirection = ForgeDirection.UNKNOWN;
        return false;
    }

    void accelerate() {
        speed += maxSpeed / (20 * 6);
        speed = Math.min(speed, maxSpeed);
        speedChanged();
    }

    void broadcast(short message_type, Object... msg) {
        Packet p = Core.network.entityPacket(this, message_type, msg);
        Core.network.broadcastPacket(worldObj, (int) posX, (int) posY, (int) posZ, p);
    }

    void speedChanged() {
        if (worldObj.isRemote) {
            return;
        }
        broadcast(MessageType.motor_speed, (float) speed);
    }

    void directionChanged() {
        if (worldObj.isRemote) {
            return;
        }
        broadcast(MessageType.motor_direction, (byte) direction.ordinal(), (byte) nextDirection.ordinal());
    }

    double fraction(double v) {
        return v - ((int) v);
    }

    void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - percent_complete);
        posX = pos.x + direction.offsetX * move;
        posY = pos.y + direction.offsetY * move;
        posZ = pos.z + direction.offsetZ * move;
        accumulated_motion -= move;
        percent_complete += move;
        if (percent_complete >= 1) {
            pos = pos.add(direction);
        }
    }

    @Override
    public Coord getCoord() {
        return pos.copy();
    }

    boolean hasSignal(Signal signal) {
        return false;
    }

    void onEnterNewBlock() {
    }

    @Override
    public boolean interact(EntityPlayer player) {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, int damage) {
        if (damageSource instanceof EntityDamageSourceIndirect) {
            return false;
        }
        Entity src = damageSource.getSourceOfDamage();
        if (!(src instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) src;
        if (!worldObj.isRemote) {
            setDead();
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + (worldObj.isRemote ? " client" : " server") + " " + speed;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int three) {
        // The servers sends crappy packets. TODO: Sure would be nice if it didn't send those packets...
    }

    @Override
    public void setPosition(double x, double y, double z) {
        // super.setPosition(x, y, z); //... really. Great.
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        double a = 0;
        double b = 1;
        this.boundingBox.setBounds(x - a, y - a, z - a, x + b, y + b, z + b);
    }

    @Override
    public void setPositionAndRotation(double x, double y, double z, float yaw,
            float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    @Override
    public boolean handleMessage(short messageType, DataInputStream input)
            throws IOException {
        System.out.println("MotorMessage: " + messageType); // NORELEASE
        switch (messageType) {
        case MessageType.motor_speed:
            speed = input.readFloat();
            return true;
        case MessageType.motor_direction:
            direction = ForgeDirection.getOrientation(input.readByte());
            nextDirection = ForgeDirection.getOrientation(input.readByte());
            return true;
        case MessageType.motor_position:
            double x = posX, y = posY, z = posZ;
            worldObj.spawnParticle("reddust", input.readFloat(), input.readFloat(), input.readFloat(), -1, 1, 0);
            //setPosition(input.readFloat(), input.readFloat(), input.readFloat());
            return true;
        }
        return false;
    }
}
