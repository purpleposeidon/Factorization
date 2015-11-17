package factorization.servo.stepper;

import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collection;
import java.util.HashSet;

public class GrabConnector {
    final Coord start;
    int avail;
    Block primary;
    final FastBag<Coord> frontier = new FastBag<Coord>();
    final HashSet<Coord> found = new HashSet<Coord>();

    public GrabConnector(Coord start, int maxSize) {
        this.start = start;
        avail = maxSize;
        primary = start.getBlock();
        if (start.isAir()) {
            throw new IllegalArgumentException("Don't grab air");
        }
        found.add(start);
        frontier.add(start);
    }

    Block[] natch = new Block[] {
            Blocks.stone,
            Blocks.sand,
            Blocks.dirt,
            Blocks.grass,
            Blocks.netherrack,
            Blocks.end_stone,
            Blocks.water,
            Blocks.flowing_water,
            Blocks.lava,
            Blocks.flowing_lava
    };

    Coord glueCheck(Coord at, ForgeDirection dir) {
        // Temporary. Counts as glued if it's the primary block, or two identical not-worldgen blocks
        Coord neighbor = at.add(dir);
        Block neighborBlock = neighbor.getBlock();
        if (neighborBlock == Core.registry.factory_block && at.getMd() == BlockClass.Wire.md) return null;
        if (neighborBlock == primary) return neighbor;
        for (Block n : natch) if (n == neighborBlock) return null;
        if (neighbor.isAir()) return null;
        if (neighborBlock == at.getBlock() && at.isSolid()) return neighbor;
        return null;
    }

    public Collection<Coord> fill() {
        while (!frontier.isEmpty()) {
            Coord at = frontier.removeAny();
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                Coord next = glueCheck(at, dir);
                if (next == null) continue;
                if (found.add(next)) {
                    frontier.add(next);
                    if (avail-- <= 0) break;
                }
            }
        }
        return found;
    }
}
