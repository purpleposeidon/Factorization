package factorization.fzds;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.api.Coord;
import factorization.fzds.HammerNet.HammerNetType;
import factorization.fzds.api.DeltaCapability;

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
        HammerClientProxy hcp = (HammerClientProxy) Hammer.proxy;
        MovingObjectPosition hit = hcp.shadowSelected;
        if (hit == null) {
            return; //huh.
        }
        DseRayTarget ray = (DseRayTarget) target;
        DimensionSliceEntity parent = ray.parent;
        if (!parent.can(DeltaCapability.INTERACT)) {
            return;
        }
        event.setCanceled(true);
        FMLProxyPacket toSend = null;
        switch (hit.typeOfHit) {
        case ENTITY:
            toSend = HammerNet.makePacket(rightClick ? HammerNetType.rightClickEntity : HammerNetType.leftClickEntity, ray.parent.getEntityId());
            current_attacking_target = hit;
            break;
        case BLOCK:
            if (rightClick) {
                if (!parent.can(DeltaCapability.BLOCK_PLACE)) {
                    return;
                }
                toSend = HammerNet.makePacket(HammerNetType.rightClickBlock, parent.getEntityId(), hit,
                        (float) (hit.hitVec.xCoord - hit.blockX),
                        (float) (hit.hitVec.yCoord - hit.blockY),
                        (float) (hit.hitVec.zCoord - hit.blockZ));
            } else {
                if (!parent.can(DeltaCapability.BLOCK_MINE)) {
                    return;
                }
                //TODO XXX FIXME: Implement proper digging
                toSend = HammerNet.makePacket(HammerNetType.leftClickBlock, parent.getEntityId(), hit,
                        (float) (hit.hitVec.xCoord - hit.blockX),
                        (float) (hit.hitVec.yCoord - hit.blockY),
                        (float) (hit.hitVec.zCoord - hit.blockZ));
                current_attacking_target = hit;
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
        if (current_attacking_target.blockX != hit.blockX
                || current_attacking_target.blockY != hit.blockY
                || current_attacking_target.blockZ != hit.blockZ
                || current_attacking_target.subHit != hit.subHit) {
            // NORELEASE: Cursor moved off the block. Make it so that the click can re-fire
            resetProgress();
            return;
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World shadowWorld = DeltaChunk.getClientShadowWorld();
        Minecraft mc = Minecraft.getMinecraft();
        if (player == null) return;
        
        Coord at = new Coord(shadowWorld, hit);
        Block hitBlock = at.getBlock();
        if (hitBlock == null || hitBlock.getMaterial() == Material.air) {
            resetProgress();
            return;
        }
        if (progress == 0) {
            hitBlock.onBlockClicked(shadowWorld, hit.blockX, hit.blockY, hit.blockZ, player);
        }
        //float relativeHardness = hitBlock.getPlayerRelativeBlockHardness(player, shadowWorld, hit.blockX, hit.blockY, hit.blockZ);
        if (!(mc.currentScreen == null && mc.gameSettings.keyBindAttack.getIsKeyPressed() && mc.inGameHasFocus)) {
            resetProgress();
            return;
        }
        player.swingItem();
        int int_progress = (int) (progress/10F);
        byte packetType = progress == 0 ? HammerNetType.digStart : HammerNetType.digProgress;
        progress++;
        if (int_progress >= 10) {
            packetType = HammerNetType.digFinish;
        }
        FMLProxyPacket toSend = HammerNet.makePacket(packetType, Hammer.proxy.getHitIDC().getEntityId(), hit);
        HammerNet.channel.sendToServer(toSend);
        if (int_progress >= 10) {
            resetProgress();
            System.out.println("NORELEASE: Pop!");
            // NORELEASE: Refire the click here as well!
            Hammer.proxy.setShadowWorld();
            try {
                Minecraft.getMinecraft().playerController.onPlayerDestroyBlock(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);
            } finally {
                Hammer.proxy.restoreRealWorld();
            }
        } else {
            HammerClientProxy.shadowRenderGlobal.destroyBlockPartially(player.getEntityId(), hit.blockX, hit.blockY, hit.blockZ, int_progress);
        }
    }
    
    void resetProgress() {
        if (current_attacking_target != null) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            MovingObjectPosition hit = current_attacking_target;
            HammerClientProxy.shadowRenderGlobal.destroyBlockPartially(player.getEntityId(), hit.blockX, hit.blockY, hit.blockZ, -1);
        }
        progress = 0;
        current_attacking_target = null;
    }
    
    static float progress = 0;
}