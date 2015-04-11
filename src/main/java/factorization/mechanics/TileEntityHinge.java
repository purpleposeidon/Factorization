package factorization.mechanics;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.notify.Notice;
import factorization.shared.*;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.List;

import static factorization.util.SpaceUtil.*;
import static org.lwjgl.opengl.GL11.*;

public class TileEntityHinge extends TileEntityCommon implements IDCController {
    FzOrientation facing = FzOrientation.FACE_EAST_POINT_DOWN;
    final EntityReference<IDeltaChunk> idcRef = MechanicsController.autoJoin(this);
    Vec3 dseOffset = SpaceUtil.newVec();
    transient boolean idc_ticking = false;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HINGE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    @Override
    public void setWorldObj(World w) {
        super.setWorldObj(w);
        idcRef.setWorld(w);
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        facing = getOrientation(player, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return ForgeDirection.getOrientation(side) == facing.facing.getOpposite();
    }

    @Override
    public void neighborChanged() {
        if (idcRef.trackingEntity()) {
            IDeltaChunk idc = idcRef.getEntity();
            if (idc != null && getCoord().isWeaklyPowered()) {
                idc.setRotationalVelocity(new Quaternion());
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
        IDeltaChunk idc = DeltaChunk.makeSlice(MechanismsFeature.deltachunk_channel, min, max, new DeltaChunk.AreaMap() {
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
        // idc.forbid(DeltaCapability.COLLIDE_WITH_WORLD);

        Vec3 idcPos = fromEntPos(idc);

        final int faceSign = sign(facing.facing);
        final int topSign = sign(facing.top);
        Vec3 half = fromDirection(facing.facing);
        half = scale(half, 0.5 * faceSign);
        if (topSign > 0) {
            half = add(half, fromDirection(facing.top));
        }

        Vec3 com = idc.getRotationalCenterOffset();
        com = add(com, half);
        idc.setRotationalCenterOffset(com);
        toEntPos(idc, add(idcPos, half));

        Coord dest = idc.getCenter();
        DeltaCoord hingePoint = dest.difference(idc.getCorner());
        worldObj.spawnEntityInWorld(idc);
        MechanicsController.register(idc, this);
        idcRef.trackEntity(idc);
        updateComparators();
        markDirty();
        getCoord().syncTE();
        dseOffset.xCoord = idc.posX - xCoord;
        dseOffset.yCoord = idc.posY - yCoord;
        dseOffset.zCoord = idc.posZ - zCoord;
    }

    void setProperPosition(IDeltaChunk idc) {
        idc.posX = dseOffset.xCoord + xCoord;
        idc.posY = dseOffset.yCoord + yCoord;
        idc.posZ = dseOffset.zCoord + zCoord;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        facing = data.as(Share.VISIBLE, "facing").putEnum(facing);
        data.as(Share.VISIBLE, "ref").put(idcRef);
        dseOffset = data.as(Share.PRIVATE, "dseOffset").putVec3(dseOffset);
    }

    void setSlabBounds(Block b) {
        float d = 0.5F;
        switch (facing.facing) {
            case DOWN:
                b.setBlockBounds(0, d, 0, 1, 1, 1);
                break;
            case UNKNOWN:
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
    public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) {;
        dirtyInertia();
        return false;
    }

    @Override
    public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        dirtyInertia();
        return false;
    }

    @Override
    public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        if (player.isSneaking()) return false;
        if (worldObj.isRemote) return false;
        final ItemStack held = player.getHeldItem();
        if (held != null && held.getMaxStackSize() > 1) return false;
        if (getCoord().isWeaklyPowered()) return false;
        return applyForce(idc, player, at, -1);
    }

    double getInertia(IDeltaChunk idc, Vec3 rotationAxis) {
        double inertia = InertiaCalculator.getInertia(idc, rotationAxis);
        if (inertia < 20) inertia = 20; // Don't go too crazy
        return inertia;
    }

    void dirtyInertia() {
        IDeltaChunk idc = idcRef.getEntity();
        if (idc == null) return;
        InertiaCalculator.dirty(idc);
    }

    @Override
    public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        if (player.isSneaking()) return false;
        if (worldObj.isRemote) return false;
        return applyForce(idc, player, at, 1);
    }

    private boolean applyForce(IDeltaChunk idc, EntityPlayer player, Coord at, double forceMultiplier) {
        if (getCoord().isWeaklyPowered()) return true;
        if (player.isSprinting()) {
            forceMultiplier *= 2;
        }
        if (!player.onGround) {
            forceMultiplier /= 3;
        }
        forceMultiplier *= PlayerUtil.getPuntStrengthMultiplier(player);
        Vec3 force = player.getLookVec().normalize();
        incrScale(force, forceMultiplier);

        applyForce(idc, at, force);
        return false;
    }

    void applyForce(IDeltaChunk idc, Coord at, Vec3 force) {
        Vec3 rotationAxis = getRotationAxis();
        double I = getInertia(idc, rotationAxis);


        idc.getRotation().applyReverseRotation(force);

        Vec3 hitBlock = at.createVector();

        Vec3 idcCorner = idc.getCorner().createVector();
        Vec3 idcRot = add(idcCorner, idc.getRotationalCenterOffset());

        Vec3 leverArm = subtract(hitBlock, idcRot);

        incrScale(force, 2.0 / I);

        Vec3 torque = leverArm.crossProduct(force);
        idc.getRotation().applyRotation(torque);

        if (rotationAxis.xCoord + rotationAxis.yCoord + rotationAxis.zCoord < 0) {
            incrScale(rotationAxis, -1);
        }

        incrComponentMultiply(torque, rotationAxis);

        Quaternion qx = Quaternion.getRotationQuaternionRadians(torque.xCoord, ForgeDirection.EAST);
        Quaternion qy = Quaternion.getRotationQuaternionRadians(torque.yCoord, ForgeDirection.UP);
        Quaternion qz = Quaternion.getRotationQuaternionRadians(torque.zCoord, ForgeDirection.SOUTH);

        Quaternion dOmega = qx.multiply(qy).multiply(qz);

        Quaternion origOmega = idc.getRotationalVelocity();
        Quaternion newOmega = origOmega.multiply(dOmega);
        newOmega.incrNormalize();

        idc.setRotationalVelocity(newOmega);
        limitBend(idc);
    }

    private Vec3 getRotationAxis() {
        Vec3 topVec = fromDirection(facing.top);
        Vec3 faceVec = fromDirection(facing.facing);
        return topVec.crossProduct(faceVec);
    }

    @Override
    public void idcDied(IDeltaChunk idc) {
        idcRef.trackEntity(null);
        updateComparators();
        markDirty();
        getCoord().syncTE();
    }

    IDeltaChunk getIdc() {
        return idcRef.getEntity();
    }

    static final double min_velocity = Math.PI / 20 / 20 / 20;
    static final double max_velocity = Math.PI / 20;

    boolean isBasicallyZero(Quaternion rotVel) {
        return rotVel.isZero() || rotVel.getAngleRadians() /* Opportunity to algebra our way out of a call to acos here */ < min_velocity;
    }

    @Override
    public void beforeUpdate(IDeltaChunk idc) {
        idc_ticking = true;
        Quaternion rotVel = idc.getRotationalVelocity();
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
        idc.setRotationalVelocity(dampened);
        limitBend(idc);
    }

    private boolean bendMode() {
        return sign(facing.facing.getRotation(facing.top)) == -1;
    }

    private void limitBend(IDeltaChunk idc) {
        if (idc.hasOrderedRotation()) return;
        final Quaternion nextRotation = idc.getRotation().multiply(idc.getRotationalVelocity());
        final Vec3 middle = SpaceUtil.fromDirection(facing.top);
        final Vec3 arm = SpaceUtil.fromDirection(facing.facing);
        nextRotation.applyRotation(arm);
        final double angle = SpaceUtil.getAngle(middle, arm);

        final double end = Math.PI / 2;

        if (angle <= end) return;

        double p = end / angle;
        idc.setRotationalVelocity(new Quaternion());
        idc.setRotation(new Quaternion().slerp(nextRotation, p));
    }

    @Override
    public void afterUpdate(IDeltaChunk idc) {
        idc_ticking = false;
        if (!idc.getRotationalVelocity().isZero() || idc.hasOrderedRotation()) {
            updateComparators();
        }
        if (executing_order && !idc.hasOrderedRotation()) {
            executing_order = false;
        }
        if (worldObj.getTotalWorldTime() % 60 == 0) setProperPosition(idc); // Blame pistons
    }

    @Override
    public boolean addCollisionBoxesToList(Block block, AxisAlignedBB aabb, List list, Entity entity) {
        if (idc_ticking) return true;
        return super.addCollisionBoxesToList(block, aabb, list, entity);
    }

    transient long ticks;

    @Override
    public void updateEntity() {
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
        if (mop.blockX == xCoord && mop.blockY == yCoord && mop.blockZ == zCoord) {
            ticks = 0;
        }
    }

    private void updateServer() {
        if (!idcRef.entityFound() && idcRef.trackingEntity()) {
            IDeltaChunk idc = idcRef.getEntity();
            if (idc != null) {
                MechanicsController.register(idc, this);
                executing_order = idc.hasOrderedRotation();
                updateComparators();
            }
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        if (getCoord().isWeaklyPowered()) return;
        IDeltaChunk idc = idcRef.getEntity();
        if (idc == null) return;
        if (idc.hasOrderedRotation()) return;
        Quaternion w = idc.getRotationalVelocity();
        if (!w.isZero()) {
            idc.setRotationalVelocity(new Quaternion());
        }
        Quaternion rot = idc.getRotation();
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
        idc.orderTargetRotation(align, 10, Interpolation.SQUARE);
        executing_order = true;
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        IDeltaChunk idc = idcRef.getEntity();
        boolean unload = false;
        if (idc != null) {
            if (!isBasicallyZero(idc.getRotationalVelocity()) || !isBasicallyZero(idc.getRotation())) {
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

    @SideOnly(Side.CLIENT)
    public static ObjectModel hingeTop;

    @Override
    @SideOnly(Side.CLIENT)
    public void representYoSelf() {
        super.representYoSelf();
        hingeTop = new ObjectModel(Core.getResource("models/hingeTop.obj"));
    }

    @SideOnly(Side.CLIENT)
    public void renderTesr(float partial) {
        BlockRenderHelper block = BlockRenderHelper.instance;

        if (idcRef.trackingEntity()) {
            IDeltaChunk idc = getIdc();
            if (idc != null) {
                ForgeDirection face = facing.facing, top = facing.top;
                float faced = 0.5F, topd = 0.0F;

                if (sign(facing.top) == +1) topd = 1;
                if (sign(facing.facing) == -1) faced *= -1;

                float dx = face.offsetX * faced + top.offsetX * topd;
                float dy = face.offsetY * faced + top.offsetY * topd;
                float dz = face.offsetZ * faced + top.offsetZ * topd;

                GL11.glTranslatef(dx, dy, dz);
                idc.getRotation().glRotate();
                GL11.glTranslatef(-dx, -dy, -dz);
            }
            setupHingeRotation2();
        } else {
            setupHingeRotation2();
            float nowish = ticks + partial;
            double now = (Math.cos(nowish / 24.0) - 1) * -6;
            GL11.glRotated(now, 0, 0, 1);
        }

        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        glEnable(GL_LIGHTING);
        glDisable(GL11.GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        hingeTop.render(BlockIcons.mechanism$hinge_uvs);
        glEnable(GL11.GL_CULL_FACE);
        glEnable(GL_LIGHTING);
    }

    private void setupHingeRotation2() {
        // Well, it's better than my 125 line first try. I bet Player could do better tho.
        final ForgeDirection face = facing.facing;
        final ForgeDirection top = facing.top;
        final int fsign = face.ordinal() % 2 == 0 ? -1 : +1;
        final int tsign = top.ordinal() % 2 == 0 ? -1 : +1;
        float dx = 0, dy = 0, dz = 0;
        if (tsign == +1) {
            ForgeDirection v = top;
            dx += v.offsetX; dy += v.offsetY; dz += v.offsetZ;
        }
        if (fsign == +1) {
            ForgeDirection v = facing.rotateOnFace(1).top;
            dx += v.offsetX; dy += v.offsetY; dz += v.offsetZ;
        }
        GL11.glTranslatef(dx, dy, dz);
        Quaternion.fromOrientation(facing).glRotate();
        boolean left = false;
        if (face.offsetX != 0) left = top == ForgeDirection.NORTH || top == ForgeDirection.UP;
        if (face.offsetY != 0) left = top == ForgeDirection.WEST || top == ForgeDirection.SOUTH;
        if (face.offsetZ != 0) left = top == ForgeDirection.DOWN || top == ForgeDirection.EAST;

        float dleft = 0.5F;
        if (left) {
            dleft += fsign;
        }
        GL11.glTranslatef(0, 0.5F * fsign, dleft);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        int a = -2, b = +3;
        return AxisAlignedBB.getBoundingBox(xCoord + a, yCoord + a, zCoord + a, xCoord + b, yCoord + b, zCoord + b);
    }

    private transient byte comparator_cache = 0;
    private transient boolean executing_order = false;

    @Override
    public int getComparatorValue(ForgeDirection side) {
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
        IDeltaChunk idc = idcRef.getEntity();
        if (idc == null) return comparator_cache;
        double angle = Math.toDegrees(idc.getRotation().getAngleBetween(new Quaternion()));
        angle /= 90;
        angle = 1 - angle;
        return (byte) (0xF * angle);
    }

    @Override
    public IIcon getIcon(ForgeDirection dir) {
        return Blocks.iron_block.getIcon(0, 0);
    }
}
