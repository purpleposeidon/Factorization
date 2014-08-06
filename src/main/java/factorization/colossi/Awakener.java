package factorization.colossi;

import net.minecraft.tileentity.TileEntity;
import factorization.api.Coord;

public class Awakener {
    public static void awaken(Coord src) {
        TileEntityColossalHeart heart = findNearestHeart(src);
        if (heart == null) return;
        Awakener awakener = new Awakener(heart);
        awakener.run();
    }
    
    final TileEntityColossalHeart heart;
    Awakener(TileEntityColossalHeart heart) {
        this.heart = heart;
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
}
