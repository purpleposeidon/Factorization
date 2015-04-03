package factorization.fzds;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.IFzdsShenanigans;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;

class InteractionLiason extends EntityPlayerMP implements IFzdsShenanigans {
    private static final GameProfile liasonGameProfile = new GameProfile(null /*UUID.fromString("69f64f91-665e-457d-ad32-f6082d0b8a71")*/ , "[FzdsInteractionLiason]");
    private final InventoryPlayer original_inventory;
    private ShadowPlayerAligner aligner;
    private NetworkManager networkManager;
    private EntityPlayerMP realPlayer;

    private EmbeddedChannel proxiedChannel = new EmbeddedChannel(new LiasonHandler());

    public InteractionLiason(WorldServer world, ItemInWorldManager itemManager, EntityPlayer realPlayer, IDeltaChunk idc) {
        super(MinecraftServer.getServer(), world, liasonGameProfile, itemManager);
        original_inventory = this.inventory;
        initLiason();
    }

    private void initLiason() {
        // We're fairly similar to PacketProxyingPlayer.initWrapping()
        networkManager = new CustomChannelNetworkManager(proxiedChannel, false);
        this.playerNetServerHandler = new NetHandlerPlayServer(MinecraftServer.getServer(), networkManager, this);
        playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(networkManager));
        //Compare cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER.{...}.selectNetworks(Object, ChannelHandlerContext, FMLProxyPacket)
        playerNetServerHandler.netManager.setConnectionState(EnumConnectionState.PLAY);
    }

    void initializeFor(EntityPlayerMP realPlayer, IDeltaChunk idc) {
        if (this.realPlayer != null) throw new IllegalStateException("Player wasn't reset!");
        this.realPlayer = realPlayer;
        this.inventory = realPlayer.inventory;
        this.setSprinting(realPlayer.isSprinting());
        this.setSneaking(realPlayer.isSneaking());
        this.capabilities = realPlayer.capabilities;
        aligner = new ShadowPlayerAligner(realPlayer, this, idc);
        aligner.apply();
    }

    void finishUsingLiason() {
        // Stuff? Drop our items? Die?
        realPlayer = null;
        inventory = original_inventory;
        aligner.unapply();
    }

    private class LiasonHandler extends ChannelOutboundHandlerAdapter implements IFzdsShenanigans {
        // See PacketProxyingPlayer.WrappedMulticastHandler
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            InteractionLiason.this.bouncePacket(msg);
        }
    }

    void bouncePacket(Object msg) {
        realPlayer.playerNetServerHandler.sendPacket(PacketProxyingPlayer.wrapMessage(msg));
    }
}
