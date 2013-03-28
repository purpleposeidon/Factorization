package factorization.common;

import static factorization.common.GlazeTypes.BRIGHT;
import static factorization.common.GlazeTypes.COMMON;
import static factorization.common.GlazeTypes.MATTE;
import static factorization.common.GlazeTypes.SHINY;
import static factorization.common.GlazeTypes.TRANSLUCENT;
import net.minecraft.util.Icon;

public enum BasicGlazes {
    ST_VECHS_BLACK(MATTE),
    TEMPLE_WHITE(COMMON),
    SALLYS_WHITE(SHINY),
    CLEAR(TRANSLUCENT),
    REDSTONE_OXIDE(COMMON),
    LAPIS_OXIDE(COMMON),
    PURPLE_OXIDE(COMMON),
    LEAD_OXIDE(COMMON),
    FIRE_ENGINE_RED(BRIGHT),
    CELEDON(TRANSLUCENT),
    IRON_BLUE(SHINY),
    STONEWARE_SLIP(COMMON),
    TENMOKU(COMMON),
    PEKING_BLUE(BRIGHT),
    SHINO(MATTE);
    //oribe green, woo blue, mambo, hamada rust, copper red
    
    GlazeTypes type;
    int metadata;
    Icon icon;
    
    static BasicGlazes[] values = values();
    
    private BasicGlazes(GlazeTypes type) {
        this.type = type;
        this.metadata = BlockResource.glaze_md_start + ordinal();
    }
    
    public void recipe(Object... params) {
        Core.registry.shapelessOreRecipe(Core.registry.glaze_bucket.make(this), params);
    }
}
