package factorization.fzds;

import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.Interpolation;
import factorization.fzds.interfaces.transform.Any;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;

import javax.annotation.Nonnull;
import java.io.IOException;

public class BasicTransformOrder implements ITransformOrder {

    /**
     * Orders the IDC to move to a target rotation. This will set the rotational velocity to 0.
     * If setTargetRotation is called before a previous order has completed, the old order will be interrupted,
     * and rotation will continue to the new order from whatever rotation it was at when interrupted.
     *
     * This method causes the IDC to sweep its rotation between its current rotation, and the target rotation.
     * The target rotation is *NOT* globally relative; it is instead relative to the rotation of its parent.
     * Consider a carousel with a grandfather clock standing on it. The cabinet and the carousel would be represented with 1 IDC,
     * and the hands of the clock would be each represented with an IDC. Let the carousel be still and at its default position,
     * with the clock facing NORTH. When it is 12 o'clock, the hand will have zero rotation, and when it's 6 o'clock, the hand will
     * have half a turn around the NORTH axis. Using this method to order rotations between 6 o'clock and 12 o'clock will have the
     * natural, desired effect.
     *
     * However, using setRotation or setRotationalVelocity can result in the hand not being flush with the face of the clock, since
     * those methods are relative to the world.
     *
     * @param idc The IDC to affect
     * @param target The target rotation. The method will make a copy of that parameter.
     * @param tickTime How many ticks to reach that rotation
     * @param interp How to interpolate between them. SMOOTH is a good default value.
     */
    public static void give(IDimensionSlice idc, TransformData<Any> target, int tickTime, Interpolation interp) {
        BasicTransformOrder order = new BasicTransformOrder(idc.getTransform().copy().elide(), target, 1.0 / tickTime, interp);
        idc.giveOrder(order);
    }

    public static void give(IDimensionSlice idc, Quaternion rot, int tickTime, Interpolation interp) {
        TransformData<Any> r = TransformData.newDebased().elide();
        r.setRot(rot);
        give(idc, r, tickTime, interp);
    }

    public BasicTransformOrder(TransformData<Any> start, TransformData<Any> end, double incr, Interpolation interp) {
        this.start = start;
        this.end = end;
        this.incr = incr;
        this.interp = interp;
    }


    TransformData<Any> start, end, range;
    double progress = 0;
    double incr;
    Interpolation interp;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        start = data.asSameShare(prefix + ":start").putIDS(start);
        end = data.asSameShare(prefix + ":end").putIDS(end);
        progress = data.asSameShare(prefix + ":progress").putDouble(progress);
        incr = data.asSameShare(prefix + ":incr").putDouble(incr);
        interp = data.asSameShare(prefix + ":interpMode").putEnum(interp);
        if (data.isReader()) {
            range = start.difference(end);
        }
        return this;
    }

    @Override
    public TransformData<Pure> tickTransform(IDimensionSlice idc) {
        if (progress >= 1) return null;
        TransformData<Pure> prev = TransformData.interpolate(start, end, progress).purified();
        progress += incr;
        TransformData<Pure> next = TransformData.interpolate(start, end, progress).purified();
        return next.difference(prev.elide()).purified();
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Nonnull
    @Override
    public TransformData<Pure> removed(boolean completeElseCancelled) {
        return TransformData.newPure();
    }

    public int getTicksRemaining() {
        double empty = 1 - incr;
        return (int) (empty / incr);
    }
}
