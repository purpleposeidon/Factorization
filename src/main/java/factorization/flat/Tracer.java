package factorization.flat;

import factorization.api.Coord;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IBoxList;
import factorization.flat.api.IFlatVisitor;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

class Tracer implements IFlatVisitor, IBoxList {
    final EntityPlayer player;
    final Vec3 start, end;

    double bestDistSq = Double.POSITIVE_INFINITY;
    Coord bestAt = null;
    EnumFacing bestSide = null;
    AxisAlignedBB bestBox = null;

    Tracer(EntityPlayer player, float partial) {
        this.player = player;
        start = player.getPositionEyes(partial);
        Vec3 look = player.getLook(partial);
        double reach = 5;
        end = start.add(SpaceUtil.scale(look, reach));
    }

    void run() {
        World w = player.worldObj;
        Coord min = new Coord(w, start);
        Coord max = new Coord(w, end);
        Coord.sort(min, max);
        min.adjust(-1, -1, -1);
        max.adjust(+1, +1, +1);
        Flat.iterateRegion(min, max, this);
    }


    @Override
    public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
        if (!face.canInteract(at, side, player)) return;
        subBestDSq = Double.POSITIVE_INFINITY;
        face.listSelectionBounds(at, side, player, this);
        if (subBestDSq < bestDistSq) {
            bestDistSq = subBestDSq;
            bestAt = at.copy();
            bestSide = side;
            bestBox = subBox;
        }
    }

    private double subBestDSq;
    private AxisAlignedBB subBox;

    @Override
    public void add(AxisAlignedBB box) {
        if (box.isVecInside(start)) {
            // Uh, vanilla has this check. Don't ask me! :p
            subBestDSq = 0;
            subBox = box;
            return;
        }
        MovingObjectPosition mop = box.calculateIntercept(start, end);
        if (mop == null) return;
        double d = start.squareDistanceTo(mop.hitVec);
        if (d < subBestDSq) {
            subBestDSq = d;
            subBox = box;
        }
    }
}
