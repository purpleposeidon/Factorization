package factorization.servo.iterator;

import com.google.common.collect.Lists;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzColor;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.api.energy.ContextEntity;
import factorization.api.energy.IWorker;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.FlatCoord;
import factorization.servo.rail.FlatServoRail;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class MotionHandler {
    enum TwistCategory {
        STUCK,
        // When making a turn the long way:
        // CCBB
        // CCBB
        // []AA
        // []AA
        // The iterator is in the 'STRAIGHT' state as it enters block A.
        // Then it sees it must enter the rails of C.
        // It enters the 'EXTERIOR_START' state, which moves it from A to B.
        // Then it enters 'TURN', which turns it so that it's lined up with C's rails.
        // Then it enters STRAIGHT.
        EXTERIOR_START,
        // When making a turn the short way:
        // [][][]
        // [][][]
        // []B2CC
        // []1 CC
        // []AA
        // []AA
        // The iterator is in the 'STRAIGHT' state as it enters block A.
        // It continues to be in the 'STRAIGHT' state as it rides the rail B1.
        // When it sees it must make an interior turn, it enters the 'TURN' state, which aligns it with
        // block C's rails.
        // It enters the 'STRAIGHT' state.
        TURN,
        // TURN will nominally have STRAIGHT as its next action.
        STRAIGHT,
    }

    static class MotionAction implements IDataSerializable {
        MotionAction(World world) {
            src = new FlatCoord(new Coord(world, 0, 0, 0), EnumFacing.UP);
            dst = new FlatCoord(new Coord(world, 0, 0, 0), EnumFacing.UP);
            srcFzo = FzOrientation.FACE_UP_POINT_EAST;
            dstFzo = FzOrientation.FACE_UP_POINT_EAST;
            category = TwistCategory.STUCK;
        }

        MotionAction chain() {
            MotionAction ret = new MotionAction(src.at.w);
            ret.src = this.dst.copy();
            ret.srcFzo = this.dstFzo;
            return ret;
        }

        FlatCoord src, dst;
        FzOrientation srcFzo, dstFzo;
        TwistCategory category;
        float progress = 0;

        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            src = data.asSameShare(prefix + ":src").putIDS(src);
            dst = data.asSameShare(prefix + ":dst").putIDS(dst);
            srcFzo = data.asSameShare(prefix + ":srcO").putFzOrientation(srcFzo);
            dstFzo = data.asSameShare(prefix + ":dstO").putFzOrientation(dstFzo);
            category = data.asSameShare(prefix + ":cat").putEnum(category);
            progress = data.asSameShare(prefix + ":progress").putFloat(progress);
            return this;
        }
    }

    public final AbstractServoMachine motor;

    MotionAction motionAction;
    byte speed_b;
    byte target_speed_index = 2;
    static final byte max_speed_b = 127;
    double accumulated_motion;
    public FzColor color = FzColor.NO_COLOR;
    boolean stopped = false;

    private static final byte normal_speed_byte = (byte) (max_speed_b/4);
    private static final byte[] target_speeds_b = {normal_speed_byte/3, normal_speed_byte/2, normal_speed_byte, normal_speed_byte*2, normal_speed_byte*4};
    private static final double normal_speed_double = 0.0875;
    private static final double max_speed_double = normal_speed_double*4;
    
    public MotionHandler(AbstractServoMachine motor) {
        this.motor = motor;
        motionAction = new MotionAction(motor.worldObj);
    }
    
    protected void putData(DataHelper data) throws IOException {
        motionAction = data.as(Share.VISIBLE, "motionAction").putIDS(motionAction);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        setTargetSpeed(data.as(Share.VISIBLE, "speedt").putByte(target_speed_index));
        accumulated_motion = data.as(Share.VISIBLE, "accumulated_motion").putDouble(accumulated_motion);
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
        stopped = data.as(Share.VISIBLE, "stopped").putBoolean(stopped);
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
        motionAction.src = new FlatCoord(new Coord(motor), motionAction.src.side);
        motionAction.dst = motionAction.src.copy();
        chooseNextAction();
        interpolatePosition(motionAction);
    }
    

    public void interpolatePosition(MotionAction action) {
        float interp = action.progress;
        Coord pos_prev = action.src.at;
        Coord pos_next = action.dst.at;
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
        
        boolean should_accelerate = speed_b < target_speed_b;
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

    public void penalizeSpeed() {
        if (speed_b > 4) {
            speed_b--;
        }
    }

    int score(FlatCoord at) {
        FlatServoRail fsr = at.get(FlatServoRail.class);
        if (fsr == null) return Integer.MIN_VALUE; // Bad state.
        int score = fsr.getComponent().getPriority() * 1000;
        Coord mpos = motor.getCurrentPos();
        FzOrientation fzo = motor.getOrientation();
        if (at.side == fzo.top) {
            score += 100;
        } else if (at.side == fzo.top.getOpposite()) {
            // checkerboard case:
            // .##|...
            // .##|...
            // ...|##.
            // ...|##.
            score += 50;
        }
        DeltaCoord dc = at.at.difference(mpos);
        EnumFacing proposedMotion = dc.getDirection();
        if (proposedMotion == fzo.facing) {
            score += 30;
        } else if (proposedMotion == fzo.facing.getOpposite()) {
            score -= 30;
        }
        return score;
    }

    static int support_flag(FlatCoord at) {
        return (at.at.isSolid() ? 1 : 0)
                | (at.flip().at.isSolid() ? 2 : 0);
    }

    List<MotionAction> availableActions(MotionAction startAction) {
        List<MotionAction> ret = Lists.newArrayList();
        int base_support = support_flag(startAction.src);
        AbstractFlatWire.iterateConnectable(startAction.src, new AbstractFlatWire.IConnectionIter() {
            @Override
            public void apply(FlatCoord at, EnumFacing hand, int wrap) {
                FlatServoRail ff = at.get(FlatServoRail.class);
                if (ff == null) return;
                if (ff.getSpecies() != FlatServoRail.SPECIES) return;
                if (MotionHandler.this.color.conflictsWith(ff.color)) return;
                /*
                Don't allow flipping
                ||||W
                ||||W
                ||||W
                ||||W
                   W||||
                   W||||
                   W||||
                   W||||

                Don't allow a weird case with non-solid blocks
                ||||----||||
                ||||----||||
                ||||----||||
                ||||----||||
                   W|WWW
                   W|
                   W|
                   W|

                Don't enter a wire that's covered on both sides.
                Don't enter a wire that's floating unsupported
                */
                int at_support = support_flag(at);
                if (at_support == 0 || at_support == 3) return;
                int support = at_support | base_support;
                if (support == 0 || support == 3) return;
                NORELEASE.fixme("Probably an error here involving the side being normalized?");

                MotionAction action = startAction.chain();
                action.dst = at;
                EnumFacing new_top = at.at.isSolid() ? at.side.getOpposite() : at.side;
                action.dstFzo = action.srcFzo.pointTopTo(new_top);
            }
        });
        return ret;
    }

    void sort(List<MotionAction> actions) {
        actions.sort(new Comparator<MotionAction>() {
            @Override
            public int compare(MotionAction a, MotionAction b) {
                int sa = score(a.dst);
                int sb = score(b.dst);
                if (sa > sb) return +1;
                if (sa < sb) return -1;
                return 0;
            }
        });
    }

    void chooseNextAction() {
        List<MotionAction> potentials = availableActions(this.motionAction);
        if (potentials.isEmpty()) {
            this.motionAction = this.motionAction.chain();
            this.motionAction.category = TwistCategory.STUCK;
            return;
        }
        sort(potentials);
        this.motionAction = potentials.get(0);
    }

    void accelerate() {
        speed_b += 1;
        speed_b = (byte) Math.min(speed_b, max_speed_b);
    }
    
    protected void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - motionAction.progress);
        accumulated_motion -= move;
        motionAction.progress += move;
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
        doMotionLogic();
        interpolatePosition(motionAction);
    }

    protected void tryUnstop() {
        if (motor.waitingForPower) {
            IWorker.requestPower(new ContextEntity(motor));
        }
        if (motor.waitingForPower) {
            return;
        }
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
        if (!motor.worldObj.isRemote) {
            updateSpeed();
        }
        final double speed = getProperSpeed();
        if (speed <= 0) {
            motor.updateSocket();
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (motionAction.progress >= 1) {
            float extra_progress = motionAction.progress - 1;
            accumulated_motion = Math.min(extra_progress, speed);
            Chunk oldChunk = motionAction.src.at.getChunk(), newChunk = motionAction.dst.at.getChunk();
            motor.updateSocket();
            motor.onEnterNewBlock();
            chooseNextAction();
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
