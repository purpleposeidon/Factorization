package factorization.charge;

import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.world.World;

public class EntitySteamFX extends EntitySmokeFX {

    public EntitySteamFX(World w, double x, double y, double z) {
        super(w, pos, 0, 0.05, 0);
        particleRed = particleGreen = particleBlue = 0.95F;
        setParticleTextureIndex(0);
    }

}
