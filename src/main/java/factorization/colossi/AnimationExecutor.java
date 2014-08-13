package factorization.colossi;

import java.io.IOException;
import java.util.List;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.colossi.ColossusController.LimbInfo;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.FzUtil;
import static factorization.shared.FzUtil.interp;

public class AnimationExecutor implements IDataSerializable {
    String animationName;
    int keyframeIndex = 0;
    int frameTicks = 0;
    
    List<KeyFrame> keys;
    KeyFrame current, next;
    
    double executionSpeed = 1.0/20.0;
    
    public AnimationExecutor(String animationName) {
        this.animationName = animationName;
        keys = Animation.lookup(animationName);
        setKeyFrame(0);
    }
    
    void setExecutionSpeed(double speed) {
        this.executionSpeed = speed;
    }
    
    void setKeyFrame(int i) {
        current = keys.get(i);
        next = keys.get(i + 1);
        keyframeIndex = i;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        animationName = data.asSameShare(prefix + "name").putString(animationName);
        keyframeIndex = data.asSameShare(prefix + "keyframe").putInt(keyframeIndex);
        frameTicks = data.asSameShare(prefix + "ticks").putInt(frameTicks);
        return this;
    }
    
    public boolean tick(LimbInfo body, LimbInfo limb) {
        double ticks_per_keyframe = current.duration / executionSpeed;
        double partial = frameTicks / ticks_per_keyframe;
        applyPartial(body, limb, partial);
        frameTicks++;
        if (current.duration == 0) return true;
        if (frameTicks * executionSpeed > current.duration) {
            if (next.duration == 0) return true;
            setKeyFrame(keyframeIndex + 1);
        }
        return false;
    }
    
    void applyPartial(LimbInfo body, LimbInfo limb, double partial) {
        Vec3 offset = Vec3.createVectorHelper(0, interp(current.extension, next.extension, partial), 0);
        Quaternion twistRotation = Quaternion.getRotationQuaternionRadians(interp(current.twist, next.twist, partial), ForgeDirection.UP);
        Quaternion swingRotation = Quaternion.getRotationQuaternionRadians(interp(current.swing, next.swing, partial), ForgeDirection.SOUTH);
        Quaternion sweepRotation = Quaternion.getRotationQuaternionRadians(interp(current.sweep, next.sweep, partial), ForgeDirection.UP);
        Quaternion rotation = twistRotation.multiply(swingRotation).multiply(sweepRotation);
        rotation.applyReverseRotation(offset);
        limb.ent.posX = body.ent.posX + offset.xCoord;
        limb.ent.posY = body.ent.posY + offset.yCoord;
        limb.ent.posZ = body.ent.posZ + offset.zCoord;
        limb.ent.setRotation(rotation);
    }
    
    
}
