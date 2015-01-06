package factorization.colossi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import sun.util.locale.provider.AvailableLanguageTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
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
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.shared.EntityFz;
import factorization.shared.NORELEASE;

public class ColossusController extends EntityFz implements IBossDisplayData {
    static enum BodySide { LEFT, RIGHT, CENTER, UNKNOWN_BODY_SIDE };
    static enum LimbType { BODY, ARM, LEG, UNKNOWN_LIMB_TYPE };
    LimbInfo[] limbs;
    IDeltaChunk body;
    LimbInfo bodyLimbInfo;
    final StateMachineExecutor walk_controller = new StateMachineExecutor(this, "walk", WalkState.IDLE);
    final StateMachineExecutor ai_controller = new StateMachineExecutor(this, "tech", Technique.STATE_MACHINE_ENTRY);
    boolean setup = false;
    int arm_size = 0, arm_length = 0;
    int leg_size = 0, leg_length = 0;
    //int cracked_body_blocks = 0;
    private Coord home = null;
    private boolean been_hurt = false;
    
    EnumSet<Technique> known = EnumSet.<Technique>of(Technique.INITIAL_BOW);
    EnumSet<Technique> locked = EnumSet.<Technique>complementOf(known);
    
    private Coord path_target = null;
    int turningDirection = 0;
    boolean target_changed = false;
    double walked = 0;
    int last_step_direction = -100;
    
    transient int last_pos_hash = -1;
    double target_y = Double.NaN;
    
    int target_count = 0;
    transient Entity target_entity;

    private static final int _destroyed_cracked_block_id = 2;
    private static final int _unbroken_cracked_block_id = 3;
    
    public ColossusController(World world) {
        super(world);
        path_target = null;
        ignoreFrustumCheck = true;
        dataWatcher.addObject(_destroyed_cracked_block_id, (Integer) 0);
        dataWatcher.addObject(_unbroken_cracked_block_id, (Integer) 0);
    }
    
    public ColossusController(World world, LimbInfo[] limbInfo, int arm_size, int arm_length, int leg_size, int leg_length) {
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
        calcLimbParity();
        setTotalCracks(getNaturalCrackCount() + 1);
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
        walk_controller.serialize("walk", data);
        ai_controller.serialize("ai", data);
        
        
        leg_size = data.as(Share.VISIBLE, "legSize").putInt(leg_size);
        arm_size = data.as(Share.VISIBLE, "armSize").putInt(arm_size);
        arm_length = data.as(Share.VISIBLE, "armLength").putInt(arm_length);
        home = data.as(Share.PRIVATE, "home").put(home);
        if (data.as(Share.PRIVATE, "has_path_target").putBoolean(path_target != null)) {
            path_target = data.as(Share.PRIVATE, "path_target").put(path_target);
        }
        setTotalCracks(data.as(Share.VISIBLE, "cracks").putInt(getTotalCracks()));
        setDestroyedCracks(data.as(Share.VISIBLE, "broken").putInt(getDestroyedCracks()));
        turningDirection = data.as(Share.PRIVATE, "turningDirection").putInt(turningDirection);
        target_changed = data.as(Share.PRIVATE, "targetChanged").putBoolean(target_changed);
        walked = data.as(Share.PRIVATE, "walked").putDouble(walked);
        target_count = data.as(Share.PRIVATE, "target_count").putInt(target_count);
        target_y = data.as(Share.PRIVATE, "target_y").putDouble(target_y);
        last_step_direction = data.as(Share.PRIVATE, "last_step_direction").putInt(last_step_direction);
        been_hurt = data.as(Share.PRIVATE, "been_hurt").putBoolean(been_hurt);
        
        known = putEnumSet(data, "known", known, Technique.class);
        locked = putEnumSet(data, "locked", locked, Technique.class);
    }
    
