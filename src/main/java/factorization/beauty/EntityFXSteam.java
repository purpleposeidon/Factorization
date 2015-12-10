package factorization.beauty;

import factorization.shared.FzIcons;
import factorization.util.NumUtil;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

public class EntityFXSteam extends EntityFX {
    int ux, vx;
    protected EntityFXSteam(World world, double x, double y, double z) {
        super(world, x, y, z);
        setParticleIcon(FzIcons.blocks$steam);
        ux = world.rand.nextInt(0xF - 4);
        vx = world.rand.nextInt(0xF - 4);
        particleAlpha = 0.25F;
        particleScale = NumUtil.interp(0.75F, 1.25F, world.rand.nextFloat());
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.particleScale *= 1.04;
        this.particleAlpha *= 0.96F;
    }
}
