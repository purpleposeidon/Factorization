package factorization.darkiron;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.BlockIcons;

public class BlockDarkIronOre extends Block {
    public BlockDarkIronOre() {
        super(Material.rock);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int par1, int par2) {
        return BlockIcons.ore_dark_iron;
    }
    
    static int te_particles = 0;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getTotalWorldTime() % 3 != 0) {
            return;
        }
        if (te_particles > 80) {
            return;
        }
        if (!inRange(x, y, z, Minecraft.getMinecraft().thePlayer)) {
            return;
        }
        if (world.getTileEntity(x, y, z) != null) {
            return;
        }
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (world.isBlockNormalCubeDefault(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, true)) {
                continue;
            }
            TileEntity te = new Glint();
            world.setTileEntity(x, y, z, te);
            world.markBlockForUpdate(x, y, z);
            return;
        }
    }
    
    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }
    
    static float maxDistSq = 6*6;
    
    static boolean inRange(int xCoord, int yCoord, int zCoord, EntityPlayer player) {
        if (player == null) {
            return false;
        }
        double dx = (player.posX - xCoord);
        double dy = (player.posY - yCoord);
        double dz = (player.posZ - zCoord);
        double distSq = dx*dx + dy*dy + dz*dz;
        return distSq < maxDistSq;
    }
    
    public static class Glint extends TileEntity {
        public int age = 0;
        public long lastRenderedTick = Long.MAX_VALUE;
        
        @SideOnly(Side.CLIENT)
        @Override
        public void updateEntity() {
            age++;
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (lastRenderedTick + 60 < worldObj.getTotalWorldTime() && !inRange(age, age, age, player)) {
                worldObj.removeTileEntity(xCoord, yCoord, zCoord);
            }
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public double getMaxRenderDistanceSquared() {
            return maxDistSq;
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public AxisAlignedBB getRenderBoundingBox() {
            return getBlockType().getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
        }
        
        @Override
        public void invalidate() {
            super.invalidate();
            te_particles--;
        }
        
        @Override
        public void validate() {
            super.validate();
            te_particles++;
        }
    }
}
