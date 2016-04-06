package factorization.sockets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import factorization.shared.FzModel;
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
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.client.model.pipeline.VertexTransformer;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SocketModel implements ISmartItemModel, ISmartBlockModel, IPerspectiveAwareModel {
    private final boolean isItem;
    private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms;

    public static FzModel base = new FzModel(new ResourceLocation("factorization:block/socket"));

    public SocketModel(boolean isItem) {
        this.isItem = isItem;
        transforms = IPerspectiveAwareModel.MapWrapper.getTransforms(ItemCameraTransforms.DEFAULT);
    }

    final TileEntitySocketBase repr = new SocketEmpty();
    @Override
    public IBakedModel handleItemState(ItemStack stack) {
        repr.loadFromStack(stack);
        return get(SocketCacheInfo.from(repr));
    }

    @Override
    public Pair<? extends IFlexibleBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, transforms, cameraTransformType);
    }

    @Override
    public VertexFormat getFormat() {
        return isItem ? DefaultVertexFormats.ITEM : DefaultVertexFormats.BLOCK;
    }

    private final HashMap<SocketCacheInfo, IBakedModel> modelCache = new HashMap<SocketCacheInfo, IBakedModel>();

    IBakedModel get(SocketCacheInfo info) {
        IBakedModel ret = modelCache.get(info);
        if (ret != null) return ret;
        ret = build(info);
        if (NORELEASE.disabledUntilRelease) {
            modelCache.put(info, ret);
        }
        return ret;
    }

    IBakedModel build(SocketCacheInfo info) {
        TextureAtlasSprite particle = null;
        List<BakedQuad> general = Lists.newArrayList();
        List<List<BakedQuad>> sided = Lists.newArrayList();
        for (EnumFacing face : EnumFacing.VALUES) {
            sided.add(Lists.newArrayList());
        }

        QuadTransformer transform;
        if (info.facing == null) {
            transform = new QuadTransformer(TRSRTransformation.identity(), getFormat());
        } else {
            TRSRTransformation trsrt;
            switch (info.facing) {
                default:
                case DOWN: trsrt = TRSRTransformation.identity(); break;
                case UP: trsrt = new TRSRTransformation(ModelRotation.X180_Y0); break;
                case NORTH: trsrt = new TRSRTransformation(ModelRotation.X90_Y180); break;
                case SOUTH: trsrt = new TRSRTransformation(ModelRotation.X90_Y0); break;
                case WEST: trsrt = new TRSRTransformation(ModelRotation.X90_Y90); break;
                case EAST: trsrt = new TRSRTransformation(ModelRotation.X90_Y270); break;
            }
            transform = new QuadTransformer(trsrt, getFormat());
        }
        for (IBakedModel model : info.parts) {
            if (model == null) continue;
            if (particle == null) {
                particle = model.getParticleTexture();
            }
            for (BakedQuad quad : model.getGeneralQuads()) {
                general.add(transform.apply(quad));
            }
            for (EnumFacing face : EnumFacing.VALUES) {
                for (BakedQuad quad : model.getFaceQuads(face)) {
                    general.add(transform.apply(quad));
                }
            }
        }
        if (particle == null) {
            particle = getParticleTexture();
        }
        ItemCameraTransforms camera = RenderUtil.getBakedModel(new ItemStack(Blocks.stone)).getItemCameraTransforms();
        return new SimpleBakedModel(general, sided, false /* ao */, true /* gui 3D */, particle, camera);
    }

    public static class QuadTransformer {
        Impl inst; // FIXME: UnpackedBaekdQuad.Builder can only be used once per quad
        public QuadTransformer(TRSRTransformation transform, VertexFormat format) {
            inst = new Impl(transform, format);
        }

        public BakedQuad apply(BakedQuad quad) {
            BakedQuad ret = inst.apply(quad);
            inst = new Impl(inst);
            return ret;
        }

        private static class Impl extends VertexTransformer {
            // Thankyou fry! https://gist.github.com/RainWarrior/b1d5772f710cd39f4740
            final Matrix4f transform;
            final Matrix3f invTranslate;
            final UnpackedBakedQuad.Builder builder;
            final VertexFormat format;

            private Impl(TRSRTransformation transform, VertexFormat format) {
                super(new UnpackedBakedQuad.Builder(format));
                this.transform = transform.getMatrix();
                Vector3f vtr = transform.getTranslation();
                this.invTranslate = new Matrix3f();
                this.invTranslate.transform(vtr);
                this.invTranslate.setIdentity();
                this.builder = (UnpackedBakedQuad.Builder) super.parent;
                this.format = format;
            }

            private Impl(Impl orig) {
                super(new UnpackedBakedQuad.Builder(orig.format));
                this.format = orig.format;
                this.builder = (UnpackedBakedQuad.Builder) super.parent;
                this.transform = orig.transform;
                this.invTranslate = orig.invTranslate;
            }

            public BakedQuad apply(BakedQuad quad) {
                quad.pipe(this);
                return builder.build();
            }

            final float[] newData = new float[4];

            @Override
            public void put(int element, float... data) {
                VertexFormatElement el = format.getElement(element);
                switch (el.getUsage()) {
                    case POSITION: {
                        Vector4f vec = new Vector4f(data);
                        vec.w = 1;
                        transform.transform(vec);
                        vec.get(newData);
                        parent.put(element, newData);
                        break;
                    }
                    case NORMAL: {
                        parent.put(element, data);
                        /*Vector4f vec = new Vector4f(data);
                        vec.w = 0;
                        transform.transform(vec);
                        vec.w = 0;
                        vec.get(newData);
                        parent.put(element, newData);*/
                        break;
                    }
                    default:
                        parent.put(element, data);
                        break;
                }
            }

        }
    }

    @Override
    public IBakedModel handleBlockState(IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState bs = (IExtendedBlockState) state;
            SocketCacheInfo info = bs.getValue(BlockSocket.SOCKET_INFO);
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
        //return RenderUtil.getBakedModel(Core.registry.empty_socket_item).getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
        //return RenderUtil.getBakedModel(Core.registry.empty_socket_item).getItemCameraTransforms();
    }
}
