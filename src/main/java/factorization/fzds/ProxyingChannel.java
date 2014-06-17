package factorization.fzds;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

import java.net.SocketAddress;

final class ProxyingChannel extends AbstractChannel {
    final GenericProxyPlayer player;
    
    ProxyingChannel(GenericProxyPlayer player) {
        super(null);
        this.player = player;
    }

    @Override
    public ChannelConfig config() {
        return null; // TODO: Please, no.
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public ChannelMetadata metadata() {
        return null; // TODO: No! Please! No! No more!
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return null; // TODO: Agh! What the fuck is this!?
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return false; // No! No loops!
    }

    @Override
    protected SocketAddress localAddress0() {
        return null; // Hey! Stop that! I have no address!
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null; // Look. Uhm. Just, yeah. No!
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        // Don't be ridiculous!
    }

    @Override
    protected void doDisconnect() throws Exception {
        // What!? There's nothing!
    }

    @Override
    protected void doClose() throws Exception {
        // How!?
    }

    @Override
    protected void doBeginRead() throws Exception {
        // Okay. This guy actually looks interesting.
        // We don't actually need it tho?
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        // Hmm? Maybe?
    }
    
    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        // Yeah! Now *THAT*'s more like it!
        writePacket(msg);
        return null; // The return value is apparently never actually used?
    }
    
    private void writePacket(Object msg) {
        player.addNettyMessage(this, msg);
    }
}