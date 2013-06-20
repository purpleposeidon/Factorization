package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityWire extends TileEntityCommon implements IChargeConductor {
    public byte supporting_side;
    private boolean extended_wire = false;
    Charge charge = new Charge(this);

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEADWIRE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Wire;
    }
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        return false;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        return null;
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
            if (canPlaceAgainst(null, getCoord().towardSide(side), side)) {
                supporting_side = side;
                shareInfo();
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
    boolean canPlaceAgainst(EntityPlayer player, Coord supporter, int side) {
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
        if (worldObj.isRemote) {
            return;
        }
        charge.update();
    }
    
    @Override
    public void neighborChanged() {
        if (!is_supported() /*&& !find_support()*/ ) {
            Core.registry.factory_block.dropBlockAsItem(worldObj, xCoord, yCoord, zCoord, BlockClass.Wire.md, 0);
            Coord here = getCoord();
            here.setId(0);
            here.rmTE();
        }
    }

    int getComplexity(byte new_side) {
        supporting_side = new_side;
        int complexity = new WireConnections(this).getComplexity();
        for (Coord ne : getCoord().getNeighborsAdjacent()) {
            TileEntityWire w = ne.getTE(TileEntityWire.class);
            if (w == null) {
                continue;
            }
            complexity += new WireConnections(w).getComplexity();
        }
        TileEntityWire below = getCoord().add(ForgeDirection.getOrientation(supporting_side)).getTE(TileEntityWire.class);
        if (below != null && below.supporting_side == supporting_side) {
            complexity += 16;
        }
        return complexity;
    }

    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        side = new int[] { 1, 0, 3, 2, 5, 4, }[side];
        if (player.isSneaking()) {
            supporting_side = (byte) side;
            if (is_supported()) {
                shareInfo();
                return;
            }
        }
        byte best_side = (byte) side;
        int best_complexity = getComplexity(best_side) - 1;
        if (!is_supported()) {
            best_complexity = 0x999;
        }
        for (byte s = 0; s < 6; s++) {
            if (s == side) {
                continue;
            }
            supporting_side = s;
            if (!is_supported()) {
                continue;
            }
            int test = getComplexity(s);
            if (test < best_complexity) {
                best_complexity = test;
                best_side = s;
            }
        }
        supporting_side = best_side;
        shareInfo();
    }

    void shareInfo() {
        broadcastMessage(null, MessageType.WireFace, supporting_side);
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }

    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        return new WireConnections(this).collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        return null;
//		setBlockBounds(Core.registry.resource_block);
//		AxisAlignedBB ret = Core.registry.resource_block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
//		Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
//		return ret;
    }

    @Override
    public void setBlockBounds(Block b) {
        new WireConnections(this).setBlockBounds(b);
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.WireFace) {
            byte new_side = input.readByte();
            if (new_side != supporting_side) {
                supporting_side = new_side;
                getCoord().redraw();
            }
            return true;
        }
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.wire;
    }
}
