package factorization.fzds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.network.ITinyPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import factorization.api.Quaternion;


public class HammerNet implements ITinyPacketHandler {
    public static class HammerNetType {
        public static final short rotation = 0, rotationVelocity = 1, rotationBoth = 2;
    }
    
    @Override
    public void handle(NetHandler handler, Packet131MapData mapData) {
        ByteArrayInputStream bais = new ByteArrayInputStream(mapData.itemData);
        DataInputStream dis = new DataInputStream(bais);
        if (!handler.getPlayer().worldObj.isRemote) {
            return;
        }
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
            dse.rotation = q;
        }
    }
    
    void setRotationalVelocity(DataInputStream dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.rotationalVelocity = q;
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
            } else {
                throw new IllegalArgumentException("Can only do Quaternions and Integers!");
            }
        }
        return PacketDispatcher.getTinyPacket(Hammer.instance, type, dos.toByteArray());
    }
}
