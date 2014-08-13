package factorization.fzds;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import factorization.api.Coord;
import factorization.fzds.api.IDeltaChunk;

/**
 * Map<Chunk, IDeltaChunk[]>
 */
public class DeltaChunkMap {
    //private TLongObjectHashMap<IDeltaChunk[]> values = new TLongObjectHashMap<IDeltaChunk[]>(32);
    private Map<Long, IDeltaChunk[]> values = new HashMap();
    
    private long hash(int chunkX, int chunkZ) {
        long ret = chunkX | (((long) chunkZ) << 32);
        return ret;
    }
    
    private static final IDeltaChunk[] EMPTY_ARRAY = new IDeltaChunk[0];
    
    private IDeltaChunk[] normalize(IDeltaChunk[] val) {
        return val == null ? EMPTY_ARRAY : val;
    }
    
    public IDeltaChunk[] get(int chunkX, int chunkZ) {
        return normalize(values.get(hash(chunkX, chunkZ)));
    }
    
    public IDeltaChunk[] get(Coord at) {
        return normalize(values.get(hash(at.x/16, at.z/16)));
    }
    
    public boolean remove(IDeltaChunk dse) {
        Coord lower = dse.getCorner();
        Coord upper = dse.getFarCorner();
        boolean any = false;
        for (int x = lower.x - 16; x <= upper.x + 16; x += 16) {
            for (int z = lower.z - 16; z <= upper.z + 16; z += 16) {
                any |= remove0(dse, x/16, z/16);
            }
        }
        return any;
    }
    
    private boolean remove0(IDeltaChunk dse, int chunkX, int chunkZ) {
        IDeltaChunk[] origArray = get(chunkX, chunkZ);
        if (origArray.length == 1 && origArray[0] == dse) {
            values.remove(hash(chunkX, chunkZ));
            return true;
        }
        IDeltaChunk[] newArray = ArrayUtils.removeElement(origArray, dse);
        values.put(hash(chunkX, chunkZ), newArray);
        return true;
    }
    
    public boolean add(IDeltaChunk dse) {
        Coord lower = dse.getCorner();
        Coord upper = dse.getFarCorner();
        boolean any = false;
        for (int x = lower.x - 16; x <= upper.x + 16; x += 16) {
            for (int z = lower.z - 16; z <= upper.z + 16; z += 16) {
                any |= add0(dse, x/16, z/16);
            }
        }
        return any;
    }
    
    private boolean add0(IDeltaChunk dse, int chunkX, int chunkZ) {
        IDeltaChunk[] origArray = get(chunkX, chunkZ);
        for (IDeltaChunk idc : origArray) {
            if (idc == dse) {
                return false;
            }
        }
        values.put(hash(chunkX, chunkZ), ArrayUtils.add(origArray, dse));
        return true;
    }
    
    public void clear() {
        values.clear();
    }
}
