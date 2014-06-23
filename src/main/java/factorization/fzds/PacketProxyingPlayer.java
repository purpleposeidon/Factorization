package factorization.fzds;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import factorization.api.Coord;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsEntryControl;
import factorization.fzds.network.FzdsPacketRegistry;
import factorization.shared.Core;

public class PacketProxyingPlayer extends EntityPlayerMP implements
        IFzdsEntryControl {
    DimensionSliceEntity dimensionSlice;
    static boolean useShortViewRadius = true; // true doesn't actually change the view radius

    private HashSet<EntityPlayerMP> trackedPlayers = new HashSet();
    NetworkManager networkManager = new NetworkManager(false) {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            throw new IllegalArgumentException("No, go away");
        }
        
        @Override
        public void setConnectionState(EnumConnectionState state) {
            throw new IllegalArgumentException("No solicitors!");
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            throw new IllegalArgumentException("Blllauergh!");
        }
        
        public Channel channel() {
            return proxiedChannel;
        }
        
        EmbeddedChannel proxiedChannel = new EmbeddedChannel(new WrappedMulticastHandler());
    };
    
    class WrappedMulticastHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            super.write(ctx, msg, promise);
        }
    }
    
    void initWrapping() {
        
    }

    public PacketProxyingPlayer(final DimensionSliceEntity dimensionSlice, World shadowWorld) {
        super(MinecraftServer.getServer(), (WorldServer) shadowWorld, new GameProfile(null, "[FzdsPacket]"), new ItemInWorldManager(shadowWorld));
        this.dimensionSlice = dimensionSlice;
        Coord c = dimensionSlice.getCenter();
        c.y = -8; // lurk in the void; we should catch most mod's packets.
        c.setAsEntityLocation(this);
        ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
        if (useShortViewRadius) {
            int orig = savePlayerViewRadius();
            restorePlayerViewRadius(3);
            try {
                scm.func_72375_a(this, null);
            } finally {
                restorePlayerViewRadius(orig);
                // altho the server might just crash anyways. Then again, there might be a handler higher up.
            }
        } else {
            scm.func_72375_a(this, null);
        }
        ticks_since_last_update = (int) (Math.random() * 20);
        
        // TODO: I think the chunks are unloading despite the PPP's presence.
        // Either figure out how to get this to act like an actual player, or
        // make chunk loaders happen as well
        playerNetServerHandler = new NetHandlerPlayServer(mcServer, networkManager, this);
        initWrapping();
        playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(this.networkManager));
    }
    
    int savePlayerViewRadius() {
        return getServerForPlayer().getPlayerManager().playerViewRadius;
    }

    void restorePlayerViewRadius(int orig) {
        if (orig == -1) {
            return;
        }
        getServerForPlayer().getPlayerManager().playerViewRadius = orig;
    }

    private int ticks_since_last_update = 0;

    @Override
    public void onUpdate() {
        if (this.dimensionSlice.isDead) {
            endProxy();
        } else if (ticks_since_last_update > 0) {
            ticks_since_last_update--;
        } else if (worldObj.isRemote) {
            // Just call super
        } else {
            ticks_since_last_update = 20;
            List playerList = getTargetablePlayers();
            for (int i = 0; i < playerList.size(); i++) {
                Object o = playerList.get(i);
                if (!(o instanceof EntityPlayerMP)) {
                    continue;
                }
                EntityPlayerMP player = (EntityPlayerMP) o;
                if (isPlayerInUpdateRange(player)) {
                    boolean new_player = trackedPlayers.add(player);
                    if (new_player && shouldShareChunks()) {
                        // welcome to the club. This may net-lag a bit. (Well, it depends on the chunk's contents. Air compresseswell tho.)
                        sendChunkMapDataToPlayer(player);
                    }
                } else {
                    trackedPlayers.remove(player);
                }
            }
        }
        super.onUpdate(); // we probably want to keep this one, just for the EntityMP stuff
    }

    List getTargetablePlayers() {
        return dimensionSlice.worldObj.playerEntities;
    }

    boolean isPlayerInUpdateRange(EntityPlayerMP player) {
        return !player.isDead && dimensionSlice.getDistanceSqToEntity(player) <= Hammer.DSE_ChunkUpdateRangeSquared;
    }

    boolean shouldShareChunks() {
        return true;
    }

    void sendChunkMapDataToPlayer(EntityPlayerMP target) {
        // Inspired by EntityPlayerMP.onUpdate. Shame we can't just add chunks... but there'd be no wrapper for the packets.
        ArrayList<Chunk> chunks = new ArrayList();
        ArrayList<TileEntity> tileEntities = new ArrayList();
        World world = DeltaChunk.getServerShadowWorld();

        Coord low = dimensionSlice.getCorner();
        Coord far = dimensionSlice.getFarCorner();
        int chunkCount = 0, teCount = 0; // TODO NORELEASE: Won't need this...
        for (int x = low.x - 16; x <= far.x + 16; x += 16) {
            for (int z = low.z - 16; z <= far.z + 16; z += 16) {
                if (!world.blockExists(x + 1, 0, z + 1)) {
                    continue;
                }
                Chunk chunk = world.getChunkFromBlockCoords(x, z);
                chunks.add(chunk);
                tileEntities.addAll(chunk.chunkTileEntityMap.values());
                chunkCount++;
            }
        }

        // NOTE: This has the potential to go badly if there's a large amount of data in the chunks.
        NetHandlerPlayServer net = target.playerNetServerHandler;
        if (!chunks.isEmpty()) {
            Packet toSend = FzdsPacketRegistry.wrap(new S26PacketMapChunkBulk(chunks));
            net.sendPacket(toSend);
        }
        teCount = tileEntities.size();
        if (!tileEntities.isEmpty()) {
            for (TileEntity te : tileEntities) {
                Packet description = te.getDescriptionPacket();
                if (description == null) {
                    continue;
                }
                Packet toSend = FzdsPacketRegistry.wrap(description);
                net.sendPacket(toSend);
            }
        }
        Core.logInfo("Sending data of " + chunkCount + " chunks with " + teCount + " tileEntities"); // NORELEASE
    }

    public void endProxy() {
        // From playerNetServerHandler.mcServer.getConfigurationManager().playerLoggedOut(this);
        WorldServer var2 = getServerForPlayer();
        var2.removeEntity(this); // setEntityDead
        var2.getPlayerManager().removePlayer(this); // No comod?
        MinecraftServer.getServer().getConfigurationManager().playerEntityList.remove(playerNetServerHandler);
        // The stuff above might not be necessary.
        setDead();
        dimensionSlice.proxy = null;
    }

    boolean shouldForceChunkLoad() {
        return !trackedPlayers.isEmpty();
    }

    @Override
    public void addToSendQueue(Packet packet) {
        if (trackedPlayers.isEmpty()) {
            return;
        }
        if (dimensionSlice.isDead) {
            setDead();
            return;
        }
        Packet wrappedPacket = FzdsPacketRegistry.wrap(packet);
        Iterator<EntityPlayerMP> it = trackedPlayers.iterator();
        while (it.hasNext()) {
            EntityPlayerMP player = it.next();
            if (player.isDead || player.worldObj != dimensionSlice.worldObj) {
                it.remove();
            } else {
                player.playerNetServerHandler.sendPacket(wrappedPacket);
            }
        }
    }
    
    @Override
    public void addNettyMessage(Channel sourceChannel, Object msg) {
        // Return a future?
        if (trackedPlayers.isEmpty()) {
            return;
        }
        if (dimensionSlice.isDead) {
            setDead();
            return;
        }
        Iterator<EntityPlayerMP> it = trackedPlayers.iterator();
        while (it.hasNext()) {
            EntityPlayerMP player = it.next();
            if (player.isDead || player.worldObj != dimensionSlice.worldObj) {
                it.remove();
            } else {
                addNettyMessageForPlayer(sourceChannel, msg, player);
            }
        }
    }
    
    void addNettyMessageForPlayer(final Channel sourceChannel, final Object packet, final EntityPlayerMP player, final GenericFutureListener... futureListeners) {
        // See NetworkManager.dispatchPacket
        final Channel destinationChannel = player.playerNetServerHandler.netManager.channel();
        if (destinationChannel == sourceChannel || player instanceof PacketProxyingPlayer) {
            throw new IllegalStateException("Sending a packet to myself!");
        }
        if (destinationChannel.attr(NetworkManager.attrKeyConnectionState).get() != EnumConnectionState.PLAY) {
            Core.logWarning("Not sending packet to " + player + ", because they are not in EnumConnectionState.PLAY: " + packet);
            return; // We're not going to attempt to send packets if they're not in the proper state.
        }

        if (destinationChannel.eventLoop().inEventLoop()) {
            destinationChannel.writeAndFlush(packet).addListeners(futureListeners).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            destinationChannel.eventLoop().execute(new Runnable() {
                public void run() {
                    destinationChannel.writeAndFlush(packet).addListeners(futureListeners).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            });
        }
    }
    
    @Override
    public void setDead() {
        super.setDead();
    }

    // IFzdsEntryControl implementation
    @Override
    public boolean canEnter(IDeltaChunk dse) {
        return false;
    } // PPP must stay in the shadow (It stays out of range anyways.)

    @Override
    public boolean canExit(IDeltaChunk dse) {
        return false;
    }

    @Override
    public void onEnter(IDeltaChunk dse) {
    }

    @Override
    public void onExit(IDeltaChunk dse) {
    }
}
