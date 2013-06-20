package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.ICoord;
import factorization.api.IFactoryType;
import factorization.common.NetworkFactorization.MessageType;

public abstract class TileEntityCommon extends TileEntity implements ICoord, IFactoryType {
    static Random rand = new Random();
    protected String customName = null;

    public abstract BlockClass getBlockClass();
    
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.error;
    }

    @Override
    public Packet getDescriptionPacket() {
        //Wow. Would you people please *stop* fucking with the names?
        return getAuxillaryInfoPacket();
    }
    
    public Packet getAuxillaryInfoPacket() {
        Packet p = Core.network.TEmessagePacket(getCoord(), MessageType.FactoryType, getFactoryType().md, getExtraInfo(), getExtraInfo2());
        p.isChunkDataPacket = true;
        return p;
    }

    void onRemove() {
        if (this instanceof IChargeConductor) {
            ((IChargeConductor) this).getCharge().remove();
        }
    }

    byte getExtraInfo() {
        return 0;
    }

    byte getExtraInfo2() {
        return 0;
    }

    void useExtraInfo(byte b) {
    }

    void useExtraInfo2(byte b) {
    }

    void sendFullDescription(EntityPlayer player) {
    }

    boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        return true;
    }

    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        customName = FactorizationUtil.getCustomItemName(is);
    }
    
    boolean removeBlockByPlayer(EntityPlayer player) {
        return Core.registry.resource_block.removeBlockByPlayer(worldObj, player, xCoord, yCoord, zCoord);
    }

    public void neighborChanged() {
    }
    
    private static TileEntityCommon pulsingTE = null;
    
    public void pulse() {
        Coord here = getCoord();
        if (here.w.isRemote) {
            return;
        }
        pulsingTE = this;
        here.notifyNeighbors();
        here.scheduleUpdate(4);
        pulsingTE = null;
    }
    
    public boolean power() {
        return this == pulsingTE;
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
        return Block.stone.collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
    }

    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    protected Packet getDescriptionPacketWith(Object... args) {
        Object[] suffix = new Object[args.length + 3];
        suffix[0] = getFactoryType().md;
        suffix[1] = getExtraInfo();
        suffix[2] = getExtraInfo2();
        for (int i = 0; i < args.length; i++) {
            suffix[i + 3] = args[i];
        }
        Packet p = Core.network.TEmessagePacket(getCoord(), MessageType.FactoryType, suffix);
        p.isChunkDataPacket = true;
        return p;
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
    
    public boolean takeUpgrade(ItemStack is) {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("ver", Core.version);
        getBlockClass().enforceQuiet(getCoord()); //NOTE: This won't actually work for the quiting save; but a second save'll take care of that.
        if (customName != null) {
            tag.setString("customName", customName);
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("customName")) {
            customName = tag.getString("customName");
        }
    }

    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        return false;
    }

    public boolean handleMessageFromClient(int messageType, DataInputStream input) throws IOException {
        // There are no base attributes a client can edit
        return false;
    }

    protected void broadcastMessage(EntityPlayer who, int messageType, Object... msg) {
        Core.network.broadcastMessage(who, getCoord(), messageType, msg);
    }
    
    protected void broadcastMessage(EntityPlayer who, Packet toSend) {
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
    
    public boolean rotate(ForgeDirection axis) {
        return false;
    }
    
    static ForgeDirection[] empty_rotation_array = new ForgeDirection[0];
    static ForgeDirection[] flat_rotation_array = new ForgeDirection[] { 
        ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST
    };
    static ForgeDirection[] full_rotation_array = new ForgeDirection[6];
    static {
        for (int i = 0; i < 6; i++) {
            full_rotation_array[i] = ForgeDirection.getOrientation(i);
        }
    }
    public ForgeDirection[] getValidRotations() {
        return empty_rotation_array;
    }
}
