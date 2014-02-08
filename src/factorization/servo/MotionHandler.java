package factorization.servo;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class MotionHandler {
    public final ServoMotor motor;
    
    Coord pos_prev, pos_next;
    float pos_progress;
    FzOrientation prevOrientation = FzOrientation.UNKNOWN, orientation = FzOrientation.UNKNOWN;
    FzOrientation pendingClientOrientation = FzOrientation.UNKNOWN;
    ForgeDirection nextDirection = ForgeDirection.UNKNOWN, lastDirection = ForgeDirection.UNKNOWN;
    byte speed_b;
    byte target_speed_index = 2;
    static final byte max_speed_b = 127;
    double accumulated_motion;
    boolean stopped = false;
    
    //For client-side rendering
    double sprocket_rotation = 0, prev_sprocket_rotation = 0;
    double servo_reorient = 0, prev_servo_reorient = 0;

    private static final byte normal_speed_byte = (byte) (max_speed_b/4);
    private static final byte[] target_speeds_b = {normal_speed_byte/3, normal_speed_byte/2, normal_speed_byte, normal_speed_byte*2, normal_speed_byte*4};
    private static final double normal_speed_double = 0.0875;
    private static final double max_speed_double = normal_speed_double*4;
    
    public MotionHandler(ServoMotor motor) {
        this.motor = motor;
        pos_prev = new Coord(motor.worldObj, 0, 0, 0);
        pos_next = pos_prev.copy();
    }
    
    void putData(DataHelper data) throws IOException {
        orientation = data.as(Share.VISIBLE, "Orient").putFzOrientation(orientation);
        nextDirection = data.as(Share.VISIBLE, "nextDir").putEnum(nextDirection);
        lastDirection = data.as(Share.VISIBLE, "lastDir").putEnum(lastDirection);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        target_speed_index = data.as(Share.VISIBLE, "speedt").putByte(target_speed_index);
        accumulated_motion = data.as(Share.VISIBLE, "accumulated_motion").putDouble(accumulated_motion);
        stopped = data.as(Share.VISIBLE, "stop").putBoolean(stopped);
        pos_next = data.as(Share.VISIBLE, "pos_next").put(pos_next);
        pos_prev = data.as(Share.VISIBLE, "pos_prev").put(pos_prev);
        pos_progress = data.as(Share.VISIBLE, "pos_progress").putFloat(pos_progress);
        
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
        
        boolean should_accelerate = speed_b < target_speed_b && orientation != FzOrientation.UNKNOWN;
        if (speed_b > target_speed_b) {
            speed_b = (byte)Math.max(target_speed_b, speed_b*3/4 - 1);
            return;
        }
        if (Core.cheat_servo_energy) {
            if (should_accelerate) {
                accelerate();
            }
        }
        long now = motor.worldObj.getTotalWorldTime();
        int m = 1 + target_speed_index;
        if (should_accelerate && now % 3 == 0) {
            if (motor.extractCharge(8)) {
                accelerate();
            }
        }
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
        return sr.priority >= 0 || desperate;
    }

    boolean validDirection(ForgeDirection dir, boolean desperate) {
        Coord at = motor.getCurrentPos();
        at.adjust(dir);
        try {
            return validPosition(at, desperate);
        } finally {
            at.adjust(dir.getOpposite());
        }
    }

    public boolean testDirection(ForgeDirection d, boolean desperate) {
        if (d == ForgeDirection.UNKNOWN) {
            return false;
        }
        return validDirection(d, desperate);
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
    
    public void changeOrientation(ForgeDirection dir) {
        ForgeDirection orig_direction = orientation.facing;
        ForgeDirection orig_top = orientation.top;
        FzOrientation start = FzOrientation.fromDirection(dir);
        FzOrientation perfect = start.pointTopTo(orig_top);
        if (perfect == FzOrientation.UNKNOWN) {
            if (dir == orig_top) {
                //convex turn
                perfect = start.pointTopTo(orig_direction.getOpposite());
            } else if (dir == orig_top.getOpposite()) {
                //concave turn
                perfect = start.pointTopTo(orig_direction);
            }
            if (perfect == FzOrientation.UNKNOWN) {
                perfect = start; //Might be impossible?
            }
        }
        orientation = perfect;
        lastDirection = orig_direction;
        if (orientation.facing == nextDirection) {
            nextDirection = ForgeDirection.UNKNOWN;
        }
    }

    boolean pickNextOrientation_impl() {
        ArrayList<ForgeDirection> dirs = FzUtil.getRandomDirections(motor.worldObj.rand);
        int available_nonbackwards_directions = 0;
        Coord look = pos_next.copy();
        int all_count = 0;
        for (int i = 0; i < dirs.size(); i++) {
            ForgeDirection fd = dirs.get(i);
            look.set(pos_next);
            look.adjust(fd);
            TileEntityServoRail sr = look.getTE(TileEntityServoRail.class);
            if (sr == null) {
                continue;
            }
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
        final ForgeDirection direction = orientation.facing;
        final ForgeDirection opposite = direction.getOpposite();
        
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
        final ForgeDirection top = orientation.top;
        if (testDirection(top, desperate) /* top being opposite won't be an issue because of Geometry */ ) {
            // We'll turn upwards.
            changeOrientation(top);
            return true;
        }
        
        // We'll pick a random direction; we're re-using the list from before, should be fine.
        // Going backwards is our last resort.
        for (int i = 0; i < 6; i++) {
            ForgeDirection d = dirs.get(i);
            if (d == nextDirection || d == direction || d == opposite || d == lastDirection || d == top) {
                continue;
            }
            if (validDirection(d, desperate)) {
                changeOrientation(d);
                return true;
            }
        }
        if (validDirection(opposite, true)) {
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
    
    void moveMotor() {
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
                if (servo_reorient >= 1) {
                    servo_reorient -= 1;
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
    
    void updateServoMotion() {
        prev_sprocket_rotation = sprocket_rotation;
        prev_servo_reorient = servo_reorient;
        doMotionLogic();
        interpolatePosition(pos_progress);
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
            motor.updateSocket();
            return;
        }
        if (orientation == FzOrientation.UNKNOWN) {
            pickNextOrientation();
        }
        if (!motor.worldObj.isRemote) {
            updateSpeed();
        }
        final double speed = getProperSpeed();
        if (speed <= 0 || orientation == FzOrientation.UNKNOWN) {
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
