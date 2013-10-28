package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityCompressionCrafter extends TileEntityCommon {
    static ThreadLocal<CompressionState> states = new ThreadLocal();
    
    ArrayList<ItemStack> buffer = new ArrayList();
    
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
    public Coord upperCorner, lowerCorner;
    public ForgeDirection craftingAxis = ForgeDirection.UP;
    
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
    
    public boolean isPrimaryCrafter() {
        return isCrafterRoot && progress > 0;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("prog", progress);
        tag.setByte("dir", b_facing);
        tag.setBoolean("root", isCrafterRoot);
        tag.setBoolean("rs", powered);
        writeBuffer("buff", tag, buffer);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        progress = tag.getByte("prog");
        b_facing = tag.getByte("dir");
        isCrafterRoot = tag.getBoolean("root");
        powered = tag.getBoolean("rs");
        readBuffer("buff", tag, buffer);
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
            } else {
                upperCorner = lowerCorner = null;
            }
            progress = -10;
        }
        
        if (!buffer.isEmpty()) {
            Coord sc = getCoord();
            for (int side = 0; side < 6; side++) {
                ForgeDirection fd = ForgeDirection.getOrientation(side);
                if (fd == getFacing()) {
                    continue;
                }
                Coord neighbor = sc.add(fd);
                IInventory inv = neighbor.getTE(IInventory.class);
                if (inv == null) {
                    continue;
                }
                FzInv fz = FactorizationUtil.openInventory(inv, fd.getOpposite());
                while (!buffer.isEmpty()) {
                    ItemStack is = FactorizationUtil.normalize(buffer.get(0));
                    if (is != null) {
                        is = FactorizationUtil.normalize(fz.push(is));
                    }
                    if (is == null) {
                        buffer.remove(0);
                    } else {
                        break;
                    }
                }
            }
        }
    }
    
    @Override
    public void neighborChanged() {
        //look for a redstone signal
        if (worldObj.isRemote) {
            return;
        }
        boolean signal = worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
        if (signal != powered && signal && progress == 0) {
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
        if (messageType == MessageType.CompressionCrafterBounds) {
            upperCorner = new Coord(worldObj, input.readInt(), input.readInt(), input.readInt());
            lowerCorner = new Coord(worldObj, input.readInt(), input.readInt(), input.readInt());
            craftingAxis = ForgeDirection.getOrientation(input.readByte());
            Coord.sort(lowerCorner, upperCorner);
            if (lowerCorner.distanceSq(upperCorner) > 25) {
                Core.logFine("Server wanted us to render a large area!");
                lowerCorner = upperCorner = null;
            } else {
                isCrafterRoot = true;
            }
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
    
    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        while (!buffer.isEmpty()) {
            here.spawnItem(buffer.remove(0));
        }
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return false;
        }
        if (entityplayer.isSneaking()) {
            return false;
        }
        Coord c = getCoord();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 3) {
                        continue;
                    }
                    c.set(worldObj, xCoord + dx, yCoord + dy, zCoord + dz);
                    TileEntity te = c.getTE(TileEntityCompressionCrafter.class);
                    if (te == this) {
                        continue;
                    }
                    if (te != null) {
                        return false;
                    }
                }
            }
        }
        getStateHelper().showTutorial(entityplayer, this);
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        if (isPrimaryCrafter()) {
            AxisAlignedBB ab = super.getRenderBoundingBox();
            //This could be more precise.
            int d = 7;
            ab.maxX += d;
            ab.maxY += d;
            ab.maxZ += d;
            ab.minX -= d;
            ab.minY -= d;
            ab.minZ -= d;
            return ab;
        }
        return super.getRenderBoundingBox();
    }
}
