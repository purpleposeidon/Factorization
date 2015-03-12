package factorization.colossi;

import java.util.ArrayList;

import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.IconFlipped;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;

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
    
    ForgeDirection getDir(int md) {
        return ForgeDirection.getOrientation(md);
    }
    
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        Coord at = new Coord(world, x, y, z);
        int md = at.getMd();
        ForgeDirection dir = getDir(md);
        boolean good = false;
        // Return false if we are missing our mate & our direction is negative.
        // Missing mates can be caused by pistons; so this prevents duping.
        // NOTE: If the block is pistoned, and then rotated, there could be dupes.
        // (So I guess when we get rotated, we'd have to break our direction if the mate is missing)
        if (dir != ForgeDirection.UNKNOWN) {
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
        ForgeDirection dir = getDir(md);
        ForgeDirection side = ForgeDirection.getOrientation(side_);
        if (dir == side || dir.getOpposite() == side) {
            return end;
        }
        if (dir.offsetX != 0) {
            return (dir == ForgeDirection.EAST) ^ (side.offsetZ == -1) ? high : low;
        }
        if (dir == ForgeDirection.SOUTH) {
            if (side == ForgeDirection.DOWN) return high_fu;
            if (side == ForgeDirection.UP) return high_fu; //return high_f;
            if (side == ForgeDirection.EAST) return low;
            if (side == ForgeDirection.WEST) return high;
        }
        if (dir == ForgeDirection.NORTH) {
            if (side == ForgeDirection.DOWN) return low_fu;
            if (side == ForgeDirection.UP) return low_fu;
            if (side == ForgeDirection.EAST) return high;
            if (side == ForgeDirection.WEST) return low;
        }
        if (dir == ForgeDirection.DOWN) {
            return high_fu;
        }
        if (dir == ForgeDirection.UP) {
            return low_fu;
        }
        return end;
    }
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        /*ForgeDirection dir = getDir(metadata);
        if (SpaceUtil.sign(dir) < 0) {
            Coord mate = new Coord(world, x, y, z).add(dir);
            if (mate.getBlock() != this || getDir(mate.getMd()) != dir.getOpposite()) return new ArrayList<ItemStack>();
        }*/
        return super.getDrops(world, x, y, z, metadata, fortune);
    }

}
