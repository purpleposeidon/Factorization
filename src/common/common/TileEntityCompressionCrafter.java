package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityCompressionCrafter extends TileEntityCommon {

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
    final ArrayList<ItemStack> outputBuffer = new ArrayList(4);
    
    ForgeDirection getFacing() {
        return ForgeDirection.getOrientation(b_facing);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("prog", progress);
        tag.setByte("dir", b_facing);
        writeBuffer("buffer", tag, outputBuffer);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        progress = tag.getByte("prog");
        b_facing = tag.getByte("dir");
        readBuffer("buffer", tag, outputBuffer);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        ForgeDirection f = getFacing();
        if (dir == f) {
            return BlockIcons.compactFace;
        }
        return BlockIcons.dark_iron_block;
        /*
        if (dir == f) {
            return BlockIcons.compactFace;
        } else if (dir == f.getOpposite()) {
            return BlockIcons.compactBack;
        }
        return BlockIcons.compactSide;*/
    }
    
    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        b_facing = (byte) FactorizationUtil.determineOrientation(player);
        b_facing = (byte) ForgeDirection.getOrientation(b_facing).getOpposite().ordinal();
    }
    
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return; //NORELEASE: Can take this out client-side?w
        }
        if (progress != 0 && progress++ == 20) {
            if (getFacing() != ForgeDirection.UNKNOWN) {
                craft(false);
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
        boolean ready = progress == 0 && worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0;
        if (!ready) {
            return;
        }
        if (craft(true)) {
            progress = 1;
        }
    }
    
    /** return true if something is/canbe crafted */
    boolean craft(boolean fake) {
        CellInfo[] cells = getCells();
        if (cells == null) {
            return false;
        }
        ItemStack[] craftingGrid = new ItemStack[9];
        for (int mode = 0; mode < CellInfo.length; mode++) {
            boolean any = false;
            for (int i = 0; i < 9; i++) {
                CellInfo ci = cells[i];
                if (ci == null) {
                    craftingGrid[i] = null;
                    continue;
                }
                ItemStack is = ci.items[ci.getBestMode(mode)];
                craftingGrid[i] = is;
                any |= is != null;
            }
            if (!any) {
                continue;
            }
            List<ItemStack> result = FactorizationUtil.craft3x3(this, fake, craftingGrid);
            if (FactorizationUtil.craft_succeeded) {
                if (fake) {
                    return true;
                }
                double mx = 0, my = 0, mz = 0;
                int count = 0;
                for (int i = 0; i < 9; i++) {
                    CellInfo ci = cells[i];
                    if (ci != null) {
                        ci.consume(ci.getBestMode(mode));
                        mx += ci.cell.x;
                        my += ci.cell.y;
                        mz += ci.cell.z;
                        count++;
                    }
                }
                if (count == 0) {
                    Coord c = getCoord();
                    mx = c.x;
                    my = c.y;
                    mz = c.z;
                } else {
                    mx /= count;
                    my /= count;
                    mz /= count;
                }
                for (ItemStack is : result) {
                    EntityItem ei = new EntityItem(worldObj, mx + 0.5, my + 0.5, mz + 0.5, is);
                    worldObj.spawnEntityInWorld(ei);
                }
                return true;
            }
        }
        return false;
    }
    
    static class CellInfo {
        static final int BARREL = 0, BREAK = 1, SMACKED = 2, PICKED = 3, length = 4;
        
        final Coord cell;
        ItemStack[] items = new ItemStack[length];
        
        public CellInfo(Coord cell, ForgeDirection top) {
            this.cell = cell;
            TileEntityBarrel barrel = cell.getTE(TileEntityBarrel.class);
            if (barrel != null) {
                ItemStack b = barrel.item.copy();
                b.stackSize = 1;
                items[BARREL] = b;
                return;
            }
            items[PICKED] = cell.getPickBlock(top);
            items[BREAK] = cell.getBrokenBlock();
            if (items[BREAK] != null) {
                ItemStack b = items[BREAK];
                List<ItemStack> craftRes = FactorizationUtil.craft1x1(null, true, b);
                if (craftRes != null && craftRes.size() == 1) {
                    items[SMACKED] = craftRes.get(0);
                }
            }
        }
        
        private static final ArrayList<ItemStack> empty = new ArrayList<ItemStack>();
        List<ItemStack> consume(int mode) {
            ItemStack leftOvers = items[mode];
            if (leftOvers == null) {
                return empty;
            }
            switch (mode) {
            case PICKED:
                cell.setId(0);
                break;
            case BREAK:
                leftOvers.stackSize--;
                cell.setId(0);
                break;
            case BARREL:
                leftOvers = null;
                cell.getTE(TileEntityBarrel.class).changeItemCount(-1);
                break;
            case SMACKED:
                List<ItemStack> craftRes = FactorizationUtil.craft1x1(null, true, items[BREAK]);
                for (Iterator<ItemStack> it = craftRes.iterator(); it.hasNext();) {
                    ItemStack is = it.next();
                    if (FactorizationUtil.couldMerge(is, leftOvers)) {
                        is.stackSize--;
                        if (is.stackSize <= 0) {
                            it.remove();
                        }
                        break;
                    }
                }
                cell.setId(0);
                return craftRes;
            }
            List<ItemStack> ret = new ArrayList(1);
            ret.add(FactorizationUtil.normalize(leftOvers));
            return ret;
        }
        
        int getBestMode(int lastModeAllowed) {
            int last_valid = lastModeAllowed;
            for (int i = 0; i <= lastModeAllowed; i++) {
                if (items[i] != null) {
                    last_valid = i;
                }
            }
            return last_valid;
        }
    }
    
    /** @return array of 9 cells, some of which may be null. Or it may return null if the frame is invalid. */
    public CellInfo[] getCells() {
        return findEdgeRoot().getRootsCells();
    }
    
    // ***** Here starts all the code for getCells *****
    private CellInfo[] getRootsCells() {
        if (!checkFrame()) {
            return null;
        }
        final ForgeDirection up = getFacing();
        final ForgeDirection right = getRightDirection();
        final int height = getPairDistance() - 1;
        if (height < 0) {
            return null;
        }
        TileEntityCompressionCrafter otherEdge = getOtherSideRoot(right);
        if (otherEdge == null) {
            return null;
        }
        final int width = otherEdge.getPairDistance() - 1;
        if (width < 0) {
            return null;
        }
        CellInfo[] ret = new CellInfo[9];
        Coord corner = getCoord();
        corner.adjust(up);
        ForgeDirection out = right.getRotation(up);
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                Coord c = corner.add(dx*right.offsetX + dy*up.offsetX, dx*right.offsetY + dy*up.offsetY, dx*right.offsetZ + dy*up.offsetZ);
                ret[cellIndex(dx, dy)] = new CellInfo(c, out);
            }
        }
        return ret;
    }
    
    private static final int[][] cellIndices = new int[][] {
        {0, 1, 2},
        {3, 4, 5},
        {6, 7, 8}
    };
    
    private static int cellIndex(int x, int y) {
        return cellIndices[y][2 - x];
    }
    
    private TileEntityCompressionCrafter look(ForgeDirection d) {
        TileEntity te = worldObj.getBlockTileEntity(xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ);
        if (te instanceof TileEntityCompressionCrafter) {
            return (TileEntityCompressionCrafter) te;
        }
        return null;
    }
    
    private TileEntityCompressionCrafter findEdgeRoot() {
        final ForgeDirection cd = getFacing();
        TileEntityCompressionCrafter at = this;
        for (ForgeDirection d : new ForgeDirection[] { ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.WEST }) {
            if (d == cd || d.getOpposite() == cd) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < 2; i++) {
                TileEntityCompressionCrafter l = at.look(d);
                if (l == null) {
                    break;
                }
                at = l;
                found = true;
            }
            if (found) {
                return at;
            }
        }
        return this;
    }
    
    /** This assumes that this is a root! */
    private boolean checkFrame() {
        if (!checkPairs()) {
            return false;
        }
        TileEntityCompressionCrafter cc = getOtherSideRoot(getRightDirection());
        if (cc == null) {
            return false;
        }
        return cc.checkPairs();
    }
    
    private TileEntityCompressionCrafter getOtherSideRoot(final ForgeDirection right) {
        Coord c = getCoord();
        c.adjust(right.getOpposite());
        c.adjust(getFacing());
        TileEntityCompressionCrafter cc = c.getTE(TileEntityCompressionCrafter.class);
        if (cc == null) {
            return null;
        }
        return cc.findEdgeRoot();
    }
    
    private ForgeDirection getRightDirection() {
        //"The right chest is obviously the right one" -- etho, badly quoted.
        Coord here = getCoord();
        ForgeDirection[] validDirections = ForgeDirection.VALID_DIRECTIONS;
        for (int i = 0; i < validDirections.length; i++) {
            ForgeDirection dir = validDirections[i];
            if (isFrame(here.add(dir))) {
                return dir;
            }
        }
        return ForgeDirection.UNKNOWN;
    }
    
    private boolean checkPairs() {
        int height = getPairDistance();
        if (height <= 0) {
            return false;
        }
        ForgeDirection right = getRightDirection();
        if (right == ForgeDirection.UNKNOWN) {
            return false;
        }
        Coord here = getCoord();
        ForgeDirection my_cd = getFacing();
        for (int i = 0; i < 3; i++) {
            here.adjust(right);
            TileEntityCompressionCrafter other = here.getTE(TileEntityCompressionCrafter.class);
            if (other == null) {
                break; //short frame
            }
            if (other.getFacing() != my_cd) {
                return false;
            }
            if (other.getPairDistance() != height) {
                return false;
            }
        }
        return true;
    }
    
    static boolean isFrame(Coord c) {
        return c.getTE(TileEntityCompressionCrafter.class) != null;
    }
    

    int getPairDistance() {
        final ForgeDirection cd = getFacing();
        Coord here = getCoord();
        for (int i = 1; i <= 4; i++) {
            here.adjust(cd);
            TileEntityCompressionCrafter cc = here.getTE(TileEntityCompressionCrafter.class);
            if (cc != null) {
                if (cc.getFacing().getOpposite() != cd) {
                    return -1;
                }
                return i;
            }
        }
        return -1;
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
        return false;
    }
    
    @Override
    public Packet getDescriptionPacket() {
        return getDescriptionPacketWith(MessageType.CompressionCrafter, b_facing, progress);
    }
    
}
