package factorization.mechanics;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

public class WinchSound extends MovingSound {
    SocketPoweredCrank source;
    byte direction;

    public WinchSound(byte direction, SocketPoweredCrank source) {
        super(new ResourceLocation(direction == -1 ? "factorization:winch.powered" : "factorization:winch.unwind"));
        this.direction = direction;
        this.source = source;
        this.repeat = true;
        updateIntensity();
    }

    @Override
    public void update() {
        updateIntensity();
        this.xPosF = source.getPos().getX() + 0.5F;
        this.yPosF = source.getPos().getY() + 0.5F;
        this.zPosF = source.getPos().getZ() + 0.5F;
    }

    @Override
    public boolean isDonePlaying() {
        if (source.isInvalid() || Math.signum(source.chainDelta) != direction) {
            source.soundActive = false;
            direction = 4; // Now the signum can never be correct
            return true;
        }
        return false;
    }

    void updateIntensity() {
        float delta = (float) Math.abs(source.chainDelta);
        volume = Math.max(0.25F, delta);
        float pitch = (float) Math.min(Math.max(0.05, delta), 8);
        source.soundActive = true;
        this.field_147663_c = pitch;
    }
}
