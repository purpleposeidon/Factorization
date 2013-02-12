package factorization.common;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import factorization.api.DeltaCoord;

public class TileEntityExtension extends TileEntityCommon {
    private TileEntityCommon parent = null;
    private DeltaCoord pc;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.EXTENDED;
    }

    @Override
    public BlockClass getBlockClass() {
        if (parent == null) {
            return BlockClass.Default;
        }
        return parent.getBlockClass();
    }

    @Override
    void onRemove() {
        super.onRemove();
        if (parent != null) {
            parent.onRemove();
        }
    }
    
    @Override
    public boolean canUpdate() {
        return false;
    }
    
    @Override
    public Packet getDescriptionPacket() {
        return null;
    }
    
    public TileEntityCommon getParent() {
        if (parent == null) {
            parent = getCoord().add(pc).getTE(TileEntityCommon.class);
        }
        return parent;
    }
    
    public void setParent(TileEntityCommon parent) {
        this.parent = parent;
        pc = parent.getCoord().difference(this.getCoord());
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
}
