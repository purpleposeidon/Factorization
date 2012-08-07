package factorization.common;

import java.util.Random;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Packet;
import net.minecraft.src.TileEntity;
import factorization.api.Coord;
import factorization.api.ICoord;
import factorization.api.IFactoryType;
import factorization.common.NetworkFactorization.MessageType;

public abstract class TileEntityCommon extends TileEntity implements ICoord, IFactoryType {
    static Random rand = new Random();
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
}
