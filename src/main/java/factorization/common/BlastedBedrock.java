package factorization.common;

import com.google.common.base.Predicate;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlastedBedrock extends Block {
    protected BlastedBedrock() {
        super(Material.rock);
        setBlockUnbreakable();
        setResistance(6000000);
        setCreativeTab(Core.tabFactorization);
        setUnlocalizedName("factorization:blasted_bedrock");
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public Item getItem(World world, BlockPos pos) {
        return null;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return null;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        // Not explodable!
    }

    @Override
    public boolean isAssociatedBlock(Block block) {
        return block == Blocks.bedrock || block == this;
    }

    @Override
    public boolean isReplaceableOreGen(World world, BlockPos pos, Predicate<IBlockState> target) {
        return target.apply(Blocks.bedrock.getDefaultState());
    }

    @Override
    public boolean canCreatureSpawn(IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }
}
