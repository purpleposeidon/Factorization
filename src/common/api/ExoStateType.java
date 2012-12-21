package factorization.api;

import static factorization.api.ExoStateShader.INVERSE;
import static factorization.api.ExoStateShader.NORMAL;
import static factorization.api.ExoStateShader.RISINGEDGE;
import factorization.common.Core;

public enum ExoStateType {
    NEVER, BUTTON1(1), BUTTON2(2), BUTTON3(3),
    /*EATING(ExoArmorRestriction.HEAD), */ HURT(ExoArmorRestriction.HEAD), WOUNDED(ExoArmorRestriction.HEAD),
    MOVING(ExoArmorRestriction.CHEST), ONFIRE(ExoArmorRestriction.CHEST),
    SNEAKING(ExoArmorRestriction.PANTS), RIDING(ExoArmorRestriction.PANTS),
    SPRINTING(ExoArmorRestriction.FEET), ONGROUND(ExoArmorRestriction.FEET), INWATER(ExoArmorRestriction.FEET);

    public final ExoArmorRestriction armorRestriction;
    public final int key;

    ExoStateType() {
        armorRestriction = ExoArmorRestriction.NONE;
        key = 0;
    }

    ExoStateType(int key) {
        armorRestriction = ExoArmorRestriction.NONE;
        this.key = key;
    }

    ExoStateType(ExoArmorRestriction ar) {
        armorRestriction = ar;
        key = 0;
    }

    public String when(ExoStateShader shader) {
        return "exo." + this + "." + shader;
    }

    private static void en(String local, String en) {
        Core.proxy.addName(local, en);
    }

    static {
        en(NEVER.when(NORMAL), "Never");
        en(NEVER.when(INVERSE), "Always");
        en(NEVER.when(RISINGEDGE), "Never (rising edge)");
        for (ExoStateType button : new ExoStateType[] { BUTTON1, BUTTON2,
                BUTTON3 }) {
            en(button.when(NORMAL), "While %s is held");
            en(button.when(INVERSE), "While %s is not held");
            en(button.when(RISINGEDGE), "When %s is tapped");
        }
        //en(EATING.when(NORMAL), "While eating");
        //en(EATING.when(INVERSE), "While not eating");
        //en(EATING.when(RISINGEDGE), "When first eating");
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
        en(INWATER.when(INVERSE), "While not in water");
        en(INWATER.when(RISINGEDGE), "When entering water");
    }

    public String brief() {
        if (this.key > 0) {
            return Core.proxy.getExoKeyBrief(this.key);
        }
        return this.name();
    }
}
