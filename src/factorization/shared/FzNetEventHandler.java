package factorization.shared;

import factorization.shared.NetworkFactorization.MessageType;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.CustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

public class FzNetEventHandler {
    private static final String channelName = "FZ";
    private final FMLEventChannel channel;
    
    
    FzNetEventHandler() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }
    
    @EventHandler
    public void onPacket(ServerCustomPacketEvent event) {
        handlePacket(event, true, ((NetHandlerPlayServer) event.handler).playerEntity);
    }
    
    @EventHandler
    public void onPacket(ClientCustomPacketEvent event) {
        handlePacket(event, false, Core.proxy.getClientPlayer());
    }
    
    private void handlePacket(CustomPacketEvent event, boolean isServer, EntityPlayer player) {
        ByteBufInputStream input = new ByteBufInputStream(event.packet.payload());
        try {
            byte msgType = input.readByte();
            NetworkFactorization.MessageType mt = MessageType.fromId(msgType);
            if (mt == null) {
                throw new IOException("Unknown type: " + msgType);
            }
            switch (mt) {
            case factorizeCmdChannel:
                Core.network.handleCmd(input);
                break;
            case factorizeNtfyChannel:
                Core.network.handleNtfy(input);
                break;
            case factorizeEntityChannel:
                Core.network.handleEntity(input);
            default:
                Core.network.handleTE(input, mt);
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
