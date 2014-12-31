package factorization.colossi;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.Core;
import factorization.shared.ReservoirSampler;

public enum AIState implements IStateMachine<AIState> {
    IDLE {
        @Override
        public AIState tick(ColossusController controller, int age) {
            // Picks a state
            AIState pre = this.preempt(controller, age);
            if (pre != this) return pre;
            Coord at = new Coord(controller);
            Coord home = controller.getHome();
            if (at.x == home.x && at.z == home.z) {
                controller.setTarget(home.add(24, 0, 0));
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
            return RUN_BACK;
        }
    },
    
    WALK_EAST_SAFELY {
        @Override
        public AIState tick(ColossusController controller, int age) {
            if (age % 40 == 0) {
                if (controller.atTarget() && controller.worldObj.rand.nextBoolean()) {
                    return IDLE;
                }
            }
            return preempt(controller, age);
        }
    },
    
    RUN_BACK {
        @Override
        public AIState tick(ColossusController controller, int age) {
            if (controller.atTarget()) return IDLE;
            return this;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState prevState) {
            controller.goHome();
        }
    },
    
    WANDER {
        @Override
        public AIState tick(ColossusController controller, int age) {
            if (controller.atTarget()) return IDLE;
            return this;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState prevState) {
            int d = (int) (WorldGenColossus.SMOOTH_END * 0.85);
            Coord target = controller.getHome().copy();
            target.x += target.w.rand.nextInt(d) - d / 2;
            target.z += target.w.rand.nextInt(d) - d / 2;
            controller.setTarget(target);
        }
        
    },
    
    WAIT {
        @Override
        public AIState tick(ColossusController controller, int age) {
            if (age > 20*3) return IDLE;
            return IDLE;
        }
    },
    
    IGNORE {
        @Override
        public AIState tick(ColossusController controller, int age) {
            // Give the player some time to attack the colossus unmolested
            if (age < 20 * 8) return IGNORE;
            if (age % 20 != 0) return IGNORE;
            if (controller.worldObj.rand.nextBoolean()) return ATTACK;
            return IGNORE;
        }
    },
    
    FLAIL_ARMS {
        @Override
        public AIState tick(ColossusController controller, int age) {
            int cycle_length = controller.arm_length * 40;
            // TODO: There's many wrong usages of arm_length; it needs some kind of radial conversion.
            // We the instantaneous linear velocity of something rotating radially with a radius of arm_length to have some specific target velocity for all arm_lengths.
            if (cycle_length == 0) return IDLE;
            double maxVelocity = Math.PI / cycle_length;
            double t = age / cycle_length;
            if (t == 0) {
                AIState pre = this.preempt(controller, age);
                if (pre != this) {
                    return pre;
                }
            }
            double d = -maxVelocity * Math.sin(Math.PI * 2 * t);
            Quaternion leftRotation = Quaternion.getRotationQuaternionRadians(d, ForgeDirection.EAST);
            Quaternion rightRotation = new Quaternion(leftRotation);
            rightRotation.incrConjugate();
            
            for (LimbInfo li : controller.limbs) {
                if (li.type != LimbType.ARM) continue;
                int parity = li.side == BodySide.LEFT ? -1 : 1;
                li.idc.getEntity().setRotationalVelocity(parity < 0 ? leftRotation : rightRotation);
            }
            if (age >= cycle_length * 3) {
                return IDLE;
            }
            return FLAIL_ARMS;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState prevState) {
            for (LimbInfo li : controller.limbs){
                if (li.type == LimbType.ARM){
                    li.idc.getEntity().permit(DeltaCapability.VIOLENT_COLLISIONS);
                }
            }
            controller.setTarget(null);
            // Do we need this? controller.turning = 0;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState nextState) {
            for (LimbInfo li : controller.limbs){
                if (li.type == LimbType.ARM){
                    li.idc.getEntity().forbid(DeltaCapability.VIOLENT_COLLISIONS);
                    li.idc.getEntity().setRotation(new Quaternion());
                }
            }
        }
    },
    
    CHASE_PLAYER_ON_GROUND {
        @Override
        public AIState tick(ColossusController controller, int age) {
            if (!controller.atTarget()) return this;
            if (controller.target_count > 8) return IDLE;
            if (controller.target_entity == null || controller.target_entity.isDead) return IDLE;
            if (!controller.canTargetPlayer(controller.target_entity)) return IDLE;
            controller.target_count++;
            if (!controller.canTargetPlayer(controller.target_entity)) {
                return IDLE;
            }
            controller.setTarget(new Coord(controller.target_entity));
            if (controller.atTarget()) {
                return SHAKE;
            }
            return this;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState state) {
            controller.target_count = 0;
        }
    };
    
    

    
    public abstract AIState tick(ColossusController controller, int age);
    public void onEnterState(ColossusController controller, AIState prevState) { }
    public void onExitState(ColossusController controller, AIState nextState) { }
    
    protected AIState preempt(ColossusController controller, int age) {
        if (controller.getHealth() <= 0) return FALL;
        return this;
    }
}