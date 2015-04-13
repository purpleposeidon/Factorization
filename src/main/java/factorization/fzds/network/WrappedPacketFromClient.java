package factorization.fzds.network;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.Hammer;
import factorization.shared.Core;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;

public class WrappedPacketFromClient extends WrappedPacket {
    public WrappedPacketFromClient(Packet towrap) {
        super(towrap);
    }

    @Override
    public void processPacket(INetHandler handler) {
        if (wrapped == null) return;
        // May possibly need to inject the liason into things up higher, during reading. Somehow. Hopefulloy won't be needed.
        NetHandlerPlayServer nhs = (NetHandlerPlayServer) handler;

        InteractionLiason liason = InteractionLiason.activeLiasons.get(nhs.playerEntity);
        if (liason == null) {
            Hammer.logWarning("Recieved wrapped packet from client who does not have a liason: " + wrapped.serialize());
            return;
        }
        if (wrapped instanceof FMLProxyPacket) {
            FMLProxyPacket fml = (FMLProxyPacket) wrapped;
            NetHandlerPlayServer nhps = (NetHandlerPlayServer) handler;
            NetworkDispatcher dispatcher = nhps.netManager.channel.attr(NetworkDispatcher.FML_DISPATCHER).get();
            fml.setTarget(Side.CLIENT);
            fml.setDispatcher(dispatcher);
            if (fml.payload().readerIndex() != 0) {
                Core.logSevere("Packet double-processing detected! Channel: " + fml.channel());
                return;
            }
        }
        wrapped.processPacket(liason.playerNetServerHandler);
    }
}
