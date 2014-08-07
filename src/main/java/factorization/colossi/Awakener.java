package factorization.colossi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbInfo;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.DeltaChunk.AreaMap;
import factorization.fzds.DeltaChunk.DseDestination;
import factorization.fzds.api.DeltaCapability;
import factorization.fzds.api.IDeltaChunk;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class Awakener {
    int arm_size = 0, arm_length = 0;
    int leg_size = 0, leg_length = 0;
    
    public static void awaken(Coord src) {
        TileEntityColossalHeart heart = findNearestHeart(src);
        if (heart == null) return;
        Core.logInfo("XR10-class entity detected. Neutralization protocol selected."); 
        Awakener awakener = new Awakener(heart);
        boolean success = awakener.abandonedLongAgo_thisAncientGuardianBurnsItsRemainingPower();
        if (!success) {
            Core.logInfo("Body is deformed. Beginning self-destruct sequence.");
            Coord core = new Coord(heart);
            Core.logInfo("Heart located. Disconnecting LMP.");
            ItemStack lmp = new ItemStack(Core.registry.logicMatrixProgrammer);
            core.setAir();
            Core.logInfo("Good-bye, world!");
            core.w.newExplosion(null, core.x + 0.5, core.y + 0.5, core.z + 0.5, 0.25F, false, true);
            core.spawnItem(lmp);
            return;
        }
    }
    
    final TileEntityColossalHeart heartTE;
    Awakener(TileEntityColossalHeart heart) {
        this.heartTE = heart;
    }
    
    static TileEntityColossalHeart findNearestHeart(Coord src) {
        TileEntityColossalHeart ret = null;
        double ret_dist = 0;
        int r = 2;
        Coord at = src.copy();
        for (int dxChunk = -r; dxChunk <= r; dxChunk++) {
            for (int dzChunk = -r; dzChunk <= r; dzChunk++) {
                at.set(src);
                at.adjust(dxChunk * 16, 0, dzChunk * 16);
                for (TileEntity te : (Iterable<TileEntity>) at.getChunk().chunkTileEntityMap.values()) {
                    if (!(te instanceof TileEntityColossalHeart)) continue;
                    TileEntityColossalHeart heart = (TileEntityColossalHeart) te;
                    double dist = src.distanceSq(at);
                    if (ret == null) {
                        ret = heart;
                        ret_dist = dist;
                    } else if (dist < ret_dist) {
                        ret_dist = dist;
                        ret = heart;
                    }
                }
            }
        }
        
        return ret;
    }
    
    void details(String name, Set<Coord> set) {
        Core.logInfo("Body part " + name + " made of " + set.size() + " blocks");
    }
    
    void limbDetails(String name, ArrayList<Set<Coord>> limbs) {
        int i = 0;
        for (Set<Coord> limb : limbs) {
            details(name + i, limb);
            i++;
        }
    }
    
    void mark(Set<Coord> set, String msg) {
        for (Coord c : set) {
            new Notice(c, msg).withStyle(Style.FORCE, Style.EXACTPOSITION).sendToAll();
        }
    }
    
    void markSets(ArrayList<Set<Coord>> sets, String msg) {
        for (Set<Coord> set : sets) {
            mark(set, msg);
        }
    }
    
    int ground_level = -1;
    BlockState valid_natural_blocks = new BlockState(null, 0) {
        @Override
        public boolean matches(Coord at) {
            if (at.y <= ground_level) return false;
            if (at.getBlock() == Core.registry.colossal_block) return false;
            return !at.isAir() && at.getHardness() >= 0;
        }
    };
    
    BlockState BODY_ANY = new BlockState(null, 0) {
        @Override
        public boolean matches(Coord at) {
            if (at.getBlock() == Core.registry.colossal_block) {
                int md = at.getMd();
                return md == ColossalBlock.MD_BODY || md == ColossalBlock.MD_BODY_CRACKED;
            }
            return false;
        }
    };
    
    static class SetAndInfo {
        Set<Coord> set;
        int length;
        int size;
        Vec3 rotation;
        LimbType limbType;
        BodySide limbSide;
        public SetAndInfo(Set<Coord> set, int length, int size, Vec3 rotation, LimbType limbType, BodySide limbSide) {
            this.set = set;
            this.length = length;
            this.size = size;
            this.rotation = rotation;
            this.limbType = limbType;
            this.limbSide = limbSide;
        }
    }
    
    public final boolean abandonedLongAgo_thisAncientGuardianBurnsItsRemainingPower() {
        Core.logInfo("Awakening Collossus at %s...", new Coord(heartTE));
        Set<Coord> heart = new HashSet<Coord>();
        heart.add(new Coord(heartTE));
        details("heart", heart);
        Set<Coord> body = iterateFrom(heart, BODY_ANY, false);
        details("body", body);
        Set<Coord> mask = iterateFrom(body, ColossalBuilder.MASK, true);
        details("mask", mask);
        Set<Coord> eyes = iterateFrom(mask, ColossalBuilder.EYE, true);
        details("eyes", eyes);
        ArrayList<Set<Coord>> arms = getConnectedLimbs(body, ColossalBuilder.ARM);
        limbDetails("arms", arms);
        ArrayList<Set<Coord>> legs = getConnectedLimbs(body, ColossalBuilder.LEG);
        limbDetails("legs", legs);
        
        if (arms.isEmpty() || legs.isEmpty()) return false;
        
        body.addAll(heart);
        body.addAll(mask);
        body.addAll(eyes);
        heart = mask = eyes = null;
        details("mainBody", body);
        
        if (!verifyArmDimensions(arms)) return false;
        if (!verifyLegDimensions(legs)) return false;
        
        Core.logInfo("Limb sizes match");
        
        ArrayList<SetAndInfo> limbInfo = new ArrayList();
        for (Set<Coord> arm : arms) {
            Vec3 joint = calculateJointPosition(arm, arm_size, arm_length);
            SetAndInfo sai = new SetAndInfo(arm, arm_length, arm_size, joint, LimbType.ARM, getSide(arm));
            limbInfo.add(sai);
        }
        Vec3 leg_sum = Vec3.createVectorHelper(0, 0, 0);
        for (Set<Coord> leg: legs) {
            Vec3 joint = calculateJointPosition(leg, leg_size, leg_length);
            SetAndInfo sai = new SetAndInfo(leg, leg_length, leg_size, joint, LimbType.LEG, getSide(leg));
            limbInfo.add(sai);
            FzUtil.incrAdd(leg_sum, joint);
        }
        Vec3 body_center_of_mass = leg_sum;
        FzUtil.scale(body_center_of_mass, 1.0/legs.size());
        body_center_of_mass.yCoord += 1;
        SetAndInfo sai = new SetAndInfo(body, measure_dim(body, 1), leg_size, body_center_of_mass, LimbType.BODY, BodySide.RIGHT);
        limbInfo.add(sai);
        
        ArrayList<Set<Coord>> all_members = new ArrayList();
        all_members.add(body);
        all_members.addAll(arms);
        all_members.addAll(legs);
        
        boolean first = true;
        for (Set<Coord> set : all_members) {
            int l = lowest(set);
            if (first) {
                first = false;
                ground_level = l;
            } else if (l < ground_level) {
                ground_level = l;
            }
        }
        
        double max_iter = Math.pow(body.size(), 1.0/3.0) * 2;
        includeShell(all_members, new HashSet(), (int) max_iter + 3);
        Core.logInfo("Attatched blocks included. New stats:");
        limbDetails("arms", arms);
        limbDetails("legs", legs);
        details("mainBody", body);
        
        // markSets(legs, "|");
        // markSets(arms, "-");
        // mark(body, "+");
        
        ArrayList<LimbInfo> parts = new ArrayList();
        int i = 0;
        for (SetAndInfo partInfo : limbInfo) {
            //if (i != 0) continue;
            IDeltaChunk idc = createIDC(partInfo.set, partInfo.rotation);
            LimbInfo li = new LimbInfo(partInfo.limbType, partInfo.limbSide, partInfo.length, idc);
            parts.add(li);
            i++;
        }
        
        int part_size = parts.size();
        Core.logInfo("Activated %s parts", part_size);
        LimbInfo[] info = parts.toArray(new LimbInfo[part_size]);
        ColossusController controller = new ColossusController(heartTE.getWorldObj(), info);
        new Coord(heartTE).setAsEntityLocation(controller);
        controller.worldObj.spawnEntityInWorld(controller);
        return true;
    }
    
    BodySide getSide(Set<Coord> set) {
        return one(set).x < heartTE.xCoord ? BodySide.LEFT : BodySide.RIGHT;
    }
    
    Vec3 calculateJointPosition(Set<Coord> limb, int size, int length) {
        Coord corner = null;
        for (Coord c : limb) {
            if (corner == null) {
                corner = c;
                continue;
            }
            if (c.y > corner.y) {
                c = corner;
            } else if (c.x < corner.x || c.z < corner.z) {
                c = corner;
            }
        }
        if (corner == null) return Vec3.createVectorHelper(0, 0, 0);
        Vec3 ret = corner.createVector();
        ret.xCoord += size/2;
        ret.yCoord -= size/2;
        ret.zCoord += size/2;
        return ret;
    }

    boolean verifyLegDimensions(ArrayList<Set<Coord>> legs) {
        boolean first = true;
        for (Set<Coord> leg : legs) {
            if (first) {
                leg_length = measure_dim(leg, 1);
                leg_size = measure_size(leg);
                first = false;
            } else {
                int new_leg_length = measure_dim(leg, 1);
                int new_leg_size = measure_size(leg);
                if (leg_length != new_leg_length || leg_size != new_leg_size) {
                    Core.logInfo("Mismatched legs!");
                    return false;
                }
            }
        }
        if (leg_length == -1 || leg_size == -1) {
            Core.logInfo("Invalid legs!");
            return false;
        }
        return true;
    }
    
    boolean verifyArmDimensions(ArrayList<Set<Coord>> arms) {
        boolean first = true;
        for (Set<Coord> arm : arms) {
            if (first) {
                arm_length = measure_dim(arm, 1);
                arm_size = measure_size(arm);
                first = false;
            } else {
                int new_arm_length = measure_dim(arm, 1);
                int new_arm_size = measure_size(arm);
                if (arm_length != new_arm_length || arm_size != new_arm_size) {
                    Core.logInfo("Mismatched arms!");
                    return false;
                }
            }
        }
        if (arm_length == -1 || arm_size == -1) {
            Core.logInfo("Invalid arms!");
            return false;
        }
        return true;
    }
    
    int measure_dim(Set<Coord> set, int axis) {
        Coord min = null, max = null;
        for (Coord c : set) {
            if (min == null || max == null) {
                min = c;
                max = c;
            } else if (c.get(axis) < min.get(axis)){
                min = c;
            } else if (c.get(axis) > max.get(axis)){
                max = c;
            }
        }
        if (min == null || max == null) return 0;
        return max.get(axis) - min.get(axis);
    }
    
    int measure_size(Set<Coord> set) {
        int w = measure_dim(set, 0);
        int d = measure_dim(set, 2);
        if (w != d) return 0;
        return w;
    }
    
    int lowest(Set<Coord> set) {
        Coord min = null;
        for (Coord c : set) {
            if (min == null) {
                min = c;
            } else if (c.y < min.y){
                min = c;
            }
        }
        if (min == null) return -1;
        return min.y;
    }
    
    Set<Coord> iterateFrom(Set<Coord> start, BlockState block, boolean diag) {
        ArrayList<Coord> frontier = new ArrayList(start.size());
        frontier.addAll(start);
        Set<Coord> ret = new HashSet(frontier.size());
        while (!frontier.isEmpty()) {
            Coord at = frontier.remove(0);
            Iterable<Coord> neighbors = diag ? at.getNeighborsDiagonal() : at.getNeighborsAdjacent();
            for (Coord neighbor : neighbors) {
                if (block.matches(neighbor) && ret.add(neighbor)) {
                    frontier.add(neighbor);
                }
            }
        }
        return ret;
    }
    
    ArrayList<Set<Coord>> getConnectedLimbs(Set<Coord> body, BlockState block) {
        ArrayList<Set<Coord>> ret = new ArrayList();
        for (Coord at : body) {
            for (Coord neighbor : at.getNeighborsAdjacent()) {
                if (!body.contains(neighbor) && !inClasses(ret, neighbor) && block.matches(neighbor)) {
                    Set<Coord> newSeed = new HashSet();
                    newSeed.add(neighbor);
                    Set<Coord> newTerrain = iterateFrom(newSeed, block, false);
                    ret.add(newTerrain);
                }
            }
        }
        return ret;
    }
    
    boolean inClasses(ArrayList<Set<Coord>> lists, Coord at) {
        for (Set<Coord> sc : lists) {
            if (sc.contains(at)) return true;
        }
        return false;
    }
    
    void includeShell(ArrayList<Set<Coord>> sets, Set<Coord> exclude, int maxIter) {
        HashMap<Set<Coord>, Set<Coord>> pending2origs = new HashMap();
        Set<Coord> allFound = new HashSet();
        for (Set<Coord> f : sets) {
            pending2origs.put(f, f);
            allFound.addAll(f);
        }
        while (!pending2origs.isEmpty() && maxIter-- > 0) {
            Set<Coord> set = one(pending2origs);
            Set<Coord> orig = pending2origs.remove(set);
            
            Set<Coord> newBlocks = new HashSet();
            for (Coord at : set) {
                for (Coord neighbor : at.getNeighborsAdjacent()) {
                    if (allFound.contains(neighbor) || set.contains(neighbor) || exclude.contains(neighbor) || !valid_natural_blocks.matches(neighbor)) continue;
                    newBlocks.add(neighbor);
                    allFound.add(neighbor);
                }
            }
            if (!newBlocks.isEmpty()) {
                orig.addAll(newBlocks);
                pending2origs.put(newBlocks, orig);
            }
        }
    }
    
    Set<Coord> one(HashMap<Set<Coord>, Set<Coord>> map) {
        for (Set<Coord> ret : map.keySet()) return ret;
        return null;
    }
    
    Coord one(Set<Coord> set) {
        for (Coord ret : set) return ret;
        return null;
    }
    
    IDeltaChunk createIDC(final Set<Coord> parts, Vec3 rotationCenter) {
        Coord min = null, max = null;
        for (Coord c : parts) {
            if (min == null || max == null) {
                min = c;
                max = c;
            } else {
                if (max.isCompletelySubmissiveTo(c)) {
                    max = c;
                } else if (c.isCompletelySubmissiveTo(min)) {
                    min = c;
                }
            }
        }
        if (min == null || max == null) return null;
        Coord.sort(min, max);
        int r = 5;
        min.adjust(new DeltaCoord(-r, -r, -r));
        max.adjust(new DeltaCoord(r, r, r));
        IDeltaChunk ret = DeltaChunk.makeSlice(ColossusFeature.deltachunk_channel, min, max, new AreaMap() {
            @Override
            public void fillDse(DseDestination destination) {
                for (Coord c : parts) {
                    destination.include(c);
                }
            }
        }, true);
        for (DeltaCapability permit : new DeltaCapability[] {
                DeltaCapability.INTERACT,
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.ROTATE,
                DeltaCapability.COLLIDE,
                DeltaCapability.DRAG,
                DeltaCapability.ENTITY_PHYSICS,
                DeltaCapability.MOVE,
                DeltaCapability.PHYSICS_DAMAGE,
                DeltaCapability.REMOVE_ITEM_ENTITIES,
                DeltaCapability.REMOVE_ALL_ENTITIES
        }) {
            ret.permit(permit);
        }
        ret.forbid(DeltaCapability.EMPTY);
        ret.worldObj.spawnEntityInWorld(ret);
        //ret.setRotationalCenterOffset(rotationCenter);
        return ret;
    }
}
