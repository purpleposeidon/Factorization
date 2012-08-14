package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import factorization.api.Coord;
import factorization.api.ICoord;

public class TileEntityWrathFire extends TileEntity implements ICoord {
    //constants
    final int max_generation = 45;
    final int max_age = 35;
    static Random rand = new Random();
    final static List<Integer> deltas = Arrays.asList(-1, 0, +1);

    //Information on what burns, and what it burns into
    public static class BlockMatch {
        int id, md;

        public BlockMatch(int id, int md) {
            this.id = id;
            this.md = md;
        }

        public BlockMatch(Block block) {
            this(block.blockID, -1);
        }

        public BlockMatch(Block block, int md) {
            this(block.blockID, md);
        }

        boolean matches(Coord c) {
            int i = c.getId();
            if (i != this.id) {
                return false;
            }
            if (this.md == -1) {
                return true;
            }
            return this.md == c.getMd();
        }

        boolean isType(int id, int md) {
            if (id != this.id) {
                return false;
            }
            if (this.md == -1) {
                return true;
            }
            return this.md == md;
        }

        void set(Coord c) {
            if (md == -1) {
                c.setId(this.id);
            }
            else {
                c.setIdMd(this.id, this.md);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BlockMatch) {
                BlockMatch m = (BlockMatch) obj;
                return this.id == m.id && this.md == m.md;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.id << 4 + this.md;
        }

        public String toString() {
            return this.id + ":" + this.md;
        }
    }

    public static HashMap<BlockMatch, BlockMatch> transforms = new HashMap();

    public static void burn(Object key, Object val) {
        if (key instanceof Block) {
            key = new BlockMatch((Block) key);
        }
        if (val instanceof Block) {
            val = new BlockMatch((Block) val);
        }
        transforms.put((BlockMatch) key, (BlockMatch) val);
    }

    static BlockMatch air = new BlockMatch(0, -1);
    static BlockMatch fire = new BlockMatch(Block.fire);
    static BlockMatch netherBrick = new BlockMatch(Block.netherBrick);
    static BlockMatch netherFence = new BlockMatch(Block.netherFence);
    static BlockMatch netherStair = new BlockMatch(Block.stairsNetherBrick);

    public static void burn(Object key) {
        burn(key, air);
    }

    public static void setupBurning() {
        //removals
        for (int i = 0; i < Block.blocksList.length; i++) {
            Block block = Block.blocksList[i];
            if (block == null) {
                continue;
            }
            Material m = block.blockMaterial;
            if (m == Material.wood
                    || m == Material.cloth
                    || m == Material.plants
                    || m == Material.leaves
                    || m == Material.vine
                    || m == Material.cactus
                    || m == Material.web) {
                burn(block);
            }
        }

        //TODO: Netherrack should be a special case...
        //And maybe netherbrick for that matter...? Brick decrease generation count, rack decreases age...?
        //burn(Block.netherrack);
        burn(Block.ice);
        burn(Block.netherBrick, Block.netherBrick);

        //reversibles
        burn(Block.sand, Block.glass);
        burn(Block.glass, Block.sand);
        burn(Block.grass, Block.dirt);
        burn(Block.obsidian, Block.lavaMoving); //undo with water bucket or Wand of Cooling
        burn(Block.cobblestone, Block.stone);
        burn(Block.stone, Block.cobblestone);
        burn(new BlockMatch(Block.stoneBrick, 0x0), new BlockMatch(Block.stoneBrick, 0x2)); //normal to cracked
        burn(new BlockMatch(Block.stoneBrick, 0x2), new BlockMatch(Block.stoneBrick, 0x0)); //cracked to normal

        //demossing
        burn(new BlockMatch(Block.stoneBrick, 0x1), new BlockMatch(Block.stoneBrick, 0x0)); //mossy to normal
        burn(Block.cobblestoneMossy, Block.cobblestone);
        burn(Block.blockClay, Block.brick);

        burn(Block.blockSteel, new BlockMatch(Core.registry.resource_block, ResourceType.DARKIRONBLOCK.md));
        burn(new BlockMatch(Core.registry.resource_block, ResourceType.SILVERBLOCK.md), new BlockMatch(Core.registry.resource_block, ResourceType.LEADBLOCK.md));

    }

    BlockMatch host = null;
    int age = 0, generation = 0;

    int random_time_offset = rand.nextInt();

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (host == null) {
            return;
        }
        tag.setInteger("host_id", host.id);
        tag.setInteger("host_md", host.md);
        tag.setInteger("age", age);
        tag.setInteger("generation", generation);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        host = new BlockMatch(tag.getInteger("host_id"), tag.getInteger("host_md"));
        age = tag.getInteger("age");
        generation = tag.getInteger("generation");
    }

    static TileEntityWrathFire getFire(World worldObj, int x, int y, int z) {
        TileEntity te = worldObj.getBlockTileEntity(x, y, z);
        if (te instanceof TileEntityWrathFire) {
            return (TileEntityWrathFire) te;
        }
        return null;
    }

    boolean blockSupports(Coord c) {
        return host.matches(c);
    }

    boolean isBlockSupported(Coord c) {
        for (Coord n : c.getNeighborsAdjacent()) {
            if (blockSupports(n)) {
                return true;
            }
        }
        return false;
    }

