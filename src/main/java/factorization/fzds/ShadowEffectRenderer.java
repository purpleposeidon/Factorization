package factorization.fzds;

import cpw.mods.fml.repackage.com.nothome.delta.Delta;
import factorization.api.Coord;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ShadowEffectRenderer extends EffectRenderer {
    final EffectRenderer original;
    final World realWorld;
    final Entity camera;

    public ShadowEffectRenderer(World shadowWorld, TextureManager textureManager, EffectRenderer original, World realWorld, Entity camera) {
        super(shadowWorld, textureManager);
        this.original = original;
        this.realWorld = realWorld;
        this.camera = camera;
    }

    IDeltaChunk idc = null;

    IDeltaChunk getClosestIdc(EntityFX fx) {
        final Coord pos = new Coord(fx);
        final int r = 3;
        final int n = -r * 2;
        if (idc != null && idc.getCorner().lesserOrEqual(pos.adjust(r, r, r)) && pos.adjust(n, n, n).lesserOrEqual(idc.getFarCorner())) {
            // Particles may spawn slightly outside the IDC, accomidate them. (And IDC padding's usually fairly large.)
            // If the particle's further than the threshold, the correct IDC will still be found.
            return idc;
        }
        return idc = DeltaChunk.findClosest(camera, pos);
    }

    @Override
    public void addEffect(EntityFX fx) {
        IDeltaChunk idc = getClosestIdc(fx);
        Vec3 at = SpaceUtil.fromEntPos(fx);
        SpaceUtil.toEntPos(fx, idc.shadow2real(at));
        fx.worldObj = realWorld;
        fx.prevPosX = fx.posX;
        fx.prevPosY = fx.posY;
        fx.prevPosZ = fx.posZ;
        original.addEffect(fx);
    }
}