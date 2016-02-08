package factorization.mechanics;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.fzds.BasicTransformOrder;
import factorization.fzds.DeltaChunk;
import factorization.fzds.DimensionSliceEntity;
import factorization.fzds.NullOrder;
import factorization.fzds.interfaces.DimensionSliceEntityBase;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.Interpolation;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.shared.FzModel;
import factorization.sockets.ISocketHolder;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class SocketPoweredCrank extends TileEntitySocketBase implements IChargeConductor, IDCController, ITickable {
    private Charge charge = new Charge(this);
    final EntityReference<DimensionSliceEntityBase> hookedIdc = MechanicsController.autoJoin(this);
    Vec3 hookLocation = SpaceUtil.newVec();
    DeltaCoord hookDelta = new DeltaCoord();
    byte powerTime = 0;

    static final float sprocketRadius = 8F / 16F;


    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_POWERED_CRANK;
    }

    @Override
    public ItemStack getCreatingItem() {
        return Core.registry.dark_iron_sprocket;
    }

    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_BARE_MOTOR;
    }

    @Override
    public boolean onAttacked(IDimensionSlice idc, DamageSource damageSource, float damage) { return false; }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        charge = data.as(Share.PRIVATE, "charge").putIDS(charge);
        data.as(Share.VISIBLE, "hookedEntity");
        hookedIdc.serialize(prefix, data);
        hookLocation = data.as(Share.VISIBLE, "hookLocation").putVec3(hookLocation);
        if (data.isReader() && chainDraw != null) {
            chainDraw.release();
            chainDraw = null;
            chainDelta = 0;
        }
        hookDelta = data.as(Share.PRIVATE, "hookDelta").putIDS(hookDelta);
        powerTime = data.as(Share.PRIVATE, "powerTime").putByte(powerTime);
        return this;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        if (!hookedIdc.trackingEntity()) return "Not connected";
        if (hookedIdc.getEntity() == null) return "Chained (unloaded)";
        return "Chained";
    }

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        hookedIdc.setWorld(world);
    }

    @Override
    public void update() {
        charge.update();
    }

    void shareCharge() {
        Coord anchorPoint = getAnchorBlock();
        if (anchorPoint == null) return;
        IChargeConductor friendConductor = anchorPoint.getTE(IChargeConductor.class);
        if (friendConductor == null) return;
        final Charge friend = friendConductor.getCharge();
        int total = charge.getValue() + friend.getValue();
        int split = total / 2;
        int rem = total % 2;
        int mine = split + rem;
        int his = split;
        charge.setValue(mine);
        friend.setValue(his);
    }

    static final double MAX_CHAIN_LEN = 24;
    static final double MIN_CHAIN_LEN = 1;
    static final double BROKEN_CHAIN_LENGTH = MAX_CHAIN_LEN + 8;
    static final int WINDING_CHARGE_COST = 16;
    static final double FORCE_PER_TICK = 0.05;
    static final double RESTORATIVE_FORCE_MIN = 0.05 / FORCE_PER_TICK;
    static final double RESTORATIVE_FORCE_MAX = 0.10 / FORCE_PER_TICK;

    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        updateChain(socket == null ? this : socket);
        if (worldObj.isRemote) {
            return;
        }
        IDimensionSlice idc = hookedIdc.getEntity();
        if (idc == null) return;
        if (chainLen > BROKEN_CHAIN_LENGTH) {
            breakChain();
            return;
        }
        shareCharge();
        if (chainLen < MIN_CHAIN_LEN) return;
        if (chainLen >= MAX_CHAIN_LEN) {
            yoinkChain(socket, idc, 1.0 / 16.0);
            return;
        }
        if (powered && powerTime >= 0) {
            if (charge.tryTake(10) == 0) {
                if (powerTime > 5) {
                    NullOrder.give(idc);
                    powerTime = 4;
                    return;
                }
                powerTime++; // keep sort of consistent behavior even when out of power
                return;
            }
        }
        if (!powered) {
            if (5 > powerTime && powerTime > 0) {
                // Brief pulse: wind anchor in 1 block
                yankChain(socket, idc, true);
            } else if (powerTime > 0 && powerTime != -1) {
                // Long pulse has been interrupted
                // powerTime == -1 --> already cancelled, so another one should not be done.
                NullOrder.give(idc);
            }
            powerTime = 0;
        } else if (powerTime == 5) {
            // Long pulse: Pull it all the way towards us, stopping when the signal does
            yankChain(socket, idc, false);
            powerTime++;
        } else if (powered && powerTime >= 0) {
            if (powerTime > 5 && !idc.hasOrders()) {
                // Rotation completed; stop so that we aren't irrelevantly canceling rotation
                powerTime = -1;
                return;
            }
            powerTime++;
        }
    }

    private void yoinkChain(ISocketHolder socket, IDimensionSlice idc, double targetSpeed) {
        Vec3 force = getForce(idc, socket, targetSpeed);
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), hookLocation);
        MechanicsController.push(idc, at, force);
    }

    private void yankChain(ISocketHolder socket, IDimensionSlice idc, boolean singleBlock) {
        TileEntityHinge hinge = getHinge(idc);
        if (hinge == null) {
            yoinkChain(socket, idc, 1.0 / 16.0);
            return;
        }
        if (new Coord(hinge).isWeaklyPowered()) {
            powerTime = -1;
            return;
        }
        Vec3 rotationAxis = hinge.getRotationAxis();
        TransformData<Pure> transform = idc.getTransform();
        Quaternion rot = transform.getRot();
        Quaternion min = getMinimizedRotation(idc, hinge, rotationAxis);
        double dtheta = rot.getAngleBetween(min);
        double r = SpaceUtil.subtract(idc.shadow2real(hookLocation), transform.getPos()).lengthVector();
        double deltaC = dtheta * r;
        double totalC = deltaC;
        if (singleBlock && deltaC > 1) {
            deltaC = 1;
        }
        if (deltaC == 0) return;
        double I = hinge.getInertia(idc, rotationAxis);
        I = NumUtil.clip(I, 100, 5000);
        double radiansPerTick = 10 / I;
        double t = deltaC / totalC;
        Quaternion target = rot.shortSlerp(min, t);
        int ticks = (int) (deltaC / radiansPerTick);
        ticks = NumUtil.clip(ticks, 10, (int) (20 * 10 * totalC));
        Interpolation interp = singleBlock ? Interpolation.SMOOTH : Interpolation.LINEAR;
        BasicTransformOrder.give(idc, target, ticks, interp);
    }

    private TileEntityHinge getHinge(IDimensionSlice idc) {
        IDCController controller = idc.getController();
        if (controller instanceof MechanicsController) {
            MechanicsController mc = (MechanicsController) controller;
            for (IDCController constraint : mc.getConstraints()) {
                if (constraint instanceof TileEntityHinge) {
                    return (TileEntityHinge) constraint;
                }
            }
        } else if (controller instanceof TileEntityHinge) {
            return (TileEntityHinge) controller;
        }
        return null;
    }

    private Quaternion getMinimizedRotation(IDimensionSlice idc, TileEntityHinge hinge, Vec3 rotationAxis) {
        // Return the rotation that happens when the anchor point is pointing at us as much as possible.
        Vec3 idcPos = idc.getTransform().getPos();
        Vec3 anchorVec;
        {
            Vec3 com = idc.real2shadow(idcPos);
            anchorVec = SpaceUtil.subtract(hookLocation, com).normalize();
        }
        Vec3 minVec;
        {
            Vec3 me = new Coord(this).toMiddleVector();
            Vec3 vec = SpaceUtil.subtract(me, idcPos);
            Vec3 mask = new Vec3(
                    rotationAxis.xCoord == 0 ? 1 : 0,
                    rotationAxis.yCoord == 0 ? 1 : 0,
                    rotationAxis.zCoord == 0 ? 1 : 0);
            vec = SpaceUtil.componentMultiply(vec, mask);
            minVec = vec.normalize();
        }

        double angle = SpaceUtil.getAngle(anchorVec, minVec);
        return Quaternion.getRotationQuaternionRadians(angle, rotationAxis);
    }

    private Vec3 getForce(IDimensionSlice idc, ISocketHolder socket, double targetSpeed) {
        Vec3 realHookLocation = idc.shadow2real(hookLocation);
        Vec3 selfPos = socket.getServoPos();
        Vec3 chainVec = SpaceUtil.subtract(realHookLocation, selfPos).normalize();
        return SpaceUtil.scale(chainVec, -targetSpeed);
    }

    private Vec3 limitForce(IDimensionSlice idc, Vec3 force, double targetSpeed) {
        DimensionSliceEntity dse = (DimensionSliceEntity) idc;
        Vec3 realHookLocation = idc.shadow2real(hookLocation);
        Vec3 inst = dse.getInstantaneousRotationalVelocityAtPointInCornerSpace(realHookLocation);
        return new Vec3(
                c(inst.xCoord, force.xCoord),
                c(inst.yCoord, force.yCoord),
                c(inst.zCoord, force.zCoord));
    }

    private static double c(double inst, double force) {
        if (inst == 0 || force == 0) return force; // Can of course push fully; or we weren't actually going to push.
        else if (inst > 0 && force > 0) {
            double d = force - inst;
            if (d < 0) return 0; // Already moving quite fast. Don't continue to accelerate
            return d; // Already moving somewhat. Don't accelerate fully.
        } else if (inst < 0 && force < 0) {
            double d = inst - force;
            if (d < 0) return d; // There's already some motion
            return 0; // There's already too much motion
        } else {
            // Pointing in opposite directions. Full throttle.
            return force;
        }
    }

    public void setChain(IDimensionSlice idc, Vec3 hookLocation, Coord hookedBlock) {
        if (hookedIdc.trackingEntity()) {
            getCoord().spawnItem(Core.registry.darkIronChain);
        }
        hookedIdc.trackEntity(idc.getEntity());
        this.hookLocation = hookLocation;
        hookDelta = hookedBlock.asDeltaCoord();
        getCoord().syncTE();
        MechanicsController.register(idc, this);
        updateComparator();
    }

    public boolean isChained() {
        return hookedIdc.trackingEntity();
    }

    public boolean breakChain() {
        if (!isChained()) return false;
        IDimensionSlice idc = hookedIdc.getEntity();
        if (idc == null) {
            return true;
        }
        MechanicsController.deregister(idc, this);
        hookedIdc.trackEntity(null);
        final Coord at = getCoord();
        at.spawnItem(new ItemStack(Core.registry.darkIronChain));
        at.syncTE();
        updateComparator();
        chainDelta = 0;
        return false;
    }

    @Override
    public void uninstall() {
        breakChain();
    }

    void updateComparator() {
        int orig = compareValue;
        calcComparator();
        if (compareValue != orig) {
            markDirty();
        }
    }

    void calcComparator() {
        if (!isChained()) {
            compareValue = 0;
            return;
        }
        if (chainLen >= MAX_CHAIN_LEN - 1) {
            compareValue = 0xF;
        } else {
            int l = (int) (chainLen / 2);
            compareValue = l;
            if (compareValue < 0) compareValue = 1;
            if (compareValue > 0xE) compareValue = 0xE;
            compareValue = 0xE - compareValue;
        }
    }

    int compareValue;
    @Override
    public int getComparatorValue(EnumFacing side) {
        return compareValue;
    }


    ChainLink chainDraw;
    float chainLen, prevChainLen;
    double chainDelta = 0;
    boolean soundActive;
    byte spinSign = +1;

    void updateChain(ISocketHolder socket) {
        IDimensionSlice idc = hookedIdc.getEntity();
        if (idc == null) return;
        Vec3 realHookLocation = idc.shadow2real(hookLocation);
        Vec3 selfPos = socket.getServoPos();
        Vec3 chainVec = SpaceUtil.subtract(realHookLocation, selfPos);
        Vec3 point = SpaceUtil.fromDirection(facing);
        Vec3 right = SpaceUtil.scale(point.crossProduct(chainVec).normalize(), sprocketRadius);
        spinSign = (byte) (SpaceUtil.sum(right) > 0 ? +1 : -1);
        selfPos = selfPos.add(right);
        float len = (float) SpaceUtil.lineDistance(selfPos, realHookLocation);
        if (worldObj.isRemote) {
            setChainDraw(realHookLocation, selfPos, len);
        } else {
            chainLen = len;
        }
    }

    @SideOnly(Side.CLIENT)
    private void setChainDraw(Vec3 realHookLocation, Vec3 selfPos, float len) {
        boolean first = false;
        if (chainDraw == null) {
            chainDraw = ChainRender.instance.add();
            first = true;
        }
        chainDraw.update(selfPos, realHookLocation);
        if (first) {
            chainLen = prevChainLen = len;
        } else {
            chainDelta += len - prevChainLen;
            prevChainLen = chainLen;
            chainLen = len;
        }
        if (soundActive) {
            chainDelta /= 2;
            if (Math.abs(chainDelta) < 0.0001) chainDelta = 0;
            return;
        }

        double min = 0.15;
        byte direction = 0;
        if (chainDelta < -min) {
            direction = -1;
        } else if (chainDelta > min) {
            direction = +1;
        } else {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new WinchSound(direction, this));
    }

    @Override
    public Vec3 getServoPos() {
        double d = 0.5;
        return new Vec3(pos.getX() + d, pos.getY() + d, pos.getZ() + d);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        onChunkUnload();
    }

    @Override
    public void onChunkUnload() {
        if (chainDraw != null) {
            chainDraw.release();
            chainDraw = null;
            chainDelta = 0;
        }
    }

    static FzModel socketModel = new FzModel("sprocket/socket");

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        super.renderTesr(motor, partial);
        float sprocketTheta = 0;
        if (chainDraw != null) {
            float len = NumUtil.interp(prevChainLen, chainLen, partial);
            sprocketTheta = len / sprocketRadius;
        }


        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        GL11.glTranslatef(0, -5F/64F, 0);
        GL11.glScalef(1, 2.5F, 1);


        GL11.glRotated(Math.toDegrees(sprocketTheta), 0, 1, 0);
        //TileEntityGrinderRender.renderGrindHead();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glTranslatef(-0.5F, -0.5F, -0F);
        socketModel.draw();
    }

    Coord getAnchorBlock() {
        IDimensionSlice idc = hookedIdc.getEntity();
        if (idc == null) return null;
        return new Coord(idc.getMinCorner().w, 0, 0, 0).add(hookDelta);
    }

    @Override
    public boolean placeBlock(IDimensionSlice idc, EntityPlayer player, Coord at) {
        InertiaCalculator.dirty(idc);
        return false;
    }

    @Override
    public boolean breakBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        Coord anchorPoint = getAnchorBlock();
        if (anchorPoint == null) return false;
        if (anchorPoint.equals(at)) {
            breakChain();
        }
        InertiaCalculator.dirty(idc);
        return false;
    }

    @Override
    public boolean hitBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        return false;
    }

    @Override
    public boolean useBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) {
        return false;
    }

    @Override
    public void idcDied(IDimensionSlice idc) {

    }

    @Override
    public void beforeUpdate(IDimensionSlice idc) {

    }

    @Override
    public void afterUpdate(IDimensionSlice idc) {
        if (idc.hasOrders() || !idc.getVel().isZero()) {
            updateComparator();
        }
    }

    @Override
    public CollisionAction collidedWithWorld(World realWorld, AxisAlignedBB realBox, World shadowWorld, AxisAlignedBB shadowBox) {
        return CollisionAction.STOP_BEFORE;
    }
}
