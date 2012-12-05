package factorization.api;

import net.minecraftforge.common.ForgeDirection;

public class DeltaCoord {
    public int x, y, z;

    public DeltaCoord() {
        x = y = z = 0;
    }

    public DeltaCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public DeltaCoord add(DeltaCoord o) {
        return new DeltaCoord(x + o.x, y + o.y, z + o.z);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    @Override
    public String toString() {
        return "DeltaCoord(" + x + ", " + y + ", " + z + ")";
    }

    private static DeltaCoord d(int x, int y, int z) {
        return new DeltaCoord(x, y, z);
    }

    public static DeltaCoord directNeighbors[] = {
            d(+1, 0, 0),
            d(-1, 0, 0),
            d(0, -1, 0),
            d(0, +1, 0),
            d(0, 0, -1),
            d(0, 0, +1) };

    public double getAngleHorizontal() {
        return Math.atan2(z, -x);
    }
    
    public ForgeDirection getDirection() {
        for (ForgeDirection d : ForgeDirection.values()) {
            if (d.offsetX == x && d.offsetY == y && d.offsetZ == z) {
                return d;
            }
        }
        return ForgeDirection.UNKNOWN;
    }

    public int getFaceSide() {
        if (x == 0 && z == 0) {
            if (y == -1) {
                return 0;
            } else if (y == 1) {
                return 1;
            }
        } else if (y == 0 && x == 0) {
            if (z == -1) {
                return 2;
            } else if (z == 1) {
                return 3;
            }
        } else if (y == 0 && z == 0) {
            if (x == -1) {
                return 4;
            } else if (x == 1) {
                return 5;
            }
        }

        return -1;
    }

    public DeltaCoord reverse() {
        return new DeltaCoord(-x, -y, -z);
    }

    public boolean isSubmissive() {
        return x < 0 || y < 0 || z < 0;
    }

    public boolean equals(DeltaCoord o) {
        return x == o.x && y == o.y && z == o.z;
    }
    
    public void alignToAxis() {
        int ax = Math.abs(x);
        int ay = Math.abs(y);
        int az = Math.abs(z);
        if (ax >= ay && ax >= az) {
            x = (int) Math.signum(x);
            return;
        }
        if (ay >= ax && ay >= az) {
            y = (int) Math.signum(y);
            return;
        }
        if (az >= ay && az >= ax) {
            z = (int) Math.signum(z);
            return;
        }
        x = y = z = 0;
    }
    
    public int get(int id) {
        switch (id) {
        case 0: return x;
        case 1: return y;
        case 2: return z;
        default: throw new RuntimeException("not an dimension index");
        }
    }
    
    public void set(int id, int val) {
        switch (id) {
        case 0: x = val; break;
        case 1: y = val; break;
        case 2: z = val; break;
        default: throw new RuntimeException("not an dimension index");
        }
    }
    
    public void init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
