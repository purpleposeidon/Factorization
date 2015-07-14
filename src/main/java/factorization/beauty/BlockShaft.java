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
    public BlockShaft(Material material, ForgeDirection axis) {
        super(material);
        if (SpaceUtil.sign(axis) <= 0) throw new IllegalArgumentException();
        this.axis = axis;
    }

    // Bit layout:
    //      SSDD
    // S = speed
    // D = shaftDirection
    public final ForgeDirection axis;
    private BlockShaft[] shafts;
    public static final byte MAX_SPEED = 4;
    public static final byte[] meta2speed = new byte[] {
            0, 1, 2, 3, 4,
            0, -1, -2, -3, -4,
            0, 0, 0, 0, 0, 0 /* Invalid */
    };

    public void setShafts(BlockShaft[] shafts) {
        this.shafts = shafts;
        if (shafts[axis.ordinal()] != this || shafts[axis.getOpposite().ordinal()] != this) {
            throw new IllegalArgumentException();
        }
    }

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
        ForgeDirection dir = axis;
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
        ForgeDirection dir = axis;
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

    boolean isUnconnected(Coord at) {
        return !isConnected(at, axis) && !isConnected(at, axis.getOpposite());
    }

    boolean isConnected(Coord at, ForgeDirection dir) {
        if (axis != dir && axis != dir.getOpposite()) return false;
        Coord neighbor = at.add(dir);
        if (!(neighbor.getBlock() instanceof BlockShaft)) {
            TileEntity te = neighbor.getTE();
            IShaftPowerSource shaft = KineticProxy.cast(te);
            return shaft != null && shaft.canConnect(dir.getOpposite());
        }
        return at.getBlock() == this;
    }

    public static void propagateVelocity(IShaftPowerSource src, Coord at, ForgeDirection dir) {
        ForgeDirection dirAxis = normalizeDirection(dir);
        double angularVelocity = src.getAngularVelocity(dir);
        int i_speed = (int) (Math.abs(angularVelocity) / Math.PI / 20);
        byte speedMd = i_speed > MAX_SPEED ? MAX_SPEED : (byte) i_speed;
        while (dir.offsetY != 0 || at.blockExists()) {
            Block atBlock = at.getBlock();
            if (!(atBlock instanceof BlockShaft)) break;
            if (((BlockShaft) atBlock).axis != dirAxis) break;
            int origMd = at.getMd();
            if (origMd == speedMd) break;
            at.setMd(speedMd, true);
            at.adjust(dir);
        }
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, Block block, int md) {
        super.breakBlock(w, x, y, z, block, md); // Yes, we want the TE invalidation here
        Coord at = new Coord(w, x, y, z);
        ForgeDirection dir = axis;
        invalidateLine(at.copy(), dir);
        invalidateLine(at.copy(), dir.getOpposite());
    }

    public static ForgeDirection normalizeDirection(ForgeDirection dir) {
        return SpaceUtil.sign(dir) == -1 ? dir.getOpposite() : dir;
    }

    void invalidateLine(Coord at, ForgeDirection dir) {
        if (dir == null || dir == ForgeDirection.UNKNOWN) {
            dir = axis;
        }
        ForgeDirection normal = normalizeDirection(dir);
        if (normal != axis) {
            shafts[normal.ordinal()].invalidateLine(at, dir);
            return;
        }
        while (true) {
            at.adjust(dir);
            if (!(at.getBlock() instanceof BlockShaft)) {
                at.notifyBlockChange();
                break;
            }
            if (at.getBlock() != this) break;
            at.setMd(0, true);
            TileEntity te = at.forceGetTE();
            if (te instanceof TileEntityShaftUpdater) te.invalidate();
        }
    }


}
