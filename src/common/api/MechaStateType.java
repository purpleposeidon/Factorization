package factorization.api;

import static factorization.api.MechaStateShader.*;
import factorization.common.Core;

public enum MechaStateType {
    NEVER, BUTTON1(1), BUTTON2(2), BUTTON3(3), EATING(
            MechaArmorRestriction.HEAD), HURT(MechaArmorRestriction.HEAD), WOUNDED(
            MechaArmorRestriction.HEAD), MOVING(MechaArmorRestriction.CHEST), ONFIRE(
            MechaArmorRestriction.CHEST), SNEAKING(MechaArmorRestriction.PANTS), RIDING(
            MechaArmorRestriction.PANTS), SPRINTING(MechaArmorRestriction.FEET), ONGROUND(
            MechaArmorRestriction.FEET), INWATER(MechaArmorRestriction.FEET);

    public final MechaArmorRestriction armorRestriction;
    public final int key;

    MechaStateType() {
        armorRestriction = MechaArmorRestriction.NONE;
        key = 0;
    }

    MechaStateType(int key) {
        armorRestriction = MechaArmorRestriction.NONE;
        this.key = key;
    }

    MechaStateType(MechaArmorRestriction ar) {
        armorRestriction = ar;
        key = 0;
    }

    public String when(MechaStateShader shader) {
        return "mecha." + this + "." + shader;
    }

    private static void en(String local, String en) {
        Core.proxy.addName(local, en);
    }

    static {
        en(NEVER.when(NORMAL), "Never");
        en(NEVER.when(INVERSE), "Always");
        en(NEVER.when(RISINGEDGE), "Never (rising edge)");
        for (MechaStateType button : new MechaStateType[] { BUTTON1, BUTTON2, BUTTON3 }) {
            en(button.when(NORMAL), "While %s is held");
            en(button.when(INVERSE), "While %s is not held");
            en(button.when(RISINGEDGE), "When %s is tapped");
        }
        en(EATING.when(NORMAL), "While eating");
        en(EATING.when(INVERSE), "While not eating");
        en(EATING.when(RISINGEDGE), "When first eating");
        en(HURT.when(NORMAL), "While being hurt");
        en(HURT.when(INVERSE), "While not being hurt");
        en(HURT.when(RISINGEDGE), "When first hurt");
        en(WOUNDED.when(NORMAL), "While injurred");
        en(WOUNDED.when(INVERSE), "While healthy");
        en(WOUNDED.when(RISINGEDGE), "When first injurred");
        en(MOVING.when(NORMAL), "While moving");
        en(MOVING.when(INVERSE), "While still");
        en(MOVING.when(RISINGEDGE), "When starting to move");
        en(ONFIRE.when(NORMAL), "While on fire");
        en(ONFIRE.when(INVERSE), "While not on fire");
        en(ONFIRE.when(RISINGEDGE), "When lit on fire");
        en(SNEAKING.when(NORMAL), "While sneaking");
        en(SNEAKING.when(INVERSE), "While not sneaking");
        en(SNEAKING.when(RISINGEDGE), "When starting to sneak");
        en(RIDING.when(NORMAL), "While riding");
        en(RIDING.when(INVERSE), "While not riding");
        en(RIDING.when(RISINGEDGE), "When mounting");
        en(SPRINTING.when(NORMAL), "While running");
        en(SPRINTING.when(INVERSE), "While not running");
        en(SPRINTING.when(RISINGEDGE), "When starting to run");
        en(ONGROUND.when(NORMAL), "While on the ground");
        en(ONGROUND.when(INVERSE), "While not on the ground");
        en(ONGROUND.when(RISINGEDGE), "When first touching the ground");
        en(INWATER.when(NORMAL), "While in water");
        en(INWATER.when(INVERSE), "While not in the water");
        en(INWATER.when(RISINGEDGE), "When entering water");
        System.out.println("XXX Added MST l##n");
    }
}