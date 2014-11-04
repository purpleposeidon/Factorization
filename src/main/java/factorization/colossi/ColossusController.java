package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.fzds.api.DeltaCapability;
import factorization.fzds.api.IDeltaChunk;

public class ColossusController extends Entity implements IBossDisplayData {
    static enum BodySide { LEFT, RIGHT, UNKNOWN_BODY_SIDE };
    static enum LimbType { BODY, ARM, LEG, UNKNOWN_LIMB_TYPE };
    LimbInfo[] limbs;
    IDeltaChunk body;
    final ColossusAI controller = new ColossusAI(this);
    boolean setup = false;
    int arm_size = 0, arm_length = 0;
    int leg_size = 0, leg_length = 0;
    
    int cracked_body_blocks = 0;
    
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
            li.entityId = li.ent.getUniqueID();
            if (li.type == LimbType.BODY) {
                body = li.ent;
            } else if (li.type == LimbType.LEG) {
                li.ent.permit(DeltaCapability.VIOLENT_COLLISIONS);
            }
        }
        for (LimbInfo li : limbs) {
            li.setOffsetFromBody(body);
        }
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
        this.arm_size = arm_size;
        this.arm_length = arm_length;
        this.leg_size = leg_size;
        this.leg_length = leg_length;
        this.cracked_body_blocks = crackedBlocks;
    }
    
    @Override
    public void onEntityUpdate() {
        if (worldObj.isRemote) {
            client_ticks++;
            return;
        }
        if (!setup) {
            setup = true;
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
        moveToTarget(body);
        tickLimbSwing();
        controller.tick();
        Quaternion bodyRotation = body.getRotation();
        for (LimbInfo limb : limbs) {
            if (limb.type == LimbType.BODY) continue;
            if (!limb.controlled) continue;
            Vec3 joint = limb.originalBodyOffset.addVector(0, limb.extension, 0);
            bodyRotation.applyRotation(joint);
            Quaternion rot = new Quaternion(bodyRotation);
            
            if (limb.sweep != 0) {
                Quaternion sweep = Quaternion.getRotationQuaternionRadians(limb.sweep, ForgeDirection.UP);
                rot.incrMultiply(sweep);
            }
            
            if (limb.swing != 0) {
                Quaternion swing = Quaternion.getRotationQuaternionRadians(limb.swing, ForgeDirection.SOUTH);
                rot.incrMultiply(swing);
            }
            
            if (limb.twist != 0) {
                Quaternion twist = Quaternion.getRotationQuaternionRadians(limb.twist, ForgeDirection.UP);
                rot.incrMultiply(twist);
            }
            
            if (limb.flap != 0) {
                Quaternion flap = Quaternion.getRotationQuaternionRadians(limb.flap, ForgeDirection.EAST);
                rot.incrMultiply(flap);
            }
            
            
            rot.incrNormalize();
            limb.ent.setRotation(rot);
            limb.ent.posX = body.posX + joint.xCoord;
            limb.ent.posY = body.posY + joint.yCoord;
            limb.ent.posZ = body.posZ + joint.zCoord;
        }
        setPosition(body.posX, body.posY, body.posZ);
    }
    
    void loadLimbs() {
        boolean needed = false;
        for (LimbInfo li : limbs) {
            if (li.ent == null) {
                needed = true;
                break;
            } else if (li.entityId == null) {
                li.entityId = li.ent.getUniqueID();
            }
        }
        if (!needed) return;
        for (Entity ent : (Iterable<Entity>) worldObj.loadedEntityList) {
            UUID test = ent.getUniqueID();
            for (LimbInfo li : limbs) {
                if (li.entityId == null || li.ent != null) continue;
                if (li.entityId.equals(test) && ent instanceof IDeltaChunk) {
                    li.ent = (IDeltaChunk) ent;
                    break;
                }
            }
        }
        int i = 0;
        for (LimbInfo li : limbs) {
            if (li.ent == null) {
                setup = false;
                break;
            }
            if (li.type == LimbType.BODY) {
                body = li.ent;
            }
            li.ent.setPartName(li.side + " " + li.type + "#" + (i++));
        }
    }

    @Override
    protected void entityInit() { }
    
    void putData(DataHelper data) throws IOException {
        int limb_count = data.as(Share.PRIVATE, "limbCount").putInt(limbs == null ? 0 : limbs.length);
        if (data.isReader()) {
            limbs = new LimbInfo[limb_count];
        }
        for (int i = 0; i < limbs.length; i++) {
            if (data.isReader()) {
                limbs[i] = new LimbInfo();
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

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        try {
            putData(new DataInNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    
    int turning = 0;
    
    void moveToTarget(IDeltaChunk body) {
        if (atTarget()) {
            for (LimbInfo limb : limbs) {
                limb.ent.motionX = 0;
                limb.ent.motionY = 0;
                limb.ent.motionZ = 0;
            }
            return;
        }
        Vec3 target = path_target.createVector();
        target.yCoord = posY;
        Vec3 me = Vec3.createVectorHelper(body.posX, body.posY, body.posZ);
        Vec3 delta = me.subtract(target);
        double speed = Math.min(1.0/20.0, delta.lengthVector());
        delta = delta.normalize();
        double angle = Math.atan2(delta.xCoord, delta.zCoord) - Math.PI / 2;
        //System.out.println(Math.toDegrees(angle));
        Quaternion target_rotation = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.UP);
        Quaternion current_rotation = body.getRotation();
        double rotation_distance = target_rotation.getAngleBetween(current_rotation);
        turning = 0;
        if (rotation_distance > Math.PI / 1800) {
            int size = leg_size + 1;
            double rotation_speed = (Math.PI * 2) / (360 * size * size * 2);
            double t = rotation_speed / rotation_distance;
            t = Math.min(0.5, t);
            target_rotation.incrLerp(current_rotation, 1 - t);
            body.setRotation(target_rotation);
            turning = angle > 0 ? 1 : -1;
        } else if (rotation_distance > Math.PI * 0.0001) {
            body.setRotation(target_rotation);
            body.setRotationalVelocity(new Quaternion());
        } else {
            if (!body.getRotationalVelocity().isZero()) {
                body.setRotationalVelocity(new Quaternion());
            }
            for (LimbInfo limb : limbs) {
                limb.ent.motionX = delta.xCoord * speed;
                limb.ent.motionY = delta.yCoord * speed;
                limb.ent.motionZ = delta.zCoord * speed;
            }
            walked += speed;
        }
    }
    
    
    double swingTicks = 0;
    double walked = 0;
    void tickLimbSwing() {
        if (turning != 0) {
            tickLimbTurn();
            return;
        } else {
            turnTicks = 0;
        }
        double slow = 1;
        double offsetTick = swingTicks * slow + Math.PI / 2;
        double loop = offsetTick % (2 * Math.PI);
        loop = Math.abs(loop - Math.PI) - Math.PI / 2;
        double swing = loop * 3.0 / 16.0;
        for (LimbInfo limb : limbs) {
            if (limb.type == LimbType.LEG || limb.type == LimbType.ARM) {
                limb.swing = swing;
                if (limb.limbSwingParity()) {
                    limb.swing *= -1;
                }
                if (limb.type == LimbType.ARM) {
                    boolean should_swing_arms = true;
                    if (should_swing_arms) {
                        limb.swing *= 0.025;
                        limb.extension = swing * 0.01;
                        limb.flap = Math.abs(swing) / arm_length * (limb.limbSwingParity() ? 1 : -1);
                    } else {
                        limb.swing = 0;
                        limb.extension = 0;
                        limb.flap = 0;
                    }
                }
            }
        }
        if (walked == 0) {
            double clip = 0.01;
            double stop_moving_delta = (Math.abs(swing) - clip) * 0.25;
            if (swing > clip) {
                swingTicks += stop_moving_delta;
            } else if (swing < -clip) {
                swingTicks -= stop_moving_delta;
            }
        } else {
            swingTicks += walked;
            walked = 0;
        }
    }
    
    int turnTicks = 0;
    void tickLimbTurn() {
        // lift right leg while twisting left leg
        // set right leg down
        // lift left leg while twiting right leg
        // set left leg down
        
        double lift_height = 1.5F/16F;
        double base_twist = Math.PI * 2 * 0.005;
        double phase_length = 18;
        double freq = Math.sin(turnTicks * phase_length / Math.PI);
        double deriv = Math.cos(turnTicks * phase_length / Math.PI);
        for (LimbInfo limb : limbs) {
            if (limb.type != LimbType.LEG) continue;
            if (limb.limbSwingParity() ^ freq > 0) {
                limb.extension = Math.abs(freq * lift_height);
                limb.swing = freq * 0.01;
                if (deriv < 0) {
                    limb.twist = freq * base_twist;
                }
            } else {
                limb.extension *= 0.90;
                if (limb.extension < 0.01) {
                    limb.extension = 0;
                }
                if (deriv > 0) {
                    limb.twist = freq * base_twist;
                }
            }
            limb.act_violently = true;
        }
        
        
        turnTicks++;
    }

    @Override
    public float getMaxHealth() {
        return cracked_body_blocks; // + leg_size * leg_size;
    }
    
    public int getCracks() {
        return dataWatcher.getWatchableObjectInt(destroyed_cracked_block_id);
    }

    @Override
    public float getHealth() {
        return cracked_body_blocks - getCracks();
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
        if (getCracks() % 3 == 0 && getCracks() > 0) {
            return new ChatComponentTranslation("colossus.name.true");
        }
        if ((client_ticks % 300 < 60 && client_ticks % 4 == 0) || client_ticks % 50 == 0) {
            current_name = worldObj.rand.nextInt(max_names);
        }
        return new ChatComponentTranslation("colossus.name." + current_name);
    }
}
