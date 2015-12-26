package factorization.net;

import java.io.IOException;

import factorization.shared.Core;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.CustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class FzNetEventHandler {
    static final String channelName = "FZ";
    static FMLEventChannel channel;

    FzNetEventHandler() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }

    @SubscribeEvent
    public void onPacket(final ServerCustomPacketEvent event) {
        WorldServer world = ((NetHandlerPlayServer) event.handler).playerEntity.getServerForPlayer();
        if (!world.isCallingFromMinecraftThread()) {
            world.addScheduledTask(new Runnable() {
                public void run() {
                    handlePacket(event, true, ((NetHandlerPlayServer) event.handler).playerEntity);
                }
            });
        } else {
            handlePacket(event, true, ((NetHandlerPlayServer) event.handler).playerEntity);
        }
    }
    
    @SubscribeEvent
    public void onPacket(final ClientCustomPacketEvent event) {
        if (!Core.proxy.isClientThread()) {
            Core.proxy.addScheduledClientTask(new Runnable() {
                public void run() {
                    handlePacket(event, false, Core.proxy.getClientPlayer());
                }
            });
        } else {
            handlePacket(event, false, Core.proxy.getClientPlayer());
        }
    }
    
    private void handlePacket(CustomPacketEvent event, boolean isServer, EntityPlayer player) {
        ByteBuf input = event.packet.payload();
        StandardMessageType mt = StandardMessageType.read(input);
        if (mt.isPlayerMessage) {
            Core.network.handlePlayer(mt, input, player);
        } else if (mt.isEntityMessage) {
            Core.network.handleEntity(mt, input, player);
        } else {
            switch (mt) {
                case factorizeCmdChannel:
                    Core.network.handleCmd(input, player);
                    break;
                default:
                    Core.network.handleTE(input, mt, player);
                    break;
            }
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
