package factorization.fzds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import factorization.common.Core;

public class HammerNet implements IPacketHandler {
    static final String teleport = "fzds.teleport";
    static final String puppet = "fzds.puppet";
    
    public static void puppetPlayer(EntityPlayerMP player, PuppetPlayer pp) {
        World w = pp.worldObj;
        NetServerHandler nsh = player.playerNetServerHandler;
        pp.playerNetServerHandler = nsh;
        nsh.playerEntity = pp;
        w.spawnEntityInWorld(pp);
        
        Packet250CustomPayload toSend = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeBoolean(pp.worldObj != player.worldObj);
            output.writeInt(pp.entityId);
            output.flush();
            toSend = PacketDispatcher.getPacket(puppet, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Core.proxy.addPacket(player, toSend);
    }
    
    public static void transferPlayer(EntityPlayerMP player, DimensionSliceEntity dse, World newWorld, Vec3 newPosition) {
        MinecraftServer mc = MinecraftServer.getServer();
        PuppetPlayer puppet = new PuppetPlayer(mc, newWorld, player.username + "#", player.theItemInWorldManager);
        newWorld.spawnEntityInWorld(puppet);
        Packet250CustomPayload toSend = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeInt(puppet.entityId);
            output.flush();
            toSend = PacketDispatcher.getPacket(teleport, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Core.proxy.addPacket(player, toSend);
        player.timeUntilPortal = Integer.MAX_VALUE;
    }
    
    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player FML_player) {
        EntityPlayer player = (EntityPlayer) FML_player;
        if (!player.worldObj.isRemote) {
            return;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(packet.data);
        DataInputStream input = new DataInputStream(inputStream);
        try {
            if (packet.channel.equals(teleport)) {
                handleTeleport(input);
            } else if (packet.channel.equals(puppet)) {
                handlePuppet(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    
    void handleTeleport(DataInputStream input) throws IOException {
        int entityId = input.readInt();
        PuppetPlayer puppet = (PuppetPlayer) Hammer.getClientShadowWorld().getEntityByID(entityId);
        if (puppet == null && entityId > 0) {
            System.out.println("Didn't find puppet"); //NORELEASE
        }
        Hammer.proxy.setPuppet(puppet);
    }
    
    void handlePuppet(DataInputStream input) throws IOException {
        World world = input.readBoolean() ? Hammer.getClientShadowWorld() : Hammer.getClientRealWorld();
        int entityId = input.readInt();
        if (entityId < 0) {
            Hammer.proxy.setPuppet((PuppetPlayer)null);
            return;
        }
        Entity puppet = world.getEntityByID(entityId);
        if (puppet == null && entityId > 0) {
            System.out.println("Didn't find puppet"); //NORELEASE
            //uhm, maybe put the packet back on the read queue?
            return;
        }
        Hammer.proxy.setPuppet((PuppetPlayer) puppet);
        
    }
}
