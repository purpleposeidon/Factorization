package factorization.colossi;

import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class GargantuanBlock extends Block {
    public static final IProperty<EnumFacing> FACE = PropertyDirection.create("face");


    public GargantuanBlock() {
        super(Material.rock);
        setHardness(2.0F).setResistance(10.0F).setStepSound(soundTypePiston);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACE).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACE, EnumFacing.VALUES[meta]);
    }

    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACE, facing);
    }

    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        Coord at = new Coord(world, pos);
        IBlockState bs = world.getBlockState(pos);
        EnumFacing dir = bs.getValue(FACE);
        boolean good = false;
        // Return false if we are missing our mate & our direction is negative.
        // Missing mates can be caused by pistons; so this prevents duping.
        // NOTE: If the block is pistoned, and then rotated, there could be dupes.
        // (So I guess when we get rotated, we'd have to break our direction if the mate is missing)
        if (dir != null) {
            Coord child = at.add(dir);
            IBlockState cbs = child.getState();
            if (cbs.getBlock() == this && cbs.getValue(FACE) == dir.getOpposite()) {
                child.setAir();
                good = true;
            } else {
                good = SpaceUtil.sign(dir) > 0;
            }
        }
        return super.removedByPlayer(world, pos, player, willHarvest) && good;
    }

}
