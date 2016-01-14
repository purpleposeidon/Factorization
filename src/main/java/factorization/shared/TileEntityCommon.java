package factorization.shared;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.api.datahelpers.DataOutNBT;
import factorization.common.FactoryType;
import factorization.migration.MigrationHelper;
import factorization.net.*;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class TileEntityCommon extends TileEntity implements ICoord, IFactoryType, INet {
    public static final byte serialization_version = 2;
    public static final String serialization_version_key = ".";

    public String customName = null;


    public abstract BlockClass getBlockClass();

    public abstract void putData(DataHelper data) throws IOException;

    @Override
    public final Coord getCoord() {
        return new Coord(this);
    }

    @Override
    public String toString() {
        return getFactoryType() + " at " + getCoord();
    }

    public void representYoSelf() {
        Core.loadBus(this);
    }

    public void mappingsChanged(FMLModIdMappingEvent event) { }


    @Override
    public FMLProxyPacket getDescriptionPacket() {
        ByteBuf buf = Unpooled.buffer();
        DataOutByteBuf data = new DataOutByteBuf(buf, Side.SERVER);
        try {
            NetworkFactorization.writeMessage(buf, FzNetEventHandler.TO_BLOCK, StandardMessageType.TileFzType);
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
            int ftId = getFactoryType().md;
            buf.writeByte(ftId);
            putData(data);
            return FzNetDispatch.generate(buf);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void loadFromStack(ItemStack is) {
        customName = ItemUtil.getCustomItemName(is);
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
                TeMigrationDirtier.instance.register(this);
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

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        return false;
    }

    @Override
    public boolean handleMessageFromClient(Enum messageType, ByteBuf input) throws IOException {
        // There are no base attributes a client can edit
        return false;
    }

    public void broadcastMessage(EntityPlayer who, Enum messageType, Object... msg) {
        Core.network.broadcastMessage(who, this, messageType, msg);
    }

    public void broadcastMessage(EntityPlayer who, FMLProxyPacket toSend) {
        Core.network.broadcastPacket(who, getCoord(), toSend);
    }

    public void spawnPacketReceived() { }

    public boolean redrawOnSync() {
        return false;
    }


    protected void onRemove() {
        if (this instanceof IChargeConductor) {
            ((IChargeConductor) this).getCharge().remove();
        }
    }

    public boolean canPlaceAgainst(EntityPlayer player, Coord c, EnumFacing side) {
        return true;
    }

    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (is != null && is.hasTagCompound()) {
            loadFromStack(is);
        }
    }

    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        return worldObj.setBlockToAir(pos);
    }

    public void click(EntityPlayer entityplayer) {
    }

    /** Called when there's a block update. */
    public void neighborChanged() {
    }

    /** Called when an adjacent TE changes */
    public void neighborChanged(Block neighbor) {
        neighborChanged(); // NORELEASE ???
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
        return worldObj != null ? pulseTime + 4 > worldObj.getTotalWorldTime() : false;
    }

    public AxisAlignedBB getCollisionBoundingBox() {
        setBlockBounds(Core.registry.resource_block);
        AxisAlignedBB ret = Core.registry.resource_block.getCollisionBoundingBox(worldObj, pos, worldObj.getBlockState(pos));
        Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
        return ret;
    }

    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List<AxisAlignedBB> list, Entity entity) {
        return false;
    }

    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        return Blocks.bedrock.collisionRayTrace(worldObj, pos, startVec, endVec);
    }

    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
        FactoryType type = getFactoryType();

        if (type.hasGui) {
            if (!entityplayer.worldObj.isRemote) {
                entityplayer.openGui(Core.instance, type.gui, worldObj, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }

    public boolean isBlockSolidOnSide(EnumFacing side) {
        return true;
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
    public boolean rotate(EnumFacing axis) {
        return false;
    }

    public static final EnumFacing[] empty_rotation_array = new EnumFacing[0];
    public static final EnumFacing[] flat_rotation_array = new EnumFacing[] {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };
    public static final EnumFacing[] full_rotation_array = new EnumFacing[6];
    static {
        for (int i = 0; i < 6; i++) {
            full_rotation_array[i] = SpaceUtil.getOrientation(i);
        }
    }
    public EnumFacing[] getValidRotations() {
        return empty_rotation_array;
    }

    //Requires the BlockClass to be MachineDynamicLightable
    public int getDynamicLight() {
        return 0;
    }

    public int getComparatorValue(EnumFacing side) {
        if (this instanceof IInventory) {
            return Container.calcRedstoneFromInventory((IInventory) this);
        }
        return 0;
    }


    public ItemStack getDroppedBlock() {
        return getFactoryType().itemStack();
    }

    public ItemStack getPickedBlock() {
        return getDroppedBlock();
    }

    /** Called when there's a comparatory-inventory-ish update */
    @Deprecated // NORELEASE: Figure this stuff out.
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {}

    public boolean recolourBlock(EnumFacing side, FzColor fzColor) { return false; }


    public void spawnDisplayTickParticles(Random rand) { }

    public void blockUpdateTick(Block myself) {
        worldObj.notifyBlockOfStateChange(pos, myself);
    }

    @Override
    public Enum[] getMessages() {
        return null;
    }
}
