package factorization.common;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FactorizationTextureLoader {
    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.FIELD)
    public @interface Directory {
        public String value();
    }
    
    public abstract static class IconGroup {
        public String group_prefix;
        public IconGroup prefix(String prefix) {
            this.group_prefix = prefix + "_";
            return this;
        }
    }
    
    public static void register(IconRegister reg, Class base) {
        register(reg, base, null, "factorization:");
    }
    
    public static void register(IconRegister reg, Class base, Object instance, String base_prefix) {
        try {
            for (Field f : base.getFields()) {
                String prefix = base_prefix;
                
                Directory dir = f.getAnnotation(Directory.class);
                if (dir != null) {
                    prefix += dir.value() + "/";
                }
                
                if (IconGroup.class.isAssignableFrom(f.getType())) {
                    IconGroup ig = (IconGroup) f.get(instance);
                    if (ig == null) {
                        ig = (IconGroup) f.getType().newInstance();
                        ig.prefix(f.getName());
                        f.set(instance, ig);
                    }
                    register(reg, f.getType(), ig, prefix + ig.group_prefix);
                }
                if (Icon.class.isAssignableFrom(f.getType())) {
                    String icon_file = prefix + f.getName();
                    f.set(instance, reg.makeIcon(icon_file.replace('$', '/')));
                }
            }
        } catch (IllegalAccessException e) {
            //Shouldn't happen
            throw new IllegalArgumentException(e);
        } catch (InstantiationException e) {
            //Also shouldn't happen
            throw new IllegalArgumentException(e);
        }
    }
}
