package factorization.mechanics;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.NORELEASE;
import factorization.sockets.ISocketHolder;
import factorization.sockets.SocketBareMotor;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class SocketPoweredCrank extends TileEntitySocketBase implements IChargeConductor, IDCController {
    private Charge charge = new Charge(this);
    final EntityReference<IDeltaChunk> hookedIdc = MechanicsController.autoJoin(this);
    Vec3 hookLocation = SpaceUtil.newVec();
    DeltaCoord hookDelta = new DeltaCoord();

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
    public boolean canUpdate() {
        return true;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        charge = data.as(Share.PRIVATE, "charge").put(charge);
        data.as(Share.VISIBLE, "hookedEntity");
        hookedIdc.serialize(prefix, data);
        hookLocation = data.as(Share.VISIBLE, "hookLocation").putVec3(hookLocation);
        if (data.isReader() && chainDraw != null) {
            chainDraw.release();
            chainDraw = null;
            chainDelta = 0;
        }
        hookDelta = data.as(Share.PRIVATE, "hookDelta").put(hookDelta);
        return this;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        if (!hookedIdc.trackingEntity()) return "Not connected";
        if (hookedIdc.getEntity() == null) return "Chained (but IDC not found)";
        return "Chained";
    }

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        hookedIdc.setWorld(world);
    }

    @Override
    public void updateEntity() {
        charge.update();
        super.updateEntity();
    }

    void shareCharge() {
        Coord anchorPoint = getAnchorBlock();
        if (anchorPoint == null) return;
        IChargeConductor friend = anchorPoint.getTE(IChargeConductor.class);
        if (friend == null) return;
        int mine = charge.getValue();
        int his = friend.getCharge().getValue();
        int give = mine - his;
        if (give <= 0) return;
        charge.deplete(give);
        friend.getCharge().addValue(give);
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
        IDeltaChunk idc = hookedIdc.getEntity();
        if (idc == null) return;
        if (chainLen > BROKEN_CHAIN_LENGTH) {
            breakChain();
            return;
        }
        shareCharge();
        if (chainLen < MIN_CHAIN_LEN) return;
        boolean hyperExtended = chainLen >= MAX_CHAIN_LEN;
        if (!powered && !hyperExtended) return;
        if (!hyperExtended) {
            // Consume charge
            if (charge.tryTake(WINDING_CHARGE_COST) == 0) {
                return;
            }
        }
        // retract
        double scale = 1;
        if (hyperExtended) {
            double r = NumUtil.uninterp(MAX_CHAIN_LEN, BROKEN_CHAIN_LENGTH, chainLen);
            scale = NumUtil.interp(RESTORATIVE_FORCE_MIN, RESTORATIVE_FORCE_MAX, r);
        } else if (socket == this) {
            double power = coord.getPowerInput();
            scale = (1 + power) / 16.0;
            if (power == 0 /* getting indrect power */) {
                scale = 1;
            }
        } else {
            scale = 1.0 / 32.0; // Servos don't look very sturdy
        }
        Vec3 force = getForce(idc, socket, scale);
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), hookLocation);
        MechanicsController.push(idc, at, force);
    }

    private Vec3 getForce(IDeltaChunk idc, ISocketHolder socket, double scale) {
        Vec3 realHookLocation = idc.shadow2real(hookLocation);
        Vec3 selfPos = socket.getPos();
        Vec3 chainVec = SpaceUtil.subtract(realHookLocation, selfPos).normalize();
        SpaceUtil.incrScale(chainVec, -FORCE_PER_TICK * scale);
        return chainVec;
    }

    public void setChain(IDeltaChunk idc, Vec3 hookLocation, Coord hookedBlock) {
        if (hookedIdc.trackingEntity()) {
            getCoord().spawnItem(Core.registry.darkIronChain);
        }
        hookedIdc.trackEntity(idc);
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
        IDeltaChunk idc = hookedIdc.getEntity();
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
    public int getComparatorValue(ForgeDirection side) {
        return compareValue;
    }

    @Override
    public void renderStatic(ServoMotor motor, Tessellator tess) {
        SocketBareMotor sbm = (SocketBareMotor) FactoryType.SOCKET_BARE_MOTOR.getRepresentative();
        getCoord().setAsTileEntityLocation(sbm);
        sbm.facing = facing;
        sbm.renderStatic(motor, tess);
        sbm.setWorldObj(null); // Don't leak the world (TODO: Unset all worldObj for all representitives when the world unload?)
    }


    ChainLink chainDraw;
    float chainLen, prevChainLen;
    double chainDelta = 0;
    boolean soundActive;
    byte spinSign = +1;

    void updateChain(ISocketHolder socket) {
        IDeltaChunk idc = hookedIdc.getEntity();
        if (idc == null) return;
        Vec3 realHookLocation = idc.shadow2real(hookLocation);
        Vec3 selfPos = socket.getPos();
        Vec3 chainVec = SpaceUtil.subtract(realHookLocation, selfPos);
        Vec3 point = SpaceUtil.fromDirection(facing);
        Vec3 right = SpaceUtil.scale(point.crossProduct(chainVec).normalize(), sprocketRadius);
        spinSign = (byte) (SpaceUtil.sum(right) > 0 ? +1 : -1);
        SpaceUtil.incrAdd(selfPos, right);
        float len = (float) SpaceUtil.lineDistance(selfPos, realHookLocation);
        if (worldObj.isRemote) {
            setChainDraw(realHookLocation, selfPos, len);
        } else {
            chainLen = len;
        }
    }

    private void setChainDraw(Vec3 realHookLocation, Vec3 selfPos, float len) {
        NORELEASE.fixme("How's this gonna work on the server?");
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
    public Vec3 getPos() {
        double d = 0.5;
        return Vec3.createVectorHelper(xCoord + d, yCoord + d, zCoord + d);
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

    @Override
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
        FactorizationBlockRender.renderItemIIcon(getCreatingItem().getItem().getIconFromDamage(0));
    }

    Coord getAnchorBlock() {
        IDeltaChunk idc = hookedIdc.getEntity();
        if (idc == null) return null;
        return new Coord(idc.getCorner().w, 0, 0, 0).add(hookDelta);
    }

    @Override
    public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) {
        InertiaCalculator.dirty(idc);
        return false;
    }

    @Override
    public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        Coord anchorPoint = getAnchorBlock();
        if (anchorPoint == null) return false;
        if (anchorPoint.equals(at)) {
            breakChain();
        }
        InertiaCalculator.dirty(idc);
        return false;
    }

    @Override
    public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        return false;
    }

    @Override
    public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        return false;
    }

    @Override
    public void idcDied(IDeltaChunk idc) {

    }

    @Override
    public void beforeUpdate(IDeltaChunk idc) {

    }

    @Override
    public void afterUpdate(IDeltaChunk idc) {
        if (idc.hasOrderedRotation() || !idc.getRotationalVelocity().isZero() || idc.motionX != 0 || idc.motionY != 0 || idc.motionZ != 0) {
            updateComparator();
        }
    }
}
