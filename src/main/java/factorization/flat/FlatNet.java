package factorization.flat;

import com.google.common.collect.Queues;
import factorization.api.Coord;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatVisitor;
import factorization.net.FzNetDispatch;
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
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
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
            Coord at = readCoord(buf, player);
            EnumFacing side = readSide(buf);
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
        clientBuff.add(event.packet.payload());
    }

    final Queue<ByteBuf> clientBuff = Queues.newConcurrentLinkedQueue();
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void readClientPackets(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        while (true) {
            ByteBuf buf = clientBuff.poll();
            if (buf == null) return;
            byte kind = buf.readByte();
            if (kind == SYNC) {
                syncRead(buf, mc.thePlayer);
            } else if (kind == FX_BREAK || kind == FX_PLACE) {
                Coord at = readCoord(buf, mc.thePlayer);
                EnumFacing side = readSide(buf);
                FlatFace face = readFace(buf);
                if (face == null) continue;
                if (kind == FX_BREAK) {
                    face.spawnParticle(at, side);
                    face.playSound(at, side, true);
                } else {
                    face.playSound(at, side, false);
                }
            } else {
                log("Invalid packet ID " + kind);
            }
        }
    }

    public static byte INTERACT_HIT = 1, INTERACT_USE = 2, SYNC = 3, FX_PLACE = 4, FX_BREAK = 5;

    public static void fx(Coord at, EnumFacing side, FlatFace face, byte type) {
        ByteBuf buff = prepare(type);
        writeCoord(buff, at);
        writeSide(buff, side);
        writeFace(buff, face);
        FMLProxyPacket packet = build(buff);
        sendAround(at.getChunk(), packet);
    }

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
        private final ByteBuf buff = prepare(SYNC);

        @Override
        public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
            // It's sort of terrible to use C-style strings instead of pascal-style, but I'd have to count them...
            buff.writeByte(1);
            writeCoord(buff, at);
            writeSide(buff, side);
            writeFace(buff, face);
        }

        public ByteBuf finish() {
            buff.writeByte(0);
            return buff;
        }
    }

    private static Coord readCoord(ByteBuf buf, EntityPlayer player) {
        Coord at = new Coord(player);
        at.readFromStream(buf);
        return at;
    }

    private static EnumFacing readSide(ByteBuf buf) {
        int ord = buf.readByte();
        return EnumFacing.getFront(ord);
    }

    private static void writeCoord(ByteBuf buff, Coord at) {
        at.writeToStream(buff);
    }

    private static void writeSide(ByteBuf buff, EnumFacing side) {
        buff.writeByte(side.ordinal());
    }

    private static void writeFace(ByteBuf buff, FlatFace face) {
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

    static void syncRead(ByteBuf buff, EntityPlayer player) {
        while (true) {
            byte state = buff.readByte();
            if (state == 0) return;
            if (state != 1) {
                log("Corrupt data sync packet!?");
                return;
            }
            Coord at = readCoord(buff, player);
            EnumFacing side = readSide(buff);
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
        FzNetDispatch.addPacketFrom(toSend, chunk);
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
    @SubscribeEvent
    public void playerWatchesChunk(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.player.worldObj.getChunkFromChunkCoords(event.chunk.chunkXPos, event.chunk.chunkZPos);
        IExtraChunkData ecd = (IExtraChunkData) chunk;
        FlatChunkLayer layer = ecd.getFlatLayer();
        if (layer.isEmpty()) return;
        SyncWrite sw = new SyncWrite();
        layer.iterate(sw);
        send(event.player, build(sw.finish()));
    }
}
