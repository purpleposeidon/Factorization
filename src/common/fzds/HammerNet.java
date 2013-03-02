package factorization.fzds;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.network.ITinyPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import factorization.api.Quaternion;
import factorization.common.Core;


public class HammerNet implements ITinyPacketHandler {
    public static class HammerNetType {
        public static final short rotation = 0, rotationVelocity = 1, rotationBoth = 2,
                rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6;
    }
    
    @Override
    public void handle(NetHandler handler, Packet131MapData mapData) {
        if (!handler.getPlayer().worldObj.isRemote) {
            try {
                handleDseClick(handler, mapData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(mapData.itemData);
        DataInputStream dis = new DataInputStream(bais);
        World world = handler.getPlayer().worldObj;
        try {
            short type = mapData.uniqueID;
            int e_id = dis.readInt();
            Entity ent = world.getEntityByID(e_id);
            DimensionSliceEntity dse = null;
            if (ent instanceof DimensionSliceEntity) {
                dse = (DimensionSliceEntity) ent;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    void handleDseClick(NetHandler handler, Packet131MapData mapData) throws IOException {
        World world = handler.getPlayer().worldObj;
        World shadow = DeltaChunk.getServerShadowWorld();
        MinecraftServer server = MinecraftServer.getServer();
        EntityPlayerMP player = (EntityPlayerMP) handler.getPlayer();
        
        InteractionProxyingPlayer ipp = new InteractionProxyingPlayer(server, player, player.theItemInWorldManager);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(mapData.itemData);
        DataInputStream dis = new DataInputStream(bais);
        
        short packetType = mapData.uniqueID;
        switch (packetType) {
        case HammerNetType.leftClickEntity:
        case HammerNetType.rightClickEntity:
            int entityId = dis.readInt();
            Entity smacked = shadow.getEntityByID(entityId);
            if (smacked == null) {
                Core.logWarning("%s clicked unknown entity %s", player, entityId);
                return;
            }
            if (packetType == HammerNetType.rightClickEntity) {
                ipp.interactWith(smacked);
            } else {
                ipp.attackTargetEntityWithCurrentItem(smacked);
            }
            break;
        case HammerNetType.leftClickBlock:
        case HammerNetType.rightClickBlock:
            int hitX = dis.readInt();
            int hitY = dis.readInt();
            int hitZ = dis.readInt();
            int sideHit = dis.readInt();
            float xOffset = dis.readFloat();
            float yOffset = dis.readFloat();
            float zOffset = dis.readFloat();
            
            if (packetType == HammerNetType.leftClickBlock) {
            } else if (packetType == HammerNetType.rightClickBlock) {
                ItemStack is = player.getHeldItem();
                ipp.theItemInWorldManager.activateBlockOrUseItem(ipp, shadow, is, hitX, hitY, hitZ, sideHit, xOffset, yOffset, zOffset);
            }
            break;
        default:
            ((NetServerHandler) handler).kickPlayerFromServer("DeltaChunk: bad click packet");
            break;
        }
        
    }
    
    void setRotation(DataInputStream dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotation(q);
        }
    }
    
    void setRotationalVelocity(DataInputStream dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotationalVelocity(q);
        }
    }
    
    static Packet131MapData makePacket(short type, Object... items) {
        ByteArrayDataOutput dos = ByteStreams.newDataOutput();
        for (int i = 0; i < items.length; i++) {
            Object obj = items[i];
            if (obj instanceof Quaternion) {
                ((Quaternion) obj).write(dos);
            } else if (obj instanceof Integer) {
                dos.writeInt((Integer) obj);
            } else if (obj instanceof Float) {
                dos.writeFloat((Float) obj);
            } else {
                throw new IllegalArgumentException("Can only do Quaternions/Integers/Floats!");
            }
        }
        return PacketDispatcher.getTinyPacket(Hammer.instance, type, dos.toByteArray());
    }
}
