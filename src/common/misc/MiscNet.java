package factorization.misc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class MiscNet implements IPacketHandler {
    public static final String channel = "fz.bounce";
    
    public MiscNet() {
        MiscellaneousNonsense.net = this;
    }
    
    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player _player) {
        EntityPlayer player = (EntityPlayer) _player;
        if (!player.worldObj.isRemote) {
            return;
        }
        if (!channel.equals(packet.channel)) {
            return;
        }
        ArrayList<String> text = new ArrayList();
        try {
            if (packet.data != null && packet.data.length > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.data);
                DataInputStream input = new DataInputStream(bais);
                while (input.available() > 0) {
                    text.add(input.readUTF());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MiscellaneousNonsense.proxy.runCommand(text);
    }

}
