package factorization.misc;

import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;

public class ProxiedSound implements ISound {
    private final ISound parent;

    public ProxiedSound(ISound parent) {
        this.parent = parent;
    }

    @Override
    public ResourceLocation getSoundLocation() {
        return parent.getSoundLocation();
    }

    @Override
    public boolean canRepeat() {
        return parent.canRepeat();
    }

    @Override
    public int getRepeatDelay() {
        return parent.getRepeatDelay();
    }

    @Override
    public float getVolume() {
        return parent.getVolume();
    }

    @Override
    public float getPitch() {
        return parent.getPitch();
    }

    @Override
    public float getXPosF() {
        return parent.getXPosF();
    }

    @Override
    public float getYPosF() {
        return parent.getYPosF();
    }

    @Override
    public float getZPosF() {
        return parent.getZPosF();
    }

    @Override
    public AttenuationType getAttenuationType() {
        return parent.getAttenuationType();
    }
}
