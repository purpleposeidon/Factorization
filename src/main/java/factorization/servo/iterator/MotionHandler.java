package factorization.servo.iterator;

import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.servo.rail.TileEntityServoRail;
import factorization.shared.Core;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;

import java.io.IOException;
import java.util.ArrayList;

public class MotionHandler {
    public final AbstractServoMachine motor;
    
    Coord pos_prev, pos_next;
    public float pos_progress;
    public FzOrientation prevOrientation = null, orientation = null;
    public EnumFacing nextDirection = null, lastDirection = null;
    byte speed_b;
    byte target_speed_index = 2;
    static final byte max_speed_b = 127;
    double accumulated_motion;
    boolean stopped = false;
    public FzColor color = FzColor.NO_COLOR;
    
    //For client-side rendering
    public double sprocket_rotation = 0, prev_sprocket_rotation = 0;
    public double servo_reorient = 0, prev_servo_reorient = 0;

    private static final byte normal_speed_byte = (byte) (max_speed_b/4);
    private static final byte[] target_speeds_b = {normal_speed_byte/3, normal_speed_byte/2, normal_speed_byte, normal_speed_byte*2, normal_speed_byte*4};
    private static final double normal_speed_double = 0.0875;
    private static final double max_speed_double = normal_speed_double*4;
    
    public MotionHandler(AbstractServoMachine motor) {
        this.motor = motor;
        pos_prev = new Coord(motor.worldObj, 0, 0, 0);
        pos_next = pos_prev.copy();
    }
    
    protected void putData(DataHelper data) throws IOException {
        orientation = data.as(Share.VISIBLE, "Orient").putFzOrientation(orientation);
        nextDirection = data.as(Share.VISIBLE, "nextDir").putEnum(nextDirection);
        lastDirection = data.as(Share.VISIBLE, "lastDir").putEnum(lastDirection);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        setTargetSpeed(data.as(Share.VISIBLE, "speedt").putByte(target_speed_index));
        accumulated_motion = data.as(Share.VISIBLE, "accumulated_motion").putDouble(accumulated_motion);
        stopped = data.as(Share.VISIBLE, "stop").putBoolean(stopped);
        pos_next = data.as(Share.VISIBLE, "pos_next").putIDS(pos_next);
        pos_prev = data.as(Share.VISIBLE, "pos_prev").putIDS(pos_prev);
        pos_progress = data.as(Share.VISIBLE, "pos_progress").putFloat(pos_progress);
        if (target_speed_index < 0) {
            target_speed_index = 0;
        } else if (target_speed_index >= target_speeds_b.length) {
            target_speed_index = (byte) (target_speeds_b.length - 1);
        }
        if (data.isNBT() && data.isReader()) {
            if (!data.hasLegacy("color")) {
                color = FzColor.NO_COLOR;
            }
        }
        color = data.as(Share.VISIBLE, "color").putEnum(color);
        if (color == null) {
            color = FzColor.NO_COLOR;
        }
    }
    
    public void setTargetSpeed(byte newSpeed) {
        if (newSpeed < 0) {
            newSpeed = 0;
        } else if (newSpeed >= target_speeds_b.length) {
            newSpeed = (byte) (target_speeds_b.length - 1);
        }
        target_speed_index = newSpeed;
    }
    
    void beforeSpawn() {
        pos_prev = new Coord(motor);
        pos_next = pos_prev.copy();
        pickNextOrientation();
        pickNextOrientation();
        interpolatePosition(0);
        prevOrientation = orientation;
    }
    

    public void interpolatePosition(float interp) {
        motor.setPosition(
                ip(pos_prev.x, pos_next.x, interp),
                ip(pos_prev.y, pos_next.y, interp),
                ip(pos_prev.z, pos_next.z, interp));
    }
    
    static double ip(int a, int b, float interp) {
        return a + (b - a)*interp;
    }

    void updateSpeed() {
        byte target_speed_b = target_speeds_b[target_speed_index];
        
        boolean should_accelerate = speed_b < target_speed_b && orientation != null;
        if (speed_b > target_speed_b) {
            speed_b = (byte)Math.max(target_speed_b, speed_b*3/4 - 1);
            return;
        }
        long now = motor.worldObj.getTotalWorldTime();
        if (should_accelerate && now % 3 == 0) {
            if (Core.cheat_servo_energy || motor.extractAccelerationEnergy()) {
                accelerate();
            }
        }
    }

    public Vec3 getVelocity() {
        double speed = getProperSpeed();
        Vec3 direction = pos_next.difference(pos_prev).toVector().normalize();
        return SpaceUtil.scale(direction, speed);
    }
    
    public void penalizeSpeed() {
        if (speed_b > 4) {
            speed_b--;
        }
    }
    
    boolean validPosition(Coord c, boolean desperate) {
        TileEntityServoRail sr = c.getTE(TileEntityServoRail.class);
        if (sr == null) {
            return false;
        }
        return (sr.priority >= 0 || desperate) && !color.conflictsWith(sr.color);
    }

    boolean validDirection(EnumFacing dir, boolean desperate) {
        Coord at = motor.getCurrentPos();
        at.adjust(dir);
        try {
            return validPosition(at, desperate);
        } finally {
            at.adjust(dir.getOpposite());
        }
    }

    public boolean testDirection(EnumFacing d, boolean desperate) {
        if (d == null) {
            return false;
        }
        return validDirection(d, desperate);
    }
    
    
    boolean pickNextOrientation() {
        boolean ret = pickNextOrientation_impl();
        pos_next = pos_prev.add(orientation.facing);
        return ret;
    }
    
