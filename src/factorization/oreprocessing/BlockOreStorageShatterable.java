package factorization.oreprocessing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOreStorage;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.shared.Core;

public class BlockOreStorageShatterable extends BlockOreStorage {
    private final Block original;
    public BlockOreStorageShatterable(int id, Block original) {
        super(id);
        this.original = original;
    }
    
    @Override
    public boolean canDropFromExplosion(Explosion boom) {
        return false;
    }
    
    //<Player> i think i'm calling getblockdropped before killing it
    //<Player> so if you drop stuff yourself in breakblock or whatever, it'll break
    
    //See Explosion.doExplosionB
    
    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
        super.onBlockDestroyedByExplosion(world, x, y, z, explosion);
        if (world.isRemote) {
            return;
        }
        Coord c = new Coord(world, x, y, z);
        c.setId(0);
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
    
    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        super.onBlockExploded(world, x, y, z, explosion);
    }
    
    double randShardVelocity(World world) {
        double r = world.rand.nextGaussian()/4;
        double max = 0.3;
        if (r > max) {
            r = max;
        } else if (r < -max) {
            r = -max;
        }
        return r;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister register) {
        original.registerIcons(register);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(int side, int md) {
        return original.getIcon(side, md);
    }
}
