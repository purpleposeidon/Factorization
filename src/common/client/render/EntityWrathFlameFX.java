package factorization.client.render;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import factorization.api.Coord;
import factorization.common.Core;

public class EntityWrathFlameFX extends EntityFX {
    double defaultY = 0;

    public EntityWrathFlameFX(World par1World, double x, double y, double z,
            double vx, double vy, double vz) {
        super(par1World, x, y, z, vx, vy, vz);
        this.particleTextureJitterX = 0;
        this.particleTextureJitterY = 0;
        this.particleScale = 2;
        setParticleTextureIndex(0);
        this.particleGravity = 0;

        this.motionX = vx;
        this.motionY = vy;
        this.motionZ = vz;
        defaultY = vy;
        this.particleRed = (float) (0.90F - rand.nextFloat() * 0.1);
        this.particleGreen = (20 + rand.nextInt(100)) / 255;
        this.particleBlue = 0.05F;
        this.particleMaxAge = (int) (50 * (1 + 0.1 * rand.nextFloat()));
    }

    static final float colorDelta = 32F / 255F;
    static final float scaleDelta = 0.03F;
    static final float motionDelta = 0.1F;

    
    
    //	@Override
    //	public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4,
    //			float par5, float par6, float par7) {
    //		//XXX TODO: This is probably inefficient?
    //		//We could pretend to be a digging/breaking particle; then forge'd help out.
    //		RenderEngine engine = ModLoader.getMinecraftInstance().renderEngine;
    //		//int orig_text = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    //		engine.bindTexture(engine.getTexture(FactorizationCore.texture_file_particle));
    //		super.renderParticle(par1Tessellator, par2, par3, par4, par5, par6, par7);
    //		engine.bindTexture(engine.getTexture("/particles.png"));
    //		//engine.bindTexture(orig_text);
    //	}

    @Override
    public void onUpdate() {
        Coord here = new Coord(this);
        //		if (here.isSolid() || !here.isAir()) {
        //			setDead();
        //			return;
        //		}
        super.onUpdate();
        //if on a roof, wiggle about...
        if (motionY <= 0.01 && motionY >= 0 && particleAge > particleMaxAge / 10) {
            if (Math.abs(motionX) < 0.1) {
                motionX *= 1.5;
            }
            if (Math.abs(motionZ) < 0.1) {
                motionZ *= 1.5;
            }
            if (Math.abs(motionZ) > Math.abs(motionX)) {
                motionX = 0;
            }
            else {
                motionZ = 0;
            }
            motionY = defaultY;
        }
        else {
            motionX *= 0.8;
            motionZ *= 0.8;
            if (Math.abs(motionX) > 0.1) {
                motionX *= 0.5;
            }
            if (Math.abs(motionZ) > 0.1) {
                motionZ *= 0.5;
            }
        }
        //grow, then shrink
        if (particleAge > particleMaxAge / 4) {
            particleScale -= scaleDelta * 2;
            if (particleAge > particleMaxAge * 3 / 4) {
                particleScale -= scaleDelta * 3;
            }
            if (particleScale < 0.1) {
                particleScale = 0.1F;
            }
        }
        else {
            particleScale += scaleDelta;
        }

        //go from [red-orange] to [yellow] to [black]
        if (particleAge < particleMaxAge / 3) {
            //hold color
        }
        else if (particleAge < particleMaxAge * 2 / 3) {
            particleGreen += colorDelta;
            particleBlue += colorDelta / 8;
            if (particleGreen > 0.7) {
                particleGreen = 0.7F;
            }
            particleGravity = -0.01F;
        }
        else {
            particleRed -= colorDelta;
            particleGreen -= colorDelta * 2;
            particleBlue -= colorDelta * 2;
            float gray = 0.1F;
            if (particleRed < gray)
                particleRed = gray;
            if (particleGreen < gray)
                particleGreen = gray;
            if (particleBlue < gray)
                particleBlue = gray;
        }
        setParticleTextureIndex(0);
    }

//	@Override
//	public String getTextureFile() {
//		return Core.texture_file_item;
//	}

    public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4,
            float par5, float par6, float par7)
    {
        super.renderParticle(par1Tessellator, par2, par3, par4, par5, par6, par7);
        //I guess we don't need this anymore? (Dude, could have just wrapped the super call in the bind...)
        /*
        ForgeHooksClient.bindTexture(Core.texture_file_item, 0);
        float var8 = (float) (0 % 16) / 16.0F;
        float var9 = var8 + 0.0624375F;
        float var10 = (float) (0 / 16) / 16.0F;
        float var11 = var10 + 0.0624375F;
        float var12 = 0.1F * this.particleScale;
        float var13 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) par2 - interpPosX);
        float var14 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) par2 - interpPosY);
        float var15 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) par2 - interpPosZ);
        float var16 = 1.0F;
        par1Tessellator.setColorOpaque_F(this.particleRed * var16, this.particleGreen * var16, this.particleBlue * var16);
        par1Tessellator.setBrightness(0xF0);
        par1Tessellator.addVertexWithUV((double) (var13 - par3 * var12 - par6 * var12), (double) (var14 - par4 * var12), (double) (var15 - par5 * var12 - par7 * var12), (double) var9, (double) var11);
        par1Tessellator.addVertexWithUV((double) (var13 - par3 * var12 + par6 * var12), (double) (var14 + par4 * var12), (double) (var15 - par5 * var12 + par7 * var12), (double) var9, (double) var10);
        par1Tessellator.addVertexWithUV((double) (var13 + par3 * var12 + par6 * var12), (double) (var14 + par4 * var12), (double) (var15 + par5 * var12 + par7 * var12), (double) var8, (double) var10);
        par1Tessellator.addVertexWithUV((double) (var13 + par3 * var12 - par6 * var12), (double) (var14 - par4 * var12), (double) (var15 + par5 * var12 - par7 * var12), (double) var8, (double) var11);
        ForgeHooksClient.unbindTexture();
        */
    }

    public void setScale(int scale) {
        particleScale = scale;
    }
}
