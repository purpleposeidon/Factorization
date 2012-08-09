package factorization.common;

import java.util.ArrayList;

import net.minecraft.src.Block;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.Profiler;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IChargeConductor;
import factorization.common.RenderingCube.Vector;

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

        Profiler.startSection("factoryWire");
        calculate();
        Profiler.endSection();
    }

    void calculate() {
        int wire_neighbor_count = 0;
        for (DeltaCoord d : DeltaCoord.directNeighbors) {
            Coord nc = here.add(d);
            TileEntityWire n = nc.getTE(TileEntityWire.class);
            CubeFace delta_face = CubeFace.fromVector(d);
            if (n != null) {
                CubeFace neighbor_face = new CubeFace(n.supporting_side);
                addNeighbor(d, n, neighbor_face, delta_face);
                wire_neighbor_count++;
                continue;
            }
            IChargeConductor c = nc.getTE(IChargeConductor.class);
            if (c != null) {
                if (delta_face.equals(my_face)) {
                    //under us
                    faces |= my_face.getFaceFlag();
                } else if (delta_face.opposite().equals(my_face)) {
                    //above us
                    faces |= my_face.getFaceFlag() | delta_face.getFaceFlag();
                    center_core = true;
                } else {
                    //adjacent
                    //edges |= my_face.getEdgeFlags() & delta_face.getEdgeFlags();
                    //edges |= my_face.getEdgeFlags() & new CubeFace(0).getEdgeFlags();
                    edges |= new CubeFace(0).getEdgeFlags() & delta_face.getEdgeFlags();
                    //faces |= my_face.getFaceFlag();
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

        cleanup();
    }

    void cleanup() {
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

    public void conductorRestrict() {
        faces = 0;
        center_core = false;
//		cleanup();
    }

    void addNeighbor(DeltaCoord neighbor, TileEntityWire nte, CubeFace neighbor_face, CubeFace delta_face) {
        if (neighbor.equals(my_face_vector) || neighbor.reverse().equals(my_face_vector)) {
            center_core = true;
            faces |= neighbor_face.getFaceFlag() | my_face.getFaceFlag();
            return;
        }
        long edge_to_add = delta_face.getEdgeFlags() & my_face.getEdgeFlags();
        if (edge_to_add == 0) {
            //neighbor's support is far away
            edge_to_add = delta_face.getEdgeFlags() & neighbor_face.getEdgeFlags();
            if (Long.bitCount(edge_to_add) > 1) {
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
    
    static RenderingCube cube(Vector corner, Vector origin) {
        final int icon = 11; //34... was old. 11 is right, but... what?
        return new RenderingCube(icon, corner, origin);
    }

    final float h = 2, w = 4; //these are half of the actual size.
    RenderingCube base_face = cube(new Vector(w, h, w), new Vector(0, -8 + h, 0));
    RenderingCube base_face_side = base_face.copy().rotate(1, 0, 0, 90).normalize();
    RenderingCube face_map[] = {
            base_face.copy(),
            base_face.rotate(1, 0, 0, 180).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 0).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 2).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 1).normalize(),
            base_face_side.rotate(0, 1, 0, 90 * 3).normalize(),
    };
    RenderingCube bottom_edge = cube(new Vector(w, h, h), new Vector(0, -8 + h, 8 - h)),
            top_edge = bottom_edge.copy().rotate(1, 0, 0, 180).normalize(),
            side_edge = bottom_edge.copy().rotate(0, 0, 1, 90).normalize();
    RenderingCube edge_map[] = {
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

    public Iterable<RenderingCube> getParts() {
        ArrayList<RenderingCube> ret = new ArrayList(20);
        if (center_core) {
            ret.add(cube(new Vector(w, w, w), null));
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

    private void getExtremes(RenderingCube cube, Vector min, Vector max) {
        for (int face = 0; face < 6; face++) {
            for (Vector v : cube.faceVerts(face)) {
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
        Vector min = null, max = null;
        boolean first = true;
        for (RenderingCube part : getParts()) {
            if (first) {
                first = false;
                min = max = part.faceVerts(0)[2];
                max = part.faceVerts(0)[1];
            }
            getExtremes(part, min, max);
        }
        min = min.add(8, 8, 8);
        max = max.add(8, 8, 8);
        min.scale(1F/16F);
        max.scale(1F/16F);
        float d = 0;
        block.setBlockBounds(min.x + d, min.y + d, min.z + d, max.x + d, max.y + d, max.z + d);
    }

    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3D startVec,
            Vec3D endVec) {
        for (RenderingCube part : getParts()) {
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
}
