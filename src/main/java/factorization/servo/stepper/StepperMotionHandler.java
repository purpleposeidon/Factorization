package factorization.servo.stepper;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.servo.MotionHandler;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class StepperMotionHandler extends MotionHandler {
    final StepperEngine engine;
    int stepperReorient = 0, reorientDistance = 0;
    EnumObstructionKind obstruction = EnumObstructionKind.NONE;
    static int TICKS_PER_HALF_TURN = 40;

    enum EnumObstructionKind {
        NONE, LINEAR, ROTATIONAL;
    }

    public StepperMotionHandler(StepperEngine stepperEngine) {
        super(stepperEngine);
        engine = stepperEngine;
    }

    IDeltaChunk get() {
        EntityGrabController grabber = engine.grabber.getEntity();
        if (grabber == null) return null;
        return grabber.idcRef.getEntity();
    }

    void turnThrough(Quaternion start, Quaternion end) {
        double angle = start.getAngleBetween(end);
        double ticksNeeded = (angle / Math.PI) * TICKS_PER_HALF_TURN;
        int t = (int) ticksNeeded;
        if (t < 10) t = 10;
        reorientDistance = stepperReorient = t;
    }

    @Override
    public void changeOrientation(EnumFacing dir) {
        FzOrientation orig = orientation;
        super.changeOrientation(dir);
        if (orientation == orig) return;
        if (orig != null /* was not just spawned */) setStopped(true);
        if (!engine.grabbed()) {
            turnThrough(Quaternion.fromOrientation(orig), Quaternion.fromOrientation(orientation));
            return;
        }
        IDeltaChunk idc = get();
        if (idc == null) return;
        faceTarget(idc);
    }

    private void faceTarget(IDeltaChunk idc) {
        Quaternion startRot = idc.getRotation();
        Quaternion endRot = Quaternion.fromOrientation(orientation);
        turnThrough(startRot, endRot);
        idc.orderTargetRotation(endRot, reorientDistance, Interpolation.SMOOTH);
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        super.putData(data);
        stepperReorient = data.as(Share.VISIBLE, "stepperReorient").putInt(stepperReorient);
        obstruction = data.as(Share.VISIBLE, "obstruction").putEnum(obstruction);
        stepperReorient = data.as(Share.VISIBLE, "stepperReorient").putInt(stepperReorient);
        reorientDistance = data.as(Share.VISIBLE, "stepperReorientDistance").putInt(reorientDistance);
    }

    boolean waitForObstructionClear(EntityGrabController grabber, IDeltaChunk idc) {
        if (obstruction == EnumObstructionKind.NONE) return false;
        if (engine.worldObj.getTotalWorldTime() % 40 != 0) return true;
        idc.findAnyCollidingBox();
        if (grabber.hitReal == null) {
            if (obstruction == EnumObstructionKind.LINEAR) {
                // Move again!
                matchVelocity(idc);
            } else if (obstruction == EnumObstructionKind.ROTATIONAL) {
                // Rotate again!
                faceTarget(idc);
            }
            obstruction = EnumObstructionKind.NONE;
            return false;
        }
        // NORELEASE.fixme("Emit sparks");
        grabber.hitReal = grabber.hitShadow = null;
        return true;
    }

    private void matchVelocity(IDeltaChunk idc) {
        idc.setVelocity(getVelocity());
    }

    @Override
    protected void updateServoMotion() {
        if (!engine.grabbed() || engine.worldObj.isRemote) {
            super.updateServoMotion();
            if (stepperReorient > 0) {
                stepperReorient--;
                setStopped(false);
            }
            return;
        }
        EntityGrabController grabber = engine.grabber.getEntity();
        if (grabber == null) {
            return;
        }
        IDeltaChunk idc = grabber.idcRef.getEntity();
        if (idc == null) return;
        if (!idc.worldObj.isRemote && waitForObstructionClear(grabber, idc)) return;
        if (stepperReorient > 0) {
            stepperReorient--;
            setStopped(false);
        }
        removeIdcPositionError();
        super.updateServoMotion();
    }

    void removeIdcPositionError() {

    }
}
