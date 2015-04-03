package factorization.fzds;

import factorization.fzds.interfaces.IFzdsShenanigans;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;

class CustomChannelNetworkManager extends NetworkManager implements IFzdsShenanigans {
    public CustomChannelNetworkManager(Channel myChannel, boolean isRemote) {
        super(isRemote);
        this.channel = myChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        throw new IllegalArgumentException("No, go away");
    }

    @Override
    public void setConnectionState(EnumConnectionState state) {
        if (state != EnumConnectionState.PLAY) throw new IllegalArgumentException("No solicitors!");
        super.setConnectionState(state);
        return;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        throw new IllegalArgumentException("Blllauergh!");
    }
}
