package factorization.charge;

import factorization.shared.FzIcons;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.world.World;

public class EntitySteamFX extends EntitySmokeFX {

    public EntitySteamFX(World w, double x, double y, double z) {
        super(w, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0, 1);
        particleRed = particleGreen = particleBlue = 0.95F;
        setParticleTextureIndex(0);
        setParticleIcon(FzIcons.blocks$steam);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
