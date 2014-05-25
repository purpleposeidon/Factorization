package factorization.fzds;

import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.MovingObjectPosition;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import factorization.fzds.HammerNet.HammerNetType;
import factorization.fzds.api.DeltaCapability;
import factorization.shared.Core;

public class DseRayTarget extends Entity {
    //This is used on the client side to give the player something to smack
    DimensionSliceEntity parent;

    public DseRayTarget(DimensionSliceEntity parent) {
        super(parent.worldObj);
        this.parent = parent;
    }

    @Override
    protected void entityInit() { }

    @Override
    protected void readEntityFromNBT(NBTTagCompound var1) { }

    @Override
    protected void writeEntityToNBT(NBTTagCompound var1) { }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    public static class ClickHandler {
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
        
        void handle(PlayerEvent event, Entity target, boolean rightClick) {
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
            Packet toSend = null;
            switch (hit.typeOfHit) {
            case ENTITY:
                toSend = HammerNet.makePacket(rightClick ? HammerNetType.rightClickEntity : HammerNetType.leftClickEntity, ray.parent.getEntityId());
                break;
            case BLOCK:
                if (rightClick) {
                    if (!parent.can(DeltaCapability.BLOCK_PLACE)) {
                        return;
                    }
                    toSend = HammerNet.makePacket(HammerNetType.rightClickBlock, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                            (float) (hit.hitVec.xCoord - hit.blockX), (float) (hit.hitVec.yCoord - hit.blockY), (float) (hit.hitVec.zCoord - hit.blockZ));
                } else {
                    if (!parent.can(DeltaCapability.BLOCK_MINE)) {
                        return;
                    }
                    //TODO XXX FIXME: Implement proper digging
                    toSend = HammerNet.makePacket(HammerNetType.leftClickBlock, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                            (float) (hit.hitVec.xCoord - hit.blockX), (float) (hit.hitVec.yCoord - hit.blockY), (float) (hit.hitVec.zCoord - hit.blockZ));
                    //Hammer.proxy.mineBlock(hit);
                    //return;
                }
                break;
            default:
                Core.logWarning("What did you just click? " + hit.typeOfHit + " " + hit);
                return;
            }
            PacketDispatcher.sendPacketToServer(toSend);
            //XXX Mmm, no, not quite. Need to do stuff in shadow on the client-side.
        }
    }
}
