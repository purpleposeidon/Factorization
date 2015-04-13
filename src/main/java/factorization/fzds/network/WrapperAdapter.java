package factorization.fzds.network;

import factorization.fzds.interfaces.IFzdsShenanigans;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

public class WrapperAdapter extends ChannelOutboundHandlerAdapter implements IFzdsShenanigans {
    public static void addToPipeline(NetworkManager manager) {
        WrapperAdapter.manager = manager;
        manager.channel.pipeline().addAfter("fml:packet_handler" /* see NetworkDispatcher.insertIntoChannel */, "fzds:wrapper", new WrapperAdapter());
    }

    public static void setShadow(boolean shadow) {
        manager.channel.write(shadow ? ENTER_SHADOW : EXIT_SHADOW);
    }

    private static final Object ENTER_SHADOW = new Object(), EXIT_SHADOW = new Object();
    private static NetworkManager manager;

    private boolean inShadow = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == ENTER_SHADOW) {
            inShadow = true;
        } else if (msg == EXIT_SHADOW) {
            inShadow = false;
        } else if (inShadow && msg instanceof Packet) {
            // How do we handle non-Packets if we *are* in shadow?
            // Just send it on as normal and pray that nothing horrific happens.
            ctx.write(new WrappedPacketFromClient((Packet) msg), promise);
        } else {
            ctx.write(msg, promise);
        }
    }

}
