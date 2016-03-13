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
import net.minecraft.world.World;

import java.util.Random;

public class BlockOreExtruder extends Block {
    public static final IProperty<Integer> EXTRUSIONS = PropertyInteger.create("extrusions", 0, 15);

    private static IBlockState clay(EnumDyeColor color) {
        if (color == null) return Blocks.hardened_clay.getDefaultState();
        return Blocks.stained_hardened_clay.getDefaultState().withProperty(BlockStainedGlass.COLOR, color);
    }
    private static IBlockState copper() {
        return ResourceType.COPPER_ORE.blockState();
    }
    public int delay_min = 24, delay_rng_add = 8;
    public Object[] extrudeInfo = new Object[] {
            // BlockState                       yield  delay
            copper(),                           8,     0, // Oooh, copper! And *more* copper?
            clay(null),                         6,     0, // Now it gives clay?
            copper(),                           8,     1, // Copper again!
            clay(EnumDyeColor.WHITE),           4,     8, // Gasp! *White clay*!?
            copper(),                           16,    4, // Ah, more of what I really want.
            clay(null),                         24,    16, //This stupid clay is slow.
            copper(),                           32,    12, // Hey, is the copper slowing as well?
            clay(EnumDyeColor.LIME),            18,    20 * 2, // Jeeze this clay is slow
            copper(),                           48,    20 * 4, // This is boring.
            clay(EnumDyeColor.BROWN),           18,    20 * 5, // I'll mine a different extruder
            copper(),                           64,    20 * 8, // Or perhaps automate it.
            clay(null),                         128,   20 * 10,
            copper(),                           64,    20 * 30,
            clay(EnumDyeColor.ORANGE),          32,    20 * 60,
            copper(),                           64,    20 * 120,
            clay(EnumDyeColor.RED),             16,    0 // You'll probably never notice this. You might notice red is the last one tho.
    };
    {
        for (int i = 0; i < 16; i++) {
            toExtrude(i);
            getDecayChance(i);
            getDelay(i);
        }
        if (extrudeInfo.length != 16 * 3) throw new AssertionError("Extrusions must have 16 * 3 elements");
    }

    IBlockState toExtrude(int extrusionLevel) {
        return (IBlockState) extrudeInfo[extrusionLevel * 3];
    }

    public double getDecayChance(int extrusionLevel) {
        int expect = (Integer) extrudeInfo[extrusionLevel * 3 + 1];
        return 1.0 / expect;
    }

    public int getDelay(int extrusionLevel) {
        return (Integer) extrudeInfo[extrusionLevel * 3 + 2];
    }


    public BlockOreExtruder() {
        super(Material.rock, MapColor.stoneColor);
        setHardness(40);
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
        int i = world.getBlockState(pos).getValue(EXTRUSIONS);
        int delay = world.rand.nextInt(delay_rng_add) + delay_min + getDelay(i);
        world.scheduleBlockUpdate(pos, this, delay, 0);
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
        double pDegrade = this.getDecayChance(extrusions);
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
