package factorization.notify;

import com.google.common.base.Joiner;
import factorization.api.Coord;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import java.io.IOException;
import java.util.List;

public class PointCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "point";
    }

    @Override
    public String getCommandUsage(ICommandSender var1) {
        return "/point [optional text]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return super.canCommandSenderUseCommand(sender) && sender instanceof EntityPlayer;
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String msg = Joiner.on(" ").join(args);
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        MovingObjectPosition mop = getMouseOver(player, 64);
        if (mop == null || mop.typeOfHit == MovingObjectType.MISS) {
            sender.addChatMessage(new ChatComponentTranslation("notify.point.toofar"));
            return;
        }
        try {
            switch (mop.typeOfHit) {
            default: return;
            case BLOCK:
                PointNetworkHandler.INSTANCE.pointAtCoord(new Coord(player.worldObj, mop), msg);
                break;
            case ENTITY:
                PointNetworkHandler.INSTANCE.pointAtEntity(mop.entityHit, msg);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static MovingObjectPosition getMouseOver(EntityPlayer player, double reachDistance) {
        float par1 = 1;
        Entity pointedEntity = null;
        double d0 = reachDistance;
        MovingObjectPosition objectMouseOver = player.rayTrace(d0, par1);
        double d1 = d0;
        Vec3 vec3 = player.getPosition(par1);

        if (objectMouseOver != null) {
            d1 = objectMouseOver.hitVec.distanceTo(vec3);
        }

        Vec3 vec31 = player.getLook(par1);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0,
                vec31.zCoord * d0);
        pointedEntity = null;
        Vec3 vec33 = null;
        float f1 = 1.0F;
        List list = player.worldObj.getEntitiesWithinAABBExcludingEntity(
                player,
                player.boundingBox.addCoord(vec31.xCoord * d0,
                        vec31.yCoord * d0, vec31.zCoord * d0).expand(
                        (double) f1, (double) f1, (double) f1));
        double d2 = d1;

        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity) list.get(i);

            if (entity.canBeCollidedWith()) {
                float f2 = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.boundingBox.expand(
                        (double) f2, (double) f2, (double) f2);
                MovingObjectPosition movingobjectposition = axisalignedbb
                        .calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (0.0D < d2 || d2 == 0.0D) {
                        pointedEntity = entity;
                        vec33 = movingobjectposition == null ? vec3
                                : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity == player.ridingEntity
                                && !entity.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }
        }

        if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
            objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
        }
        return objectMouseOver;
    }
}
