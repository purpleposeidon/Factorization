package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.Random;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3D;
import net.minecraft.src.World;
import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.IFactoryType;
import factorization.common.NetworkFactorization.MessageType;

public abstract class TileEntityCommon extends TileEntity implements ICoord, IFactoryType {
    static Random rand = new Random();

    public abstract BlockClass getBlockClass();

    //@Override -- can't override due to MY GOD ITS THE CLIENTS FAULT THIS TIME
    public Packet getDescriptionPacket() {
        Packet p = Core.network.messagePacket(getCoord(), MessageType.FactoryType, getFactoryType().md, getExtraInfo(), getExtraInfo2());
        p.isChunkDataPacket = true;
        return p;
    }

    void onRemove() {
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

    boolean canPlaceAgainst(Coord c, int side) {
        return true;
    }

    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
    }

    public void neighborChanged() {
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        setBlockBounds(Core.registry.resource_block);
        AxisAlignedBB ret = Core.registry.resource_block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord);
        Core.registry.resource_block.setBlockBounds(0, 0, 0, 1, 1, 1);
        return ret;
    }

    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z,
            Vec3D startVec, Vec3D endVec) {
        return Block.stone.collisionRayTrace(worldObj, xCoord, yCoord, zCoord, startVec, endVec);
    }

    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    Packet getDescriptionPacketWith(Object... args) {
        Object[] suffix = new Object[args.length + 3];
        suffix[0] = getFactoryType().md;
        suffix[1] = getExtraInfo();
        suffix[2] = getExtraInfo2();
        for (int i = 0; i < args.length; i++) {
            suffix[i + 3] = args[i];
        }
        Packet p = Core.network.messagePacket(getCoord(), MessageType.FactoryType, suffix);
        p.isChunkDataPacket = true;
        return p;
    }

    @Override
    public Coord getCoord() {
        return new Coord(this);
    }

    public boolean activate(EntityPlayer entityplayer) {
        FactoryType type = getFactoryType();

        if (type.hasGui) {
            entityplayer.openGui(Core.instance, type.gui, worldObj, xCoord, yCoord, zCoord);
            sendFullDescription(entityplayer);
            return true;
        }
        return false;
    }

    public boolean isBlockSolidOnSide(int side) {
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("ver", Core.instance.getVersion());
        getBlockClass().enforceQuiet(getCoord()); //NOTE: This won't actually work for the quiting save; but a second save'll take care of that.
    }

    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        return false;
    }

    public boolean handleMessageFromClient(int messageType, DataInput input) throws IOException {
        // There are no base attributes a client can edit
        return false;
    }

    void broadcastMessage(EntityPlayer who, int messageType, Object... msg) {
        Core.network.broadcastMessage(who, getCoord(), messageType, msg);
    }
}
