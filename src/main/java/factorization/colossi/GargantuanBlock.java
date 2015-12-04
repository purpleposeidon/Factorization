package factorization.colossi;

import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.IconFlipped;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

public class GargantuanBlock extends Block {

    public GargantuanBlock() {
        super(Material.rock);
        setHardness(2.0F).setResistance(10.0F).setStepSound(soundTypePiston);
    }
    
    @SideOnly(Side.CLIENT)
    IIcon end, low, high, low_f, high_f, low_fu, high_fu;
    
    @Override
    public void registerBlockIcons(IIconRegister registry) {
        end = registry.registerIcon("factorization:colossi/gargantuan_end");
        low = registry.registerIcon("factorization:colossi/gargantuan_low");
        high = registry.registerIcon("factorization:colossi/gargantuan_high");
        low_f = registry.registerIcon("factorization:colossi/gargantuan_low_f");
        high_f = registry.registerIcon("factorization:colossi/gargantuan_high_f");
        low_fu = new IconFlipped(low_f, true, false);
        high_fu = new IconFlipped(high_f, true, false);
    }
    
    EnumFacing getDir(int md) {
        return SpaceUtil.getOrientation(md);
    }
    
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        Coord at = new Coord(world, x, y, z);
        int md = at.getMd();
        EnumFacing dir = getDir(md);
        boolean good = false;
        // Return false if we are missing our mate & our direction is negative.
        // Missing mates can be caused by pistons; so this prevents duping.
        // NOTE: If the block is pistoned, and then rotated, there could be dupes.
        // (So I guess when we get rotated, we'd have to break our direction if the mate is missing)
        if (dir != null) {
            Coord child = at.add(dir);
            if (child.getId() == this && getDir(child.getMd()) == dir.getOpposite()) {
                child.setAir();
                good = true;
            } else {
                good = SpaceUtil.sign(dir) > 0;
            }
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest) && good;
    }
    
    @Override
    public IIcon getIcon(int side_, int md) {
        EnumFacing dir = getDir(md);
        EnumFacing side = SpaceUtil.getOrientation(side_);
        if (dir == side || dir.getOpposite() == side) {
            return end;
        }
        if (dir.getDirectionVec().getX() != 0) {
            return (dir == EnumFacing.EAST) ^ (side.getDirectionVec().getZ() == -1) ? high : low;
        }
        if (dir == EnumFacing.SOUTH) {
            if (side == EnumFacing.DOWN) return high_fu;
            if (side == EnumFacing.UP) return high_fu; //return high_f;
            if (side == EnumFacing.EAST) return low;
            if (side == EnumFacing.WEST) return high;
        }
        if (dir == EnumFacing.NORTH) {
            if (side == EnumFacing.DOWN) return low_fu;
            if (side == EnumFacing.UP) return low_fu;
            if (side == EnumFacing.EAST) return high;
            if (side == EnumFacing.WEST) return low;
        }
        if (dir == EnumFacing.DOWN) {
            return high_fu;
        }
        if (dir == EnumFacing.UP) {
            return low_fu;
        }
        return end;
    }
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        /*EnumFacing dir = getDir(metadata);
        if (SpaceUtil.sign(dir) < 0) {
            Coord mate = new Coord(world, x, y, z).add(dir);
            if (mate.getBlock() != this || getDir(mate.getMd()) != dir.getOpposite()) return new ArrayList<ItemStack>();
        }*/
        return super.getDrops(world, x, y, z, metadata, fortune);
    }

}
