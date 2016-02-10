package factorization.fzds;

import factorization.fzds.interfaces.IDimensionSlice;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class ShadowPlayerAligner {
    final EntityPlayer real, shadow;
    final IDimensionSlice idc;


    public ShadowPlayerAligner(EntityPlayer real, EntityPlayer shadow, IDimensionSlice idc) {
        this.real = real;
        this.shadow = shadow;
        this.idc = idc;
    }

    public void apply() {
        // (Hmm, this could probably be done better)
        Vec3 realPos = SpaceUtil.fromPlayerEyePos(real);
        Vec3 shadowPos = idc.real2shadow(realPos); // This used to be the raw ent pos, which isn't the same.
        Vec3 shadowLook = idc.getTransform().getRot().applyRotation(real.getLookVec());
        double xz_len = Math.hypot(shadowLook.xCoord, shadowLook.zCoord);
        double shadow_pitch = -Math.toDegrees(Math.atan2(shadowLook.yCoord, xz_len)); // erm, negative? Dunno.
        double shadow_yaw = Math.toDegrees(Math.atan2(-shadowLook.xCoord, shadowLook.zCoord)); // Another weird negative!

        shadow.rotationPitch = (float) shadow_pitch;
        shadow.rotationYawHead = (float) shadow_yaw;
        shadow.setPosition(shadowPos.xCoord, shadowPos.yCoord, shadowPos.zCoord);
        NORELEASE.fixme("spaceutil that");
        //SpaceUtil.setEntPos(shadow, shadowPos);
    }

    public void unapply() {
        // Stub for undoing the effects in case this becomes necessary.
        // In that case, original state of shadow will be saved & restored. But may not be needed ever.
    }
}
