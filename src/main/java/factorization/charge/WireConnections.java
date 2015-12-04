package factorization.charge;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IChargeConductor;
import factorization.api.VectorUV;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;

public class WireConnections {
    TileEntityWire me;
    CubeFace my_face;
    DeltaCoord my_face_vector;
    Coord here;

    long edges = 0; //bitmask of edges: edges on lower face NESW, upper face NESW, middle edges starting from NE and going clockwise
    long faces = 0; //bitmask of faces, proceeds as usual: -Y +Y -Z +Z -X +X
    boolean center_core = false; //render the middle cube

    public WireConnections(TileEntityWire me) {
        this.me = me;
        this.my_face = new CubeFace(me.supporting_side);
        this.my_face_vector = my_face.toVector();
        this.here = me.getCoord();

        Core.profileStart("factoryWire"); //Looks like this is *very* efficient!
        calculate();
        Core.profileEnd();
    }

    void calculate() {
        //******************************************************************************************************************************
        //NOTE: Be *very* careful with this code, it's a huge PITA. git commit & check the wire testing area before & after any changes.
        //******************************************************************************************************************************
        int wire_neighbor_count = 0;
        for (DeltaCoord d : DeltaCoord.directNeighbors) {
            Coord nc = here.add(d);
            TileEntity neighbor = nc.getTE();
            if (neighbor == null) {
                continue;
            }
            CubeFace delta_face = CubeFace.fromVector(d);
            
            if (neighbor instanceof TileEntityWire) {
                TileEntityWire n = (TileEntityWire) neighbor;
                CubeFace neighbor_face = new CubeFace(n.supporting_side);
                addNeighbor(d, n, neighbor_face, delta_face);
                wire_neighbor_count++;
                continue;
            }
            if (neighbor instanceof IChargeConductor) {
                if (delta_face.equals(my_face)) {
                    //under us
                    faces |= my_face.getFaceFlag();
                } else if (delta_face.opposite().equals(my_face)) {
                    //above us
                    faces |= my_face.getFaceFlag() | delta_face.getFaceFlag();
                    center_core = true;
                } else {
                    //adjacent
                    edges |= my_face.getEdgeFlags() & delta_face.getEdgeFlags();
                    faces |= my_face.getFaceFlag();
                }
            }
        }

        //don't be invisible
        if (faces == 0 && edges == 0) {
            faces = my_face.getFaceFlag();
            return;
        }

        if (faces == 0 && Long.bitCount(edges) == 1 && wire_neighbor_count == 1) {
            faces = my_face.getFaceFlag();
        }

        //if a face has more than 1 edge drawn, draw the face as well
        for (int side = 0; side < 6; side++) {
            if (Long.bitCount(edges & CubeFace.getEdgeFlags(side)) > 1) {
                faces |= (1 << side);
            }
        }

        if (Long.bitCount(edges) > 1 && faces == 0) {
            faces = my_face.getFaceFlag();
        }
    }

    void addNeighbor(DeltaCoord neighbor, TileEntityWire nte, CubeFace neighbor_face,
            CubeFace delta_face) {
        //******************************************************************************************************************************
        //NOTE: Be *very* careful with this code, it's a huge PITA. git commit & check the wire testing area before & after any changes.
        //******************************************************************************************************************************
        long edge_to_add = delta_face.getEdgeFlags() & my_face.getEdgeFlags();
        if (edge_to_add == 0) {
            //neighbor's support is far away
            edge_to_add = delta_face.getEdgeFlags() & neighbor_face.getEdgeFlags();
            if (edge_to_add == 0) {
                //neighbor's on top of us. This doesn't happen with normal wire. Do nothing.
            } else if (Long.bitCount(edge_to_add) > 1) {
                center_core = true;
                faces |= delta_face.getFaceFlag() | my_face.getFaceFlag();
            } else {
                edges |= edge_to_add;
                faces |= neighbor_face.getFaceFlag();
                edges |= neighbor_face.getEdgeFlags() & my_face.getEdgeFlags();
            }
        } else if (Long.bitCount(edge_to_add) > 1) {
            //we're supporting the neighbor.
            edges |= neighbor_face.getEdgeFlags() & my_face.getEdgeFlags();
        } else {
            edges |= edge_to_add;
            if (neighbor.isSubmissive()) {
                edge_to_add = neighbor_face.getEdgeFlags() & delta_face.getEdgeFlags();
                if (Long.bitCount(edge_to_add) == 1) {
                    edges |= edge_to_add;
                }
            }
        }
    }

    int getComplexity() {
        return Long.bitCount(edges) + Long.bitCount(faces) * 2 + (center_core ? 4 : 0);
    }

