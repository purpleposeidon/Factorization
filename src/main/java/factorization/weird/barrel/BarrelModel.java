package factorization.weird.barrel;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.shared.FzIcons;
import factorization.shared.NORELEASE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.*;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import javax.vecmath.AxisAngle4f;
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
        final HashMap<ResourceLocation, TextureAtlasSprite> map = new HashMap<ResourceLocation, TextureAtlasSprite>();
        EnumWorldBlockLayer layer = MinecraftForgeClient.getRenderLayer();
        if (layer == EnumWorldBlockLayer.SOLID) {
            textures.put("log", log.getIconName());
            textures.put("plank", plank.getIconName());
        } else if (layer == EnumWorldBlockLayer.CUTOUT) {
            textures.put("top", top.getIconName());
        } else if (layer == EnumWorldBlockLayer.TRANSLUCENT) {
            textures.put("front", front.getIconName());
            textures.put("side", side.getIconName());
        }
        for (String s : new String[] { "log", "plank", "top", "front", "side"}) {
            if (textures.get(s) == null) {
                textures.put(s, "");
                textures.put("#" + s, "");
            }
        }
        textures.put("particle", log.getIconName());
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

        IModelState state = getMatrix(info.orientation);
        ImmutableMap<String, String> textureMap = ImmutableMap.copyOf(textures);
        IModel retexture = template.retexture(textureMap);
        return retexture.bake(state, DefaultVertexFormats.BLOCK, lookup);
    }

    private IModelState getMatrix(FzOrientation fzo) {
        Quaternion fzq = Quaternion.fromOrientation(fzo.getSwapped());
        javax.vecmath.Matrix4f trans = newMat();
        javax.vecmath.Matrix4f rot = newMat();
        javax.vecmath.Matrix4f r90 = newMat();

        r90.setRotation(new AxisAngle4f(0, 1, 0, (float) Math.PI / 2));

        trans.setTranslation(new javax.vecmath.Vector3f(0.5F, 0.5F, 0.5F));
        javax.vecmath.Matrix4f iTrans = new javax.vecmath.Matrix4f(trans);
        iTrans.invert();
        rot.setRotation(fzq.toJavax());
        rot.mul(r90);

        trans.mul(rot);
        trans.mul(iTrans);

        return new TRSRTransformation(trans);
    }

    private static javax.vecmath.Matrix4f newMat() {
        javax.vecmath.Matrix4f ret = new javax.vecmath.Matrix4f();
        ret.setIdentity();
        return ret;
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
