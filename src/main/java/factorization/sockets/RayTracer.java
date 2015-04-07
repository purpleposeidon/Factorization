package factorization.sockets;

import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class RayTracer {
    final TileEntitySocketBase base;
    final ISocketHolder socket;
    final Coord trueCoord;
    final FzOrientation trueOrientation;
    final boolean powered;

    boolean lookAround = false;
    boolean onlyFirstBlock = false;
    boolean checkEnts = false;
    boolean checkFzds = true;
    boolean checkFzdsFirst = false;
    boolean fzdsMovesTe = true;

    AxisAlignedBB entBox = null;

    public RayTracer(TileEntitySocketBase base, ISocketHolder socket, Coord at, FzOrientation orientation, boolean powered) {
        this.base = base;
        this.socket = socket;
        this.trueCoord = at;
        this.trueOrientation = orientation;
        this.powered = powered;
    }

    public RayTracer onlyFrontBlock() {
        onlyFirstBlock = true;
        return this;
    }

    public RayTracer lookAround() {
        lookAround = true;
        return this;
    }

    public RayTracer checkEnts() {
        checkEnts = true;
        return this;
    }

    public RayTracer checkFzdsFirst() {
        checkFzdsFirst = true;
        checkFzds = false;
        return this;
    }

    boolean fzdsPass = false;

    boolean checkReal() {
        fzdsPass = false;
        return runPass(trueOrientation, trueCoord, null);
    }

    boolean checkFzds() {
        fzdsPass = true;
        if (trueCoord.w != DeltaChunk.getServerShadowWorld()) return false;

        Coord shadowBaseLocation = new Coord(base);

        for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(trueCoord)) {
            FzOrientation orientation = shadowOrientation(idc);
            if (orientation == FzOrientation.UNKNOWN) continue;
            Coord at = new Coord(base);
            Vec3 v = SpaceUtil.newVec();
            at.setAsVector(v);
            v.xCoord += 0.5 + trueOrientation.top.offsetX;
            v.yCoord += 0.5 + trueOrientation.top.offsetY;
            v.zCoord += 0.5 + trueOrientation.top.offsetZ;
            v = idc.shadow2real(v);
            idc.shadow2real(at);
            AxisAlignedBB box = SpaceUtil.createAABB(v, v);
            box.minY += 2;
            AabbDebugger.addBox(box);
            double x = v.xCoord;
            double y = v.yCoord;
            double z = v.zCoord;
            Coord target = new Coord(at.w, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            target.adjust(orientation.facing.getOpposite());
            orientation = orientation.getSwapped();

            try {
                if (fzdsMovesTe) {
                    target.setAsTileEntityLocation(base);
                }
                if (runPass(orientation, target, idc)) return true;
            } finally {
                if (fzdsMovesTe) {
                    shadowBaseLocation.setAsTileEntityLocation(base);
                }
            }

            /*if (mopBlock(target, orientation.facing)) {
                return true;
            }*/

            /*idc.shadow2real(at);
            if (runPass(orientation, at, idc)) return true;*/
        }
        return false;
    }

    public boolean trace() {
        if (checkFzdsFirst && checkFzds()) return true;
        entBox = null;
        if (checkReal()) return true;
        entBox = null;
        return checkFzds && checkFzds();
    }

    FzOrientation shadowOrientation(IDeltaChunk idc) {
        Quaternion rot = idc.getRotation();
        Vec3 topVec = SpaceUtil.fromDirection(trueOrientation.top);
        Vec3 faceVec = SpaceUtil.fromDirection(trueOrientation.facing);
        rot.applyRotation(topVec);
        rot.applyRotation(faceVec);
        ForgeDirection top = SpaceUtil.round(topVec, ForgeDirection.UNKNOWN);
        ForgeDirection facing = SpaceUtil.round(faceVec, top);
        FzOrientation to = FzOrientation.fromDirection(top);
        if (to == FzOrientation.UNKNOWN) {
            return FzOrientation.fromDirection(facing);
        }
        FzOrientation tofo = to.pointTopTo(facing);
        if (tofo == FzOrientation.UNKNOWN) {
            return to;
        }
        return tofo;
    }


    boolean runPass(FzOrientation orientation, Coord coord, IDeltaChunk idc) {
        final ForgeDirection top = orientation.top;
        final ForgeDirection face = orientation.facing;
        final ForgeDirection right = face.getRotation(top);

        if (checkEnts) {
            if (entBox == null) {
                entBox = base.getEntityBox(socket, coord, top, 0);
                if (idc != null) {
                    entBox = idc.shadow2real(entBox);
                    AabbDebugger.addBox(entBox);
                }
            }
            for (Entity entity : getEntities(coord, top, idc)) {
                if (entity == socket) {
                    continue;
                }
                if (base.handleRay(socket, new MovingObjectPosition(entity), coord.w, false, powered)) {
                    return true;
                }
            }
        }

        Coord targetBlock = coord.add(top);
        if (mopBlock(targetBlock, top.getOpposite())) return true; //nose-to-nose with the servo
        if (onlyFirstBlock) return false;
        if (mopBlock(targetBlock.add(top), top.getOpposite())) return true; //a block away
        if (mopBlock(coord, top)) return true;
        if (!lookAround) return false;
        if (mopBlock(targetBlock.add(face), face.getOpposite())) return true; //running forward
        if (mopBlock(targetBlock.add(face.getOpposite()), face)) return true; //running backward
        if (mopBlock(targetBlock.add(right), right.getOpposite())) return true; //to the servo's right
        if (mopBlock(targetBlock.add(right.getOpposite()), right)) return true; //to the servo's left

        return false;
    }

    boolean mopBlock(Coord target, ForgeDirection side) {
        if (target.w != DeltaChunk.getServerShadowWorld()) {
            AxisAlignedBB debug = Coord.aabbFromRange(target, target.add(1, 1, 1));
            AabbDebugger.addBox(debug);
        }
        boolean isThis = base == socket && target.isAt(base);
        Vec3 hitVec = Vec3.createVectorHelper(base.xCoord + side.offsetX, base.yCoord + side.offsetY, base.zCoord + side.offsetZ);
        return base.handleRay(socket, target.createMop(side, hitVec), target.w, isThis, powered);
    }

    Iterable<Entity> getEntities(Coord coord, ForgeDirection top, IDeltaChunk idc) {
        if (idc == null) {
            Entity ent = null;
            if (socket instanceof Entity) {
                ent = (Entity) socket;
            }
            return (Iterable<Entity>) coord.w.getEntitiesWithinAABBExcludingEntity(ent, entBox);
        }
        // Sorta like MetaBox.convertShadowBoxToRealBox ... but lazier.
        Vec3 min = SpaceUtil.newVec();
        Vec3 max = SpaceUtil.newVec();
        SpaceUtil.setMin(entBox, min);
        SpaceUtil.setMax(entBox, max);
        AxisAlignedBB realBox = SpaceUtil.newBox();
        SpaceUtil.setMin(entBox, idc.shadow2real(min));
        SpaceUtil.setMax(entBox, idc.shadow2real(max)); // IDC re-uses the same copy of this vector, hence these contortions.
        return (Iterable<Entity>) coord.w.getEntitiesWithinAABBExcludingEntity(null, realBox);
    }
}
