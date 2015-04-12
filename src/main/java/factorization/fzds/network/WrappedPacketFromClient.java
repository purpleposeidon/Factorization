package factorization.fzds.network;

import factorization.fzds.Hammer;
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
        wrapped.processPacket(liason.playerNetServerHandler);
    }
}
