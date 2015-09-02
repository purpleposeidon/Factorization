package factorization.colossi;

import factorization.coremodhooks.HookTargetsServer;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;
import java.util.Random;

public class BlockBlast extends Block {
    public BlockBlast() {
        super(Material.tnt);
        setBlockName("blastBlock");
        setBlockTextureName("factorization:blastBlock");
        setCreativeTab(Core.tabFactorization);
        setHardness(1.5F);
        setResistance(10F);
    }

    int blast_radius = 1;

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        world.scheduleBlockUpdate(x, y, z, this, 6 + world.rand.nextInt(4));
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
        /*if (world instanceof World) {
            onNeighborBlockChange((World) world, x, y, z, world.getBlock(tileX, tileY, tileZ));
        }*/
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
        for (int dx = -blast_radius; dx <= +blast_radius; dx++) {
            for (int dy = -blast_radius; dy <= +blast_radius; dy++) {
                for (int dz = -blast_radius; dz <= +blast_radius; dz++) {
                    if (world.getBlock(x + dx, y + dy, z + dz) == this) {
                        world.setBlockMetadataWithNotify(x + dx, y + dy, z + dz, 1, 0);
                        onNeighborBlockChange(world, x + dx, y + dy, z + dz, this);
                    }
                }
            }
        }
    }

    @Override
    public float getExplosionResistance(Entity explosion, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
        world.setBlockMetadataWithNotify(x, y, z, 1, 0);
        onNeighborBlockChange(world, x, y, z, this);
        return super.getExplosionResistance(explosion, world, x, y, z, explosionX, explosionY, explosionZ);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        boolean boom = world.getBlockMetadata(x, y, z) == 1;
        if (!boom) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int atX = x + dir.offsetX;
                int atY = y + dir.offsetY;
                int atZ = z + dir.offsetZ;
                Block b = world.getBlock(atX, atY, atZ);
                Material mat = b.getMaterial();
                if (mat == Material.lava || mat == Material.fire || b.isBurning(world, atX, atY, atZ)) {
                    boom = true;
                    break;
                }
            }
        }
        if (!boom) return;
        boom(world, x + 0.5, y + 0.5, z + 0.5);
        world.setBlockToAir(x, y, z);
        this.onBlockExploded(world, x, y, z, null);
    }
    
    double explosionSize = 8;
    
    void boom(World world, double explosionX, double explosionY, double explosionZ) {
        world.playSoundEffect(explosionX, explosionY, explosionZ, "random.explode", 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        world.spawnParticle("largeexplode", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D);
        double r = 5;
        List list = world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(explosionX - r, explosionY - r, explosionZ - r, explosionX + r, explosionY + r, explosionZ + r));

        Vec3 vec3 = Vec3.createVectorHelper(explosionX, explosionY, explosionZ);
        for (Object aList : list) {
            Entity entity = (Entity) aList;
            double dist = entity.getDistance(explosionX, explosionY, explosionZ) / explosionSize;

            if (dist > 1.0D) continue;
            double dx = entity.posX - explosionX;
            double dy = entity.posY + (double) entity.getEyeHeight() - explosionY;
            double dz = entity.posZ - explosionZ;
            double entDist = (double) MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);

            if (entDist == 0.0D) continue;
            dx /= entDist;
            dy /= entDist;
            dz /= entDist;
            double density = (double) world.getBlockDensity(vec3, entity.boundingBox);
            double pain = (1.0D - dist) * density;
            entity.attackEntityFrom(DamageSource.setExplosionSource(null), (float) ((int) ((pain * pain + pain) / 2.0D * 8.0D * (double) explosionSize + 1.0D)));
            // Yeah, we're manually applying the coremod here, ahem.
            double blastbackResistance = HookTargetsServer.clipExplosionResistance(entity, pain);
            //EnchantmentProtection.func_92092_a(entity, pain);
            entity.motionX += dx * blastbackResistance;
            entity.motionY += dy * blastbackResistance;
            entity.motionZ += dz * blastbackResistance;
        }

    }
}
