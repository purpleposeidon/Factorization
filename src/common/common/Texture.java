package factorization.common;

public class Texture {
    static public int force_texture = -1;

    static final int cutter_normal = 16 * 3;
    static final int router_new = 16 * 5;
    static final int router_new_top = 16 * 4 + 0;
    static final int router_new_bottom = router_new_top + 1;
    static final int maker_start = 16 * 7;
    static final int stamper_start = 16 * 8;
    static final int packager_start = 16 * 11;
    static final int craft_top = 0;
    static final int craft_side = 1;
    static final int craft_side_active = 2;
    static final int craft_bottom = 3;
    static final int barrel_top = 16 * 9 + 0;
    static final int barrel_side = 16 * 9 + 1;
    static final int queue_top = 16 * 10 + 0;
    static final int queue_bottom = 16 * 10 + 1;
    static final int queue_side = 16 * 10 + 2;
    static public final int lamp_iron = 2;
    static final int block_iron = 3;
    static final int glass = 4;
    static final int bars = 5;
    static final int mecha_top = 16;
    static final int mecha_side = mecha_top + 1;
    static final int mecha_bottom = mecha_top + 2;
    static final int mecha_config = mecha_top + 3;
    static final int furnace_start = 16 * 4 + 2;

    static int pick(int md, int side, boolean active, int facing_direction) {
        if (FactoryType.ROUTER.is(md)) {
            // Texture layout:
            // [top][bottom]; [normal NSWE]; [Active NSWE]
            if (side == 0) {
                return router_new_bottom;
            }
            if (side == 1) {
                return router_new_top;
            }
            int index = side - 2;
            if (active) {
                return router_new + index + 16;
            } else {
                return router_new + index;
            }
        }

        if (FactoryType.CUTTER.is(md)) {
            return cutter_normal;
        }

        if (FactoryType.MAKER.is(md) || FactoryType.STAMPER.is(md) || FactoryType.PACKAGER.is(md)) {
            int start = 0;
            if (FactoryType.MAKER.is(md)) {
                start = maker_start;
            }
            if (FactoryType.STAMPER.is(md)) {
                start = stamper_start;
            }
            if (FactoryType.PACKAGER.is(md)) {
                start = packager_start;
            }
            if (side == 0) {
                return start + craft_bottom;
            }
            if (side == 1) {
                return start + craft_top;
            }
            if (active) {
                return start + craft_side_active;
            }
            return start + craft_side;
        }

        if (FactoryType.BARREL.is(md)) {
            if (side == 0 || side == 1) {
                return barrel_top;
            }
            //TODO: Have barrel render info on one side only, and put a nice texture on that side.
            if (side == facing_direction) {
                return barrel_side + 1;
            }
            return barrel_side;
        }

        if (FactoryType.QUEUE.is(md)) {
            if (side == 0) {
                return queue_bottom;
            }
            if (side == 1) {
                return queue_top;
            }
            return queue_side;
        }

        if (FactoryType.LAMP.is(md)) {
            return lamp_iron;
        }

        if (FactoryType.SENTRYDEMON.is(md)) {
            if (side == 0 || side == 1) {
                return bars + 1;
            }
            return bars;
        }
        if (FactoryType.SLAGFURNACE.is(md)) {
            if (side == 0 || side == 1) {
                //verts
                return furnace_start;
            }
            if (side == facing_direction) {
                if (active) {
                    return furnace_start + 3;
                }
                return furnace_start + 2;
            }
            return furnace_start + 1;
        }
        return 0;
    }
}
