package factorization.fzds.network;

import com.mojang.authlib.GameProfile;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.fzds.DeltaChunk;
import factorization.fzds.DimensionSliceEntity;
import factorization.fzds.Hammer;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.IFzdsEntryControl;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.*;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.*;

public class PacketProxyingPlayer extends EntityPlayerMP implements
        IFzdsEntryControl, IFzdsShenanigans {
    WeakReference<DimensionSliceEntity> dimensionSlice = new WeakReference<DimensionSliceEntity>(null);
    static boolean useShortViewRadius = true; // true doesn't actually change the view radius

    private HashSet<EntityPlayerMP> listeningPlayers = new HashSet();
    
    
    
    EmbeddedChannel proxiedChannel = new EmbeddedChannel(new WrappedMulticastHandler());
    NetworkManager networkManager = new CustomChannelNetworkManager(proxiedChannel, EnumPacketDirection.CLIENTBOUND);
    
    class WrappedMulticastHandler extends ChannelOutboundHandlerAdapter implements IFzdsShenanigans {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            PacketProxyingPlayer.this.addNettyMessage(msg);
            //promise.setFailure(new UnsupportedOperationException("Sorry!")); // Nooooope, causes spam.
        }
    }

    void preinitWrapping() {
        playerNetServerHandler = new NetHandlerPlayServer(mcServer, networkManager, this);
        playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).set(new NetworkDispatcher(this.networkManager));
        //Compare net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER.{...}.selectNetworks(Object, ChannelHandlerContext, FMLProxyPacket)
        playerNetServerHandler.netManager.setConnectionState(EnumConnectionState.PLAY);
        /* (misc notes here)
         * We don't need to touch NetworkDispatcher; we need a NetworkManager.
         *
         * NetworkManager.scheduleOutboundPacket is too early I think?
         * What we really want is its channel.
         */
    }

    void initWrapping() {
        registerChunkLoading();
        updateListenerList();
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
        this.dimensionSlice = new WeakReference<DimensionSliceEntity>(dimensionSlice);
        Coord c = dimensionSlice.getCenter();
        c.y = -8; // lurk in the void; we should catch most mod's packets.
        DeltaCoord size = dimensionSlice.getFarCorner().difference(dimensionSlice.getCorner());
        size.y = 0;
        int width = Math.abs(size.x);
        int depth = Math.abs(size.z);
        double blockRadius = Math.max(width, depth) / 2;
        int chunkRadius = (int) ((blockRadius / 16) + 2);
        chunkRadius = Math.max(3, chunkRadius);
        c.setAsEntityLocation(this);
        preinitWrapping();
        ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
        if (useShortViewRadius) {
            int orig = savePlayerViewRadius();
            restorePlayerViewRadius(chunkRadius);
            try {
                scm.preparePlayer(this, null /* previous world; allowed to be null */);
            } finally {
                restorePlayerViewRadius(orig);
                // altho the server might just crash anyways. Then again, there might be a handler higher up.
            }
        } else {
            scm.preparePlayer(this, null /* previous world; allowed to be null */);
        }
        initWrapping();
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

    @Override
    public void onUpdate() {
        DimensionSliceEntity dse = dimensionSlice.get();
        if (dse == null || dse.isDead) {
            endProxy();
            return;
        }
        super.onUpdate(); // we probably want to keep this one, just for the EntityMP stuff
        if (worldObj.isRemote) return; // Won't happen.
        boolean should_update = ticksExisted % 20 == 1;
        if (!should_update) return;
        updateListenerList();
    }

    void updateListenerList() {
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
        final ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        final ArrayList<TileEntity> tileEntities = new ArrayList<TileEntity>();
        World world = DeltaChunk.getServerShadowWorld();

        Coord low = dse.getCorner().add(-16, 0, -16);
        Coord far = dse.getFarCorner().add(+16, 0, +16);
        Coord.iterateChunks(low, far, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                if (!here.blockExists()) return;
                Chunk chunk = here.getChunk();
                chunks.add(chunk);
                tileEntities.addAll(chunk.chunkTileEntityMap.values());
            }
        });

        // NOTE: This has the potential to go badly if there's a large amount of data in the chunks.
        if (!chunks.isEmpty()) {
            Packet toSend = new S26PacketMapChunkBulk(chunks);
            addNettyMessageForPlayer(target, toSend);
        }
        if (!tileEntities.isEmpty()) {
            for (TileEntity te : tileEntities) {
                Packet description = te.getDescriptionPacket();
                if (description == null) {
                    continue;
                }
                addNettyMessageForPlayer(target, description);
            }
        }
    }

    boolean canDie = false;

    public void endProxy() {
        if (canDie && isDead) {
            // !!! Recursion death!?
            return;
        }
        canDie = true;
        setDead();
        // From playerNetServerHandler.mcServer.getConfigurationManager().playerLoggedOut(this);
        WorldServer world = getServerForPlayer();
        //world.removeEntity(this); // setEntityDead
        world.playerEntities.remove(this);
        world.getPlayerManager().removePlayer(this); // No comod?
        MinecraftServer.getServer().getConfigurationManager().playerEntityList.remove(playerNetServerHandler);
        dimensionSlice.clear();
    }

    boolean shouldForceChunkLoad() { //TODO: Chunk loading!
        return !listeningPlayers.isEmpty();
    }

    public void addNettyMessage(Object msg) {
        // Return a future?
        if (listeningPlayers.isEmpty()) {
            return;
        }
        DimensionSliceEntity dse = dimensionSlice.get(); // NORELEASE: Is there a world leak here? Does the DSE clean us up?
        if (dse == null || dse.isDead) {
            endProxy();
            return;
        }
        Iterator<EntityPlayerMP> it = listeningPlayers.iterator();
        while (it.hasNext()) {
            EntityPlayerMP player = it.next();
            if (player.isDead || player.worldObj != dse.worldObj) {
                it.remove();
            } else {
                addNettyMessageForPlayer(player, msg);
            }
        }
    }
    
    void addNettyMessageForPlayer(EntityPlayerMP player, Object packet) {
        // See NetworkManager.dispatchPacket
        if (player instanceof PacketProxyingPlayer) {
            throw new IllegalStateException("Sending a packet to myself!");
        }
        PacketJunction.switchJunction(player.playerNetServerHandler, true);
        try {
            player.playerNetServerHandler.sendPacket((Packet) packet);
        } finally {
            PacketJunction.switchJunction(player.playerNetServerHandler, false);
        }
    }

    /**
     * use endProxy()
     */
    @Override
    @Deprecated
    public void setDead() {
        if (worldObj.isRemote) {
            super.setDead();
            return;
        }
        if (!canDie) {
            Core.logWarning("Unexpected PacketProxingPlayer death at " + new Coord(this));
            Thread.dumpStack();
            canDie = true;
        }
        releaseChunkLoading();
        super.setDead();
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
