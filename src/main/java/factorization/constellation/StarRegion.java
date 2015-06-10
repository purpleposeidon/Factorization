package factorization.constellation;

import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.util.AxisAlignedBB;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class StarRegion extends FastBag<IStar> implements IStarRegion, Comparator<IStar> {
    public static final int MAX_SIZE = 16;
    public static final int MAX_LENGTH = 12;

    private AxisAlignedBB bounds = null, draw_bounds = null;
    private float density = 1;

    StarRegion(IStar first) {
        super();
        addStar(first);
    }

    void addStar(IStar star) {
        this.add(star);
        star.setRegion(this);
        final Coord at = star.getStarPos();
        if (bounds == null) {
            bounds = SpaceUtil.createAABB(at, at);
        } else {
            SpaceUtil.include(bounds, at);
        }
        updateInfo();
    }

    @Override
    public void removeStar(IStar star) {
        this.remove(star);
        star.setRegion(null);
        updateInfo();
    }

    void updateInfo() {
        draw_bounds = null;
        calcDensity();
    }

    void calcDensity() {
        density = this.size() / (float) SpaceUtil.getVolume(bounds);
    }

    void updateDrawBounds() {
        if (bounds == null) {
            draw_bounds = null;
            return;
        }
        draw_bounds = bounds.expand(2, 2, 2).addCoord(1, 1, 1);
    }

    boolean isFull() {
        return size() >= MAX_SIZE;
    }

    float getPoachCost() {
        if (size() == 1) return 0F;
        return 0.02F; // Poach cost increases if we've got a display list
    }

    private static boolean inRange(Coord a, Coord b) {
        return a.distanceManhatten(b) < MAX_LENGTH * 3;
    }

    boolean couldPoachRegion(StarRegion other) {
        return density > other.density * (1 + getPoachCost() + other.getPoachCost());
    }

    boolean couldEat(IStar star) {
        final Coord starPos = star.getStarPos();
        AxisAlignedBB blob = bounds.addCoord(starPos.x, starPos.y, starPos.z);
        if (blob.maxX - blob.minX > MAX_LENGTH) return false;
        if (blob.maxY - blob.minY > MAX_LENGTH) return false;
        if (blob.maxZ - blob.minZ > MAX_LENGTH) return false;
        return true;
    }

    Coord myCom; // Not a true COM, hmm? Hopefully it's fine.
    void tick(Iterable<StarRegion> others) {
        if (isFull()) return;
        myCom = get(0).getStarPos(); // Could also pick a random one. Nah.
        for (Iterator<StarRegion> iterator = others.iterator(); iterator.hasNext(); ) {
            StarRegion other = iterator.next();
            if (other == this) {
                if (this.isEmpty()) {
                    iterator.remove();
                    break;
                }
                continue;
            }
            if (!couldPoachRegion(other)) continue;
            Coord otherCom = other.get(0).getStarPos();
            if (!inRange(myCom, otherCom)) continue;
            if (consume(other)) {
                if (other.isEmpty()) {
                    iterator.remove();
                } else {
                    other.updateInfo();
                }
            }
        }
        myCom = null;
    }

    boolean consume(StarRegion other) {
        boolean any = false;
        Collections.sort(other, this);
        for (Iterator<IStar> dinner = other.iterator(); dinner.hasNext(); ) {
            if (isFull()) return any;
            IStar tasty = dinner.next();
            if (couldEat(tasty)) {
                dinner.remove();
                addStar(tasty);
                any = true;
            }
        }
        return any;
    }

    @Override
    public int compare(IStar a, IStar b) {
        double da = myCom.distanceSq(a.getStarPos());
        double db = myCom.distanceSq(b.getStarPos());
        if (da > db) return +1;
        if (da < db) return -1;
        return 0;
    }

    public void draw(BulkRender bulkRender, Frustrum frustum) {
        if (isEmpty()) return;
        if (draw_bounds == null) {
            updateDrawBounds();
        }
        if (!frustum.isBoundingBoxInFrustum(draw_bounds)) return;
        for (IStar star : this) {
            star.draw(bulkRender);
        }
    }

    @Override
    public void dirtyStar(IStar star) {
        // Unused
    }
}
