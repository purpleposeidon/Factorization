package factorization.colossi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class Awakener {
    public static void awaken(Coord src) {
        TileEntityColossalHeart heart = findNearestHeart(src);
        if (heart == null) return;
        Awakener awakener = new Awakener(heart);
        awakener.run();
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
    
    public void run() {
        Core.logInfo("Awakening Collossus at %s...", new Coord(heartTE));
        Set<Coord> heart = new HashSet<Coord>();
        heart.add(new Coord(heartTE));
        details("heart", heart);
        Set<Coord> body = iterateFrom(heart, ColossalBuilder.BODY, false);
        details("body", body);
        Set<Coord> mask = iterateFrom(body, ColossalBuilder.MASK, true);
        details("mask", mask);
        Set<Coord> eyes = iterateFrom(mask, ColossalBuilder.EYE, true);
        details("eyes", eyes);
        ArrayList<Set<Coord>> legs = getConnectedLimbs(body, ColossalBuilder.LEG);
        limbDetails("legs", legs);
        ArrayList<Set<Coord>> arms = getConnectedLimbs(body, ColossalBuilder.ARM);
        limbDetails("arms", arms);
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
    
    Coord excersizeAxiomOfChoice(Set<Coord> bulk) {
        for (Iterator<Coord> iterator = bulk.iterator(); iterator.hasNext();) {
            Coord ret = iterator.next();
            iterator.remove();
            return ret;
        }
        return null;
    }
    
    boolean adjacentToSet(Set<Coord> bulk, Coord at) {
        for (Coord member : bulk) {
            if (at.distanceSq(member) <= 1) return true;
        }
        return false;
    }
}
