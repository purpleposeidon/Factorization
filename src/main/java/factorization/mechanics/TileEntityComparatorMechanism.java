package factorization.mechanics;

import factorization.api.Coord;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.IDeltaChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityComparatorMechanism extends TileEntityComparator {
    public static void replace(TileEntityComparator orig) {
        if (orig == null) return;
        if (orig instanceof TileEntityComparatorMechanism) return;
        TileEntityComparatorMechanism rep = new TileEntityComparatorMechanism();
        final Coord at = new Coord(orig);
        at.setAsTileEntityLocation(rep);
        rep.working = true;
        rep.setOutputSignal(orig.getOutputSignal());
        rep.working = false;
        at.setTE(rep);
    }

    ForgeDirection dir() {
        int md = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        return ForgeDirection.getOrientation(BlockDirectional.getDirection(md));
    }

    boolean working = false;

    @Override
    public int getOutputSignal() {
        int power = super.getOutputSignal();
        if (power >= 0xE || working) return power;

        working = true;

        try {
            // Look for real world behind us
            final Coord at = new Coord(this);
            final Coord back = at.add(dir());
            for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(at)) {
                Coord real = idc.shadow2realCoord(back);
                power = Math.max(power, real.getPowerInput());
            }
            return power;
        } finally {
            working = false;
        }
    }


    @Override
    public void setOutputSignal(int power) {
        super.setOutputSignal(power);

        if (working) return;
        working = true;

        try {
            // Export to comparator in real world in front of us
            final Coord at = new Coord(this);
            final Coord front = at.add(dir().getOpposite());
            for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(at)) {
                Coord real = idc.shadow2realCoordPrecise(front);
                Block b = real.getBlock();
                if (!(b == Blocks.powered_comparator || b == Blocks.unpowered_comparator)) {
                    //if (NORELEASE.on) real.setId(Blocks.stone);
                    continue;
                }
                TileEntityComparator comp = real.getTE(TileEntityComparator.class);
                comp.setOutputSignal(power);
                Blocks.powered_comparator.onNeighborBlockChange(real.w, real.x, real.y, real.z, Blocks.powered_comparator);
                real.scheduleUpdate(20);
            }
        } finally {
            working = false;
        }
    }
}
