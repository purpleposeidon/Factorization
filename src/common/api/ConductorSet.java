package factorization.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

class ConductorSet implements Comparable {
    int totalCharge = 0;
    int memberCount = 0;
    static final int maxMemberCount = 24;
    
    IChargeConductor leader = null;
    TreeSet<ConductorSet> neighbors = null;
    Iterator<ConductorSet> neighborIterator = null;
    
    ConductorSet(IChargeConductor leader) {
        this.leader = leader;
        this.memberCount = 1;
        Charge lc = leader.getCharge();
        lc.conductorSet = this;
        lc.isConductorSetLeader = true;
        lc.justCreated = true;
    }
    
    boolean addConductor(IChargeConductor other) {
        if (memberCount > maxMemberCount) {
            return false;
        }
        other.getCharge().conductorSet = this;
        memberCount++;
        return true;
    }
    
    void update() {
        if (neighbors == null || neighbors.size() == 0) {
            neighbors = null;
            return;
        }
        if (neighborIterator == null || !neighborIterator.hasNext()) {
            neighborIterator = neighbors.iterator();
        }
        ConductorSet luckyNeighbor = neighborIterator.next(); //balance our charge with this neighbor
        if (luckyNeighbor.memberCount + memberCount < maxMemberCount && luckyNeighbor.memberCount <= memberCount) {
            //EAT our neighbor! Oh my!
            Iterable<IChargeConductor> noms = luckyNeighbor.getMembers(luckyNeighbor.leader);
            for (IChargeConductor nom : noms) {
                //nom.om();
                addConductor(nom);
            }
            neighborIterator.remove();
            return;
        }
        //balance our charge with the random neighbor
        int ourCharge = totalCharge + luckyNeighbor.totalCharge;
        int ourMemberCount = memberCount + luckyNeighbor.memberCount;
        
        int hisNewCharge = ourCharge * luckyNeighbor.memberCount / ourMemberCount;
        luckyNeighbor.totalCharge = hisNewCharge;
        totalCharge = ourCharge - hisNewCharge;
    }
    
    boolean addNeighbor(ConductorSet neighbor) {
        if (neighbor == this || neighbor == null) {
            return false;
        }
        if (neighbors == null) {
            neighbors = new TreeSet<ConductorSet>();
        }
        if (neighbors.add(neighbor)) {
            neighborIterator = null;
            neighbor.addNeighbor(this); //Recurses thrice. ({b}, {}) -> ({b}, {a}) -> ({b, b}, {a})
            return true;
        }
        return false;
    }
    
    private static ArrayList<IChargeConductor> frontier = new ArrayList<IChargeConductor>(5 * 5 * 4);
    private static HashSet<IChargeConductor> visited = new HashSet<IChargeConductor>(5 * 5 * 5);
    
    Iterable<IChargeConductor> getMembers(IChargeConductor seed) {
        if (seed == null) {
            return new ArrayList<IChargeConductor>(0);
        }
        frontier.clear();
        visited.clear();
        frontier.add(seed);
        visited.add(seed);
        ArrayList<IChargeConductor> ret = new ArrayList(memberCount);
        while (frontier.size() > 0) {
            IChargeConductor here = frontier.remove(0);
            Coord hereCoord = here.getCoord();
            Charge hereCharge = here.getCharge();
            if (hereCharge.conductorSet != this) {
                continue;
            }
            ret.add(here);
            for (IChargeConductor neighbor : hereCoord.getAdjacentTEs(IChargeConductor.class)) {
                if (visited.add(neighbor)) {
                    frontier.add(neighbor);
                }
            }
        }
        return ret;
    }

    @Override
    public int compareTo(Object arg0) {
        return this.hashCode() - arg0.hashCode();
    }
    
    
    
}
