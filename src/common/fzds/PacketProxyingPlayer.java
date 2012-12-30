package factorization.fzds;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.api.Coord;
import factorization.fzds.api.IFzdsEntryControl;

public class PacketProxyingPlayer extends EntityPlayerMP implements IFzdsEntryControl, INetworkManager {
    DimensionSliceEntity dimensionSlice;
    static boolean useShortViewRadius = false;
    
    private HashSet<EntityPlayerMP> trackedPlayers;
    
    public PacketProxyingPlayer(DimensionSliceEntity dimensionSlice) {
        super(MinecraftServer.getServer(), dimensionSlice.hammerCell.w, "FZDS" + dimensionSlice.cell, new ItemInWorldManager(dimensionSlice.hammerCell.w));
        this.dimensionSlice = dimensionSlice;
        this.playerNetServerHandler = new NetServerHandler(MinecraftServer.getServer(), this, this);
        Coord c = Hammer.getCellCenter(dimensionSlice.cell);
        c.y = -8;
        c.setAsEntityLocation(this);
        WorldServer ws = (WorldServer) dimensionSlice.worldObj;
        if (useShortViewRadius) {
            int orig = savePlayerViewRadius();
            try {
                MinecraftServer.getServerConfigurationManager(mcServer).func_72375_a(this, null);
            } finally {
                restorePlayerViewRadius(orig);
                //altho the server might just crash anyways. Then again, there might be a handler higher up.
            }
        } else {
            MinecraftServer.getServerConfigurationManager(mcServer).func_72375_a(this, null);
        }
        ticks_since_last_update = (int) (Math.random()*20);
    }
    
    private final int PlayerManager_playerViewRadius_field = 4;
    
    int savePlayerViewRadius() {
        try {
            return ObfuscationReflectionHelper.<Integer, PlayerManager>getPrivateValue(PlayerManager.class, getServerForPlayer().getPlayerManager(), PlayerManager_playerViewRadius_field);
        } catch (Exception e) {
            return -1;
        }
    }
    
    void restorePlayerViewRadius(int orig) {
        if (orig == -1) {
            return;
        }
        ReflectionHelper.setPrivateValue(PlayerManager.class, getServerForPlayer().getPlayerManager(), orig, PlayerManager_playerViewRadius_field);
    }
    
    private int ticks_since_last_update = 0;
    @Override
    public void onUpdate() {
        if (this.dimensionSlice.isDead) {
            endProxy();
        } else if (ticks_since_last_update == 0) {
            ticks_since_last_update = 20;
        } else {
            ticks_since_last_update--;
            List playerList = dimensionSlice.worldObj.playerEntities;
            for (int i = 0; i < playerList.size(); i++) {
                Object o = playerList.get(i);
                if (!(o instanceof EntityPlayerMP)) {
                    continue;
                }
                EntityPlayerMP player = (EntityPlayerMP) o;
                if (isPlayerInUpdateRange(player)) {
                    
                } else {
                    trackedPlayers.remove(player);
                }
            }
        }
        super.onUpdate(); //we probably want to keep this one
    }
     
    boolean isPlayerInUpdateRange(EntityPlayerMP player) {
        return dimensionSlice.getDistanceSqToEntity(player) <= Hammer.DSE_ChunkUpdateRangeSquared;
    }
    
    void sendChunkMapDataToPlayer(EntityPlayerMP target) {
        
    }
    
    public void endProxy() {
        //From playerNetServerHandler.mcServer.getConfigurationManager().playerLoggedOut(this);
        WorldServer var2 = getServerForPlayer();
        var2.setEntityDead(this);
        var2.getPlayerManager().removePlayer(this); //No comod?
        var2.getMinecraftServer().getConfigurationManager().playerEntityList.remove(playerNetServerHandler);
        setDead();
        //Might be able to get away with just setDead() here.
    }

    
    
    //INetworkManager implementation -- or whatever this is.
    @Override
    public void setNetHandler(NetHandler netHandler) { }

    @Override
    public void addToSendQueue(Packet packet) {
        Packet wrappedPacket = new Packet220FzdsWrap(packet);
        for (EntityPlayerMP player : trackedPlayers) {
            player.playerNetServerHandler.sendPacketToPlayer(wrappedPacket);
        }
        return;
    }
    
    @Override
    public void wakeThreads() { }

    @Override
    public void processReadPackets() { }

    @Override
    public SocketAddress getSocketAddress() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "<Packet Proxying Player for FZDS " + dimensionSlice + ">";
            }
        };
    }

    @Override
    public void serverShutdown() { }

    @Override
    public int packetSize() {
        //usages suggests this is used only to delay sending item map data, and that only happens if this is <= 5. Yeaaah. No.
        //The real player should be receiving that kind of details anyways. Besides, PPP won't be carrying items.
        return 10;
    }

    @Override
    public void networkShutdown(String str, Object... args) { }

    @Override
    public void closeConnections() { }
    

    
    
    //IFzdsEntryControl implementation
    
    @Override
    public boolean canEnter(DimensionSliceEntity dse) { return false; } //PPP must stay in the shadow (It stays out of range anyways.)
    
    @Override
    public boolean canExit(DimensionSliceEntity dse) { return false; }
    
    @Override
    public void onEnter(DimensionSliceEntity dse) { }
    
    @Override
    public void onExit(DimensionSliceEntity dse) { }
}
