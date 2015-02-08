package factorization.util;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class FluidUtil {
    public static void spill(Coord where, FluidStack what) {
        //TODO: Should be in Coord, no?
        if (what == null || what.amount < 0) {
            return;
        }
        FluidEvent.fireEvent(new FluidEvent.FluidSpilledEvent(what, where.w, where.x, where.y, where.z));
    }

    public static FluidStack drainSpecificBlockFluid(World worldObj, int x, int y, int z, boolean doDrain, Fluid targetFluid) {
        Block b = worldObj.getBlock(x, y, z);
        if (!(b instanceof IFluidBlock)) {
            Fluid vanilla;
            if (b == Blocks.water || b == Blocks.flowing_water) {
                vanilla = FluidRegistry.WATER;
            } else if (b == Blocks.lava || b == Blocks.flowing_lava) {
                vanilla = FluidRegistry.LAVA;
            } else {
                return null;
            }
            if (worldObj.getBlockMetadata(x, y, z) != 0) {
                return null;
            }
            if (doDrain) {
                worldObj.setBlockToAir(x, y, z);
            }
            return new FluidStack(vanilla, FluidContainerRegistry.BUCKET_VOLUME);
        }
        IFluidBlock block = (IFluidBlock) b;
        if (!block.canDrain(worldObj, x, y, z)) return null;
        FluidStack fs = block.drain(worldObj, x, y, z, false);
        if (fs == null) return null;
        if (fs.getFluid() != targetFluid) return null;
        if (doDrain) {
            fs = block.drain(worldObj, x, y, z, true);
        }
        if (fs == null || fs.amount <= 0) return null;
        return fs;
    }
}
