package factorization.fzds;


import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;

import com.mojang.authlib.GameProfile;

public abstract class GenericProxyPlayer extends EntityPlayerMP {
    NetworkManager networkManager = new NetworkManager(false) {
        public void channelActive(io.netty.channel.ChannelHandlerContext p_channelActive_1_) throws Exception {
            throw new IllegalArgumentException("No, go away");
        }
        
        public Channel channel() {
            return proxiedChannel;
        }
        
        Channel proxiedChannel = new ProxyingChannel(GenericProxyPlayer.this);
    };
    public GenericProxyPlayer(MinecraftServer server, WorldServer world, GameProfile gameProfile, ItemInWorldManager itemInWorldManager) {
        super(server, world, gameProfile, itemInWorldManager);
        //this.playerNetServerHandler = new NetHandlerPlayServer(server, networkManager, this);
    }
    public abstract void addToSendQueue(Packet packet);
    public abstract void addNettyMessage(Channel sourceChannel, Object msg);
}
