package factorization.shared;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author neptunepink
 * This uses reflection to register Icons from a class. The fields give the names of the icons.
 * If a field extends IconGroup, then Icon registering will recurse into it.
 * This is useless for Items/Blocks that use a single icon, but quite helpful everywhere else. 
 *
 */
@SideOnly(Side.CLIENT)
public class FactorizationTextureLoader {
    /**
     * This annotation indicates that the Icon is in a subdirectory. The following are equivalent, and will cause the icon to be registered as "servo/catGrabber"
     * Icon servo$catGrabber;
     * @Directory("servo")
     * Icon catGrabber;
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Directory {
        public String value();
    }
    
    /** Icons with this annotation will not be registered.
     */
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Ignore { };
    
    public abstract static class IconGroup {
        public String group_prefix;
        public IconGroup prefix(String prefix) {
            this.group_prefix = prefix + "_";
            return this;
        }
        public void afterRegister() { }
    }
    
    public static void register(IconRegister reg, Class base, Object instance, String base_prefix) {
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
                
                if (IconGroup.class.isAssignableFrom(f.getType())) {
                    IconGroup ig = (IconGroup) f.get(instance);
                    if (ig == null) {
                        ig = (IconGroup) f.getType().newInstance();
                    }
                    ig.prefix(f.getName());
                    f.set(instance, ig);
                    register(reg, f.getType(), ig, prefix + ig.group_prefix);
                    ig.afterRegister();
                }
                if (Icon.class.isAssignableFrom(f.getType())) {
                    String icon_file = prefix + f.getName();
                    f.set(instance, reg.registerIcon(icon_file.replace('$', '/')));
                }
            }
        } catch (ReflectiveOperationException e) {
            //Shouldn't happen
            throw new IllegalArgumentException(e);
        }
    }
}
