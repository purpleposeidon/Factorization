package factorization.fzds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
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

import factorization.api.Coord;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsEntryControl;
import factorization.fzds.network.FzdsPacketRegistry;
import factorization.shared.FzNetDispatch;

public class PacketProxyingPlayer extends GenericProxyPlayer implements
        IFzdsEntryControl {
    DimensionSliceEntity dimensionSlice;
    static boolean useShortViewRadius = false; // true doesn't actually change
                                                // the view radius

    private HashSet<EntityPlayerMP> trackedPlayers = new HashSet();

    public PacketProxyingPlayer(final DimensionSliceEntity dimensionSlice,
            World shadowWorld) {
        super(MinecraftServer.getServer(), (WorldServer) shadowWorld,
                new GameProfile(null, "[FzdsPacket]"), new ItemInWorldManager(
                        shadowWorld));
        this.dimensionSlice = dimensionSlice;
        Coord c = dimensionSlice.getCenter();
        c.y = -8; // lurk in the void; we should catch most mod's packets.
        c.setAsEntityLocation(this);
        WorldServer ws = (WorldServer) dimensionSlice.worldObj;
        ServerConfigurationManager scm = MinecraftServer.getServer()
                .getConfigurationManager();
        if (useShortViewRadius) {
            int orig = savePlayerViewRadius();
            try {

                scm.func_72375_a(this, null);
            } finally {
                restorePlayerViewRadius(orig);
                // altho the server might just crash anyways. Then again, there
                // might be a handler higher up.
            }
        } else {
            scm.func_72375_a(this, null);
        }
        ticks_since_last_update = (int) (Math.random() * 20);
        // TODO: I think the chunks are unloading despite the PPP's presence.
        // Either figure out how to get this to act like an actual player, or
        // make chunk loaders happen as well
        playerNetServerHandler = new NetHandlerPlayServer(mcServer,
                networkManager, this) {
            @Override
            public void sendPacket(Packet packet) {
                // NORELEASE: Check if we're wrapping an already wrapped packet
                Chunk chunk = dimensionSlice.worldObj.getChunkFromBlockCoords((int) dimensionSlice.posX, (int) dimensionSlice.posZ);
                Packet wrapped = FzdsPacketRegistry.wrap(packet);
                FzNetDispatch.addPacketFrom(wrapped, chunk);
            }
        };
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
                        // welcome to the club. This may net-lag a bit. (Well,
                        // it depends on the chunk's contents. Air compresses
                        // well tho.)
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
        // Inspired by EntityPlayerMP.onUpdate. Shame we can't just add
        // chunks... but there'd be no wrapper for the packets.
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

        // NOTE: This has the potential to go badly if there's a large amount of
        // data in the chunks.
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
        System.out.println("Sending data of " + chunkCount + " chunks with " + teCount + " tileEntities");
    }

    public void endProxy() {
        // From
        // playerNetServerHandler.mcServer.getConfigurationManager().playerLoggedOut(this);
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
