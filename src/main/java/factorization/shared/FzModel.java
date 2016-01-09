package factorization.shared;

import com.google.common.base.Function;
import factorization.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

public class FzModel {
    // Don't SideOnly this class.

    final String name;
    @SideOnly(Side.CLIENT)
    IBakedModel model;

    public FzModel(String name) {
        this.name = "fzmodel/" + name;
        instances.add(this);
        if (ModelWrangler.hasLoaded) {
            Core.logSevere("FzModel was constructed too late: " + name);
        }
    }

    private static final ArrayList<FzModel> instances = new ArrayList<FzModel>();
    static {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new ModelWrangler());
        }
    }

    @SideOnly(Side.CLIENT)
    public void draw() {
        draw(-1);
    }

    @SideOnly(Side.CLIENT)
    public void draw(int colorARGB) {
        if (model == null) return;
        RenderUtil.checkGLError("Unknown error");
        GlStateManager.color(1, 1, 1, 1);
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(Core.blockAtlas);
        Tessellator tessI = Tessellator.getInstance();
        WorldRenderer tess = tessI.getWorldRenderer();
        tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        putQuads(tess, model.getGeneralQuads(), colorARGB);
        for (EnumFacing ef : EnumFacing.VALUES) {
            putQuads(tess, model.getFaceQuads(ef), colorARGB);
        }
        tessI.draw();
        RenderUtil.checkGLError("model draw");
    }

    @SideOnly(Side.CLIENT)
    public void putQuads(WorldRenderer tess, List<BakedQuad> quads, int color) {
        for (BakedQuad quad : quads) {
            tess.addVertexData(quad.getVertexData());
            tess.putColor4(color);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ModelWrangler {
        static boolean hasLoaded = false;
        @SubscribeEvent
        public void loadModels(TextureStitchEvent event) {
            hasLoaded = true;
            //event.modelBakery.reg
            Minecraft mc = Minecraft.getMinecraft();

            IResourceManager irm = mc.getResourceManager();
            HashSet<ResourceLocation> textures = new HashSet<ResourceLocation>();
            IdentityHashMap<FzModel, IModel> raws = new IdentityHashMap<FzModel, IModel>();
            for (FzModel fzm : instances) {
                fzm.model = null;
                ResourceLocation location = fzm.getLocation(irm);
                if (location == null) continue;
                IModel rawModel = null;
                try {
                    rawModel = ModelLoaderRegistry.getModel(location);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (rawModel == null) continue;
                raws.put(fzm, rawModel);
                textures.addAll(rawModel.getTextures());
            }
            for (ResourceLocation texture : textures) {
                event.map.registerSprite(texture);
            }
            final TextureMap map = event.map;
            Function<ResourceLocation,TextureAtlasSprite> lookup = new Function<ResourceLocation, TextureAtlasSprite>() {
                @Nullable
                @Override
                public TextureAtlasSprite apply(@Nullable ResourceLocation input) {
                    if (input == null) return map.getAtlasSprite(null);
                    return map.getAtlasSprite(input.toString());
                }
            };
            IModelState identityMatrix = new TRSRTransformation(null, null, null, null);
            for (FzModel fzm : instances) {
                IModel rawModel = raws.get(fzm);
                if (rawModel == null) {
                    fzm.model = null;
                    continue;
                }
                fzm.model = rawModel.bake(identityMatrix, DefaultVertexFormats.BLOCK, lookup);
            }

        }
    }

    @Override
    public String toString() {
        return "FzModel " + name;
    }

    public static final ArrayList<String> extensions = new ArrayList<String>();
    static {
        extensions.add("obj");
        extensions.add("json");
    }
    protected ResourceLocation getLocation(IResourceManager irm) {
        String basename = "factorization:models/" + name;
        for (String ext : extensions) {
            String path = basename + "." + ext;
            ResourceLocation location = new ResourceLocation(path);
            try {
                irm.getResource(location);
                if (ext.equalsIgnoreCase("json")) {
                    return new ResourceLocation("factorization:" + name);
                }
                return new ResourceLocation("factorization:" + name + "." + ext);
            } catch (IOException e) {
                // ignored
            }
        }
        Core.logSevere("Failed to load model: " + basename);
        return null;
    }
}
