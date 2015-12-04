package factorization.notify;

import factorization.util.DataUtil;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

public class NotifyNetwork {
    static final String channelName = "fzNotify";
    static FMLEventChannel channel;
    
    static final byte COORD = 0, VEC3 = 1, ENTITY = 2, TILEENTITY = 3, ONSCREEN = 4, REPLACEABLE = 5;
    
    public NotifyNetwork() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void recievePacket(ClientCustomPacketEvent event) {
        ByteBufInputStream input = new ByteBufInputStream(event.packet.payload());
        try {
            handleNotify(input, Minecraft.getMinecraft().thePlayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static final ItemStack EMPTY_ITEMSTACK = new ItemStack(Blocks.air);
    
    @SideOnly(Side.CLIENT)
    void handleNotify(DataInput input, EntityPlayer me) throws IOException {
        Object target = null;
        int pos;
        switch (input.readByte()) {
        case COORD:
            x = input.readInt();
            y = input.readInt();
            z = input.readInt();
            target = new SimpleCoord(me.world, pos);
            break;
        case ENTITY:
            int id = input.readInt();
            if (id == me.getEntityId()) {
                target = me; //bebna
            } else {
                target = me.worldObj.getEntityByID(id);
            }
            break;
        case TILEENTITY:
            x = input.readInt();
            y = input.readInt();
            z = input.readInt();
            target = me.worldObj.getTileEntity(pos);
            if (target == null) {
                target = new SimpleCoord(me.world, pos);
            }
            break;
        case VEC3:
            target = new Vec3(input.readDouble(), input.readDouble(), input.readDouble());
            break;
        case ONSCREEN:
            String message = input.readUTF();
            String[] formatArgs = readStrings(input);
            NotifyImplementation.proxy.onscreen(message, formatArgs);
            return;
        case REPLACEABLE:
            String str = input.readUTF();
            int msgKey = input.readInt();
            IChatComponent msg = IChatComponent.Serializer.func_150699_a(str);
            NotifyImplementation.proxy.replaceable(msg, msgKey);
            return;
        default: return;
        }
        if (target == null) {
            return;
        }
        
        ItemStack item = DataUtil.readStack(input);
        if (item != null && EMPTY_ITEMSTACK.isItemEqual(item)) {
            item = null;
        }
        
        String msg = input.readUTF();
        String args[] = readStrings(input);
        NotifyImplementation.recieve(me, target, item, msg, args);
    }
    
    
    static void broadcast(FMLProxyPacket packet, EntityPlayer player, TargetPoint area) {
        if (player == null) {
            NotifyNetwork.channel.sendToAll(packet);
        } else if (player instanceof EntityPlayerMP) {
            NotifyNetwork.channel.sendTo(packet, (EntityPlayerMP) player);
        }
    }
    
    private static void writeStrings(DataOutputStream output, String[] args) throws IOException {
        output.writeByte((byte) args.length);
        for (String s : args) {
            if (s == null) s = "null";
            output.writeUTF(s);
        }
    }
    
    private static String[] readStrings(DataInput input)  throws IOException {
        String[] ret = new String[input.readByte()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = input.readUTF();
        }
        return ret;
    }
    
    static FMLProxyPacket notifyPacket(Object where, ItemStack item, String format, String ...args) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            
            if (where instanceof Vec3) {
                output.writeByte(VEC3);
                Vec3 v = (Vec3) where;
                output.writeDouble(v.xCoord);
                output.writeDouble(v.yCoord);
                output.writeDouble(v.zCoord);
            } else if (where instanceof ISaneCoord) {
                output.writeByte(COORD);
                ISaneCoord c = (ISaneCoord) where;
                output.writeInt(c.x());
                output.writeInt(c.y());
                output.writeInt(c.z());
            } else if (where instanceof Entity) {
                output.writeByte(ENTITY);
                Entity ent = (Entity) where;
                output.writeInt(ent.getEntityId());
            } else if (where instanceof TileEntity) {
                output.writeByte(TILEENTITY);
                TileEntity te = (TileEntity) where;
                output.writeInt(te.getPos().getX());
                output.writeInt(te.getPos().getY());
                output.writeInt(te.getPos().getZ());
            } else {
                return null;
            }
            
            if (item == null) {
                item = EMPTY_ITEMSTACK;
            }
            NBTTagCompound tag = new NBTTagCompound();
            item.writeToNBT(tag);
            CompressedStreamTools.write(tag, output);
            
            output.writeUTF(format);
            writeStrings(output, args);
            output.flush();
            return generate(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static FMLProxyPacket onscreenPacket(String message, String[] formatArgs) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeByte(ONSCREEN);
            output.writeUTF(message);
            writeStrings(output, formatArgs);
            output.flush();
            return generate(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static FMLProxyPacket replaceableChatPacket(IChatComponent msg, int msgKey) {
        String str = IChatComponent.Serializer.func_150696_a(msg);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeByte(REPLACEABLE);
            output.writeUTF(str);
            output.writeInt(msgKey);
            output.flush();
            return generate(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    public static FMLProxyPacket generate(ByteArrayOutputStream baos) {
        return new FMLProxyPacket(Unpooled.wrappedBuffer(baos.toByteArray()), channelName);
    }
}
