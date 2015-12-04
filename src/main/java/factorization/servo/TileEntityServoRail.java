package factorization.servo;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityCommon;
import factorization.util.ItemUtil;
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
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileEntityServoRail extends TileEntityCommon implements IChargeConductor {
    public static final float width = 7F/16F;
    
    Charge charge = new Charge(this);
    public Decorator decoration = null;
    public byte priority = 0;
    String comment = "";
    FzColor color = FzColor.NO_COLOR;
    
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
    public void updateEntity() {
        charge.update();
    }
    
    private static final String decor_tag_key = "decor";

    @Override
    public void putData(DataHelper data) throws IOException {
        charge.serialize("", data);
        priority = data.as(Share.VISIBLE, "priority").putByte(priority);
        color = data.as(Share.VISIBLE, "color").putEnum(color);
        comment = data.as(Share.VISIBLE, "rem").putString(comment);
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
        TileEntity te = worldObj.getTileEntity(pos.getX() + dir.getDirectionVec().getX(), pos.getY() + dir.getDirectionVec().getY(), pos.getZ() + dir.getDirectionVec().getZ());
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
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
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
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
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
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        if (count == 0) {
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        return true;
    }
    
    @Override
    public boolean addCollisionBoxesToList(Block ignore, AxisAlignedBB aabb, List list, Entity entity) {
        if (decoration != null && decoration.collides()) {
            float f = decoration.getSize();
            boolean remote = (entity != null && entity.worldObj != null) ? entity.worldObj.isRemote : FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
            BlockRenderHelper block = remote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, pos.getX(), pos.getY(), pos.getZ());
            if (aabb == null || aabb.intersectsWith(a)) {
                list.add(a);
                return true;
            }
        }
        //return getCollisionBoxes(aabb, list, entity);
        return false;
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        return null;
    }
    
    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        ArrayList<AxisAlignedBB> boxes = new ArrayList(4);
        getCollisionBoxes(null, boxes, null);
        Block b = FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER ? Core.registry.serverTraceHelper : Core.registry.clientTraceHelper;
        for (AxisAlignedBB ab : boxes) {
            ab = ab.getOffsetBoundingBox(-pos.getX(), -pos.getY(), -pos.getZ());
            float d = 1F/16F;
            b.setBlockBounds((float) ab.minX - d, (float) ab.minY - d, (float) ab.minZ - d, (float) ab.maxX + d, (float) ab.maxY + d, (float) ab.maxZ + d);
            MovingObjectPosition mop = b.collisionRayTrace(worldObj, pos.getX(), pos.getY(), pos.getZ(), startVec, endVec);
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
    public IIcon getIcon(EnumFacing dir) {
        return BlockIcons.servo$rail;
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
        if (comment.length() > 0) {
            if (info.length() > 0) {
                info += "\n";
            }
            info += EnumChatFormatting.ITALIC + comment;
        }
        if (color != FzColor.NO_COLOR) {
            info += "\n" + color;
        }
        if (info.length() > 0 || powered) {
            Notice notice = new Notice(here, StringUtils.strip(info, "\n"));
            if (powered) {
                notice.withItem(new ItemStack(Blocks.redstone_torch));
                notice.withStyle(Style.DRAWITEM);
            }
            notice.send(player);
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
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.ServoRailDecor) {
            color = FzColor.fromOrdinal(input.readByte());
            comment = input.readBoolean() ? "x" : null;
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
        if (messageType == MessageType.ServoRailEditComment) {
            comment = ByteBufUtils.readUTF8String(input);
            FMLCommonHandler.instance().showGuiScreen(new GuiCommentEditor(this));
            return true;
        }
        return false;
    }
    
    @Override
    public boolean handleMessageFromClient(MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == MessageType.ServoRailEditComment) {
            comment = ByteBufUtils.readUTF8String(input);
            return true;
        }
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
}
