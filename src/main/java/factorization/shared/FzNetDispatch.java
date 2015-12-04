package factorization.shared;

import factorization.api.Coord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class FzNetDispatch {
    
    public static FMLProxyPacket generate(byte[] data) {
        return generate(Unpooled.wrappedBuffer(data));
    }
    
    public static FMLProxyPacket generate(ByteBufOutputStream buf) {
        return generate(buf.buffer());
    }
    
    public static FMLProxyPacket generate(ByteArrayOutputStream baos) {
        return generate(Unpooled.wrappedBuffer(baos.toByteArray()));
    }

    public static FMLProxyPacket generate(ByteBuf buf) {
        return new FMLProxyPacket(buf, FzNetEventHandler.channelName);
    }
    
    public static void addPacket(FMLProxyPacket packet, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            FzNetEventHandler.channel.sendToServer(packet);
        } else if (player instanceof EntityPlayerMP) {
            FzNetEventHandler.channel.sendTo(packet, (EntityPlayerMP) player);
        }
    }
    
    public static void addPacketFrom(Packet packet, Chunk chunk) {
        if (chunk.worldObj.isRemote) return;
        final WorldServer world = (WorldServer) chunk.worldObj;
        final PlayerManager pm = world.getPlayerManager();
        PlayerManager.PlayerInstance watcher = pm.getOrCreateChunkWatcher(chunk.xPosition, chunk.zPosition, false);
        if (watcher == null) return;
        watcher.sendToAllPlayersWatchingChunk(packet);

        // The above requires an AT that is impossible to get working?
        
        /*if (chunk.worldObj.isRemote) return;
        final WorldServer world = (WorldServer) chunk.worldObj;
        final PlayerManager pm = world.getPlayerManager();
        final int near = 10;
        final int far = 16;
        final int sufficientlyClose = (near*16)*(near*16);
        final int superlativelyFar = (far*16)*(far*16);
        final int chunkBlockX = chunk.xPosition*16 + 8;
        final int chunkBlockZ = chunk.zPosition*16 + 8;
        for (EntityPlayerMP player : (Iterable<EntityPlayerMP>) world.playerEntities) {
            double dx = player.posX - chunkBlockX;
            double dz = player.posZ - chunkBlockZ;
            double dist = dx*dx + dz*dz;
            if (dist > superlativelyFar) continue;
            if (dist < sufficientlyClose || pm.isPlayerWatchingChunk(player, chunk.xPosition, chunk.zPosition)) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }*/
    }
    
    public static void addPacketFrom(Packet packet, World world, double x, double z) {
        int chunkX = MathHelper.floor_double(x / 16.0D);
        int chunkZ = MathHelper.floor_double(z / 16.0D);
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        addPacketFrom(packet, chunk);
    }
    
    public static void addPacketFrom(Packet packet, Entity ent) {
        addPacketFrom(packet, ent.worldObj, ent.posX, ent.posZ);
    }
    
    public static void addPacketFrom(Packet packet, TileEntity ent) {
        World w = ent.getWorld();
        addPacketFrom(packet, w.getChunkFromBlockCoords(ent.pos.getX(), ent.pos.getZ()));
    }
    
    public static void addPacketFrom(Packet packet, Coord c) {
        addPacketFrom(packet, c.getChunk());
    }
    
}
