package factorization.common.servo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
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

public class ServoMotor extends Entity implements IEntityAdditionalSpawnData, IEntityMessage {
    public static final int STACKS = 16;
    public static final int STACK_EQUIPMENT = 0, STACK_ARGUMENT = 1, STACK_IO = 2, STACK_CONFIG = 3, STACK_ERRNO = 4;
    private ServoStack[] stacks = new ServoStack[STACKS];
    {
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new ServoStack();
        }
    }
    public boolean sneaking = false;
    public int next_stack = 0;

    boolean dampenVelocity;

    Coord pos_prev, pos_next;
    float pos_progress;
    
    
    public FzOrientation prevOrientation = FzOrientation.UNKNOWN, orientation = FzOrientation.UNKNOWN;
    public ForgeDirection nextDirection = ForgeDirection.UNKNOWN;
    private byte speed_b;
    private static final double max_speed_b = 127;
    double accumulated_motion;
    
    boolean new_motor = true;
    
    //For client-side rendering
    double sprocket_rotation = 0, prev_sprocket_rotation = 0;
    double servo_reorient = 0, prev_servo_reorient = 0;

    static final double maxSpeed = 0.05 /* NORELEASE: 0.1 */, slowedSpeed = maxSpeed / 20, minSpeed = slowedSpeed / 10;

    private static class MessageType {
        static final short motor_description = 100, motor_direction = 101, motor_speed = 102;
    }

    public ServoMotor(World world) {
        super(world);
        setSize(1, 1);
        double d = 0.5;
        pos_prev = new Coord(world, 0, 0, 0);
        pos_next = pos_prev.copy();
    }
    
    /**
     * You <b>must</b> call this method instead of using worldObj.spawnEntityInWorld!
     */
    public void spawnServoMotor() {
        pos_prev = new Coord(this);
        pos_next = pos_prev.copy();
        pickNextOrientation();
        pickNextOrientation();
        interpolatePosition(0);
        worldObj.spawnEntityInWorld(this);
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
        prevOrientation = data.as(Share.PRIVATE, "prevOrient").putFzOrientation(prevOrientation);
        orientation = data.as(Share.VISIBLE, "Orient").putFzOrientation(orientation);
        nextDirection = data.as(Share.VISIBLE, "nextDir").putEnum(nextDirection);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        accumulated_motion = data.as(Share.PRIVATE, "accumulated_motion").putDouble(accumulated_motion);
        pos_next = data.as(Share.VISIBLE, "pos_next").put(pos_next);
        pos_prev = data.as(Share.VISIBLE, "pos_prev").put(pos_prev);
        pos_progress = data.as(Share.VISIBLE, "pos_progress").putFloat(pos_progress);
        new_motor = data.as(Share.PRIVATE, "new").putBoolean(new_motor);
        for (int i = 0; i < STACKS; i++) {
            String name = "stack_" + i;
            stacks[i] = data.as(Share.VISIBLE, name).put(stacks[i]);
        }
        next_stack = data.as(Share.VISIBLE, "next_stack").putInt(next_stack);
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
        if (validDirection(nextDirection)) {
            swapOrientations();
            return;
        }
        speed_b = 0;
        orientation = FzOrientation.UNKNOWN;
    }

    private boolean need_description_packet = false;
    private static Quaternion target_orientation = new Quaternion();
    @Override
    public void onEntityUpdate() {
        if (new_motor) {
            if (prevOrientation == FzOrientation.UNKNOWN && orientation != FzOrientation.UNKNOWN) {
                prevOrientation = orientation;
            }
        }
        if (ticksExisted == 1) {
            checkDirection();
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
        }
        interpolatePosition(pos_progress);
    }
    
    public void interpolatePosition(float interp) {
        setPosition(
                ip(pos_prev.x, pos_next.x, interp),
                ip(pos_prev.y, pos_next.y, interp),
                ip(pos_prev.z, pos_next.z, interp));
    }
    
    static double ip(int a, int b, float interp) {
        return a + (b - a)*interp;
    }

    void doLogic() {
        double x = posX, y = posY, z = posZ;
        worldObj.spawnParticle("reddust", x, y, z, 0, 0, 0);
        
        if (orientation == FzOrientation.UNKNOWN) {
            pickNextOrientation();
        }
        final double speed = getProperSpeed() ;
        boolean should_accelerate = speed < maxSpeed && orientation != FzOrientation.UNKNOWN;
        if (!worldObj.isRemote) {
            //Core.notify(null, getCurrentPos(), "X");
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
                should_accelerate = false;
            }
        }
        if (Core.cheat_servo_energy) {
            if (should_accelerate) {
                accelerate();
            }
        } else {
            long now = worldObj.getTotalWorldTime();
            if (now % 5 == 0) {
                int to_drain = should_accelerate ? 2 : 1;
                IChargeConductor conductor = getCurrentPos().getTE(IChargeConductor.class);
                if (conductor != null) {
                    if (conductor.getCharge().tryTake(to_drain) >= to_drain) {
                        accelerate();
                    }
                }
            }
        }
        
        if (speed <= 0 || orientation == FzOrientation.UNKNOWN) {
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (pos_progress >= 1) {
            pos_progress--;
            accumulated_motion = Math.min(pos_progress, speed);
            pos_prev = pos_next;
            onEnterNewBlock();
            pickNextOrientation();
        }
    }
    
    public Random getRandom() {
        //Synchronizing RNG state isn't worthwhile
        //It's possible things could end up in loops like this.
        //Could probably think of something else to throw in.
        Random rand = FactorizationUtil.dirtyRandomCache();
        long seed = entityId + getCurrentPos().seed() << 5 + orientation.ordinal() << 2 + nextDirection.ordinal();
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
    
    
    boolean pickNextOrientation() {
        boolean ret = pickNextOrientation_impl();
        pos_next = pos_prev.add(orientation.facing);
        return ret;
    }
    
    void swapOrientations() {
        ForgeDirection orig_direction = orientation.facing;
        FzOrientation start = FzOrientation.fromDirection(nextDirection);
        FzOrientation perfect = start.pointTopTo(orientation.top);
        if (perfect == FzOrientation.UNKNOWN) {
            perfect = start.pointTopTo(orig_direction);
        }
        if (perfect == FzOrientation.UNKNOWN) {
            perfect = start;
        }
        orientation = perfect;
        nextDirection = orig_direction;
    }

    boolean pickNextOrientation_impl() {
        final ForgeDirection direction = orientation.facing;
        if (testDirection(nextDirection)) {
            // We can go the way we were told to go next
            swapOrientations();
            return true;
        }
        if (testDirection(direction)) {
            // Our course is fine.
            return true;
        }
        // We've hit a T intersection, and don't know where to go next.
        final ForgeDirection top = orientation.top;
        if (testDirection(top)) {
            // We'll turn upwards
            nextDirection = top;
            swapOrientations();
            return true;
        }
        // We'll pick a random direction. Going backwards is our last resort.
        final ForgeDirection opposite = direction.getOpposite();
        ArrayList<ForgeDirection> dirs = FactorizationUtil.dirtyDirectionCache();
        Collections.shuffle(dirs, getRandom());
        for (int i = 0; i < 6; i++) {
            ForgeDirection d = dirs.get(i);
            if (d == opposite || d == direction || d == nextDirection || d == top || d == nextDirection) {
                continue;
            }
            if (validDirection(d)) {
                nextDirection = d;
                swapOrientations();
                need_description_packet = true; //NORELEASE: Check this out: Does the client simulate the same as the server? (Probably not.) Use a particle marker.
                return true;
            }
        }
        if (validDirection(opposite)) {
            orientation = FzOrientation.fromDirection(opposite).pointTopTo(top);
            if (orientation != FzOrientation.UNKNOWN) {
                return true;
            }
        }
        orientation = FzOrientation.UNKNOWN;
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

    void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - pos_progress);
        accumulated_motion -= move;
        pos_progress += move;
        if (worldObj.isRemote) {
            prev_sprocket_rotation = sprocket_rotation;
            sprocket_rotation += move;
            
            prev_servo_reorient = servo_reorient;
            servo_reorient = Math.min(1, servo_reorient + move);
            if (servo_reorient >= 1) {
                servo_reorient = 0;
                prevOrientation = orientation;
            }
        }
    }
    
    public Coord getCurrentPos() {
        return pos_prev;
    }
    
    public Coord getNextPos() {
        return pos_next;
    }

    boolean hasSignal(Signal signal) {
        return false;
    }

    void onEnterNewBlock() {
        TileEntityServoRail rail = getCurrentPos().getTE(TileEntityServoRail.class);
        if (rail == null /* :| */ || rail.decoration == null) {
            return;
        }
        rail.decoration.motorHit(this);
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
            if (player.capabilities.isCreativeMode && !player.isSneaking()) {
                return true;
            }
            ArrayList<Object> toDrop = new ArrayList(30);
            toDrop.add(new ItemStack(Core.registry.servo_motor_placer));
            for (ServoStack ss : stacks) {
                while (ss.getSize() > 0) {
                    Object o = ss.pop();
                    if (o instanceof ItemStack) {
                        toDrop.add(o);
                    }
                }
            }
            dropItemStacks(toDrop);
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + (worldObj.isRemote ? " client" : " server") + " " + getProperSpeed();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int three) {
        // The servers sends crappy packets. TODO: Sure would be nice if it didn't send those packets...
    }

    @Override
    public void setPosition(double x, double y, double z) {
        // super.setPosition(x, y, z); //Super does some stupid shit to the bounding box.
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        double neg_size = 0;
        double pos_size = 1;
        double height = 8F/16F;
        double dy = 0.5;
        this.boundingBox.setBounds(x - neg_size, dy + y - height, z - neg_size, x + pos_size, dy + y + height, z + pos_size);
    }

    @Override
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    void describe() {
        broadcast(MessageType.motor_description,
                speed_b,
                pos_progress,
                pos_prev.asDeltaCoord(),
                pos_next.asDeltaCoord(),
                (byte) orientation.ordinal(),
                (byte) nextDirection.ordinal());
    }
    
    @Override
    public boolean handleMessage(short messageType, DataInputStream input)
            throws IOException {
        switch (messageType) {
        case MessageType.motor_description:
            speed_b = input.readByte();
            pos_progress = input.readFloat();
            pos_prev.set(DeltaCoord.read(input));
            pos_next.set(DeltaCoord.read(input));
            interpolatePosition(pos_progress);
            //$FALL-THROUGH$~~~~~!
        case MessageType.motor_direction:
            orientation = FzOrientation.getOrientation(input.readByte());
            nextDirection = ForgeDirection.getOrientation(input.readByte());
            return true;
        }
        return false;
    }

    public double getProperSpeed() {
        double perc = speed_b/(max_speed_b);
        return maxSpeed*perc;
    }
    
    public void dropItemStacks(List<Object> toDrop) {
        for (Object o : toDrop) {
            if (o instanceof ItemStack) {
                FactorizationUtil.spawnItemStack(this, (ItemStack) o);
            }
        }
    }
    
    public ServoStack getServoStack(int stackId) {
        stackId = Math.max(0, stackId);
        stackId = Math.min(stackId, STACKS);
        return stacks[stackId];
    }
    
    public static boolean canClone(Object o) {
        return !(o instanceof ItemStack);
    }
    
    public void putError(Object error) {
        ServoStack ss = getServoStack(STACK_ERRNO);
        if (ss.getFreeSpace() <= 0) {
            ss.popEnd();
        }
        ss.push(error);
    }
}
