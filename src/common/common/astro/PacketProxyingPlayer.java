package factorization.common.astro;

import factorization.common.Core;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet;

public class PacketProxyingPlayer extends EntityPlayerMP {
    EntityPlayerMP proxiedPlayer;
    DimensionSliceEntity dimensionSlice;
    DimensionWrappingNetServerHandler DWNSH;
    
    public PacketProxyingPlayer(EntityPlayerMP proxiedPlayer, DimensionSliceEntity dimensionSlice) {
        super(proxiedPlayer.mcServer, proxiedPlayer.worldObj, "PROXY:" + proxiedPlayer.username, proxiedPlayer.theItemInWorldManager);
        if (proxiedPlayer instanceof PacketProxyingPlayer) {
            throw new RuntimeException("tried to nest player dimension proxies");
        }
        this.proxiedPlayer = proxiedPlayer;
        this.dimensionSlice = dimensionSlice;
        this.playerNetServerHandler = DWNSH = new DimensionWrappingNetServerHandler(proxiedPlayer.mcServer, proxiedPlayer.playerNetServerHandler.netManager, this);
        proxiedPlayer.playerNetServerHandler.netManager.setNetHandler(proxiedPlayer.playerNetServerHandler);
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
        public void sendPacketToPlayer(Packet packet) {
            if (packetsSentThisTick == 0 && inTick) {
                sendWorldPush();
            }
            packetsSentThisTick++;
            System.out.println("Proxying packet: " + packet);
            super.sendPacketToPlayer(packet);
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
        if (packetsSentThisTick > 0) {
            DWNSH.sendWorldPop();
        }
        inTick = false;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
    }
}
