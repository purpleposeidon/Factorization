package factorization.common.servo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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
import factorization.api.FzOrientation;
import factorization.api.IEntityMessage;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataInPacket;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.DataOutPacket;
import factorization.api.datahelpers.Share;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.servo.controllers.DummyController;

public class ServoMotor extends Entity implements IEntityAdditionalSpawnData, IEntityMessage {
    Controller controller = new DummyController();

    boolean dampenVelocity;

    Coord pos;
    FzOrientation prevOrientation = FzOrientation.UNKNOWN, orientation = FzOrientation.UNKNOWN, nextOrientation = FzOrientation.UNKNOWN;
    private byte speed_b;
    private static final double max_speed_b = 127;
    double accumulated_motion;
    double percent_complete;
    
    boolean new_motor = true;
    
    //For client-side rendering
    double gear_rotation = 0, prev_gear_rotation = 0;
    double servo_reorient = 0, prev_servo_reorient = 0;

    static final double maxSpeed = 0.05 /* NORELEASE: 0.1 */, slowedSpeed = maxSpeed / 20, minSpeed = slowedSpeed / 10;

    private static class MessageType {
        static final short motor_speed = 100, motor_direction = 101, motor_position = 102;
    }

    public ServoMotor(World world) {
        super(world);
        setSize(1, 1);
        double d = 0.5;
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
        if (!worldObj.isRemote) {
            initPosition();
        }
        data.as(Share.VISIBLE, "controller");
        controller.putData(data);
        prevOrientation = data.as(Share.PRIVATE, "prevOrient").putFzOrientation(prevOrientation);
        orientation = data.as(Share.VISIBLE, "Orient").putFzOrientation(orientation);
        nextOrientation = data.as(Share.VISIBLE, "nextOrient").putFzOrientation(nextOrientation);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        accumulated_motion = data.as(Share.PRIVATE, "accumulated_motion").putDouble(accumulated_motion);
        percent_complete = data.as(Share.VISIBLE, "percent_complete").putDouble(percent_complete);
        pos = data.as(Share.VISIBLE, "pos").put(pos);
        if (data.isReader()) {
            pos.w = worldObj;
        }
        new_motor = data.as(Share.PRIVATE, "new").putBoolean(new_motor);
    }

    boolean validPosition(Coord c) {
        return c.getTE(TileEntityServoRail.class) != null;
    }

    boolean validDirection(ForgeDirection dir) {
        return validPosition(getCurrentPos().add(dir));
    }

    void checkDirection() {
        if (validDirection(orientation.facing)) {
            return;
        }
        if (validDirection(nextOrientation.facing)) {
            swapOrientations();
            return;
        }
        speed_b = 0;
        orientation = FzOrientation.UNKNOWN;
    }

    void swapOrientations() {
        FzOrientation shwahpah = orientation;
        orientation = nextOrientation;
        nextOrientation = shwahpah;
    }
    
    void initPosition() {
        new_motor = false;
        if (!worldObj.isRemote) {
            pos = new Coord(this);
        }
    }

    private boolean need_description_packet = false;
    private static Quaternion target_orientation = new Quaternion();
    @Override
    public void onEntityUpdate() {
        if (new_motor) {
            initPosition(); //TODO: Is that Coord field necessary?
            if (prevOrientation == FzOrientation.UNKNOWN && orientation != FzOrientation.UNKNOWN) {
                prevOrientation = orientation;
            }
        }
        if (ticksExisted == 1) {
            checkDirection();
            if (!worldObj.isRemote) {
                pos.w = worldObj;
                
            }
        }
        super.onEntityUpdate();
        if (worldObj.isRemote) {
            doLogic();
        } else {
            byte orig_speed = speed_b;
            doLogic();
            need_description_packet |= orig_speed != speed_b;
            if (need_description_packet) {
                need_description_packet  = false;
                describe();
            }
//			if (old_speed != speed_b || first) {
//				speedChanged();
//			}
//			if (old_dir != direction || old_next_dir != nextDirection || first) {
//				orientationChanged();
//			}
            broadcast(MessageType.motor_position, (float) posX, (float) posY, (float) posZ); //NORELEASE
        }
        setPosition(posX, posY, posZ);
    }

