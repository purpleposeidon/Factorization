package factorization.fzds.network;

import com.google.common.collect.BiMap;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import java.io.IOException;

public abstract class WrappedPacket extends Packet implements IFzdsShenanigans {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer#decode}
     */
    static final BiMap<Integer, Class> serverPacketMap = EnumConnectionState.PLAY.func_150755_b();
    static final BiMap<Integer, Class> clientPacketMap = EnumConnectionState.PLAY.func_150753_a();
    
    
    static int server_packet_id = 92;
    static int client_packet_id = 92;
    public static void registerPacket() {
        if (serverPacketMap.containsKey(server_packet_id)) {
            throw new RuntimeException("Packet " + server_packet_id + " is already registered!");
        }
        serverPacketMap.put(server_packet_id, WrappedPacketFromServer.class); //server -> client packets
        EnumConnectionState.PLAY.field_150761_f.put(WrappedPacketFromServer.class, EnumConnectionState.PLAY);

        if (clientPacketMap.containsKey(client_packet_id)) {
            throw new RuntimeException("Packet " + client_packet_id + " is already registered!");
        }
        clientPacketMap.put(client_packet_id, WrappedPacketFromClient.class);
        EnumConnectionState.PLAY.field_150761_f.put(WrappedPacketFromClient.class, EnumConnectionState.PLAY);
    }
    
    Packet wrapped = null;
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
        int packetId = buf.readVarIntFromBuffer();
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

    protected abstract boolean isServerside();
    protected abstract BiMap<Integer, Class> getPacketMap();

    @Override
    public void writePacketData(PacketBuffer data) {
        PacketBuffer buff = new PacketBuffer(data);
        if (wrapped == null) {
            buff.writeVarIntToBuffer(-1);
            return;
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket pp = (FMLProxyPacket) wrapped;
            wrapped = this.isServerside() ? pp.toS3FPacket() : pp.toC17Packet();
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
        } catch (Exception e /* IOException. Compiler derpage. */) {
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
