package factorization.fzds.network;

import factorization.fzds.interfaces.IFzdsShenanigans;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;

class CustomChannelNetworkManager extends NetworkManager implements IFzdsShenanigans {
    public CustomChannelNetworkManager(Channel myChannel, EnumPacketDirection direction) {
        super(direction);
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
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        throw new IllegalArgumentException("Blllauergh!");
    }
}
