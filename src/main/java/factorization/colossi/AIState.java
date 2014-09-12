package factorization.colossi;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import factorization.api.Coord;

enum AIState {
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
            return IDLE;
        }
    },
    SHAKE_BODY {
        @Override
        AIState tick(ColossusController controller, int age) {
            return IDLE;
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