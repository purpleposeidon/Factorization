package factorization.fzds;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.fzds.api.IDeltaChunk;

public class DeltaChunk {
    static Set<IDeltaChunk> getSlices(World w) {
        if (w == null) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                return Hammer.clientSlices;
            } else {
                return Hammer.serverSlices;
            }
        }
        return w.isRemote ? Hammer.clientSlices : Hammer.serverSlices;
    }
    
    public static World getClientShadowWorld() {
        return Hammer.worldClient;
    }
    
    public static World getServerShadowWorld() {
        return DimensionManager.getWorld(Hammer.dimensionID);
    }
    
    public static World getClientRealWorld() {
        return Hammer.proxy.getClientRealWorld();
    }
    
    /***
     * @return the thread-appropriate shadow world
     */
    public static World getWorld(World realWorld) {
        if (realWorld == null) {
            return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ? getClientShadowWorld() : getServerShadowWorld();
        }
        return realWorld.isRemote ? getClientShadowWorld() : getServerShadowWorld();
    }
    
    public static IDeltaChunk allocateSlice(World spawnWorld, int channel, DeltaCoord size) {
        Coord base = Hammer.hammerInfo.takeCell(channel, size);
        return new DimensionSliceEntity(spawnWorld, base, base.add(size));
    }
    
    public static IDeltaChunk findClosest(Entity target, Coord pos) {
        if (target == null) {
            return null;
        }
        IDeltaChunk closest = null;
        double dist = Double.POSITIVE_INFINITY;
        World real_world = DeltaChunk.getClientRealWorld();
        
        for (IDeltaChunk here : DeltaChunk.getSlices(target.worldObj)) {
            if (here.worldObj != real_world && !pos.inside(here.getCorner(), here.getFarCorner())) {
                continue;
            }
            if (closest == null) {
                closest = here;
                continue;
            }
            double here_dist = target.getDistanceSqToEntity(here);
            if (here_dist < dist) {
                dist = here_dist;
                closest = here;
            }
        }
        return closest;
    }
    
    private static Vec3 buffer = Vec3.createVectorHelper(0, 0, 0);
    
    public static Vec3 shadow2nearestReal(Entity player, double x, double y, double z) {
        //The JVM sometimes segfaults in this function.
        IDeltaChunk closest = findClosest(player, new Coord(player.worldObj, x, y, z));
        if (closest == null) {
            return null;
        }
        buffer.xCoord = x;
        buffer.yCoord = y;
        buffer.zCoord = z;
        Vec3 ret = closest.shadow2real(buffer);
        return ret;
    }
    
    public static interface AreaMap {
        void fillDse(DseDestination destination);
    }
    
    public static interface DseDestination {
        void include(Coord c);
    }
    
    private static Coord shadow = new Coord(null, 0, 0, 0);
    
    public static IDeltaChunk makeSlice(int channel, final Coord min, final Coord max, AreaMap mapper, boolean wipeSrc) {
        DeltaCoord size = max.difference(min);
        final IDeltaChunk dse = allocateSlice(min.w, channel, size);
        Vec3 vrm = min.centerVec(max);
        dse.posX = (int)vrm.xCoord;
        dse.posY = (int)vrm.yCoord;
        dse.posZ = (int)vrm.zCoord;
        mapper.fillDse(new DseDestination() {public void include(Coord real) {
            shadow.set(real);
            dse.real2shadow(shadow);
            TransferLib.move(real, shadow, false, true);
        }});
        if (wipeSrc) {
            mapper.fillDse(new DseDestination() {public void include(Coord real) {
                shadow.set(real);
                dse.real2shadow(shadow);
                TransferLib.setRaw(shadow, 1, 0);
                shadow.setId(0);
            }});
        }
        return dse;
    }
    
    public static void paste(IDeltaChunk selected, boolean overwriteDestination) {
        Coord a = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
        Coord b = a.copy();
        Vec3 vShadowMin = Vec3.createVectorHelper(0, 0, 0);
        Vec3 vShadowMax = Vec3.createVectorHelper(0, 0, 0);
        selected.getCorner().setAsVector(vShadowMin);
        selected.getFarCorner().setAsVector(vShadowMax);
        a.set(vShadowMin);
        b.set(vShadowMax);
        DeltaCoord dc = b.difference(a);
        Coord dest = new Coord(selected);
        Coord c = new Coord(a.w, 0, 0, 0);
        
        for (int x = a.x; x < b.x; x++) {
            for (int y = a.y; y < b.y; y++) {
                for (int z = a.z; z < b.z; z++) {
                    c.set(a.w, x, y, z);
                    dest.set(c);
                    selected.shadow2real(dest);
                    TransferLib.move(c, dest, false, overwriteDestination);
                }
            }
        }
    }
    
    public static void clear(IDeltaChunk selected) {
        Coord a = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
        Coord b = a.copy();
        Vec3 vShadowMin = Vec3.createVectorHelper(0, 0, 0);
        Vec3 vShadowMax = Vec3.createVectorHelper(0, 0, 0);
        selected.getCorner().setAsVector(vShadowMin);
        selected.getFarCorner().setAsVector(vShadowMax);
        a.set(vShadowMin);
        b.set(vShadowMax);
        
        Coord c = new Coord(a.w, 0, 0, 0);
        for (int x = a.x; x < b.x; x++) {
            for (int y = a.y; y < b.y; y++) {
                for (int z = a.z; z < b.z; z++) {
                    c.set(a.w, x, y, z);
                    selected.shadow2real(c);
                    c.markBlockForUpdate();
                }
            }
        }
    }
    
}
