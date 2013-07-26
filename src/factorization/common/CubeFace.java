package factorization.common;

import factorization.api.DeltaCoord;

class CubeFace {
    private static final int side_reverser[] = { 1, 0, 3, 2, 5, 4 };
    int side;

    public CubeFace(int side) {
        this.side = side;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CubeFace) {
            return ((CubeFace) (obj)).side == side;
        }
        return false;
    }

    static int oppositeSide(int side) {
        return side_reverser[side];
    }

    CubeFace opposite() {
        return new CubeFace(side_reverser[side]);
    }

    static private final int vecMap[][] = {
            { 0, -1, 0 },
            { 0, +1, 0 },
            { 0, 0, -1 },
            { 0, 0, +1 },
            { -1, 0, 0 },
            { +1, 0, 0 }
    };
    DeltaCoord toVector() {
        int a[] = vecMap[this.side];
        return new DeltaCoord(a[0], a[1], a[2]);
    }

    static CubeFace fromVector(DeltaCoord d) {
        for (int side = 0; side < vecMap.length; side++) {
            if (d.x == vecMap[side][0] && d.y == vecMap[side][1] && d.z == vecMap[side][2]) {
                return new CubeFace(side);
            }
        }
        return null;
    }

    long getFaceFlag() {
        return 1 << this.side;
    }
    
    private static long e(int... args) {
        long r = 0;
        for (int a : args) {
            r |= (1 << a);
        }
        return r;
    }
    
    private static final long edgeMap[] = {
            e(0, 1, 2, 3), //-y
            e(4, 5, 6, 7), //+y
            e(4, 8, 0, 11), //-z
            e(6, 9, 2, 10), //+z
            e(7, 10, 3, 11), //-x
            e(5, 8, 1, 9) //+x
    };

    long getEdgeFlags() {
        return edgeMap[side];
    }

    static long getEdgeFlags(int side) {
        return edgeMap[side];
    }

    private static final long edge2face[] = new long[12];
    static {
        for (int edge = 0; edge < 12; edge++) {
            long edge_flag = 1 << edge;
            for (int side = 0; side < 6; side++) {
                if ((edgeMap[side] & edge_flag) != 0) {
                    edge2face[edge] |= 1 << side;
                }
            }
        }
    }

    static long getFacesTouchingEdge(int edge) {
        return edge2face[edge];
    }
}
