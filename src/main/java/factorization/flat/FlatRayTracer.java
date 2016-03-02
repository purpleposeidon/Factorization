package factorization.flat;

import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.coremodhooks.HandleAttackKeyEvent;
import factorization.coremodhooks.HandleUseKeyEvent;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IBoxList;
import factorization.flat.api.IFlatVisitor;
import factorization.flat.render.FlatRayTargetRender;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;

public enum FlatRayTracer {
    INSTANCE;

    void init() {
        Core.loadBus(this);
        RenderingRegistry.registerEntityRenderingHandler(FlatRayTarget.class, new IRenderFactory<FlatRayTarget>() {
            @Override
            public Render<? super FlatRayTarget> createRenderFor(RenderManager manager) {
                return new FlatRayTargetRender(manager);
            }
        });
    }

    final Minecraft mc = Minecraft.getMinecraft();

    FlatRayTarget target = null;

    @SubscribeEvent
    public void updateRayPosition(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.theWorld == null || mc.thePlayer == null) {
            target = null;
            return;
        }
        if (target != null) {
            target.side = EnumFacing.DOWN;
            target.at = null;
            target.box = null;
            target.posX = mc.thePlayer.posX;
            target.posY = mc.thePlayer.posY + 200;
            target.posZ = mc.thePlayer.posZ;
        }
        Tracer tracer = new Tracer(mc.thePlayer, 1F);
        tracer.run();
        if (tracer.bestAt == null) return;
        if (target == null || target.worldObj != mc.theWorld || target.isDead) {
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
        final Vec3 start, end;

        double closestDistSq = Double.POSITIVE_INFINITY;
        double closestDist = closestDistSq;
        Coord bestAt = null;
        EnumFacing bestSide = null;
        AxisAlignedBB bestBox = null;

        Tracer(Entity player, float partial) {
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
            AabbDebugger.addBox(box);
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
        MovingObjectPosition mop = mc.objectMouseOver;
        if (target == null) return;
        if (target.at == null) return;
        if (mc.thePlayer == null) return;
        if (mop == null) return;
        if (mop.entityHit != target) return;
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) return;
        FlatNet.playerInteract(mc.thePlayer, target.at, target.side, useElseHit);
    }
}
