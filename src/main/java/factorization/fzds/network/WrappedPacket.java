package factorization.fzds.network;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import com.google.common.collect.BiMap;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.Hammer;
import factorization.fzds.api.IFzdsShenanigans;
import factorization.shared.Core;

public class WrappedPacket extends Packet implements IFzdsShenanigans {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer.decode(ChannelHandlerContext, ByteBuf, List)}
     */
    static final BiMap<Integer, Class> serverPacketMap = EnumConnectionState.PLAY.func_150755_b();
    static final BiMap<Integer, Class> clientPacketMap = EnumConnectionState.PLAY.func_150753_a();
    
    
    static int packet_id = 92;
    public static void registerPacket() {
        serverPacketMap.put(packet_id, WrappedPacket.class); //server -> client packets
        EnumConnectionState.PLAY.field_150761_f.put(WrappedPacket.class, EnumConnectionState.PLAY);
    }
    
    Packet wrapped = null;
    public WrappedPacket() {
    }
    
    public WrappedPacket(Packet wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void readPacketData(PacketBuffer data) {
        wrapped = unwrapPacket(data);
    }
    
    private static Packet unwrapPacket(PacketBuffer buf) {
        int packetId = buf.readVarIntFromBuffer();
        Packet recieved_packet = Packet.generatePacket(serverPacketMap, packetId);
        if (recieved_packet == null) {
            Core.logWarning("Bad packet ID " + packetId);
            return null;
        }
        try {
            recieved_packet.readPacketData(buf);
        } catch (Throwable e) {
            // TODO: This can go away; just to make the compiler happy...
        }
        return recieved_packet;
    }

    @Override
    public void writePacketData(PacketBuffer data) {
        if (wrapped == null) {
            return;
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket pp = (FMLProxyPacket) wrapped;
            wrapped = pp.toS3FPacket();
            // NORELEASE: What about reading?
        }
        PacketBuffer buff = new PacketBuffer(data);
        Integer packetId = serverPacketMap.inverse().get(wrapped.getClass());
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

    @Override
    public void processPacket(INetHandler netHandler) {
        if (wrapped == null) return;
        if (Minecraft.getMinecraft().thePlayer == null) {
            return; // :/
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket fml = (FMLProxyPacket) wrapped;
            fml.setTarget(Side.CLIENT);
        }
        Hammer.proxy.setShadowWorld(); //behold my power of voodoo
        try {
            wrapped.processPacket(netHandler); //who do?
        } finally {
            Hammer.proxy.restoreRealWorld(); //You do.
        }
    }

}
