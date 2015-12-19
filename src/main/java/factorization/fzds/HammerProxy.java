package factorization.fzds;

import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.network.InteractionLiason;
import factorization.shared.Core;
import factorization.shared.NORELEASE;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

public class HammerProxy {
    // Clients should use DeltaChunk
    public World getClientRealWorld() { return null; }

    public EntityPlayer getRealPlayerWhileInShadow() { return null; }

    public EntityPlayer getFakePlayerWhileInShadow() { return null; }

    public void setShadowWorld() {
        throw new RuntimeException("Tried to setShadowWorld on server");
    }

    public void restoreRealWorld() {
        throw new RuntimeException("Tried to restoreRealWorld on server");
    }

    public boolean isInShadowWorld() { return false; }
    
    public void clientInit() { }
    
    void registerStuff() { }
    
    void updateRayPosition(DseRayTarget ray) { }
    
    public MovingObjectPosition getShadowHit() { return null; }
    
    IDeltaChunk getHitIDC() { return null; }

    public void createClientShadowWorld() { }

    public void cleanupClientWorld() { }

    public boolean guiCheckStart() {
        return false;
    }

    public void guiCheckEnd(boolean oldState) {

    }

    public boolean queueUnwrappedPacket(EntityPlayer player, Object packetObj) {
        if (packetObj == null) return true;
        if (player == null) {
            if (packetObj instanceof C00PacketKeepAlive || packetObj instanceof S00PacketKeepAlive) {
                NORELEASE.fixme("Shouldn't this, like, not happen?");
                return true;
            }
            NORELEASE.println("No player to handle: " + packetObj + " " + packetObj.getClass().getSimpleName());
            return true;
        }
        if (player.worldObj.isRemote) return false;
        Packet packet = (Packet) packetObj;
        // May possibly need to inject the liason into things up higher, during reading. Somehow. Hopefulloy won't be needed.
        InteractionLiason liason = InteractionLiason.activeLiasons.get(player);
        if (liason == null) {
            if (!(packet instanceof C00PacketKeepAlive)) {
                // Seems our system isn't perfect. :/
                // Keepalives are generated from a different thread I guess.
                Hammer.logWarning("Liasonless wrapped packet: " + packet.getClass().getSimpleName() + " " + packet.toString());
                return true;
            }
        }
        NetHandlerPlayServer handler = liason.playerNetServerHandler;
        if (packet instanceof FMLProxyPacket) {
            FMLProxyPacket fml = (FMLProxyPacket) packet;
            NetworkDispatcher dispatcher = handler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).get();
            fml.setTarget(Side.CLIENT);
            fml.setDispatcher(dispatcher);
            if (fml.payload().readerIndex() != 0) {
                Core.logSevere("Packet double-processing detected! Channel: " + fml.channel());
                return true;
            }
        }
        packet.processPacket(handler);
        return true;
    }
}
