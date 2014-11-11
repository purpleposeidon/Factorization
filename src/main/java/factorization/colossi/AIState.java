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
import factorization.fzds.api.DeltaCapability;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.Core;
import factorization.shared.ReservoirSampler;

public enum AIState {
    INITIAL_STATE {
        @Override
        AIState tick(ColossusController controller, int age) {
            return IDLE;
        }
    },
    
    IDLE {
        @Override
        AIState tick(ColossusController controller, int age) {
            // Picks a state
            AIState pre = this.preempt(controller, age);
            if (pre != this) return pre;
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
            return preempt(controller, age);
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
    
    WANDER {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (controller.atTarget()) return IDLE;
            return this;
        }
        
        @Override
        public void onEnterState(ColossusController controller, AIState state) {
            int d = (int) (WorldGenColossus.SMOOTH_END * 0.85);
            Coord target = controller.home.copy();
            target.x += target.w.rand.nextInt(d) - d / 2;
            target.z += target.w.rand.nextInt(d) - d / 2;
            controller.path_target = target;
        }
        
    },
    
    WAIT {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (age > 20*3) return IDLE;
            return IDLE;
        }
    },
    
    ATTACK {
        @Override
        AIState tick(ColossusController controller, int age) {
            // TODO Auto-generated method stub
            return null;
        }
    },
    
    SELECT_NEW_TARGET {
        @Override
        AIState tick(ColossusController controller, int age) {
            return this;
        }
        
    },
    
    IGNORE {
        @Override
        AIState tick(ColossusController controller, int age) {
            // Give the player some time to attack the colossus unmolested
            if (age < 20 * 8) return IGNORE;
            if (age % 20 != 0) return IGNORE;
            if (controller.worldObj.rand.nextBoolean()) return ATTACK;
            return IGNORE;
        }
    },
    
    DISLODGE_RIDING_PLAYER {
        @Override
        AIState tick(ColossusController controller, int age) {
            // TODO Auto-generated method stub
            return null;
        }
    },
    
