package factorization.shared;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
     * IIcon servo$catGrabber;
     * @Directory("servo")
     * IIcon catGrabber;
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Directory {
        public String value();
    }
    
    /** IIcons with this annotation will not be registered.
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Ignore { };
    
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
            }
        } catch (Exception e) {
            // Sigh.
            throw new IllegalArgumentException(e);
        }
    }
}
