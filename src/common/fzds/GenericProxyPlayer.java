package factorization.fzds;

import java.net.SocketAddress;
import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public abstract class GenericProxyPlayer extends EntityPlayerMP implements INetworkManager {

    public GenericProxyPlayer(MinecraftServer server, World world, String playername, ItemInWorldManager itemManager) {
        super(server, world, playername, itemManager);
        this.playerNetServerHandler = new NetServerHandler(server, this, this);
    }

    //INetworkManager implementation -- or whatever this is.
    @Override
    public void setNetHandler(NetHandler netHandler) { }

    @Override
    public abstract void addToSendQueue(Packet packet);
    
    @Override
    public void wakeThreads() { }

    @Override
    public void processReadPackets() { }

    @Override
    public SocketAddress getSocketAddress() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "<Proxying Player: " + GenericProxyPlayer.this.toString() + ">";
            }
        };
    }

    @Override
    public void serverShutdown() { }

    @Override
    public int packetSize() {
        //usages suggests this is used only to delay sending item map data, and that only happens if this is <= 5. Yeaaah. No.
        //The real player should be receiving that kind of details anyways. Besides, PPP won't be carrying items.
        return 10;
    }

    @Override
    public void networkShutdown(String str, Object... args) { }

    @Override
    public void closeConnections() { }
    

}