    BOW {
        @Override
        AIState tick(ColossusController controller, int age) {
            return BOW;
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
                li.ent.setRotationalVelocity(parity < 0 ? leftRotation : rightRotation);
            }
            if (age >= cycle_length * 3) {
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
                    li.ent.setRotationalVelocity(new Quaternion());
                    li.setControlled(true);
                }
            }
        }
    },
    
    SHAKE {
        @Override
        AIState tick(ColossusController controller, int age) {
            return FLAIL_ARMS;
        }
    },
    
    SPIN {
        @Override
        AIState tick(ColossusController controller, int age) {
            // TODO Auto-generated method stub
            return null;
        }
    },
    
    HURT_GROUNDED_PLAYER {
        @Override
        AIState tick(ColossusController controller, int age) {
            // TODO Auto-generated method stub
            return null;
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
                return SHAKE;
            }
            return this;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState state) {
            controller.target_count = 0;
        }
    },
    
    
    
    FALL {
        @Override
        public void onEnterState(ColossusController controller, AIState state) {
            int death_ticks = 20 * 6 * controller.leg_size;
            for (LimbInfo li : controller.limbs) {
                li.ent.setVelocity(0, 0, 0);
                li.ent.setRotationalVelocity(new Quaternion());
                li.act_violently = true;
                if (li.type == LimbType.BODY) {
                    IDeltaChunk idc = li.ent;
                    Quaternion fallAxis = Quaternion.getRotationQuaternionRadians(Math.PI / 2 / death_ticks, ForgeDirection.SOUTH);
                    Quaternion rotation = idc.getRotation();
                    fallAxis.incrRotateBy(rotation);
                    idc.setRotationalVelocity(fallAxis);
                }
            }
            controller.path_target = null;
        }
        
        @Override
        AIState tick(ColossusController controller, int age) {
            int death_ticks = 20 * 6 * controller.leg_size;
            if (age < death_ticks) return this;
            return EXPLODE;
        }
        
        @Override
        public void onExitState(ColossusController controller, AIState nextState) {
            for (LimbInfo li : controller.limbs) {
                li.ent.setVelocity(0, 0, 0);
                li.ent.setRotationalVelocity(new Quaternion());
            }
        }
    },
    EXPLODE {
        @Override
        AIState tick(ColossusController controller, int age) {
            if (age % 15 != 0) return this;
            boolean any = false;
            double n = 1 + (controller.leg_size / 2.0) * (age / 45);
            for (LimbInfo li : controller.limbs) {
                final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>((int)n, null);
                IDeltaChunk idc = li.ent;
                Coord.iterateCube(idc.getCorner(), idc.getFarCorner(), new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        if (here.isAir()) return;
                        if (here.getBlock() == Core.registry.colossal_block) {
                            int md = here.getMd();
                            if (md == ColossalBlock.MD_MASK) return;
                            if (md == ColossalBlock.MD_CORE) return;
                        }
                        sampler.give(here.copy());
                    }
                });
                for (Coord c : sampler) {
                    dislodge(idc, c);
                    any = true;
                }
            }
            if (any) {
                // Explosions can mess with our IDCs. :|
                for (LimbInfo li : controller.limbs) {
                    li.ent.motionX = li.ent.motionY = li.ent.motionZ = 0;
                }
            }
            return any ? this : EXPIRE;
        }
        
        void dislodge(IDeltaChunk idc, Coord src) {
            Coord dest = src.copy();
            idc.shadow2real(dest);
            Block b = src.getBlock();
            int md = src.getMd();
            float explosionPower = 2F;
            float explodeChance = 0.125F;
            if (b == Core.registry.colossal_block && md == ColossalBlock.MD_EYE) {
                explodeChance = 1;
                explodeChance = 4;
            }
            if (src.w.rand.nextFloat() < explodeChance) {
                dest.w.createExplosion(null, dest.x + 0.5, dest.y + 0.5, dest.z + 0.5, explosionPower, false);
            }
            if (!dest.isReplacable() || src.getTE() != null) {
                src.breakBlock();
                src.setAir();
                return;
            }
            if (b == Core.registry.colossal_block || src.getHardness() <= 0) {
                src.setAir();
                return;
            }
            TransferLib.move(src, dest, true, true);
            EntityFallingBlock sand = new EntityFallingBlock(dest.w, dest.x, dest.y, dest.z, dest.getId(), dest.getMd());
            sand.field_145812_b = 1; // "Time" field. This is set to make it not suicide immediately.
            dest.setAir();
            double gs = 1.0/20.0;
            sand.motionX = 0; //dest.w.rand.nextGaussian() * gs;
            sand.motionZ = 0; //dest.w.rand.nextGaussian() * gs;
            sand.motionY = Math.abs(dest.w.rand.nextGaussian() * gs);
            sand.worldObj.spawnEntityInWorld(sand);
        }
        
    },
    EXPIRE {
        @Override
        AIState tick(ColossusController controller, int age) { return this; }
        
        @Override
        public void onEnterState(final ColossusController controller, AIState state) {
            final ArrayList<Entity> lmps = new ArrayList();
            for (final LimbInfo li : controller.limbs) {
                Coord min = li.ent.getCorner();
                Coord max = li.ent.getFarCorner();
                Coord.iterateCube(min, max, new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        if (here.getBlock() != Core.registry.colossal_block) return;
                        int md = here.getMd();
                        switch (md) {
                        default: return;
                        case ColossalBlock.MD_EYE:
                        case ColossalBlock.MD_BODY_CRACKED:
                        case ColossalBlock.MD_CORE:
                            here.setAir();
                            Vec3 core = li.ent.shadow2real(here.createVector().addVector(0.5, 0.5, 0.5));
                            controller.worldObj.newExplosion(null, core.xCoord, core.yCoord, core.zCoord, 0.25F, false, true);
                            if (md == ColossalBlock.MD_CORE) {
                                ItemStack lmp = new ItemStack(Core.registry.logicMatrixProgrammer);
                                EntityItem ei = new EntityItem(controller.worldObj, core.xCoord, core.yCoord, core.zCoord, lmp);
                                ei.invulnerable = true;
                                ei.motionY = 1;
                                lmps.add(ei);
                                EntityFireworkRocket flare = new EntityFireworkRocket(controller.worldObj, core.xCoord, core.yCoord, core.zCoord, null);
                                lmps.add(flare);
                            }
                            break;
                        case ColossalBlock.MD_MASK:
                            here.setAir();
                            Coord real = here.copy();
                            li.ent.shadow2real(real);
                            if (real.isReplacable()) {
                                EntityFallingBlock mask = new EntityFallingBlock(real.w, real.x, real.y, real.z, Core.registry.colossal_block, ColossalBlock.MD_MASK);
                                mask.field_145812_b = 1; // "Time" field. This is set to make it not suicide immediately.
                                lmps.add(mask);
                            }
                            break;
                        }
                    }
                });
            }
            
            // This is so that they don't get blown up by the core explosion
            for (Entity l : lmps) {
                l.worldObj.spawnEntityInWorld(l);
            }
            
            for (LimbInfo li : controller.limbs) {
                li.ent.setDead();
            }
            controller.setDead();
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
    
    protected AIState preempt(ColossusController controller, int age) {
        if (controller.getHealth() <= 0) return FALL;
        return this;
    }
}