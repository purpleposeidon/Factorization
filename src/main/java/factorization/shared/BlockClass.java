package factorization.shared;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * This associates various block properties with metadata.
 * 
 */
public enum BlockClass implements IStringSerializable, Comparable<BlockClass> {

    Default(0, true, 0, 0, 1F),
    DarkIron(1, true, 0, 0, 3.25F),
    Barrel(Core.registry.factory_block_barrel, 2, true, 25, 0, 2F),
    //Cage(3, false, 0, 0, 5F),
    Socket(3, false, 0, 0, 3F),
    Lamp(4, false, 0, 15, 6F),
    //LightAir(5, false, 0, 15, 0),
    //WrathFire(6, false, 0, 4, 0),
    MachineDynamicLightable(6, true, 0, 0, 3F),
    Machine(7, true, 0, 0, 3F),
    MachineLightable(8, true, 0, 13, 3F),
    Wire(9, false, 0, 0, 0.25F),
    Ceramic(10, false, 0, 0, 0.75F);

    static {
        Wire.setAbnormal();
        Machine.setAbnormal();
        Ceramic.setAbnormal();
        Wire.passable = true;
    }

    static class Md {
        //Some say java is retarded. I disagree. Wait -- No I don't.
        static BlockClass map[] = new BlockClass[16];
    }

    public final Block block; //XXX This actually gets set to the block properly right now, but it might not in a dynamic-block-ids future or something...?
    public final int md;
    final boolean normalCube;
    final int flamability;
    final boolean isFlamable;
    final int lightValue;
    final float hardness; //Our default is 2
    boolean normal_cube = true;
    boolean passable = false;

    BlockClass(Block block, int md, boolean normalCube, int flamability, int lightValue, float hardness) {
        if (block == null) throw new NullPointerException("BlockClass loaded too early?");
        this.block = block;
        this.md = md;
        this.normalCube = normalCube;
        this.flamability = flamability;
        this.isFlamable = flamability > 0;
        this.lightValue = lightValue;
        this.hardness = hardness;

        if (Md.map[this.md] != null) {
            throw new RuntimeException("Duplicate BlockProperty metadata ID");
        }
        Md.map[this.md] = this;
    }

    BlockClass(int md, boolean normalCube, int flamability, int lightValue, float hardness) {
        this(Core.registry.factory_block, md, normalCube, flamability, lightValue, hardness);
    }

    BlockClass setAbnormal() {
        normal_cube = false;
        return this;
    }

    public static BlockClass get(int md) {
        if (md < 0) return null;
        if (md > Md.map.length) return null;
        BlockClass ret = Md.map[md];
        if (ret == null) {
            return Md.map[0];
        }
        return ret;
    }

    //Makes sure the block at Coord is what it should be
    public void enforce(Coord c) {
        if (c.getBlock() == block) {
            //only enforce if the block's right.
            c.setMd(md, false);
        }
    }

    public BlockClass harvest(String tool, int level) {
        this.block.setHarvestLevel(tool, level, this.md);
        return this;
    }

    boolean isNormal() {
        return normal_cube;
    }

    @Override
    public String getName() {
        return super.toString().toLowerCase(Locale.ROOT);
    }
}
