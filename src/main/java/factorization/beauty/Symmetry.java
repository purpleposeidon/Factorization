package factorization.beauty;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.util.SpaceUtil;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;

public class Symmetry {
    final Coord center;
    final int max_radius;
    EnumFacing normal, right, up;

    public int score = 0, asymetry = 0;
    public int max_score;
    public double measured_radius = 1;

    public Symmetry(Coord center, int max_radius, EnumFacing normal) {
        this.center = center;
        this.max_radius = max_radius;
        this.normal = normal;
        score += scoreBlock(center);
        int normAxis = SpaceUtil.getAxis(normal);
        int upAxis = -1;
        for (EnumFacing dir : EnumFacing.VALUES) {
            final int axis = SpaceUtil.getAxis(dir);
            if (upAxis == -1 && axis != normAxis) {
                upAxis = axis;
                up = dir;
            } else if (axis != normAxis && axis != upAxis) {
                right = dir;
                break;
            }
        }
        this.max_score = (max_radius * max_radius * 4) * 1;
        assert normal != null && right != null && up != null;
    }

    public void calculate() {
        // The region is split into 4 quadrants, and rotational symmetry is checked in each point simultaneously.
        // The iteration is shaped like:
        // for each point moving right { for each point moving up }
        EnumFacing[] rights = new EnumFacing[4];
        EnumFacing[] ups = new EnumFacing[4];
        DeltaCoord[] dRight = new DeltaCoord[4];
        DeltaCoord[] dUp = new DeltaCoord[4];
        EnumFacing _R = right, _U = up;
        for (int i = 0; i < 4; i++) {
            rights[i] = _R;
            ups[i] = _U;
            _R = _R.getRotation(normal);
            _U = _U.getRotation(normal);
            dRight[i] = new DeltaCoord();
        }

        for (int r = 1; r <= max_radius; r++) {
            move(dRight, rights);
            for (int i = 0; i < 4; i++) {
                dUp[i] = new DeltaCoord(dRight[i]);
            }
            for (int y = 0; y <= max_radius; y++) {
                Block found = null;
                boolean all_match = true;
                int spoke_score = 0;
                for (int i = 0; i < 4; i++) {
                    final Coord spoke = center.add(dUp[i]);
                    Block peeked = spoke.getBlock();
                    if (spoke.isSolid()) {
                        measured_radius = Math.max(measured_radius, dUp[i].magnitude());
                    }
                    if (found == null) {
                        found = peeked;
                        spoke_score = scoreBlock(spoke);
                    } else if (peeked != found) {
                        all_match = false;
                        break;
                    }
                }
                if (all_match) {
                    score += spoke_score;
                } else {
                    asymetry++;
                }
                move(dUp, ups);
            }
        }
    }

    void move(DeltaCoord[] deltas, EnumFacing[] dirs) {
        for (int i = 0; i < 4; i++) {
            deltas[i].move(dirs[i]);
        }
    }

    int scoreBlock(Coord at) {
        Block block = at.getBlock();
        Material mat = block.getMaterial();
        if (mat == Material.cloth || mat == Material.wood || mat == Material.carpet || mat == Material.iron
                || mat == Material.piston || mat == Material.clay || mat == Material.glass || mat == Material.web) {
            return 1;
        }
        if (block instanceof BlockLog || block instanceof BlockCarpet || block instanceof BlockFence || block instanceof BlockStairs) {
            return 1;
        }
        return 0;
    }
}
