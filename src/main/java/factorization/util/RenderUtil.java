package factorization.util;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.vecmath.AxisAngle4f;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.lwjgl.opengl.GL11.glGetError;

public final class RenderUtil {
    @SideOnly(Side.CLIENT)
    public static void rotateForDirection(EnumFacing dir) {
        switch (dir) {
        case WEST:
            break;
        case EAST:
            GL11.glRotatef(180, 0, 1, 0);
            break;
        case NORTH:
            GL11.glRotatef(-90, 0, 1, 0);
            break;
        case SOUTH:
            GL11.glRotatef(90, 0, 1, 0);
            break;
        case UP:
            GL11.glRotatef(-90, 0, 0, 1);
            break;
        case DOWN:
            GL11.glRotatef(90, 0, 0, 1);
            break;
        default: break;
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean checkGLError(String op) {
        int errSym = glGetError();
        if (errSym != 0) {
            Core.logSevere("GL Error @ " + op);
            Core.logSevere(errSym + ": " + GLU.gluErrorString(errSym));
            return true;
        }
        return false;
    }

    public static TextureAtlasSprite getSprite(ItemStack log) {
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

    public static IBakedModel getModel(ModelResourceLocation name) {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(name);
    }

    public static IBakedModel getBakedModel(ItemStack is) {
        final Minecraft mc = Minecraft.getMinecraft();
        return mc.getRenderItem().getItemModelMesher().getItemModel(is);
    }

    public static IModel getModel(ItemStack is) {
        if (is == null) return null;
        ResourceLocation name = Item.itemRegistry.getNameForObject(is.getItem());
        try {
            return ModelLoaderRegistry.getModel(name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TRSRTransformation getMatrix(FzOrientation fzo) {
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

    public static javax.vecmath.Matrix4f newMat() {
        javax.vecmath.Matrix4f ret = new javax.vecmath.Matrix4f();
        ret.setIdentity();
        return ret;
    }

    /**
     * Classes that have this annotation may be have sprites recursively loaded.
     * @see RenderUtil#loadSprites(String, Object, String, TextureStitchEvent.Pre)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface LoadSprite { }

    /**
     * Use reflection to set all TextureAtlasSprites in a class.
     * All fields that are TextureAtlasSprites will be assigned, and any field whose type
     * is annotated with {@link LoadSprite} will be instantiated, assigned, and recursively loaded.
     * If a field name contains a '$', it will be replaced with a '/' for the icon's path name,
     * and also when recursion occurs.
     * @param domain    The domain of the textures.
     * @param toVisit   The object to visit. If it is a class, then its static fields will be loaded.
     * @param prefix    The path prefix. You likely want "".
     * @param event     Call this method during the TextureStitchEvent.Pre event.
     * @see LoadSprite
     * @throws RuntimeException on any error
     */
    public static void loadSprites(String domain, Object toVisit, String prefix, TextureStitchEvent.Pre event)  {
        Class base;
        Object instance;
        if (toVisit instanceof Class) {
            base = (Class) toVisit;
            instance = null;
        } else {
            base = toVisit.getClass();
            instance = toVisit;
        }
        try {
            load0(domain, base, instance, prefix, event);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void load0(String domain, Class base, Object instance, String prefix, TextureStitchEvent.Pre event) throws IllegalAccessException, InstantiationException {
        for (Field field : base.getFields()) {
            String name = field.getName().replace("$", "/");

            Class<?> type = field.getType();
            boolean staticField = (field.getModifiers() & Modifier.STATIC) != 0;
            boolean staticAccess = instance == null;
            if (staticField != staticAccess) continue;
            Object set;
            if (type == TextureAtlasSprite.class) {
                ResourceLocation location = new ResourceLocation(domain, prefix + name);
                set = event.map.registerSprite(location);
            } else if (canVisit(type)) {
                set = type.newInstance();
                load0(domain, type, set, prefix + name + "/", event);
            } else {
                continue;
            }
            field.set(instance, set);
        }
    }

    private static boolean canVisit(Class<?> type) {
        return type.getAnnotation(LoadSprite.class) != null;
    }

    public static void scale3(double s) {
        GL11.glScaled(s, s, s);
    }
}
