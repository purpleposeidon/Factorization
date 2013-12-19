package factorization.servo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.notify.Notify;
import factorization.notify.Notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FactoryType;
import factorization.shared.TileEntityCommon;
import factorization.shared.NetworkFactorization.MessageType;

public class TileEntityServoRail extends TileEntityCommon implements IChargeConductor {
    public static final float width = 7F/16F;
    
    Charge charge = new Charge(this);
    Decorator decoration = null;
    public byte priority = 0;
    
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
    
    private String decor_tag_key = "decor";
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag);
        tag.setByte("priority", priority);
        if (decoration != null) {
            NBTTagCompound decor = new NBTTagCompound();
            decoration.save(decor);
            tag.setTag(decor_tag_key, decor);
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag);
        priority = tag.getByte("priority");
        if (!tag.hasKey(decor_tag_key)) {
            return;
        }
        NBTTagCompound dtag = tag.getCompoundTag(decor_tag_key);
        ServoComponent component = ServoComponent.load(dtag);
        if (component instanceof Decorator) {
            decoration = (Decorator) component;
        }
    }
    
    boolean has(ForgeDirection dir) {
        TileEntity te = worldObj.getBlockTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
        if (te instanceof TileEntityServoRail) {
            return true;
        }
        return false;
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
    public boolean addCollisionBoxesToList(Block ignore, AxisAlignedBB aabb, List list, Entity entity) {
        if (decoration != null && !(decoration instanceof Instruction)) {
            float f = decoration.getSize();
            boolean remote = (entity != null && entity.worldObj != null) ? entity.worldObj.isRemote : FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
            BlockRenderHelper block = remote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            AxisAlignedBB a = block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
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
    
    public void setDecoration(Decorator newDecor) {
        decoration = newDecor;
        if (decoration != null) {
            decoration.onPlacedOnRail(this);
        }
        onInventoryChanged();
    }
    
    public Decorator getDecoration() {
        return decoration;
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (decoration == null) {
            return false;
        }
        final Coord here = getCoord();
        boolean ret = decoration.onClick(entityplayer, here, side);
        if (!worldObj.isRemote) {
            WorldServer world = (WorldServer) worldObj;
            PlayerInstance playerInstance = world.getPlayerManager().getOrCreateChunkWatcher(xCoord >> 4, zCoord >> 4, false);
            if (playerInstance != null) {
                playerInstance.sendToAllPlayersWatchingChunk(_getDescriptionPacket(true));
            }
            String info = decoration.getInfo();
            if (here.isWeaklyPowered()) {
                Notify.withItem(new ItemStack(Block.torchRedstoneActive));
                Notify.withStyle(Style.DRAWITEM);
                if (info == null) {
                    info = "";
                }
            }
            if (info != null) {
                Notify.send(entityplayer, here, info);
            }
        }
        return ret;
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.ServoRailDecor || messageType == MessageType.ServoRailDecorUpdate) {
            if (messageType == MessageType.ServoRailDecorUpdate) {
                getCoord().redraw();
            }
            DataInputStream dis = new DataInputStream(input);
            ServoComponent sc = ServoComponent.readFromPacket(input);
            if (!(sc instanceof Decorator)) {
                return false;
            }
            decoration = (Decorator) sc;
            return true;
        }
        return false;
    }
    
    @Override
    public Packet getDescriptionPacket() {
        return _getDescriptionPacket(false);
    }
    
    public Packet _getDescriptionPacket(boolean with_update) {
        if (decoration == null) {
            return super.getDescriptionPacket();
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            try {
                decoration.writeToPacket(dos);
            } catch (Exception e) {
                Core.logWarning("Component packet error at %s %s:", this, getCoord());
                e.printStackTrace();
                decoration = null;
                Notify.withStyle(Style.FORCE, Style.LONG);
                Notify.send(null, getCoord(), "Component packet error!\nSee console log.");
                return super.getDescriptionPacket();
            }
            return getDescriptionPacketWith(with_update ? MessageType.ServoRailDecorUpdate : MessageType.ServoRailDecor, baos.toByteArray());
        }
    }
    
    @Override
    public boolean isBlockSolidOnSide(int side) {
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
}
