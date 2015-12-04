package factorization.util;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class FluidUtil {
    public static void spill(Coord where, FluidStack what) {
        //TODO: Should be in Coord, no?
        if (what == null || what.amount < 0) {
            return;
        }
        FluidEvent.fireEvent(new FluidEvent.FluidSpilledEvent(what, where.w, where.toBlockPos()));
    }

    public static FluidStack drainSpecificBlockFluid(World worldObj, BlockPos pos, boolean doDrain, Fluid targetFluid) {
        IBlockState bs = worldObj.getBlockState(pos);
        Block b = bs.getBlock();
        Integer fluidLevel = bs.getValue(BlockLiquid.LEVEL);
        if (fluidLevel != null) {
            if (fluidLevel != 0) return null;
            Fluid vanilla;
            if (b == Blocks.water || b == Blocks.flowing_water) {
                vanilla = FluidRegistry.WATER;
            } else if (b == Blocks.lava || b == Blocks.flowing_lava) {
                vanilla = FluidRegistry.LAVA;
            } else {
                return null;
            }
            if (doDrain) {
                worldObj.setBlockToAir(pos);
            }
            return new FluidStack(vanilla, FluidContainerRegistry.BUCKET_VOLUME);
        }
        IFluidBlock block = (IFluidBlock) b;
        if (!block.canDrain(worldObj, pos)) return null;
        FluidStack fs = block.drain(worldObj, pos, false);
        if (fs == null) return null;
        if (fs.getFluid() != targetFluid) return null;
        if (doDrain) {
            fs = block.drain(worldObj, pos, true);
        }
        if (fs == null || fs.amount <= 0) return null;
        return fs;
    }

    public static int transfer(IFluidTank dst, IFluidTank src) {
        int free = dst.getCapacity() - dst.getFluidAmount();
        int use = dst.fill(src.drain(free, false), false);
        return dst.fill(src.drain(use, true), true);
    }
}
