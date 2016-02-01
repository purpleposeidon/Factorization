package factorization.common;

import factorization.api.Coord;
import factorization.charge.TileEntitySolarBoiler;
import factorization.fzds.network.PPPChunkLoader;
import factorization.shared.Sound;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHardenedClay;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.gui.ForgeGuiFactory;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Random;

public class BlockGeyser extends BlockHardenedClay {
    public static final IProperty<Boolean> PRESSURIZED = PropertyBool.create("pressurized");

    public BlockGeyser() {
        setHardness(7);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, PRESSURIZED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(PRESSURIZED, meta == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PRESSURIZED) ? 1 : 0;
    }

    int getHashVal(World world, BlockPos pos, int min, int max) {
        long hash = Math.abs(pos.hashCode() + world.getSeed()) % 1000;
        double t = hash / 1000.0;
        int range = max - min;
        return min + (int) (range * t);
    }

    int getDelay(World world, BlockPos pos) {
        int min_time = 20 * 6;
        int max_time = 20 * 20;
        return getHashVal(world, pos, min_time, max_time);
    }

    int getAmount(World world, BlockPos pos) {
        return getHashVal(world, pos, 500, 1000);
    }

    void scheduleNextTick(World world, BlockPos pos) {
        if (world.isRemote) return;
        world.scheduleBlockUpdate(pos, this, getDelay(world, pos), 0);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        scheduleNextTick(world, pos);
    }


    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        // No item.
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        BlockPos up = pos.up();
        boolean relieved = relievePressure(world, pos, up);
        Boolean isPressurized = state.getValue(PRESSURIZED);
        if (relieved) {
            Sound.geyserBlast.playAt(new Coord(world, up));
            if (isPressurized) {
                world.setBlockState(pos, state.withProperty(PRESSURIZED, false));
            }
            scheduleNextTick(world, pos);
        } else if (!isPressurized) {
            world.setBlockState(pos, state.withProperty(PRESSURIZED, true));
        }
    }

    public boolean relievePressure(World world, BlockPos pos, BlockPos up) {
        IBlockState stateAbove = world.getBlockState(up);
        Block blockAbove = stateAbove.getBlock();
        if (blockAbove.hasTileEntity(stateAbove)) {
            TileEntity te = world.getTileEntity(up);
            if (te instanceof IFluidHandler) {
                IFluidHandler tank = (IFluidHandler) te;
                FluidStack steam = TileEntitySolarBoiler.getSteamStack().copy();
                steam.amount = getAmount(world, pos);
                return tank.fill(EnumFacing.DOWN, steam, true) > 0;
            }
        }
        if (!blockAbove.isPassable(world, pos)) {
            return false;
        }
        if (world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16 * 3) != null) {
            // Could also check ForgeChunkManager to see if this chunk is force-loaded.
            // I think I won't though.
            EntitySteamGeyser geyser = new EntitySteamGeyser(world);
            geyser.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            world.spawnEntityInWorld(geyser);
        }
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (state.getValue(PRESSURIZED)) {
            updateTick(world, pos, state, world.rand);
        }
    }
}
