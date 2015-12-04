package factorization.beauty;

import factorization.api.Coord;
import factorization.api.IRotationalEnergySource;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;

public class TileEntityShaft extends TileEntityCommon implements IRotationalEnergySource {
    EnumFacing axis = EnumFacing.UP;
    IRotationalEnergySource _src = null;
    Coord srcPos = null;
    double angle = 0, prev_angle = 0;
    EnumFacing srcConnection = null;
    boolean useCustomVelocity = false;
    double customVelocity = 0;
    byte velocitySign = 1;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        axis = data.as(Share.VISIBLE, "axis").putEnum(axis);
        if (srcPos == null) srcPos = getCoord();
        srcPos = data.as(Share.VISIBLE, "src").putIDS(srcPos);
        srcConnection = data.as(Share.VISIBLE, "connectDir").putEnum(srcConnection);
        if (data.isReader()) {
            _src = null;
        }
        useCustomVelocity = data.as(Share.VISIBLE, "useCustom").putBoolean(useCustomVelocity);
        customVelocity = data.as(Share.VISIBLE, "customVel").putDouble(customVelocity);
        velocitySign = data.as(Share.VISIBLE, "velocitySign").putByte(velocitySign);
        if (velocitySign == 0) velocitySign = 1;
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
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            prev_angle = angle;
            if (useCustomVelocity) {
                angle += customVelocity * velocitySign;
                return;
            }
            IRotationalEnergySource src = getSrc();
            if (src == null) return;
            angle += src.getVelocity(srcConnection) * velocitySign;
        } else if (worldObj.getTotalWorldTime() % 20 == 0) {
            IRotationalEnergySource src = getSrc();
            if (!(src instanceof TileEntity) && src != null) {
                useCustomVelocity = true;
                double newv = src.getVelocity(srcConnection);
                if (NumUtil.significantChange(newv, customVelocity)) {
                    customVelocity = newv;
                    broadcastMessage(null, getDescriptionPacket());
                }
            } else if (useCustomVelocity) {
                useCustomVelocity = false;
                customVelocity = 0;
                broadcastMessage(null, getDescriptionPacket());
            }
        }
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
            _src = IRotationalEnergySource.adapter.cast(te);
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
        byte p = 1;
        if (axis.getDirectionVec().getX() != 0) p = -1;
        if (found != null) {
            velocitySign = p;
            return found;
        }
        velocitySign = (byte) -p;
        return find(at, axis);
    }

    private IRotationalEnergySource find(Coord at, EnumFacing dir) {
        while (true) {
            at.adjust(dir);
            if (at.isAir()) return null;
            TileEntity te = at.getTE();
            if (te instanceof TileEntityShaft && !te.isInvalid()) {
                TileEntityShaft shaft = (TileEntityShaft) te;
                if (shaft.axis != axis) return null;
                continue;
            }
            IRotationalEnergySource ret = IRotationalEnergySource.adapter.cast(te);
            if (ret != null) {
                srcConnection = dir.getOpposite();
                srcPos = at.copy();
            }
            return ret;
        }
    }

    @Override
    public boolean canConnect(EnumFacing direction) {
        return direction == axis || direction == axis.getOpposite();
    }

    @Override
    public double availableEnergy(EnumFacing direction) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.availableEnergy(direction);
    }

    @Override
    public double takeEnergy(EnumFacing direction, double maxPower) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.takeEnergy(direction, maxPower);
    }

    @Override
    public double getVelocity(EnumFacing direction) {
        IRotationalEnergySource src = getSrc();
        if (src == null) return 0;
        return src.getVelocity(direction) * velocitySign;
    }

    @Override
    public boolean isTileEntityInvalid() {
        return this.isInvalid();
    }

    @Override
    public void setBlockBounds(Block b) {
        EnumFacing dir = axis;
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == EnumFacing.SOUTH) {
            b.setBlockBounds(l, l, 0, h, h, 1);
        } else if (dir == EnumFacing.EAST) {
            b.setBlockBounds(0, l, l, 1, h, h);
        } else { // UP
            b.setBlockBounds(l, 0, l, h, 1, h);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        EnumFacing dir = axis;
        float l = 0.5F - 2F / 16F;
        float h = 1 - l;
        if (dir == EnumFacing.SOUTH) {
            return new AxisAlignedBB(pos.getX() + l, pos.getY() + l, pos.getZ() + 0, pos.getX() + h, pos.getY() + h, pos.getZ() + 1);
        } else if (dir == EnumFacing.EAST) {
            return new AxisAlignedBB(pos.getX() + 0, pos.getY() + l, pos.getZ() + l, pos.getX() + 1, pos.getY() + h, pos.getZ() + h);
        } else { // UP
            return new AxisAlignedBB(pos.getX() + l, pos.getY() + 0, pos.getZ() + l, pos.getX() + h, pos.getY() + 1, pos.getZ() + h);
        }
    }

    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        // TODO: This belongs... somewhere else
        BlockRenderHelper block = worldObj.isRemote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
        setBlockBounds(block);
        return block.collisionRayTrace(worldObj, pos.getX(), pos.getY(), pos.getZ(), startVec, endVec);
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

    void invalidateLine(Coord at, EnumFacing dir) {
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

    public static EnumFacing normalizeDirection(EnumFacing dir) {
        return SpaceUtil.sign(dir) == -1 ? dir.getOpposite() : dir;
    }

    public void setAxis(EnumFacing axis) {
        this.axis = normalizeDirection(axis);
    }

    private boolean isUnconnected() {
        EnumFacing back = axis.getOpposite();
        boolean a = IRotationalEnergySource.adapter.cast(getCoord().add(axis).getTE()) == null;
        boolean b = IRotationalEnergySource.adapter.cast(getCoord().add(back).getTE()) == null;
        return a && b;
    }


    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        joinAdjacent(player, side);
        invalidateConnections();
    }

    private void joinAdjacent(EntityPlayer player, int side) {
        EnumFacing defaultDirection = SpaceUtil.getOrientation(side);

        Coord at = getCoord();
        ArrayList<Coord> lockedNeighbors = new ArrayList<Coord>();
        ArrayList<Coord> freeNeighbors = new ArrayList<Coord>();
        EnumFacing lockedDirection = null, free_direction = null;
        TileEntityShaft free_neighbor = null;
        for (EnumFacing neighborDirection : EnumFacing.VALUES) {
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
            IRotationalEnergySource res = IRotationalEnergySource.adapter.cast(neighbor.getTE());
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
    public boolean rotate(EnumFacing axis) {
        axis = normalizeDirection(axis);
        if (axis == this.axis) return false;
        invalidateConnections();
        this.axis = axis;
        return true;
    }

    @Override
    public IIcon getIcon(EnumFacing dir) {
        return BlockIcons.beauty$shaft;
    }
}
