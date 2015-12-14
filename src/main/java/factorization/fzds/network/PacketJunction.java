package factorization.fzds.network;

import factorization.fzds.Hammer;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.NORELEASE;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.io.IOException;
import java.net.SocketAddress;

public class PacketJunction extends SimpleChannelInboundHandler<Object> implements ChannelOutboundHandler, IFzdsShenanigans {
    private static final Packet WRAP_ON = new MarkerPacket(), WRAP_OFF = new MarkerPacket();
    private static final AttributeKey<PacketJunction> junction = AttributeKey.valueOf("factorization:fzdsPacketJunction");
    private static final String CHANNEL_ENABLE_WRAP = "FZ9";
    private static final String CHANNEL_DISABLE_WRAP = "FZ8";

    public static void setup(FMLNetworkEvent event, boolean isLocal) {
        PacketJunction handler = new PacketJunction(isLocal);
        Channel channel = event.manager.channel();
        ChannelPipeline pipeline = channel.pipeline();
        channel.attr(junction).set(handler);
        // Not so sure about this.
        // When we're transmitting, we want to be last (err, unless the transmitter is part of the pipeline.)
        // And if we're receiving, we want to be first?
        pipeline.addFirst(handler);
    }

    /**
     * Use this to enable & disable wrapping. You'll probably want to send WRAP_OFF in a finally clause!
     */
    public static void switchJunction(NetHandlerPlayServer net, boolean state) {
        net.sendPacket(state ? WRAP_ON : WRAP_OFF);
    }

    public static void switchJunction(NetHandlerPlayClient net, boolean state) {
        net.addToSendQueue(state ? WRAP_ON : WRAP_OFF);
    }

    private final boolean isLocal;
    private PacketJunction(boolean isLocal) {
        super(); // This is the auto-releasing version. Whatever that means.
        this.isLocal = isLocal;
    }


    private boolean readingWrapped = false; // There won't be any threading issues with this, right?
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String channel = null;
        if (msg instanceof C17PacketCustomPayload) {
            C17PacketCustomPayload packet = (C17PacketCustomPayload) msg;
            channel = packet.getChannelName();
        }
        if (msg instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) msg;
            channel = packet.getChannelName();
        }
        if (channel != null) {
            if (CHANNEL_ENABLE_WRAP.equals(channel)) msg = WRAP_ON;
            if (CHANNEL_DISABLE_WRAP.equals(channel)) msg = WRAP_OFF;
        }
        if (msg == WRAP_ON) {
            readingWrapped = true;
            return;
        }
        if (msg == WRAP_OFF) {
            readingWrapped = false;
            return;
        }
        if (readingWrapped) {
            INetHandler inh = ctx.attr(NetworkRegistry.NET_HANDLER).get();
            EntityPlayer player = null;
            if (inh instanceof NetHandlerPlayServer) {
                NetHandlerPlayServer netHandler = (NetHandlerPlayServer) inh;
                player = netHandler.playerEntity;
            }
            Hammer.proxy.queueUnwrappedPacket(player, msg);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    private boolean lastWriteState = false;
    private boolean nextWriteState = false;
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == WRAP_ON || msg == WRAP_OFF) {
            nextWriteState = msg == WRAP_ON;
            return;
        }
        if (lastWriteState != nextWriteState) {
            writeJunctionStateChange(ctx);
        }
        ctx.write(msg, promise);
    }

    void writeJunctionStateChange(ChannelHandlerContext ctx) {
        lastWriteState = nextWriteState;
        if (isLocal) {
            ctx.write(nextWriteState ? WRAP_ON : WRAP_OFF);
        } else {
            String channel = nextWriteState ? CHANNEL_ENABLE_WRAP : CHANNEL_DISABLE_WRAP;
            PacketBuffer empty = new PacketBuffer(Unpooled.buffer(0));
            ctx.write(new S3FPacketCustomPayload(channel, empty));
        }
        NORELEASE.fixme("Write nextWriteState");
    }

    private static class MarkerPacket implements Packet<INetHandler> {
        @Override
        public void readPacketData(PacketBuffer buf) throws IOException {
            throw new IOException("Unsupported!");
        }

        @Override
        public void writePacketData(PacketBuffer buf) throws IOException {
            throw new IOException("Unsupported!");
        }

        @Override
        public void processPacket(INetHandler handler) {
            // Well, I guess that's okay.
        }
    }


    // Boiler plate!? Is this right?
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
