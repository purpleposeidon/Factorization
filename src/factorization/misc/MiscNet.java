package factorization.misc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class MiscNet implements IPacketHandler {
    public static final String tpsChannel = "fzmsc.tps";
    
    public MiscNet() {
        MiscellaneousNonsense.net = this;
    }
    
    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player _player) {
        EntityPlayer player = (EntityPlayer) _player;
        if (!player.worldObj.isRemote) {
            return;
        }
        if (tpsChannel.equals(packet.channel)) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.data);
                DataInputStream input = new DataInputStream(bais);
                MiscellaneousNonsense.proxy.handleTpsReport(input.readFloat());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return;
    }

}
