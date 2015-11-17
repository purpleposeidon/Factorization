package factorization.servo.stepper;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.fzds.DimensionSliceEntity;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.EntityFz;
import factorization.shared.EntityReference;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class EntityGrabController extends EntityFz implements IDCController {
    private static final long WAIT_TIME = 20 * 30;
    final EntityReference<Entity> holderRef = new EntityReference<Entity>();
    final EntityReference<IDeltaChunk> idcRef = new EntityReference<IDeltaChunk>().whenFound(new EntityReference.OnFound<IDeltaChunk>() {
        @Override
        public void found(IDeltaChunk ent) {
            ent.setController(EntityGrabController.this);
        }
    });
    DropMode dropMode = DropMode.EVENTUALLY;
    long unheldTime = 0;
    boolean needs_future_glue_applied = true;

    public EntityGrabController(World w) {
        super(w);
    }

    public EntityGrabController(Entity holder, DropMode dropMode) {
        super(holder.worldObj);
        this.holderRef.trackEntity(holder);
        this.dropMode = dropMode;
        SpaceUtil.toEntPos(this, SpaceUtil.fromEntPos(holder));
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        holderRef.serialize("holder", data);
        idcRef.serialize("idc", data);
        dropMode = data.as(Share.PRIVATE, "dropMode").putEnum(dropMode);
        unheldTime = data.as(Share.PRIVATE, "unheldTime").putLong(unheldTime);
        needs_future_glue_applied = data.as(Share.PRIVATE, "need4glue").putBoolean(needs_future_glue_applied);
    }

    @Override
    protected void entityInit() {

    }

    public void release() {
        holderRef.trackEntity(null);
    }

    public boolean isUngrabbed() {
        return !holderRef.trackingEntity();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        IDeltaChunk idc = idcRef.getEntity();
        Entity holder = holderRef.getEntity();
        if (idc != null && !holderRef.trackingEntity()) {
            if (unheldTime++ > WAIT_TIME && unheldTime % 20 == 0) {
                drop(idc);
            }
        } else {
            unheldTime = 0;
        }
        if (holder != null && holder.isDead) {
            holderRef.trackEntity(null);
        }
        if (idc != null && idc.isDead) {
            setDead();
        }
    }

    public static void drop(final IDeltaChunk idc) {
        final Coord min = idc.getCorner();
        final Coord max = idc.getFarCorner();
        ForgeDirection up = ForgeDirection.UP;
        ForgeDirection south = ForgeDirection.SOUTH;
        ForgeDirection east = ForgeDirection.EAST;
        final Coord realStart = idc.shadow2realCoord(min);
        DeltaCoord range = max.difference(min);

        // Rotate our axiis
        final Quaternion rot = idc.getRotation();
        ArrayList<ForgeDirection> known = new ArrayList<ForgeDirection>();
        Collections.addAll(known, ForgeDirection.VALID_DIRECTIONS);
        up = SpaceUtil.rotateDirectionAndExclude(up, rot, known);
        south = SpaceUtil.rotateDirectionAndExclude(south, rot, known);
        east = SpaceUtil.rotateDirectionAndExclude(east, rot, known);

        // Find best place to drop
        int bestScore = Integer.MAX_VALUE;
        DeltaCoord bestShift = null;
        for (DeltaCoord shift : DeltaCoord.directNeighborsPlusMe) {
            IdcDropper dropper = new IdcDropper(up, south, east, min, realStart.add(shift), range, true /* break source block on collision w/ destination */);
            int score = dropper.drop(true);
            if (score < bestScore) {
                bestScore = score;
                bestShift = shift;
            }
        }

        // Drop, move any items that fell from block collisions
        IdcDropper dropper = new IdcDropper(up, south, east, min, realStart.add(bestShift), range, true /* break source block on collision w/ destination */);
        if (dropper.drop(false) > 0) {
            ((DimensionSliceEntity) idc).removeItemEntities();
        }
        idc.setDead();
    }

    transient AxisAlignedBB hitReal, hitShadow;

    @Override public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) { return true; }
    @Override public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return true; }
    @Override public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return true; }
    @Override public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return true; }
    @Override public void beforeUpdate(IDeltaChunk idc) { }
    @Override public void afterUpdate(IDeltaChunk idc) { }
    @Override public boolean onAttacked(IDeltaChunk idc, DamageSource damageSource, float damage) { return false; }
    @Override public CollisionAction collidedWithWorld(World realWorld, AxisAlignedBB realBox, World shadowWorld, AxisAlignedBB shadowBox) {
        hitReal = realBox.copy();
        hitShadow = shadowBox.copy();
        return CollisionAction.STOP_INSIDE;
    }

    @Override
    public void idcDied(IDeltaChunk idc) {
        setDead();
    }
}
