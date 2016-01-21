package factorization.redstone;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.SpaceUtil;

public class BlockMatcher extends Block {
    public static final IProperty<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
    public static final IProperty<FiringState> FIRING = PropertyEnum.create("firing", FiringState.class);
    public enum FiringState implements IStringSerializable, Comparable<FiringState> {
        READY(0), FIRING(4), MATCHED(8);
        final int field;

        FiringState(int field) {
            this.field = field;
        }

        boolean on(int f) {
            return (f & field) != 0;
        }

        @Override
        public String getName() {
            return toString();
        }
    }

    public BlockMatcher() {
        super(Material.rock);
        setTickRandomly(false);
        setCreativeTab(Core.tabFactorization);
        setUnlocalizedName("factorization:BlockMatcher");
    }

    @Override
    protected BlockState createBlockState() {
        // NORELEASE: defualt state?
        return new BlockState(this, AXIS, FIRING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        EnumFacing.Axis axis = state.getValue(AXIS);
        FiringState firing = state.getValue(FIRING);
        int md = 0;
        if (axis == EnumFacing.Axis.Y) md = 0;
        if (axis == EnumFacing.Axis.Z) md = 1;
        if (axis == EnumFacing.Axis.X) md = 2;
        md |= firing.field;
        return md;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        FiringState firing = FiringState.READY;
        for (FiringState f : FiringState.values()) {
            if (f.on(meta)) {
                firing = f;
                break;
            }
        }
        int fd = meta & 3;
        EnumFacing.Axis a = EnumFacing.Axis.Y;
        if (fd == 0)  a = EnumFacing.Axis.Y;
        if (fd == 1) a = EnumFacing.Axis.Z;
        if (fd == 2) a = EnumFacing.Axis.X;
        return getDefaultState().withProperty(FIRING, firing).withProperty(AXIS, a);
    }

    byte match(IBlockAccess world, BlockPos pos, EnumFacing axis) {
        // 0: Not at all similar
        // 1: Same solidity
        // 2: Same material
        // 3: Same block
        // 4: Same MD
        // NORELEASE TODO: Barrels. Not just 'are the items identical', but 'is this item the same as that block'.
        // This stuff may be easier in 1.8.
        BlockPos frontpos = pos.offset(axis);
        IBlockState frontbs = world.getBlockState(frontpos);
        Block frontBlock = frontbs.getBlock();
        boolean frontAir = frontBlock.isAir(world, frontpos);

        BlockPos backpos = pos.offset(axis.getOpposite());
        IBlockState backbs = world.getBlockState(backpos);
        Block backBlock = backbs.getBlock();
        boolean backAir = backBlock.isAir(world, backpos);

        // NORELEASE if (frontAir && backAir) return 0;

        if (frontBlock == backBlock) {
            if (FzUtil.sameState(frontbs, backbs)) return 4;
            return 3;
        }
        if (frontBlock.getMaterial() == backBlock.getMaterial()) {
            return 2;
        }
        if (frontBlock.isNormalCube() == backBlock.isNormalCube()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, BlockPos pos) {
        // Oh no! We've lost the side in 1.8!
        IBlockState bs = world.getBlockState(pos);
        EnumFacing.Axis axis = bs.getValue(AXIS);
        byte match = match(world, pos, SpaceUtil.fromAxis(axis));
        if (match == 0) return 0;
        return (match * 5) - 4;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState bs, Block neighborBlock) {
        if (neighborBlock == this) return;
        FiringState state = bs.getValue(FIRING);
        if (state == FiringState.FIRING) return;
        EnumFacing.Axis axis = bs.getValue(AXIS);
        byte match = match(world, pos, SpaceUtil.fromAxis(axis));
        FiringState next_state = match >= 3 ? FiringState.FIRING : FiringState.READY;
        world.updateComparatorOutputLevel(pos, this); // 'update comparators'; don't do this when firing as it might cause a loop?
        if (state == FiringState.MATCHED && next_state == FiringState.FIRING) {
            return;
        }
        int notify = Coord.UPDATE | Coord.NOTIFY_NEIGHBORS;
        IBlockState nbs = bs.withProperty(FIRING, next_state);
        if (FzUtil.sameState(bs, nbs)) return;
        //println("neighbor changed", block.getLocalizedName(), state, "-->", next_state);
        world.setBlockState(pos, nbs, notify);
        world.updateBlockTick(pos, this, 4, 0);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, BlockPos pos, EnumFacing side) {
        IBlockState bs = world.getBlockState(pos);
        EnumFacing.Axis axis = bs.getValue(AXIS);
        return !axis.apply(side);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return canConnectRedstone(world, pos, side);
    }

    @Override
    public int getWeakPower(IBlockAccess world, BlockPos pos, IBlockState bs, EnumFacing side) {
        FiringState state = bs.getValue(FIRING);
        if (state != FiringState.FIRING) return 0;
        EnumFacing.Axis axis = bs.getValue(AXIS);
        if (axis.apply(side)) return 0;

        byte match = match(world, pos, SpaceUtil.fromAxis(axis));
        if (match >= 3) return 0xF;
        return 0;
    }


    @Override
    public void updateTick(World world, BlockPos pos, IBlockState bs, Random rand) {
        EnumFacing.Axis axis = bs.getValue(AXIS);
        byte match = match(world, pos, SpaceUtil.fromAxis(axis));
        FiringState nextState = match >= 3 ? FiringState.MATCHED : FiringState.READY;
        int notify = Coord.UPDATE | Coord.NOTIFY_NEIGHBORS;
        IBlockState nbs = bs.withProperty(FIRING, nextState);
        world.setBlockState(pos, nbs, notify);
    }

    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(AXIS, facing.getAxis());
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState bs) {
        if (bs.getValue(FIRING) == FiringState.READY) return;
        world.setBlockState(pos, bs.withProperty(FIRING, FiringState.READY));
    }
}
