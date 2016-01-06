package factorization.shared;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * This associates various block properties with metadata.
 * 
 */
public enum BlockClass implements IStringSerializable, Comparable<BlockClass> {

    Default(0, true, 0, 0, 1F),
    DarkIron(1, true, 0, 0, 3.25F),
    Barrel(2, true, 25, 0, 2F),
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
        NORELEASE.fixme("Destroy this");
    }

    static class Md {
        //Some say java is retarded. I disagree. Wait -- No I don't.
        static BlockClass map[] = new BlockClass[16];
    }

    public Block block = null; // has to be set later unfortunately, due to a loop
    public final int md;
    final boolean normalCube;
    final int flamability;
    final boolean isFlamable;
    final int lightValue;
    final float hardness; //Our default is 2
    boolean normal_cube = true;
    boolean passable = false;

    BlockClass(int md, boolean normalCube, int flamability, int lightValue, float hardness) {
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

    BlockClass setAbnormal() {
        normal_cube = false;
        return this;
    }

    public static BlockClass get(int md) {
        if (md < 0) return Default;
        if (md > Md.map.length) return Default;
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
        IBlockState defaultState = this.block.getDefaultState();
        if (defaultState.getProperties().containsKey(BlockFactorization.BLOCK_CLASS)) {
            IBlockState ibs = defaultState.withProperty(BlockFactorization.BLOCK_CLASS, this);
            this.block.setHarvestLevel(tool, level, ibs);
        }
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
