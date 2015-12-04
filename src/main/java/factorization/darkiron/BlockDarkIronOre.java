package factorization.darkiron;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

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
    public void randomDisplayTick(World world, BlockPos pos, Random random) {
        if (world.getTotalWorldTime() % 3 != 0) {
            return;
        }
        if (te_particles > 80) {
            return;
        }
        if (!inRange(pos, Minecraft.getMinecraft().thePlayer)) {
            return;
        }
        if (world.getTileEntity(pos) != null) {
            return;
        }
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (world.isBlockNormalCubeDefault(x + dir.getDirectionVec().getX(), y + dir.getDirectionVec().getY(), z + dir.getDirectionVec().getZ(), true)) {
                continue;
            }
            TileEntity te = new Glint();
            world.setTileEntity(pos, te);
            world.markBlockForUpdate(pos);
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
                worldObj.removeTileEntity(pos.getX(), pos.getY(), pos.getZ());
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
            return getBlockType().getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
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
