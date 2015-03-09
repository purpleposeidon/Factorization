package factorization.fzds;

import factorization.api.ICoordFunction;
import factorization.shared.Core;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;

import java.lang.ref.WeakReference;
import java.util.*;

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

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.IFzdsEntryControl;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.fzds.network.WrappedPacket;
import net.minecraftforge.common.ForgeChunkManager;

public class PacketProxyingPlayer extends EntityPlayerMP implements
        IFzdsEntryControl, IFzdsShenanigans {
    WeakReference<DimensionSliceEntity> dimensionSlice = new WeakReference<DimensionSliceEntity>(null);
    static boolean useShortViewRadius = true; // true doesn't actually change the view radius

    private HashSet<EntityPlayerMP> listeningPlayers = new HashSet();
    
    
    
    EmbeddedChannel proxiedChannel = new EmbeddedChannel(new WrappedMulticastHandler());
    NetworkManager networkManager = new ProxyingNetworkManager();
    
    class ProxyingNetworkManager extends NetworkManager implements IFzdsShenanigans {
        public ProxyingNetworkManager() {
            super(false);
            this.channel = proxiedChannel;
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
    };
    
    class WrappedMulticastHandler extends ChannelOutboundHandlerAdapter implements IFzdsShenanigans {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            PacketProxyingPlayer.this.addNettyMessage(proxiedChannel, msg);
            //promise.setFailure(new UnsupportedOperationException("Sorry!")); // Nooooope, causes spam.
        }
    }
    
    void initWrapping() {
        playerNetServerHandler = new NetHandlerPlayServer(mcServer, networkManager, this);
        playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(this.networkManager));
        //Compare cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER.{...}.selectNetworks(Object, ChannelHandlerContext, FMLProxyPacket)
        playerNetServerHandler.netManager.setConnectionState(EnumConnectionState.PLAY);
        /* (misc notes here)
         * We don't need to touch NetworkDispatcher; we need a NetworkManager.
         * 
         * NetworkManager.scheduleOutboundPacket is too early I think?
         * What we really want is its channel.
         */
        registerChunkLoading();
    }


    ForgeChunkManager.Ticket ticket = null;
    void registerChunkLoading() {
        ticket = PPPChunkLoader.instance.register(getChunks());
    }

    void releaseChunkLoading() {
        if (ticket != null) {
            PPPChunkLoader.instance.release(ticket);
            ticket = null;
        }
    }

    private Set<Chunk> getChunks() {
        final HashSet<Chunk> ret = new HashSet<Chunk>();
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null) return ret;
        Coord min = dse.getCorner();
        Coord max = dse.getFarCorner();
        Coord.iterateChunks(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                ret.add(here.getChunk());
            }
        });
        return ret;
    }

    private static final UUID proxyUuid = UUID.fromString("69f64f92-665f-457e-ad33-f6082d0b8a75");

    public PacketProxyingPlayer(final DimensionSliceEntity dimensionSlice, World shadowWorld) {
        super(MinecraftServer.getServer(), (WorldServer) shadowWorld, new GameProfile(proxyUuid, "[FzdsPacket]"), new ItemInWorldManager(shadowWorld));
        invulnerable = true;
        isImmuneToFire = true;
        initWrapping();
        this.dimensionSlice = new WeakReference<DimensionSliceEntity>(dimensionSlice);
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
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null || dse.isDead) {
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
                    boolean new_player = listeningPlayers.add(player);
                    if (new_player && shouldShareChunks()) {
                        // welcome to the club. This may net-lag a bit. (Well, it depends on the chunk's contents. Air compresses well tho.)
                        sendChunkMapDataToPlayer(player);
                    }
                } else {
                    listeningPlayers.remove(player);
                }
            }
        }
        super.onUpdate(); // we probably want to keep this one, just for the EntityMP stuff
    }

    static final ArrayList empty = new ArrayList();

    List getTargetablePlayers() {
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null) return empty;
        return dse.worldObj.playerEntities;
    }

    boolean isPlayerInUpdateRange(EntityPlayerMP player) {
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null) return false;
        return !player.isDead && dse.getDistanceSqToEntity(player) <= Hammer.DSE_ChunkUpdateRangeSquared;
    }

    boolean shouldShareChunks() {
        return true;
    }

    void sendChunkMapDataToPlayer(EntityPlayerMP target) {
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null) return;
        // Inspired by EntityPlayerMP.onUpdate. Shame we can't just add chunks directly to target's chunkwatcher... but there'd be no wrapper for the packets.
        ArrayList<Chunk> chunks = new ArrayList();
        ArrayList<TileEntity> tileEntities = new ArrayList();
        World world = DeltaChunk.getServerShadowWorld();

        Coord low = dse.getCorner();
        Coord far = dse.getFarCorner();
        for (int x = low.x - 16; x <= far.x + 16; x += 16) {
            for (int z = low.z - 16; z <= far.z + 16; z += 16) {
                if (!world.blockExists(x + 1, 0, z + 1)) {
                    continue;
                }
                Chunk chunk = world.getChunkFromBlockCoords(x, z);
                chunks.add(chunk);
                tileEntities.addAll(chunk.chunkTileEntityMap.values());
            }
        }

        // NOTE: This has the potential to go badly if there's a large amount of data in the chunks.
        if (!chunks.isEmpty()) {
            Packet toSend = new S26PacketMapChunkBulk(chunks);
            addNettyMessageForPlayer(target, new WrappedPacket(toSend));
        }
        if (!tileEntities.isEmpty()) {
            for (TileEntity te : tileEntities) {
                Packet description = te.getDescriptionPacket();
                if (description == null) {
                    continue;
                }
                addNettyMessageForPlayer(target, new WrappedPacket(description));
            }
        }
    }

    boolean canDie = false;

    public void endProxy() {
        // From playerNetServerHandler.mcServer.getConfigurationManager().playerLoggedOut(this);
        WorldServer var2 = getServerForPlayer();
        var2.removeEntity(this); // setEntityDead
        var2.getPlayerManager().removePlayer(this); // No comod?
        MinecraftServer.getServer().getConfigurationManager().playerEntityList.remove(playerNetServerHandler);
        // The stuff above might not be necessary.
        canDie = true;
        setDead();
        dimensionSlice.clear();
    }

    boolean shouldForceChunkLoad() { //TODO: Chunk loading!
        return !listeningPlayers.isEmpty();
    }
    
    FMLEmbeddedChannel wrapper_channel = new FMLEmbeddedChannel("?", Side.SERVER, new ChannelHandler() {
        @Override public void handlerRemoved(ChannelHandlerContext ctx) throws Exception { }
        @Override public void handlerAdded(ChannelHandlerContext ctx) throws Exception { }
        @Override @Deprecated public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception { }
    });
    
    public Packet wrapMessage(Object msg) {
        if (msg instanceof Packet) {
            return new WrappedPacket((Packet) msg);
        }
        Packet pkt = wrapper_channel.generatePacketFrom(msg);
        return new WrappedPacket(pkt);
    }
    
    public void addNettyMessage(Channel sourceChannel, Object msg) {
        // Return a future?
        if (listeningPlayers.isEmpty()) {
            return;
        }
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null || dse.isDead) {
            setDead();
            return;
        }
        Object wrappedMsg = wrapMessage(msg);
        Iterator<EntityPlayerMP> it = listeningPlayers.iterator();
        while (it.hasNext()) {
            EntityPlayerMP player = it.next();
            if (player.isDead || player.worldObj != dse.worldObj) {
                it.remove();
            } else {
                addNettyMessageForPlayer(player, wrappedMsg);
            }
        }
    }
    
    void addNettyMessageForPlayer(EntityPlayerMP player, Object packet) {
        // See NetworkManager.dispatchPacket
        if (player instanceof PacketProxyingPlayer) {
            throw new IllegalStateException("Sending a packet to myself!");
        }
        player.playerNetServerHandler.sendPacket((Packet) packet);
    }
    
    @Override
    public void setDead() {
        if (canDie) {
            releaseChunkLoading();
            super.setDead();
        } else {
            Core.logWarning("Denying PacketProxingPlayer.setDead at " + new Coord(this));
        }
    }

    // IFzdsEntryControl implementation
    // PPP must stay in the shadow (It stays out of range anyways.)
    @Override
    public boolean canEnter(IDeltaChunk dse) {
        return false;
    }

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
