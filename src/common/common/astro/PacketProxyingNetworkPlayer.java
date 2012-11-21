package factorization.common.astro;

import factorization.common.Core;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet;

public class PacketProxyingNetworkPlayer extends EntityPlayerMP {
    EntityPlayerMP proxiedPlayer;
    DimensionSliceEntity dimensionSlice;
    
    
    public PacketProxyingNetworkPlayer(EntityPlayerMP proxiedPlayer, DimensionSliceEntity dimensionSlice) {
        super(proxiedPlayer.mcServer, proxiedPlayer.worldObj, proxiedPlayer.username, proxiedPlayer.theItemInWorldManager);
        if (proxiedPlayer instanceof PacketProxyingNetworkPlayer) {
            throw new RuntimeException("tried to nest player dimension proxies");
        }
        this.proxiedPlayer = proxiedPlayer;
        this.dimensionSlice = dimensionSlice;
        this.playerNetServerHandler = new DimensionWrappingNetServerHandler(proxiedPlayer.mcServer, proxiedPlayer.playerNetServerHandler.netManager, this);
    }
    

    
    EntityPlayerMP getProxiedPlayer() {
        return proxiedPlayer;
    }
    
    int packetsSentThisTick = 0;
    boolean inTick = false;

    class DimensionWrappingNetServerHandler extends NetServerHandler {
        public DimensionWrappingNetServerHandler(MinecraftServer par1, INetworkManager par2, EntityPlayerMP par3) {
            super(par1, par2, par3);
        }
        
        @Override
        public void sendPacketToPlayer(Packet par1Packet) {
            if (packetsSentThisTick == 0 && inTick) {
                
            }
            packetsSentThisTick++;
            super.sendPacketToPlayer(par1Packet);
        }
        
        void sendWorldPush() {
            super.sendPacketToPlayer(Core.network.worldPushPacket(dimensionSlice));
        }
        
        void sendWorldPop() {
            super.sendPacketToPlayer(Core.network.worldPopPacket());
        }
    }


    public void enterTick() {
        packetsSentThisTick = 0;
        inTick = true;
    }
    
    public void leaveTick() {
        inTick = false;
    }
}
