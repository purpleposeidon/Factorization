package factorization.common;

import factorization.algos.ReservoirSampler;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.beauty.EntityFXSteam;
import factorization.shared.EntityFz;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntitySelectors;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class EntitySteamGeyser extends EntityFz {
    public EntitySteamGeyser(World worldIn) {
        super(worldIn);
        width = 0.5F;
        height = 8;
    }

    @Override
    protected void entityInit() {

    }

    private static final int MAX_LIFETIME = 30;
    int lifetime = MAX_LIFETIME;
    @Override
    protected void putData(DataHelper data) throws IOException {
        lifetime = data.as(Share.VISIBLE, "lifetime").putInt(lifetime);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        double min_speed = 0.09, max_speed = 0.3;
        double speed = NumUtil.interp(min_speed, max_speed, (double) lifetime / MAX_LIFETIME);
        for (Entity ent : worldObj.getEntitiesInAABBexcluding(this, getEntityBoundingBox(), EntitySelectors.selectAnything)) {
            double dy = ent.isSneaking() ? min_speed : speed;
            ent.addVelocity(0, dy, 0);
            if (worldObj.isRemote || rand.nextInt(16) != 0 || !(ent instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase elb = (EntityLivingBase) ent;
            Collection<PotionEffect> potions = elb.getActivePotionEffects();
            if (potions.isEmpty()) continue;
            ReservoirSampler<PotionEffect> reservoir = new ReservoirSampler<PotionEffect>(1, rand);
            reservoir.giveAll(potions);
            for (PotionEffect pot : reservoir) {
                List<ItemStack> cures = pot.getCurativeItems();
                if (cures == null || cures.isEmpty()) continue;
                elb.removePotionEffect(pot.getPotionID());
            }
        }
        if (lifetime-- < 0) {
            setDead();
        }
        if (worldObj.isRemote) {
            spawnParticles();
        }
    }

    @SideOnly(Side.CLIENT)
    void spawnParticles() {
        final double r = 0.5;
        EffectRenderer effectRenderer = Minecraft.getMinecraft().effectRenderer;
        for (double dy = 0; dy < height; dy += r) {
            EntityFXSteam particle = new EntityFXSteam(worldObj, posX, posY + dy, posZ);
            particle.motionX = rand.nextGaussian() / 16;
            particle.motionY = 0.3 + Math.abs(rand.nextGaussian()) / 16;
            particle.motionZ = rand.nextGaussian() / 16;
            effectRenderer.addEffect(particle);
        }
    }
}
