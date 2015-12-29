package factorization.weird.barrel;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import factorization.api.Quaternion;
import factorization.shared.FzIcons;
import factorization.shared.NORELEASE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BarrelModel implements ISmartBlockModel {

    private final HashMap<CacheInfo, IBakedModel> modelCache = new HashMap<CacheInfo, IBakedModel>();
    public static IRetexturableModel template;

    IBakedModel get(CacheInfo info) {
        IBakedModel ret = modelCache.get(info);
        if (ret != null) return ret;
        ret = build(info);
        if (NORELEASE.off) { // delete this if statement
            modelCache.put(info, ret);
        }
        return ret;
    }

    IBakedModel build(CacheInfo info) {
        TextureAtlasSprite log = info.log;
        TextureAtlasSprite plank = info.plank;
        TextureAtlasSprite top = info.isMetal ? FzIcons.blocks$storage$normal$top_metal : FzIcons.blocks$storage$normal$top;
        TextureAtlasSprite front = FzIcons.blocks$storage$normal$front;
        TextureAtlasSprite side = FzIcons.blocks$storage$normal$side;
        HashMap<String, String> textures = new HashMap<String, String>();
        textures.put("log", log.getIconName());
        textures.put("plank", plank.getIconName());
        textures.put("top", top.getIconName());
        textures.put("front", front.getIconName());
        textures.put("side", side.getIconName());
        final HashMap<ResourceLocation, TextureAtlasSprite> map = new HashMap<ResourceLocation, TextureAtlasSprite>();
        map.put(new ResourceLocation(log.getIconName()), log);
        map.put(new ResourceLocation(plank.getIconName()), plank);
        map.put(new ResourceLocation(top.getIconName()), top);
        map.put(new ResourceLocation(front.getIconName()), front);
        map.put(new ResourceLocation(side.getIconName()), side);
        Function<ResourceLocation, TextureAtlasSprite> lookup = new Function<ResourceLocation, TextureAtlasSprite>() {
            @Nullable
            @Override
            public TextureAtlasSprite apply(@Nullable ResourceLocation input) {
                return map.get(input);
            }
        };
        Quaternion fzq = Quaternion.fromOrientation(info.orientation);
        NORELEASE.println(info.orientation);
        javax.vecmath.Matrix4f mat = TRSRTransformation.mul(null, null, null, fzq.toJavax());
        IModelState state = new TRSRTransformation(mat);
        return template.retexture(ImmutableMap.copyOf(textures)).bake(state, DefaultVertexFormats.BLOCK, lookup);
    }

    ModelBlock loadTemplate() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            IResourceManager rm = mc.getResourceManager();
            IResource iresource = rm.getResource(new ModelResourceLocation("factorization:block/barrel_template"));
            InputStreamReader reader = new InputStreamReader(iresource.getInputStream(), Charsets.UTF_8);
            return ModelBlock.deserialize(reader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState bs = (IExtendedBlockState) state;
            CacheInfo info = bs.getValue(BlockBarrel.BARREL_INFO);
            return get(info);
        }

        Block use = Blocks.stone;
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(use.getDefaultState());
    }

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing face) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.planks.getDefaultState());
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }
}
