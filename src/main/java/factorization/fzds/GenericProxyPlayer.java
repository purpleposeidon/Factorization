package factorization.fzds;

import java.net.SocketAddress;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public abstract class GenericProxyPlayer extends EntityPlayerMP {
    NetworkManager networkManager = new NetworkManager(false);
    public GenericProxyPlayer(MinecraftServer server, WorldServer world, GameProfile gameProfile, ItemInWorldManager itemInWorldManager) {
        super(server, world, gameProfile, itemInWorldManager);
        //this.playerNetServerHandler = new NetHandlerPlayServer(server, networkManager, this);
    }
    public abstract void addToSendQueue(Packet packet);
}