    void doLogic() {
        double x = posX, y = posY, z = posZ;
        worldObj.spawnParticle("reddust", x, y, z, 0, 0, 0);
        
        if (orientation == FzOrientation.UNKNOWN) {
            if (nextOrientation != FzOrientation.UNKNOWN) {
                swapOrientations();
            } else {
                pickNextOrientation();
            }
        }
        
        if (!worldObj.isRemote) {
            final double speed = getSpeed();
            if (dampenVelocity || hasSignal(Signal.STOP_MOTOR) || hasSignal(Signal.SLOW_MOTOR)) {
                speed_b = (byte) (speed_b * 2 / 3);
                if (speed < slowedSpeed) {
                    dampenVelocity = false;
                    if (hasSignal(Signal.STOP_MOTOR)) {
                        if (speed < minSpeed) {
                            speed_b = 0;
                        }
                    }
                }
            } else if (orientation != FzOrientation.UNKNOWN && speed < maxSpeed) {
                accelerate(); // NORELEASE
//				long now = worldObj.getTotalWorldTime();
//				if (now % 10 == 0) {
//					IChargeConductor conductor = getCurrentPos().getTE(IChargeConductor.class);
//					if (conductor != null) {
//						int drain = 4;
//						if (conductor.getCharge().tryTake(drain) >= drain) {
//							accelerate();
//						}
//					}
//				}
            }
        }
        if (controller != null) {
            controller.doUpdate(this);
        }
        final double speed = getSpeed();
        if (speed <= 0 || orientation == FzOrientation.UNKNOWN) {
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (percent_complete >= 1) {
            percent_complete = 0;
            pos = pos.add(orientation.facing);
            onEnterNewBlock();
            if (pickNextOrientation()) {
                // We're relying on the speed being < 1 here.
                moveMotor();
            }
        }
    }
    
    public Random getRandom() {
        //Synchronizing RNG state isn't worthwhile
        //It's possible things could end up in loops like this.
        //Could probably think of something else to throw in.
        Random rand = FactorizationUtil.dirtyRandomCache();
        long seed = entityId + getCurrentPos().seed() + orientation.ordinal() << 2 + nextOrientation.ordinal();
        rand.setSeed(seed);
        return rand;
    }
    
    private boolean testDirection(ForgeDirection d) {
        if (d == ForgeDirection.UNKNOWN) {
            return false;
        }
        return validDirection(d);
    }
    
    static int similarity(FzOrientation base, FzOrientation novel) {
        int score = 0;
        //if pointing in plane, we want them to face the same direction
        
        return score;
    }
    
    int scorePotentialOrientation(FzOrientation o) {
        int points = 0;
        if (o == nextOrientation) {
            points += 200;
        } else if (o == orientation) {
            points += 100;
        } else {
            
        }
        
        return points;
    }

    boolean pickNextOrientation() {
        final ForgeDirection direction = orientation.facing, nextDirection = nextOrientation.facing;
        ForgeDirection opposite = direction.getOpposite();
        if (testDirection(nextDirection)) {
            swapOrientations();
            return true;
        }
        if (testDirection(direction)) {
            return true;
        }
        final ForgeDirection top = orientation.top, nextTop = nextOrientation.top;
        if (testDirection(top)) {
            nextOrientation = FzOrientation.fromDirection(top).pointTopTo(direction);
            swapOrientations();
        }
        if (testDirection(nextTop)) {
            nextOrientation = FzOrientation.fromDirection(nextTop).pointTopTo(nextDirection);
            swapOrientations();
        }
        need_description_packet = true;
        ArrayList<ForgeDirection> dirs = FactorizationUtil.dirtyDirectionCache();
        Collections.shuffle(dirs, getRandom());
        for (int i = 0; i < 6; i++) {
            ForgeDirection d = dirs.get(i);
            if (d == opposite || d == direction || d == nextDirection || d == top || d == nextTop) {
                continue;
            }
            if (validDirection(d)) {
                nextOrientation = FzOrientation.fromDirection(d);
                FzOrientation perfect = nextOrientation.pointTopTo(orientation.top);
                if (perfect != FzOrientation.UNKNOWN) {
                    nextOrientation = perfect;
                }
                swapOrientations();
                return true;
            }
        }
        orientation = nextOrientation = FzOrientation.UNKNOWN;
        return false;
    }

    void accelerate() {
        speed_b += 1;
        speed_b = (byte) Math.min(speed_b, max_speed_b);
    }

    void broadcast(short message_type, Object... msg) {
        Packet p = Core.network.entityPacket(this, message_type, msg);
        Core.network.broadcastPacket(worldObj, (int) posX, (int) posY, (int) posZ, p);
    }

    void describe() {
        Coord c = getCurrentPos();
        broadcast(MessageType.motor_speed, speed_b, (float) percent_complete, c.x, c.y, c.z /* not sure why... */, (byte) orientation.ordinal(), (byte) nextOrientation.ordinal());
    }

    double fraction(double v) {
        return v - ((int) v);
    }

    void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - percent_complete);
        accumulated_motion -= move;
        percent_complete += move;
        posX = pos.x + orientation.facing.offsetX * percent_complete;
        posY = pos.y + orientation.facing.offsetY * percent_complete;
        posZ = pos.z + orientation.facing.offsetZ * percent_complete;
        if (worldObj.isRemote) {
            prev_gear_rotation = gear_rotation;
            gear_rotation += move;
            
            prev_servo_reorient = servo_reorient;
            servo_reorient = Math.min(1, servo_reorient + move);
            /*if (direction != orientation.facing) {
                prevOrientation = orientation;
                FzOrientation default_orientation = FzOrientation.fromDirection(direction);
                FzOrientation next = default_orientation.pointTopTo(prevOrientation.top);
                if (next != FzOrientation.UNKNOWN) {
                    //A turn that keeps the gears in the same plane
                    orientation = next;
                } else {
                    //The gears are being tilted somehow
                    orientation = default_orientation.pointTopTo(prevOrientation.facing);
                    if (orientation == FzOrientation.UNKNOWN) {
                        orientation = next;
                    }
                }
            }*/
            if (servo_reorient >= 1) {
                servo_reorient = 0;
                prevOrientation = orientation;
            }
        }
    }
    
    Coord getCurrentPos() {
        return pos;
    }
    
    Coord getNextPos() {
        return pos.add(orientation.facing);
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
        return super.toString() + (worldObj.isRemote ? " client" : " server") + " " + getSpeed();
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
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    @Override
    public boolean handleMessage(short messageType, DataInputStream input)
            throws IOException {
//		System.out.println("MotorMessage: " + messageType); // NORELEASE
        switch (messageType) {
        case MessageType.motor_speed:
            speed_b = input.readByte();
            percent_complete = input.readFloat();
            Coord c = getCurrentPos();
            c.x = input.readInt();
            c.y = input.readInt();
            c.z = input.readInt();
            //$FALL-THROUGH$~~~~~!
        case MessageType.motor_direction:
            orientation = FzOrientation.getOrientation(input.readByte());
            nextOrientation = FzOrientation.getOrientation(input.readByte());
            return true;
        case MessageType.motor_position:
            double x = posX, y = posY, z = posZ;
            worldObj.spawnParticle("reddust", input.readFloat(), input.readFloat(), input.readFloat(), -1, 1, 0);
            //setPosition(input.readFloat(), input.readFloat(), input.readFloat());
            return true;
        }
        return false;
    }

    public double getSpeed() {
        double perc = speed_b/(max_speed_b);
        return maxSpeed*perc;
    }

    public void setSpeed(byte new_speed) {
        speed_b = new_speed;
    }
}
