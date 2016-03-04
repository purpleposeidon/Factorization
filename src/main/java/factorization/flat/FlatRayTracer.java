package factorization.flat;

import factorization.coremodhooks.HandleAttackKeyEvent;
import factorization.coremodhooks.HandleUseKeyEvent;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.render.FlatRayTargetRender;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.NORELEASE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

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
        if (mc.theWorld == null || mc.thePlayer == null) {
            target = null;
            return;
        }
        if (event.phase != TickEvent.Phase.START) {
            if (resetAttackKey && mc.leftClickCounter <= 0) {
                resetAttackKey = false;
                mc.gameSettings.keyBindAttack.pressTime = 1;
                mc.leftClickCounter = 10;
            } else {
                if (mc.objectMouseOver.entityHit == target && target != null) {
                    if (mc.gameSettings.keyBindAttack.isKeyDown()) {
                        mc.gameSettings.keyBindAttack.pressTime = 1;
                    }
                }
            }
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
            tracer.bestAt.setAsEntityLocation(target);
            FzUtil.spawn(target);
        }
        tracer.bestAt.setAsEntityLocation(target);
        target.at = tracer.bestAt.copy();
        target.side = tracer.bestSide;
        target.box = tracer.bestBox;
        target.setEntityBoundingBox(target.box);
        //NORELEASE.println(target.at);
    }

    @SubscribeEvent
    public void interceptUse(HandleUseKeyEvent event) {
        interact(true);
    }

    @SubscribeEvent
    public void interceptAttack(HandleAttackKeyEvent event) {
        NORELEASE.println("Stab");
        if (interact(false)) {
            event.setCanceled(true);
            mc.leftClickCounter = 5;
            mc.thePlayer.swingItem();
            resetAttackKey = true;
        }
    }

    boolean interact(boolean useElseHit) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (target == null) return false;
        if (target.at == null) return false;
        if (mc.thePlayer == null) return false;
        if (mop == null) return false;
        if (mop.entityHit != target) return false;
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) return false;
        FlatFace face = Flat.get(target.at, target.side);
        if (face.isNull()) return false;
        if (useElseHit) {
            face.onActivate(target.at, target.side, mc.thePlayer);
        } else {
            face.onHit(target.at, target.side, mc.thePlayer);
        }
        FMLProxyPacket p = FlatNet.playerInteract(mc.thePlayer, target.at, target.side, useElseHit);
        FlatNet.send(mc.thePlayer, p);
        return true;
    }

    boolean resetAttackKey = false;
}
