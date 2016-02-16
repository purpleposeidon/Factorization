package factorization.flat;

import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;

final class AtSide {
    final Coord at;
    final EnumFacing side;

    AtSide(Coord at, EnumFacing side) {
        if (SpaceUtil.sign(side) == -1) {
            this.at = at.add(side);
            this.side = side.getOpposite();
        } else {
            this.at = at;
            this.side = side;
        }
    }

    AtSide(Chunk c, int index) {
        side = FlatFeature.byte2side(index);
        int slabX = (index >> 2) & 0xF;
        int slabY = (index >> 6) & 0xF;
        int slabZ = (index >> 10) & 0xF;
        int x = c.xPosition << 4;
        int z = c.zPosition << 4;
        at = new Coord(c.getWorld(), x, slabY, z);
    }

    FlatChunkLayer getLayer() {
        Chunk chunk = at.getChunk();
        return ((IExtraChunkData) chunk).getFlatLayer();
    }

    FlatFace get() {
        return getLayer().get(at, side);
    }

    void set(FlatFace face) {
        getLayer().set(at, side, face);
    }
}
