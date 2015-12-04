package factorization.crafting;

import java.io.IOException;
import java.util.ArrayList;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.notify.Notice;
import factorization.shared.*;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.util.InvUtil.FzInv;
import factorization.shared.NetworkFactorization.MessageType;

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
    byte b_facing = (byte) EnumFacing.UP.ordinal();
    boolean isCrafterRoot = false;
    boolean powered = false;
    public Coord upperCorner, lowerCorner;
    public EnumFacing craftingAxis = EnumFacing.UP;
    
    public EnumFacing getFacing() {
        return SpaceUtil.getOrientation(b_facing);
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
    public void putData(DataHelper data) throws IOException {
        progress = data.as(Share.PRIVATE, "prog").putByte(progress);
        b_facing = data.as(Share.VISIBLE, "dir").putByte(b_facing);
        isCrafterRoot = data.as(Share.VISIBLE, "root").putBoolean(isCrafterRoot);
        powered = data.as(Share.PRIVATE, "rs").putBoolean(powered);
        buffer = data.as(Share.PRIVATE, "buff").putItemList(buffer);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(EnumFacing dir) {
        EnumFacing f = getFacing();
        if (dir == f) {
            return BlockIcons.compactFace;
        } else if (dir == f.getOpposite()) {
            return BlockIcons.compactBack;
        }
        return BlockIcons.compactSide;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        b_facing = SpaceUtil.getOpposite(SpaceUtil.determineOrientation(player));
    }
    
    @Override
    public void updateEntity() {
        if (progress != 0 && progress++ == 20) {
            if (!worldObj.isRemote && getFacing() != null && isCrafterRoot) {
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
                ItemStack is = ItemUtil.normalize(buffer.get(0));
                if (is != null) {
                    is = ItemUtil.normalize(fz.push(is));
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
        final EnumFacing facing = getFacing();
        final EnumFacing behind = facing.getOpposite();
        for (EnumFacing fd : EnumFacing.VALUES) {
            if (fd == facing) {
                continue;
            }
            EnumFacing back = fd.getOpposite();
            Coord sc = me.add(fd);
            IInventory inv = sc.getTE(IInventory.class);
            if (inv != null) {
                ret[i++] = InvUtil.openInventory(inv, back);
            } else if (fd != behind) {
                TileEntityCompressionCrafter neighbor = sc.getTE(TileEntityCompressionCrafter.class);
                if (neighbor == null) continue;
                if (neighbor.getFacing() != facing) continue;
                //recursiveish search
                for (EnumFacing nfd : EnumFacing.VALUES) {
                    if (nfd == facing) continue;
                    if (nfd == back) continue;
                    Coord sd = sc.add(nfd);
                    EnumFacing newBack = nfd.getOpposite();
                    IInventory newInv = sd.getTE(IInventory.class);
                    if (newInv != null) ret[i++] = InvUtil.openInventory(newInv, newBack);
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
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
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
            craftingAxis = SpaceUtil.getOrientation(input.readByte());
            Coord.sort(lowerCorner, upperCorner);
            if (lowerCorner.distanceSq(upperCorner) > 25) {
                Core.logWarning("Server wanted us to render a large area!?");
                lowerCorner = upperCorner = null;
            } else {
                isCrafterRoot = true;
            }
            return true;
        }
        return false;
    }
    
    void informClient() {
        broadcastMessage(null, MessageType.CompressionCrafterBeginCrafting);
        progress = 1;
        powered = true;
    }
    
    TileEntityCompressionCrafter look(EnumFacing d) {
        TileEntity te = worldObj.getTileEntity(xCoord + d.getDirectionVec().getX(), yCoord + d.getDirectionVec().getY(), zCoord + d.getDirectionVec().getZ());
        if (te instanceof TileEntityCompressionCrafter) {
            return (TileEntityCompressionCrafter) te;
        }
        return null;
    }
    
    @Override
    public boolean rotate(EnumFacing axis) {
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
    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
        if (worldObj.isRemote) {
            return false;
        }
        if (entityplayer.isSneaking()) {
            return false;
        }
        if (!buffer.isEmpty()) {
            new Notice(this, "Buffered output").sendTo(entityplayer);
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
    
    @Override
    public void click(EntityPlayer entityplayer) {
        InvUtil.emptyBuffer(entityplayer, buffer, this);
    }
}
