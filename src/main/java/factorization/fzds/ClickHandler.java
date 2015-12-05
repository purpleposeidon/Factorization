package factorization.fzds;

import factorization.api.Coord;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.network.HammerNet;
import factorization.fzds.network.HammerNet.HammerNetType;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class ClickHandler {
    //Note that these events will be triggered client-side only, as this entity is only used client-side.
    //(And this object will not be registered server-side)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void leftClick(AttackEntityEvent event) {
        handle(event, event.target, false);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void rightClick(EntityInteractEvent event) {
        handle(event, event.target, true);
    }
    
    MovingObjectPosition current_attacking_target = null;
    
    void handle(PlayerEvent event, Entity target, boolean rightClick) {
        if (!target.worldObj.isRemote) return;
        current_attacking_target = null;
        if (target.getClass() != DseRayTarget.class) {
            return;
        }
        MovingObjectPosition hit = Hammer.proxy.getShadowHit();
        if (hit == null) {
            return; //huh.
        }
        DseRayTarget ray = (DseRayTarget) target;
        DimensionSliceEntity parent = ray.parent;
        if (!parent.can(DeltaCapability.INTERACT)) {
            return;
        }
        FMLProxyPacket toSend;
        event.setCanceled(true);
        switch (hit.typeOfHit) {
        case ENTITY:
            final byte packetId = rightClick ? HammerNetType.rightClickEntity : HammerNetType.leftClickEntity;
            toSend = HammerNet.makePacket(packetId, parent.getEntityId(), hit.entityHit.getEntityId());
            current_attacking_target = hit;
            break;
        case BLOCK:
            if (rightClick) {
                toSend = HammerNet.makePacket(HammerNetType.rightClickBlock, parent.getEntityId(), hit,
                        hit.hitVec.subtract(new Vec3(hit.getBlockPos())));
                Coord at = new Coord(DeltaChunk.getClientShadowWorld(), hit);
                Block block = at.getBlock();
                EntityPlayer real_player = Minecraft.getMinecraft().thePlayer;
                Hammer.proxy.setShadowWorld();
                try {
                    EntityPlayer shadow_player = Minecraft.getMinecraft().thePlayer;
                    if (block.onBlockActivated(at.w, at.toBlockPos(), at.getState(), shadow_player, hit.sideHit, (float) hit.hitVec.xCoord, (float) hit.hitVec.yCoord, (float) hit.hitVec.zCoord)) {
                        real_player.swingItem();
                    }
                } finally {
                    Hammer.proxy.restoreRealWorld();
                }
            } else {
                if (!parent.can(DeltaCapability.BLOCK_MINE)) {
                    return;
                }
                toSend = HammerNet.makePacket(HammerNetType.leftClickBlock, parent.getEntityId(), hit,
                        hit.hitVec.subtract(new Vec3(hit.getBlockPos())));
                current_attacking_target = hit;
                // Digging code happens in a ticker
            }
            break;
        default: return;
        }
        if (toSend == null) return;
        HammerNet.channel.sendToServer(toSend);
        //XXX Mmm, no, not quite. Need to do stuff in shadow on the client-side.
    }
    
    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        // NORELEASE: Just move everything to the proxy?
        if (event.phase != Phase.START) return;
        if (current_attacking_target == null) {
            resetProgress();
            return;
        }
        if (current_attacking_target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            resetProgress();
            return;
        }
        MovingObjectPosition hit = Hammer.proxy.getShadowHit();
        if (!current_attacking_target.getBlockPos().equals(hit.getBlockPos())
                || current_attacking_target.subHit != hit.subHit) {
            resetClick();
            resetProgress();
            return;
        }
        tickClickBlock(hit);
    }
    
    void sendDigPacket(byte packetType, MovingObjectPosition hit) {
        FMLProxyPacket toSend = HammerNet.makePacket(packetType, Hammer.proxy.getHitIDC().getEntityId(), hit);
        HammerNet.channel.sendToServer(toSend);
    }
    
    ItemStack original_tool;
    MovingObjectPosition original_block;
    
    void tickClickBlock(MovingObjectPosition hit) {
        if (left_click_delay > 0) {
            left_click_delay--;
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        World shadowWorld = DeltaChunk.getClientShadowWorld();
        Minecraft mc = Minecraft.getMinecraft();
        Coord at = new Coord(shadowWorld, hit);
        Block hitBlock = at.getBlock();
        PlayerControllerMP controller = mc.playerController;
        if (!(mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.inGameHasFocus)) {
            resetProgress();
            return;
        }
        if (!player.canPlayerEdit(hit.getBlockPos(), hit.sideHit, player.getHeldItem())) return;
        if (controller.currentGameType.isCreative()) {
            if (!Hammer.proxy.getHitIDC().can(DeltaCapability.BLOCK_MINE)) return;
            sendDigPacket(HammerNetType.digFinish, hit);
            Hammer.proxy.setShadowWorld();
            try {
                if (shadowWorld.extinguishFire(player, hit.getBlockPos(), hit.sideHit)) return; // :|
                controller.onPlayerDestroyBlock(hit.getBlockPos(), hit.sideHit);
            }
            finally {
                Hammer.proxy.restoreRealWorld();
            }
            return;
        }
        if (hitBlock == null || hitBlock.getMaterial() == Material.air) {
            resetProgress();
            return;
        }
        
        if (progress == 0) {
            hitBlock.onBlockClicked(shadowWorld, hit.getBlockPos(), player);
            original_block = hit;
            original_tool = player.getHeldItem();
        } else {
            if (!Hammer.proxy.getHitIDC().can(DeltaCapability.BLOCK_MINE)) {
                resetProgress();
                return;
            }
            ItemStack held = player.getHeldItem();
            if (original_tool != held) {
                if (original_tool == null || held == null) {
                    resetProgress();
                    return;
                }
                if (original_tool.getItem() != held.getItem()) {
                    // (Personally, I think the progress should be preserved if the player switches to a stronger tool.)
                    // (But we're copying vanilla here, not fixing it. Oh well.)
                    resetProgress();
                    return;
                }
            }
            if (!hit.getBlockPos().equals(original_block.getBlockPos())) {
                resetProgress();
                return;
            }
        }
        progress += hitBlock.getPlayerRelativeBlockHardness(player, shadowWorld, hit.getBlockPos());
        player.swingItem();
        byte packetType = progress == 0 ? HammerNetType.digStart : HammerNetType.digProgress;
        if (progress >= 1) {
            packetType = HammerNetType.digFinish;
        }
        sendDigPacket(packetType, hit);
        if (progress >= 1) {
            resetProgress();
            resetClick();
            Hammer.proxy.setShadowWorld();
            try {
                controller.onPlayerDestroyBlock(hit.getBlockPos(), hit.sideHit);
            } finally {
                Hammer.proxy.restoreRealWorld();
            }
        } else {
            HammerClientProxy.shadowRenderGlobal.sendBlockBreakProgress(player.getEntityId(), hit.getBlockPos(), (int) (progress * 10F) - 1);
        }
    }
    
    int left_click_delay = 5;
    void resetClick() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.keyBindAttack.pressTime = 1;
        if (mc.playerController.isInCreativeMode()) {
            left_click_delay = 5;
        }
    }
    
    void resetProgress() {
        if (current_attacking_target != null) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            MovingObjectPosition hit = current_attacking_target;
            HammerClientProxy.shadowRenderGlobal.sendBlockBreakProgress(player.getEntityId(), hit.getBlockPos(), -1);
        }
        progress = 0;
        current_attacking_target = null;
    }
    
    static float progress = 0;
}