package factorization.common;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.shared.TileEntityFactorization;
import factorization.shared.FactorizationTextureLoader.Directory;
import factorization.shared.FactorizationTextureLoader.IIconGroup;
import factorization.shared.FactorizationTextureLoader.Ignore;

public class BlockIcons {
    public static IIcon uv_test,
        default_icon,
        error;
    
    @Directory("material")
    public static IIcon
        edgeless_glass,
        iron_bar_grid,
        transparent,
        wood;
    
    @Directory("resource")
    public static IIcon
        dark_iron_block,
        galena_ore,
        lead_block,
        silver_block,
        ore_dark_iron,
        ore_dark_iron_glint;
    
    @Directory("charge")
    public static IIcon
        motor_texture,
        wire,
        battery_bottom, battery_top, battery_side, battery_meter,
        boiler_side, boiler_top,
        mirror_front, mirror_back, mirror_side,
        turbine_top, turbine_bottom, turbine_side,
        leyden_glass, leyden_glass_side, leyden_knob,
        caliometric_side, caliometric_top;

    @Directory("charge")
    public static CauldronTextureset leyden;
    
    @Directory("craft")
    public static BlockIcons.ActivatingMachineIIcon stamper, packager;
    
    @Directory("machine")
    public static IIcon compactFace, compactBack, compactSide, compactSideSlide;
    
    @Directory("machine")
    public static IIcon
        generic_metal,
        grinder_bottom, grinder_top, grinder_side, grinder_bottom_top_edge,
        heater_heat, heater_spiral,
        parasieve_front, parasieve_side, parasieve_back;

    @Directory("machine")
    public static CauldronTextureset crystallizer, mixer;

    public static class CauldronTextureset extends IIconGroup {
        public IIcon side, top, bottom;
    }
    
    @Directory("machine")
    public static SimpleMachine slag_furnace;
    public static IIcon machine$slag_furnace_face_on;
        
    @Directory("rocket")
    public static IIcon
        rocket_engine_top, rocket_engine_bottom_hole, rocket_engine_nozzle, rocket_engine_valid, rocket_engine_invalid;
    
    public static IIcon servo$rail, servo$rail_comment;
    public static IIcon servo$model$chasis, servo$model$sprocket;
    
    public static IIcon ceramics$bisque, ceramics$dry, ceramics$stand, ceramics$rawglaze;
    
    public static IIcon socket$face, socket$side;
    public static IIcon socket$hand, socket$arm0, socket$arm1, socket$arm2, socket$arm3;
    public static IIcon socket$shifter_front, socket$shifter_side;
    public static IIcon socket$corkscrew;
    public static IIcon socket$mini_piston;

    public static IIcon mechanism$hinge_uvs;

    
    public static IIcon utiligoo$invasion;

    public static IIcon beauty$saptap, beauty$saptap_top, beauty$anthrogen;
    public static IIcon[] beauty$shaft = new IIcon[4];
    
    @Directory("storage")
    public static BarrelTextureset normal, silky, hopping, sticky;
    @Directory("storage")
    public static IIcon barrel_font;
    
    public static class BarrelTextureset extends IIconGroup {
        public IIcon side, front, top, top_metal;
        
        @Override
        public IIconGroup prefix(String prefix) {
            this.group_prefix = prefix + "/";
            return this;
        }
    }
    
    
    public static ArrowyBox servo$set_direction, servo$set_facing;
    public static IIcon servo$one, servo$zero, servo$negative_one, servo$number, servo$sum, servo$product, servo$dup, servo$drop, servo$true, servo$false;
    public static IIcon servo$pulse;
    public static IIcon servo$spin_cc, servo$spin_ccc;
    public static IIcon servo$speed1, servo$speed2, servo$speed3, servo$speed4, servo$speed5;
    public static IIcon servo$cmp_lt, servo$cmp_le, servo$cmp_eq, servo$cmp_ne, servo$cmp_ge, servo$cmp_gt;
    public static IIcon servo$jmp_instruction, servo$jmp_tile;
    public static IIcon servo$entry_require, servo$entry_forbid;
    public static IIcon servo$socket_on, servo$socket_off, servo$socket_pulse;
    public static IIcon servo$ctrl$shift_import, servo$ctrl$shift_export, servo$ctrl$shift_target_slot, servo$ctrl$shift_transfer_limit, servo$ctrl$shift_stream, servo$ctrl$shift_pulse_some, servo$ctrl$shift_pulse_exact, servo$ctrl$shift_probe;
    public static IIcon servo$instruction_plate;
    public static IIcon servo$trap;
    public static IIcon servo$entry_execute, servo$entry_load, servo$entry_write, servo$entry_ignore;
    public static IIcon servo$scan_color;
    public static IIcon servo$group_empty, servo$group_something;
    public static IIcon servo$count_items, servo$read_redstone, servo$repeated_instruction;
    
    
    
    public static IIcon colossi$body, colossi$body_cracked, colossi$mask, colossi$mask_cracked, colossi$core, colossi$core_back, colossi$leg, colossi$eye, colossi$eye_open;
    public static IIcon colossi$arm_bottom, colossi$arm_side, colossi$arm_side_top, colossi$arm_side_bottom, colossi$arm_top;
    
    public static class ActivatingMachineIIcon extends IIconGroup {
        public IIcon top, bottom, side, side_on;
        public IIcon get(TileEntityFactorization tef, ForgeDirection dir) {
            switch (dir) {
            case UP: return top;
            case DOWN: return bottom;
            default: return tef.draw_active > 0 ? side_on : side;
            }
        }
    }
    
    public static class SimpleMachine extends IIconGroup {
        public IIcon face, side, top, bottom;
        
        public IIcon get(TileEntityFactorization tef, ForgeDirection dir) {
            switch (dir) {
            case UP: return top;
            case DOWN: return bottom;
            default: return tef.facing_direction == dir.ordinal() ? face : side;
            }
        }
    }
    
    public static abstract class ExtendedIIcon implements IIcon {
        public IIcon under;
        
        public ExtendedIIcon(IIcon under) {
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
    
    public static class ArrowyBox extends IIconGroup {
        public IIcon front, side_N, side_E, back;
        @Ignore
        public IIcon side_S, side_W;

        @Override
        public void afterRegister() {
            side_S = new ExtendedIIcon(side_N) {
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
            side_W = new ExtendedIIcon(side_E) {
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
        
        public IIcon get(ForgeDirection arrow_direction, ForgeDirection face) {
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
    
    public static IIcon steam;
}
