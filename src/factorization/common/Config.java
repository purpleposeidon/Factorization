package factorization.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import net.minecraftforge.common.Configuration;


public class Config {
    @Conf("serverAdmin")
    @Comment("This is a java regular expression. If a TileEntity's name matches this, it will not be accessible by routers.")
    public static String bannedRouterInventoriesRegex = "";
    @Conf("serverAdmin")
    public static int watchDemonChunkRange = 3;

    @block
    @Comment("used for the majority of Factorization's blocks")
    public static int factory = 254;

    @block
    @Comment("Used for Wrathlamps and Wrathfire.")
    public static int wrath = 253;

    @Conf
    @Comment("May need to change for 4092")
    public static int blockItemIdOffset = -256;

    @Conf("client")
    public static boolean renderBarrelItem = true;
    @Conf("client")
    public static boolean renderBarrelText = true;
    @Conf("client")
    public static boolean debugLightAir = false;

    static void load(Configuration c) {
        for (Field field : Config.class.getFields()) {
            String section = null;
            for (Annotation a : field.getAnnotations()) {
                if (a.getClass() == Conf.class) {
                    section = field.getAnnotation(Conf.class).value();
                    break;
                }
                if (a.getClass() == block.class) {
                    section = "blockId";
                    break;
                }
                if (a.getClass() == item.class) {
                    section = "itemId";
                    break;
                }
            }
            Conf p = field.getAnnotation(Conf.class);
        }
    }

    @interface Conf {
        String value() default "general";
    }

    @interface block {
    }

    @interface item {
    }

    @interface Comment {
        String value();
    }
}
