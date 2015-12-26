package factorization.net;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IEntityMessage;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.artifact.ContainerForge;
import factorization.common.Command;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.shared.Core;
import factorization.shared.Sound;
import factorization.shared.TileEntityCommon;
import factorization.util.DataUtil;
import factorization.utiligoo.ItemGoo;
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

public class NetworkFactorization<TE extends TileEntity & INet> {
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
            } else if (item instanceof StandardMessageType) {
                StandardMessageType mt = (StandardMessageType) item;
                mt.write(output);
            } else if (item instanceof NBTTagCompound) {
                NBTTagCompound tag = (NBTTagCompound) item;
                ByteBufUtils.writeTag(output, tag);
            } else {
                throw new RuntimeException("Don't know how to serialize " + item.getClass() + " (" + item + ")");
            }
        }
    }

    public static byte getMessageIndex(Enum msg) {
        if (msg instanceof StandardMessageType) {
            return (byte) -msg.ordinal();
        }
        return (byte) msg.ordinal();
    }

    public static Enum getMessage(byte index, Enum[] custom) throws IOException {
        if (index < 0) {
            int i = -index;
            if (i >= StandardMessageType.VALUES.length) throw new IOException("Invalid standard message index " + i);
            return StandardMessageType.VALUES[i];
        }
        if (index >= custom.length) throw new IOException("Invalid custom message index " + index + " for " + custom);
        return custom[index];
    }
    
    public void prefixTePacket(ByteBuf output, TE src, StandardMessageType messageType) throws IOException {
        messageType.write(output);
        BlockPos at = src.getPos();
        output.writeInt(at.getX());
        output.writeInt(at.getY());
        output.writeInt(at.getZ());
    }
    
    public FMLProxyPacket TEmessagePacket(TE src, StandardMessageType messageType, Object... items) {
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
            messageType.write(output);
            writeObjects(output, items);
            return FzNetDispatch.generate(output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void prefixEntityPacket(ByteBuf output, Entity to, StandardMessageType messageType) throws IOException {
        messageType.write(output);
        output.writeInt(to.getEntityId());
    }
    
    public FMLProxyPacket entityPacket(ByteBuf output) throws IOException {
        return FzNetDispatch.generate(output);
    }
    
    public FMLProxyPacket entityPacket(Entity to, StandardMessageType messageType, Object ...items) {
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
        StandardMessageType.factorizeCmdChannel.write(out);
        out.writeByte(cmd.id);
        out.writeInt(arg);
        FzNetDispatch.addPacket(FzNetDispatch.generate(out), player);
    }

    public void sendPlayerMessage(EntityPlayer player, StandardMessageType messageType, Object... msg) {
        FzNetDispatch.addPacket(playerMessagePacket(messageType, msg), player);
    }

    public void broadcastMessage(EntityPlayer who, TE src, StandardMessageType messageType, Object... msg) {
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

    void handleTE(ByteBuf input, StandardMessageType messageType, EntityPlayer player) {
        try {
            World world = player.worldObj;
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            Coord here = new Coord(world, x, y, z);
            BlockPos pos = here.toBlockPos();
            
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
                return;
            }
            
            if (messageType == StandardMessageType.DescriptionRequest && !world.isRemote) {
                TileEntityCommon tec = here.getTE(TileEntityCommon.class);
                if (tec != null) {
                    FzNetDispatch.addPacket(tec.getDescriptionPacket(), player);
                }
                return;
            }
            
            if (messageType == StandardMessageType.RedrawOnClient && world.isRemote) {
                world.markBlockForUpdate(pos);
                return;
            }

            if (messageType == StandardMessageType.FactoryType && world.isRemote) {
                //create a Tile Entity of that type there.

                byte ftId = input.readByte();
                FactoryType ft = FactoryType.fromMd(ftId);
                if (ft == null) {
                    Core.logSevere("Got invalid FactoryType ID %s", ftId);
                    return;
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
                        return;
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
                return;
            }

            if (messageType == null) {
                return;
            }

            TileEntityCommon tec = here.getTE(TileEntityCommon.class);
            if (tec == null) {
                handleForeignMessage(world, pos, tec, messageType, input);
                return;
            }
            boolean handled;
            if (here.w.isRemote) {
                handled = tec.handleMessageFromServer(messageType, input);
            } else {
                handled = tec.handleMessageFromClient(messageType, input);
            }
            if (!handled) {
                handleForeignMessage(world, pos, tec, messageType, input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handleForeignMessage(World world, BlockPos pos, TileEntity ent, StandardMessageType messageType, ByteBuf input) throws IOException {
        if (!world.isRemote) {
            //Nothing for the server to deal with
        } else {
            Coord here = new Coord(world, pos);
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
    
    boolean handleForeignEntityMessage(Entity ent, StandardMessageType messageType, ByteBuf input) throws IOException {
        if (messageType == StandardMessageType.UtilityGooState) {
            ItemGoo.handlePacket(input);
            return true;
        }
        return false;
    }
    
    void handleCmd(ByteBuf data, EntityPlayer player) {
        byte s = data.readByte();
        int arg = data.readInt();
        Command.fromNetwork(player, s, arg);
    }
    
    void handleEntity(StandardMessageType messageType, ByteBuf input, EntityPlayer player) {
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
                if (!player.worldObj.isRemote) {
                    Core.logSevere("Sending the server messages to non-IEntityMessages is not allowed, %s!", player);
                    return;
                }
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
    
    
    public void handlePlayer(StandardMessageType mt, ByteBuf input, EntityPlayer player) {
        if (mt == StandardMessageType.ArtifactForgeName) {
            String name = ByteBufUtils.readUTF8String(input);
            String lore = ByteBufUtils.readUTF8String(input);
            if (player.openContainer instanceof ContainerForge) {
                ContainerForge forge = (ContainerForge) player.openContainer;
                forge.forge.name = name;
                forge.forge.lore = lore;
                forge.forge.markDirty();
                forge.detectAndSendChanges();
            }
        } else if (mt == StandardMessageType.ArtifactForgeError) {
            String err = ByteBufUtils.readUTF8String(input);
            if (player.openContainer instanceof ContainerForge) {
                ContainerForge forge = (ContainerForge) player.openContainer;
                forge.forge.error_message = err;
                input.readBytes(forge.forge.warnings);
            }
        }
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
