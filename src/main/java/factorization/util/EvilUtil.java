package factorization.util;

import factorization.api.Coord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Random;

public class EvilUtil {

    public static int randNear(Random random, int range) {
        if (range <= 0) return 0;
        return random.nextInt(range * 2 + 1) - range;
    }

    public static boolean spawn(TileEntity te, Random random, int range, Entity ent) {
        positionEntity(te, random, range, ent);
        return trySpawn(te, ent);
    }

    public static boolean canSpawn(Entity ent) {
        if (ent instanceof EntityLiving) {
            EntityLiving alive = (EntityLiving) ent;
            return alive.getCanSpawnHere();
        }
        return true;
    }

    private static boolean trySpawn(TileEntity te, Entity ent) {
        if (canSpawn(ent)) {
            forceSpawn(te, ent);
            return true;
        }
        return false;
    }

    public static void forceSpawn(TileEntity te, Entity ent) {
        ent.worldObj.spawnEntityInWorld(ent);
        ent.worldObj.playAuxSFX(2004, te.xCoord, te.yCoord, te.zCoord, 0);
        if (ent instanceof EntityLiving) {
            ((EntityLiving) ent).spawnExplosionParticle();
        }
    }

    public static void positionEntity(TileEntity te, Random random, int range, Entity ent) {
        double x = te.xCoord + randNear(random, range);
        double y = te.yCoord + randNear(random, range);
        double z = te.zCoord + randNear(random, range);
        ent.setLocationAndAngles(x, y, z, random.nextFloat() * 360.0F, 0.0F);
    }

    public static void givePotion(EntityLiving ent, Potion potion, int level, boolean ambient) {
        ent.addPotionEffect(new PotionEffect(potion.getId(), Short.MAX_VALUE, level, ambient));
    }

    public static EntityPlayer getClosestPlayer(Coord at, double maxRange) {
        maxRange *= maxRange;
        double bestDist = Double.POSITIVE_INFINITY;
        EntityPlayer best = null;
        for (EntityPlayer player : (Iterable<EntityPlayer>) at.w.playerEntities) {
            if (PlayerUtil.isPlayerCreative(player)) continue;
            double dist = player.getDistanceSq(at.x + 0.5, at.y + 0.5, at.z + 0.5);
            if (dist > maxRange) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    public static float getMoonPower(World worldObj) {
        float moon = worldObj.getCurrentMoonPhaseFactor();
        if (worldObj.isDaytime()) {
            moon = 0;
        }
        if (worldObj.isThundering()) {
            moon += 0.75;
        }
        if (worldObj.isRaining()) {
            moon *= 2;
        }
        return moon;
    }
}