    <E extends Enum<E>> EnumSet<E> putEnumSet(DataHelper data, String prefix, EnumSet<E> set, Class<E> elementType) throws IOException {
        if (!data.isNBT()) return set;
        if (data.isWriter()) {
            for (E e : set) {
                data.asSameShare(prefix + e.name()).putBoolean(true);
            }
            return set;
        } else {
            EnumSet<E> ret = EnumSet.noneOf(elementType);
            for (E e : elementType.getEnumConstants()) {
                if (data.asSameShare(prefix + e.name()).putBoolean(false)) {
                    ret.add(e);
                }
            }
            return ret;
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
            }
            if (!setup) return;
        }
        if (body == null || body.isDead) {
            setDead();
            return;
        }
        walk_controller.tick();
        updateBlockClimb();
        ai_controller.tick();
        /*if (atTarget()) {
            int d = 16;
            int dx = worldObj.rand.nextInt(d) - d/2;
            int dz = worldObj.rand.nextInt(d) - d/2;
            path_target = home.copy().add(dx * 2, 0, dz * 2);
        }*/
        setPosition(body.posX, body.posY, body.posZ);
    }

    public boolean atTarget() {
        if (path_target == null) return true;
        double d = 0.1;
        double dx = path_target.x - body.posX;
        double dz = path_target.z - body.posZ;
        
        if (dx < d && dx > -d && dz < d && dz > -d) {
            return true;
        }
        return false;
    }
    
    public void setTarget(Coord at) {
        if (at == null && path_target == null) return;
        if (at != null && path_target != null && at.equals(path_target)) return;
        path_target = at;
        target_changed = true;
    }
    
    public Coord getTarget() {
        return path_target;
    }
    
    boolean targetChanged() {
        boolean ret = target_changed;
        target_changed = false;
        return ret;
    }
    
    public void goHome() {
        setTarget(home.copy());
    }
    
    public Coord getHome() {
        return home;
    }
    
    public void resetLimbs(int time, Interpolation interp) {
        for (LimbInfo li : limbs) {
            if (li.type == LimbType.BODY) continue;
            li.lastTurnDirection = 0;
            IDeltaChunk idc = li.idc.getEntity();
            if (idc == null) continue;
            idc.setRotationalVelocity(new Quaternion());
            li.reset(time, interp);
        }
    }
    
    public int getNaturalCrackCount() {
        int ls = leg_size + 1;
        int count = ls * ls;
        return count;
    }

    @Override
    public float getMaxHealth() {
        return getTotalCracks() * 3;
    }
    
    public int getDestroyedCracks() {
        return dataWatcher.getWatchableObjectInt(_destroyed_cracked_block_id);
    }
    
    public void setDestroyedCracks(int newCount) {
        dataWatcher.updateObject(_destroyed_cracked_block_id, newCount);
    }
    
    public int getTotalCracks() {
        return dataWatcher.getWatchableObjectInt(_unbroken_cracked_block_id);
    }
    
    public void setTotalCracks(int newCount) {
        dataWatcher.updateObject(_unbroken_cracked_block_id, newCount);
    }

    @Override
    public float getHealth() {
        float wiggle = 1 - 0.1F * (current_name / (float) max_names);
        return (getTotalCracks() - getDestroyedCracks()) * wiggle;
    }
    
    public void crackBroken() {
        setDestroyedCracks(getDestroyedCracks() + 1);
        been_hurt = true;
    }
    
    static int max_names = -1;
    transient int current_name = 0;
    transient int client_ticks = 0;
    
    @Override
    public IChatComponent func_145748_c_() {
        if (getDestroyedCracks() == 0) {
            return new ChatComponentTranslation("colossus.name.null");
        }
        if (max_names == -1) {
            try {
                max_names = Integer.parseInt(Core.translate("colossus.name.count"));
            } catch (NumberFormatException e) {
                max_names = 1;
            } 
        }
        if (getHealth() <= 0) {
            return new ChatComponentTranslation("colossus.name.true");
        }
        if ((client_ticks % 300 < 60 && client_ticks % 4 == 0) || client_ticks % 50 == 0) {
            current_name = worldObj.rand.nextInt(max_names);
        }
        return new ChatComponentTranslation("colossus.name." + current_name);
    }
    
    void updateBlockClimb() {
        if (walk_controller.state != WalkState.FORWARD) return;
        if (ticksExisted <= 5) return; // Make sure limbs are in position
        if (getHealth() <= 0) {
            return;
        }
        int currentPosHash = new Coord(this).hashCode() + 100 * (int) posY;
        if (currentPosHash != last_pos_hash) {
            last_pos_hash = currentPosHash;
            int dy = new HeightCalculator().calc();
            double new_target = (int) (posY + dy);
            if (new_target != (int) target_y) {
                //NORELEASE.println("target_y: " + new_target);
                target_y = new_target;
            }
            if (last_step_direction != dy) {
                last_step_direction = dy;
                if (dy == 0 && target_y == posY) {
                    body.motionY = 0;
                    return;
                }
            }
        }
        //if (target_y == posY) return;
        double close_enough = 1.0 / 64.0;
        if (Math.abs(posY - target_y) <= close_enough) {
            target_y = posY;
            body.motionY = 0;
            return;
        }
        double maxV = 1.0/16.0;
        int sign = posY > target_y ? -1 : +1;
        if (posY == target_y) sign = 0;
        double delta = Math.abs(posY - target_y);
        if (delta < 1.0/16.0) sign = 0;
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
                //if (down.getAngleBetween(idc.getRotation()) > max_leg_angle) continue;
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
            if (total == 0) {
                return 0; // Or return -1?
            }
            double solid_at = 0;
            double solid_below = 0;
            //NORELEASE.println();
            for (Coord at : found) {
                if (at.isSolid()) {
                    //new Notice(at, "#").withStyle(Style.FORCE, Style.DRAWFAR, Style.LONG).sendToAll();
                    solid_at++;
                }
                at.y--;
                if (at.isSolid()) {
                    //new Notice(at, ".").withStyle(Style.FORCE, Style.DRAWFAR, Style.LONG).sendToAll();
                    solid_below++;
                }
                at.y++;
            }
            double at = solid_at / total;
            double below = solid_below / total;
            //NORELEASE.println("total: " + total);
            //NORELEASE.println("at: " + at);
            //NORELEASE.println("below: " + below);
            if (at > 0.4) {
                // We're standing in something pretty fat; we *must* move up!
                return 1;
            }
            if (below < 0.2) {
                // We're unsupported, so we *must* move down!
                //NORELEASE.println("*** GET DOWN ***");
                return -1;
            } else {
                return 0;
            }
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
            NORELEASE.fixme("hashset");
            if (found.contains(real)) return;
            found.add(real);
            any = true;
        }
    }
    
    boolean checkHurt(boolean reset) {
        if (been_hurt && reset) {
            been_hurt = false;
            wakeUpMore();
            return true;
        }
        return been_hurt;
    }
    
    void wakeUpMore() {
        openEyes();
        if (!locked.isEmpty()) {
            ArrayList<Technique> avail = new ArrayList(locked);
            ArrayList<Technique> toRemove = new ArrayList();
            Collections.shuffle(avail);
            Technique learned = avail.remove(0);
            {
                toRemove.add(Technique.INITIAL_BOW);
                // Merely hard-wire the forgetting
                switch (learned) {
                default: break;
                case HAMMAR: toRemove.add(Technique.BOW); break;
                }
            }
            locked.remove(learned);
            known.add(learned);
            known.removeAll(toRemove);
        }
    }
    
    void openEyes() {
        NORELEASE.fixme("Eyelids.");
    }
    
    boolean canTargetPlayer(Entity player) {
        double max_dist = 24 * 24 * leg_size;
        double max_home_dist = 32 * 32;
        if (getDistanceSqToEntity(player) > max_dist) return false;
        if (getHome().distanceSq(new Coord(player)) > max_home_dist) return false;
        return true;
    }
    
    
}
