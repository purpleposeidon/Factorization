package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.DeltaCoord;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityExtension extends TileEntityCommon {
    private TileEntityCommon _parent = null;
    private DeltaCoord pc;
    
    public TileEntityExtension() { }
    public TileEntityExtension(TileEntityCommon parent) {
        this._parent = parent;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.EXTENDED;
    }

    @Override
    public BlockClass getBlockClass() {
        TileEntityCommon p = getParent();
        if (p == null) {
            return BlockClass.Default;
        }
        return p.getBlockClass();
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        TileEntityCommon p = getParent();
        if (p != null) {
            p.onRemove();
        }
    }
    
    @Override
    public boolean canUpdate() {
        return false;
    }
    
    public TileEntityCommon getParent() {
        if (_parent != null && _parent.isInvalid()) {
            setParent(null);
            _parent = null;
            getCoord().setId(0);
        }
        if (_parent == null && pc != null) {
            _parent = getCoord().add(pc).getTE(TileEntityCommon.class);
            if (_parent == null || _parent.getClass() == TileEntityExtension.class) {
                setParent(null);
            }
        }
        return _parent;
    }
    
    public void setParent(TileEntityCommon newParent) {
        if (newParent == null || newParent.getClass() == TileEntityExtension.class) {
            _parent = null;
            pc = null;
            return;
        }
        this._parent = newParent;
        pc = newParent.getCoord().difference(this.getCoord());
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (pc != null) {
            pc.writeToTag("p", tag);
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        pc = DeltaCoord.readFromTag("p", tag);
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        TileEntityCommon p = getParent();
        if (p == null) {
            return super.getCollisionBoundingBoxFromPool();
        }
        return p.getCollisionBoundingBoxFromPool();
    }
    
    @Override
    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List list, Entity entity) {
        TileEntityCommon p = getParent();
        if (p == null) {
            return super.addCollisionBoxesToList(block, aabb, list, entity);
        }
        return p.addCollisionBoxesToList(block, aabb, list, entity);
    }
    
    @Override
    public void validate() {
        super.validate();
        if (this._parent != null && pc == null) {
            setParent(_parent);
        }
    }
    
    @Override
    public Packet getAuxillaryInfoPacket() {
        if (pc == null) {
            return super.getAuxillaryInfoPacket();
        }
        return getDescriptionPacketWith(MessageType.ExtensionInfo, pc);
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.ExtensionInfo) {
            pc = DeltaCoord.read(input);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        TileEntityCommon p = getParent();
        if (p != null) {
            return p.activate(entityplayer, side);
        }
        return false;
    }
    
    @Override
    public void neighborChanged() {
        TileEntityCommon p = getParent();
        if (p != null) {
            p.neighborChanged();
        }
    }
    
    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        TileEntityCommon p = getParent();
        if (p != null) {
            MovingObjectPosition ret = p.collisionRayTrace(startVec, endVec);
            if (!(p instanceof TileEntityGreenware)) {
                //hax
                if (ret != null && ret.typeOfHit == EnumMovingObjectType.TILE) {
                    ret.blockX = xCoord;
                    ret.blockY = yCoord;
                    ret.blockZ = zCoord;
                }
            }
            return ret;
        }
        return super.collisionRayTrace(startVec, endVec);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        TileEntityCommon p = getParent();
        if (p == null) {
            return super.getIcon(dir);
        }
        return p.getIcon(dir);
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        TileEntityCommon p = getParent();
        if (p == null) {
            return null;
        }
        return p.getDroppedBlock();
    }
}

