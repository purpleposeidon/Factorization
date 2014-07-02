package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.fzds.HammerNet.HammerNetType;
import factorization.fzds.api.DeltaCapability;
import factorization.shared.Core;

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
                toSend = HammerNet.makePacket(HammerNetType.rightClickBlock, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                        (float) (hit.hitVec.xCoord - hit.blockX),
                        (float) (hit.hitVec.yCoord - hit.blockY),
                        (float) (hit.hitVec.zCoord - hit.blockZ));
            } else {
                if (!parent.can(DeltaCapability.BLOCK_MINE)) {
                    return;
                }
                //TODO XXX FIXME: Implement proper digging
                toSend = HammerNet.makePacket(HammerNetType.leftClickBlock, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                        (float) (hit.hitVec.xCoord - hit.blockX),
                        (float) (hit.hitVec.yCoord - hit.blockY),
                        (float) (hit.hitVec.zCoord - hit.blockZ));
                Hammer.proxy.mineBlock(hit);
                current_attacking_target = hit;
            }
            break;
        default: return;
        }
        HammerNetEventHandler.INSTANCE.channel.sendToServer(toSend);
        //XXX Mmm, no, not quite. Need to do stuff in shadow on the client-side.
    }
    
    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        if (current_attacking_target == null) return;
        MovingObjectPosition hit = Hammer.proxy.getShadowHit();
        if (current_attacking_target.blockX != hit.blockX
                || current_attacking_target.blockY != hit.blockY
                || current_attacking_target.blockZ != hit.blockZ
                || current_attacking_target.subHit != hit.subHit) {
            current_attacking_target = null;
            return;
        }
        Hammer.proxy.mineBlock(hit);
        //NORELEASE: Proxify?
        
    }
}