package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityCompressionCrafter extends TileEntityCommon {
    static ThreadLocal<CompressionState> states = new ThreadLocal();
    
    CompressionState getStateHelper() {
        CompressionState cs = states.get();
        if (cs == null) {
            states.set(cs = new CompressionState());
        }
        return cs;
    }
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.COMPRESSIONCRAFTER;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }
    
    byte progress = 0;
    byte b_facing = (byte) ForgeDirection.UP.ordinal();
    boolean isCrafterRoot = false;
    boolean powered = false;
    
    public ForgeDirection getFacing() {
        return ForgeDirection.getOrientation(b_facing);
    }
    
    public float getProgressPerc() {
        if (progress == 0) {
            return 0;
        }
        if (progress > 0) {
            return (float) Math.sqrt(progress/20F);
        }
        return Math.min(1, (progress*1F)/-10F);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("prog", progress);
        tag.setByte("dir", b_facing);
        tag.setBoolean("root", isCrafterRoot);
        tag.setBoolean("rs", powered);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        progress = tag.getByte("prog");
        b_facing = tag.getByte("dir");
        isCrafterRoot = tag.getBoolean("root");
        powered = tag.getBoolean("rs");
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        ForgeDirection f = getFacing();
        if (dir == f) {
            return BlockIcons.compactFace;
        } else if (dir == f.getOpposite()) {
            return BlockIcons.compactBack;
        }
        return BlockIcons.compactSide;
    }
    
    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        b_facing = (byte) FactorizationUtil.determineOrientation(player);
        b_facing = (byte) ForgeDirection.getOrientation(b_facing).getOpposite().ordinal();
    }
    
    @Override
    public void updateEntity() {
        if (progress != 0 && progress++ == 20) {
            if (!worldObj.isRemote && getFacing() != ForgeDirection.UNKNOWN && isCrafterRoot) {
                getStateHelper().craft(false, this);
                isCrafterRoot = false;
                boolean signal = worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
                if (!signal) {
                    powered = false;
                }
            }
            progress = -10;
        }
    }
    
    @Override
    public void neighborChanged() {
        //look for a redstone signal
        if (worldObj.isRemote) {
            return;
        }
        if (progress != 0) {
            return;
        }
        boolean signal = worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
        if (signal != powered && signal) {
            getStateHelper().craft(true, this);
            isCrafterRoot = true;
        }
        powered = signal;
    }
    
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.CompressionCrafter) {
            b_facing = input.readByte();
            progress = input.readByte();
            return true;
        }
        if (messageType == MessageType.CompressionCrafterBeginCrafting) {
            progress = 1;
            return true;
        }
        return false;
    }
    
    @Override
    public Packet getDescriptionPacket() {
        return getDescriptionPacketWith(MessageType.CompressionCrafter, b_facing, progress);
    }
    
    void informClient() {
        broadcastMessage(null, MessageType.CompressionCrafterBeginCrafting);
        progress = 1;
        powered = true;
    }
    
    TileEntityCompressionCrafter look(ForgeDirection d) {
        TileEntity te = worldObj.getBlockTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
        if (te instanceof TileEntityCompressionCrafter) {
            return (TileEntityCompressionCrafter) te;
        }
        return null;
    }
    
    @Override
    public boolean rotate(ForgeDirection axis) {
        byte new_b = (byte) axis.ordinal();
        if (new_b == b_facing) {
            return false;
        }
        b_facing = new_b;
        return true;
    }
}