    void inherit(TileEntityWrathFire parent) {
        if (parent == null) {
            return;
        }
        host = parent.host;
        generation = Math.max(generation, parent.generation + 1);
    }

    boolean trySpawn(Coord c) {
        if (!Core.spread_wrathfire) {
            return false;
        }
        if (generation > max_generation) {
            return false;
        }
        int id = c.getId();
        if (id != 0 && !c.isAir()) {
            return false;
        }
        if (!isBlockSupported(c)) {
            return false;
        }
        if (c.is(Core.registry.lightair_block) && c.getMd() == BlockLightAir.fire_md) {
            return false;
        }
        //could put some randomness here?
        if (rand.nextInt(3) == 0) {
            return true;
        }
        c.setIdMd(Core.lightair_id, Core.registry.lightair_block.fire_md);
        TileEntityWrathFire fire = c.getTE(TileEntityWrathFire.class);
        if (fire != null) {
            fire.inherit(this);
            return true;
        }
        else {
            //what? Okay, forget it.
            c.setId(0);
            return false;
        }
    }
    
    @Override
    public Coord getCoord() {
        return new Coord(this.worldObj, this);
    }

    void die() {
        fire.set(getCoord());
    }

    public static int updateCount = 0;

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) {
            doUpdate();
        }
    }

    void doUpdate() {
        if ((worldObj.getWorldTime() + random_time_offset) % 25 != 0) {
            return;
        }
        if (rand.nextFloat() > .95) {
            return;
        }
        if (age > max_age) {
            die();
            return;
        }
        if (updateCount > 100) {
            if (rand.nextBoolean()) {
                return;
            }
        }
        updateCount += 1;
        BlockMatch burnTo = transforms.get(host);
        if (burnTo == null) {
            die();
            return;
        }
        Coord here = getCoord();

        //if (host.id == Block.netherBrick.blockID) {
        if (netherBrick.equals(host) || netherStair.equals(host) || netherFence.equals(host)) {
            int furnace_size = 13;
            int fueling = 4;
            //try to live forever
            int src_count = 0;
            for (Coord c : here.getRandomNeighborsDiagonal()) {
                if (netherBrick.matches(c) || netherStair.matches(c) || netherFence.matches(c)) {
                    src_count += 1;
                }
                if (c.is(Block.netherrack)) {
                    //maybe eat it up!
                    if (age > fueling) {
                        age -= fueling;
                        c.setId(Block.fire);
                    }
                }
            }
            if (src_count < furnace_size) {
                if (src_count == 0) {
                    die();
                    return;
                }
                age += furnace_size - src_count;
                return;
            }
            //burn neighbors
            ArrayList<Coord> n = here.getRandomNeighborsAdjacent();
            n.addAll(here.getRandomNeighborsDiagonal());
            for (Coord c : n) {
                if (c.is(Block.netherBrick)) {
                    continue;
                }
                for (BlockMatch match : transforms.keySet()) {
                    if (match.matches(c)) {
                        BlockMatch burnsTo = transforms.get(match);
                        if (transforms.containsKey(burnsTo)) {
                            continue; //don't burn something that we're just going to reverse
                        }
                        burnsTo.set(c);
                        age += 1;
                        return;
                    }
                }
            }
            return;
        }
        else {
            //BURN THE WORLD
            age += 1;

            //try to spread fire
            for (Coord c : here.getRandomNeighborsDiagonal()) {
                if (trySpawn(c)) {
                    return;
                }
            }

            if (age < max_age / 3) {
                return;
            }

            //now burn the babies
            for (Coord c : here.getRandomNeighborsDiagonal()) {
                if (host.matches(c)) {
                    burnTo.set(c);
                    return;
                }
            }
            age += 10; //die quickly if idle
        }
    }

    boolean tryLoadTarget(int id, int md) {
        for (BlockMatch match : transforms.keySet()) {
            if (match.isType(id, md)) {
                host = match;
                return true;
            }
        }
        if (netherBrick.isType(id, md) || netherFence.isType(id, md) || netherStair.isType(id, md)) {
            host = netherBrick;
            return true;
        }
        return false;
    }

    void setTarget(int id, int md) {
        //try to make this our target... otherwise, improvise.
        if (tryLoadTarget(id, md)) {
            return;
        }
        //Eh. I think this is too dangerous & annoying.
        //		for (Coord c : new Coord(this).getRandomNeighborsAdjacent()) {
        //			if (tryLoadTarget(c.getId(), c.getMd())) {
        //				return;
        //			}
        //		}

    }

    public static void ignite(Coord baseBlock, Coord fireBlock, EntityPlayer player) {
        fireBlock.setIdMd(Core.lightair_id, BlockLightAir.fire_md);
        TileEntityWrathFire fire = fireBlock.getTE(TileEntityWrathFire.class);
        if (fire == null) {
            return;
        }
        if (netherBrick.matches(baseBlock) || netherFence.matches(baseBlock) || netherStair.matches(baseBlock)) {
            Sound.wrathForge.playAt(player);
        }
        else {
            Sound.wrathLight.playAt(player);
        }
        fire.setTarget(baseBlock.getId(), baseBlock.getMd());
    }
}
