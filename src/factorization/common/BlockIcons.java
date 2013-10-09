package factorization.common;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.FactorizationTextureLoader.Directory;
import factorization.common.FactorizationTextureLoader.IconGroup;
import factorization.common.FactorizationTextureLoader.Ignore;

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
        turbine_top, turbine_bottom, turbine_side,
        leyden_metal, leyden_rim, leyden_glass, leyden_glass_side, leyden_knob,
        caliometric_side, caliometric_top;
    
    @Directory("craft")
    public static BlockIcons.ActivatingMachineIcon stamper, packager;
    
    @Directory("machine")
    public static Icon compactFace, compactBack, compactSide, compactSideSlide;
    
    @Directory("machine")
    public static Icon
        cauldron_side, cauldron_top,
        generic_metal,
        grinder_bottom, grinder_top, grinder_side, grinder_bottom_top_edge,
        heater_heat, heater_spiral,
        parasieve_front, parasieve_side, parasieve_back;
    
    @Directory("machine")
    public static SimpleMachine slag_furnace;
    public static Icon machine$slag_furnace_face_on;
    
    @Directory("rocket")
    public static Icon
        rocket_engine_top, rocket_engine_bottom_hole, rocket_engine_nozzle, rocket_engine_valid, rocket_engine_invalid;
    
    public static Icon servo$rail;
    public static Icon servo$model$chasis, servo$model$gear;
    
    public static Icon ceramics$bisque, ceramics$dry, ceramics$stand, ceramics$rawglaze;
    
    public static Icon socket$face, socket$side;
    public static Icon socket$hand, socket$arm0, socket$arm1, socket$arm2, socket$arm3;
    public static Icon socket$shifter_front, socket$shifter_side;
    
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
    
    @Directory("storage")
    public static BarrelTextureset normal, silky, hopping, larger, sticky;
    
    public static class BarrelTextureset extends IconGroup {
        public Icon side, front, top;
        
        @Override
        public IconGroup prefix(String prefix) {
            this.group_prefix = prefix + "/";
            return this;
        }
    }
    
    
    public static ArrowyBox servo$set_direction, servo$set_facing;
    public static Icon servo$activate, servo$activate_sneaky;
    public static Icon servo$bay, servo$bay_bottom, servo$bay_top;
    public static Icon servo$one, servo$zero, servo$number, servo$sum, servo$product, servo$dup, servo$drop;
    public static Icon servo$configure, servo$deconfigure;
    public static Icon servo$pulse;
    public static Icon servo$spin_cc, servo$spin_ccc;
    public static Icon servo$speed1, servo$speed2, servo$speed3, servo$speed4, servo$speed5;
    public static Icon servo$cmp_lt, servo$cmp_le, servo$cmp_eq, servo$cmp_ne, servo$cmp_ge, servo$cmp_gt;
    public static Icon servo$jmp;
    public static Icon servo$entry_require, servo$entry_forbid;
    
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
    
    public static abstract class ExtendedIcon implements Icon {
        public Icon under;
        
        public ExtendedIcon(Icon under) {
            this.under = under;
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public int getIconWidth() {
            return under.getIconWidth();
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public int getIconHeight() {
            return under.getIconHeight();
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public float getMinV() {
            return getInterpolatedV(0);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public float getMinU() {
            return getInterpolatedU(0);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxV() {
            return getInterpolatedV(16);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxU() {
            return getInterpolatedU(16);
        }
        
        @Override
        @SideOnly(Side.CLIENT)
        public String getIconName() {
            return under.getIconName();
        }
    }
    
    public static class ArrowyBox extends IconGroup {
        public Icon front, side_N, side_E, back;
        @Ignore
        public Icon side_S, side_W;

        @Override
        public void afterRegister() {
            side_S = new ExtendedIcon(side_N) {
                @Override
                @SideOnly(Side.CLIENT)
                public float getInterpolatedV(double d0) {
                    return under.getInterpolatedV(16 - d0);
                }
                
                @Override
                @SideOnly(Side.CLIENT)
                public float getInterpolatedU(double d0) {
                    return under.getInterpolatedU(16 - d0);
                }
            };
            side_W = new ExtendedIcon(side_E) {
                @Override
                @SideOnly(Side.CLIENT)
                public float getInterpolatedV(double d0) {
                    return under.getInterpolatedV(16 - d0);
                }
                
                @Override
                @SideOnly(Side.CLIENT)
                public float getInterpolatedU(double d0) {
                    return under.getInterpolatedU(16 - d0);
                }
            };
        }
        
        public Icon get(ForgeDirection arrow_direction, ForgeDirection face) {
            if (arrow_direction == face) {
                return front;
            }
            if (arrow_direction.getOpposite() == face) {
                return back;
            }
            if (arrow_direction == ForgeDirection.UP) {
                return side_N;
            }
            if (arrow_direction == ForgeDirection.DOWN) {
                return side_S;
            }
            if (face.offsetY != 0) {
                if (arrow_direction == ForgeDirection.WEST) return side_W;
                if (arrow_direction == ForgeDirection.EAST) return side_E;
                if (arrow_direction == ForgeDirection.NORTH) return side_N;
                if (arrow_direction == ForgeDirection.SOUTH) return side_S;
            }
            if (face == ForgeDirection.WEST) {;
                return arrow_direction.offsetZ == 1 ? side_E : side_W;
            }
            if (face == ForgeDirection.EAST) {;
                return arrow_direction.offsetZ == -1 ? side_E : side_W;
            }
            if (face == ForgeDirection.NORTH) {
                return arrow_direction.offsetX == -1 ? side_E : side_W;
            }
            if (face == ForgeDirection.SOUTH) {
                return arrow_direction.offsetX == 1 ? side_E : side_W;
            }
            return uv_test;
        }
        public void unsetRotations(RenderBlocks rb) {
            rb.uvRotateNorth = rb.uvRotateEast = rb.uvRotateSouth = rb.uvRotateWest = rb.uvRotateTop = rb.uvRotateBottom = 0;
        }
    }
    
    public static Icon steam;
}