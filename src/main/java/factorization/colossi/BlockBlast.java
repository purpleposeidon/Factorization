package factorization.colossi;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import factorization.api.Coord;
import factorization.coremodhooks.HookTargetsServer;
import factorization.shared.Core;
import factorization.util.SpaceUtil;

public class BlockBlast extends Block {
    public static final IProperty<Boolean> EXPLODING = PropertyBool.create("exploding");

    public BlockBlast() {
        super(Material.tnt);
        setUnlocalizedName("factorization:blastBlock");
        setCreativeTab(Core.tabFactorization);
        setHardness(1.5F);
        setResistance(10F);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, EXPLODING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        if (state.getValue(EXPLODING)) return 1;
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        if (meta > 0) return getDefaultState().withProperty(EXPLODING, true);
        return getDefaultState().withProperty(EXPLODING, false);
    }

    int blast_radius = 1;

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        world.scheduleBlockUpdate(pos, this, 4 + world.rand.nextInt(4), 0);
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        for (BlockPos hit : SpaceUtil.iteratePos(pos, blast_radius)) {
            IBlockState bs = world.getBlockState(hit);
            if (bs.getBlock() != this) continue;
            if (hit.equals(pos)) continue;
            new Coord(world, pos).trySet(EXPLODING, true);
            onNeighborBlockChange(world, hit, bs, this);
        }
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        Coord coord = new Coord(world, pos);
        coord.trySet(EXPLODING, true);
        onNeighborBlockChange(world, pos, coord.getState(), this);
        return super.getExplosionResistance(world, pos, exploder, explosion);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) return;
        IBlockState ibs = world.getBlockState(pos);
        boolean boom = ibs.getValue(EXPLODING);
        if (!boom) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos at = pos.offset(dir);
                Block b = world.getBlockState(at).getBlock();
                Material mat = b.getMaterial();
                if (mat == Material.lava || mat == Material.fire || b.isBurning(world, at)) {
                    boom = true;
                    break;
                }
            }
        }
        if (!boom) return;
        boom(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        world.setBlockToAir(pos);
        this.onBlockExploded(world, pos, null);
    }
    
    double explosionSize = 8;
    
    void boom(World world, double explosionX, double explosionY, double explosionZ) {
        world.playSoundEffect(explosionX, explosionY, explosionZ, "random.explode", 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        if (world.isRemote) {
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, explosionX, explosionY, explosionZ, 4, 1, 0, 0);
        } else {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, explosionX, explosionY, explosionZ, 4, 1, 0, 0);
        }
        double r = 5;
        List list = world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(explosionX - r, explosionY - r, explosionZ - r, explosionX + r, explosionY + r, explosionZ + r));

        Vec3 vec3 = new Vec3(explosionX, explosionY, explosionZ);
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
            double density = (double) world.getBlockDensity(vec3, entity.getEntityBoundingBox());
            double pain = (1.0D - dist) * density;
            // TODO: Blast Blocks should do some actual damage.
            // (Especially now that they're no longer "used" in the colossus fight...)
            // Presence/ratios of sand/sandstone/blastblocks should influence the explosion behavior
            // Perlin noise stuff!
            entity.attackEntityFrom(DamageSource.setExplosionSource(null), (float) ((int) ((pain * pain + pain) / 2.0D * 8.0D * explosionSize + 1.0D)));
            // Yeah, we're manually applying the coremod here, ahem.
            double blastbackResistance = HookTargetsServer.clipExplosionResistance(entity, pain);
            //EnchantmentProtection.func_92092_a(entity, pain);
            entity.motionX += dx * blastbackResistance;
            entity.motionY += dy * blastbackResistance;
            entity.motionZ += dz * blastbackResistance;
        }

    }
}
