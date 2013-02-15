package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.AxisAlignedBB;
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
    void onRemove() {
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
        if (p == null && pc != null) {
            p = getCoord().add(pc).getTE(TileEntityCommon.class);
        }
        if (p != null) {
            return p.getCollisionBoundingBoxFromPool();
        }
        return super.getCollisionBoundingBoxFromPool();
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
    public boolean handleMessageFromServer(int messageType, DataInput input)
            throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.ExtensionInfo) {
            pc = DeltaCoord.read(input);
            return true;
        }
        return false;
    }
}

