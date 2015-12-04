package factorization.wrath;

import factorization.api.datahelpers.DataHelper;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;

public class TileEntityWrathLamp extends TileEntityCommon {
    static final int radius = 6;
    static final int radiusSq = radius * radius;
    static final int diameter = radius * 2;
    static final int maxDepth = 24; //XXX TODO
    private short beamDepths[] = new short[(diameter + 1) * (diameter + 1)];
    private Updater updater = new Idler();
    static boolean isUpdating = false; // TODO: Bad for threads.

    static int update_count;
    static final int update_limit = 512;
    static PriorityQueue<LampCoord> airToUpdate = new PriorityQueue<LampCoord>(1024);

    private static class LampCoord implements Comparable<LampCoord> {
        final World w;
        final BlockPos pos;

        LampCoord(World w, BlockPos pos) {
            this.w = w;
            this.pos = pos;
        }

        void check() {
            doAirCheck(w, pos);
        }

        @Override
        public int compareTo(LampCoord o) {
            return Integer.compare(o.pos.getY(), pos.getY());
        }
    }

    public static void handleAirUpdates() {
        update_count = 0;
        while (update_count < update_limit && airToUpdate.size() > 0) {
            airToUpdate.remove().check();
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        updater = new InitialBuild();
    }
    
    static final int NOTIFY_NEIGHBORS = factorization.api.Coord.NOTIFY_NEIGHBORS;
    static final int UPDATE_CLIENT = factorization.api.Coord.UPDATE;
    static boolean spammed_console = false;

    static void doAirCheck(World w, BlockPos pos) {
        if (w.isRemote) {
            return;
        }
        if (update_count > update_limit) {
            if (airToUpdate.size() > 1024*8) {
                if (!spammed_console) {
                    Core.logSevere("TileEntityWrathLamp.airToUpdate has %s entries!", airToUpdate.size());
                    spammed_console = true;
                }
                return;
            }
            airToUpdate.add(new LampCoord(w, pos));
            return;
        }
        TileEntityWrathLamp lamp = TileEntityWrathLamp.findLightAirParent(w, pos);
        if (lamp == null) {
            update_count += 1;
            w.setBlockToAir(pos);
        } else {
            lamp.activate(pos.getY());
        }
    }

    private int getDepthIndex(int x, int z) {
        int dx = pos.getX() - x, dz = pos.getZ() - z;
        dx += radius;
        dz += radius;
        if (dx < 0 || dx > diameter) {
            throw new IndexOutOfBoundsException("x = " + dx);
        }
        if (dz < 0 || dz > diameter) {
            throw new IndexOutOfBoundsException("z = " + dz);
        }
        return dx + dz * diameter;
    }

    static final int deltas[] = new int[] { 0, -16, +16 };

    static HashSet<Chunk> toVisit = new HashSet<Chunk>(9 * 5);

    static TileEntityWrathLamp findLightAirParent(World world, BlockPos pos) {
        //NOTE: This could be optimized. Probably not really worth it tho.
        toVisit.clear();
        for (int dcx : deltas) {
            for (int dcz : deltas) {
                BlockPos dpos = pos.add(dcx, 0, dcz);
                if (!world.isBlockLoaded(dpos)) continue;
                toVisit.add(world.getChunkFromBlockCoords(dpos));
            }
        }
        for (Chunk chunk : toVisit) {
            for (Object o : chunk.chunkTileEntityMap.values()) {
                if (!(o instanceof TileEntityWrathLamp)) {
                    continue;
                }
                TileEntityWrathLamp lamp = (TileEntityWrathLamp) o;
                if (lamp.lightsBlock(x, y, z)) {
                    // NOTE: It's possible for two lamps to overlap eachother...
                    return lamp;
                }
            }
        }
        return null;
    }

    private boolean inArea(int x, int y, int z) {
        if (y > pos.getY()) {
            return false;
        }
        if (y < pos.getY() - maxDepth) {
            return false;
        }
        return pos.getX() - radius <= x && x <= pos.getX() + radius && pos.getZ() - radius <= z && z <= pos.getZ() + radius;
    }

    private boolean lightsBlock(int x, int y, int z) {
        if (!inArea(x, y, z)) {
            return false;
        }
        int depth = beamDepths[getDepthIndex(x, z)];
        return y >= depth && depth != -1;
    }

    @Override
    public void updateEntity() {
        Core.profileStart("WrathLamp");
        isUpdating = true;
        this.updater = this.updater.update();
        isUpdating = false;
        Core.profileEnd();
    }

    private static ThreadLocal<Boolean> invalidating = new ThreadLocal<Boolean>();
    @Override
    public void invalidate() {
        super.invalidate();
        if (invalidating.get() != null) return;
        invalidating.set(Boolean.TRUE);
        try {
            Core.profileStart("WrathLamp");
            for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
                for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                    Block id = worldObj.getBlock(x, pos.getY(), z);
                    if (id == Core.registry.lightair_block) {
                        if (worldObj.isRemote) {
                            worldObj.setBlockToAir(x, pos.getY(), z);
                        } else {
                            worldObj.setBlock(x, pos.getY(), z, Blocks.air);
                        }
                    }
                }
            }
            Core.profileEnd();
            if (worldObj.isRemote) {
                RelightTask task = new RelightTask(worldObj);
                task.setPosition(pos.getX(), pos.getY(), pos.getZ());
                worldObj.spawnEntityInWorld(task);
            }
        } finally {
            invalidating.remove();
        }
    }

    double dist(int x, int y, int z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    boolean eq(double a, double b) {
        return Math.abs(a - b) < 0.9;
    }

    float div(int a, int b) {
        if (b == 0) {
            return Math.signum(a) * 0xFFF;
        }
        return a / b;
    }

    boolean myTrace(int x, int z) {
        int dx = x - pos.getX(), dz = z - pos.getZ();
        float idealm = div(dz, dx);

        float old_dist = Float.MAX_VALUE;
        while (true) {
            if (x == pos.getX() && z == pos.getZ()) {
                return true;
            }
            Block id = worldObj.getBlock(x, pos.getY(), z);
            if (id != null && id.isOpaqueCube() && id.getLightOpacity() != 0) {
                return false;
            }
            dx = x - pos.getX();
            dz = z - pos.getZ();
            float m = div(dz, dx);
            int addx = (int) -Math.signum(dx), addz = (int) -Math.signum(dz);
            if (addx == 0 && addz == 0) {
                return true;
            }
            if (addx == 0) {
                z += addz;
                continue;
            }
            if (addz == 0) {
                x += addx;
                continue;
            }
            float m_x = div(dz, dx + addx);
            float m_z = div(dz + addz, dx);
            if (Math.abs(idealm - m_x) <= Math.abs(idealm - m_z)) {
                x += addx;
            }
            else {
                z += addz;
            }
        }

    }

    boolean clearTo(int x, int y, int z) {
        return myTrace(x, z);
    }

    @Override
    public void neighborChanged() {
        activate(pos.getY());
    }

    void activate(int at_height) {
        updater = updater.getActive(at_height);
    }

    abstract class Updater {
        int start_height = -100;

        abstract Updater update();

        abstract Updater getActive(int start_height);
    }

    class InitialBuild extends Updater {
        int height = -100;
        int start_delay = 20;
        Updater next_updater = new Idler();

        @Override
        public Updater update() {
            if (start_delay >= 0) {
                start_delay--;
                return this;
            }
            if (height == -100) {
                if (start_height == -100) {
                    height = pos.getY();
                }
                else {
                    height = start_height;
                }
            }
            else {
                height -= 1;
            }
            if (height < pos.getY() - maxDepth) {
                return next_updater;
            }
            if (height == pos.getY()) {
                //we are level with the lamp.
                //Set areas we can't reach to -1
                Arrays.fill(beamDepths, (short) 0);
                for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
                    for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                        if (!clearTo(x, pos.getY(), z)) {
                            beamDepths[getDepthIndex(x, z)] = -1;
                        }
                    }
                }
            }
            //NORELEASE: I've probably messed this up
            for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
                for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                    // If it is air, make it LightAir™
                    // If already lightair, carry on
                    // if a (solid?) block, stop
                    int index = getDepthIndex(x, z);
                    if (beamDepths[index] != 0) {
                        continue;
                    }
                    Block block = worldObj.getBlock(x, height, z);
                    Block belowBlock = worldObj.getBlock(x, height - 3, z);
                    if (belowBlock != Core.registry.lightair_block && height != pos.getY()) {
                        beamDepths[index] = (short) height;
                        continue;
                    }
                    /*if (block == 0 && worldObj.getBlock(x, height - 1, z) == Blocks.cobblestone_wall) {
                        block = -1;
                    }*/
                    if (worldObj.getBlock(x, height, z) == Blocks.air) {
                        //Nice work, Mojang. If we didn't do this the hard way, the client will lag very badly near chunks that are unloaded.
                        //XXX TODO FIXME: Seems a bit difficult. What's the right way to do this?
                        Chunk chunk = worldObj.getChunkFromBlockCoords(x, z);
                        chunk.func_150807_a(x & 15, height, z & 15, Core.registry.lightair_block, 0);
                        worldObj.markBlockForUpdate(x, height, z);
                    } else if (block == Core.registry.lightair_block) {
                    } else if (x == pos.getX() && height == pos.getY() && z == pos.getZ()) {
                        //this is ourself. Hi, self.
                        //Don't terminate the beamDepth early.
                    } else {
                        beamDepths[index] = (short) height;
                    }

                }
            }

            if (height == 0) {
                return next_updater;
            }
            else {
                return this;
            }
        }

        @Override
        Updater getActive(int start_height) {
            if (!(this.next_updater instanceof InitialBuild)) {
                this.next_updater = new InitialBuild();
            }
            start_height++;
            next_updater.start_height = Math.max(start_height, next_updater.start_height);
            return this;
        }
    }

    class Idler extends Updater {
        boolean couldUpdate(int dx, int dz) {
            return worldObj.isAirBlock(pos.add(dx, 0, dz));
        }

        @Override
        public Updater update() {
            if (couldUpdate(0, +1) || couldUpdate(0, -1) || couldUpdate(+1, 0) || couldUpdate(-1, 0)) {
                return new InitialBuild();
            }
            return this;
        }

        @Override
        Updater getActive(int start_height) {
            Updater r = new InitialBuild();
            r.start_height = Math.max(r.start_height, start_height);
            return r;
        }
    }

    public static class RelightTask extends Entity {
        int delay;

        public RelightTask(World par1World) {
            super(par1World);
        }

        @Override
        protected void entityInit() {
            delay = 20 * 4;
        }

        //No need to bother saving this.
        @Override
        protected void readEntityFromNBT(NBTTagCompound var1) {
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound var1) {
        }

        @Override
        public void onUpdate() {
            delay -= 1;
            if (delay == 0) {
                int r = radius + 15;
                for (int x = (int) (posX - r); x <= posX + r; x++) {
                    for (int z = (int) (posZ - r); z <= posZ + r; z++) {
                        for (int y = (int) posY; y >= posY - maxDepth; y--) {
                            worldObj.func_147451_t(x, y, z);
                        }
                    }
                }
                this.setDead();
            }
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LAMP;
    }

    @Override
    public void putData(DataHelper data) throws IOException {

    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Lamp;
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return false;
    }
}
