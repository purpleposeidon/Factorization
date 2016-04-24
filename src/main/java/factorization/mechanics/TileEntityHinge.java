package factorization.mechanics;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.fzds.BasicTransformOrder;
import factorization.fzds.DeltaChunk;
import factorization.fzds.NullOrder;
import factorization.fzds.interfaces.*;
import factorization.shared.*;
import factorization.util.NumUtil;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.List;

import static factorization.util.SpaceUtil.*;

public class TileEntityHinge extends TileEntityCommon implements IDCController, ITickable {
    FzOrientation facing = FzOrientation.FACE_EAST_POINT_DOWN;
    final EntityReference<DimensionSliceEntityBase> idcRef = MechanicsController.autoJoin(this);
    Vec3 dseOffset = SpaceUtil.newVec();
    transient boolean idc_ticking = false;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HINGE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Socket;
    }

    @Override
    public void setWorldObj(World w) {
        super.setWorldObj(w);
        idcRef.setWorld(w);
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        facing = getOrientation(player, side, new Vec3(hitX, hitY, hitZ));
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return side == facing.facing.getOpposite();
    }

    @Override
    public void neighborChanged() {
        if (worldObj.isRemote) return;
        if (idcRef.trackingEntity()) {
            IDimensionSlice idc = idcRef.getEntity();
            if (idc != null && getCoord().isWeaklyPowered()) {
                idc.getVel().setRot(new Quaternion());
            }
        } else {
            initHinge();
        }
    }

    static ThreadLocal<Boolean> initializing = new ThreadLocal<Boolean>();

    private void initHinge() {
        if (idcRef.trackingEntity()) return;
        if (initializing.get() == Boolean.TRUE) return;
        initializing.set(Boolean.TRUE);
        try {
            initHinge0();
        } finally {
            initializing.remove();
        }
    }

    private void initHinge0() {
        final Coord target = getCoord().add(facing.facing);
        if (target.isReplacable()) return;
        if (target.isBedrock()) return;
        DeltaCoord size = new DeltaCoord(8, 8, 8);
        Coord min = getCoord().add(size.reverse());
        Coord max = getCoord().add(size);
        IDimensionSlice idc = DeltaChunk.makeSlice(MechanismsFeature.deltachunk_channel, min, max, new DeltaChunk.AreaMap() {
            @Override
            public void fillDse(DeltaChunk.DseDestination destination) {
                destination.include(target);
            }
        }, true);

        idc.loadUsualCapabilities();
        idc.permit(DeltaCapability.COLLIDE_WITH_WORLD);
        idc.permit(DeltaCapability.DIE_WHEN_EMPTY);
        idc.permit(DeltaCapability.ENTITY_PHYSICS);
        idc.permit(DeltaCapability.PHYSICS_DAMAGE);
        idc.permit(DeltaCapability.CONSERVE_MOMENTUM);

        final int faceSign = sign(facing.facing);
        final int topSign = sign(facing.top);
        Vec3 half = fromDirection(facing.facing);
        half = scale(half, 0.5 * faceSign);
        if (topSign > 0) {
            half = half.add(fromDirection(facing.top));
        }

        Vec3 com = idc.getTransform().getOffset();
        com = com.add(half);
        idc.getTransform().setOffset(com);
        idc.getTransform().addPos(half);

        idcRef.spawnAndTrack(idc.getEntity());
        MechanicsController.register(idc, this);
        updateComparators();
        markDirty();
        getCoord().syncTE();
        dseOffset = idc.getTransform().getPos().subtract(new Vec3(pos));
    }

    void setProperPosition(IDimensionSlice idc) {
        idc.getTransform().setPos(dseOffset.add(new Vec3(pos)));
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        facing = data.as(Share.VISIBLE, "facing").putEnum(facing);
        data.as(Share.VISIBLE, "ref").putIDS(idcRef);
        dseOffset = data.as(Share.PRIVATE, "dseOffset").putVec3(dseOffset);
    }

    void setSlabBounds(Block b) {
        float d = 0.5F;
        if (facing == null) return;
        switch (facing.facing) {
            case DOWN:
                b.setBlockBounds(0, d, 0, 1, 1, 1);
                break;
            case UP:
                b.setBlockBounds(0, 0, 0, 1, d, 1);
                break;
            case NORTH:
                b.setBlockBounds(0, 0, d, 1, 1, 1);
                break;
            case SOUTH:
                b.setBlockBounds(0, 0, 0, 1, 1, d);
                break;
            case WEST:
                b.setBlockBounds(d, 0, 0, 1, 1, 1);
                break;
            case EAST:
                b.setBlockBounds(0, 0, 0, d, 1, 1);
                break;
        }
    }

    @Override
    public boolean placeBlock(IDimensionSlice idc, EntityPlayer player, Coord at) {;
        dirtyInertia();
        return false;
    }

    @Override
    public boolean breakBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        dirtyInertia();
        return false;
    }

    @Override
    public boolean useBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        if (player.isSneaking()) return false;
        if (worldObj.isRemote) return false;
        final ItemStack held = player.getHeldItem();
        if (held != null && held.getMaxStackSize() > 1) return false;
        if (getCoord().isWeaklyPowered()) return false;
        return applyForce(idc, player, at, -1);
    }

    double getInertia(IDimensionSlice idc, Vec3 rotationAxis) {
        double inertia = InertiaCalculator.getInertia(idc, rotationAxis);
        if (inertia < 20) inertia = 20; // Don't go too crazy
        return inertia;
    }

    void dirtyInertia() {
        IDimensionSlice idc = idcRef.getEntity();
        if (idc == null) return;
        InertiaCalculator.dirty(idc);
    }

    @Override
    public boolean hitBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        if (player.isSneaking()) return false;
        if (worldObj.isRemote) return false;
        return applyForce(idc, player, at, 1);
    }

    private boolean applyForce(IDimensionSlice idc, EntityPlayer player, Coord at, double forceMultiplier) {
        if (getCoord().isWeaklyPowered()) return true;
        if (player.isSprinting()) {
            forceMultiplier *= 2;
        }
        if (!player.onGround) {
            forceMultiplier /= 3;
        }
        forceMultiplier *= PlayerUtil.getPuntStrengthMultiplier(player);
        Vec3 force = player.getLookVec().normalize();
        force = SpaceUtil.scale(force, forceMultiplier);

        applyForce(idc, at, force);
        limitBend(idc);
        limitVelocity(idc);
        return false;
    }

    void applyForce(IDimensionSlice idc, Coord at, Vec3 force) {
        Vec3 rotationAxis = getRotationAxis();
        double I = getInertia(idc, rotationAxis);


        idc.getTransform().getRot().applyReverseRotation(force);

        Vec3 hitBlock = at.createVector();

        Vec3 idcCorner = idc.getMinCorner().createVector();
        Vec3 idcRot = idcCorner.add(idc.getTransform().getOffset());

        Vec3 leverArm = subtract(hitBlock, idcRot);

        force = SpaceUtil.scale(force, 2.0 / I);

        Vec3 torque = leverArm.crossProduct(force);
        idc.getTransform().getRot().applyRotation(torque);

        if (SpaceUtil.sum(rotationAxis) < 0) {
            rotationAxis = SpaceUtil.scale(rotationAxis, -1);
        }

        torque = SpaceUtil.componentMultiply(torque, rotationAxis);

        Quaternion qx = Quaternion.getRotationQuaternionRadians(torque.xCoord, EnumFacing.EAST);
        Quaternion qy = Quaternion.getRotationQuaternionRadians(torque.yCoord, EnumFacing.UP);
        Quaternion qz = Quaternion.getRotationQuaternionRadians(torque.zCoord, EnumFacing.SOUTH);

        Quaternion dOmega = qx.multiply(qy).multiply(qz);

        if (dOmega.getAngleRadians() < min_push_force) {
            dOmega = Quaternion.getRotationQuaternionRadians(min_push_force, SpaceUtil.normalize(dOmega.toVector()));
        }

        Quaternion origOmega = idc.getVel().getRot();
        Quaternion newOmega = origOmega.multiply(dOmega);
        newOmega.incrNormalize();

        idc.getVel().setRot(newOmega);
    }

    Vec3 getRotationAxis() {
        Vec3 topVec = fromDirection(facing.top);
        Vec3 faceVec = fromDirection(facing.facing);
        return topVec.crossProduct(faceVec);
    }

    @Override
    public void idcDied(IDimensionSlice idc) {
        idcRef.trackEntity(null);
        updateComparators();
        markDirty();
        getCoord().syncTE();
    }

    @Override
    public boolean onAttacked(IDimensionSlice idc, DamageSource damageSource, float damage) { return false; }

    @Override
    public CollisionAction collidedWithWorld(World realWorld, AxisAlignedBB realBox, World shadowWorld, AxisAlignedBB shadowBox) {
        return CollisionAction.STOP_BEFORE;
    }

    IDimensionSlice getIdc() {
        return idcRef.getEntity();
    }

    static final double min_velocity = Math.PI / 20 / 20 / 20;
    static final double max_velocity = min_velocity * 100;
    static final double min_push_force = NumUtil.interp(min_velocity, max_velocity, 0.01);

    boolean isBasicallyZero(Quaternion rotVel) {
        return rotVel.isZero() || rotVel.getAngleRadians() /* Opportunity to algebra our way out of a call to acos here */ < min_velocity;
    }

    @Override
    public void beforeUpdate(IDimensionSlice idc) {
        if (idc.hasOrders()) {
            limitBend(idc);
            return;
        }
        idc_ticking = true;
        Quaternion rotVel = idc.getVel().getRot();
        if (rotVel.isZero()) return;
        Quaternion dampened;
        if (isBasicallyZero(rotVel)) {
            dampened = new Quaternion();
        } else {
            double angle = rotVel.getAngleRadians();
            if (angle > max_velocity) {
                Vec3 axis = rotVel.toVector().normalize();
                dampened = Quaternion.getRotationQuaternionRadians(max_velocity, axis);
            } else {
                dampened = rotVel.slerp(new Quaternion(), 0.05);
            }
        }
        idc.getVel().setRot(dampened);
        limitBend(idc);
        limitVelocity(idc);
    }

    private void limitBend(IDimensionSlice idc) {
        final Quaternion rotationalVelocity = idc.getVel().getRot();
        if (!idc.hasOrders() && rotationalVelocity.isZero()) return;
        final Quaternion nextRotation = idc.getTransform().getRot().multiply(rotationalVelocity);
        final Vec3 middle = SpaceUtil.fromDirection(facing.top);
        final Vec3 arm = SpaceUtil.fromDirection(facing.facing);
        nextRotation.applyRotation(arm);
        final double angle = SpaceUtil.getAngle(middle, arm);

        final double end = Math.PI / 2;

        if (angle < end) return;

        if (idc.hasOrders()) {
            NullOrder.give(idc);
        } else {
            idc.getVel().setRot(new Quaternion());
        }
        double t = nextRotation.getAngleRadians();
        if (t < 0) t = -end;
        if (t > Math.PI) t = Math.PI;
        idc.getTransform().setRot(Quaternion.getRotationQuaternionRadians(t, nextRotation.toVector()));
    }

    private void limitVelocity(IDimensionSlice idc) {
        final Quaternion rot = idc.getVel().getRot();
        double rv = rot.getAngleRadians();
        if (rv < max_velocity) return;
        double p = max_velocity / rv;
        idc.getVel().setRot(new Quaternion().slerp(rot, p));
    }

    @Override
    public void afterUpdate(IDimensionSlice idc) {
        idc_ticking = false;
        if (!idc.getVel().getRot().isZero() || idc.hasOrders()) {
            updateComparators();
        }
        if (executing_order && !idc.hasOrders()) {
            executing_order = false;
        }
        if (worldObj.getTotalWorldTime() % 60 == 0) setProperPosition(idc); // Blame pistons
    }

    @Override
    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List<AxisAlignedBB> list, Entity entity) {
        if (idc_ticking) return true;
        return super.addCollisionBoxesToList(block, aabb, list, entity);
    }

    transient long ticks;

    @Override
    public void update() {
        if (worldObj.isRemote) updateClient();
        else updateServer();
    }

    private void updateClient() {
        if (idcRef.trackingEntity()) {
            ticks = 0;
            return;
        }
        ticks++;
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (mop.getBlockPos().equals(pos)) {
            ticks = 0;
        }
    }

    private void updateServer() {
        if (!idcRef.entityFound() && idcRef.trackingEntity()) {
            IDimensionSlice idc = idcRef.getEntity();
            if (idc != null) {
                MechanicsController.register(idc, this);
                executing_order = idc.hasOrders();
                updateComparators();
            }
        }
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        if (getCoord().isWeaklyPowered()) return;
        IDimensionSlice idc = idcRef.getEntity();
        if (idc == null) return;
        if (idc.hasOrders()) return;
        Quaternion w = idc.getVel().getRot();
        if (!w.isZero()) {
            idc.getVel().setRot(new Quaternion());
        }
        Quaternion rot = idc.getTransform().getRot();
        if (rot.hasNaN() || rot.hasInf() || w.hasNaN() || w.hasInf()) {
            idc.getTransform().setRot(new Quaternion());
            idc.getVel().setRot(new Quaternion());
            return;
        }
        Quaternion align;
        double theta = rot.getAngleBetween(new Quaternion());
        if (theta < Math.PI * 0.01) {
            align = new Quaternion();
        } else {
            double d = Math.PI / 8;
            double t = d / theta;
            if (t < 0 || t > 1) t = 1;
            align = rot.slerp(new Quaternion(), t);
        }
        BasicTransformOrder.give(idc, align, 10, Interpolation.SQUARE);
        executing_order = true;
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        IDimensionSlice idc = idcRef.getEntity();
        boolean unload = false;
        if (idc != null) {
            if (!isBasicallyZero(idc.getVel().getRot()) || !isBasicallyZero(idc.getTransform().getRot())) {
                return false;
            }
            unload = true;
        } else if (idcRef.trackingEntity()) {
            return false;
        }
        if (getCoord().isWeaklyPowered()) return false;
        if (worldObj.isRemote) return true;
        boolean ret = super.removedByPlayer(player, willHarvest);
        if (ret && unload) {
            MechanicsController.deregister(idc, this);
        }
        return ret;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        int a = -2, b = +3;
        return new AxisAlignedBB(pos.getX() + a, pos.getY() + a, pos.getZ() + a, pos.getX() + b, pos.getY() + b, pos.getZ() + b);
    }

    private transient byte comparator_cache = 0;
    private transient boolean executing_order = false;

    @Override
    public int getComparatorValue(EnumFacing side) {
        return comparator_cache;
    }

    private void updateComparators() {
        byte new_val = comparatorMeasure();
        if (new_val == comparator_cache) return;
        comparator_cache = new_val;
        markDirty();
    }

    private byte comparatorMeasure() {
        if (!idcRef.trackingEntity()) {
            return 0;
        }
        IDimensionSlice idc = idcRef.getEntity();
        if (idc == null) return comparator_cache;
        double angle = Math.toDegrees(idc.getTransform().getRot().getAngleBetween(new Quaternion()));
        angle /= 90;
        angle = 1 - angle;
        return (byte) (0xF * angle);
    }

    static final FzModel top = new FzModel("hingeTop");
    public void renderTesr(float partial) {
        if (idcRef.trackingEntity()) {
            IDimensionSlice idc = getIdc();
            if (idc != null) {
                EnumFacing face = facing.facing, top = facing.top;
                float faced = 0.5F, topd = 0.0F;

                if (SpaceUtil.sign(facing.top) == +1) topd = 1;
                if (SpaceUtil.sign(facing.facing) == -1) faced *= -1;

                float dx = face.getFrontOffsetX() * faced + top.getFrontOffsetX() * topd;
                float dy = face.getFrontOffsetY() * faced + top.getFrontOffsetY() * topd;
                float dz = face.getFrontOffsetZ() * faced + top.getFrontOffsetZ() * topd;

                GL11.glTranslatef(dx, dy, dz);
                idc.getTransform(partial).getRot().glRotate();
                GL11.glTranslatef(-dx, -dy, -dz);
            }
            setupHingeRotation2();
        } else {
            setupHingeRotation2();
            float nowish = ticks + partial;
            double now = (Math.cos(nowish / 24.0) - 1) * -6;
            GL11.glRotated(now, 0, 0, 1);
        }

        GL11.glRotated(180, 0, 1, 0);
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        top.draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private void setupHingeRotation2() {
        // Well, it's better than my 125 line first try. I bet Player could do better tho.
        final EnumFacing face = facing.facing;
        final EnumFacing top = facing.top;
        final int fsign = face.ordinal() % 2 == 0 ? -1 : +1;
        final int tsign = top.ordinal() % 2 == 0 ? -1 : +1;
        float dx = 0, dy = 0, dz = 0;
        if (tsign == +1) {
            EnumFacing v = top;
            dx += v.getFrontOffsetX();
            dy += v.getFrontOffsetY();
            dz += v.getFrontOffsetZ();
        }
        if (fsign == +1) {
            EnumFacing v = facing.rotateOnFace(1).top;
            dx += v.getFrontOffsetX();
            dy += v.getFrontOffsetY();
            dz += v.getFrontOffsetZ();
        }
        GL11.glTranslatef(dx, dy, dz);
        Quaternion.fromOrientation(facing).glRotate();
        boolean left = false;
        if (face.getFrontOffsetX() != 0) left = top == EnumFacing.NORTH || top == EnumFacing.UP;
        if (face.getFrontOffsetY() != 0) left = top == EnumFacing.WEST || top == EnumFacing.SOUTH;
        if (face.getFrontOffsetZ() != 0) left = top == EnumFacing.DOWN || top == EnumFacing.EAST;

        float dleft = 0.5F;
        if (left) {
            dleft += fsign;
        }
        GL11.glTranslatef(0, 0.5F * fsign, dleft);
    }
}
