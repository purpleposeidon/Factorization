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
import net.minecraft.src.FactorizationHack;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NetworkManager;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.StringTranslate;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import factorization.api.Coord;

public class NetworkFactorization implements IConnectionHandler, IPacketHandler {
    protected final static String factorizeChannel = "factorize"; //used for tile entities
    protected final static String factorizeMsgChannel = "factorizeMsg"; //used for sending translatable chat messages
    protected final static String factorizeCmdChannel = "factorizeCmd"; //used for player keys

    @Override
    public void onConnect(NetworkManager network) {
    };

    @Override
    public void onDisconnect(NetworkManager network, String message, Object[] args) {
    };

    private void registerChannel(NetworkManager network, String channel) {
        MessageManager.getInstance().registerChannel(network, this, channel);
    }

    @Override
    public void onLogin(NetworkManager network, Packet1Login login) {
        registerChannel(network, factorizeChannel);
        registerChannel(network, factorizeMsgChannel);
        registerChannel(network, factorizeCmdChannel);
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
            packet.channel = factorizeChannel;
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

    static EntityPlayer currentPlayer = null;

    @Override
    public void onPacketData(NetworkManager network, String channel, byte[] data) {
        //wow, long function...
        EntityPlayer player = Core.instance.getPlayer(network.getNetHandler());
        currentPlayer = player;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        DataInput input = new DataInputStream(inputStream);
        if (channel.equals(factorizeMsgChannel)) {
            if (Core.instance.isServer()) {
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
                player.addChatMessage(String.format(main, items.toArray()));
            } catch (IllegalFormatException e) {
                System.out.print("Illegal format: \"" + main + '"');
                for (String i : items) {
                    System.out.print(" \"" + i + "\"");
                }
                System.out.println();
                e.printStackTrace();
            }
            return;
        } else if (channel.equals(factorizeChannel)) {
            try {
                int x = input.readInt();
                int y = input.readInt();
                int z = input.readInt();
                int messageType = input.readInt();

                if (!player.worldObj.blockExists(x, y, z)) {
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
                    TileEntity spawn = ft.makeTileEntity();
                    player.worldObj.setBlockTileEntity(x, y, z, spawn);

                    if (spawn instanceof TileEntityCommon) {
                        ((TileEntityCommon) spawn).useExtraInfo(extraData);
                        ((TileEntityCommon) spawn).useExtraInfo2(extraData2);
                    }
                }

                if (messageType == -1) {
                    return;
                }

                Coord target = new Coord(player.worldObj, x, y, z);
                TileEntityCommon tec = target.getTE(TileEntityCommon.class);
                if (tec == null) {
                    handleForeignMessage(player.worldObj, x, y, z, tec, messageType, input);
                    return;
                }
                boolean handled;
                if (Core.instance.isCannonical(player.worldObj)) {
                    // server. And not SSP 'cause there's no packets
                    handled = tec.handleMessageFromClient(messageType, input);
                } else {
                    // SMP client
                    handled = tec.handleMessageFromServer(messageType, input);
                }
                if (!handled) {
                    handleForeignMessage(player.worldObj, x, y, z, tec, messageType, input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (channel.equals(factorizeCmdChannel)) {
            if (data == null || data.length < 2) {
                return;
            }
            byte s = data[0];
            byte arg = data[1];
            Command.fromNetwork(player, s, arg);
            return;
        }
    }

    //Messages for non-TEFs.
    void handleForeignMessage(World world, int x, int y, int z, TileEntity ent, int messageType,
            DataInput input) throws IOException {
        if (Core.instance.isCannonical(world)) {
            //Nothing for the server to deal with
        } else {
            switch (messageType) {
            case MessageType.DemonEnterChest:
                if (ent instanceof TileEntityChest) {
                    Core.instance.pokeChest((TileEntityChest) ent);
                }
                break;
            case MessageType.PlaySound:
                Sound.receive(input);
                break;
            case MessageType.PistonPush:
                Block.pistonBase.receiveClientEvent(world, x, y, z, 0, input.readInt());
                new Coord(world, x, y, z).setId(0);
                break;
            default:
                if (world.blockExists(x, y, z)) {
                    System.err.println("Got unhandled message: " + messageType + " for " + (x + "," + y + "," + z));
                }
                else {
                    //XXX: Need to figure out how to keep the server from sending these things!
                    System.err.println("Got message to unloaded chunk: " + messageType + " for " + (x + "," + y + "," + z));
                }
                break;
            }
        }

    }

    public void sendCommand(EntityPlayer player, Command cmd, byte arg) {
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = factorizeCmdChannel;
        packet.data = new byte[2];
        packet.data[0] = cmd.id;
        packet.data[1] = arg;
        packet.length = packet.data.length;
        Core.instance.addPacket(player, packet);
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
                if (!Core.instance.playerListensToCoord(player, src)) {
                    continue;
                }
                Core.instance.addPacket(player, toSend);
            }
        }
        else {
            Core.instance.addPacket(who, toSend);
        }
    }

    public void broadcastMessage(EntityPlayer who, Coord src, int messageType, Object... msg) {
        //		// who is ignored
        //		if (!Core.instance.isServer() && who == null) {
        //			return;
        //		}
        Packet toSend = Core.network.messagePacket(src, messageType, msg);
        if (Core.instance.isServer()) {
            broadcastPacket(who, src, toSend);
        }
        else {
            Core.instance.addPacket(who, toSend);
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
                MakerTarget = 101, MakerFuel = 102,
                //
                RouterSlot = 200, RouterTargetSide = 201, RouterMatch = 202, RouterIsInput = 203,
                RouterLastSeen = 204, RouterMatchToVisit = 205, RouterDowngrade = 206,
                RouterUpgradeState = 207, RouterEjectDirection = 208,
                //
                BarrelDescription = 300, BarrelItem = 301, BarrelCount = 302,
                //
                BatteryLevel = 400,
                //
                MirrorTargetRotation = 500, MirrorDescription = 501,
                //
                TurbineWater = 601, TurbineSpeed = 602,
                //
                HeaterHeat = 700
                ;
    }
}
