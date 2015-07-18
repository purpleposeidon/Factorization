package factorization.beauty;

import factorization.api.Coord;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;

public class ItemBlockShaft extends ItemBlock {
    public ItemBlockShaft(Block block, BlockShaft[] shafts) {
        super(block);
        setShafts(shafts);
    }

    public BlockShaft[] shafts = null;

    public void setShafts(BlockShaft[] shafts) {
        this.shafts = shafts;
        if (shafts.length != ForgeDirection.values().length) throw new IllegalArgumentException();
        for (BlockShaft shaft : shafts) {
            shaft.setShafts(shafts);
        }
    }

    boolean set(Coord at, ForgeDirection axis, int md) {
        BlockShaft theBlock = shafts[axis.ordinal()];
        boolean ret = at.setIdMd(theBlock, md, true);
        if (ret) {
            theBlock.invalidate(at);
        }
        return ret;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        ForgeDirection defaultDirection = ForgeDirection.getOrientation(side);

        Coord at = new Coord(w, x, y, z);
        ArrayList<Coord> lockedNeighbors = new ArrayList<Coord>();
        ArrayList<Coord> freeNeighbors = new ArrayList<Coord>();
        ForgeDirection lockedDirection = ForgeDirection.UNKNOWN;
        int free_md = 0, locked_md = 0;
        for (ForgeDirection neighborDirection : ForgeDirection.VALID_DIRECTIONS) {
            Coord neighbor = at.add(neighborDirection);
            Block neighborBlock = neighbor.getBlock();
            if (!(neighborBlock instanceof BlockShaft)) continue;
            if (BlockShaft.meta2speedNumber[at.getMd()] != 0) continue;
            BlockShaft neighborShaft = (BlockShaft) neighborBlock;
            if (neighborShaft.isUnconnected(neighbor)) {
                freeNeighbors.add(neighbor);
                free_md = neighbor.getMd();
            } else if (neighborShaft.axis == neighborDirection || neighborShaft.axis == neighborDirection.getOpposite()) {
                lockedDirection = neighborShaft.axis;
                lockedNeighbors.add(neighbor);
                locked_md = neighbor.getMd();
            }
        }
        int n = lockedNeighbors.size() + freeNeighbors.size();
        if (n != 1 || player.isSneaking()) {
            return set(at, defaultDirection, 0);
        }
        if (!lockedNeighbors.isEmpty()) {
            return set(at, lockedDirection, locked_md);
        }
        for (Coord neighbor : freeNeighbors) {
            ForgeDirection dir = BlockShaft.normalizeDirection(neighbor.difference(at).getDirection());
            if (neighbor.getBlock() == shafts[dir.ordinal()]) {
                return set(at, dir, free_md);
            }
            set(neighbor, dir, 0); // We're turning the neighbor, so speed won't be kept
            return set(at, dir, 0);
        }

        return false;
    }
}
