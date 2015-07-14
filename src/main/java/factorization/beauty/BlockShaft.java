package factorization.beauty;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.IShaftPowerSource;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.IRenderNonTE;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;

import static net.minecraftforge.common.util.ForgeDirection.*;

public class BlockShaft extends Block implements IRenderNonTE {
    public BlockShaft(Material material) {
        super(material);
    }

    // Bit layout:
    //      SSDD
    // S = speed
    // D = shaftDirection

    public static final ForgeDirection[] meta2direction = new ForgeDirection[] {
            UP, UP, UP, UP, UP,
            SOUTH, SOUTH, SOUTH, SOUTH, SOUTH,
            EAST, EAST, EAST, EAST, EAST,
            UP /* Invalid. Could be UNKNOWN, let's just use something that's valid */
    };

    public static final byte MAX_SPEED = 4;
    public static final byte[] meta2speed = new byte[] {
            0, 1, 2, 3, 4,
            0, 1, 2, 3, 4,
            0, 1, 2, 3, 4,
            0 /* Invalid */
    };


    @SideOnly(Side.CLIENT)
    FactorizationBlockRender render;

    @Override
    public FactorizationBlockRender getFBR() {
        if (render == null) render = new RenderShaft();
        return render;
    }

    @Override
    public int getRenderType() {
        return Core.nonte_rendertype;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void registerBlockIcons(IIconRegister registry) {
        // NADA!
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
        int md = w.getBlockMetadata(x, y, z);
        ForgeDirection dir = meta2direction[md];
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == ForgeDirection.SOUTH) {
            setBlockBounds(l, l, 0, h, h, 1);
        } else if (dir == ForgeDirection.EAST) {
            setBlockBounds(0, l, l, 1, h, h);
        } else { // UP
            setBlockBounds(l, 0, l, h, 1, h);
        }
    }

    // These two methods are necessary due to threading issues
    // Otherwise 'setBlockBoundsBasedOnState' would ordinarily take care of everything
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        int md = w.getBlockMetadata(x, y, z);
        ForgeDirection dir = meta2direction[md];
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == ForgeDirection.SOUTH) {
            return AxisAlignedBB.getBoundingBox(x + l, y + l, z + 0, x + h, y + h, z + 1);
        } else if (dir == ForgeDirection.EAST) {
            return AxisAlignedBB.getBoundingBox(x + 0, y + l, z + l, x + 1, y + h, z + h);
        } else { // UP
            return AxisAlignedBB.getBoundingBox(x + l, y + 0, z + l, x + h, y + 1, z + h);
        }
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
        return getCollisionBoundingBoxFromPool(w, x, y, z);
    }

    static int getDirection(ForgeDirection fd, int speed) {
        if (SpaceUtil.sign(fd) == -1) fd = fd.getOpposite();
        for (int i = 0; i < meta2direction.length; i++) {
            if (meta2direction[i] == fd && meta2speed[i] == speed) return i;
        }
        return speed;
    }

    @Override
    public int onBlockPlaced(World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        // Just figure out the correct metadata
        ForgeDirection fd = ForgeDirection.getOrientation(side);
        return getDirection(fd, 0);
    }

    @Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        Coord at = new Coord(w, x, y, z);
        if (!isUnconnected(at)) return;
        ArrayList<Coord> lockedNeighbors = new ArrayList<Coord>();
        ArrayList<Coord> freeNeighbors = new ArrayList<Coord>();
        for (Coord neighbor : at.getNeighborsAdjacent()) {
            if (!(neighbor.getBlock() instanceof BlockShaft)) continue;
            if (isUnconnected(neighbor)) {
                freeNeighbors.add(neighbor);
            } else {
                int nmd = neighbor.getMd();
                ForgeDirection dir = meta2direction[nmd];
                if (neighbor.add(dir).equals(at) || neighbor.add(dir.getOpposite()).equals(at)) {
                    lockedNeighbors.add(neighbor);
                }
            }
        }
        int n = lockedNeighbors.size() + freeNeighbors.size();
        if (n != 1) return;
        for (Coord neighbor : lockedNeighbors) {
            int nmd = neighbor.getMd();
            ForgeDirection dir = meta2direction[nmd];
            at.setMd(getDirection(dir, meta2speed[nmd]), true);
            return;
        }
        for (Coord neighbor : freeNeighbors) {
            ForgeDirection dir = neighbor.difference(at).getDirection();
            if (SpaceUtil.sign(dir) == -1) dir = dir.getOpposite();
            // We're turning the neighbor, so speed won't be kept
            int dirMd = getDirection(dir, 0);
            neighbor.setMd(dirMd, true);
            at.setMd(dirMd, true);
            return;
        }
    }

    boolean isUnconnected(Coord at) {
        ForgeDirection dir = meta2direction[at.getMd()];
        return !isConnected(at, dir) && !isConnected(at, dir.getOpposite());
    }

    boolean isConnected(Coord at, ForgeDirection dir) {
        ForgeDirection myDir = meta2direction[at.getMd()];
        if (myDir != dir && myDir != dir.getOpposite()) return false;
        Coord neighbor = at.add(dir);
        if (!(neighbor.getBlock() instanceof BlockShaft)) {
            TileEntity te = neighbor.getTE();
            IShaftPowerSource shaft = KineticProxy.cast(te);
            return shaft != null && shaft.canConnect(dir.getOpposite());
        }
        ForgeDirection nDir = meta2direction[neighbor.getMd()];
        return nDir == myDir;
    }

    public static void propagateVelocity(IShaftPowerSource src, Coord at, ForgeDirection dir) {
        double angularVelocity = src.getAngularVelocity(dir);
        int i_speed = (int) (Math.abs(angularVelocity) / Math.PI / 20);
        byte speed = i_speed > MAX_SPEED ? MAX_SPEED : (byte) i_speed;
        while (dir.offsetY != 0 || at.blockExists()) {
            if (!(at.getBlock() instanceof BlockShaft)) break;
            int origMd = at.getMd();
            ForgeDirection d = meta2direction[origMd];
            if (d != dir && d != dir.getOpposite()) break;
            int newMd = getDirection(d, speed);
            if (origMd == newMd) break;
            at.setMd(newMd, true);
            at.adjust(dir);
        }
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, Block block, int md) {
        super.breakBlock(w, x, y, z, block, md); // Yes, we want the TE invalidation here
        Coord at = new Coord(w, x, y, z);
        ForgeDirection dir = meta2direction[at.getMd()];
        invalidateLine(at.copy(), dir);
        invalidateLine(at.copy(), dir.getOpposite());
    }

    public static ForgeDirection normalizeDirection(ForgeDirection dir) {
        return SpaceUtil.sign(dir) == -1 ? dir.getOpposite() : dir;
    }

    void invalidateLine(Coord at, ForgeDirection dir) {
        ForgeDirection normal = normalizeDirection(dir);
        while (true) {
            at.adjust(dir);
            if (!(at.getBlock() instanceof BlockShaft)) {
                at.notifyBlockChange();
                break;
            }
            int md = at.getMd();
            if (meta2direction[md] != normal) break;
            at.setMd(getDirection(normal, 0), true);
            TileEntity te = at.forceGetTE();
            if (te instanceof TileEntityShaftUpdater) te.invalidate();
        }
    }


}
