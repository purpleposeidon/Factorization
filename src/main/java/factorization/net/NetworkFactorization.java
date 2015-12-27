package factorization.net;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.common.Command;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.DataUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

public class NetworkFactorization {
    public static final ItemStack EMPTY_ITEMSTACK = new ItemStack(Blocks.air);
    
    private void writeObjects(ByteBuf output, Object... items) throws IOException {
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
                ByteBufUtils.writeUTF8String(output, (String) item);
            } else if (item instanceof Boolean) {
                output.writeBoolean((Boolean) item);
            } else if (item instanceof Float) {
                output.writeFloat((Float) item);
            } else if (item instanceof Double) {
                output.writeDouble((Double) item);
            } else if (item instanceof ItemStack) {
                ItemStack is = (ItemStack) item;
                if (is == EMPTY_ITEMSTACK) is = null;
                ByteBufUtils.writeItemStack(output, is);
            } else if (item instanceof DeltaCoord) {
                DeltaCoord dc = (DeltaCoord) item;
                dc.write(output);
            } else if (item instanceof Quaternion) {
                Quaternion q = (Quaternion) item;
                q.write(output);
            } else if (item instanceof byte[]) {
                byte[] b = (byte[]) item;
                output.writeBytes(b);
            } else if (item instanceof NBTTagCompound) {
                NBTTagCompound tag = (NBTTagCompound) item;
                ByteBufUtils.writeTag(output, tag);
            } else {
                throw new RuntimeException("Don't know how to serialize " + item.getClass() + " (" + item + ")");
            }
        }
    }

    public static void writeMessage(ByteBuf output, byte targetClass, Enum msg) {
        output.writeByte(targetClass);
        output.writeByte(getMessageIndex(msg));
    }

    public static byte getMessageIndex(Enum msg) {
        if (msg instanceof StandardMessageType) {
            return (byte) -msg.ordinal();
        }
        return (byte) msg.ordinal();
    }

    public static Enum readMessage(ByteBuf input, INet holder) throws IOException {
        return getMessage(input.readByte(), holder);
    }

    public static Enum getMessage(byte index, INet holder) throws IOException {
        if (index < 0) {
            int i = -index;
            if (i >= StandardMessageType.VALUES.length) throw new IOException("Invalid standard message index " + i);
            return StandardMessageType.VALUES[i];
        }
        Enum[] custom = holder == null ? null : holder.getMessages();
        if (custom == null || index >= custom.length) throw new IOException("Invalid custom message index " + index + " for " + custom);
        return custom[index];
    }
    
    public void prefixTePacket(ByteBuf output, TileEntity src, Enum messageType) throws IOException {
        output.writeByte(getMessageIndex(messageType));
        BlockPos at = src.getPos();
        output.writeInt(at.getX());
        output.writeInt(at.getY());
        output.writeInt(at.getZ());
    }
    
    public FMLProxyPacket TEmessagePacket(TileEntity src, Enum messageType, Object... items) {
        try {
            ByteBuf output = Unpooled.buffer();
            prefixTePacket(output, src, messageType);
            writeObjects(output, items);
            return FzNetDispatch.generate(output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public FMLProxyPacket playerMessagePacket(StandardMessageType messageType, Object... items) {
        try {
            ByteBuf output = Unpooled.buffer();
            writeMessage(output, FzNetEventHandler.TO_PLAYER, messageType);
            writeObjects(output, items);
            return FzNetDispatch.generate(output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public FMLProxyPacket blockMessagePacket(Coord src, byte targetClass, StandardMessageType messageType, Object... items) {
        try {
            ByteBuf output = Unpooled.buffer();
            output.writeByte(targetClass);
            output.writeByte(getMessageIndex(messageType));
            output.writeInt(src.x);
            output.writeInt(src.y);
            output.writeInt(src.z);
            writeObjects(output, items);
            return FzNetDispatch.generate(output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void prefixEntityPacket(ByteBuf output, Entity to, Enum messageType) throws IOException {
        output.writeByte(getMessageIndex(messageType));
        output.writeInt(to.getEntityId());
    }
    
    public FMLProxyPacket entityPacket(ByteBuf output) throws IOException {
        return FzNetDispatch.generate(output);
    }
    
    public FMLProxyPacket entityPacket(Entity to, Enum messageType, Object ...items) {
        try {
            ByteBuf output = Unpooled.buffer();
            prefixEntityPacket(output, to, messageType);
            writeObjects(output, items);
            return entityPacket(output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void sendCommand(EntityPlayer player, Command cmd, int arg) {
        ByteBuf out = Unpooled.buffer();
        writeMessage(out, FzNetEventHandler.TO_PLAYER, StandardMessageType.factorizeCmdChannel);
        out.writeByte(cmd.id);
        out.writeInt(arg);
        FzNetDispatch.addPacket(FzNetDispatch.generate(out), player);
    }

    public void sendPlayerMessage(EntityPlayer player, StandardMessageType messageType, Object... msg) {
        FzNetDispatch.addPacket(playerMessagePacket(messageType, msg), player);
    }

    public void broadcastMessage(EntityPlayer who, TileEntity src, Enum messageType, Object... msg) {
        FMLProxyPacket toSend = TEmessagePacket(src, messageType, msg);
        if (who != null) {
            FzNetDispatch.addPacket(toSend, who);
        } else {
            FzNetDispatch.addPacketFrom(toSend, src);
        }
    }

    public void broadcastMessageToBlock(EntityPlayer who, Coord src, StandardMessageType messageType, Object... msg) {
        FMLProxyPacket toSend = blockMessagePacket(src, FzNetEventHandler.TO_BLOCK, messageType, msg);
        broadcastPacket(who, src, toSend);
    }

    public void broadcastPacket(EntityPlayer who, Coord src, FMLProxyPacket toSend) {
        if (who != null) {
            FzNetDispatch.addPacket(toSend, who);
        } else {
            FzNetDispatch.addPacketFrom(toSend, src);
        }
    }

    boolean checkTileEntity(ByteBuf input, StandardMessageType messageType, EntityPlayer player, BlockPos pos) throws IOException {
        Coord here = new Coord(player.worldObj, pos);
        World world = player.worldObj;

        if (Core.debug_network) {
            if (world.isRemote) {
                new Notice(here, messageType.name()).sendTo(player);
            } else {
                Core.logFine("FactorNet: " + messageType + "      " + here);
            }
        }

        if (!here.blockExists() && world.isRemote) {
            // I suppose we can't avoid this.
            // (Unless we can get a proper server-side check)
            return true;
        }

        if (messageType == StandardMessageType.DescriptionRequest) {
            if (world.isRemote) return true; // Foolishness
            TileEntityCommon tec = here.getTE(TileEntityCommon.class);
            if (tec != null) {
                FzNetDispatch.addPacket(tec.getDescriptionPacket(), player);
            }
            return true;
        }

        if (messageType == StandardMessageType.RedrawOnClient) {
            if (!world.isRemote) return true; // Foolishness
            world.markBlockForUpdate(pos);
            return true;
        }

        if (messageType == StandardMessageType.TileFzType) {
            if (!world.isRemote) {
                // Extremely dangerous foolishness
                Core.logSevere("Player tried to send us a TileEntity!?? " + player + " to " + here);
                return true;
            }
            //create a Tile Entity of that type there.

            byte ftId = input.readByte();
            FactoryType ft = FactoryType.fromMd(ftId);
            if (ft == null) {
                Core.logSevere("Got invalid FactoryType ID %s", ftId);
                return true;
            }
            TileEntityCommon spawn = here.getTE(TileEntityCommon.class);
            if (spawn != null && spawn.getFactoryType() != ft) {
                world.removeTileEntity(pos);
                spawn = null;
            }
            if (spawn == null) {
                spawn = ft.makeTileEntity();
                if (spawn == null) {
                    Core.logSevere("Tried to spawn FactoryType with no associated TE %s", ft);
                    return true;
                }
                spawn.setWorldObj(world);
                world.setTileEntity(pos, spawn);
            }

            DataInByteBuf data = new DataInByteBuf(input, Side.CLIENT);
            spawn.putData(data);
            spawn.spawnPacketReceived();
            if (spawn.redrawOnSync()) {
                here.redraw();
            }
            return true;
        }

        if (messageType == null) {
            return true;
        }

        TileEntityCommon tec = here.getTE(TileEntityCommon.class);
        boolean handled;
        if (here.w.isRemote) {
            handled = tec.handleMessageFromServer(messageType, input);
        } else {
            handled = tec.handleMessageFromClient(messageType, input);
        }
        return handled;
    }

    public static ItemStack nullItem(ItemStack is) {
        return is == null ? EMPTY_ITEMSTACK : is;
    }
    
    public static ItemStack denullItem(ItemStack is) {
        if (is == null) return null;
        if (DataUtil.getId(is) == DataUtil.getId(Blocks.air)) return null;
        return is;
    }
}
