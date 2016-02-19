package factorization.beauty;

import factorization.util.FzUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ShaftModel implements ISmartItemModel {
    final HashMap<ItemStack, IBakedModel> modelCache = new HashMap<ItemStack, IBakedModel>();
    public static IRetexturableModel template;

    ItemStack buildCacheKey(ItemStack stack) {
        return stack;
    }

    @Override
    public IBakedModel handleItemState(ItemStack stack) {
        ItemStack cacheKey = buildCacheKey(stack);
        IBakedModel ret = modelCache.get(cacheKey);
        if (ret != null) return ret;
        ret = build(cacheKey);
        modelCache.put(cacheKey, ret);
        return ret;
    }

    IBakedModel build(ItemStack logStack) {
        IBakedModel log = FzUtil.getModel(logStack);
        ModelTransformer pipe = new Shrink(log, DefaultVertexFormats.ITEM);
        return pipe.apply();
    }

    public static class Shrink extends ModelTransformer {
        Shrink(IBakedModel log, VertexFormat format) {
            super(log, format);
        }

        static float shrink(float xi) {
            float mid = 0.5F;
            return mid + (xi - mid) / 4;
        }

        @Override
        protected void pos(float x, float y, float z) {
            super.pos(shrink(x), y, shrink(z));
        }

        @Override
        protected void tex(float u, float v) {
            if (quadSide == null || quadSide.getAxis() != EnumFacing.Axis.Y) {
                super.tex(shrink(u), shrink(v));
            } else {
                super.tex(u, v);
            }
        }
    }

    public static class ModelTransformer implements IVertexConsumer {
        protected final ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
        protected final IBakedModel source;
        protected final VertexFormat format;
        private UnpackedBakedQuad.Builder bakery;

        ModelTransformer(IBakedModel source, VertexFormat format) {
            this.source = source;
            this.format = format;
            reset();
        }

        public IBakedModel apply() {
            quadSide = null;
            for (BakedQuad quad : source.getGeneralQuads()) {
                quad.pipe(this);
                flush();
            }
            for (EnumFacing face : EnumFacing.VALUES) {
                quadSide = face;
                for (BakedQuad quad : source.getFaceQuads(face)) {
                    quad.pipe(this);
                    flush();
                }
            }
            return new SimpleBakedModel(quads, Collections.emptyList(), false, true, source.getParticleTexture(), source.getItemCameraTransforms());
        }

        @Override
        public VertexFormat getVertexFormat() {
            return format;
        }

        /** The face (from getFaceQuads/getGeneralQuads) that the currently processed face is from.
         * NOTE: All quads are General in the output; modifying this changes nothing. */
        @Nullable
        protected EnumFacing quadSide = null;

        protected int tint;
        protected EnumFacing orientation;
        protected boolean colored;
        protected boolean dropQuad;


        protected void reset() {
            tint  = -1;
            orientation = null;
            colored = false;
            bakery = new UnpackedBakedQuad.Builder(format);
            dropQuad = false;
        }

        protected void flush() {
            if (tint != -1) bakery.setQuadTint(tint);
            if (orientation != null) bakery.setQuadOrientation(orientation);
            if (colored) bakery.setQuadColored();
            if (!dropQuad) quads.add(bakery.build());
            reset();
        }

        @Override
        public void setQuadTint(int tint) {
            this.tint = tint;
        }

        @Override
        public void setQuadOrientation(EnumFacing orientation) {
            this.orientation = orientation;
        }

        @Override
        public void setQuadColored() {
            colored = true;
        }

        private int ei;
        @Override
        public void put(int elementIndex, float... data) {
            ei = elementIndex;
            VertexFormatElement element = getVertexFormat().getElement(elementIndex);
            /** @see net.minecraftforge.client.model.obj.OBJModel.OBJBakedModel#putVertexData */
            if (element == DefaultVertexFormats.POSITION_3F) {
                pos(data[0], data[1], data[2]);
            } else if (element == DefaultVertexFormats.COLOR_4UB) {
                color((char) data[0], (char) data[1], (char) data[2], (char) data[3]);
            } else if (element == DefaultVertexFormats.TEX_2F) {
                tex(data[0], data[1]);
            } else if (element == DefaultVertexFormats.NORMAL_3B) {
                normal((byte) data[0], (byte) data[1], (byte) data[2]);
            }
        }

        protected void normal(byte x, byte y, byte z) {
            bakery.put(ei, x, y, z);
        }

        protected void tex(float u, float v) {
            bakery.put(ei, u, v);
        }

        protected void color(char r, char g, char b, char a) {
            bakery.put(ei, r, g, b, a);
        }

        /**
         * This is what you came here for. These 4 methods will be called in the order described by format.
         * To manipulate, override this methods, and then call super.pos()
         */
        protected void pos(float x, float y, float z) {
            bakery.put(ei, x, y, z);
        }
    }


    // Boilerplate

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing facing) {
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
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

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }
}
