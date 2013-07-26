package factorization.common;

import static factorization.common.GlazeTypes.BRIGHT;
import static factorization.common.GlazeTypes.COMMON;
import static factorization.common.GlazeTypes.MATTE;
import static factorization.common.GlazeTypes.SHINY;
import static factorization.common.GlazeTypes.TRANSLUCENT;
import net.minecraft.util.Icon;

public enum BasicGlazes {
    ST_VECHS_BLACK(MATTE, 0x1F1C1B),
    TEMPLE_WHITE(COMMON, 0xEED8C3),
    SALLYS_WHITE(SHINY, 0xDCE2EE),
    CLEAR(TRANSLUCENT, 0xE8C1EE),
    REDSTONE_OXIDE(COMMON, 0xEE0000),
    LAPIS_OXIDE(COMMON, 0x0000EE),
    PURPLE_OXIDE(COMMON, 0x7B00EE),
    LEAD_OXIDE(COMMON, 0x231E1E),
    FIRE_ENGINE_RED(BRIGHT, 0xAF9164),
    CELEDON(TRANSLUCENT, 0x9AAF8F),
    IRON_BLUE(SHINY, 0x9F8DAF),
    STONEWARE_SLIP(COMMON, 0xAF9B9B),
    TENMOKU(COMMON, 0x3F0C01),
    PEKING_BLUE(BRIGHT, 0x262E28),
    SHINO(MATTE, 0x2E2219);
    //oribe green, woo blue, mambo, hamada rust, copper red
    
    public GlazeTypes type;
    public int metadata;
    public Icon icon;
    public int raw_color;
    
    static BasicGlazes[] values = values();
    
    private BasicGlazes(GlazeTypes type, int raw_color) {
        this.type = type;
        this.metadata = BlockResource.glaze_md_start + ordinal();
        this.raw_color = raw_color;
    }
    
    public void recipe(Object... params) {
        Core.registry.shapelessOreRecipe(Core.registry.glaze_bucket.make(this), params);
    }
}
