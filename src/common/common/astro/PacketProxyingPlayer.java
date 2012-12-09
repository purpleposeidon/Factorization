package factorization.common.astro;

import java.net.SocketAddress;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.ItemInWorldManager;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.PlayerManager;
import net.minecraft.src.ServerConfigurationManager;
import net.minecraft.src.WorldServer;
import factorization.common.Core;

public class PacketProxyingPlayer extends EntityPlayerMP {
    EntityPlayerMP proxiedPlayer;
    DimensionSliceEntity dimensionSlice;
    DimensionNetworkManager wrappedNetworkManager;
    static boolean useShortViewRadius = false;
    
    public PacketProxyingPlayer(EntityPlayerMP proxiedPlayer, DimensionSliceEntity dimensionSlice) {
        super(proxiedPlayer.mcServer, dimensionSlice.hammerCell.w, "FZDS.PlayerProxy:" + dimensionSlice.cell, new ItemInWorldManager(dimensionSlice.hammerCell.w));
        if (proxiedPlayer instanceof PacketProxyingPlayer) {
            throw new RuntimeException("tried to nest player dimension proxies");
        }
        this.proxiedPlayer = proxiedPlayer;
        this.dimensionSlice = dimensionSlice;
        wrappedNetworkManager = new DimensionNetworkManager(proxiedPlayer.playerNetServerHandler.netManager);
        this.playerNetServerHandler = new NetServerHandler(proxiedPlayer.mcServer, wrappedNetworkManager, this);
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
    }
    
    private final int PlayerManager_playerViewRadius_field = 4;
    
    int savePlayerViewRadius() {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(PlayerManager.class, getServerForPlayer().getPlayerManager(), PlayerManager_playerViewRadius_field);
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

    
    EntityPlayerMP getProxiedPlayer() {
        return proxiedPlayer;
    }
    
    int packetsSentThisTick = 0;
    boolean inTick = false;
    
    class DimensionNetworkManager implements INetworkManager {
        INetworkManager wrapped;
        public DimensionNetworkManager(INetworkManager wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public void setNetHandler(NetHandler netHandler) {
            //wrapped.setNetHandler(netHandler);
        }

        @Override
        public void addToSendQueue(Packet packet) {
            if (packetsSentThisTick == 0 && inTick) {
                sendWorldPush();
            }
            packetsSentThisTick++;
            System.out.println("Proxying packet: " + packet);
            wrapped.addToSendQueue(packet);
        }
        
        void sendWorldPush() {
            wrapped.addToSendQueue(Core.network.worldPushPacket(dimensionSlice));
        }
        
        void sendWorldPop() {
            wrapped.addToSendQueue(Core.network.worldPopPacket());
        }

        @Override
        public void wakeThreads() {
            wrapped.wakeThreads();
        }

        @Override
        public void processReadPackets() {
            //wrapped.processReadPackets();
        }

        @Override
        public SocketAddress getSocketAddress() {
            return wrapped.getSocketAddress();
        }

        @Override
        public void serverShutdown() {
            //wrapped.serverShutdown();
        }

        @Override
        public int packetSize() {
            return wrapped.packetSize();
        }

        @Override
        public void networkShutdown(String str, Object... args) {
            wrapped.networkShutdown(str, args);
        }

        @Override
        public void closeConnections() {
            wrapped.closeConnections();
        }
        
    }

    public void enterTick() {
        packetsSentThisTick = 0;
        inTick = true;
    }
    
    public void leaveTick() {
        if (packetsSentThisTick > 0) {
            wrappedNetworkManager.sendWorldPop();
        }
        inTick = false;
    }
    
    @Override
    public void onUpdate() {
        this.isDead = this.dimensionSlice.isDead;
        super.onUpdate();
    }
}
