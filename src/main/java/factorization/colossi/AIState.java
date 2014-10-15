package factorization.colossi;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.api.DeltaCapability;

public enum AIState {
    IDLE {
        @Override
        AIState tick(ColossusController controller, int age) {
            Coord at = new Coord(controller);
            if (at.x == controller.home.x && at.z == controller.home.z) {
                controller.path_target = controller.home.add(24, 0, 0);
                return WALK_EAST_SAFELY;
            }
            for (EntityPlayer player : (Iterable<EntityPlayer>) controller.worldObj.playerEntities) {
                if (!canTargetPlayer(controller, player)) continue;
                if (player.posY <= at.y && player.onGround) {
                    controller.target_entity = player;
                    return CHASE_PLAYER_ON_GROUND;
                } else {
                    if (controller.worldObj.rand.nextBoolean()) {
                        return FLAIL_ARMS;
                    } else {
                        return FLAIL_ARMS;
                    }
                }
            }
            controller.path_target = controller.home.copy();
            return RUN_BACK;
        }
    },
    WALK_EAST_SAFELY {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (age % 40 == 0) {
                if (controller.atTarget() && controller.worldObj.rand.nextBoolean()) {
                    return IDLE;
                }
            }
            return this;
        }
    },
    RUN_BACK {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (controller.atTarget()) return IDLE;
            return this;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState state) {
            controller.path_target = controller.home.copy();
        }
    },
    FLAIL_ARMS {
        @Override
        AIState tick(ColossusController controller, int age) {
            int cycle_length = controller.arm_length * 40;
            // TODO: There's many wrong usages of arm_length; it needs some kind of radial conversion.
            // We the instantaneous linear velocity of something rotating radially with a radius of arm_length to have some specific target velocity for all arm_lengths.
            if (cycle_length == 0) return IDLE;
            double maxVelocity = Math.PI / cycle_length;
            double d = -maxVelocity * Math.sin(Math.PI * 2 * age / cycle_length);
            Quaternion flapxis = Quaternion.getRotationQuaternionRadians(d, ForgeDirection.EAST);
            Quaternion bod = controller.body.getRotation();
            bod.toVector().normalize() Grrr...
            bod.incrToOtherMultiply(flapxis);
            bod.incrConjugate();
            flapxis.incrMultiply(bod);
            bod.incrConjugate();
            
            Quaternion leftRotation = flapxis;
            Quaternion rightRotation = new Quaternion(leftRotation);
            rightRotation.incrConjugate();
            
            for (LimbInfo li : controller.limbs) {
                if (li.type != LimbType.ARM) continue;
                int parity = li.side == BodySide.LEFT ? -1 : 1;
                li.ent.setRotationalVelocity(parity < 0 ? leftRotation : rightRotation);
            }
            if (age >= cycle_length * 3) {
                for (LimbInfo li : controller.limbs) {
                    if (li.type != LimbType.ARM) continue;
                    li.ent.setRotationalVelocity(new Quaternion());
                }
                return IDLE;
            }
            return FLAIL_ARMS;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState state) {
            for (LimbInfo li : controller.limbs){
                if (li.type == LimbType.ARM){
                    li.ent.permit(DeltaCapability.VIOLENT_COLLISIONS);
                    li.setControlled(false);
                }
            }
            controller.path_target = null;
            controller.turning = 0;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState nextState) {
            for (LimbInfo li : controller.limbs){
                if (li.type == LimbType.ARM){
                    li.ent.forbid(DeltaCapability.VIOLENT_COLLISIONS);
                    li.setControlled(true);
                }
            }
        }
    },
    SHAKE_BODY {
        @Override
        AIState tick(ColossusController controller, int age) {
            return FLAIL_ARMS;
        }
    },
    CHASE_PLAYER_ON_GROUND {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (!controller.atTarget()) return this;
            if (controller.target_count > 8) return IDLE;
            if (controller.target_entity == null || controller.target_entity.isDead) return IDLE;
            if (!canTargetPlayer(controller, controller.target_entity)) return IDLE;
            controller.target_count++;
            if (!canTargetPlayer(controller, controller.target_entity)) {
                return IDLE;
            }
            controller.path_target = new Coord(controller.target_entity);
            if (controller.atTarget()) {
                return SHAKE_BODY;
            }
            return this;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState state) {
            controller.target_count = 0;
        }
    },
    SELECT_NEW_TARGET {
        @Override
        AIState tick(ColossusController controller, int age) {
            return this;
        }
        
    };
    
    boolean canTargetPlayer(ColossusController controller, Entity player) {
        double max_dist = 24 * 24 * controller.leg_size;
        double max_home_dist = 32 * 32;
        if (controller.getDistanceSqToEntity(player) > max_dist) return false;
        if (controller.home.distanceSq(new Coord(player)) > max_home_dist) return false;
        return true;
    }

    
    abstract AIState tick(ColossusController controller, int age);
    public void onEnterState(ColossusController controller, AIState state) { }
    public void onExitState(ColossusController controller, AIState nextState) { }
}