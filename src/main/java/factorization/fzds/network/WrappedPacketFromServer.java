package factorization.fzds.network;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.Hammer;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;

public class WrappedPacketFromServer extends WrappedPacket {
    public WrappedPacketFromServer(Packet towrap) {
        super(towrap);
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
            NetHandlerPlayClient nhpc = (NetHandlerPlayClient) netHandler;
            NetworkDispatcher dispatcher = nhpc.getNetworkManager().channel().attr(NetworkDispatcher.FML_DISPATCHER).get();
            fml.setTarget(Side.CLIENT);
            fml.setDispatcher(dispatcher);
            if (fml.payload().readerIndex() != 0) {
                Core.logSevere("Packet double-processing detected! Channel: " + fml.channel());
                return;
            }
            needReset = localPacket;
        }
        Hammer.proxy.setShadowWorld(); //behold my power of voodoo
        try {
            boolean b = Hammer.proxy.guiCheckStart();
            wrapped.processPacket(netHandler); //who do?
            Hammer.proxy.guiCheckEnd(b);
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
