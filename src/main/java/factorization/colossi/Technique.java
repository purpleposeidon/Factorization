package factorization.colossi;

import static factorization.colossi.TechniqueKind.*;

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
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
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.Core;
import factorization.shared.ReservoirSampler;

public enum Technique implements IStateMachine<Technique> {
    STATE_MACHINE_ENTRY {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return INITIAL_BOW;
        }
        
    },
    PICK_NEXT_TECHNIQUE {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            ArrayList<Technique> avail = new ArrayList(controller.known);
            if (avail.isEmpty()) {
                Core.logWarning("This colossus is brain-dead!? " + controller);
                return DEATH_FALL;
            }
            boolean use_defense = controller.checkHurt(true);
            Collections.shuffle(avail);
            Technique chosen_offense = null;
            Technique chosen_defense = null;
            Technique chosen_idler = null;
            for (Technique tech : avail) {
                TechniqueKind kind = tech.getKind();
                if (use_defense && kind != DEFENSIVE) continue;
                switch (kind) {
                case OFFENSIVE:
                    chosen_offense = grade(chosen_offense, controller, tech);
                    if (chosen_offense != null) return chosen_offense;
                    break;
                case DEFENSIVE:
                    chosen_defense = grade(chosen_defense, controller, tech);
                    break;
                case IDLER:
                    chosen_idler = grade(chosen_idler, controller, tech);
                    break;
                }
            }
            if (chosen_defense != null) return chosen_defense;
            if (chosen_idler != null) return chosen_idler;
            return STAND_STILL; // Shouldn't happen. Delay for a bit.
        }
        
        Technique grade(Technique orig, ColossusController controller, Technique next) {
            if (orig != null) return orig;
            if (next.usable(controller)) return next;
            return null;
        }
        
    },
    
    
    INITIAL_BOW {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return false;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            BOW.onEnterState(controller, prevState);
            // Crack 2 mask blocks that are exposed UP but not EAST
            final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>(1, controller.worldObj.rand);
            Coord.iterateCube(controller.body.getCorner(), controller.body.getFarCorner(), new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (here.getBlock() != Core.registry.colossal_block) return;
                    if (here.getMd() != ColossalBlock.MD_MASK) return;
                    if (!here.add(ForgeDirection.UP).isAir()) return;
                    if (here.add(ForgeDirection.EAST).isAir()) return;
                    sampler.give(here.copy());
                }
            });
            for (Coord found : sampler) {
                // We might want a "cracked mask"
                found.setIdMd(Core.registry.colossal_block, ColossalBlock.MD_BODY_CRACKED, true);
            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            // Copy of BOW.tick >_>
            if (controller.checkHurt(false)) return UNBOW;
            return this; // Because of this!
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            // Add the other cracks
            int count = controller.getNaturalCrackCount();
            final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>(count, controller.worldObj.rand);
            Coord.iterateCube(controller.body.getCorner(), controller.body.getFarCorner(), new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (isExposedSkin(here)) {
                        sampler.give(here.copy());
                    }
                }
            });
            for (Coord found : sampler) {
                found.setIdMd(Core.registry.colossal_block, ColossalBlock.MD_BODY_CRACKED /* Unlike the case above, we DO want MD_BODY_CRACKED. I know you're going to mess this up. Don't do it. */, true);
            }
            controller.setTotalCracks(sampler.size());
        }
        
        boolean isExposedSkin(Coord cell) {
            if (cell.getBlock() != Core.registry.colossal_block) return false;
            if (cell.getMd() != ColossalBlock.MD_BODY) return false;
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                Coord n = cell.add(dir);
                if (n.isAir() || n.isReplacable()) return true;
            }
            return false;
        }
    },
    
    BOW {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            // So, uh, we really should do some IK here. But that's rather more math than I want to deal with. O.o
            // So body bends to 90°, arms bend to 45°, and it might clip through the ground or be too high up or something.
            // (And the legs bend -90° since they're rooted to the body)
            // And hopefully we're bipedal! It'd do something hilarious & derpy if quadrapedal or polypedal.
            Quaternion bodyBend = Quaternion.getRotationQuaternionRadians(Math.toRadians(70), ForgeDirection.NORTH);
            controller.bodyLimbInfo.target(bodyBend, bow_power, bendInterp);
            int bodyBendTime;
            if (controller.body.hasOrderedRotation()) {
                bodyBendTime = controller.body.getRemainingRotationTime();
            } else {
                bodyBendTime = 60; // Hmph! Make something up. Shouldn't happen.
            }
            for (LimbInfo limb : controller.limbs) {
                IDeltaChunk idc = limb.idc.getEntity();
                if (idc == null) continue;
                if (limb.type == LimbType.LEG) {
                    idc.orderTargetRotation(bodyBend.conjugate(), bodyBendTime, bendInterp);
                } else if (limb.type == LimbType.ARM) {
                    double armFlap = Math.toRadians(limb.side == BodySide.RIGHT ? -25 : 25);
                    double armHang = Math.toRadians(-90 - 45);
                    Quaternion flap = Quaternion.getRotationQuaternionRadians(armFlap, ForgeDirection.EAST);
                    Quaternion hang = Quaternion.getRotationQuaternionRadians(armHang, ForgeDirection.NORTH);
                    idc.orderTargetRotation(flap.multiply(hang).multiply(bodyBend), bodyBendTime, Interpolation.SMOOTH);
                }
            }
            controller.setTarget(null);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (controller.checkHurt(false)) return UNBOW;
            return this;
        }
    },
    
    UNBOW {
        @Override
        TechniqueKind getKind() {
            return TechniqueKind.OFFENSIVE;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                li.target(new Quaternion(), bow_power, bendInterp);
            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            for (LimbInfo li : controller.limbs) {
                if (li.isTurning()) return this;
            }
            return PICK_NEXT_TECHNIQUE;
        }
    },
    
    HAMMAR {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return true;
        }
    },
    
    KICK {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return true;
        }
    },
    
    BELLY_FLOP {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return true;
        }
    },
    
    PUNT {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return true;
        }
    },
    
    LEAN_BACK_AND_FLAIL {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
    },
    
    SPIN {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
    },
    
    SHRUG {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
    },
    
    SEMI_CARTWHEEL {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
    },
    
    WANDER {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
    },
    
    STAND_STILL {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                li.target(controller.body.getRotation(), 1);
            }
            super.onEnterState(controller, prevState);
        }
    },
    
    DEATH_FALL {
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                IDeltaChunk idc = li.idc.getEntity();
                idc.setVelocity(0, 0, 0);
                idc.setRotationalVelocity(new Quaternion());
                idc.permit(DeltaCapability.VIOLENT_COLLISIONS);
            }
            Quaternion fallAxis = Quaternion.getRotationQuaternionRadians(Math.PI / 2, ForgeDirection.SOUTH);
            Quaternion rotation = controller.body.getRotation();
            fallAxis.incrRotateBy(rotation);
            controller.bodyLimbInfo.target(fallAxis, 3, Interpolation.CUBIC);
            controller.setTarget(null);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return controller.body.hasOrderedRotation() ? this : DEATH_EXPLODE;
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            for (LimbInfo li : controller.limbs) {
                IDeltaChunk idc = li.idc.getEntity();
                idc.setVelocity(0, 0, 0);
                idc.setRotationalVelocity(new Quaternion());
            }
        }

        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
    },
    
    DEATH_EXPLODE {
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (age % 15 != 0) return this;
            boolean any = false;
            double n = 1 + (controller.leg_size / 2.0) * (age / 45);
            for (LimbInfo li : controller.limbs) {
                final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>((int)n, null);
                IDeltaChunk idc = li.idc.getEntity();
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
                    IDeltaChunk idc = li.idc.getEntity();
                    idc.motionX = idc.motionY = idc.motionZ = 0;
                }
            }
            return any ? this : DEATH_EXPIRE;
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
        
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
    },
    
    DEATH_EXPIRE {
        @Override
        public Technique tick(ColossusController controller, int age) { return this; }
        
        @Override
        public void onEnterState(final ColossusController controller, Technique prevState) {
            final ArrayList<Entity> lmps = new ArrayList();
            for (final LimbInfo li : controller.limbs) {
                final IDeltaChunk idc = li.idc.getEntity();
                Coord min = idc.getCorner();
                Coord max = idc.getFarCorner();
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
                            Vec3 core = idc.shadow2real(here.createVector().addVector(0.5, 0.5, 0.5));
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
                            idc.shadow2real(real);
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
                li.idc.getEntity().setDead();
            }
            controller.setDead();
        }
        
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
    }
    
    ;
    
    abstract TechniqueKind getKind();
    
    boolean usable(ColossusController controller) {
        return true;
    }
    
    @Override
    public Technique tick(ColossusController controller, int age) {
        return this;
    }

    @Override
    public void onEnterState(ColossusController controller, Technique prevState) { }

    @Override
    public void onExitState(ColossusController controller, Technique nextState) { }
    
    static final double bow_power = 0.1;
    static final Interpolation bendInterp = Interpolation.LINEAR; // SMOOTH could work; playing it safe tho
}
