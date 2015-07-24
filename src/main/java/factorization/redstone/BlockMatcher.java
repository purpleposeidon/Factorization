package factorization.redstone;

import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.shared.Core;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

public class BlockMatcher extends Block {
    public BlockMatcher() {
        super(Material.rock);
        setTickRandomly(false);
        setCreativeTab(Core.tabFactorization);
        setBlockName("BlockMatcher");
    }

    static final byte STATE_READY = 0, STATE_FIRING = 4, STATE_WAIT = 8;

    int makeMd(ForgeDirection axis, byte state) {
        if (SpaceUtil.sign(axis) == 1) axis = axis.getOpposite();
        int md = 0;
        if (axis == ForgeDirection.DOWN) md = 0;
        if (axis == ForgeDirection.NORTH) md = 1;
        if (axis == ForgeDirection.WEST) md = 2;
        md |= state;
        return md;
    }

    ForgeDirection getAxis(int md) {
        switch (md & 0x3) {
            default:
            case 0: return ForgeDirection.DOWN;
            case 1: return ForgeDirection.NORTH;
            case 2: return ForgeDirection.WEST;
        }
    }

    byte getState(int md) {
        return (byte) (md & 0xC);
    }

    byte match(IBlockAccess world, int x, int y, int z, ForgeDirection axis) {
        // 0: Not at all similar
        // 1: Same solidity
        // 2: Same material
        // 3: Same block
        // 4: Same MD
        @SuppressWarnings("UnnecessaryLocalVariable")
        ForgeDirection dir1 = axis;
        ForgeDirection dir2 = axis.getOpposite();
        Block front = world.getBlock(x + dir1.offsetX, y + dir1.offsetY, z + dir1.offsetZ);
        int frontMd = world.getBlockMetadata(x + dir1.offsetX, y + dir1.offsetY, z + dir1.offsetZ);
        boolean frontAir = world.isAirBlock(x + dir1.offsetX, y + dir1.offsetY, z + dir1.offsetZ);

        Block back = world.getBlock(x + dir2.offsetX, y + dir2.offsetY, z + dir2.offsetZ);
        int backMd = world.getBlockMetadata(x + dir2.offsetX, y + dir2.offsetY, z + dir2.offsetZ);
        boolean backAir = world.isAirBlock(x + dir2.offsetX, y + dir2.offsetY, z + dir2.offsetZ);
        if (frontAir != backAir) return 0;
        if (front == back) {
            if (frontMd == backMd) {
                return 4;
            }
            return 3;
        }
        if (front.getMaterial() == back.getMaterial()) {
            return 2;
        }
        if (front.isNormalCube() == back.isNormalCube()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        int md = world.getBlockMetadata(x, y, z);
        ForgeDirection axis = getAxis(md);
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (axis == dir || axis == dir.getOpposite()) return 0;
        byte match = match(world, x, y, z, axis);
        if (match == 0) return 0;
        return (match * 5) - 4;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        if (block == this) return;
        int md = world.getBlockMetadata(x, y, z);
        byte state = getState(md);
        if (state == STATE_READY) {
            world.scheduleBlockUpdate(x, y, z, this, 2);
        } else if (state == STATE_WAIT) {
            ForgeDirection axis = getAxis(md);
            byte match = match(world, x, y, z, axis);
            if (match >= 3) return;
            world.setBlockMetadataWithNotify(x, y, z, makeMd(axis, STATE_READY), 0);
        }
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        int md = world.getBlockMetadata(x, y, z);
        byte state = getState(md);
        if (state != STATE_FIRING) return 0;
        ForgeDirection axis = getAxis(md);
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (axis == dir || axis == dir.getOpposite()) return 0;
        byte match = match(world, x, y, z, axis);
        if (match >= 3) return 0xF;
        return 0;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int md = world.getBlockMetadata(x, y, z);
        byte state = getState(md);
        byte nextState;
        int notify = Coord.UPDATE | Coord.NOTIFY_NEIGHBORS;
        if (state == STATE_READY) {
            nextState = STATE_FIRING;
            world.scheduleBlockUpdate(x, y, z, this, 4);
        } else if (state == STATE_FIRING) {
            nextState = STATE_WAIT;
            world.scheduleBlockUpdate(x, y, z, this, 2);
        } else {
            return;
        }
        int nextMd = makeMd(getAxis(md), nextState);
        world.setBlockMetadataWithNotify(x, y, z, nextMd, notify);
    }

    @Override
    public int onBlockPlaced(World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int itemMetadata) {
        return makeMd(ForgeDirection.getOrientation(side), STATE_READY);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = BlockIcons.redstone$matcher_side;
    }

    @Override
    public IIcon getIcon(int side, int md) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        ForgeDirection axis = getAxis(md);
        if (axis == dir || axis == dir.getOpposite()) return BlockIcons.redstone$matcher_face;
        return BlockIcons.redstone$matcher_side;
    }
}
