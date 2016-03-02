package factorization.flat.api;

import factorization.api.Coord;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.flat.FlatMod;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;

import java.util.Iterator;

/** This class is internal and is not for general use. */
public class AtSide {
    public final Coord at;
    public final EnumFacing side;

    public AtSide(Coord at, EnumFacing side) {
        if (SpaceUtil.sign(side) == -1) {
            this.at = at.add(side);
            this.side = side.getOpposite();
        } else {
            this.at = at;
            this.side = side;
        }
    }

    AtSide(Chunk c, int index) {
        side = FlatMod.byte2side(index);
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

    public Iterable<AtSide> iterateConnected() {
        return new Connected();
    }

    private class Connected implements Iterable<AtSide>, Iterator<AtSide> {
        @Override
        public Iterator<AtSide> iterator() {
            return this;
        }

        byte proj = -1, visited = 0;
        EnumFacing[] aplanarSides = new EnumFacing[4];

        Connected() {
            int i = 0;
            for (EnumFacing face : EnumFacing.VALUES) {
                if (face.getAxis() == side.getAxis()) continue;
                aplanarSides[i++] = face;
            }
        }

        @Override
        public boolean hasNext() {
            return proj != 1 && visited < 4;
        }

        @Override
        public AtSide next() {
            try {
                if (proj == -1 || proj == +1) {
                    Coord neighborBase = at.add(proj == -1 ? side.getOpposite() : side);
                    EnumFacing neighborSide = aplanarSides[visited];
                    return new AtSide(neighborBase, neighborSide);
                } else if (proj == 0) {
                    return new AtSide(at.add(aplanarSides[visited]), side);
                } else {
                    throw new IllegalStateException("Stop iterating");
                }
            } finally {
                visited++;
                if (visited == 4) {
                    visited = 0;
                    proj++;
                }
            }
        }
    }
}
