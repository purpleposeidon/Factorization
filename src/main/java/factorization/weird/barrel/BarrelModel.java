package factorization.weird.barrel;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import factorization.idiocy.WrappedItemStack;
import factorization.shared.FzIcons;
import factorization.shared.NORELEASE;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelState;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BarrelModel implements ISmartBlockModel {
    HashMap<Pair<ItemStack, ItemStack>, IBakedModel> modelCache = new HashMap<Pair<ItemStack, ItemStack>, IBakedModel>();
    public static IRetexturableModel template;

    IBakedModel get(ItemStack log, ItemStack slab) {
        Pair<ItemStack, ItemStack> p = Pair.of(log, slab);
        IBakedModel ret = modelCache.get(p);
        if (ret != null) return ret;
        ret = build(log, slab);
        return ret;
    }

    IBakedModel build(ItemStack logItem, ItemStack slabItem) {
        TextureAtlasSprite log = getSprite(logItem);
        TextureAtlasSprite plank = getSprite(slabItem);
        TextureAtlasSprite top = isMetal(slabItem) ? FzIcons.block$storage$normal$top_metal : FzIcons.block$storage$normal$top;
        TextureAtlasSprite front = FzIcons.block$storage$normal$front;
        TextureAtlasSprite side = FzIcons.block$storage$normal$side;
        TextureAtlasSprite glass = getSprite(new ItemStack(Blocks.glass));
        NORELEASE.println(logItem, log);
        top = front = side = glass;
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
        IModelState state = ModelRotation.X0_Y0;
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

    private boolean isMetal(ItemStack it) {
        if (it == null) return true;
        Block block = DataUtil.getBlock(it);
        return block == null || !block.getMaterial().getCanBurn();
    }

    private TextureAtlasSprite getSprite(ItemStack log) {
        Minecraft mc = Minecraft.getMinecraft();
        Block b = DataUtil.getBlock(log);
        if (b == null) {
            ItemModelMesher itemModelMesher = mc.getRenderItem().getItemModelMesher();
            if (log == null) return itemModelMesher.getItemModel(null).getParticleTexture();
            return itemModelMesher.getParticleIcon(log.getItem());
        }
        IBlockState bs = b.getStateFromMeta(log.getItemDamage());
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(bs);
    }

    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        Block use = null;
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState bs = (IExtendedBlockState) state;
            WrappedItemStack log = bs.getValue(BlockBarrel.BARREL_LOG);
            WrappedItemStack slab = bs.getValue(BlockBarrel.BARREL_SLAB);
            return get(log.stack, slab.stack);
        }
        if (use == null) use = Blocks.stone;
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
