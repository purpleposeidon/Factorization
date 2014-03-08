package factorization.crafting;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityCommon;

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
    public IIcon getIcon(ForgeDirection dir) {
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
        b_facing = FzUtil.getOpposite(FzUtil.determineOrientation(player));
    }
    
    @Override
    public void updateEntity() {
        if (progress != 0 && progress++ == 20) {
            if (!worldObj.isRemote && getFacing() != ForgeDirection.UNKNOWN && isCrafterRoot) {
                getStateHelper().craft(false, this);
                isCrafterRoot = false;
                boolean signal = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
                if (!signal) {
                    powered = false;
                }
            } else {
                upperCorner = lowerCorner = null;
            }
            progress = -10;
        }
        
        dumpBuffer();
    }
    
    void dumpBuffer() {
        if (buffer.isEmpty()) return;
        for (FzInv fz : getAdjacentInventories()) {
            if (fz == null) break;
            while (!buffer.isEmpty()) {
                ItemStack is = FzUtil.normalize(buffer.get(0));
                if (is != null) {
                    is = FzUtil.normalize(fz.push(is));
                }
                if (is == null) {
                    buffer.remove(0);
                } else {
                    break;
                }
            }
        }
    }
    
    FzInv[] getAdjacentInventories() {
        FzInv[] ret = new FzInv[6*5];
        int i = 0;
        Coord me = getCoord();
        final ForgeDirection facing = getFacing();
        final ForgeDirection behind = facing.getOpposite();
        for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
            if (fd == facing) {
                continue;
            }
            ForgeDirection back = fd.getOpposite();
            Coord sc = me.add(fd);
            IInventory inv = sc.getTE(IInventory.class);
            if (inv != null) {
                ret[i++] = FzUtil.openInventory(inv, back);
            } else if (fd != behind) {
                TileEntityCompressionCrafter neighbor = sc.getTE(TileEntityCompressionCrafter.class);
                if (neighbor == null) continue;
                if (neighbor.getFacing() != facing) continue;
                //recursiveish search
                for (ForgeDirection nfd : ForgeDirection.VALID_DIRECTIONS) {
                    if (nfd == facing) continue;
                    if (nfd == back) continue;
                    Coord sd = sc.add(nfd);
                    ForgeDirection newBack = nfd.getOpposite();
                    IInventory newInv = sd.getTE(IInventory.class);
                    if (newInv != null) ret[i++] = FzUtil.openInventory(newInv, newBack);
                }
            }
        }
        return ret;
    }
    
    @Override
    public void neighborChanged() {
        //look for a redstone signal
        if (worldObj.isRemote) {
            return;
        }
        boolean signal = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
        if (signal != powered && signal && progress == 0) {
            getStateHelper().craft(true, this);
            isCrafterRoot = true;
        }
        powered = signal;
    }
    
    
    @Override
    public boolean handleMessageFromServer(MessageType messageType, DataInput input) throws IOException {
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
    public FMLProxyPacket getDescriptionPacket() {
        return getDescriptionPacketWith(MessageType.CompressionCrafter, b_facing, progress);
    }
    
    void informClient() {
        broadcastMessage(null, MessageType.CompressionCrafterBeginCrafting);
        progress = 1;
        powered = true;
    }
    
    TileEntityCompressionCrafter look(ForgeDirection d) {
        TileEntity te = worldObj.getTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
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
