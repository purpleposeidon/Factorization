package factorization.fzds;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import factorization.api.Quaternion;


public class HammerNet implements ITinyPacketHandler {
    public static class HammerNetType {
        public static final short rotation = 0, rotationVelocity = 1, rotationBoth = 2,
                rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6, digPacket = 7;
    }
    
    @Override
    public void handle(NetHandler handler, Packet131MapData mapData) {
        EntityPlayer player = handler.getPlayer();
        if (player == null || player.worldObj == null) {
            return; //Blah, what...
        }
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
    
    static Packet makePacket(short type, Object... items) {
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
