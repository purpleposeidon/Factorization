package factorization.shared;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IEntityMessage;
import factorization.api.Quaternion;
import factorization.api.VectorUV;
import factorization.common.Command;
import factorization.common.FactoryType;

public class NetworkFactorization {
    public static final ItemStack EMPTY_ITEMSTACK = new ItemStack(Blocks.air);
    
    private void writeObjects(ByteArrayOutputStream outputStream, DataOutputStream output, Object... items) throws IOException {
        for (Object item : items) {
            if (item == null) {
                throw new RuntimeException("Argument is null!");
            }
            if (item instanceof Integer) {
                output.writeInt((Integer) item);
            } else if (item instanceof Byte) {
                output.writeByte((Byte) item);
            } else if (item instanceof Short) {
                output.writeShort((Short) item);
            } else if (item instanceof String) {
                output.writeUTF((String) item);
            } else if (item instanceof Boolean) {
                output.writeBoolean((Boolean) item);
            } else if (item instanceof Float) {
                output.writeFloat((Float) item);
            } else if (item instanceof ItemStack) {
                ItemStack is = (ItemStack) item;
                NBTTagCompound tag = new NBTTagCompound();
                is.writeToNBT(tag);
                CompressedStreamTools.write(tag, output);
            } else if (item instanceof VectorUV) {
                VectorUV v = (VectorUV) item;
                output.writeFloat((float) v.x);
                output.writeFloat((float) v.y);
                output.writeFloat((float) v.z);
            } else if (item instanceof DeltaCoord) {
                DeltaCoord dc = (DeltaCoord) item;
                dc.write(output);
            } else if (item instanceof Quaternion) {
                Quaternion q = (Quaternion) item;
                q.write(output);
            } else if (item instanceof byte[]) {
                byte[] b = (byte[]) item;
                output.write(b, 0, b.length);
            } else if (item instanceof MessageType) {
                MessageType mt = (MessageType) item;
                mt.write(output);
            } else {
                throw new RuntimeException("Don't know how to serialize " + item.getClass() + " (" + item + ")");
            }
        }
    }
    
    public void prefixTePacket(DataOutputStream output, Coord src, MessageType messageType) throws IOException {
        messageType.write(output);
        output.writeInt(src.x);
        output.writeInt(src.y);
        output.writeInt(src.z);
    }
    
