package factorization.beauty;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.util.SpaceUtil;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.util.ForgeDirection;

public class Symmetry {
    final Coord center;
    final int radius;
    ForgeDirection normal, right, up;

    public int score = 0, asymetry = 0;
    public int max_score;

    public Symmetry(Coord center, int radius, ForgeDirection normal) {
        this.center = center;
        this.radius = radius;
        this.normal = normal;
        score += scoreBlock(center);
        int normAxis = SpaceUtil.getAxis(normal);
        int upAxis = -1;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final int axis = SpaceUtil.getAxis(dir);
            if (upAxis == -1 && axis != normAxis) {
                upAxis = axis;
                up = dir;
            } else if (axis != normAxis && axis != upAxis) {
                right = dir;
                break;
            }
        }
        this.max_score = (radius * radius * 4) * 1;
        assert normal != null && right != null && up != null;
    }

    public void calculate() {
        // The region is split into 4 quadrants, and rotational symmetry is checked in each point simultaneously.
        // The iteration is shaped like:
        // for each point moving right { for each point moving up }
        ForgeDirection[] rights = new ForgeDirection[4];
        ForgeDirection[] ups = new ForgeDirection[4];
        DeltaCoord[] dRight = new DeltaCoord[4];
        DeltaCoord[] dUp = new DeltaCoord[4];
        ForgeDirection _R = right, _U = up;
        for (int i = 0; i < 4; i++) {
            rights[i] = _R;
            ups[i] = _U;
            _R = _R.getRotation(normal);
            _U = _U.getRotation(normal);
            dRight[i] = new DeltaCoord();
        }

        for (int r = 1; r <= radius; r++) {
            move(dRight, rights);
            for (int i = 0; i < 4; i++) {
                dUp[i] = new DeltaCoord(dRight[i]);
            }
            for (int y = 0; y <= radius; y++) {
                Block found = null;
                boolean all_match = true;
                int score = 0;
                for (int i = 0; i < 4; i++) {
                    final Coord spoke = center.add(dUp[i]);
                    Block peeked = spoke.getBlock();
                    if (found == null) {
                        found = peeked;
                        score = scoreBlock(spoke);
                    } else if (peeked != found) {
                        all_match = false;
                        break;
                    }
                }
                if (all_match) {
                    score += score;
                } else {
                    asymetry++;
                }
                move(dUp, ups);
            }
        }
    }

    void move(DeltaCoord[] deltas, ForgeDirection[] dirs) {
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
