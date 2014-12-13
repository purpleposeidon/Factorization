package factorization.colossi;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.Core;
import factorization.shared.EntityFz;
import factorization.shared.FzUtil;
import factorization.shared.NORELEASE;

public class ColossusController extends EntityFz implements IBossDisplayData {
    static enum BodySide { LEFT, RIGHT, CENTER, UNKNOWN_BODY_SIDE };
    static enum LimbType { BODY, ARM, LEG, UNKNOWN_LIMB_TYPE };
    LimbInfo[] limbs;
    IDeltaChunk body;
    LimbInfo bodyLimbInfo;
    final ColossusAI controller = new ColossusAI(this);
    boolean setup = false;
    int arm_size = 0, arm_length = 0;
    int leg_size = 0, leg_length = 0;
    
    int cracked_body_blocks = 0;
    
    int last_pos_hash = -1;
    double target_y = Double.NaN;
    
    Coord home = null;
    Coord path_target = null;
    
    transient Entity target_entity;
    transient int target_count = 0;
    transient long shake_seed = 0;
    
    transient int client_ticks = 0;

    private static final int destroyed_cracked_block_id = 2;
    
    public ColossusController(World world) {
        super(world);
        path_target = new Coord(this);
        ignoreFrustumCheck = true;
        dataWatcher.addObject(destroyed_cracked_block_id, (Integer) 0);
    }
    
    public ColossusController(World world, LimbInfo[] limbInfo, int arm_size, int arm_length, int leg_size, int leg_length, int crackedBlocks) {
        this(world);
        this.limbs = limbInfo;
        for (LimbInfo li : limbs) {
            IDeltaChunk idc = li.idc.getEntity();
            idc.setController(this);
            if (li.type == LimbType.BODY) {
                body = idc;
                bodyLimbInfo = li;
            } else if (li.type == LimbType.LEG) {
                idc.permit(DeltaCapability.VIOLENT_COLLISIONS);
            }
        }
        this.arm_size = arm_size;
        this.arm_length = arm_length;
        this.leg_size = leg_size;
        this.leg_length = leg_length;
        this.cracked_body_blocks = crackedBlocks;
        calcLimbParity();
    }
    
    protected void calcLimbParity() {
        byte left_leg_parity = 0, left_arm_parity = 0, right_leg_parity = 0, right_arm_parity = 0;
        discover_limb_parities: for (LimbInfo li : limbs) {
            byte use_parity = 0;
            switch (li.type) {
            default: continue discover_limb_parities;
            case ARM:
                if (li.side == BodySide.RIGHT) {
                    use_parity = right_arm_parity++;
                } else if (li.side == BodySide.LEFT) {
                    use_parity = left_arm_parity++;
                } else {
                    continue;
                }
                break;
            case LEG:
                if (li.side == BodySide.RIGHT) {
                    use_parity = right_leg_parity++;
                } else if (li.side == BodySide.LEFT) {
                    use_parity = left_leg_parity++;
                } else {
                    continue;
                }
                break;
            }
            li.parity = use_parity;
        }
    }
    
    @Override
    public void onEntityUpdate() {
        if (worldObj.isRemote) {
            client_ticks++;
            return;
        }
        if (!setup) {
            loadLimbs();
            if (home == null) {
                home = new Coord(this);
                path_target = home.copy();
            }
            if (!setup) return;
        }
        if (body == null || body.isDead) {
            setDead();
            return;
        }
        moveToTarget();
        //updateBlockClimb();
        tickLimbSwing();
        //controller.tick();
        if (atTarget()) {
            int d = 16;
            int dx = worldObj.rand.nextInt(d) - d/2;
            int dz = worldObj.rand.nextInt(d) - d/2;
            path_target = home.copy().add(dx, 0, dz);
        }
        setPosition(body.posX, body.posY, body.posZ);
    }
    
