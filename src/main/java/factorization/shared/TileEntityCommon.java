package factorization.shared;

import cpw.mods.fml.common.event.FMLModIdMappingEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.api.datahelpers.DataOutNBT;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.migration.MigrationHelper;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.util.ItemUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class TileEntityCommon extends TileEntity implements ICoord, IFactoryType {
    public static final byte serialization_version = 2;
    public static final String serialization_version_key = ".";

    public String customName = null;

    public abstract BlockClass getBlockClass();

    public abstract void putData(DataHelper data) throws IOException;
    
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ForgeDirection dir) {
        return BlockIcons.error;
    }

    @Override
    public FMLProxyPacket getDescriptionPacket() {
        ByteBuf buf = Unpooled.buffer();
        DataOutByteBuf data = new DataOutByteBuf(buf, Side.SERVER);
        try {
            MessageType.FactoryType.write(buf);
            buf.writeInt(xCoord);
            buf.writeInt(yCoord);
            buf.writeInt(zCoord);
            int ftId = getFactoryType().md;
            buf.writeByte(ftId);
            putData(data);
            return FzNetDispatch.generate(buf);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void onRemove() {
        if (this instanceof IChargeConductor) {
            ((IChargeConductor) this).getCharge().remove();
        }
    }

    protected void sendFullDescription(EntityPlayer player) {
    }

    public boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        return true;
    }
    
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        if (is != null && is.hasTagCompound()) {
            loadFromStack(is);
        }
    }
    
    public void loadFromStack(ItemStack is) {
        customName = ItemUtil.getCustomItemName(is);
    }
    
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        return worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }
    
    public void click(EntityPlayer entityplayer) {
    }

    /** Called when there's a block update. */
    public void neighborChanged() {
    }

    public void neighborChanged(Block neighbor) {
        neighborChanged();
    }
    
    private long pulseTime = -1000;
    
    public void pulse() {
        Coord here = getCoord();
        if (here.w.isRemote) {
            return;
        }
        long now = worldObj.getTotalWorldTime();
        if (pulseTime + 4 >= now) {
            return;
        }
        pulseTime = now;
        here.notifyNeighbors();
        here.scheduleUpdate(4);
    }
    
    public boolean power() {
        return pulseTime + 4 > worldObj.getTotalWorldTime();
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        setBlockBounds(Core.registry.resource_block);
        AxisAlignedBB ret = Core.registry.resource_block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
        Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
        return ret;
    }
    
    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List list, Entity entity) {
        return false;
    }

    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        return Blocks.stone.collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
    }

    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    @Override
    public Coord getCoord() {
        return new Coord(this);
    }

    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        FactoryType type = getFactoryType();

        if (type.hasGui) {
            if (!entityplayer.worldObj.isRemote) {
                sendFullDescription(entityplayer);
                entityplayer.openGui(Core.instance, type.gui, worldObj, xCoord, yCoord, zCoord);
            }
            return true;
        }
        return false;
    }

    public boolean isBlockSolidOnSide(int side) {
        return true;
    }

    @Override
    public final void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (customName != null) {
            tag.setString("customName", customName);
        }
        if (worldObj != null && power()) {
            tag.setLong("rps", pulseTime);
        }
        tag.setByte(serialization_version_key, serialization_version);
        try {
            DataHelper data = new DataOutNBT(tag);
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public final void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("customName")) {
            customName = tag.getString("customName");
        }
        if (tag.hasKey("rps")) {
            pulseTime = tag.getLong("rps");
        }
        byte v = tag.getByte(serialization_version_key);
        if (v < serialization_version) {
            if (MigrationHelper.migrate(v, this.getFactoryType(), this, tag)) {
                needMarkDirty = true;
            }
            // By only marking dirty if MigrationHelper returns true, there is a small potential for migration to
            // happen multiple times. This is a good thing.
        }
        try {
            DataHelper data = new DataInNBT(tag);
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean needMarkDirty = false;
    @Override
    public void validate() {
        super.validate();
        if (needMarkDirty) {
            markDirty();
            needMarkDirty = false;
        }
    }

    protected final void writeBuffer(String bufferName, NBTTagCompound tag, ArrayList<ItemStack> outputBuffer) {
        if (outputBuffer.size() > 0) {
            NBTTagList buffer = new NBTTagList();
            for (ItemStack item : outputBuffer) {
                if (item == null) {
                    continue;
                }
                NBTTagCompound btag = new NBTTagCompound();
                item.writeToNBT(btag);
                buffer.appendTag(btag);
            }
            tag.setTag(bufferName, buffer);
        }
    }
    
    protected final void readBuffer(String bufferName, NBTTagCompound tag, ArrayList<ItemStack> outputBuffer) {
        outputBuffer.clear();
        if (tag.hasKey(bufferName)) {
            NBTTagList buffer = tag.getTagList(bufferName, Constants.NBT.TAG_COMPOUND);
            int bufferSize = buffer.tagCount();
            if (bufferSize > 0) {
                for (int i = 0; i < bufferSize; i++) {
                    final NBTTagCompound it = buffer.getCompoundTagAt(i);
                    outputBuffer.add(ItemStack.loadItemStackFromNBT(it));
                }
            }
        }
    }
    
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        return false;
    }

    public boolean handleMessageFromClient(MessageType messageType, ByteBuf input) throws IOException {
        // There are no base attributes a client can edit
        return false;
    }

    public void broadcastMessage(EntityPlayer who, MessageType messageType, Object... msg) {
        Core.network.broadcastMessage(who, getCoord(), messageType, msg);
    }
    
    public void broadcastMessage(EntityPlayer who, FMLProxyPacket toSend) {
        Core.network.broadcastPacket(who, getCoord(), toSend);
    }
    
    @Override
    public void invalidate() {
        if (this instanceof IChargeConductor) {
            IChargeConductor me = (IChargeConductor) this;
            me.getCharge().invalidate();
        }
        super.invalidate();
    }
    
    /** Looks like we're doing "face towards this axis" */
    public boolean rotate(ForgeDirection axis) {
        return false;
    }
    
    public static final ForgeDirection[] empty_rotation_array = new ForgeDirection[0];
    public static final ForgeDirection[] flat_rotation_array = new ForgeDirection[] { 
        ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST
    };
    public static final ForgeDirection[] full_rotation_array = new ForgeDirection[6];
    static {
        for (int i = 0; i < 6; i++) {
            full_rotation_array[i] = ForgeDirection.getOrientation(i);
        }
    }
    public ForgeDirection[] getValidRotations() {
        return empty_rotation_array;
    }
    
    //Requires the BlockClass to be MachineDynamicLightable
    public int getDynamicLight() {
        return 0;
    }
    
    public int getComparatorValue(ForgeDirection side) {
        if (this instanceof IInventory) {
            return Container.calcRedstoneFromInventory((IInventory) this);
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return getFactoryType() + " at " + getCoord();
    }
    
    
    public ItemStack getDroppedBlock() {
        return new ItemStack(Core.registry.item_factorization, 1, getFactoryType().md);
    }
    
    public ItemStack getPickedBlock() {
        return getDroppedBlock();
    }
    
    /** Called when there's a comparatory-inventory-ish update */
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {}
    
    public void representYoSelf() {
        Core.loadBus(this);
    }

    public void mappingsChanged(FMLModIdMappingEvent event) { }
    
    public void spawnDisplayTickParticles(Random rand) { }

    public boolean recolourBlock(ForgeDirection side, FzColor fzColor) { return false; }

    public void spawnPacketReceived() { }

    public void blockUpdateTick(Block myself) {
        worldObj.notifyBlockChange(xCoord, yCoord, zCoord, myself);
    }

    public boolean redrawOnSync() {
        return false;
    }
}
