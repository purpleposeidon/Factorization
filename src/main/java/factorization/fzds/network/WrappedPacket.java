package factorization.fzds.network;

import com.google.common.collect.BiMap;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.Hammer;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.shared.NORELEASE;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;

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
            e.printStackTrace();
            return null;
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
        if (Hammer.log_client_chunking && (wrapped instanceof S21PacketChunkData || wrapped instanceof S26PacketMapChunkBulk)) {
            Hammer.logInfo("Packet " + wrapped.serialize());
        }
        if (Minecraft.getMinecraft().thePlayer == null) {
            return; // :/
        }
        boolean needReset = false;
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket fml = (FMLProxyPacket) wrapped;
            fml.setTarget(Side.CLIENT);
            fml.setDispatcher(Hammer.proxy.getDispatcher());
            if (fml.payload().readerIndex() != 0) {
                Core.logSevere("Packet double-processing detected! Channel: " + fml.channel());
                return;
            }
            needReset = localPacket;
        }
        Hammer.proxy.setShadowWorld(); //behold my power of voodoo
        try {
            wrapped.processPacket(netHandler); //who do?
        } finally {
            Hammer.proxy.restoreRealWorld(); //You do.
        }
        // Could alternatively make a short buffer so that we don't swap worlds so often.
        // Maybe 32 packets long, gets flushed at the end of every tick?
        // Presumably it wouldn't be a problem if packets to different shadow/real arrive at different times.

        if (needReset) {
            // A single packet may be sent to multiple PPPs. (Altho this is kind of a degenerate case.)
            // Consider the multisending of { vanilla packet, FML packet } from a { integrated SSP server, SMP server, LAN-hosting SSP server }.
            // Vanilla packets & integrated: No problems; the object just moves across threads.
            // Vanilla packets & SMP: No problem; the fields get written out multiple times
            // Vanilla packets & LAN: Not really tested, but no problems anticipated.
            // FML packets & integrated: **The byte buffer gets read in the client thread multiple times; the ByteBuf is sad.**
            // FML packets & SMP: Given the above issue, you'd think it'd be a problem, but I guess it's copying the byte buffer rather than reading
            // 					  with a similar mechanism.
            // FML packets & LAN: Same issue with integrated applies. Remote clients anticipated to have the same immunity.
            //
            // So to handle local multisending of FML packets, the bytebuf's reader index needs to be reset.
            // Reset the packet before sending? That seems alright.
            FMLProxyPacket fml = (FMLProxyPacket) wrapped;
            fml.payload().resetReaderIndex();
            // An option that would be, sigh, nice would be to have a single PPP for the entire dimension.
            // The player would register itself to every chunk watcher in shadow.
            // But unfortunately peeps send packets incorrectly by player-distance instead of properly using the subscription,
            // and Forge doesn't make it reasonable to do it the right way.
        }
    }

}
