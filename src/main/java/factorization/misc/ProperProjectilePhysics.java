package factorization.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ProperProjectilePhysics {
    @SubscribeEvent
    public void projectileSpawn(EntityJoinWorldEvent event) {
        if (event.entity instanceof IProjectile) { // Most entities that get spawned aren't projectiles, of course.
            if (event.entity instanceof EntityThrowable) {
                EntityThrowable ent = (EntityThrowable) event.entity;
                Entity thrower = ent.getThrower();
                if (thrower == null) return;
                ent.motionX += thrower.motionX;
                ent.motionY += thrower.motionY;
                ent.motionZ += thrower.motionZ;
            } else if (event.entity instanceof EntityArrow) {
                EntityArrow ent = (EntityArrow) event.entity;
                Entity thrower = ent.shootingEntity;
                if (thrower == null) return;
                ent.motionX += thrower.motionX;
                ent.motionY += thrower.motionY;
                ent.motionZ += thrower.motionZ;
            }
        }
    }
}
