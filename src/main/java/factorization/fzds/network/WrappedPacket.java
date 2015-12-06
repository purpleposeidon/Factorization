package factorization.fzds.network;

import com.google.common.collect.BiMap;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.io.IOException;
import java.util.List;

public abstract class WrappedPacket implements IFzdsShenanigans, Packet {
    public static void registerPacket() {
        EnumConnectionState.PLAY.registerPacket(EnumPacketDirection.CLIENTBOUND, WrappedPacketFromServer.class);
        EnumConnectionState.PLAY.registerPacket(EnumPacketDirection.SERVERBOUND, WrappedPacketFromClient.class);
        int clientBound = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, new WrappedPacketFromServer());
        int serverBound = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.SERVERBOUND, new WrappedPacketFromClient());
        Core.logSevere("clientBound FZDS packet ID: " + clientBound);
        Core.logSevere("serverBound FZDS packet ID: " + serverBound);
    }
    
    Packet wrapped = null;
    List<Packet<INetHandlerPlayClient>> wrappedList = null;
    boolean localPacket = true;

    public WrappedPacket() {
    }
    
    public WrappedPacket(Packet wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void readPacketData(PacketBuffer data) {
        wrapped = unwrapPacket(data);
        localPacket = false;
    }
    
    private Packet unwrapPacket(PacketBuffer buf) {
        // NORELEASE: Do the state-switch thing instead of the wrap thing
        // NORELEASE: Is this packet queue thing gonna be a problem?
        PacketBuffer pb = new PacketBuffer(buf);
        int packetId = pb.readVarIntFromBuffer();
        if (packetId == -1) {
            Core.logWarning("Recieved null packet");
            return null;
        }
        Packet recieved_packet = Packet.generatePacket(getPacketMap(), packetId);
        if (recieved_packet == null) {
            Core.logWarning("Bad packet ID " + packetId);
            return null;
        }
        try {
            recieved_packet.readPacketData(buf);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (recieved_packet instanceof S3FPacketCustomPayload) {
            return new FMLProxyPacket((S3FPacketCustomPayload) recieved_packet);
        }
        if (recieved_packet instanceof C17PacketCustomPayload) {
            return new FMLProxyPacket((C17PacketCustomPayload) recieved_packet);
        }
        return recieved_packet;
    }

    protected abstract EnumPacketDirection getDirection();
    protected abstract EnumConnectionState getPacketMap();

    @Override
    public void writePacketData(PacketBuffer data) {
        PacketBuffer buff = new PacketBuffer(data);
        if (wrapped == null) {
            buff.writeVarIntToBuffer(-1);
            return;
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket pp = (FMLProxyPacket) wrapped;
            if (getDirection() == EnumPacketDirection.CLIENTBOUND) {
                try {
                    wrappedList = pp.toS3FPackets();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                wrapped = pp.toC17Packet();
            }
        }
        Integer packetId = getPacketMap().inverse().get(wrapped.getClass());
        if (packetId == null || packetId == -1) {
            if (packetId == null) {
                Core.logSevere("Can't send unregistered packet: " + wrappedToString());
            } else {
                Core.logSevere("I'm using the -1 ID for myself! Can't send this packet: " + wrappedToString());
            }
            wrapped = null;
            buff.writeVarIntToBuffer(-1);
            return;
        }
        buff.writeVarIntToBuffer(packetId);
        try {
            wrapped.writePacketData(buff);
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME: Uh oh. Should we do data.clear() here?
        }
    }

    private String wrappedToString() {
        if (wrapped == null) return "NULL";
        String info = wrapped.getClass().getName();
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket p = (FMLProxyPacket) wrapped;
            info += " channel:" + p.channel();
        }
        info += " serializes:" + wrapped.serialize();
        info += " toString:" + wrapped.toString();
        return info;
    }

    @Override
    public String serialize() {
        return getClass().getSimpleName() + ":" + wrappedToString();
    }
}
