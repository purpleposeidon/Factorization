package factorization.coremodhooks;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.shared.Core;

public class HookTargetsServer {
    public static void diamondExploded(Object dis, World world, int x, int y, int z) {
        if (dis != Blocks.diamond_block) return;
        if (world.isRemote) {
            return;
        }
        Coord c = new Coord(world, x, y, z);
        //if (c.isAir()) return;
        c.setAir();
        int i = 18;
        while (i > 0) {
            int spawn = world.rand.nextInt(3) + 2;
            spawn = Math.min(spawn, i);
            i -= spawn;
            EntityItem ent = c.spawnItem(new ItemStack(Core.registry.diamond_shard, spawn));
            ent.invulnerable = true;
            ent.motionX = randShardVelocity(world);
            ent.motionY = randShardVelocity(world);
            ent.motionZ = randShardVelocity(world);
        }
    }
    
    private static double randShardVelocity(World world) {
        double r = world.rand.nextGaussian()/4;
        double max = 0.3;
        if (r > max) {
            r = max;
        } else if (r < -max) {
            r = -max;
        }
        return r;
    }
}
