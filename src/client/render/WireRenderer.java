package factorization.client.render;

import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.RenderingCube;
import factorization.common.TileEntityWire;
import factorization.common.RenderingCube.Vector;

public class WireRenderer {
    int wire_texture = 34;
    Vector offset = new Vector(0, -6, 0);
    RenderingCube wire_base = new RenderingCube(wire_texture, new Vector(4, 2, 4), offset);
    RenderingCube ext_north = new RenderingCube(wire_texture, new Vector(4, 2, 2), offset.add(0, 0, 6)),
            ext_south = new RenderingCube(wire_texture, new Vector(4, 2, 2), offset.add(0, 0, -6)),
            ext_east = new RenderingCube(wire_texture, new Vector(2, 2, 4), offset.add(-6, 0, 0)),
            ext_west = new RenderingCube(wire_texture, new Vector(2, 2, 4), offset.add(6, 0, 0));
    RenderingCube cube_draw_map[][] = new RenderingCube[][] {
            { ext_east, ext_west, ext_south, ext_north }, //0 +
            { ext_east, ext_west, ext_north, ext_south }, //1 +
            { ext_east, ext_west, ext_north, ext_south }, //2
            { ext_east, ext_west, ext_south, ext_north }, //3
            { ext_south, ext_north, ext_west, ext_east }, //4+
            { ext_south, ext_north, ext_east, ext_west }, //5 +
    };
    RenderingCube interface_north = new RenderingCube(wire_texture, new Vector(4, 4, 2), offset.add(0, 6, 6)),
            interface_south = new RenderingCube(wire_texture, new Vector(4, 4, 2), offset.add(0, 6, -6)),
            interface_east = new RenderingCube(wire_texture, new Vector(2, 4, 4), offset.add(-6, 6, 0)),
            interface_west = new RenderingCube(wire_texture, new Vector(2, 4, 4), offset.add(6, 6, 0));
    RenderingCube foreign_interface_draw_map[][] = new RenderingCube[][] {
            { interface_east, interface_west, interface_south, interface_north }, //0 +
            { interface_east, interface_west, interface_north, interface_south }, //1 +
            { interface_east, interface_west, interface_north, interface_south }, //2
            { interface_east, interface_west, interface_south, interface_north }, //3
            { interface_south, interface_north, interface_west, interface_east }, //4+
            { interface_south, interface_north, interface_east, interface_west }, //5 +
    };
    //RenderingCube reach = new RenderingCube(wire_texture, new Vector(4, 6, 4), offset.add(0, 20, 0));
    RenderingCube reach_north = new RenderingCube(wire_texture, new Vector(4, 4, 6), new Vector(0, 0, +14)),
            reach_south = new RenderingCube(wire_texture, new Vector(4, 4, 6), new Vector(0, 0, -14)),
            reach_east = new RenderingCube(wire_texture, new Vector(6, 4, 4), new Vector(-14, 0, 0)),
            reach_west = new RenderingCube(wire_texture, new Vector(6, 4, 4), new Vector(+14, 0, 0));
    int side_to_reach_for[][] = new int[][] {
            { 4, 5, 2, 3 }, //0 y-
            { 5, 4, 3, 2 }, //1 y+
            { 4, 5, 0, 1 }, //2 z-
            { 4, 5, 0, 1 }, //3 z+
            { 2, 3, 0, 1 }, //4 x-
            { 2, 3, 0, 1 }, //5 x+
    };
    RenderingCube reacher_draw_map[][] = new RenderingCube[][] {
            { reach_east, reach_west, reach_south, reach_north },
            { reach_east, reach_west, reach_south, reach_north },
            { reach_east, reach_west, reach_south, reach_north },
            { reach_east, reach_west, reach_south, reach_north },
            { reach_south, reach_north, reach_west, reach_east },
            { reach_south, reach_north, reach_east, reach_west }
    };

    void renderWireWorld(RenderBlocks rb, Coord me) {
        Tessellator tes = Tessellator.instance;

        tes.setBrightness(me.getBlock().getMixedBrightnessForBlock(me.w, me.x, me.y, me.z));
        TileEntityWire my_te = me.getTE(TileEntityWire.class);
        int plane_side = 0;
        if (my_te != null) {
            plane_side = my_te.supporting_side;
        }

        FactorizationRender.initRenderCube();

        int neighbor_count = 0;
        int neighbor_index = -1;

        for (Coord n : me.getNeighborsInPlane(plane_side)) {
            neighbor_index++;
            TileEntityWire neighbor = n.getTE(TileEntityWire.class);
            if (neighbor == null) {
                if (n.getTE(IChargeConductor.class) == null) {
                    continue;
                }
            } else {
                if (neighbor.supporting_side != plane_side && !isCorner(n)) {
                    //big connection from neighbor to us
                    if (neighbor.supporting_side == side_to_reach_for[plane_side][neighbor_index]) {
                        //intrude into territory
                        renderCube(reacher_draw_map[plane_side][neighbor_index], plane_side);
                    }
                    renderCube(foreign_interface_draw_map[plane_side][neighbor_index], plane_side);
                }
            }
            neighbor_count++;
            renderCube(cube_draw_map[plane_side][neighbor_index], plane_side);
        }
        int corner_count = 0;
        for (Coord n : me.getNeighborsOutOfPlane(plane_side)) {
            if (n.getTE(IChargeConductor.class) == null) {
                continue;
            }
            corner_count++;
            neighbor_count++;
        }
        if (neighbor_count != 2 || !isCorner(me)) {
            renderCube(wire_base, plane_side);
        }
    }

    void renderCube(RenderingCube cube, int plane_side) {
        int rx[] = { 0, 1, 1, 1, 0, 0 };
        int rz[] = { 0, 0, 0, 0, 1, 1 };
        int rtheta[] = { 0, 180, 90, -90, -90, 90 };
        cube.rotate(rx[plane_side], 0, rz[plane_side], rtheta[plane_side]);
        FactorizationRender.renderCube(cube);
    }

    boolean isCorner(Coord here) {
        TileEntityWire me = here.getTE(TileEntityWire.class);
        if (me == null) {
            return false;
        }
        int wired_neighbors = 0;
        TileEntityWire out_of_plane_neighbor = null;
        int neighbor_index = -1, out_of_plane_neighbor_index = -1;
        for (Coord n : here.getNeighborsAdjacent()) {
            TileEntityWire w = n.getTE(TileEntityWire.class);
            neighbor_index++;
            if (w == null) {
                continue;
            }
            wired_neighbors++;
            if (wired_neighbors > 2) {
                return false;
            }
            if (w.supporting_side != me.supporting_side) {
                out_of_plane_neighbor = w;
                out_of_plane_neighbor_index = neighbor_index;
            }
        }
        if (wired_neighbors != 2 || out_of_plane_neighbor == null) {
            return false;
        }
        //we have two neighbors, one of which is a not from our plane.
        int corner_map[][] = {
                { 0, 0, 0, 0, 0, 0 },
                { 3, 3, 3, 3, 3, 3 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
        };
        return corner_map[me.supporting_side][out_of_plane_neighbor_index] == out_of_plane_neighbor.supporting_side;
    }
}
