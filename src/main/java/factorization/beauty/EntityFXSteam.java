package factorization.beauty;

import factorization.util.NumUtil;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class EntityFXSteam extends EntityFX {
    int ux, vx;
    protected EntityFXSteam(World world, double x, double y, double z, IIcon icon) {
        super(world, x, y, z);
        setParticleIcon(icon);
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

    @Override
    public void renderParticle(Tessellator tess, float partial, float dx, float dy, float dz, float xChange, float yChange) {
        float u0 = (float)this.particleTextureIndexX / 16.0F;
        float u1 = u0 + 0.0624375F;
        float v0 = (float)this.particleTextureIndexY / 16.0F;
        float v1 = v0 + 0.0624375F;
        float scale = 0.1F * this.particleScale;

        if (this.particleIcon != null) {
            u0 = this.particleIcon.getInterpolatedU(ux);
            u1 = this.particleIcon.getInterpolatedU(ux + 4);
            v0 = this.particleIcon.getInterpolatedV(vx);
            v1 = this.particleIcon.getInterpolatedV(vx + 4);
        }

        float x = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partial - interpPosX);
        float y = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partial - interpPosY);
        float z = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partial - interpPosZ);
        tess.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
        tess.addVertexWithUV((double) (x - dx * scale - xChange * scale), (double) (y - dy * scale), (double) (z - dz * scale - yChange * scale), (double) u1, (double) v1);
        tess.addVertexWithUV((double) (x - dx * scale + xChange * scale), (double) (y + dy * scale), (double) (z - dz * scale + yChange * scale), (double) u1, (double) v0);
        tess.addVertexWithUV((double) (x + dx * scale + xChange * scale), (double) (y + dy * scale), (double) (z + dz * scale + yChange * scale), (double) u0, (double) v0);
        tess.addVertexWithUV((double) (x + dx * scale - xChange * scale), (double) (y - dy * scale), (double) (z + dz * scale - yChange * scale), (double) u0, (double) v1);
    }
}
