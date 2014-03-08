package factorization.shared;

import factorization.shared.NetworkFactorization.MessageType;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.CustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

public class FzNetEventHandler {
    static final String channelName = "FZ";
    static FMLEventChannel channel;
    
    
    FzNetEventHandler() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }
    
    @SubscribeEvent
    public void onPacket(ServerCustomPacketEvent event) {
        handlePacket(event, true, ((NetHandlerPlayServer) event.handler).playerEntity);
    }
    
    @SubscribeEvent
    public void onPacket(ClientCustomPacketEvent event) {
        handlePacket(event, false, (EntityPlayerMP) Core.proxy.getClientPlayer());
    }
    
    private void handlePacket(CustomPacketEvent event, boolean isServer, EntityPlayerMP player) {
        ByteBufInputStream input = new ByteBufInputStream(event.packet.payload());
        try {
            MessageType mt = MessageType.read(input);
            switch (mt) {
            case factorizeCmdChannel:
                Core.network.handleCmd(input, player);
                break;
            case factorizeNtfyChannel:
                Core.network.handleNtfy(input, player);
                break;
            case factorizeEntityChannel:
                Core.network.handleEntity(input, player);
            default:
                Core.network.handleTE(input, mt, player);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeByteBuffer(input);
        }
    }
    
    public static void closeByteBuffer(ByteBufInputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
