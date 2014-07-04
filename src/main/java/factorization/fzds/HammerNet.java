package factorization.fzds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.Core;


public class HammerNet {
    public static HammerNet instance;
    public static final String channelName = "FZDS|Interact"; //NORELEASE: There's another network thingie around here for DSE velocity & stuff!?
    public static FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
    
    public HammerNet() {
        instance = this;
        channel.register(this);
    }
    
    public static class HammerNetType {
        public static final byte rotation = 0, rotationVelocity = 1, rotationBoth = 2,
                rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6, digStart = 7, digProgress = 8, digFinish = 9;
    }
    
    @SubscribeEvent
    public void messageFromServer(ClientCustomPacketEvent event) {
        EntityPlayer player = Core.proxy.getClientPlayer();
        try {
            handleMessageFromServer(player, event.packet.payload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void handleMessageFromServer(EntityPlayer player, ByteBuf dis) throws IOException {
        if (player == null || player.worldObj == null) {
            return; //Blah, what...
        }
        World world = player.worldObj;
        byte type = dis.readByte();
        int dse_id = dis.readInt();
        Entity ent = world.getEntityByID(dse_id);
        DimensionSliceEntity dse = null;
        if (ent instanceof DimensionSliceEntity) {
            dse = (DimensionSliceEntity) ent;
        } else {
            return;
        }
        switch (type) {
        case HammerNetType.rotation:
            setRotation(dis, dse);
            break;
        case HammerNetType.rotationVelocity:
            setRotationalVelocity(dis, dse);
            break;
        case HammerNetType.rotationBoth:
            setRotation(dis, dse);
            setRotationalVelocity(dis, dse);
            break;
        }
        
    }
    
    void setRotation(ByteBuf dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotation(q);
        }
    }
    
    void setRotationalVelocity(ByteBuf dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotationalVelocity(q);
        }
    }
    
    
    @SubscribeEvent
    public void messageFromClient(ServerCustomPacketEvent event) {
        EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
        try {
            handleMessageFromClient(player, event.packet.payload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void handleMessageFromClient(EntityPlayerMP player, ByteBuf dis) throws IOException {
        byte type = dis.readByte();
        if (type == HammerNetType.digFinish || type == HammerNetType.digProgress || type == HammerNetType.digStart) {
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            byte sideHit = dis.readByte();
            if (type == HammerNetType.digFinish) {
                breakBlock(player, dis, x, y, z);
            }
        }
    }
    
    void breakBlock(EntityPlayerMP player, ByteBuf dis, int x, int y, int z) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), x, y, z);
        if (at.isAir()) return;
        boolean found_valid = false;
        double reach_distance_sq = player.theItemInWorldManager.getBlockReachDistance();
        reach_distance_sq *= reach_distance_sq;
        for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(at)) {
            Vec3 playerAt = player.getPosition(0);
            playerAt = idc.real2shadow(playerAt);
            double d = 0;
            d += Math.pow(at.x - playerAt.xCoord, 2);
            d += Math.pow(at.y - playerAt.yCoord, 2);
            d += Math.pow(at.z - playerAt.zCoord, 2);
            if (d <= reach_distance_sq) {
                found_valid = true;
                break;
            }
        }
        if (!found_valid) return;
        World origWorld = player.theItemInWorldManager.theWorld;
        player.theItemInWorldManager.theWorld = DeltaChunk.getServerShadowWorld();
        try {
            // NORELEASE: Not quite right; this will send packets to the player, not the proxy
            player.theItemInWorldManager.tryHarvestBlock(at.x, at.y, at.z);
        } finally {
            player.theItemInWorldManager.theWorld = origWorld;
        }
    }
    
    
    
    static FMLProxyPacket makePacket(byte type, Object... items) {
        ByteArrayDataOutput dos = ByteStreams.newDataOutput();
        dos.writeByte(type);
        for (int i = 0; i < items.length; i++) {
            Object obj = items[i];
            if (obj instanceof Quaternion) {
                ((Quaternion) obj).write(dos);
            } else if (obj instanceof Integer) {
                dos.writeInt((Integer) obj);
            } else if (obj instanceof Byte) {
                dos.writeByte((Byte) obj);
            } else if (obj instanceof Float) {
                dos.writeFloat((Float) obj);
            } else {
                throw new IllegalArgumentException("Can only do Quaternions/Integers/Bytes/Floats!");
            }
        }
        return new FMLProxyPacket(Unpooled.wrappedBuffer(dos.toByteArray()), channelName);
    }
}
