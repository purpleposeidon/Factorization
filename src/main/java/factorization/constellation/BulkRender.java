package factorization.constellation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public class BulkRender {
    /**
     * Gets a tessellator that will be drawn using the Texture. <b>Do not cal this method if nothing will be drawn.</b>
     * @param textureName The {@link net.minecraft.util.ResourceLocation} that points to the texture that will be used.
     * @return A {@link net.minecraft.client.renderer.Tessellator} that is reading to start drawing quads.
     */
    public Tessellator getTessellator(ResourceLocation textureName) {
        TessInfo tess = tessmap.get(textureName);
        if (tess == null) {
            tess = newTess(textureName);
            tessmap.put(textureName, tess);
        }
        return bud(tess);
    }

    void finishDraw() {
        final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        for (TessInfo info : tessmap.values()) {
            textureManager.bindTexture(info.textureName);
            info.tess.draw();
        }
    }


    private static class TessInfo {
        final Tessellator tess = new Tessellator();
        final ResourceLocation textureName;
        int use_count = 0;

        private TessInfo(ResourceLocation textureName) {
            this.textureName = textureName;
        }
    }

    private HashMap<ResourceLocation, TessInfo> tessmap = new HashMap<ResourceLocation, TessInfo>();

    private TessInfo newTess(ResourceLocation textureName) {
        final TessInfo tess = new TessInfo(textureName);
        tess.tess.startDrawingQuads();
        return tess;
    }

    private Tessellator bud(TessInfo tess) {
        tess.use_count++;
        return tess.tess;
    }
}
