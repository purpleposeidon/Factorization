package factorization.flat;

import factorization.api.Coord;
import factorization.coremodhooks.HandleAttackKeyEvent;
import factorization.coremodhooks.HandleUseKeyEvent;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IBoxList;
import factorization.flat.api.IFlatVisitor;
import factorization.shared.Core;
import factorization.util.FzUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;

public enum FlatRayTracer {
    INSTANCE;
    {
        Core.loadBus(this);
    }

    void init() {
        // Classload! :o
    }

    final Minecraft mc = Minecraft.getMinecraft();

    FlatRayTarget target = null;

    @SubscribeEvent
    public void updateRayPosition(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (target != null) {
            target.side = EnumFacing.DOWN;
            target.at = null;
            target.box = null;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            target = null;
            return;
        }
        Tracer tracer = new Tracer(mc.thePlayer, 1);
        tracer.run();
        if (tracer.bestAt == null) return;
        if (target == null || target.worldObj != mc.theWorld) {
            target = new FlatRayTarget(mc.theWorld);
            tracer.at.setAsEntityLocation(target);
            FzUtil.spawn(target);
        }
        tracer.at.setAsEntityLocation(target);
        target.at = tracer.bestAt;
        target.side = tracer.bestSide;
        target.box = tracer.bestBox;
        target.setEntityBoundingBox(target.box);
    }

    static class Tracer implements IFlatVisitor, IBoxList {
        final Entity player;
        final Vec3 start, look, end;

        double closestDistSq = Double.POSITIVE_INFINITY;
        double closestDist = closestDistSq;
        Coord bestAt = null;
        EnumFacing bestSide = null;
        AxisAlignedBB bestBox = null;

        Tracer(Entity player, float partial) {
            this.player = player;
            start = player.getPositionEyes(partial);
            look = player.getLook(partial);
            end = start.add(look);
        }

        void run() {
            World w = player.worldObj;
            Flat.iterateRegion(new Coord(w, start).add(-1, -1, -1), new Coord(w, end).add(+1, +1, +1), this);
            if (bestBox != null) {
                closestDist = Math.sqrt(closestDistSq);
            }
        }


        protected Coord at;
        protected EnumFacing side;

        @Override
        public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
            this.at = at;
            this.side = side;
            double origD = closestDistSq;
            face.listSelectionBounds(at, side, player, this);
            if (origD != closestDistSq) {
                bestAt = at;
                bestSide = side;
            }
        }

        @Override
        public void add(AxisAlignedBB box) {
            if (box.isVecInside(start)) {
                // Uh, vanilla has this check. Don't ask me! :p
                closestDistSq = 0;
                bestBox = box;
                return;
            }
            MovingObjectPosition mop = box.calculateIntercept(start, end);
            if (mop == null) return;
            double d = start.distanceTo(mop.hitVec);
            if (d < closestDistSq) {
                closestDistSq = d;
                bestBox = box;
            }
        }
    }

    @SubscribeEvent
    public void interceptUse(HandleUseKeyEvent event) {
        interact(true);
    }

    @SubscribeEvent
    public void interceptAttack(HandleAttackKeyEvent event) {
        interact(false);
    }

    void interact(boolean useElseHit) {
        if (target == null) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.entityHit != target) return;
        FlatNet.playerInteract(mc.thePlayer, target.at, target.side, useElseHit);
    }
}
