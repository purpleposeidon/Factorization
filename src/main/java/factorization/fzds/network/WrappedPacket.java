package factorization.fzds.network;

import com.google.common.collect.BiMap;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public abstract class WrappedPacket extends Packet implements IFzdsShenanigans {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer.decode(ChannelHandlerContext, ByteBuf, List)}
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
            throw new RuntimeException("Packet " + server_packet_id + " is already registered!");
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
        Packet recieved_packet = Packet.generatePacket(getPacketMap(), packetId);
        if (recieved_packet == null) {
            Core.logWarning("Bad packet ID " + packetId);
            return null;
        }
        try {
            recieved_packet.readPacketData(buf);
        } catch (Throwable e) {
            // TODO: This can go away; just to make the compiler happy...
            e.printStackTrace();
            return null;
        }
        return recieved_packet;
    }

    protected abstract BiMap<Integer, Class> getPacketMap();

    @Override
    public void writePacketData(PacketBuffer data) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket pp = (FMLProxyPacket) wrapped;
            wrapped = pp.toS3FPacket();
        }
        PacketBuffer buff = new PacketBuffer(data);
        Integer packetId = getPacketMap().inverse().get(wrapped.getClass());
        if (packetId == null) {
            throw new IllegalArgumentException("Can't send unregistered packet: " + wrapped.serialize());
        }
        buff.writeVarIntToBuffer(packetId);
        try {
            wrapped.writePacketData(buff);
        } catch (Exception e /* IOException. Compiler derpage. */) {
            e.printStackTrace();
            // FIXME: Uh oh. Should we do data.clear() here?
        }
    }



}
