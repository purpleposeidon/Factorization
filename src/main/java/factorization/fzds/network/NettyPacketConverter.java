package factorization.fzds.network;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;

public class NettyPacketConverter extends FMLEmbeddedChannel {

    static ChannelHandler handler = new ChannelHandler() {
        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        @Deprecated
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        }
    };

    public NettyPacketConverter(Side source) {
        super("[FZDS Packet Wrapping Channel]", source, handler);
    }

    public Packet convert(Object msg) {
        return generatePacketFrom(msg);
    }
}
