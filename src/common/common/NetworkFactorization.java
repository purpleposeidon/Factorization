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
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NetworkManager;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.StringTranslate;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import factorization.api.Coord;

public class NetworkFactorization implements IPacketHandler {
    protected final static String factorizeTEChannel = "factorizeTE"; //used for tile entities
    protected final static String factorizeMsgChannel = "factorizeMsg"; //used for sending translatable chat messages
    protected final static String factorizeCmdChannel = "factorizeCmd"; //used for player keys

    public NetworkFactorization() {
        //		if (Core.network != null) {
        //			throw new RuntimeException();
        //		}
        Core.network = this;
    }

    public Packet250CustomPayload messagePacket(Coord src, int messageType, Object... items) {
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
                } else if (item instanceof ItemStack) {
                    NBTTagCompound tag = new NBTTagCompound();
                    ((ItemStack) item).writeToNBT(tag);
                    FactorizationHack.tagWrite(tag, output);
                } else {
                    throw new RuntimeException("Argument is not Integer/Byte/String/Boolean/ItemStack: " + item);
                }
            }
            output.flush();
            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = factorizeTEChannel;
            packet.data = outputStream.toByteArray();
            packet.length = packet.data.length; // XXX this is stupid.
            return packet;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Packet250CustomPayload translatePacket(String... items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            for (String i : items) {
                output.writeUTF(i);
            }
            output.flush();
            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = factorizeMsgChannel;
            packet.data = outputStream.toByteArray();
            packet.length = packet.data.length;
            return packet;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendCommand(EntityPlayer player, Command cmd, byte arg) {
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = factorizeCmdChannel;
        packet.data = new byte[2];
        packet.data[0] = cmd.id;
        packet.data[1] = arg;
        packet.length = packet.data.length;
        Core.proxy.addPacket(player, packet);
    }

    public void broadcastMessage(EntityPlayer who, Coord src, int messageType, Object... msg) {
        //		// who is ignored
        //		if (!Core.proxy.isServer() && who == null) {
        //			return;
        //		}
        Packet toSend = messagePacket(src, messageType, msg);
        if (who == null || !who.worldObj.isRemote) {
            broadcastPacket(who, src, toSend);
        }
        else {
            Core.proxy.addPacket(who, toSend);
        }
    }

    /**
     * @param who
     *            Player to send packet to; if null, send to everyone in range.
     * @param src
     *            Where the packet originated from. Ignored of player != null
     * @param toSend
     */
    public void broadcastPacket(EntityPlayer who, Coord src, Packet toSend) {
        if (src.w == null) {
            return;
        }
        if (who == null) {
            //send to everyone in range
            int max_dist = (int) (3 * Math.pow(32, 2));
            for (EntityPlayer player : (Iterable<EntityPlayer>) src.w.playerEntities) {
                if (src.distanceSq(new Coord(player)) > max_dist) {
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

    @Override
    public void onPacketData(NetworkManager network, Packet250CustomPayload packet, Player player) {
        String channel = packet.channel;
        byte[] data = packet.data;
        currentPlayer.set((EntityPlayer) player);
        //currentPlayer = (EntityPlayer) player; //Core.proxy.getPlayer(network.getNetHandler());;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        DataInput input = new DataInputStream(inputStream);
        if (channel.equals(factorizeTEChannel)) {
            handleTE(input);
        } else if (channel.equals(factorizeMsgChannel)) {
            handleMsg(input);
        } else if (channel.equals(factorizeCmdChannel)) {
            handleCmd(data);
        }

        currentPlayer.set(null);
    }

    void handleTE(DataInput input) {
        try {
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            int messageType = input.readInt();

            if (!getCurrentPlayer().worldObj.blockExists(x, y, z)) {
                // I suppose we can't avoid this.
                // (Unless we can get a proper server-side check)
                return;
            }

            if (messageType == MessageType.FactoryType) {
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
                World world = getCurrentPlayer().worldObj;
                Coord here = new Coord(world, x, y, z);
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

            Coord target = new Coord(getCurrentPlayer().worldObj, x, y, z);
            TileEntityCommon tec = target.getTE(TileEntityCommon.class);
            if (tec == null) {
                handleForeignMessage(getCurrentPlayer().worldObj, x, y, z, tec, messageType, input);
                return;
            }
            boolean handled;
            if (target.w.isRemote) {
                handled = tec.handleMessageFromServer(messageType, input);
            } else {
                handled = tec.handleMessageFromClient(messageType, input);
            }
            if (!handled) {
                handleForeignMessage(getCurrentPlayer().worldObj, x, y, z, tec, messageType, input);
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
                    Core.logWarning("Got unhandled message: " + messageType + " for " + here);
                }
                else {
                    //XXX: Need to figure out how to keep the server from sending these things!
                    Core.logWarning("Got message to unloaded chunk: " + messageType + " for " + here);
                }
                break;
            }
        }

    }

    static public class MessageType {
        //Non TEF messages
        public final static int ShareAll = -1;
        public final static int DemonEnterChest = 10, PlaySound = 11, PistonPush = 12;
        //TEF messages
        public final static int
                DrawActive = 0, FactoryType = 1,
                //
                MakerTarget = 11, MakerFuel = 12,
                //
                RouterSlot = 20, RouterTargetSide = 21, RouterMatch = 22, RouterIsInput = 23,
                RouterLastSeen = 24, RouterMatchToVisit = 25, RouterDowngrade = 26,
                RouterUpgradeState = 27, RouterEjectDirection = 28,
                //
                BarrelDescription = 40, BarrelItem = 41, BarrelCount = 42, BarrelLoss = 43,
                //
                BatteryLevel = 50,
                //
                MirrorTargetRotation = 60, MirrorDescription = 61,
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
                WireFace = 121
                ;
    }

}
