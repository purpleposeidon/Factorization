package factorization.net;

import factorization.api.Coord;
import factorization.artifact.ContainerForge;
import factorization.common.Command;
import factorization.shared.Core;
import factorization.shared.Sound;
import factorization.utiligoo.ItemGoo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.CustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.io.IOException;

public class FzNetEventHandler {
    static final String channelName = "FZ";
    static FMLEventChannel channel;

    public FzNetEventHandler() {
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

    public static final byte TO_TILEENTITY = 0, TO_ENTITY = 1, TO_PLAYER = 2, TO_BLOCK = 3;

    private void handlePacket(CustomPacketEvent event, boolean isServer, EntityPlayer player) {
        try {
            handlePacket0(event, isServer, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePacket0(CustomPacketEvent event, boolean isServer, EntityPlayer player) throws IOException {
        ByteBuf input = event.packet.payload();
        byte target = input.readByte();
        if (target == TO_TILEENTITY) {
            BlockPos at = new BlockPos(input.readInt(), input.readInt(), input.readInt());
            if (!player.worldObj.isBlockLoaded(at)) return;
            TileEntity te = player.worldObj.getTileEntity(at);
            if (te == null) {
                Core.logWarning("Tried to send packet to TileEntity that is not located at " + new Coord(player.worldObj, at));
                return;
            }
            if (!(te instanceof INet)) {
                Core.logWarning("Tried to send packet to TileEntity that can not handle our packets at " + new Coord(player.worldObj, at));
                return;
            }
            INet it = (INet) te;
            process(isServer, it, input);
        } else if (target == TO_ENTITY) {
            int entityId = input.readInt();
            Entity ent = player.worldObj.getEntityByID(entityId);
            if (ent == null) {
                Core.logWarning("Tried to send packet to entity with ID " + entityId);
                return;
            }
            if (!(ent instanceof INet)) {
                Core.logWarning("Tried to send packet to entity that can not handle our packets at " + new Coord(ent) + ", a " + ent);
                return;
            }
            INet it = (INet) ent;
            process(isServer, it, input);
        } else if (target == TO_PLAYER) {
            Enum mt = NetworkFactorization.readMessage(input, null);
            if (mt == StandardMessageType.UtilityGooState) {
                ItemGoo.handlePacket(input);
            } else if (mt == StandardMessageType.ArtifactForgeName) {
                String name = ByteBufUtils.readUTF8String(input);
                String lore = ByteBufUtils.readUTF8String(input);
                if (player.openContainer instanceof ContainerForge) {
                    ContainerForge forge = (ContainerForge) player.openContainer;
                    forge.forge.name = name;
                    forge.forge.lore = lore;
                    forge.forge.markDirty();
                    forge.detectAndSendChanges();
                }
            } else if (mt == StandardMessageType.ArtifactForgeError) {
                String err = ByteBufUtils.readUTF8String(input);
                if (player.openContainer instanceof ContainerForge) {
                    ContainerForge forge = (ContainerForge) player.openContainer;
                    forge.forge.error_message = err;
                    input.readBytes(forge.forge.warnings);
                }
            } else if (mt == StandardMessageType.playerCommand) {
                byte s = input.readByte();
                int arg = input.readInt();
                Command.fromNetwork(player, s, arg);
            } else {
                Core.logWarning("Can not send TO_PLAYER: " + mt);
            }
        } else if (target == TO_BLOCK) {
            Coord at = new Coord(player.worldObj, input.readInt(), input.readInt(), input.readInt());
            if (!at.blockExists()) return;
            Enum mt = NetworkFactorization.readMessage(input, null);
            if (mt == StandardMessageType.RedrawOnClient) {
                at.redraw();
            } else if (mt == StandardMessageType.PlaySound) {
                Sound.receive(at, input);
            } else {
                Core.logWarning("Can't handle " + mt + " to " + at);
            }
        } else {
            Core.logWarning("Packet with invalid destination type id: " + target);
        }
    }

    private static void process(boolean isServer, INet it, ByteBuf input) throws IOException {
        Enum messageType = NetworkFactorization.readMessage(input, it);
        if (isServer) {
            it.handleMessageFromClient(messageType, input);
        } else {
            it.handleMessageFromServer(messageType, input);
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
