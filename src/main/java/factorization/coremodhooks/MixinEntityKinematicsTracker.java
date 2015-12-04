package factorization.coremodhooks;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public abstract class MixinEntityKinematicsTracker extends Entity implements IKinematicTracker {

    public MixinEntityKinematicsTracker(World w) {
        super(w);
    }

    private long kinematics_last_change; // Should be initialized to somehting negative, but our mixins don't support that
    double kinematics_motX, kinematics_motY, kinematics_motZ;
    
    @Override
    public double getKinematics_motX() {
        return kinematics_motX;
    }

    @Override
    public double getKinematics_motY() {
        return kinematics_motY;
    }

    @Override
    public double getKinematics_motZ() {
        return kinematics_motZ;
    }

    @Override
    public double getKinematics_yaw() {
        return kinematics_yaw;
    }

    double kinematics_yaw;
    
    @Override
    public void reset(long now) {
        if (now == kinematics_last_change) return;
        kinematics_last_change = now;
        kinematics_motX = this.motionX;
        kinematics_motY = this.motionY;
        kinematics_motZ = this.motionZ;
        kinematics_yaw = this.rotationYaw;
    }
    
}
