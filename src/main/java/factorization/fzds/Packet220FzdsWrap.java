package factorization.fzds;

import java.io.IOException;
import java.net.Socket;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import com.google.common.collect.BiMap;

public class Packet220FzdsWrap extends Packet {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer.decode(ChannelHandlerContext, ByteBuf, List)}
     */
    static final BiMap<Integer, Class> serverPacketMap = EnumConnectionState.PLAY.func_150755_b();
    static final BiMap<Integer, Class> clientPacketMap = EnumConnectionState.PLAY.func_150753_a();
    
    Packet wrapped = null;
    
    public Packet220FzdsWrap() {
        
    }
    
    public Packet220FzdsWrap(Packet toWrap) {
        this.wrapped = toWrap;
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
