package factorization.common;

import java.util.TreeMap;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;

public class TileEntityWire extends TileEntityCommon implements IChargeConductor {
    public byte supporting_side;
    private boolean extended_wire = false;
    Charge charge = new Charge();

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEADWIRE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Wire;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    byte getExtraInfo() {
        return supporting_side;
    }

    @Override
    void useExtraInfo(byte b) {
        supporting_side = b;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("side", supporting_side);
        charge.writeToNBT(tag, "charge");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag, "charge");
        supporting_side = tag.getByte("side");
    }

    boolean find_support() {
        if (!extended_wire) {
            return false;
        }
        for (byte side = 0; side < 6; side++) {
            if (canPlaceAgainst(getCoord().towardSide(side), side)) {
                supporting_side = side;
                return true;
            }
            //			if (here.towardSide(side).isSolidOnSide(Coord.oppositeSide(side))) {
            //				supporting_side = side;
            //				return true;
            //			}
        }
        return false;
    }

    boolean is_directly_supported() {
        Coord supporter = getCoord().towardSide(supporting_side);
        if (!supporter.blockExists()) {
            return true; //block isn't loaded, so just hang tight.
        }
        if (supporter.isSolidOnSide(supporting_side)) {
            return true;
        }
        return false;
    }

    boolean is_supported() {
        if (is_directly_supported()) {
            return true;
        }
        Coord supporter = getCoord().towardSide(supporting_side);
        TileEntityWire parent = supporter.getTE(TileEntityWire.class);
        if (parent != null) {
            extended_wire = true;
            return parent.is_supported();
        }
        return false;
    }

    @Override
    boolean canPlaceAgainst(Coord supporter, int side) {
        if (supporter.isSolidOnSide(side)) {
            return true;
        }
        TileEntityWire parent = supporter.getTE(TileEntityWire.class);
        if (parent != null) {
            if (parent.is_directly_supported()) {
                if (parent.supporting_side == side || parent.supporting_side == CubeFace.oppositeSide(side)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateEntity() {
        charge.update(this);

        if (!is_supported() && !find_support()) {
            Core.registry.factory_block.dropBlockAsItem(worldObj, xCoord, yCoord, zCoord, BlockClass.Wire.md, 0);
            getCoord().setId(0);
        }
    }

    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        switch (side) {
        case 0:
            side = 1;
            break;
        case 1:
            side = 0;
            break;
        case 4:
            side = 5;
            break;
        case 5:
            side = 4;
            break;
        case 3:
            side = 2;
            break;
        case 2:
            side = 3;
            break;
        }
        supporting_side = (byte) side;

        TreeMap<Byte, Byte> nmap = new TreeMap();
        for (Coord c : getCoord().getNeighborsAdjacent()) {
            TileEntityWire n = c.getTE(TileEntityWire.class);
            if (n == null) {
                continue;
            }
            Byte b = nmap.get(n.supporting_side);
            b = (b == null) ? 0 : b;
            b++;
            nmap.put(n.supporting_side, b);
        }
        if (nmap.size() == 1) {
            byte proper = nmap.keySet().iterator().next();

            if (supporting_side != proper) {
                byte old = supporting_side;
                supporting_side = proper;
                if (!is_directly_supported()) {
                    supporting_side = old;
                }
            }
        }
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }


    @Override
    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3D startVec,
            Vec3D endVec) {
        return new WireConnections(this).collisionRayTrace(w, x, y, z, startVec, endVec);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        setBlockBounds(Core.registry.resource_block);
        AxisAlignedBB ret = Core.registry.resource_block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
        Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
        return ret;
    }

    @Override
    public void setBlockBounds(Block b) {
        new WireConnections(this).setBlockBounds(b);
    }
}
