package factorization.mechanisms;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
import factorization.shared.*;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.List;

import static factorization.util.SpaceUtil.*;
import static factorization.util.SpaceUtil.fromDirection;
import static org.lwjgl.opengl.GL11.*;

public class TileEntityHinge extends TileEntityCommon implements IDCController {
    FzOrientation facing = FzOrientation.FACE_EAST_POINT_DOWN;
    final EntityReference<IDeltaChunk> idcRef = new EntityReference<IDeltaChunk>();
    double inertia = -1.0;
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
        facing = SpaceUtil.getOrientation(player, side, hitX, hitY, hitZ);
    }

    @Override
    public void neighborChanged() {
        initHinge();
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
        DeltaCoord size = new DeltaCoord(16, 16, 16);
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

        Vec3 idcPos = SpaceUtil.fromEntPos(idc);
        Vec3 com = idc.getRotationalCenterOffset();

        final int faceSign = sign(facing.facing);
        final int topSign = sign(facing.top);
        Vec3 half = fromDirection(facing.facing);
        half = scale(half, 0.5 * faceSign);
        if (topSign > 0) {
            half = add(half, fromDirection(facing.top));
        }

        com = SpaceUtil.add(com, half);
        idc.setRotationalCenterOffset(com);
        SpaceUtil.toEntPos(idc, SpaceUtil.add(idcPos, half));

        Coord dest = idc.getCenter();
        DeltaCoord hingePoint = dest.difference(idc.getCorner());
        worldObj.spawnEntityInWorld(idc);
        idc.setController(this);
        idcRef.trackEntity(idc);
        markDirty();
        getCoord().syncTE();
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        facing = data.as(Share.VISIBLE, "facing").putEnum(facing);
        data.as(Share.VISIBLE, "ref").put(idcRef);
        inertia = data.as(Share.PRIVATE, "inertia").putDouble(inertia);
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
    public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        dirtyInertia();
        return false;
    }

    @Override
    public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        dirtyInertia();
        return false;
    }

    @Override public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }

    double getInertia(IDeltaChunk idc, Vec3 rotationAxis) {
        if (inertia >= 0) return inertia;
        IntertiaCalculator ic = new IntertiaCalculator(idc, rotationAxis);
        inertia = ic.calculate();
        if (inertia < 20) inertia = 20; // Don't go too crazy
        return inertia;
    }

    void dirtyInertia() {
        inertia = -1;
    }

    @Override
    public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        if (player.isSneaking()) return false;
        if (worldObj.isRemote) return false;
        if (getCoord().isPowered()) return false;
        double forceMultiplier = 1;
        if (player.isSprinting()) {
            forceMultiplier *= 2;
        }
        if (!player.onGround) {
            forceMultiplier /= 3;
        }
        forceMultiplier *= PlayerUtil.getPuntStrengthMultiplier(player);
        Vec3 topVec = SpaceUtil.fromDirection(facing.top);
        Vec3 faceVec = SpaceUtil.fromDirection(facing.facing);
        Vec3 rotationAxis = topVec.crossProduct(faceVec);
        double I = getInertia(idc, rotationAxis);

        Vec3 force = player.getLookVec().normalize();

        Vec3 hitBlock = at.createVector();

        Vec3 idcCorner = idc.getCorner().createVector();
        Vec3 idcRot = SpaceUtil.add(idcCorner, idc.getRotationalCenterOffset());

        Vec3 leverArm = SpaceUtil.subtract(hitBlock, idcRot);

        SpaceUtil.incrScale(force, 2.0 * forceMultiplier / I);

        Vec3 torque = leverArm.crossProduct(force);

        if (rotationAxis.xCoord + rotationAxis.yCoord + rotationAxis.zCoord < 0) {
            SpaceUtil.incrScale(rotationAxis, -1);
        }

        SpaceUtil.incrComponentMultiply(torque, rotationAxis);

        Quaternion qx = Quaternion.getRotationQuaternionRadians(torque.xCoord, ForgeDirection.EAST);
        Quaternion qy = Quaternion.getRotationQuaternionRadians(torque.yCoord, ForgeDirection.UP);
        Quaternion qz = Quaternion.getRotationQuaternionRadians(torque.zCoord, ForgeDirection.SOUTH);

        Quaternion dOmega = qx.multiply(qy).multiply(qz);

        Quaternion origOmega = idc.getRotationalVelocity();
        Quaternion newOmega = origOmega.multiply(dOmega);
        newOmega.incrNormalize();

        idc.setRotationalVelocity(newOmega);
        return false;
    }

    @Override
    public void idcDied(IDeltaChunk idc) {
        idcRef.trackEntity(null);
        inertia = -1;
        markDirty();
        getCoord().syncTE();
    }

    IDeltaChunk getIdc() {
        return idcRef.getEntity();
    }

    @Override
    public void beforeUpdate(IDeltaChunk idc) {
        idc_ticking = true;
        Quaternion rotVel = idc.getRotationalVelocity();
        if (rotVel.isZero()) return;
        Quaternion dampened = rotVel.slerp(new Quaternion(1, 0, 0, 0), 0.05);
        idc.setRotationalVelocity(dampened);
    }

    @Override
    public void afterUpdate(IDeltaChunk idc) {
        idc_ticking = false;
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
                idc.setController(this);
            }
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        IDeltaChunk idc = idcRef.getEntity();
        if (idc == null) return;
        if (idc.hasOrderedRotation()) return;
        Quaternion w = idc.getRotationalVelocity();
        if (!w.isZero()) {
            double theta = w.getAngleBetween(new Quaternion());
            if (theta > 0.001) return;
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
        idc.orderTargetRotation(align, 10, Interpolation.INV_SQUARE);
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

                if (SpaceUtil.sign(facing.top) == +1) topd = 1;

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
}
