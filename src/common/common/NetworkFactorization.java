package factorization.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.minecraft.src.Block;
import net.minecraft.src.Chunk;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet131MapData;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.StringTranslate;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.ITinyPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import factorization.api.Coord;
import factorization.api.VectorUV;

public class NetworkFactorization implements ITinyPacketHandler {
    protected final static short factorizeTEChannel = 0; //used for tile entities
    protected final static short factorizeMsgChannel = 1; //used for sending translatable chat messages
    protected final static short factorizeCmdChannel = 2; //used for player keys
    protected final static short factorizeNtfyChannel = 3; //used to show messages in-world

    public NetworkFactorization() {
        Core.network = this;
    }

    public Packet TEmessagePacket(Coord src, int messageType, Object... items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);

            output.writeInt(src.x);
            output.writeInt(src.y);
            output.writeInt(src.z);
            output.writeInt(messageType);

            for (Object item : items) {
                if (item == null) {
                    throw new RuntimeException("Argument is null!");
                }
                if (item instanceof Integer) {
                    output.writeInt((Integer) item);
                } else if (item instanceof Byte) {
                    output.writeByte((Byte) item);
                } else if (item instanceof String) {
                    output.writeUTF((String) item);
                } else if (item instanceof Boolean) {
                    output.writeBoolean((Boolean) item);
                } else if (item instanceof Float) {
                    output.writeFloat((Float) item);
                } else if (item instanceof ItemStack) {
                    NBTTagCompound tag = new NBTTagCompound();
                    ((ItemStack) item).writeToNBT(tag);
                    FactorizationHack.tagWrite(tag, output);
                } else if (item instanceof VectorUV) {
                    VectorUV v = (VectorUV) item;
                    output.writeFloat(v.x);
                    output.writeFloat(v.y);
                    output.writeFloat(v.z);
                } else {
                    throw new RuntimeException("Argument is not Integer/Byte/String/Boolean/Float/ItemStack/RenderingCube.Vector: " + item);
                }
            }
            output.flush();
            return PacketDispatcher.getTinyPacket(Core.instance, (short) 0, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Packet translatePacket(String... items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            for (String i : items) {
                output.writeUTF(i);
            }
            output.flush();
            return PacketDispatcher.getTinyPacket(Core.instance, factorizeMsgChannel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Packet notifyPacket(Coord where, String format, String ...args) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeInt(where.x);
            output.writeInt(where.y);
            output.writeInt(where.z);
            output.writeUTF(format);
            output.writeInt(args.length);
            for (String a : args) {
                output.writeUTF(a);
            }
            output.flush();
            return new PacketDispatcher().getTinyPacket(Core.instance, factorizeNtfyChannel, outputStream.toByteArray());
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
            //PacketDispatcher.sendPacketToAllAround(src.x, src.y, src.z, 128, src.w.getWorldInfo().getDimension(), toSend);
            Chunk srcChunk = src.getChunk();
            for (EntityPlayer player : (Iterable<EntityPlayer>) src.w.playerEntities) {
//				if (player.chunksToLoad.contains(srcChunk)) {
//					Core.proxy.addPacket(player, toSend);
//				}
                //XXX TODO: Make this not lame!
                //if (entityplayermp.loadedChunks.contains(chunkcoordintpair))
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

    static final private ThreadLocal<EntityPlayer> currentPlayer = new ThreadLocal<EntityPlayer>();

    EntityPlayer getCurrentPlayer() {
        EntityPlayer ret = currentPlayer.get();
        if (ret == null) {
            throw new NullPointerException("currentPlayer was unset");
        }
        return ret;
    }
    
    void handlePacketData(int channel, byte[] data, EntityPlayer me) {
        currentPlayer.set(me);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        DataInput input = new DataInputStream(inputStream);
        switch (channel) {
        case factorizeTEChannel: handleTE(input); break;
        case factorizeMsgChannel: handleMsg(input); break;
        case factorizeCmdChannel: handleCmd(data); break;
        case factorizeNtfyChannel: handleNtfy(input); break;
        }

        currentPlayer.set(null);
    }
    
    @Override
    public void handle(NetHandler handler, Packet131MapData mapData) {
        handlePacketData(factorizeTEChannel, mapData.itemData, handler.getPlayer());
    }

    void handleTE(DataInput input) {
        try {
            World world = getCurrentPlayer().worldObj;
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            int messageType = input.readInt();
            Coord here = new Coord(world, x, y, z);
            
            if (Core.debug_network) {
                System.out.println("FactorNet: " + messageType + "      " + here);
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

                if (spawn != null) {
                    spawn.useExtraInfo(extraData);
                    spawn.useExtraInfo2(extraData2);
                }
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

    void handleMsg(DataInput input) {
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) {
            return; // so, an SMP client sends *us* a message? Nah.
        }
        String main;
        try {
            main = input.readUTF();
        } catch (IOException e1) {
            return;
        }
        ArrayList<String> items = new ArrayList<String>();
        try {
            while (true) {
                String orig = input.readUTF();
                String name = orig + ".name";
                String transd = StringTranslate.getInstance().translateKey(name);
                if (transd.compareTo(name) == 0) {
                    items.add(orig);
                } else {
                    items.add(transd);
                }
            }
        } catch (IOException e) {
        }
        try {
            getCurrentPlayer().addChatMessage(String.format(main, items.toArray()));
        } catch (IllegalFormatException e) {
            System.out.print("Illegal format: \"" + main + '"');
            for (String i : items) {
                System.out.print(" \"" + i + "\"");
            }
            System.out.println();
            e.printStackTrace();
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

    void handleForeignMessage(World world, int x, int y, int z, TileEntity ent, int messageType,
            DataInput input) throws IOException {
        if (!world.isRemote) {
            //Nothing for the server to deal with
        } else {
            Coord here = new Coord(world, x, y, z);
            switch (messageType) {
            case MessageType.DemonEnterChest:
                if (ent instanceof TileEntityChest) {
                    Core.proxy.pokeChest((TileEntityChest) ent);
                }
                break;
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
                if (world.blockExists(x, y, z)) {
                    Core.logInfo("Got unhandled message: " + messageType + " for " + here);
                }
                else {
                    //XXX: Need to figure out how to keep the server from sending these things!
                    Core.logInfo("Got message to unloaded chunk: " + messageType + " for " + here);
                }
                break;
            }
        }

    }

    void handleNtfy(DataInput input) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            EntityPlayer me = getCurrentPlayer();
            if (!me.worldObj.isRemote) {
                return;
            }
            try {
                int x = input.readInt(), y = input.readInt(), z = input.readInt();
                String msg = input.readUTF();
                int argCount = input.readInt();
                String args[] = new String[argCount];
                for (int i = 0; i < argCount; i++) {
                    args[i] = input.readUTF();
                }
                Core.notify(me, new Coord(me.worldObj, x, y, z), msg, args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    static public class MessageType {
        //Non TEF messages
        public final static int ShareAll = -1;
        public final static int DemonEnterChest = 10, PlaySound = 11, PistonPush = 12;
        //TEF messages
        public final static int
                DrawActive = 0, FactoryType = 1, DescriptionRequest = 2,
                //
                MakerTarget = 11,
                //
                RouterSlot = 20, RouterTargetSide = 21, RouterMatch = 22, RouterIsInput = 23,
                RouterLastSeen = 24, RouterMatchToVisit = 25, RouterDowngrade = 26,
                RouterUpgradeState = 27, RouterEjectDirection = 28,
                //
                BarrelDescription = 40, BarrelItem = 41, BarrelCount = 42, BarrelLoss = 43,
                //
                BatteryLevel = 50,
                //
                MirrorDescription = 60,
                //
                TurbineWater = 70, TurbineSpeed = 71,
                //
                HeaterHeat = 80,
                //
                GrinderSpeed = 90,
                //
                MixerSpeed = 100,
                //
                CrystallizerInfo = 110,
                //
                WireFace = 121,
                //
                SculptDescription = 130, SculptSelect = 131, SculptNew = 132, SculptMove = 133, SculptRemove = 134, SculptState = 135, SculptWater = 136,
                //
                FurnaceBurnTime = 140
                ;
    }

}
