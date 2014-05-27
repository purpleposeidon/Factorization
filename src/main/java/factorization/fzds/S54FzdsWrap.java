package factorization.fzds;

import java.io.IOException;
import java.net.Socket;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import com.google.common.collect.BiMap;

public class S54FzdsWrap extends Packet {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer.decode(ChannelHandlerContext, ByteBuf, List)}
     */
    static final BiMap<Integer, Class> serverPacketMap = EnumConnectionState.PLAY.func_150755_b();
    static final BiMap<Integer, Class> clientPacketMap = EnumConnectionState.PLAY.func_150753_a();
    
    Packet wrapped = null;
    
    public S54FzdsWrap() {
        
    }
    
    public S54FzdsWrap(Packet toWrap) {
        this.wrapped = toWrap;
    }
    
    public static void registerPacket() {
        // FIXME: It's probably possible to setup a netty channel that's just as, or possibly even more, efficient than this.
        final int startPacketId = 0x54;
        final int naturalMax = 0x80;
        for (int pid = startPacketId; pid < naturalMax; pid++) {
            if (tryInjectPacketAt(pid)) return;
        }
        for (int pid = 0; pid < startPacketId; pid++) {
            if (tryInjectPacketAt(pid)) return;
        }
        for (int pid = naturalMax; pid < Short.MAX_VALUE; pid++) {
            if (tryInjectPacketAt(pid)) return;
        }
        throw new IllegalArgumentException("Out of packet IDs!?");
    }
    
    private static boolean tryInjectPacketAt(int id) {
        if (serverPacketMap.containsKey(id)) return false;
        serverPacketMap.put(id, S54FzdsWrap.class);
        //NORELEASE...
        return true;
    }
    
    @Override
    public void readPacketData(PacketBuffer buff) throws IOException {
        int packetId = buff.readVarIntFromBuffer();
        wrapped = Packet.generatePacket(serverPacketMap, packetId);
        if (wrapped == null) throw new IOException("Bad packet ID " + packetId);
        wrapped.readPacketData(buff);
    }

    @Override
    public void writePacketData(PacketBuffer buff) throws IOException {
        if (wrapped == null) throw new IOException("Not actually wrapping a packet");
        Integer packetId = serverPacketMap.inverse().get(wrapped);
        if (packetId == null) throw new IOException("Can't send unregistered packet: " + wrapped.serialize());
        buff.writeVarIntToBuffer(packetId);
        wrapped.writePacketData(buff);
    }

    @Override
    public void processPacket(INetHandler netHandler) {
        Hammer.proxy.setShadowWorld(); //behold my power of voodoo
        try {
            wrapped.processPacket(netHandler); //who do?
        } finally {
            Hammer.proxy.restoreRealWorld(); //You do.
        }
    }
    
    @Override
    public String toString() {
        return super.toString() + " of " + wrapped;
    }
    
    @Override
    public String serialize() {
        if (wrapped == null) {
            return "wrapped=1, null";
        }
        return "wrapped=1, " + wrapped.serialize();
    }

}
