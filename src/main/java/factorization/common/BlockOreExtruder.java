package factorization.common;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.Sound;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockOreExtruder extends Block {
    public static final IProperty<Integer> EXTRUSIONS = PropertyInteger.create("extrusions", 0, 15);

    public int EXPECTED_ORE_YIELD = 64;
    public int EXPECTED_CLAY_YIELD = 48;
    public IBlockState[] clays = new IBlockState[] {
            clay(null),
            clay(EnumDyeColor.WHITE),
            clay(null),
            clay(EnumDyeColor.LIME),
            clay(EnumDyeColor.BROWN),
            clay(null),
            clay(EnumDyeColor.ORANGE),
            clay(EnumDyeColor.RED),
    };
    public int delay_min = 24, delay_rng_add = 8;

    {
        if (clays.length != 8) throw new IllegalArgumentException("Must have 8 stained clays");
    }

    private IBlockState clay(EnumDyeColor color) {
        if (color == null) return Blocks.hardened_clay.getDefaultState();
        return Blocks.stained_hardened_clay.getDefaultState().withProperty(BlockStainedGlass.COLOR, color);
    }


    public BlockOreExtruder() {
        super(Material.rock, MapColor.stoneColor);
        setHardness(30);
    }

    @Override
    public int getMobilityFlag() {
        return 2; // unpistonable
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, EXTRUSIONS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(EXTRUSIONS, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(EXTRUSIONS);
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        // Drop nothing. Not even if silk-touched!
    }

    void scheduleTick(World world, BlockPos pos) {
        world.scheduleBlockUpdate(pos, this, world.rand.nextInt(delay_rng_add) + delay_min, 0);
    }

    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        scheduleTick(world, pos);
        return super.onBlockPlaced(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        scheduleTick(world, pos);
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        scheduleTick(world, pos);
    }

    void die(Coord at) {
        at.setId(Core.registry.mantlerock_block, true);
        Sound.extruderBreak.playAt(at);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        Coord at = new Coord(world, pos);
        if (isExposed(world, pos)) return;
        Coord up = at.add(EnumFacing.UP);
        if (!up.isReplacable()) return;
        int extrusions = at.getPropertyOr(EXTRUSIONS, 0);
        double pDegrade = degradeChance(extrusions);
        extrudeBlock(up, extrusions);
        if (world.rand.nextDouble() >= pDegrade) {
            return;
        }
        extrusions++;
        if (extrusions >= 0x10) {
            die(at);
            return;
        }
        at.set(EXTRUSIONS, extrusions);
    }

    private boolean isExposed(World world, BlockPos pos) {
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (dir == EnumFacing.UP) continue;
            BlockPos npos = pos.offset(dir);
            Block nblock = world.getBlockState(npos).getBlock();
            Material nmat = nblock.getMaterial();
            if (nmat == Material.lava) continue;
            if (nmat.isSolid()) continue;
            die(new Coord(world, pos));
            return true;
        }
        return false;
    }

    IBlockState toExtrude(int extrusions) {
        if (extrusions % 2 == 0) {
            return ResourceType.COPPER_ORE.blockState();
        } else {
            return clays[extrusions >> 1];
        }
    }

    double degradeChance(int extrusions) {
        if (extrusions % 2 == 0) {
            return 8.0 / EXPECTED_ORE_YIELD;
        } else {
            return 8.0 / EXPECTED_CLAY_YIELD;
        }
    }

    void extrudeBlock(Coord at, int extrusions) {
        // Lamer alternative: at.set(toSpawn, true);
        IBlockState pistonState = Blocks.piston_extension.getDefaultState().withProperty(BlockPistonMoving.FACING, EnumFacing.UP);
        IBlockState toSpawn = toExtrude(extrusions);
        at.set(pistonState, true);
        at.setTE(BlockPistonMoving.newTileEntity(toSpawn, EnumFacing.UP, true /* extending */, true /* renderHead */));
        Sound.extruderExtrude.playAt(at);
        if (!at.w.isRemote) {
            at.w.addBlockEvent(at.toBlockPos().down(), this, extrusions, 0);
        }
    }

    @Override
    public boolean onBlockEventReceived(World worldIn, BlockPos pos, IBlockState state, int extrusions, int eventParam) {
        if (!worldIn.isRemote) return true;
        Coord at = new Coord(worldIn, pos);
        Coord up = at.add(0, 1, 0);
        extrudeBlock(up, extrusions);
        return true;
    }
}
