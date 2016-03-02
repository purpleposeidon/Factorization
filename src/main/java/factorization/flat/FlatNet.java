package factorization.flat;

import factorization.api.Coord;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatVisitor;
import factorization.shared.Core;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FlatNet {
    static final String channelName = "fzFlat";
    static FMLEventChannel channel;
    static final FlatNet instance = new FlatNet();

    public void init() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
        Core.loadBus(this);
    }

    @SubscribeEvent
    public void packetToServer(FMLNetworkEvent.ServerCustomPacketEvent event) {
        ByteBuf buf = event.packet.payload();
        byte kind = buf.readByte();
        NetHandlerPlayServer handler = (NetHandlerPlayServer) event.handler;
        EntityPlayerMP player = handler.playerEntity;
        if (kind == INTERACT_HIT || kind == INTERACT_USE) {
            Coord at = new Coord(player.worldObj, 0, 0, 0);
            at.readFromStream(buf);
            EnumFacing side = EnumFacing.getFront(buf.readByte());
            if (!at.blockExists()) return;
            double reachSq = 6 * 6; // FIXME: There's no way to get this properly on the server?
            if (at.distanceSq(player) > reachSq) {
                log("Ignoring distant interaction packet for " + at + " from " + player);
                return;
            }
            FlatFace face = Flat.get(at, side);
            if (kind == INTERACT_HIT) {
                face.onHit(at, side, player);
            } else {
                face.onActivate(at, side, player);
            }
        } else {
            log("Invalid packet ID " + kind + " from " + player);
        }
    }

    @SubscribeEvent
    public void packetToClient(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf buf = event.packet.payload();
        byte kind = buf.readByte();
        if (kind == SYNC) {
            Minecraft mc = Minecraft.getMinecraft();
            syncRead(buf, mc.thePlayer);
        } else {
            log("Invalid packet ID " + kind);
        }
    }

    static byte INTERACT_HIT = 1, INTERACT_USE = 2, SYNC = 3;

    static ByteBuf prepare(byte packetType) {
        ByteBuf ret = Unpooled.buffer();
        ret.writeByte(packetType);
        return ret;
    }

    static FMLProxyPacket playerInteract(EntityPlayer player, Coord at, EnumFacing side, boolean useElseHit) {
        ByteBuf buff = prepare(useElseHit ? INTERACT_USE : INTERACT_HIT);
        at.writeToStream(buff);
        buff.writeByte(side.ordinal());
        return build(buff);
    }

    static FMLProxyPacket syncChunk(EntityPlayer player, Coord at) {
        IExtraChunkData ecd = (IExtraChunkData) at.getChunk();
        FlatChunkLayer data = ecd.getFlatLayer();
        SyncWrite writer = new SyncWrite();
        data.iterate(writer);
        writer.finish();
        return build(writer.buff);
    }

    static class SyncWrite implements IFlatVisitor {
        final ByteBuf buff = prepare(SYNC);

        @Override
        public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
            // It's sort of terrible to use C-style strings instead of pascal-style, but I'd have to count them...
            buff.writeByte(1);
            at.writeToStream(buff);
            buff.writeByte(side.ordinal());
            buff.writeChar(face.staticId);
            if (face.isDynamic()) {
                ByteBufUtils.writeUTF8String(buff, Flat.getName(face).toString());
                try {
                    face.serialize("", new DataOutByteBuf(buff, Side.SERVER));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void finish() {
            buff.writeByte(0);
        }
    }

    static void syncRead(ByteBuf buff, EntityPlayer player) {
        Coord at = new Coord(player);
        while (true) {
            byte state = buff.readByte();
            if (state == 0) return;
            if (state != 1) {
                log("Corrupt data sync packet!?");
                return;
            }
            at.readFromStream(buff);
            EnumFacing side = EnumFacing.getFront(buff.readByte());
            FlatFace face = readFace(buff);
            if (face == null) continue;
            Flat.set(at, side, face);
        }
    }

    @Nullable
    private static FlatFace readFace(ByteBuf buff) {
        char staticId = buff.readChar();
        if (staticId != FlatMod.DYNAMIC_SENTINEL) {
            return FlatMod.staticReg.getObjectById(staticId);
        }
        String name = ByteBufUtils.readUTF8String(buff);
        FlatFace dyn = Flat.getDynamic(new ResourceLocation(name));
        try {
            dyn.serialize("", new DataInByteBuf(buff, Side.CLIENT));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return dyn;
    }

    static FMLProxyPacket build(ByteBuf buff) {
        return new FMLProxyPacket(new PacketBuffer(buff), channelName);
    }

    static void send(EntityPlayer player, FMLProxyPacket toSend) {
        if (player.worldObj.isRemote) {
            channel.sendToServer(toSend);
        } else {
            channel.sendTo(toSend, (EntityPlayerMP) player);
        }
    }

    static void sendAround(Chunk chunk, FMLProxyPacket toSend) {

    }

    static void log(String msg) {
        Core.logWarning(msg);
    }

    public static final Set<FlatChunkLayer> pending = Collections.synchronizedSet(new HashSet<FlatChunkLayer>());
    @SubscribeEvent
    public void updateClients(TickEvent.ServerTickEvent event) {
        synchronized (pending) {
            for (FlatChunkLayer flat : pending) {
                flat.updateClients();
            }
            pending.clear();
        }
    }
}
