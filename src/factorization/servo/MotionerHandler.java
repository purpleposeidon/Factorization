package factorization.servo;

import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;

public class MotionerHandler {
    public Coord pos_prev;
    public Coord pos_next;
    public float pos_progress;
    public FzOrientation prevOrientation;
    public FzOrientation orientation;
    public FzOrientation pendingClientOrientation;
    public ForgeDirection nextDirection;
    public ForgeDirection lastDirection;
    public byte speed_b;
    public byte target_speed_index;
    public double accumulated_motion;
    public double sprocket_rotation;
    public double prev_sprocket_rotation;
    public double servo_reorient;
    public double prev_servo_reorient;

}