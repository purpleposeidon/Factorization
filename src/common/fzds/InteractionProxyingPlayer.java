package factorization.fzds;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;

public class InteractionProxyingPlayer extends GenericProxyPlayer {
    EntityPlayerMP orig;
    
    public InteractionProxyingPlayer(MinecraftServer server, EntityPlayerMP orig, ItemInWorldManager itemManager) {
        super(server, DeltaChunk.getServerShadowWorld(), "FzdsClick", itemManager);
        this.orig = orig;
        if (orig instanceof InteractionProxyingPlayer) {
            throw new IllegalArgumentException("No nesting");
        }
    }

    @Override
    public void addToSendQueue(Packet packet) {
        Packet wrappedPacket = new Packet220FzdsWrap(packet);
        orig.playerNetServerHandler.sendPacketToPlayer(wrappedPacket);
    }

}
