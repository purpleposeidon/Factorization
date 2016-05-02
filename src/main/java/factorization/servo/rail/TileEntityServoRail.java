package factorization.servo.rail;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileEntityServoRail extends TileEntityCommon implements IChargeConductor, ITickable {
    public static final float width = 7F/16F;
    
    Charge charge = new Charge(this);
    public Decorator decoration = null;
    public byte priority = 0;
    public FzColor color = FzColor.NO_COLOR;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
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
    public String getInfo() {
        return null;
    }
    
    @Override
    public void update() {
        charge.update();
    }
    
    private static final String decor_tag_key = "decor";

    @Override
    public void putData(DataHelper data) throws IOException {
        charge.serialize("", data);
        priority = data.as(Share.VISIBLE, "priority").putByte(priority);
        color = data.as(Share.VISIBLE, "color").putEnum(color);
        if (data.isReader()) {
            NBTTagCompound dtag = data.as(Share.VISIBLE, decor_tag_key).putTag(new NBTTagCompound());
            ServoComponent component = ServoComponent.load(dtag);
            if (component instanceof Decorator) {
                decoration = (Decorator) component;
            }
        } else {
            NBTTagCompound decor = new NBTTagCompound();
            if (decoration != null) {
                decoration.save(decor);
            }
            data.as(Share.VISIBLE, decor_tag_key).putTag(decor);
        }
    }
    
    boolean has(EnumFacing dir) {
        TileEntity te = worldObj.getTileEntity(pos.offset(dir));
        if (te instanceof TileEntityServoRail) {
            return true;
        }
        return false;
    }
    
    public boolean fillSideInfo(boolean[] sides) {
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            boolean flag = has(SpaceUtil.getOrientation(i));
            sides[i] = flag;
            any |= flag;
        }
        return any;
    }
    
    private boolean getCollisionBoxes(AxisAlignedBB aabb, List list, Entity entity) {
        boolean remote = (entity != null && entity.worldObj != null) ? entity.worldObj.isRemote : FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
        Block block = FzUtil.getTraceHelper();
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
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
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
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
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
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        if (count == 0) {
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        return true;
    }
    
    @Override
    public boolean addCollisionBoxesToList(Block ignore, AxisAlignedBB aabb, List<AxisAlignedBB> list, Entity entity) {
        if (decoration != null && decoration.collides()) {
            float f = decoration.getSize();
            Block block = FzUtil.getTraceHelper();
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
                return true;
            }
        }
        //return getCollisionBoxes(aabb, list, entity);
        return false;
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }
    
    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        ArrayList<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>(4);
        getCollisionBoxes(null, boxes, null);
        Block b = FzUtil.getTraceHelper();
        for (AxisAlignedBB ab : boxes) {
            ab = ab.addCoord(-pos.getX(), -pos.getY(), -pos.getZ());
            float d = 1F/16F;
            b.setBlockBounds((float) ab.minX - d, (float) ab.minY - d, (float) ab.minZ - d, (float) ab.maxX + d, (float) ab.maxY + d, (float) ab.maxZ + d);
            MovingObjectPosition mop = b.collisionRayTrace(worldObj, pos, startVec, endVec);
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

    public void setDecoration(Decorator newDecor) {
        decoration = newDecor;
        if (decoration != null) {
            decoration.onPlacedOnRail(this);
        }
        markDirty();
    }
    
    public Decorator getDecoration() {
        return decoration;
    }
    
    void showDecorNotification(EntityPlayer player) {
        String info = decoration == null ? null : decoration.getInfo();
        info = info == null ? "" : info;
        final Coord here = getCoord();
        boolean powered = here.isWeaklyPowered();
        if (color != FzColor.NO_COLOR) {
            info += "\n" + color;
        }
        if (info.length() > 0 || powered) {
            Notice notice = new Notice(here, StringUtils.strip(info, "\n"));
            if (powered) {
                notice.withItem(new ItemStack(Blocks.redstone_torch));
                notice.withStyle(Style.DRAWITEM);
            }
            notice.sendTo(player);
        }
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
        final Coord here = getCoord();
        if (worldObj.isRemote) {
            return false;
        }
        boolean ret = false;
        if (decoration != null) {
            ret = decoration.onClick(entityplayer, here, side);
        }
        if (!ret && color == FzColor.NO_COLOR) {
            FzColor newColor = FzColor.fromItem(entityplayer.getHeldItem());
            if (newColor != color) {
                color = newColor;
                if (!entityplayer.capabilities.isCreativeMode) {
                    ItemStack held = entityplayer.getHeldItem();
                    entityplayer.setCurrentItemOrArmor(0 /* held item slot */, ItemUtil.normalDecr(held));
                }
                ret = true;
            }
        }
        if (ret) {
            here.syncAndRedraw();
        }
        if (entityplayer instanceof EntityPlayerMP) {
            showDecorNotification(entityplayer);
        }
        return ret;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == ServoRailMessage.ServoRailDecor) {
            color = FzColor.fromOrdinal(input.readByte());
            boolean has_decor = input.readBoolean();
            if (has_decor) {
                ServoComponent sc = ServoComponent.readFromPacket(input);
                if (!(sc instanceof Decorator)) {
                    return false;
                }
                decoration = (Decorator) sc;
                decoration.afterClientLoad(this);
                getCoord().redraw();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException {
        return super.handleMessageFromClient(messageType, input);
    }
    
    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return false;
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        if (decoration == null) {
            return;
        }
        if (!decoration.isFreeToPlace()) {
            getCoord().spawnItem(decoration.toItem());
        }
    }
    
    @Override
    public ItemStack getPickedBlock() {
        final Decorator decoration = getDecoration();
        if (decoration != null) {
            return decoration.toItem();
        }
        return getDroppedBlock();
    }
    
    @Override
    public boolean recolourBlock(EnumFacing side, FzColor fzColor) {
        if (fzColor != color) {
            color = fzColor;
            getCoord().markBlockForUpdate();
            return true;
        }
        return false;
    }

    enum ServoRailMessage {
        ServoRailDecor;
        public static final ServoRailMessage[] VALUES = values();
    }

    @Override
    public Enum[] getMessages() {
        return ServoRailMessage.VALUES;
    }
}
