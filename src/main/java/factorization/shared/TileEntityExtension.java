package factorization.shared;

import factorization.api.DeltaCoord;
import factorization.api.HeatConverters;
import factorization.api.IFurnaceHeatable;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.ceramics.TileEntityGreenware;
import factorization.common.FactoryType;
import factorization.shared.NetworkFactorization.MessageType;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;

public class TileEntityExtension extends TileEntityCommon implements IFurnaceHeatable {
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
            getCoord().setAir();
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
    public void putData(DataHelper data) throws IOException {
        if (pc == null) {
            pc = new DeltaCoord();
        }
        pc.serialize("p", data.as(Share.VISIBLE, "p"));
        if (pc.equals(DeltaCoord.ZERO)) {
            pc = null;
        }
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
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
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
    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
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
                if (ret != null && ret.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    ret.blockX = pos.getX();
                    ret.blockY = pos.getY();
                    ret.blockZ = pos.getZ();
                }
            }
            return ret;
        }
        return super.collisionRayTrace(startVec, endVec);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(EnumFacing dir) {
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

    @Override
    public void representYoSelf() {
        HeatConverters.addConverter(new HeatConverters.IHeatConverter() {
            @Override
            public IFurnaceHeatable convert(World w, int x, int y, int z) {
                TileEntity te = w.getTileEntity(x, y, z);
                if (te instanceof TileEntityExtension) {
                    TileEntityExtension ex = (TileEntityExtension) te;
                    TileEntityCommon parent = ex.getParent();
                    if (parent instanceof IFurnaceHeatable) {
                        return (IFurnaceHeatable) parent;
                    }
                }
                return null;
            }
        });
    }

    IFurnaceHeatable heatableParent() {
        TileEntityCommon parent = getParent();
        if (parent instanceof IFurnaceHeatable) return (IFurnaceHeatable) parent;
        return null;
    }

    @Override
    public boolean acceptsHeat() {
        IFurnaceHeatable p = heatableParent();
        if (p == null) return false;
        return p.acceptsHeat();
    }

    @Override
    public void giveHeat() {
        IFurnaceHeatable p = heatableParent();
        if (p == null) return;
        p.giveHeat();
    }

    @Override
    public boolean hasLaggyStart() {
        IFurnaceHeatable p = heatableParent();
        if (p == null) return false;
        return p.hasLaggyStart();
    }

    @Override
    public boolean isStarted() {
        IFurnaceHeatable p = heatableParent();
        if (p == null) return false;
        return p.isStarted();
    }
}