    static WireRenderingCube cube(VectorUV corner, VectorUV origin) {
        return new WireRenderingCube(BlockIcons.wire, corner, origin);
    }

    static final float h = 2, w = 4; //these are half of the actual size.
    static WireRenderingCube base_face = cube(new VectorUV(w, h, w), new VectorUV(0, -8 + h, 0));
    static WireRenderingCube base_face_side = base_face.copy().rotate(1, 0, 0, 90).normalize();
    static WireRenderingCube face_map[] = {
            base_face.copy(),
            base_face.rotate(1, 0, 0, 180).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 0).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 2).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 1).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 3).normalize(),
    };
    static WireRenderingCube bottom_edge = cube(new VectorUV(w, h, h), new VectorUV(0, -8 + h, 8 - h)),
            top_edge = bottom_edge.copy().rotate(1, 0, 0, 180).normalize(),
            side_edge = bottom_edge.copy().rotate(0, 0, 1, 90).normalize();
    static WireRenderingCube edge_map[] = {
            bottom_edge.rotate(0, 1, 0, 90 * 2).normalize(),
            bottom_edge.rotate(0, 1, 0, 90 * 1).normalize(),
            bottom_edge.rotate(0, 1, 0, 90 * 0).normalize(),
            bottom_edge.rotate(0, 1, 0, 90 * 3).normalize(),
            top_edge.rotate(0, 1, 0, 90 * 0).normalize(),
            top_edge.rotate(0, 1, 0, 90 * 3).normalize(),
            top_edge.rotate(0, 1, 0, 90 * 2).normalize(),
            top_edge.rotate(0, 1, 0, 90 * 1).normalize(),
            side_edge.rotate(0, 1, 0, 90 * 1).normalize(),
            side_edge.rotate(0, 1, 0, 90 * 0).normalize(),
            side_edge.rotate(0, 1, 0, 90 * 3).normalize(),
            side_edge.rotate(0, 1, 0, 90 * 2).normalize(),
    };

    public Iterable<WireRenderingCube> getParts() {
        ArrayList<WireRenderingCube> ret = new ArrayList(20);
        if (center_core) {
            ret.add(cube(new VectorUV(w, w, w), null));
        }
        for (int face_index = 0; face_index < 6; face_index++) {
            if ((faces & (1 << face_index)) == 0) {
                continue;
            }
            ret.add(face_map[face_index].copy());
        }
        for (int edge_index = 0; edge_index < 12; edge_index++) {
            if ((edges & (1 << edge_index)) == 0) {
                continue;
            }
            ret.add(edge_map[edge_index].copy());
        }
        return ret;
    }

    static public Iterable<WireRenderingCube> getInventoryParts() {
        ArrayList<WireRenderingCube> ret = new ArrayList(2);
        ret.add(face_map[0].copy());
        ret.add(edge_map[0].copy());
        ret.add(edge_map[1].copy());
        //ret.add(edge_map[2].copy());
        ret.add(edge_map[3].copy());
        ret.add(face_map[2].copy());
        return ret;
    }

    private void getExtremes(WireRenderingCube cube, VectorUV min, VectorUV max) {
        for (int face = 0; face < 6; face++) {
            for (VectorUV v : cube.faceVerts(face)) {
                min.x = Math.min(v.x, min.x);
                min.y = Math.min(v.y, min.y);
                min.z = Math.min(v.z, min.z);
                max.x = Math.max(v.x, max.x);
                max.y = Math.max(v.y, max.y);
                max.z = Math.max(v.z, max.z);
            }
        }
    }

    public void setBlockBounds(Block block) {
        VectorUV min = null, max = null;
        boolean first = true;
        for (WireRenderingCube part : getParts()) {
            if (first) {
                first = false;
                min = max = part.faceVerts(0)[2];
                max = part.faceVerts(0)[1];
            }
            getExtremes(part, min, max);
        }
        if (min == null || max == null) {
            return;
        }
        min = min.add(8, 8, 8);
        max = max.add(8, 8, 8);
        min.scale(1F / 16F);
        max.scale(1F / 16F);
        float d = 0;
        block.setBlockBounds((float)(min.x + d), (float)(min.y + d), (float)(min.z + d), (float)(max.x + d), (float)(max.y + d), (float)(max.z + d));
    }

    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
        for (WireRenderingCube part : getParts()) {
            part.toBlockBounds(Core.registry.resource_block);
            MovingObjectPosition ret = Core.registry.resource_block.collisionRayTrace(w, x, y, z, startVec, endVec);
            if (ret != null) {
                Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
                return ret;
            }
        }
        Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
        return null;
    }

    public void conductorRestrict() {
        faces = 0;
        center_core = false;
        //		cleanup();
    }
}
