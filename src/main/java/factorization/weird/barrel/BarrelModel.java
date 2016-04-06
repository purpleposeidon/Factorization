package factorization.weird.barrel;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import factorization.util.NORELEASE;
import factorization.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.*;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarrelModel implements ISmartBlockModel, ISmartItemModel, IPerspectiveAwareModel {
    public static BarrelGroup normal, hopping, silky, sticky;
    public static TextureAtlasSprite font;

    private final boolean isItem;
    private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms;

    public BarrelModel(boolean isItem) {
        this.isItem = isItem;
        transforms = IPerspectiveAwareModel.MapWrapper.getTransforms(ItemCameraTransforms.DEFAULT);
    }

    @Override
    public IBakedModel handleItemState(ItemStack stack) {
        return get(BarrelCacheInfo.from(stack));
    }

    @Override
    public Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, transforms, cameraTransformType);
    }

    @Override
    public VertexFormat getFormat() {
        return isItem ? DefaultVertexFormats.ITEM : DefaultVertexFormats.BLOCK;
    }

    @RenderUtil.LoadSprite
    public static class BarrelGroup {
        public TextureAtlasSprite front, top, side, top_metal;
    }

    private final List<Map<BarrelCacheInfo, IBakedModel>> modelCache = Lists.newArrayList();
    {
        for (EnumWorldBlockLayer layer : EnumWorldBlockLayer.values()) {
            modelCache.add(Maps.newHashMap());
        }
        modelCache.add(Maps.newHashMap()); // null
    }
    private static final int NULL_INDEX = EnumWorldBlockLayer.values().length;
    public static IRetexturableModel template;

    IBakedModel get(BarrelCacheInfo info) {
        EnumWorldBlockLayer layer = isItem ? null : MinecraftForgeClient.getRenderLayer();
        int layerIndex = layer == null ? NULL_INDEX : layer.ordinal();
        Map<BarrelCacheInfo, IBakedModel> perLayer = modelCache.get(layerIndex);
        IBakedModel ret = perLayer.get(info);
        if (ret != null) return ret;
        ret = build(info, layer);
        if (NORELEASE.off) { // delete this if statement
            perLayer.put(info, ret);
        }
        return ret;
    }

    IBakedModel build(BarrelCacheInfo info, EnumWorldBlockLayer layer) {
        TextureAtlasSprite log = info.log;
        TextureAtlasSprite plank = info.plank;
        BarrelGroup group;
        TileEntityDayBarrel.Type type = info.type;
        if (type.isHopping()) {
            group = hopping;
        } else if (type == TileEntityDayBarrel.Type.SILKY) {
            group = silky;
        } else if (type == TileEntityDayBarrel.Type.STICKY) {
            group = sticky;
        } else {
            group = normal;
        }
        TextureAtlasSprite top = info.isMetal ? group.top_metal : group.top;
        TextureAtlasSprite front = group.front;
        TextureAtlasSprite side = group.side;
        HashMap<String, String> textures = new HashMap<String, String>();
        final HashMap<ResourceLocation, TextureAtlasSprite> map = new HashMap<ResourceLocation, TextureAtlasSprite>();
        if (isItem || layer == EnumWorldBlockLayer.SOLID) {
            textures.put("log", log.getIconName());
            textures.put("plank", plank.getIconName());
        }
        if (isItem || layer == EnumWorldBlockLayer.TRANSLUCENT) {
            textures.put("top", top.getIconName());
            textures.put("front", front.getIconName());
            textures.put("side", side.getIconName());
        }
        for (String s : new String[]{"log", "plank", "top", "front", "side"}) {
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

        IModelState state = RenderUtil.getMatrix(info.orientation);
        ImmutableMap<String, String> textureMap = ImmutableMap.copyOf(textures);
        IModel retexture = template.retexture(textureMap);
        final IFlexibleBakedModel bake = retexture.bake(state, DefaultVertexFormats.BLOCK, lookup);
        return new IFlexibleBakedModel.Wrapper(bake, getFormat()) {
            @Override
            public ItemCameraTransforms getItemCameraTransforms() {
                return BarrelModel.this.getItemCameraTransforms();
            }
        };
    }

    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState bs = (IExtendedBlockState) state;
            BarrelCacheInfo info = bs.getValue(BlockBarrel.BARREL_INFO);
            return get(info);
        }

        Block use = Blocks.stone;
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(use.getDefaultState());
    }

    // Mostly unused boilerplate
    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing face) {
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return Collections.emptyList();
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
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.planks.getDefaultState());
    }

    ItemCameraTransforms itemCameraTransforms;
    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        if (itemCameraTransforms == null) {
            itemCameraTransforms = RenderUtil.getBakedModel(new ItemStack(Blocks.stone)).getItemCameraTransforms();
        }
        return itemCameraTransforms;
    }
}