    public void changeOrientation(EnumFacing dir) {
        EnumFacing orig_direction = orientation.facing;
        EnumFacing orig_top = orientation.top;
        FzOrientation start = FzOrientation.fromDirection(dir);
        FzOrientation perfect = start.pointTopTo(orig_top);
        if (perfect == null) {
            if (dir == orig_top) {
                //convex turn
                perfect = start.pointTopTo(orig_direction.getOpposite());
            } else if (dir == orig_top.getOpposite()) {
                //concave turn
                perfect = start.pointTopTo(orig_direction);
            }
            if (perfect == null) {
                perfect = start; //Might be impossible?
            }
        }
        orientation = perfect;
        lastDirection = orig_direction;
        if (orientation.facing == nextDirection) {
            nextDirection = null;
        }
    }

    boolean pickNextOrientation_impl() {
        ArrayList<EnumFacing> dirs = SpaceUtil.getRandomDirections(motor.worldObj.rand);
        int available_nonbackwards_directions = 0;
        Coord look = pos_next.copy();
        int all_count = 0;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < dirs.size(); i++) {
            EnumFacing fd = dirs.get(i);
            look.set(pos_next);
            look.adjust(fd);
            TileEntityServoRail sr = look.getTE(TileEntityServoRail.class);
            if (sr == null) {
                continue;
            }
            if (color.conflictsWith(sr.color)) continue;
            all_count++;
            if (fd == orientation.facing.getOpposite()) {
                continue;
            }
            if (sr.priority > 0) {
                changeOrientation(fd);
                return true;
            }
            if (sr.priority == 0) {
                available_nonbackwards_directions++;
            }
        }
        
        if (all_count == 0) {
            //Sadness
            speed_b = 0;
            return false;
        }
        
        final boolean desperate = available_nonbackwards_directions < 1;
        final EnumFacing direction = orientation.facing;
        final EnumFacing opposite = direction.getOpposite();
        
        if (nextDirection != opposite && testDirection(nextDirection, desperate)) {
            // We can go the way we were told to go next
            changeOrientation(nextDirection);
            return true;
        }
        if (testDirection(direction, desperate)) {
            // Our course is fine.
            return true;
        }
        if (lastDirection != opposite && testDirection(lastDirection, desperate)) {
            // Try the direction we were going before (this makes us go in zig-zags)
            changeOrientation(lastDirection);
            return true;
        }
        final EnumFacing top = orientation.top;
        if (testDirection(top, desperate) /* top being opposite won't be an issue because of Geometry */ ) {
            // We'll turn upwards.
            changeOrientation(top);
            return true;
        }
        
        // We'll pick a random direction; we're re-using the list from before, should be fine.
        // Going backwards is our last resort.
        for (int i = 0; i < 6; i++) {
            EnumFacing d = dirs.get(i);
            if (d == nextDirection || d == direction || d == opposite || d == lastDirection || d == top) {
                continue;
            }
            if (validDirection(d, desperate)) {
                changeOrientation(d);
                return true;
            }
        }
        if (validDirection(opposite, true)) {
            changeOrientation(opposite);
            return true;
        }
        orientation = null;
        return false;
    }

    void accelerate() {
        speed_b += 1;
        speed_b = (byte) Math.min(speed_b, max_speed_b);
    }
    
    protected void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - pos_progress);
        accumulated_motion -= move;
        pos_progress += move;
        if (motor.worldObj.isRemote) {
            sprocket_rotation += move;
            
            if (orientation != prevOrientation) {
                servo_reorient += move;
                if (servo_reorient >= 0.95 /* Floating point inaccuracy!? */) {
                    servo_reorient = 0;
                    prev_servo_reorient = servo_reorient;
                    prevOrientation = orientation;
                }
            } else {
                servo_reorient = 0;
            }
        }
    }
    
    public double getProperSpeed() {
        double perc = speed_b/(double)(max_speed_b);
        return max_speed_double*perc;
    }
    
    public void setStopped(boolean newState) {
        if (stopped == newState) return;
        stopped = newState;
    }
    
    protected void updateServoMotion() {
        prev_sprocket_rotation = sprocket_rotation;
        prev_servo_reorient = servo_reorient;
        doMotionLogic();
        interpolatePosition(pos_progress);
    }

    protected void tryUnstop() {
        if (stopped && motor.getCurrentPos().isWeaklyPowered()) {
            setStopped(false);
        }
    }
    
    void doMotionLogic() {
        doLogic();
        if (stopped) {
            speed_b = 0;
        }
    }
    
    private void doLogic() {
        if (stopped) {
            tryUnstop();
            if (stopped) {
                motor.updateSocket();
                return;
            }
        }
        if (orientation == null) {
            pickNextOrientation();
        }
        if (!motor.worldObj.isRemote) {
            updateSpeed();
        }
        final double speed = getProperSpeed();
        if (speed <= 0 || orientation == null) {
            motor.updateSocket();
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (pos_progress >= 1) {
            pos_progress -= 1F;
            accumulated_motion = Math.min(pos_progress, speed);
            Chunk oldChunk = pos_prev.getChunk(), newChunk = pos_next.getChunk();
            pos_prev = pos_next;
            motor.updateSocket();
            motor.onEnterNewBlock();
            pickNextOrientation();
            if (oldChunk != newChunk) {
                oldChunk.setChunkModified();
                newChunk.setChunkModified();
            }
        } else {
            motor.updateSocket();
        }
    }

    void onEnterNewBlock() {
        final int m = target_speed_index + 1;
        if (!motor.extractCharge(m*2)) {
            speed_b = (byte) Math.max(0, speed_b*3/4 - 1);
        }
    }
}
