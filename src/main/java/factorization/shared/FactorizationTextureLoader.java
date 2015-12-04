package factorization.shared;


import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * @author neptunepink
 * This uses reflection to register IIcons from a class. The fields give the names of the icons.
 * If a field extends IIconGroup, then IIcon registering will recurse into it.
 * This is useless for Items/Blocks that use a single icon, but quite helpful everywhere else. 
 *
 */
@SideOnly(Side.CLIENT)
public class FactorizationTextureLoader {
    /**
     * This annotation indicates that the IIcon is in a subdirectory. The following are equivalent, and will cause the icon to be registered as "servo/catGrabber"
     * <pre>
     * IIcon servo$catGrabber;
     * @<b></b>Directory("servo")
     * IIcon catGrabber;
     * </pre>
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Directory {
        String value();
    }
    
    /** IIcons with this annotation will not be registered.
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Ignore { }
    
    public abstract static class IIconGroup {
        public String group_prefix;
        public IIconGroup prefix(String prefix) {
            this.group_prefix = prefix + "_";
            return this;
        }
        public void afterRegister() { }
    }
    
    public static void register(IIconRegister reg, Class base, Object instance, String base_prefix) {
        try {
            Field[] fields = base.getFields();
            for (Field f : fields) {
                String prefix = base_prefix;
                
                if (f.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                
                Directory dir = f.getAnnotation(Directory.class);
                if (dir != null) {
                    prefix += dir.value() + "/";
                }
                
                if (IIconGroup.class.isAssignableFrom(f.getType())) {
                    IIconGroup ig = (IIconGroup) f.get(instance);
                    if (ig == null) {
                        ig = (IIconGroup) f.getType().newInstance();
                    }
                    ig.prefix(f.getName());
                    f.set(instance, ig);
                    register(reg, f.getType(), ig, prefix + ig.group_prefix);
                    ig.afterRegister();
                }
                if (IIcon.class.isAssignableFrom(f.getType())) {
                    String icon_file = prefix + f.getName();
                    f.set(instance, reg.registerIcon(icon_file.replace('$', '/')));
                }
                if (IIcon[].class.isAssignableFrom(f.getType())) {
                    IIcon[] array = (IIcon[]) f.get(base);
                    for (int i = 0; i < array.length; i++) {
                        String icon_file = prefix + f.getName() + i;
                        array[i] = reg.registerIcon(icon_file.replace('$', '/'));
                    }
                } else if (f.getType().isArray()) {
                    Object theArray = f.get(base);
                    int len = Array.getLength(theArray);
                    Class<?> componentType = theArray.getClass().getComponentType();
                    for (int i = 0; i < len; i++) {
                        Object val = Array.get(theArray, i);
                        register(reg, componentType, val, prefix + i);
                    }
                }
            }
        } catch (Exception e) {
            // Sigh.
            throw new IllegalArgumentException(e);
        }
    }
}
