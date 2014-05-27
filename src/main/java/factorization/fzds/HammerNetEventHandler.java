package factorization.fzds;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.Packet;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

public enum HammerNetEventHandler {
    INSTANCE;
    
    final static String channelName = "FZDS";
    FMLEventChannel channel;
    
    HammerNetEventHandler() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }
    
    @SubscribeEvent
    public void onPacket(ServerCustomPacketEvent event) {
        
    }
}
