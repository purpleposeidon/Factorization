package factorization.wrath;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockLightAir extends BlockAir {
    public BlockLightAir() {
        super();
        setLightLevel(1F);
        setHardness(0.1F);
        setResistance(0.1F);
        setUnlocalizedName("lightair");
        float nowhere = -10000F;
        setBlockBounds(nowhere, nowhere, nowhere, nowhere, nowhere, nowhere);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) return;
        if (TileEntityWrathLamp.isUpdating) return;
        TileEntityWrathLamp.doAirCheck(world, pos);
        BlockPos below = pos.down();
        if (world.getBlockState(below).getBlock() == this) {
            world.scheduleBlockUpdate(below, this, 1, 0);
        }
    }


    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (neighborBlock instanceof BlockWall) {
            if (world.getBlockState(pos.down()).getBlock() instanceof BlockWall) {
                world.setBlockToAir(pos);
                return;
            }
        }
        TileEntityWrathLamp.doAirCheck(world, pos);
    }

    @Override
    public boolean getTickRandomly() {
        return true;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        TileEntityWrathLamp.doAirCheck(world, pos);
    }

    @Override
    public int getMobilityFlag() {
        return 1; //can't push, but can overwrite
    }
}
