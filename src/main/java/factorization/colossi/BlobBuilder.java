package factorization.colossi;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.shared.Core;

public class BlobBuilder {
    static final byte AIR = 0, SUSTAINER = -1, WALL = -2, POISON = -3, REINFORCE = 2;
    
    final byte[] cells, tick_cache;
    
    final Coord start, end;
    final DeltaCoord size;
    final Random rand;
    
    public double min_neighborliness;
    public double birth_threshold;
    public double death_threshold;
    public double wall_healing;
    public double sustainer_healing;
    
    
    
    BlobBuilder(Random rand, Coord start, Coord end) {
        this.rand = rand;
        start = start.copy();
        end = end.copy();
        Coord.sort(start, end);
        this.start = start;
        this.end = end;
        size = end.difference(start);
        set_target = cells = new byte[size.x * size.y * size.z];
        tick_cache = new byte[size.x * size.y * size.z];
    }
    
    int index(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) return -1;
        if (x >= size.x || y >= size.y || z >= size.z) return -1;
        return x + y * size.x + z * size.x * size.y;
    }
    
    byte classify(Coord at) {
        if (at.isAir()) return AIR;
        Block b = at.getBlock();
        if (b == Core.registry.colossal_block) {
            int md = at.getMd();
            if (md == ColossalBlock.MD_ARM || md == ColossalBlock.MD_LEG || md == ColossalBlock.MD_BODY || md == ColossalBlock.MD_BODY_CRACKED) {
                return SUSTAINER;
            }
            if (md == ColossalBlock.MD_MASK) {
                return WALL;
            }
            if (md == ColossalBlock.MD_CORE || md == ColossalBlock.MD_EYE) {
                return POISON;
            }
        } else {
            at.setAir(); // NORELEASE;
            return AIR;
        }
        return WALL;
    }
    
    public void populateCellsFromWorld() {
        Coord at = start.copy();
        for (int x = start.x; x < end.x; x++) {
            at.x = x;
            for (int y = start.y; y < end.y; y++) {
                at.y = y;
                for (int z = start.z; z < end.z; z++) {
                    at.z = z;
                    int index = index(x - start.x, y - start.y, z - start.z);
                    if (index == -1) continue;
                    cells[index] = classify(at);
                }
            }
        }
    }
    
    public void sprinkleSeeds(double ratio) {
        int count = (int) (size.x * size.y * size.z * ratio);
        for (int i = 0; i < count; i++) {
            int index = rand.nextInt(cells.length);
            if (cells[index] == 0) {
                cells[index] = 1;
            }
        }
    }
    
    ForgeDirection[] cardinals = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST };
    
    public void rainSeeds(double ratio, double chance_of_stick_per_slide) {
        int count = (int) (size.x * size.z * ratio);
        for (int i = 0; i < count; i++) {
            int x = rand.nextInt(size.x);
            int z = rand.nextInt(size.z);
            for (int y = size.y - 1; y >= 0; y--) {
                byte below = get(x, y - 1, z);
                if (below == AIR || below == POISON || below == WALL) {
                    for (ForgeDirection fd : cardinals) {
                        byte there = get(x + fd.offsetX, y, z + fd.offsetZ);
                        if (there == SUSTAINER || there > 0) {
                            if (rand.nextDouble() < chance_of_stick_per_slide) {
                                set(x + fd.offsetX, y, z + fd.offsetZ, (byte) 1);
                            }
                        }
                    }
                } else if (below == SUSTAINER || below > 0) {
                    set(x, y, z, (byte) 1);
                    set(x, y + 1, z, (byte) 1);
                }
            }
        }
    }
    
    ForgeDirection[] supportive = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.DOWN };
    int get_support(int x, int y, int z) {
        int support = 0;
        for (ForgeDirection fd : supportive) {
            byte there = get(x + fd.offsetX, y, z + fd.offsetZ);
            if (there != AIR) support++;
        }
        return support;
    }
    
    public void upset(double ratio, int reloop, int min_support) {
        int count = (int) (size.x * size.z * ratio);
        for (int i = 0; i < count; i++) {
            int x = rand.nextInt(size.x);
            int z = rand.nextInt(size.z);
            relooping: for (int re = 0; re < reloop; re++) {
                for (int y = size.y - 1; y >= 0; y--) {
                    byte at = get(x, y, z);
                    if (at == AIR) continue;
                    if (at <= 0) continue;
                    int support = get_support(x, y, z);
                    if (support < min_support) {
                        set(x, y, z, AIR);
                        for (int fall = y - 1; y >= 0; y--) {
                            if (get_support(x, fall, z) >= min_support) {
                                set(x, y, z, at);
                                break;
                            }
                        }
                        continue relooping;
                    }
                }
                break relooping;
            }
        }
    }
    
    public void growthTick() {
        min_neighborliness = 3;
        birth_threshold = 35;
        death_threshold = 0;
        wall_healing = -1.0;
        sustainer_healing = 5.0;
        simulateTick();
    }
    
    public void pruneTick() {
        min_neighborliness = 3;
        birth_threshold = 35;
        death_threshold = 25;
        wall_healing = -1.0;
        sustainer_healing = 2.0;
        simulateTick();
    }
    
    public void bubble() {
        tickStart();
        for (int x = 0; x <= size.x; x++) {
            for (int y = 0; y <= size.y; y++) {
                for (int z = 0; z <= size.z; z++) {
                    if (get(x, y, z) > 0) {
                        for (ForgeDirection fd : cardinals) {
                            x += fd.offsetX;
                            y += fd.offsetY;
                            z += fd.offsetZ;
                            if (get(x, y, z) == AIR) {
                                set(x, y, z, (byte) 1);
                            }
                        }
                    }
                }
            }
        }
        tickEnd();
    }
    
    public void smooth(int min) {
        tickStart();
        for (int x = 0; x <= size.x; x++) {
            for (int y = 0; y <= size.y; y++) {
                for (int z = 0; z <= size.z; z++) {
                    if (get(x, y, z) > 0) {
                        int near = 0;
                        for (ForgeDirection fd : cardinals) {
                            x += fd.offsetX;
                            y += fd.offsetY;
                            z += fd.offsetZ;
                            byte here = get(x, y, z);
                            if (here > 0 || here == REINFORCE) {
                                near++;
                            }
                        }
                        if (near < min) {
                            set(x, y, z, AIR);
                        }
                    }
                }
            }
        }
        tickEnd();
    }
    
    public void life(int dieBelow, int birthAbove) {
        tickStart();
        for (int x = 0; x <= size.x; x++) {
            for (int y = 0; y <= size.y; y++) {
                for (int z = 0; z <= size.z; z++) {
                    byte base = get(x, y, z);
                    if (base > 0 || base == AIR) {
                        int near = 0;
                        for (ForgeDirection fd : cardinals) {
                            int ax = x + fd.offsetX;
                            int ay = y + fd.offsetY;
                            int az = z + fd.offsetZ;
                            byte here = get(ax, ay, az);
                            if (here > 0) {
                                near++;
                            }
                            if (here == SUSTAINER) {
                                dieBelow--;
                            }
                        }
                        if (base > 0 && near < dieBelow) {
                            set(x, y, z, AIR);
                        } else if (base == AIR && near > birthAbove) {
                            set(x, y, z, (byte) 1);
                        }
                    }
                }
            }
        }
        tickEnd();
    }
    
    public void reinforce(double chance) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] > 0 && rand.nextDouble() < chance) {
                cells[i] = REINFORCE;
            }
        }
    }
    
    public void removeReinforcements() {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == REINFORCE) {
                cells[i] = 1;
            }
        }
    }
    
    public void flood() {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == AIR) {
                cells[i] = 1;
            }
        }
    }
    
    public void saveCellsToWorld(Block stone) {
        Coord at = start.copy();
        for (int x = start.x; x <= end.x; x++) {
            at.x = x;
            for (int y = start.y; y <= end.y; y++) {
                at.y = y;
                for (int z = start.z; z <= end.z; z++) {
                    at.z = z;
                    int index = index(x - start.x, y - start.y, z - start.z);
                    if (index == -1) continue;
                    byte cell = cells[index];
                    if (at.isAir()) {
                        if (cell > 0) at.setIdMd(stone, 0, false);
                        if (cell == REINFORCE) at.setIdMd(Blocks.stonebrick, 0, false);
                    }
                }
            }
        }
    }
    
    byte[] set_target;
    
    private void tickStart() {
        set_target = tick_cache;
        for (int i = 0; i < cells.length; i++) {
            set_target[i] = cells[i];
        }
    }
    
    private void tickEnd() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = set_target[i];
        }
        set_target = cells;
    }
    
    private void simulateTick() {
        tickStart();
        for (int x = 0; x <= size.x; x++) {
            for (int y = 0; y <= size.y; y++) {
                for (int z = 0; z <= size.z; z++) {
                    tickCell(x, y, z);
                }
            }
        }
        tickEnd();
    }
    
    byte get(int x, int y, int z) {
        int i = index(x, y, z);
        if (i == -1) return WALL;
        return cells[i];
    }
    
    void set(int x, int y, int z, byte new_val) {
        int i = index(x, y, z);
        if (i == -1) return;
        set_target[i] = new_val;
    }

    int dGauss() {
        int r = (int) Math.round(rand.nextGaussian()*3);
        if (r > 4) return 4;
        if (r < -4) return -4;
        return r;
    }
    
    private void tickCell(int x, int y, int z) {
        byte here = get(x, y, z);
        switch (here) {
        case WALL: return;
        case SUSTAINER: return;
        case POISON: return;
        case AIR:
            if (calculateLivliness(x, y, z) > birth_threshold) {
                set(x, y, z, (byte) 1);
            }
            break;
        case REINFORCE: return;
        default:
            if (calculateLivliness(x, y, z) < death_threshold) {
                set(x, y, z, (byte) 0);
            }
            break;
        }
    }
    
    double calculateLivliness(int x, int y, int z) {
        double life = 0;
        double neighborliness = 0;
        byte R = 3;
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                for (int dz = -R; dz <= R; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    int nx = x + dx;
                    int ny = y + dy;
                    int nz = z + dz;
                    double change = 0;
                    byte val = get(nx, ny, nz);
                    switch (val) {
                    case AIR: break;
                    case POISON:
                        change = sustainer_healing * -50;
                        break;
                    case WALL:
                        change = wall_healing;
                        break;
                    case SUSTAINER:
                        change = sustainer_healing;
                        break;
                    default:
                        neighborliness += 2 / distance;
                        break;
                    }
                    if (change != 0) {
                        change /= distance;
                    }
                    life += change;
                }
            }
        }
        if (neighborliness < min_neighborliness) {
            return -1;
        }
        return life + (get(x, y, z) > 0 ? 5 : 0);
    }
    
}
