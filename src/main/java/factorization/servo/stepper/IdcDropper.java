package factorization.servo.stepper;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.fzds.TransferLib;
import net.minecraftforge.common.util.ForgeDirection;

public class IdcDropper {
    final ForgeDirection ax, ay, az;
    final Coord src, dst;
    final DeltaCoord range;
    final boolean breakSrcElseDest;

    public IdcDropper(ForgeDirection up, ForgeDirection south, ForgeDirection east, Coord src, Coord dst, DeltaCoord range, boolean breakSrcElseDest) {
        this.ax = east;
        this.ay = up;
        this.az = south;
        this.src = src;
        this.dst = dst;
        this.range = range;
        this.breakSrcElseDest = breakSrcElseDest;
        if (range.isSubmissive()) throw new IllegalArgumentException("range must be positive");
    }

    static void add(Coord ret, Coord src, ForgeDirection a1, int n1, ForgeDirection a2, int n2, ForgeDirection a3, int n3) {
        ret.x = src.x + a1.offsetX * n1 + a2.offsetX * n2 + a3.offsetX * n3;
        ret.y = src.y + a1.offsetY * n1 + a2.offsetY * n2 + a3.offsetY * n3;
        ret.z = src.z + a1.offsetZ * n1 + a2.offsetZ * n2 + a3.offsetZ * n3;
    }

    int fails = 0;
    public int drop(boolean simulated) {
        Coord s = src.copy();
        Coord d = dst.copy();
        for (int dx = 0; dx < range.x; dx++) {
            for (int dy = 0; dy < range.y; dy++) {
                for (int dz = 0; dz < range.z; dz++) {
                    add(s, src, ax, dx, ay, dy, az, dz);
                    add(d, dst, ax, dx, ay, dy, az, dz);
                    move(s, d, simulated);
                }
            }
        }
        return fails;
    }

    private void move(Coord s, Coord d, boolean simulated) {
        if (simulated) {
            if (!d.isReplacable() && !d.isAir()) {
                fails += d.isSolid() ? 10 : 1;
            }
            return;
        }

        if (d.isReplacable()) {
            TransferLib.move(s, d, true, true);
            return;
        }
        // Snow or air we can break perfectly reasonably.
        // Solid blocks is less reasonable, but we've no choice in the matter.
        // If we're breaking something that isn't air, we're gonna need to check for dropped snow or cobble or whatever.
        if (breakSrcElseDest) {
            if (fails == 0 && !s.isAir()) {
                fails++;
            }
            s.breakBlock();
        } else {
            d.breakBlock();
        }
    }
}
