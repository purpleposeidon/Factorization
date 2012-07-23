package factorization.common;

import factorization.api.Coord;
import net.minecraft.src.Block;
import net.minecraft.src.forge.MinecraftForge;

/**
 * This associates various block properties with metadata.
 * 
 */
public enum BlockClass {

    Default(0, true, 0, 0, 1F),
    DarkIron(1, true, 0, 0, 4F),
    Barrel(2, true, 25, 0, 2F),
    Cage(3, false, 0, 0, 5F),
    Lamp(4, false, 0, 15, 6F),
    LightAir(5, false, 0, 15, 0),
    WrathFire(6, false, 0, 4, 0),
    Machine(7, true, 0, 0, 3.5F),
    MachineLightable(8, true, 0, 13, 3.5F),

    ;

    static class Md {
        //Some say java is retarded. I disagree. Wait -- No I don't.
        static BlockClass map[] = new BlockClass[16];
    }

    Block block;
    int md;
    boolean normalCube;
    int flamability;
    boolean isFlamable;
    int lightValue;
    float hardness; //Our default is 2

    BlockClass(Block block, int md, boolean normalCube, int flamability, int lightValue,
            float hardness) {
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

    BlockClass(int md, boolean normalCube, int flamability, int lightValue,
            float hardness) {
        this(Core.registry.factory_block, md, normalCube, flamability, lightValue, hardness);
    }

    static BlockClass get(int md) {
        BlockClass ret = Md.map[md];
        if (ret == null) {
            return Md.map[0];
        }
        return ret;
    }

    //Makes sure the block at Coord is what it should be
    void enforce(Coord c) {
        if (c.getBlock() == Core.registry.factory_block) {
            //only enforce if the block's right.
            if (c.setMd(md, false)) {
                //System.out.println("BlockClass enforce: Changed to " + this + " at " + c);
            }
        }
    }

    void enforceQuiet(Coord c) {
        if (c.getBlock() == Core.registry.factory_block) {
            c.setMd(md, false);
        }
    }

    BlockClass harvest(String tool, int level) {
        MinecraftForge.setBlockHarvestLevel(this.block, this.md, tool, level);
        return this;
    }
}
