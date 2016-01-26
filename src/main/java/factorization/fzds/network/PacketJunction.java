package factorization.fzds.network;

import factorization.fzds.Hammer;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.Attribute;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.net.SocketAddress;

public class PacketJunction extends SimpleChannelInboundHandler<Object> implements ChannelOutboundHandler, IFzdsShenanigans {
    public static void setup(FMLNetworkEvent event, Side side) {
        PacketJunction handler = new PacketJunction(side);
        Channel channel = event.manager.channel();
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get("fzds:packetJunction") == null) {
            pipeline.addAfter("fml:packet_handler", "fzds:packetJunction", handler);
        }
    }

    /**
     * Use this to enable & disable wrapping. You'll probably want to send WRAP_OFF in a finally clause!
     */
    public static void switchJunction(NetHandlerPlayServer net, boolean state) {
        ensurePlay(net.netManager.channel());
        net.sendPacket(state ? WRAP_ON_S : WRAP_OFF_S);
    }

    public static void switchJunction(NetHandlerPlayClient net, boolean state) {
        ensurePlay(net.getNetworkManager().channel());
        net.addToSendQueue(state ? WRAP_ON_C : WRAP_OFF_C);
    }

    private final Side side;
    private EntityPlayer player = null;
    private PacketJunction(Side side) {
        super(Object.class, false);
        this.side = side;
    }

    private EntityPlayer getPlayer(ChannelHandlerContext ctx) {
        if (player != null) return player;
        ChannelHandler pipe = ctx.pipeline().get("packet_handler");
        NetworkManager manager = (NetworkManager) pipe;
        return player = Hammer.proxy.getPlayerFrom(manager.getNetHandler());
    }


    private boolean readingWrapped = false; // There won't be any threading issues with this, right?
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Boolean wrap = isWrapOn(msg);
        if (wrap != null) {
            readingWrapped = wrap;
            return;
        }
        if (readingWrapped) {
            Hammer.proxy.queueUnwrappedPacket(getPlayer(ctx), msg);
            return;
        }
        ensurePlay(ctx.channel());
        ctx.fireChannelRead(msg);
    }

    private boolean lastWriteState = false;
    private boolean nextWriteState = false;
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Boolean wrap = isWrapOn(msg);
        if (wrap != null) {
            nextWriteState = wrap;
            return;
        }
        if (lastWriteState != nextWriteState) {
            writeJunctionStateChange(ctx);
        }
        if (msg == null) {
            Core.logSevere("Trying to write a null message");
        }
        ensurePlay(ctx.channel());
        ctx.write(msg, promise);
    }

    private void writeJunctionStateChange(ChannelHandlerContext ctx) {
        lastWriteState = nextWriteState;
        Packet toWrite;
        if (side == Side.CLIENT) {
            toWrite = nextWriteState ? WRAP_ON_C : WRAP_OFF_C;
        } else {
            toWrite = nextWriteState ? WRAP_ON_S : WRAP_OFF_S;
        }
        ctx.write(toWrite);
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

    private static void ensurePlay(Channel channel) {
        Attribute<EnumConnectionState> attr = channel.attr(NetworkManager.attrKeyConnectionState);
        EnumConnectionState enumConnectionState = attr.get();
        if (enumConnectionState == null) {
            Core.logSevere("Changing ConnectionState from null to PLAY: " + channel);
            attr.set(EnumConnectionState.PLAY);
        }
    }

    private static Packet make(Side side, String channel) {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER && side == Side.CLIENT) {
            return null;
        }
        if (side == Side.CLIENT) {
            return new C17PacketCustomPayload(channel, new PacketBuffer(Unpooled.buffer(0)));
        } else {
            return new S3FPacketCustomPayload(channel, new PacketBuffer(Unpooled.buffer(0)));
        }
    }

    private static Boolean isWrapOn(Object obj) {
        if (!(obj instanceof Packet)) return null;
        Packet msg = (Packet) obj;
        if (msg == WRAP_ON_S || msg == WRAP_ON_C) return Boolean.TRUE;
        if (msg == WRAP_OFF_S || msg == WRAP_OFF_C) return Boolean.FALSE;
        String channel = Hammer.proxy.getChannel(msg);
        if (channel != null) {
            if (CHANNEL_ENABLE_WRAP.equals(channel)) return Boolean.TRUE;
            if (CHANNEL_DISABLE_WRAP.equals(channel)) return Boolean.FALSE;
        }
        return null;
    }

    private static final String CHANNEL_ENABLE_WRAP = "FZ9";
    private static final String CHANNEL_DISABLE_WRAP = "FZ8";
    private static final Packet WRAP_ON_C = make(Side.CLIENT, CHANNEL_ENABLE_WRAP), WRAP_OFF_C = make(Side.CLIENT, CHANNEL_DISABLE_WRAP);
    private static final Packet WRAP_ON_S = make(Side.SERVER, CHANNEL_ENABLE_WRAP), WRAP_OFF_S = make(Side.SERVER, CHANNEL_DISABLE_WRAP);
}