    void loadLimbs() {
        int i = 0;
        for (LimbInfo li : limbs) {
            IDeltaChunk idc = li.idc.getEntity();
            if (idc == null) {
                // It's possible to get in a state where all the DSEs are dead...
                return;
            }
            idc.setController(this);
            if (li.type == LimbType.BODY) {
                body = idc;
                bodyLimbInfo = li;
            }
            idc.setPartName(li.side + " " + li.type + "#" + (i++));
        }
        setup = true;
    }

    @Override
    protected void entityInit() { }
    
    public void putData(DataHelper data) throws IOException {
        int limb_count = data.as(Share.PRIVATE, "limbCount").putInt(limbs == null ? 0 : limbs.length);
        if (data.isReader()) {
            limbs = new LimbInfo[limb_count];
        }
        for (int i = 0; i < limbs.length; i++) {
            if (data.isReader()) {
                limbs[i] = new LimbInfo(worldObj);
            }
            limbs[i].putData(data, i);
        }
        controller.serialize("controller", data);
        leg_size = data.as(Share.VISIBLE, "legSize").putInt(leg_size);
        arm_size = data.as(Share.VISIBLE, "armSize").putInt(arm_size);
        arm_length = data.as(Share.VISIBLE, "armLength").putInt(arm_length);
        home = data.as(Share.PRIVATE, "home").put(home);
        path_target = data.as(Share.PRIVATE, "path_target").put(path_target);
        cracked_body_blocks = data.as(Share.VISIBLE, "cracks").putInt(cracked_body_blocks);
        int broken_body_blocks = dataWatcher.getWatchableObjectInt(destroyed_cracked_block_id);
        broken_body_blocks = data.as(Share.VISIBLE, "broken").putInt(broken_body_blocks);
        dataWatcher.updateObject(destroyed_cracked_block_id, broken_body_blocks);
    }

    boolean atTarget() {
        if (path_target == null) return true;
        double d = 0.1;
        double dx = path_target.x - body.posX;
        double dz = path_target.z - body.posZ;
        
        if (dx < d && dx > -d && dz < d && dz > -d) {
            return true;
        }
        return false;
    }
    
    int turningDirection = 0;
    
