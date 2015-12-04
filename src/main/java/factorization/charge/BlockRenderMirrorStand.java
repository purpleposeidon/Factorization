package factorization.charge;

import factorization.common.FzConfig;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.util.EnumFacing;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.ResourceType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderMirrorStand extends FactorizationBlockRender {
    private static final int frontFace[] = {1};
    private static final int backFace[] = {0};
    private static final int sideFace[] = {2, 3, 4, 5};
    
    private static Quaternion mirrorTilt = Quaternion.getRotationQuaternionRadians(Math.toRadians(-45), 1, 0, 0);
    @Override
    public boolean render(RenderBlocks rb) {
        if (!world_mode) {
            return false;
        }
        Core.profileStart("mirror");
        float height = 7.25F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        IIcon silver = Core.registry.resource_block.getIcon(0, ResourceType.SILVERBLOCK.md);
        
        //Pole
        BlockRenderHelper block = Core.registry.blockRender;
        block.useTexture(silver);
        block.setTexture(0, null);
        block.setBlockBounds(c - radius, 0, c - radius, c + radius, height, c + radius);
        block.render(rb, x, y, z);
        
        //Base
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
        
        //Mirror
        block.setBlockBoundsOffset(2F/16F, 7.5F/16F, 2F/16F);
        //block.setBlockBoundsOffset(0, 0, 0);
        //block.setBlockBounds(0, 0, 0, 1, 1F/16F, 1);
        IIcon side = BlockIcons.mirror_side;
        IIcon face = BlockIcons.mirror_front;
        block.useTextures(face, face, side, side, side, side);
        
        block.beginWithMirroredUVs();
        Coord here = getCoord();
        
        if (world_mode) {
            TileEntityMirror mirror = (TileEntityMirror) te;
            if (mirror != null && mirror.target_rotation >= 0) {
                block.translate(-0.5F, 0F, 0F);
                Quaternion trans = Quaternion.getRotationQuaternionRadians(Math.toRadians(mirror.target_rotation - 90), EnumFacing.UP);
                trans.incrMultiply(mirrorTilt);
                block.rotate(trans);
                block.translate(0.5F, -0.20F, 0.5F);
            }
        }
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
        }
        block.renderRotated(Tessellator.instance, here);
        if (!world_mode) {
            Tessellator.instance.draw();
        }
        Core.profileEnd();
        return true;
    }

    @Override
    public boolean renderSecondPass(RenderBlocks rb) {
        if (!world_mode) return false;
        if (!FzConfig.mirror_sunbeams) return false;
        if (rb.overrideBlockTexture != null) return false;
        TileEntityMirror mirror = (TileEntityMirror) te;
        Coord hit = mirror.reflection_target;
        if (hit == null) return false;
        if (hit.x == x && hit.z == z && hit.y == y) return false;
        boolean sun = mirror.hasSun();
        mirror.last_drawn_as_lit = sun;
        if (!sun) return false;
        // *could* have a customly animated texture that updates to match the light level, similar to vanilla lighting...
        // And it's not like the whole world'd need to update; just a few chunks.
        float poses[] = new float[] { -6F/16F, 6F/16F };
        Quaternion trans = Quaternion.getRotationQuaternionRadians(Math.toRadians(mirror.target_rotation - 90), EnumFacing.UP);
        trans.incrMultiply(mirrorTilt);
        Vec3[] points = new Vec3[4];
        int i = 0;
        for (int sx = 0; sx < 2; sx++) {
            for (int sz = 0; sz < 2; sz++) {
                Vec3 vec = new Vec3(poses[sx], 0F, poses[sz]);
                trans.applyRotation(vec);
                vec.xCoord += x + 0.5F;
                vec.yCoord += y + 0.5F;
                vec.zCoord += z + 0.5F;
                points[i++] = vec;
            }
        }
        Vec3 src = new Vec3(x + 0.5, y + 0.5, z + 0.5);
        Vec3 middle = mirror.reflection_target.toMiddleVector();
        Vec3 dif = SpaceUtil.subtract(middle, src);
        float N = 1.125F;
        float invN = 1 / N;
        SpaceUtil.incrScale(dif, N);
        Vec3 far = SpaceUtil.add(src, dif);

        Tessellator tess = Tessellator.instance;
        tess.setBrightness(Blocks.glowstone.getMixedBrightnessForBlock(w, x, y, z));
        IIcon icon = BlockIcons.mirror_beam;
        tess.setColorOpaque_F(1, 1, 1);
        byte[] as = new byte[] { 2, 1, 0, 3 };
        byte[] bs = new byte[] { 0, 3, 1, 2 };
        Vec3 work = SpaceUtil.newVec();
        // default opacity: 38/0xFF
        float min_opacity = 24F / 255F;
        float opacity_per_power = 4F / 255F; // max is 9
        float alpha = min_opacity + mirror.getPower() * opacity_per_power;
        Tessellator.instance.setColorRGBA_F(1, 1, 1, alpha);
        for (i = 0; i < 4; i++) {
            Vec3 a = points[as[i]];
            Vec3 b = points[bs[i]];

            NumUtil.interp(b, far, invN, work);
            //SpaceUtil.set(work, b);
            //SpaceUtil.incrAdd(work, far);
            //SpaceUtil.incrScale(work, invN);
            tess.addVertexWithUV(work.xCoord, work.yCoord, work.zCoord, icon.getMinU(), icon.getMinV());
            NumUtil.interp(a, far, invN, work);
            //SpaceUtil.set(work, a);
//          //SpaceUtil.incrAdd(work, far);
            //SpaceUtil.incrScale(work, invN);
            tess.addVertexWithUV(work.xCoord, work.yCoord, work.zCoord, icon.getMaxU(), icon.getMinV());
            tess.addVertexWithUV(a.xCoord, a.yCoord, a.zCoord, icon.getMinU(), icon.getMaxV());
            tess.addVertexWithUV(b.xCoord, b.yCoord, b.zCoord, icon.getMaxU(), icon.getMaxV());
            // Could add interior faces as well. Meh!
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }
}
