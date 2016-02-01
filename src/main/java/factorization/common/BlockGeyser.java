package factorization.common;

import factorization.api.Coord;
import factorization.charge.TileEntitySolarBoiler;
import factorization.shared.Sound;
import net.minecraft.block.BlockHardenedClay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Random;

public class BlockGeyser extends BlockHardenedClay {

    public BlockGeyser() {
        setHardness(7);
    }

    int getHashVal(World world, BlockPos pos, int min, int max) {
        int hash = (int)(Math.abs(pos.hashCode() + world.getSeed()) % Integer.MAX_VALUE);
        int range = max - min;
        double t = (hash % 1000) / 1000.0;
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
        scheduleNextTick(world, pos);
        BlockPos up = pos.up();
        Sound.geyserBlast.playAt(new Coord(world, up));
        if (!world.getBlockState(up).getBlock().isPassable(world, pos)) {
            TileEntity te = world.getTileEntity(up);
            if (te instanceof IFluidHandler) {
                IFluidHandler tank = (IFluidHandler) te;
                FluidStack steam = TileEntitySolarBoiler.getSteamStack().copy();
                steam.amount = getAmount(world, pos);
                tank.fill(EnumFacing.DOWN, steam, true);
            }
            return;
        }
        if (world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16 * 3) == null) return;
        EntitySteamGeyser geyser = new EntitySteamGeyser(world);
        geyser.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        world.spawnEntityInWorld(geyser);
    }
}
