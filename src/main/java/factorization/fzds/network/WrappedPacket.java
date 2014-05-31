package factorization.fzds.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import com.google.common.collect.BiMap;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import factorization.fzds.Hammer;
import factorization.shared.Core;

public class WrappedPacket  implements IMessage, IMessageHandler<WrappedPacket, IMessage> {
    /**
     * These fields hold the packet maps.
     * See {@link net.minecraft.util.MessageDeserializer.decode(ChannelHandlerContext, ByteBuf, List)}
     */
    static final BiMap<Integer, Class> serverPacketMap = EnumConnectionState.PLAY.func_150755_b();
    static final BiMap<Integer, Class> clientPacketMap = EnumConnectionState.PLAY.func_150753_a();
    
    Packet wrapped = null;
    public WrappedPacket() {
    }
    
    public WrappedPacket(Packet wrapped) {
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket pp = (FMLProxyPacket) wrapped;
            wrapped = pp.toS3FPacket();
        }
        this.wrapped = wrapped;
    }
    
    @Override
    public IMessage onMessage(WrappedPacket message, MessageContext ctx) {
        if (message.wrapped == null) return null;
        Hammer.proxy.setShadowWorld(); //behold my power of voodoo
        try {
            message.wrapped.processPacket(ctx.netHandler); //who do?
        } finally {
            Hammer.proxy.restoreRealWorld(); //You do.
        }
        return null;
    }

    @Override
    public void fromBytes(ByteBuf data) {
        PacketBuffer buf = new PacketBuffer(data);
        int packetId = buf.readVarIntFromBuffer();
        wrapped = Packet.generatePacket(serverPacketMap, packetId);
        if (wrapped == null) {
            Core.logWarning("Bad packet ID " + packetId);
            return;
        }
        try {
            wrapped.readPacketData(buf);
        } catch (Exception e /* IOException. Compiler derpage. */) {
            e.printStackTrace();
            wrapped = null;
        }
    }

    @Override
    public void toBytes(ByteBuf data) {
        if (wrapped == null) {
            return;
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
    public String toString() {
        return super.toString() + " of " + wrapped;
    }

}
