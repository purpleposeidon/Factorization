package factorization.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.ITinyPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IEntityMessage;
import factorization.api.Quaternion;
import factorization.api.VectorUV;
import factorization.notify.NotifyImplementation;

public class NetworkFactorization implements ITinyPacketHandler {
    protected final static short factorizeTEChannel = 0; //used for tile entities
    
    protected final static short factorizeCmdChannel = 2; //used for player keys
    protected final static short factorizeNtfyChannel = 3; //used to show messages in-world
    protected final static short factorizeEntityChannel = 4; //used for entities

    public NetworkFactorization() {
        Core.network = this;
    }
    
    public static final ItemStack EMPTY_ITEMSTACK = new ItemStack(0, 0, 0);
    
    int huge_tag_warnings = 0;

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
                final Item is_item = is.getItem();
                NBTTagCompound orig_tag = is.getTagCompound();
                if (FactorizationUtil.isTagBig(tag, 1024) >= 1024) {
                    is.setTagCompound(null);
                    if (huge_tag_warnings == 0) {
                        Core.logWarning("FIXME: Need to add in Item.getTagForClient"); //TODO
                    }
                    if (huge_tag_warnings++ < 10) {
                        Core.logWarning("Item " + is + " has a large NBT tag; it won't be sent over the wire.");
                        if (huge_tag_warnings == 10) {
                            Core.logWarning("(This will no longer be logged)");
                        }
                    }
                }
                is.writeToNBT(tag);
                NBTBase.writeNamedTag(tag, output);
                is.setTagCompound(orig_tag);
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
            } else {
                throw new RuntimeException("Don't know how to serialize " + item.getClass() + " (" + item + ")");
            }
        }
    }
    
    public void prefixTePacket(DataOutputStream output, Coord src, int messageType) throws IOException {
        output.writeInt(src.x);
        output.writeInt(src.y);
        output.writeInt(src.z);
        output.writeShort(messageType);
    }
    
    public Packet TEmessagePacket(ByteArrayOutputStream outputStream) throws IOException {
        outputStream.flush();
        return PacketDispatcher.getTinyPacket(Core.instance, factorizeTEChannel, outputStream.toByteArray());
    }
    
    @SuppressWarnings("resource")
    public Packet TEmessagePacket(Coord src, int messageType, Object... items) { //TODO: messageType should be a short
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            prefixTePacket(output, src, messageType);
            writeObjects(outputStream, output, items);
            return TEmessagePacket(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Packet notifyPacket(Object where, ItemStack item, String format, String ...args) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            
            if (where instanceof Vec3) {
                output.writeByte(NotifyMessageType.VEC3);
                Vec3 v = (Vec3) where;
                output.writeDouble(v.xCoord);
                output.writeDouble(v.yCoord);
                output.writeDouble(v.zCoord);
            } else if (where instanceof Coord) {
                output.writeByte(NotifyMessageType.COORD);
                Coord c = (Coord) where;
                output.writeInt(c.x);
                output.writeInt(c.y);
                output.writeInt(c.z);
            } else if (where instanceof Entity) {
                output.writeByte(NotifyMessageType.ENTITY);
                Entity ent = (Entity) where;
                output.writeInt(ent.entityId);
            } else if (where instanceof TileEntity) {
                output.writeByte(NotifyMessageType.TILEENTITY);
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
            NBTBase.writeNamedTag(tag, output);
            
            output.writeUTF(format);
            output.writeInt(args.length);
            for (String a : args) {
                output.writeUTF(a);
            }
            output.flush();
            return PacketDispatcher.getTinyPacket(Core.instance, factorizeNtfyChannel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @SuppressWarnings("resource")
    public Packet entityPacket(Entity to, int messageType, Object ...items) { //TODO: messageType should be short
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);

            output.writeInt(to.entityId);
            output.writeShort(messageType);

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
                    final Item is_item = is.getItem();
                    if (is_item == null || is_item.getShareTag()) {
                        is.writeToNBT(tag);
                    } else {
                        NBTTagCompound backup = is.getTagCompound();
                        is.writeToNBT(tag);
                        is.setTagCompound(backup);
                    }
                    NBTBase.writeNamedTag(tag, output);
                    if (outputStream.size() > 65536 && is.hasTagCompound()) {
                        //Got an overflow! We'll blame the NBT tag.
                        if (huge_tag_warnings++ < 10) {
                            Core.logWarning("Item " + is + " probably has a huge NBT tag; it will be stripped from the packet; packet for entity " + to);
                            if (huge_tag_warnings == 10) {
                                Core.logWarning("(This will no longer be logged)");
                            }
                        }
                        NBTTagCompound tag_copy = is.getTagCompound();
                        is.setTagCompound(null);
                        try {
                            return entityPacket(to, messageType, items);
                        } finally {
                            is.setTagCompound(tag_copy);
                        }
                    }
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
                } else {
                    throw new RuntimeException("Don't know how to serialize " + item.getClass() + " (" + item + ")");
                }
            }
            output.flush();
            return PacketDispatcher.getTinyPacket(Core.instance, factorizeEntityChannel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void sendCommand(EntityPlayer player, Command cmd, byte arg) {
        byte data[] = new byte[2];
        data[0] = cmd.id;
        data[1] = arg;
        Packet packet = PacketDispatcher.getTinyPacket(Core.instance, factorizeCmdChannel, data);
        Core.proxy.addPacket(player, packet);
    }

    public void broadcastMessage(EntityPlayer who, Coord src, int messageType, Object... msg) {
        //		// who is ignored
        //		if (!Core.proxy.isServer() && who == null) {
        //			return;
        //		}
        Packet toSend = TEmessagePacket(src, messageType, msg);
        if (who == null || !who.worldObj.isRemote) {
            broadcastPacket(who, src, toSend);
        }
        else {
            Core.proxy.addPacket(who, toSend);
        }
    }

    private double maxBroadcastDistSq = 2 * Math.pow(64, 2);
    /**
     * @param who
     *            Player to send packet to; if null, send to everyone in range.
     * @param src
     *            Where the packet originated from. Ignored of player != null
     * @param toSend
     */
    public void broadcastPacket(EntityPlayer who, Coord src, Packet toSend) {
        if (src.w == null) {
            new NullPointerException("Coord is null").printStackTrace();
            return;
        }
        if (who == null) {
            //send to everyone in range
            Chunk srcChunk = src.getChunk();
            for (EntityPlayer player : (Iterable<EntityPlayer>) src.w.playerEntities) {
                //XXX TODO: Make this not lame!
                double x = src.x - player.posX;
                double z = src.z - player.posZ;
                if (x*x + z*z > maxBroadcastDistSq) {
                    continue;
                }
                if (!Core.proxy.playerListensToCoord(player, src)) {
                    continue;
                }
                Core.proxy.addPacket(player, toSend);
            }
        }
        else {
            Core.proxy.addPacket(who, toSend);
        }
    }
    
    public void broadcastPacket(World world, int xCoord, int yCoord, int zCoord, Packet toSend) {
        if (toSend == null) {
            return;
        }
        if (world.isRemote) {
            return;
        }
        WorldServer w = (WorldServer) world;
        PlayerInstance pi = w.getPlayerManager().getOrCreateChunkWatcher(xCoord >> 4, zCoord >> 4, false);
        if (pi == null) {
            return;
        }
        pi.sendToAllPlayersWatchingChunk(toSend);
    }

    static final private ThreadLocal<EntityPlayer> currentPlayer = new ThreadLocal<EntityPlayer>();

    EntityPlayer getCurrentPlayer() {
        EntityPlayer ret = currentPlayer.get();
        if (ret == null) {
            throw new NullPointerException("currentPlayer wasn't set");
        }
        return ret;
    }
    
    @Override
    public void handle(NetHandler handler, Packet131MapData mapData) {
        handlePacketData(handler, mapData.uniqueID, mapData.itemData, handler.getPlayer());
    }
    
    void handlePacketData(NetHandler handler, int channel, byte[] data, EntityPlayer me) {
        currentPlayer.set(me);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        DataInputStream input = new DataInputStream(inputStream);
        switch (channel) {
        case factorizeTEChannel: handleTE(input); break;
        case factorizeCmdChannel: handleCmd(data); break;
        case factorizeNtfyChannel: handleNtfy(input); break;
        case factorizeEntityChannel: handleEntity(input); break;
        default: Core.logWarning("Got packet with invalid channel %s with player = %s ", channel, me); break;
        }

        currentPlayer.set(null);
    }

    void handleTE(DataInputStream input) {
        try {
            World world = getCurrentPlayer().worldObj;
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            int messageType = input.readShort();
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
                    broadcastPacket(getCurrentPlayer(), here, tec.getDescriptionPacket());
                }
                return;
            }

            if (messageType == MessageType.FactoryType && world.isRemote) {
                //create a Tile Entity of that type there.
                FactoryType ft = FactoryType.fromMd(input.readInt());
                byte extraData = input.readByte();
                byte extraData2 = input.readByte();
                //There may be additional description data following this
                try {
                    messageType = input.readInt();
                } catch (IOException e) {
                    messageType = -1;
                }
                TileEntityCommon spawn = here.getTE(TileEntityCommon.class);
                if (spawn != null && spawn.getFactoryType() != ft) {
                    world.removeBlockTileEntity(x, y, z);
                    spawn = null;
                }
                if (spawn == null) {
                    spawn = ft.makeTileEntity();
                    spawn.worldObj = world;
                    world.setBlockTileEntity(x, y, z, spawn);
                }

                spawn.useExtraInfo(extraData);
                spawn.useExtraInfo2(extraData2);
            }

            if (messageType == -1) {
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

    void handleForeignMessage(World world, int x, int y, int z, TileEntity ent, int messageType, DataInputStream input) throws IOException {
        if (!world.isRemote) {
            //Nothing for the server to deal with
        } else {
            Coord here = new Coord(world, x, y, z);
            switch (messageType) {
            case MessageType.PlaySound:
                Sound.receive(input);
                break;
            case MessageType.PistonPush:
                Block.pistonBase.onBlockEventReceived(world, x, y, z, 0, input.readInt());
                here.setId(0);
                break;
            case MessageType.BarrelLoss:
                TileEntityBarrel.spawnBreakParticles(here, input.readInt());
                break;
            default:
                if (here.blockExists() && here.getId() != 0) {
                    Core.logFine("Got unhandled message: " + messageType + " for " + here);
                } else {
                    //XXX: Need to figure out how to keep the server from sending these things!
                    Core.logFine("Got message to unloaded chunk: " + messageType + " for " + here);
                }
                break;
            }
        }

    }
    
    void handleCmd(byte[] data) {
        if (data == null || data.length < 2) {
            return;
        }
        byte s = data[0];
        byte arg = data[1];
        Command.fromNetwork(getCurrentPlayer(), s, arg);
    }

    void handleNtfy(DataInputStream input) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            EntityPlayer me = getCurrentPlayer();
            if (!me.worldObj.isRemote) {
                return;
            }
            try {
                Object target = null;
                int x, y, z;
                switch (input.readByte()) {
                case NotifyMessageType.COORD:
                    x = input.readInt();
                    y = input.readInt();
                    z = input.readInt();
                    target = new Coord(me.worldObj, x, y, z);
                    break;
                case NotifyMessageType.ENTITY:
                    int id = input.readInt();
                    if (id == me.entityId) {
                        target = me; //bebna
                    } else {
                        target = me.worldObj.getEntityByID(id);
                    }
                    break;
                case NotifyMessageType.TILEENTITY:
                    x = input.readInt();
                    y = input.readInt();
                    z = input.readInt();
                    target = me.worldObj.getBlockTileEntity(x, y, z);
                    break;
                case NotifyMessageType.VEC3:
                    target = Vec3.createVectorHelper(input.readDouble(), input.readDouble(), input.readDouble());
                    break;
                default: return;
                }
                if (target == null) {
                    return;
                }
                
                NBTTagCompound tag = (NBTTagCompound) NBTBase.readNamedTag(input);
                ItemStack item = ItemStack.loadItemStackFromNBT(tag);
                if (item != null && EMPTY_ITEMSTACK.isItemEqual(item)) {
                    item = null;
                }
                
                String msg = input.readUTF();
                int argCount = input.readInt();
                String args[] = new String[argCount];
                for (int i = 0; i < argCount; i++) {
                    args[i] = input.readUTF();
                }
                NotifyImplementation.recieve(me, target, item, msg, args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    void handleEntity(DataInputStream input) {
        try {
            World world = getCurrentPlayer().worldObj;
            if (!world.isRemote) {
                return;
            }
            int entityId = input.readInt();
            short messageType = input.readShort();
            Entity to = world.getEntityByID(entityId);
            if (to == null) {
                if (Core.dev_environ) {
                    //Core.logFine("Packet to unknown entity #%s: %s", entityId, messageType);
                }
                return;
            }
            
            if (!(to instanceof IEntityMessage)) {
                Core.logFine("Packet to inappropriate entity #%s: %s", entityId, messageType);
            }
            IEntityMessage iem = (IEntityMessage) to;
            
            if (Core.debug_network) {
                Core.logFine("EntityNet: " + messageType + "      " + to);
            }
            
            boolean handled;
            iem.handleMessage(messageType, input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static public class MessageType {
        //Non TEF messages
        public final static int ShareAll = -1;
        public final static int PlaySound = 11, PistonPush = 12;
        //TEF messages
        public final static int
                DrawActive = 0, FactoryType = 1, DescriptionRequest = 2, DataHelperEdit = 3, OpenDataHelperGui = 4,
                //
                RouterSlot = 20, RouterTargetSide = 21, RouterMatch = 22, RouterIsInput = 23,
                RouterLastSeen = 24, RouterMatchToVisit = 25, RouterDowngrade = 26,
                RouterUpgradeState = 27, RouterEjectDirection = 28,
                //
                BarrelDescription = 40, BarrelItem = 41, BarrelCount = 42, BarrelLoss = 43,
                //
                BatteryLevel = 50, LeydenjarLevel = 51,
                //
                MirrorDescription = 60,
                //
                TurbineWater = 70, TurbineSpeed = 71,
                //
                HeaterHeat = 80,
                //
                GrinderSpeed = 90, LaceratorSpeed = 91,
                //
                MixerSpeed = 100,
                //
                CrystallizerInfo = 110,
                //
                WireFace = 121,
                //
                SculptDescription = 130, SculptNew = 132, SculptMove = 133, SculptRemove = 134, SculptState = 135,
                //
                ExtensionInfo = 150, RocketState = 151,
                //
                ServoRailDecor = 161, ServoRailDecorUpdate = 162,
                //
                CompressionCrafter = 163, CompressionCrafterBeginCrafting = 164, CompressionCrafterBounds = 165;
    }
    
    static public class NotifyMessageType {
        public static final byte COORD = 0, VEC3 = 1, ENTITY = 2, TILEENTITY = 3;
    }
    
    public static ItemStack nullItem(ItemStack is) {
        return is == null ? EMPTY_ITEMSTACK : is;
    }
}
