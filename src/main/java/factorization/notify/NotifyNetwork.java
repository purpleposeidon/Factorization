package factorization.notify;

import factorization.api.Coord;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NotifyNetwork {
    static final String channelName = "fzNotify";
    static FMLEventChannel channel;
    
    static final byte COORD = 0, VEC3 = 1, ENTITY = 2, TILEENTITY = 3, ONSCREEN = 4;
    
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
        int x, y, z;
        switch (input.readByte()) {
        case COORD:
            x = input.readInt();
            y = input.readInt();
            z = input.readInt();
            target = new Coord(me.worldObj, x, y, z);
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
            target = me.worldObj.getTileEntity(x, y, z);
            break;
        case VEC3:
            target = Vec3.createVectorHelper(input.readDouble(), input.readDouble(), input.readDouble());
            break;
        case ONSCREEN:
            String message = input.readUTF();
            String[] formatArgs = readStrings(input);
            NotifyImplementation.proxy.onscreen(message, formatArgs);
            return;
        default: return;
        }
        if (target == null) {
            return;
        }
        
        NBTTagCompound tag = CompressedStreamTools.read(input);
        ItemStack item = ItemStack.loadItemStackFromNBT(tag);
        if (item != null && EMPTY_ITEMSTACK.isItemEqual(item)) {
            item = null;
        }
        
        String msg = input.readUTF();
        String args[] = readStrings(input);
        NotifyImplementation.recieve(me, target, item, msg, args);
    }
    
    
    static void broadcast(FMLProxyPacket packet, EntityPlayer player) {
        if (player == null) {
            NotifyNetwork.channel.sendToAll(packet);
        } else if (player instanceof EntityPlayerMP) {
            NotifyNetwork.channel.sendTo(packet, (EntityPlayerMP) player);
        }
    }
    
    private static void writeStrings(DataOutputStream output, String[] args) throws IOException {
        output.writeByte((byte) args.length);
        for (String s : args) {
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
            } else if (where instanceof Coord) {
                output.writeByte(COORD);
                Coord c = (Coord) where;
                output.writeInt(c.x);
                output.writeInt(c.y);
                output.writeInt(c.z);
            } else if (where instanceof Entity) {
                output.writeByte(ENTITY);
                Entity ent = (Entity) where;
                output.writeInt(ent.getEntityId());
            } else if (where instanceof TileEntity) {
                output.writeByte(TILEENTITY);
                TileEntity te = (TileEntity) where;
                output.writeInt(te.xCoord);
                output.writeInt(te.yCoord);
                output.writeInt(te.zCoord);
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
    
    public static FMLProxyPacket generate(ByteArrayOutputStream baos) {
        return new FMLProxyPacket(Unpooled.wrappedBuffer(baos.toByteArray()), channelName);
    }
}
