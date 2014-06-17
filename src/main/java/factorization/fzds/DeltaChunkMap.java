package factorization.fzds;

import net.minecraft.world.chunk.Chunk;

import org.apache.commons.lang3.ArrayUtils;

import factorization.api.Coord;
import factorization.fzds.api.IDeltaChunk;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * Map<Chunk, IDeltaChunk[]>
 */
public class DeltaChunkMap {
    private TLongObjectHashMap<IDeltaChunk[]> values = new TLongObjectHashMap<IDeltaChunk[]>(32);
    
    private long hash(Chunk chunk) {
        return chunk.xPosition | (((long) chunk.zPosition) << 32);
    }
    
    private static final IDeltaChunk[] EMPTY_ARRAY = new IDeltaChunk[0];
    
    private IDeltaChunk[] normalize(IDeltaChunk[] val) {
        return val == null ? EMPTY_ARRAY : val;
    }
    
    public IDeltaChunk[] get(Chunk chunk) {
        return normalize(values.get(hash(chunk)));
    }
    
    public boolean remove(IDeltaChunk dse) {
        Coord lower = dse.getCorner();
        Coord upper = dse.getFarCorner();
        boolean any = false;
        for (int x = lower.x; x < upper.x; x += 16) {
            for (int z = lower.z; z < upper.z; z += 16) {
                Chunk chunk = lower.w.getChunkFromBlockCoords(x, z);
                any |= remove0(dse, chunk);
            }
        }
        return any;
    }
    
    private boolean remove0(IDeltaChunk dse, Chunk chunk) {
        IDeltaChunk[] origArray = get(chunk);
        if (origArray.length == 1 && origArray[0] == dse) {
            values.remove(hash(chunk));
            return true;
        }
        IDeltaChunk[] newArray = ArrayUtils.removeElement(origArray, dse);
        values.put(hash(chunk), newArray);
        return true;
    }
    
    public boolean add(IDeltaChunk dse) {
        Coord lower = dse.getCorner();
        Coord upper = dse.getFarCorner();
        boolean any = false;
        for (int x = lower.x; x < upper.x; x += 16) {
            for (int z = lower.z; z < upper.z; z += 16) {
                Chunk chunk = lower.w.getChunkFromBlockCoords(x, z);
                any |= add0(dse, chunk);
            }
        }
        return any;
    }
    
    private boolean add0(IDeltaChunk dse, Chunk chunk) {
        IDeltaChunk[] origArray = get(chunk);
        for (IDeltaChunk idc : origArray) {
            if (idc == dse) {
                return false;
            }
        }
        values.put(hash(chunk), ArrayUtils.add(origArray, dse));
        return true;
    }
    
    public void clear() {
        values.clear();
    }
}
