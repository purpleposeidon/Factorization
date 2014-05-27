package factorization.fzds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;

import factorization.api.Quaternion;


public class HammerNet {
    public static class HammerNetType {
        public static final byte rotation = 0, rotationVelocity = 1, rotationBoth = 2,
                rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6, digPacket = 7;
    }
    
    public void handle(EntityPlayer player, DataInputStream dis) throws IOException {
        if (player == null || player.worldObj == null) {
            return; //Blah, what...
        }
        if (!player.worldObj.isRemote) {
            /*try {
                handleDseClick(handler, mapData);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            return;
        }
        World world = player.worldObj;
        short type = dis.readByte();
        int e_id = dis.readInt();
        Entity ent = world.getEntityByID(e_id);
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
    
    static FMLProxyPacket makePacket(short type, Object... items) {
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
        return new FMLProxyPacket(Unpooled.wrappedBuffer(dos.toByteArray()), HammerNetEventHandler.channelName);
    }
}
