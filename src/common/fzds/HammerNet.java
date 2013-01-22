package factorization.fzds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import factorization.common.Core;

public class HammerNet implements IPacketHandler {
    static final String teleport = "fzds.teleport";
    
    public static void transferPlayer(EntityPlayerMP player, DimensionSliceEntity dse, World newWorld, Vec3 newPosition) {
        ServerConfigurationManager manager = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer());
        DSTeleporter tp = new DSTeleporter((WorldServer) player.worldObj);
        tp.preciseDestination = newPosition;
        
        Packet250CustomPayload toSend = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeInt(newWorld.getWorldInfo().getDimension());
            int dse_id = dse == null ? -1 : dse.entityId;
            output.writeInt(dse_id);
            output.flush();
            toSend = PacketDispatcher.getPacket(teleport, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Core.proxy.addPacket(player, toSend);
        manager.transferPlayerToDimension(player, newWorld.getWorldInfo().getDimension(), tp);
    }
    
    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player FML_player) {
        EntityPlayer player = (EntityPlayer) FML_player;
        if (!player.worldObj.isRemote) {
            return;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.data);
        DataInputStream input = new DataInputStream(inputStream);
        int dimension;
        int entityId;
        try {
            dimension = input.readInt();
            entityId = input.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        DimensionSliceEntity dse = (DimensionSliceEntity) player.worldObj.getEntityByID(entityId);
        Hammer.proxy.setPlayerIsEmbedded(dse);
    }
    
}
