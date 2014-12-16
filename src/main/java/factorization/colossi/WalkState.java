package factorization.colossi;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Quaternion;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.FzUtil;

public enum WalkState implements IStateMachine<WalkState> {
    IDLE {
        @Override
        public WalkState tick(ColossusController controller, int age) {
            return controller.atTarget() ? this : TURN;
        }
    },
    TURN {
        @Override
        public void onEnterState(ColossusController controller, WalkState state) {
            checkRotation(controller);
            for (LimbInfo limb : controller.limbs) {
                IDeltaChunk idc = limb.idc.getEntity();
                if (idc == null) continue;
                if (limb.type == LimbType.ARM) {
                    limb.reset(20, Interpolation.SMOOTH);
                }
            }
        }
        
        @Override
        public WalkState tick(ColossusController controller, int age) {
            if (!controller.body.hasOrderedRotation()) return checkRotation(controller);
            if (controller.atTarget() || controller.targetChanged()) return IDLE;
            
            // System no longer supports joint displacement, but if it did:
            // double lift_height = 1.5F/16F;
            double base_twist = Math.PI * 2 * 0.03;
            double phase_length = 36; //18;
            double arms_angle = Math.PI * 0.45;
            for (LimbInfo limb : controller.limbs) {
                // Twist the legs while the body turns
                IDeltaChunk idc = limb.idc.getEntity();
                if (idc == null) continue;
                if (limb.isTurning()) continue;
                if (limb.type == LimbType.ARM) {
                    if ((limb.side == BodySide.LEFT) ^ controller.turningDirection == 1) {
                        continue;
                    }
                    double arm_angle = arms_angle * (limb.side == BodySide.LEFT ? +1 : -1);
                    Quaternion ar = Quaternion.getRotationQuaternionRadians(arm_angle, ForgeDirection.EAST);
                    idc.multiplyParentRotations(ar);
                    limb.setTargetRotation(ar, 20, Interpolation.SMOOTH);
                    continue;
                }
                if (limb.type != LimbType.LEG) continue;
                double nextRotation = base_twist;
                double nextRotationTime = phase_length;
                
                // This is how it *ought* to work, but there's some weird corner case that I can't figure out. -_-
                // So the turning direction is tracked by a variable instead, which is less robust.
                // double dr = body.getRotation().dotProduct(down) - currentRotation.dotProduct(down);
                // nextRotation *= -Math.signum(dr);1
                
                limb.lastTurnDirection *= -1;
                Interpolation interp = Interpolation.SMOOTH;
                if (limb.lastTurnDirection == 0) {
                    limb.lastTurnDirection = (byte) (controller.turningDirection * (limb.limbSwingParity() ? 1 : -1));
                }
                if (limb.lastTurnDirection == 1 ^ limb.limbSwingParity()) {
                    interp = Interpolation.CUBIC;
                }
                nextRotation *= limb.lastTurnDirection;
                
                Quaternion nr = Quaternion.getRotationQuaternionRadians(nextRotation, ForgeDirection.DOWN);
                if (limb.lastTurnDirection == controller.turningDirection) {
                    // Lift a leg up a tiny bit
                    nr.incrMultiply(Quaternion.getRotationQuaternionRadians(Math.toRadians(2), ForgeDirection.SOUTH));
                }
                idc.multiplyParentRotations(nr);
                limb.setTargetRotation(nr, (int) nextRotationTime, interp);
            }
            
            return this;
        }
        
        WalkState checkRotation(ColossusController controller) {
            if (controller.atTarget()) return IDLE;
            IDeltaChunk body = controller.body;
            Vec3 target = controller.getTarget().createVector();
            target.yCoord = controller.posY;
            Vec3 me = FzUtil.fromEntPos(body);
            Vec3 delta = me.subtract(target);
            double angle = Math.atan2(delta.xCoord, delta.zCoord) - Math.PI / 2;
            Quaternion target_rotation = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.UP);
            Quaternion current_rotation = body.getRotation();
            double rotation_distance = target_rotation.getAngleBetween(current_rotation);
            int size = controller.leg_size + 1;
            double rotation_speed = (Math.PI * 2) / (360 * size * size * 2);
            double rotation_time = rotation_distance / rotation_speed;
            if (rotation_time >= 1) {
                controller.bodyLimbInfo.setTargetRotation(target_rotation, (int) rotation_time, Interpolation.SMOOTH);
                // Now bodyLimbInfo.isTurning() is set.
                controller.turningDirection = angle > 0 ? 1 : -1;
                for (LimbInfo li : controller.limbs) {
                    li.lastTurnDirection = 0;
                }
            } else if (rotation_time > 0.001) {
                body.setRotation(target_rotation);
                body.setRotationalVelocity(new Quaternion());
            } else {
                return FORWARD;
            }
            return TURN;
        }
        
        @Override
        public void onExitState(ColossusController controller, WalkState nextState) {
            controller.turningDirection = 0;
            controller.resetLimbs(20, Interpolation.SMOOTH);
        }
    },
    FORWARD {
        @Override
        public void onEnterState(ColossusController controller, WalkState state) {
            if (controller.atTarget()) return;
            IDeltaChunk body = controller.body;
            Vec3 target = controller.getTarget().createVector();
            target.yCoord = controller.posY;
            Vec3 me = FzUtil.fromEntPos(body);
            Vec3 delta = me.subtract(target);
            double walk_speed = Math.min(1.0/20.0 /* TODO: Inversely proportional to size? */, delta.lengthVector());
            delta = delta.normalize();
            body.motionX = delta.xCoord * walk_speed;
            body.motionZ = delta.zCoord * walk_speed;
            controller.walked += walk_speed;
            controller.resetLimbs(20, Interpolation.SMOOTH);
        }
        
        private final double max_leg_swing_degrees = 22.5;
        private final double max_leg_swing_radians = Math.toRadians(max_leg_swing_degrees);
        private final Quaternion arm_hang = Quaternion.getRotationQuaternionRadians(Math.toRadians(5), ForgeDirection.EAST);
        
        
        @Override
        public WalkState tick(ColossusController controller, int age) {
            if (controller.atTarget() || controller.targetChanged()) return IDLE;
            
            
            final double legCircumference = 2 * Math.PI * controller.leg_size;
            final double swingTime = legCircumference * 360 / (2 * max_leg_swing_degrees);
            
            
            for (LimbInfo limb : controller.limbs) {
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
                if (controller.walked == 0) {
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
            
            return this;
        }
        
        @Override
        public void onExitState(ColossusController controller, WalkState nextState) {
            controller.resetLimbs(20, Interpolation.SMOOTH);
            IDeltaChunk body = controller.body;
            body.motionX = body.motionZ = 0; // Not setting motionY so that I can easily implement jumping
        }
    }
    ;

    @Override
    public abstract WalkState tick(ColossusController controller, int age);

    @Override
    public void onEnterState(ColossusController controller, WalkState state) { }

    @Override
    public void onExitState(ColossusController controller, WalkState nextState) { }

}
