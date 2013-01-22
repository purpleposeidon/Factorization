package factorization.fzds;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;

import factorization.api.Coord;

public class ReversePacketProxyingPlayer extends PacketProxyingPlayer {
    private Coord a, b;
    public ReversePacketProxyingPlayer(DimensionSliceEntity dimensionSlice) {
        super(dimensionSlice);
        (new Coord(dimensionSlice)).setAsEntityLocation(this);
        a = Hammer.getCellCorner(worldObj, dimensionSlice.cell);
        b = Hammer.getCellOppositeCorner(worldObj, dimensionSlice.cell);
    }
    
    static String getPrefix() {
        return "~FZDS";
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        setPosition(dimensionSlice.posX, dimensionSlice.posY, dimensionSlice.posZ);
    }
    
    @Override
    boolean shouldShareChunks() {
        return false;
    }
    
    @Override
    List getTargetablePlayers() {
        return Hammer.getServerShadowWorld().playerEntities;
    }

    @Override
    boolean isPlayerInUpdateRange(EntityPlayerMP player) {
        if (player.isDead) {
            return false;
        }
        Coord p = new Coord(player);
        return a.isCompletelySubmissiveTo(p) && p.isCompletelySubmissiveTo(b);
    }
    
    @Override
    public void addToSendQueue(Packet packet) {
        //System.out.println("<-- " + packet); //NORELEASE
        super.addToSendQueue(packet);
    }
}
