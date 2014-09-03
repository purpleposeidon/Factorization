package factorization.notify;

import factorization.api.Coord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum PointNetworkHandler {
    INSTANCE;
    String channelName = NotifyNetwork.channelName + "|point";
    FMLEventChannel channel;
    
    void initialize() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }
    
    @SubscribeEvent
    public void recievePacket(ServerCustomPacketEvent event) {
        ByteBufInputStream input = new ByteBufInputStream(event.packet.payload());
        try {
            EntityPlayer player = ((NetHandlerPlayServer) event.handler).playerEntity;
            handlePoint(input, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static final byte COORD = 1, ENTITY = 2;
    
    void handlePoint(DataInput input, EntityPlayer player) throws IOException {
        Notice notice = null;
        switch (input.readByte()) {
            default: return;
            case COORD: {
                int x = input.readInt();
                int y = input.readInt();
                int z = input.readInt();
                String msg = buildMessage(player, input);
                Coord at = new Coord(player.worldObj, x, y, z);
                notice = new Notice(at, msg);
                break;
            }
            case ENTITY: {
                int entityId = input.readInt();
                String msg = buildMessage(player, input);
                Entity ent = player.worldObj.getEntityByID(entityId);
                if (ent == null) return;
                notice = new Notice(ent, msg);
                break;
            }
        }
        notice.withStyle(Style.DRAWFAR, Style.LONG, Style.SCALE_SIZE);
        double maxDist = 0xFF * 0xFF;
        for (EntityPlayer viewer : (Iterable<EntityPlayer>) player.worldObj.playerEntities) {
            if (player.getDistanceSqToEntity(viewer) > maxDist) continue;
            notice.send(viewer);
        }
    }
    
    private String buildMessage(EntityPlayer player, DataInput input) throws IOException {
        String base = "<" + player.getCommandSenderName() + ">";
        String msg = input.readUTF();
        if (msg == null || msg.length() == 0) {
            return base;
        }
        return base + "\n" + msg;
    }
    
    @SideOnly(Side.CLIENT)
    void pointAtCoord(Coord at, String msg) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        ByteBufOutputStream out = new ByteBufOutputStream(buf);
        out.writeByte(COORD);
        out.writeInt(at.x);
        out.writeInt(at.y);
        out.writeInt(at.z);
        out.writeUTF(msg);
        out.close();
        send(buf);
    }
    
    @SideOnly(Side.CLIENT)
    void pointAtEntity(Entity ent, String msg) throws IOException {
        if (ent == null) return;
        ByteBuf buf = Unpooled.buffer();
        ByteBufOutputStream out = new ByteBufOutputStream(buf);
        out.writeByte(ENTITY);
        out.writeInt(ent.getEntityId());
        out.writeUTF(msg);
        out.close();
        send(buf);
    }
    
    @SideOnly(Side.CLIENT)
    void send(ByteBuf out) {
        channel.sendToServer(new FMLProxyPacket(out, channelName));
    }
}
