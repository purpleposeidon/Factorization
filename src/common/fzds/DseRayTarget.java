package factorization.fzds;

import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import factorization.common.Core;
import factorization.fzds.HammerNet.HammerNetType;

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
        @ForgeSubscribe(priority = EventPriority.HIGHEST)
        public void leftClick(AttackEntityEvent event) {
            handle(event, event.target, false);
        }
        
        @ForgeSubscribe(priority = EventPriority.HIGHEST)
        public void rightClick(EntityInteractEvent event) {
            handle(event, event.target, true);
        }
        
        void handle(PlayerEvent event, Entity target, boolean right) {
            if (target.getClass() != DseRayTarget.class) {
                return;
            }
            HammerClientProxy hcp = (HammerClientProxy) Hammer.proxy;
            MovingObjectPosition hit = hcp.shadowSelected;
            if (hit == null) {
                return; //huh.
            }
            event.setCanceled(true);
            DseRayTarget ray = (DseRayTarget) target;
            Packet toSend = null;
            switch (hit.typeOfHit) {
            case ENTITY:
                toSend = HammerNet.makePacket(right ? HammerNetType.rightClickEntity : HammerNetType.leftClickEntity, ray.parent.entityId);
                break;
            case TILE:
                toSend = HammerNet.makePacket(right ? HammerNetType.rightClickBlock : HammerNetType.leftClickBlock, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit,
                        (float) (hit.hitVec.xCoord - hit.blockX), (float) (hit.hitVec.yCoord - hit.blockY), (float) (hit.hitVec.zCoord - hit.blockZ));
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
