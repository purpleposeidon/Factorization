package factorization.beauty;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.IMeterInfo;
import factorization.api.IRotationalEnergySource;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.*;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;
import java.util.ArrayList;

public class TileEntityShaft extends TileEntityCommon implements IRotationalEnergySource, IMeterInfo {
    ForgeDirection axis = ForgeDirection.UP;
    IRotationalEnergySource _src = null;
    Coord srcPos = null;
    double angle = 0, prev_angle = 0;
    ForgeDirection srcConnection = ForgeDirection.UNKNOWN;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        axis = data.as(Share.VISIBLE, "axis").putEnum(axis);
        if (srcPos == null) srcPos = getCoord();
        srcPos = data.as(Share.VISIBLE, "src").put(srcPos);
        srcConnection = data.as(Share.VISIBLE, "connectDir").putEnum(srcConnection);
        if (data.isReader()) {
            _src = null;
        }
    }

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        if (srcPos != null) {
            srcPos.w = world;
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SHAFT;
    }

    @Override
    public boolean canUpdate() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    @Override
    public void updateEntity() {
        prev_angle = angle;
        IRotationalEnergySource src = getSrc();
        if (src == null) return;
        angle += src.getVelocity(srcConnection);
    }

    public IRotationalEnergySource getSrc() {
        if (_src == null) {
            if (srcPos == null) return null;
            if (srcPos.equals(getCoord())) {
                _src = find();
                broadcastMessage(null, getDescriptionPacket());
                if (_src == null) {
                    _src = this;
                    return null;
                }
                return _src;
            }
            TileEntity te = srcPos.getTE();
            _src = KineticProxy.cast(te);
            if (_src == null) {
                _src = this;
            }
        }
        if (_src == this) {
            return null;
        }
        if (_src.isTileEntityInvalid()) {
            _src = this;
        }
        return _src;
    }

    private IRotationalEnergySource find() {
        srcPos = new Coord(this);
        Coord at = getCoord();
        IRotationalEnergySource found = find(at.copy(), axis.getOpposite());
        if (found != null) return found;
        return find(at, axis);
    }

    private IRotationalEnergySource find(Coord at, ForgeDirection dir) {
        while (true) {
            at.adjust(dir);
            if (at.isAir()) return null;
            TileEntity te = at.getTE();
            if (te instanceof TileEntityShaft && !te.isInvalid()) {
                TileEntityShaft shaft = (TileEntityShaft) te;
                if (shaft.axis != axis) return null;
                continue;
            }
            IRotationalEnergySource ret = KineticProxy.cast(te);
            if (ret != null) {
                srcConnection = dir.getOpposite();
                srcPos = at.copy();
            }
            return ret;
        }
    }

    @Override
    public boolean canConnect(ForgeDirection direction) {
        return direction == axis || direction == axis.getOpposite();
    }

    @Override
    public double availableEnergy(ForgeDirection direction) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.availableEnergy(direction);
    }

    @Override
    public double takeEnergy(ForgeDirection direction, double maxPower) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.takeEnergy(direction, maxPower);
    }

    @Override
    public double getVelocity(ForgeDirection direction) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.getVelocity(direction);
    }

    @Override
    public boolean isTileEntityInvalid() {
        return this.isInvalid();
    }

    @Override
    public void setBlockBounds(Block b) {
        ForgeDirection dir = axis;
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == ForgeDirection.SOUTH) {
            b.setBlockBounds(l, l, 0, h, h, 1);
        } else if (dir == ForgeDirection.EAST) {
            b.setBlockBounds(0, l, l, 1, h, h);
        } else { // UP
            b.setBlockBounds(l, 0, l, h, 1, h);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        ForgeDirection dir = axis;
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == ForgeDirection.SOUTH) {
            return AxisAlignedBB.getBoundingBox(xCoord + l, yCoord + l, zCoord + 0, xCoord + h, yCoord + h, zCoord + 1);
        } else if (dir == ForgeDirection.EAST) {
            return AxisAlignedBB.getBoundingBox(xCoord + 0, yCoord + l, zCoord + l, xCoord + 1, yCoord + h, zCoord + h);
        } else { // UP
            return AxisAlignedBB.getBoundingBox(xCoord + l, yCoord + 0, zCoord + l, xCoord + h, yCoord + 1, zCoord + h);
        }
    }

    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        // TODO: This belongs... somewhere else
        BlockRenderHelper block = worldObj.isRemote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
        setBlockBounds(block);
        return block.collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
    }

    @Override
    public void neighborChanged(Block neighbor) {
        invalidateConnections();
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        invalidateConnections();
    }

    private static ThreadLocal<Boolean> working = new ThreadLocal<Boolean>();
    private void invalidateConnections() {
        if (working.get() != null) return;
        working.set(true);
        try {
            _src = null;
            srcPos = new Coord(this);
            Coord at = getCoord();
            invalidateLine(at.copy(), axis);
            invalidateLine(at, axis.getOpposite());
        } finally {
            working.remove();
        }
    }

    void invalidateLine(Coord at, ForgeDirection dir) {
        while (true) {
            at.adjust(dir);
            TileEntityShaft shaft = at.getTE(TileEntityShaft.class);
            if (shaft != null && shaft.axis == axis) {
                shaft._src = null;
                shaft.srcPos = new Coord(shaft);
                shaft.getSrc();
            } else {
                break;
            }
        }
    }

    public static ForgeDirection normalizeDirection(ForgeDirection dir) {
        return SpaceUtil.sign(dir) == -1 ? dir.getOpposite() : dir;
    }

    public void setAxis(ForgeDirection axis) {
        this.axis = normalizeDirection(axis);
    }

    private boolean isUnconnected() {
        ForgeDirection back = axis.getOpposite();
        boolean a = KineticProxy.cast(getCoord().add(axis).getTE()) == null;
        boolean b = KineticProxy.cast(getCoord().add(back).getTE()) == null;
        return a && b;
    }


    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        joinAdjacent(player, side);
        invalidateConnections();
    }

    private void joinAdjacent(EntityPlayer player, int side) {
        ForgeDirection defaultDirection = ForgeDirection.getOrientation(side);

        Coord at = getCoord();
        ArrayList<Coord> lockedNeighbors = new ArrayList<Coord>();
        ArrayList<Coord> freeNeighbors = new ArrayList<Coord>();
        ForgeDirection lockedDirection = ForgeDirection.UNKNOWN, free_direction = ForgeDirection.UNKNOWN;
        TileEntityShaft free_neighbor = null;
        for (ForgeDirection neighborDirection : ForgeDirection.VALID_DIRECTIONS) {
            Coord neighbor = at.add(neighborDirection);
            TileEntityShaft tes = at.getTE(TileEntityShaft.class);
            if (tes != null) {
                {
                    IRotationalEnergySource isrc = tes.getSrc();
                    if (isrc != null && (isrc.getVelocity(tes.axis) + isrc.getVelocity(tes.axis.getOpposite())) > 0) {
                        continue;
                    }
                }
                if (tes.isUnconnected()) {
                    freeNeighbors.add(neighbor);
                    free_neighbor = tes;
                    free_direction = neighborDirection;
                } else if (tes.axis == neighborDirection || tes.axis == neighborDirection.getOpposite()) {
                    lockedDirection = tes.axis;
                    lockedNeighbors.add(neighbor);
                }
                continue;
            }
            IRotationalEnergySource res = KineticProxy.cast(neighbor.getTE());
            if (res == null) continue;
            if (res.canConnect(neighborDirection.getOpposite())) {
                lockedDirection = neighborDirection.getOpposite();
                lockedNeighbors.add(neighbor);
            }
        }
        int n = lockedNeighbors.size() + freeNeighbors.size();
        if (n != 1 || player.isSneaking()) {
            setAxis(defaultDirection);
            return;
        }
        if (!lockedNeighbors.isEmpty()) {
            setAxis(lockedDirection);
            return;
        }
        if (free_neighbor == null) {
            setAxis(defaultDirection);
            return;
        }
        free_neighbor.setAxis(free_direction);
        setAxis(free_direction);
    }

    @Override
    public void spawnPacketReceived() {
        // Will happen multiple times when a chunk gets sent tho
        if (srcPos == null) return;
        Coord at = getCoord();
        TileEntityShaft shaft = this;
        while (true) {
            at.adjust(axis);
            TileEntityShaft tes;
            if ((tes = at.getTE(TileEntityShaft.class)) == null) {
                at.adjust(axis.getOpposite());
                break;
            }
            shaft = tes;
        }
        double baseAngle = shaft.angle;
        while (true) {
            at.adjust(axis.getOpposite());
            TileEntityShaft tes = at.getTE(TileEntityShaft.class);
            if (tes == null) break;
            tes.angle = baseAngle;
        }
    }

    @Override
    public String getInfo() {
        return "Axis: " + axis + "\nSrc: " + getSrc() + "\nSrcpos: " + srcPos + "\nEq: " + (_src == this);
    }
}
