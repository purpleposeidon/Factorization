package factorization.beauty;

import factorization.shared.FzModel;
import factorization.util.FzUtil;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
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
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.ISmartItemModel;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import javax.annotation.Nullable;
import java.util.*;

public class ShaftModel implements ISmartItemModel {
    final HashMap<ShaftItemCache, IBakedModel> modelCache = new HashMap<ShaftItemCache, IBakedModel>();
    static FzModel template_sheared = new FzModel("beauty/shaft_overlay_sheared", false, FzModel.FORMAT_ITEM);
    static FzModel template = new FzModel("beauty/shaft_overlay", false, FzModel.FORMAT_ITEM);

    ShaftItemCache buildCacheKey(ItemStack stack) {
        return new ShaftItemCache(stack);
    }

    @Override
    public IBakedModel handleItemState(ItemStack stack) {
        ShaftItemCache cacheKey = buildCacheKey(stack);
        IBakedModel ret = modelCache.get(cacheKey);
        if (NORELEASE.on) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.thePlayer.isSneaking() && stack == mc.thePlayer.getHeldItem()) {
                ret = null;
            }
        }
        if (ret != null) return ret;
        ret = build(cacheKey);
        modelCache.put(cacheKey, ret);
        return ret;
    }

    IBakedModel build(ShaftItemCache info) {
        IBakedModel log = FzUtil.getModel(info.log);
        Shrink shrinker = new Shrink(log);
        shrinker.apply();
        IBakedModel mod = (info.sheared ? template_sheared : template).model;
        //IBakedModel mod = template.model;
        //IBakedModel mod = (info.sheared ? TileEntitySteamShaftRenderer.whirligig : template).model;
        shrinker.quads.addAll(mod.getGeneralQuads());
        for (EnumFacing face : EnumFacing.VALUES) {
            shrinker.quads.addAll(mod.getFaceQuads(face));
        }
        return shrinker.bake();
    }

    public static class Shrink extends ItemModelTransformer {
        Shrink(IBakedModel log) {
            super(log);
        }

        static float shrink(float xi) {
            float mid = 0.5F;
            float r = mid + (xi - mid) / 4;
            return r;
        }

        static Vert sample(Vert x0, Vert x1, Vert s) {
            // Sample the texture coordinates at position s between x0 and x1
            // x0 & x1 are like the two points in the 'linear gradient' tool in GIMP w/ black & white.
            Vert ret = new Vert();
            ret.x = s.x;
            ret.y = s.y;
            ret.z = s.z;
            float p = (float) NumUtil.uninterpGradient(x0.vec(), x1.vec(), s.vec());
            ret.u = NumUtil.interp(x0.u, x1.u, p);
            ret.v = NumUtil.interp(x0.v, x1.v, p);
            return ret;
        }


        @Override
        protected void convert() {
            // We're cutting the quad into a smaller one by interpolating vertices towards eachother in a particular way.
            // Basically, we scale X & Z towards the center.
            for (int i = 1; i <= 4; i++) {
                repl[i].x = shrink(vert[i].x);
                repl[i].y = vert[i].y;
                repl[i].z = shrink(vert[i].z);
                repl[i].u = vert[i].u;
                repl[i].v = vert[i].v;
            }
            // This part is super tricky.
            // We've got two neighbors, at vert[i±1].
            for (int i = 1; i <= 4; i++) {
                Vert left = vert[i - 1];
                Vert orig = vert[i];
                Vert right = vert[i + 1];
                Vert moved = repl[i];
                Vert movedLeft = sample(orig, left, moved);
                Vert movedRight = sample(orig, right, moved);
                double du = (movedLeft.u - orig.u) + (movedRight.u - orig.u);
                double dv = (movedLeft.v - orig.v) + (movedRight.v - orig.v);
                moved.u += du;
                moved.v += dv;
            }
        }
    }

    public abstract static class ItemModelTransformer extends UselesslyGenericModelTransformer {
        ItemModelTransformer(IBakedModel model) {
            super(model, DefaultVertexFormats.ITEM);
        }

        @Override
        protected final int getPasses() {
            return 2;
        }

        public static class Vert {
            public float x, y, z, u, v;

            public Vec3 vec() {
                return new Vec3(x, y, z);
            }
        }

        // This array wraps around: we iterate from [1, 4] inclusive, allowing i±1 to be in bounds for getting the neighbor
        protected Vert[] vert = new Vert[6], repl = new Vert[6];
        {
            for (int i = 0; i < 4; i++) {
                vert[i] = new Vert();
                repl[i] = new Vert();
            }
            vert[4] = vert[0];
            repl[4] = repl[0];
            vert[5] = vert[1];
            repl[5] = repl[1];
        }

        private Vert get() {
            return vert[vertex];
        }

        private Vert got() {
            return repl[vertex];
        }

        @Override
        protected final void pos(float x, float y, float z) {
            if (pass == 0) {
                Vert vert = get();
                vert.x = x;
                vert.y = y;
                vert.z = z;
                return;
            }
            Vert vert = got();
            super.pos(vert.x, vert.y, vert.z);
        }

        @Override
        protected final void tex(float u, float v) {
            if (pass == 0) {
                Vert vert = get();
                vert.u = u;
                vert.v = v;
                return;
            }
            Vert vert = got();
            super.tex(vert.u, vert.v);
        }

        @Override
        protected void flushQuad() {
            super.flushQuad();
            if (pass != 0) return;
            convert();
        }

        protected abstract void convert();
    }

    public static class UselesslyGenericModelTransformer implements IVertexConsumer {
        public final ArrayList<BakedQuad> quads = new ArrayList<BakedQuad>();
        protected final IBakedModel source;
        protected final VertexFormat format;
        private UnpackedBakedQuad.Builder bakery;

        public UselesslyGenericModelTransformer(IBakedModel source, VertexFormat format) {
            this.source = source;
            this.format = format;
            reset();
        }

        public void apply() {
            apply(source);
        }

        public void apply(IBakedModel source) {
            quadSide = null;
            for (BakedQuad quad : source.getGeneralQuads()) {
                visit(quad);
            }
            for (EnumFacing face : EnumFacing.VALUES) {
                quadSide = face;
                for (BakedQuad quad : source.getFaceQuads(face)) {
                    visit(quad);
                }
            }
        }

        public IBakedModel bake() {
            return bake(source);
        }

        public IBakedModel bake(IBakedModel source) {
            return new SimpleBakedModel(quads, empty_faces, false, true, source.getParticleTexture(), source.getItemCameraTransforms());
        }

        protected void visit(BakedQuad quad) {
            int passes = getPasses();
            for (int i = 0; i < passes; i++) {
                pass = i;
                quad.pipe(this);
                if (i + 1 != passes) dropQuad = true;
                flushQuad();
            }
        }

        protected int getPasses() {
            return 1;
        }

        private static final List<List<BakedQuad>> empty_faces = Arrays.asList(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());

        @Override
        public VertexFormat getVertexFormat() {
            return format;
        }

        /** The face (from getFaceQuads/getGeneralQuads) that the currently processed face is from.
         * NOTE: All quads are General in the output; modifying this changes nothing. */
        @Nullable
        protected EnumFacing quadSide = null;

        protected int pass;
        protected int tint;
        protected EnumFacing orientation;
        protected boolean colored;
        protected boolean dropQuad;
        protected int vertex = -1;

        protected void resetPass() {
            vertex = -1;
        }

        protected void reset() {
            tint  = -1;
            orientation = null;
            colored = false;
            bakery = new UnpackedBakedQuad.Builder(format);
            dropQuad = false;
            resetPass();
        }

        protected void flushQuad() {
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
            if (elementIndex == 0) {
                vertex++;
            }
            /** @see net.minecraftforge.client.model.obj.OBJModel.OBJBakedModel#putVertexData */
            if (element == DefaultVertexFormats.POSITION_3F) {
                pos(data[0], data[1], data[2]);
            } else if (element == DefaultVertexFormats.COLOR_4UB) {
                color((char) data[0], (char) data[1], (char) data[2], (char) data[3]);
            } else if (element == DefaultVertexFormats.TEX_2F) {
                tex(data[0], data[1]);
            } else if (element == DefaultVertexFormats.NORMAL_3B) {
                normal((byte) data[0], (byte) data[1], (byte) data[2]);
            } else {
                bakery.put(ei, data);
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
