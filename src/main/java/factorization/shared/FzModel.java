package factorization.shared;

import com.google.common.base.Function;
import factorization.util.NORELEASE;
import factorization.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class FzModel {
    // Don't SideOnly this class.

    public final ResourceLocation url;
    @SideOnly(Side.CLIENT)
    public IBakedModel model;
    public final boolean blend;
    private final int format_id;

    public static final int FORMAT_BLOCK = 1, FORMAT_ITEM = 2;

    public FzModel(String name) {
        // Really ought to use a factory method instead.
        this(name, false, FORMAT_BLOCK);
    }

    public FzModel(String name, boolean blend) {
        this(name, blend, FORMAT_BLOCK);
    }

    public FzModel(String name, boolean blend, int format_id) {
        this(new ResourceLocation("factorization:fzmodel/" + name), blend, format_id);
    }

    public FzModel(ResourceLocation url) {
        this(url, false, FORMAT_BLOCK);
    }

    public FzModel(ResourceLocation url, boolean blend, int format_id) {
        this.url = url;
        this.blend = blend;
        this.format_id = format_id;
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT) return;
        instances.add(this);
        Core.logInfo("Added FzModel: " + url);
    }

    private static final ArrayList<FzModel> instances = new ArrayList<FzModel>();
    static {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new ModelWrangler());
        }
    }

    @SideOnly(Side.CLIENT)
    public VertexFormat getFormat() {
        if (format_id == FORMAT_BLOCK) return DefaultVertexFormats.BLOCK;
        if (format_id == FORMAT_ITEM) return DefaultVertexFormats.ITEM;
        return null;
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

        boolean blendAnyways = (colorARGB & 0xFF000000) != 0xFF000000;
        if (blend || blendAnyways) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
            // This state is inconsistently enabled, and this is what it's
            // "supposed" to be, so not re-enabling.
        }
        GlStateManager.disableLighting();
        tessI.draw();
        GlStateManager.enableLighting();
        if (blend) {
            GlStateManager.disableBlend();
        }
        RenderUtil.checkGLError("model draw");
    }

    @SideOnly(Side.CLIENT)
    public void putQuads(WorldRenderer tess, List<BakedQuad> quads, int color) {
        for (BakedQuad quad : quads) {
            tess.addVertexData(quad.getVertexData());
            //tess.putColor4(color);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ModelWrangler {
        public static boolean hasLoaded = false;
        IdentityHashMap<FzModel, IModel> raws = new IdentityHashMap<FzModel, IModel>();
        @SubscribeEvent
        public void loadModels(TextureStitchEvent.Pre event) {
            raws.clear();
            //event.modelBakery.reg
            Minecraft mc = Minecraft.getMinecraft();

            IResourceManager irm = mc.getResourceManager();
            HashSet<ResourceLocation> textures = new HashSet<ResourceLocation>();
            for (FzModel fzm : instances) {
                fzm.model = null;
                ResourceLocation location = fzm.getLocation(irm);
                IModel rawModel = null;
                try {
                    if (location != null) {
                        rawModel = ModelLoaderRegistry.getModel(location);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (rawModel == null) {
                    rawModel = ModelLoaderRegistry.getMissingModel();
                }
                raws.put(fzm, rawModel);
                Collection<ResourceLocation> modelTextures = rawModel.getTextures();
                textures.addAll(modelTextures);
            }
            for (ResourceLocation texture : textures) {
                event.map.registerSprite(texture);
            }
        }

        @SubscribeEvent
        public void bakeModels(TextureStitchEvent.Post event) {
            final TextureMap map = event.map;
            Function<ResourceLocation,TextureAtlasSprite> lookup = new Function<ResourceLocation, TextureAtlasSprite>() {
                @Nullable
                @Override
                public TextureAtlasSprite apply(@Nullable ResourceLocation input) {
                    if (input == null) return map.getAtlasSprite(null);
                    return map.getAtlasSprite(input.toString());
                }
            };
            for (FzModel fzm : instances) {
                IModel rawModel = raws.get(fzm);
                if (rawModel == null) {
                    fzm.model = null;
                    continue;
                }
                fzm.model = rawModel.bake(fzm.trsrt, fzm.getFormat(), lookup);
            }
            raws.clear();
        }
    }

    public TRSRTransformation trsrt = new TRSRTransformation(null, null, null, null);

    @Override
    public String toString() {
        return "FzModel(" + url + ")";
    }

    public static final ArrayList<String> extensions = new ArrayList<String>();
    static {
        extensions.add("obj");
        extensions.add("json");
    }
    protected ResourceLocation getLocation(IResourceManager irm) {
        IOException lastError = null;
        for (String ext : extensions) {
            try {
                ResourceLocation testLocation = new ResourceLocation(url.getResourceDomain(), "models/" + url.getResourcePath() + "." + ext);
                irm.getResource(testLocation);
                if (ext.equalsIgnoreCase("json")) {
                    return url;
                }
                return new ResourceLocation(url.getResourceDomain(), url.getResourcePath() + "." + ext);
            } catch (IOException e) {
                lastError = e;
            }
        }
        Core.logSevere("Failed to load model: " + url);
        if (lastError != null) {
            lastError.printStackTrace();
        }
        return null;
    }
}
