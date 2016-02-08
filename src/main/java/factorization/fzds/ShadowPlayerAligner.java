package factorization.fzds;

import factorization.fzds.interfaces.IDimensionSlice;
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
        Vec3 realLookEnd = real.getLookVec().add(realPos);
        Vec3 shadowPos = idc.real2shadow(realPos); // This used to be the raw ent pos, which isn't the same.
        Vec3 shadowLook = idc.real2shadow(realLookEnd).subtract(shadowPos);
        double xz_len = Math.hypot(shadowLook.xCoord, shadowLook.zCoord);
        double shadow_pitch = -Math.toDegrees(Math.atan2(shadowLook.yCoord, xz_len)); // erm, negative? Dunno.
        double shadow_yaw = Math.toDegrees(Math.atan2(-shadowLook.xCoord, shadowLook.zCoord)); // Another weird negative!

        shadow.posX = shadowPos.xCoord;
        shadow.posY = shadowPos.yCoord;
        shadow.posZ = shadowPos.zCoord;
        shadow.rotationPitch = (float) shadow_pitch;
        shadow.rotationYaw = (float) shadow_yaw;
        double hx = shadow.width / 2;
        double hy = shadow.height / 2;
        double hz = shadow.width / 2;
        shadow.setEntityBoundingBox(new AxisAlignedBB(
                shadow.posX - hx,
                shadow.posY - hx,
                shadow.posZ - hy,
                shadow.posX + hy,
                shadow.posY + hz,
                shadow.posZ + hz
        ));
    }

    public void unapply() {
        // Stub for undoing the effects in case this becomes necessary.
        // In that case, original state of shadow will be saved & restored. But may not be needed ever.
    }
}
