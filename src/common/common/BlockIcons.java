package factorization.common;

import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.common.FactorizationTextureLoader.Directory;
import factorization.common.FactorizationTextureLoader.IconGroup;

public class BlockIcons {
    public static Icon uv_test,
        default_icon,
        error;
    
    @Directory("material")
    public static Icon
        edgeless_glass,
        iron_bar_grid,
        transparent,
        wood;
    
    @Directory("resource")
    public static Icon
        dark_iron_block,
        galena_ore,
        lead_block,
        silver_block;
    
    @Directory("charge")
    public static Icon
        motor_texture,
        wire,
        battery_bottom, battery_top, battery_side, battery_meter,
        boiler_side, boiler_top,
        mirror_front, mirror_back, mirror_side,
        turbine_top, turbine_bottom, turbine_side;
    
    @Directory("craft")
    public static BlockIcons.ActivatingMachineIcon maker, stamper, packager;
    
    @Directory("exo")
    public static Icon modder_top, modder_bottom, modder_side;
    
    @Directory("machine")
    public static Icon
        cauldron_side, cauldron_top,
        generic_metal,
        grinder_bottom, grinder_top, grinder_side, grinder_bottom_top_edge,
        heater_heat, heater_spiral;
    @Directory("machine")
    public static SimpleMachine slag_furnace;
    public static Icon machine$slag_furnace_face_on;
    
    @Directory("rocket")
    public static Icon
        rocket_engine_top, rocket_engine_bottom_hole, rocket_engine_nozzle, rocket_engine_valid, rocket_engine_invalid;
    
    public static Icon ceramics$bisque, ceramics$dry, ceramics$stand, ceramic$rawglaze;
    
    public static class RouterFace extends IconGroup {
        public Icon on, off;
        
        public Icon get(TileEntityFactorization tef) {
            return tef.draw_active > 0 ? on : off;
        }
    }
    
    public static RouterFace router$north, router$south, router$east, router$west;
    
    public static Icon router$top, router$bottom;
    
    @Directory("storage")
    public static SimpleMachine barrel, ed_barrel;
    
    
    public static class ActivatingMachineIcon extends IconGroup {
        public Icon top, bottom, side, side_on;
        public Icon get(TileEntityFactorization tef, ForgeDirection dir) {
            switch (dir) {
            case UP: return top;
            case DOWN: return bottom;
            default: return tef.draw_active > 0 ? side_on : side;
            }
        }
    }
    
    public static class SimpleMachine extends IconGroup {
        public Icon face, side, top, bottom;
        
        public Icon get(TileEntityFactorization tef, ForgeDirection dir) {
            switch (dir) {
            case UP: return top;
            case DOWN: return bottom;
            default: return tef.facing_direction == dir.ordinal() ? face : side;
            }
        }
    }
}