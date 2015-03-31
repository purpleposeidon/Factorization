package factorization.fzds;

import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

public class ShadowPlayerAligner {
    final EntityPlayer real, shadow;
    final IDeltaChunk idc;


    public ShadowPlayerAligner(EntityPlayer real, EntityPlayer shadow, IDeltaChunk idc) {
        this.real = real;
        this.shadow = shadow;
        this.idc = idc;
    }

    public void apply() {
        // (Hmm, this could probably be done better)
        Vec3 realPos = SpaceUtil.fromPlayerEyePos(real);
        Vec3 tmp = real.getLookVec();
        SpaceUtil.incrAdd(tmp, realPos);
        Vec3 realLookEnd = tmp;
        Vec3 shadowPos = idc.real2shadow(realPos); // This used to be the raw ent pos, which isn't the same.
        Vec3 tmp_shadowLookEnd = idc.real2shadow(realLookEnd);
        SpaceUtil.incrSubtract(tmp_shadowLookEnd, shadowPos);
        Vec3 shadowLook = tmp_shadowLookEnd;
        double xz_len = Math.hypot(shadowLook.xCoord, shadowLook.zCoord);
        double shadow_pitch = -Math.toDegrees(Math.atan2(shadowLook.yCoord, xz_len)); // erm, negative? Dunno.
        double shadow_yaw = Math.toDegrees(Math.atan2(-shadowLook.xCoord, shadowLook.zCoord)); // Another weird negative!

        shadow.posX = shadowPos.xCoord;
        shadow.posY = shadowPos.yCoord;
        shadow.posZ = shadowPos.zCoord;
        shadow.rotationPitch = (float) shadow_pitch;
        shadow.rotationYaw = (float) shadow_yaw;
    }

    public void unapply() {
        // Stub for undoing the effects in case this becomes necessary.
        // In that case, original state of shadow will be saved & restored. But may not be needed ever.
    }
}
