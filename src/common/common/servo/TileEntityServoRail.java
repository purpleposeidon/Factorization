package factorization.common.servo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockClass;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsEntryControl;

public class TileEntityServoRail extends TileEntityCommon implements IChargeConductor {
    public static final float width = 7F/16F;
    
    Charge charge = new Charge(this);
    ServoComponent decoration = null;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
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
    public void updateEntity() {
        charge.update();
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (decoration != null) {
            NBTTagCompound decor = decoration.save();
            if (decor != null) {
                tag.setTag("decor", decor);
            }
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagCompound dtag = tag.getCompoundTag("decor");
        ServoComponent component = ServoComponent.load(dtag);
        if (component instanceof Decorator) {
            decoration = component;
        }
    }
    
    TileEntityServoRail getAt(ForgeDirection dir) {
        TileEntity te = worldObj.getBlockTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
        if (te instanceof TileEntityServoRail) {
            return (TileEntityServoRail) te;
        }
        return null;
    }
    
    boolean has(ForgeDirection dir) {
        return getAt(dir) != null;
    }
    
    public boolean fillSideInfo(boolean[] sides) {
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            boolean flag = has(ForgeDirection.getOrientation(i));
            sides[i] = flag;
            any |= flag;
        }
        return any;
    }
    
    @Override
    public boolean addCollisionBoxesToList(Block ignore, AxisAlignedBB aabb, List list, Entity entity) {
        boolean remote = (entity != null && entity.worldObj != null) ? entity.worldObj.isRemote : FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
        BlockRenderHelper block = remote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
        boolean[] sides = new boolean[6];
        fillSideInfo(sides);
        int count = 0;
        float f = TileEntityServoRail.width;
        //Shame java doesn't have macros, huh?
        if (sides[0] || sides[1]) {
            //DOWN, UP
            count++;
            float low = sides[0] ? 0 : f;
            float high = sides[1] ? 1 : 1 - f;
            block.setBlockBounds(f, low, f, 1 - f, high, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        if (sides[2] || sides[3]) {
            //NORTH, SOUTH
            count++;
            float low = sides[2] ? 0 : f;
            float high = sides[3] ? 1 : 1 - f;
            block.setBlockBounds(f, f, low, 1 - f, 1 - f, high);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        if (sides[4] || sides[5]) {
            //WEST, EAST
            count++;
            float low = sides[4] ? 0 : f;
            float high = sides[5] ? 1 : 1 - f;
            block.setBlockBounds(low, f, f, high, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        if (count == 0) {
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        return true;
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        return null;
    }
    
    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        ArrayList<AxisAlignedBB> boxes = new ArrayList(4);
        addCollisionBoxesToList(null, null, boxes, null);
        Block b = FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER ? Core.registry.serverTraceHelper : Core.registry.clientTraceHelper;
        for (AxisAlignedBB ab : boxes) {
            ab = ab.getOffsetBoundingBox(-xCoord, -yCoord, -zCoord);
            float d = 1F/16F;
            b.setBlockBounds((float) ab.minX - d, (float) ab.minY - d, (float) ab.minZ - d, (float) ab.maxX + d, (float) ab.maxY + d, (float) ab.maxZ + d);
            MovingObjectPosition mop = b.collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
            if (mop != null) {
                return mop;
            }
        }
        
        return null;
    }
    
    @Override
    public void setBlockBounds(Block b) {
        float f = width; // - 1F/32F;
        b.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.servo$rail;
    }
}