    void moveToTarget() {
        if (atTarget()) {
            body.motionX = body.motionZ = 0; // Not setting motionY so that I can easily implement jumping
            return;
        }
        if (bodyLimbInfo.isTurning()) return;
        Vec3 target = path_target.createVector();
        target.yCoord = posY;
        Vec3 me = FzUtil.fromEntPos(body);
        Vec3 delta = me.subtract(target);
        double angle = Math.atan2(delta.xCoord, delta.zCoord) - Math.PI / 2;
        Quaternion target_rotation = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.UP);
        Quaternion current_rotation = body.getRotation();
        double rotation_distance = target_rotation.getAngleBetween(current_rotation);
        if (rotation_distance > Math.PI / 1800) {
            int size = leg_size + 1;
            double rotation_speed = (Math.PI * 2) / (360 * size * size * 2);
            double rotation_time = rotation_distance / rotation_speed;
            bodyLimbInfo.setTargetRotation(target_rotation, (int) rotation_time, Interpolation.SMOOTH);
            // Now bodyLimbInfo.isTurning() is set.
            turningDirection = angle > 0 ? 1 : -1;
            for (LimbInfo li : limbs) {
                li.lastTurnDirection = 0;
            }
        } else if (rotation_distance > Math.PI * 0.0001) {
            body.setRotation(target_rotation);
            body.setRotationalVelocity(new Quaternion());
        } else {
            if (turningDirection != 0) {
                for (LimbInfo li : limbs) {
                    li.lastTurnDirection = 0;
                }
                turningDirection = 0;
            }
            if (!body.getRotationalVelocity().isZero()) {
                body.setRotationalVelocity(new Quaternion());
            }
            double walk_speed = Math.min(1.0/20.0 /* TODO: Inversely proportional to size? */, delta.lengthVector());
            delta = delta.normalize();
            body.motionX = delta.xCoord * walk_speed;
            body.motionZ = delta.zCoord * walk_speed;
            walked += walk_speed;
        }
    }
    
    
    double max_leg_swing_degrees = 22.5;
    double max_leg_swing_radians = Math.toRadians(max_leg_swing_degrees);
    double walked = 0;
    private static final Quaternion arm_hang = Quaternion.getRotationQuaternionRadians(Math.toRadians(5), ForgeDirection.EAST);
    void tickLimbSwing() {
        if (turningDirection != 0) {
            tickLegTurn();
            return;
        }
        
        final double legCircumference = 2 * Math.PI * leg_size;
        final double swingTime = legCircumference * 360 / (2 * max_leg_swing_degrees);
        
        
        for (LimbInfo limb : limbs) {
            if (limb.type != LimbType.LEG && limb.type != LimbType.ARM) continue;
            if (limb.isTurning()) continue;
            IDeltaChunk idc = limb.idc.getEntity();
            if (idc == null) continue;
            double nextRotationTime = swingTime;
            int p = limb.limbSwingParity() ? 1 : -1;
            if (limb.lastTurnDirection == 0) {
                // We were standing straight; begin with half a swing
                nextRotationTime /= 2;
                limb.lastTurnDirection = (byte) p;
            } else {
                // Swing the other direction
                limb.lastTurnDirection *= -1;
                p = limb.lastTurnDirection;
            }
            if (walked == 0) {
                p = 0;
            }
            Quaternion nextRotation = Quaternion.getRotationQuaternionRadians(max_leg_swing_radians * p, ForgeDirection.NORTH);
            idc.multiplyParentRotations(nextRotation);
            if (limb.type == LimbType.ARM) {
                if (limb.limbSwingParity()) {
                    nextRotation.incrMultiply(arm_hang);
                } else {
                    arm_hang.incrToOtherMultiply(nextRotation);
                }
            }
            limb.setTargetRotation(nextRotation, (int) nextRotationTime, Interpolation.SMOOTH);
        }
    }
    
    void tickLegTurn() {
        if (path_target == null) {
            turningDirection = 0;
            return;
        }
        if (turningDirection == 0) {
            // There's a path_target, but we aren't turning. So we must be moving forward.
            return;
        }
        
        // System no longer supports joint displacement, but if it did:
        // double lift_height = 1.5F/16F;
        double base_twist = Math.PI * 2 * 0.03;
        double phase_length = 36; //18;
        double arms_angle = Math.PI * 0.45;
        for (LimbInfo limb : limbs) {
            IDeltaChunk idc = limb.idc.getEntity();
            if (idc == null) continue;
            if (limb.type == LimbType.ARM) {
                double arm_angle = arms_angle * (limb.side == BodySide.LEFT ? +1 : -1);
                Quaternion ar = Quaternion.getRotationQuaternionRadians(arm_angle, ForgeDirection.EAST);
                idc.multiplyParentRotations(ar);
                limb.setTargetRotation(ar, 20, Interpolation.SMOOTH);
                continue;
            }
            if (limb.type != LimbType.LEG) continue;
            if (limb.isTurning()) continue;
            double nextRotation = base_twist;
            double nextRotationTime = phase_length;
            
            limb.lastTurnDirection *= -1;
            Interpolation interp = Interpolation.SMOOTH;
            if (limb.lastTurnDirection == 0) {
                limb.lastTurnDirection = (byte) (turningDirection * (limb.limbSwingParity() ? 1 : -1));
            }
            if (limb.lastTurnDirection == 1 ^ limb.limbSwingParity()) {
                interp = Interpolation.CUBIC;
            }
            nextRotation *= limb.lastTurnDirection;
            
            /* This is how it *ought* to work, but there's some weird corner case that I can't figure out. -_-
            double dr = body.getRotation().dotProduct(down) - currentRotation.dotProduct(down);
            nextRotation *= -Math.signum(dr);
            */
            
            Quaternion nr = Quaternion.getRotationQuaternionRadians(nextRotation, ForgeDirection.DOWN);
            idc.multiplyParentRotations(nr);
            limb.setTargetRotation(nr, (int) nextRotationTime, interp);
        }
    }

    @Override
    public float getMaxHealth() {
        return cracked_body_blocks * 3;
    }
    
    public int getCracks() {
        return dataWatcher.getWatchableObjectInt(destroyed_cracked_block_id);
    }

    @Override
    public float getHealth() {
        float wiggle = 1 - 0.1F * (current_name / (float) max_names);
        return (cracked_body_blocks - getCracks()) * wiggle;
    }
    
    public void crackBroken() {
        int cracks = getCracks();
        cracks++;
        dataWatcher.updateObject(destroyed_cracked_block_id, cracks);
    }
    
    int current_name = 0;
    static final int max_names = 20;
    
    @Override
    public IChatComponent func_145748_c_() {
        if (getHealth() <= 0) {
            return new ChatComponentTranslation("colossus.name.true");
        }
        if ((client_ticks % 300 < 60 && client_ticks % 4 == 0) || client_ticks % 50 == 0) {
            current_name = worldObj.rand.nextInt(max_names);
        }
        return new ChatComponentTranslation("colossus.name." + current_name);
    }
    
    void updateBlockClimb() {
        if (ticksExisted <= 5) return; // Make sure limbs are in position
        if (getHealth() <= 0) {
            return;
        }
        int currentPosHash = new Coord(this).hashCode();
        if (currentPosHash != last_pos_hash) {
            last_pos_hash = currentPosHash;
            int dy = new HeightCalculator().calc();
            target_y = posY + dy;
        }
        if (target_y == posY) return;
        double close_enough = 1.0 / 64.0;
        if (Math.abs(posY - target_y) <= close_enough) {
            target_y = posY;
            body.motionY = 0;
            return;
        }
        double maxV = 1.0/5.0;
        int sign = posY > target_y ? -1 : +1;
        double delta = Math.abs(posY - target_y);
        body.motionY = sign * Math.min(maxV, delta);
    }
    
    private static final Quaternion down = Quaternion.getRotationQuaternionRadians(0, ForgeDirection.DOWN);
    private static final double max_leg_angle = Math.toRadians(25);
    class HeightCalculator implements ICoordFunction {
        
        int lowest = Integer.MAX_VALUE;
        ArrayList<Coord> found = new ArrayList();
        IDeltaChunk idc;
        boolean any;
        int calc() {
            found.clear();
            for (LimbInfo li : limbs) {
                if (li.type != LimbType.LEG) continue;
                idc = li.idc.getEntity();
                if (down.getAngleBetween(idc.getRotation()) > max_leg_angle) continue;
                Coord min = idc.getCorner().copy();
                Coord max = idc.getFarCorner().copy();
                Coord.sort(min, max);
                final int maxY = max.y;
                any = false;
                for (int y = min.y; y < maxY; y++) {
                    max.y = min.y = y;
                    Coord.iterateCube(min, max, this);
                    if (any) break;
                }
            }
            int total = found.size();
            if (total == 0) return 0;
            double solid_at = 0;
            double solid_below = 0;
            NORELEASE.println();
            for (Coord at : found) {
                if (at.isSolid()) {
                    solid_at++;
                    NORELEASE.println("solid_at: " + at.getBlock());
                } else {
                    at.y--;
                    if (at.isSolid()) {
                        NORELEASE.println("solid_below: " + at.getBlock());
                        solid_below++;
                    } else {
                        NORELEASE.println("no support: " + at.getBlock());
                    }
                    at.y++;
                }
            }
            double at = solid_at / total;
            double below = solid_below / total;
            NORELEASE.println("total: " + total);
            NORELEASE.println("at: " + at);
            NORELEASE.println("below: " + below);
            NORELEASE.println("lowest: " + lowest);
            if (at >= 0.5) {
                return +1;
            }
            if (below <= 0.4 && at <= 0.15) {
                return -1;
            }
            return 0;
        }
        
        @Override
        public void handle(Coord here) {
            if (here.getBlock() != Core.registry.colossal_block) return;
            Coord real = here.copy();
            idc.shadow2real(real);
            if (real.y > lowest) return;
            if (real.y < lowest) {
                lowest = real.y;
                found.clear();
            }
            found.add(real);
            any = true;
        }
    }
    
    
    
    
    
}