    public FMLProxyPacket TEmessagePacket(Coord src, MessageType messageType, Object... items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            prefixTePacket(output, src, messageType);
            writeObjects(outputStream, output, items);
            return FzNetDispatch.generate(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void prefixEntityPacket(DataOutputStream output, Entity to, MessageType messageType) throws IOException {
        messageType.write(output);
        output.writeInt(to.getEntityId());
    }
    
    public FMLProxyPacket entityPacket(ByteArrayOutputStream outputStream) throws IOException {
        outputStream.flush();
        return FzNetDispatch.generate(outputStream);
    }
    
    public FMLProxyPacket entityPacket(Entity to, MessageType messageType, Object ...items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            prefixEntityPacket(output, to, messageType);
            writeObjects(outputStream, output, items);
            return entityPacket(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void sendCommand(EntityPlayer player, Command cmd, byte arg) {
        try {
            ByteBuf buf = Unpooled.buffer();
            ByteBufOutputStream bo = new ByteBufOutputStream(buf);
            MessageType.factorizeCmdChannel.write(bo);
            bo.writeByte(cmd.id);
            bo.writeByte(arg);
            FzNetDispatch.addPacket(FzNetDispatch.generate(bo), player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(EntityPlayer who, Coord src, MessageType messageType, Object... msg) {
        if (who != null) {
            FMLProxyPacket toSend = TEmessagePacket(src, messageType, msg);
            FzNetDispatch.addPacket(toSend, who);
        } else {
            FMLProxyPacket toSend = TEmessagePacket(src, messageType, msg);
            FzNetDispatch.addPacketFrom(toSend, src);
        }
    }

    public void broadcastPacket(EntityPlayer who, Coord src, FMLProxyPacket toSend) {
        if (who != null) {
            FzNetDispatch.addPacket(toSend, who);
        } else {
            FzNetDispatch.addPacketFrom(toSend, src);
        }
    }

    void handleTE(DataInput input, MessageType messageType, EntityPlayer player) {
        try {
            World world = player.worldObj;
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            Coord here = new Coord(world, x, y, z);
            
            if (Core.debug_network) {
                Core.logFine("FactorNet: " + messageType + "      " + here);
            }

            if (!here.blockExists() && world.isRemote) {
                // I suppose we can't avoid this.
                // (Unless we can get a proper server-side check)
                return;
            }
            
            if (messageType == MessageType.DescriptionRequest && !world.isRemote) {
                TileEntityCommon tec = here.getTE(TileEntityCommon.class);
                if (tec != null) {
                    FzNetDispatch.addPacket(tec.getDescriptionPacket(), player);
                }
                return;
            }
            
            if (messageType == MessageType.RedrawOnClient && world.isRemote) {
                world.markBlockForUpdate(x, y, z);
                return;
            }

            if ((messageType == MessageType.FactoryType || messageType == MessageType.FactoryTypeWithSecondMessage) && world.isRemote) {
                //create a Tile Entity of that type there.
                FactoryType ft = FactoryType.fromMd(input.readInt());
                byte extraData = input.readByte();
                byte extraData2 = input.readByte();
                if (messageType == MessageType.FactoryTypeWithSecondMessage) {
                    messageType = MessageType.read(input);
                } else {
                    messageType = null;
                }
                TileEntityCommon spawn = here.getTE(TileEntityCommon.class);
                if (spawn != null && spawn.getFactoryType() != ft) {
                    world.removeTileEntity(x, y, z);
                    spawn = null;
                }
                if (spawn == null) {
                    spawn = ft.makeTileEntity();
                    spawn.setWorldObj(world);
                    world.setTileEntity(x, y, z, spawn);
                    here.redraw();
                }

                spawn.useExtraInfo(extraData);
                spawn.useExtraInfo2(extraData2);
            }

            if (messageType == null) {
                return;
            }

            TileEntityCommon tec = here.getTE(TileEntityCommon.class);
            if (tec == null) {
                handleForeignMessage(world, x, y, z, tec, messageType, input);
                return;
            }
            boolean handled;
            if (here.w.isRemote) {
                handled = tec.handleMessageFromServer(messageType, input);
            } else {
                handled = tec.handleMessageFromClient(messageType, input);
            }
            if (!handled) {
                handleForeignMessage(world, x, y, z, tec, messageType, input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handleForeignMessage(World world, int x, int y, int z, TileEntity ent, MessageType messageType, DataInput input) throws IOException {
        if (!world.isRemote) {
            //Nothing for the server to deal with
        } else {
            Coord here = new Coord(world, x, y, z);
            switch (messageType) {
            case PlaySound:
                Sound.receive(here, input);
                break;
            default:
                if (here.blockExists()) {
                    //Core.logFine("Got unhandled message: " + messageType + " for " + here);
                } else {
                    //XXX: Need to figure out how to keep the server from sending these things!
                    Core.logFine("Got message to unloaded chunk: " + messageType + " for " + here);
                }
                break;
            }
        }

    }
    
    boolean handleForeignEntityMessage(Entity ent, MessageType messageType, DataInput input) throws IOException {
        if (messageType == MessageType.EntityParticles) {
            Random rand = new Random();
            double px = rand.nextGaussian() * 0.02;
            double py = rand.nextGaussian() * 0.02;
            double pz = rand.nextGaussian() * 0.02;
            
            byte count = input.readByte();
            String type = input.readUTF();
            for (int i = 0; i < count; i++) {
                ent.worldObj.spawnParticle(type,
                        ent.posX + rand.nextFloat() * ent.width * 2.0 - ent.width,
                        ent.posY + 0.5 + rand.nextFloat() * ent.height,
                        ent.posZ + rand.nextFloat() * ent.width * 2.0 - ent.width,
                        px, py, pz);
            }
            return true;
        }
        return false;
    }
    
    void handleCmd(DataInput data, EntityPlayer player) throws IOException {
        byte s = data.readByte();
        byte arg = data.readByte();
        Command.fromNetwork(player, s, arg);
    }
    
    void handleEntity(MessageType messageType, DataInput input, EntityPlayer player) {
        try {
            World world = player.worldObj;
            int entityId = input.readInt();
            Entity to = world.getEntityByID(entityId);
            if (to == null) {
                if (Core.dev_environ) {
                    Core.logFine("Packet to unknown entity #%s: %s", entityId, messageType);
                }
                return;
            }
            
            if (!(to instanceof IEntityMessage)) {
                if (!handleForeignEntityMessage(to, messageType, input)) {
                    Core.logFine("Packet to inappropriate entity #%s: %s", entityId, messageType);
                }
                return;
            }
            IEntityMessage iem = (IEntityMessage) to;
            
            if (Core.debug_network) {
                Core.logFine("EntityNet: " + messageType + "      " + to);
            }
            
            boolean handled;
            if (world.isRemote) {
                handled = iem.handleMessageFromServer(messageType, input);
            } else {
                handled = iem.handleMessageFromClient(messageType, input);
            }
            if (!handled) {
                if (!handleForeignEntityMessage(to, messageType, input)) {
                    Core.logFine("Got unhandled message: " + messageType + " for " + iem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private static byte message_type_count = 0;
    static public enum MessageType {
        factorizeCmdChannel,
        PlaySound, EntityParticles(true),
        
        DrawActive, FactoryType, FactoryTypeWithSecondMessage, DescriptionRequest, DataHelperEdit, RedrawOnClient, DataHelperEditOnEntity(true), OpenDataHelperGui, OpenDataHelperGuiOnEntity(true),
        TileEntityMessageOnEntity(true),
        BarrelDescription, BarrelItem, BarrelCount, BarrelDoubleClickHack,
        BatteryLevel, LeydenjarLevel,
        MirrorDescription,
        TurbineWater, TurbineSpeed,
        HeaterHeat,
        LaceratorSpeed,
        MixerSpeed, FanturpellerSpeed,
        CrystallizerInfo,
        WireFace,
        SculptDescription, SculptNew, SculptMove, SculptRemove, SculptState,
        ExtensionInfo, RocketState,
        ServoRailDecor, ServoRailEditComment,
        CompressionCrafter, CompressionCrafterBeginCrafting, CompressionCrafterBounds,
        
        servo_brief(true), servo_item(true), servo_complete(true), servo_stopped(true);
        
        public boolean isEntityMessage;
        private static final MessageType[] valuesCache = values();
        
        private final byte id;
        MessageType() {
            this(false);
        }
        
        MessageType(boolean isEntity) {
            id = message_type_count++;
            if (id < 0) {
                throw new IllegalArgumentException("Too many message types!");
            }
            isEntityMessage = isEntity;
        }
        
        private static MessageType fromId(byte id) {
            if (id < 0 || id >= valuesCache.length) {
                return null;
            }
            return valuesCache[id];
        }
        
        public static MessageType read(DataInput in) throws IOException {
            byte b = in.readByte();
            MessageType mt = fromId(b);
            if (mt == null) {
                throw new IOException("Unknown type: " + b);
            }
            return mt;
        }
        
        public void write(DataOutput out) throws IOException {
            out.writeByte(id);
        }
        
    }
    
    public static ItemStack nullItem(ItemStack is) {
        return is == null ? EMPTY_ITEMSTACK : is;
    }
    
    public static ItemStack denullItem(ItemStack is) {
        if (is == null) return null;
        if (FzUtil.getId(is) == FzUtil.getId(Blocks.air)) return null;
        return is;
    }
}
