package factorization.client.render;

import factorization.api.VectorUV;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.Texture;
import factorization.common.TileEntityMirror;
import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderMirrorStand extends FactorizationBlockRender {
    RenderingCube mirror = new RenderingCube(Texture.mirrorStart, new VectorUV(6, 0.5F, 6));
    
    private static final int frontFace[] = {1};
    private static final int backFace[] = {0};
    private static final int sideFace[] = {2, 3, 4, 5};
    @Override
    void render(RenderBlocks rb) {
        Core.profileStart("mirror");
        float height = 7.25F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        renderPart(rb, Texture.silver, c - radius, 0, c - radius, c + radius, height, c + radius);
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, Texture.silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
        
        mirror.trans.reset();
        if (world_mode) {
            TileEntityMirror tem = getCoord().getTE(TileEntityMirror.class);
            if (tem != null && tem.target_rotation >= 0) {
                mirror.trans.rotate(0, -1, 0, (float) Math.toRadians(tem.target_rotation + 90));
                mirror.trans.rotate(1, 0, 0, (float) Math.toRadians(-45));
            }
        }
        
        mirror.setIcon(Texture.mirrorStart + 0);
        renderCube(mirror, frontFace);
        mirror.setIcon(Texture.mirrorStart + 1);
        renderCube(mirror, backFace);
        mirror.setIcon(Texture.mirrorStart + 2);
        renderCube(mirror, sideFace);
        Core.profileEnd();
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }
}
