package factorization.servo.stepper;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.servo.AbstractServoMachine;
import factorization.servo.MotionHandler;
import factorization.servo.TileEntityServoRail;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.shared.NetworkFactorization;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.Collection;

public class StepperEngine extends AbstractServoMachine {
    public StepperEngine(World w) {
        super(w);
        setSize(1, 1);
        grabber.setWorld(w);
    }

    @Override
    protected void entityInit() {

    }

    final EntityReference<EntityGrabController> grabber = new EntityReference<EntityGrabController>();
    int number_of_grabbed_blocks = 0;
    static int MAX_SIZE = 16 * 16 * 8;

    @Override
    public void putData(DataHelper data) throws IOException {
        super.putData(data);
        data.as(Share.VISIBLE, "grabber").putIDS(grabber);
        number_of_grabbed_blocks = data.as(Share.VISIBLE, "numberOfGrabbedBlocks").putInt(number_of_grabbed_blocks);
    }

    @Override
    public boolean handleMessageFromClient(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        return super.handleMessageFromClient(messageType, input);
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        return super.handleMessageFromServer(messageType, input);
    }

    @Override
    protected void markDirty() {

    }

    @Override
    public boolean extractAccelerationEnergy() {
        return extractCharge(number_of_grabbed_blocks + 2);
    }

    @Override
    public void updateServoLogic() {

    }

    @Override
    protected MotionHandler newMotionHandler() {
        return new StepperMotionHandler(this);
    }

    @Override
    protected void dropItemsOnBreak() {
        new Coord(this).spawnItem(Core.registry.stepper_placer);
    }

    @Override
    public void onEnterNewBlock() {
        TileEntityServoRail rail = getCurrentPos().getTE(TileEntityServoRail.class);
        if (rail != null && rail.decoration != null) {
            rail.decoration.stepperHit(this);
        }
    }

    public boolean grabbed() {
        return grabber.trackingEntity();
    }

    public void fzdsGrab() {
        if (worldObj.isRemote) return;
        if (grabbed()) return;
        Coord front = getCurrentPos().add(getOrientation().top);
        if (grabExistingIdc(front)) return;
        grabNewIdc(front);
        broadcastFullUpdate();
    }

    private void grabNewIdc(Coord front) {
        if (front.isReplacable() || front.isAir()) return;
        GrabConnector gc = new GrabConnector(front, MAX_SIZE);
        final Collection<Coord> mesh = gc.fill();
        Coord min = null, max = null;
        for (Coord c : mesh) {
            if (min == null) {
                min = c.copy();
                max = c.copy();
            } else {
                c = c.copy();
                Coord.sort(min, c);
                Coord.sort(c, max);
            }
        }
        if (min == null) return;
        IDeltaChunk idc = DeltaChunk.makeSlice(ItemStepperEngine.channel, min, max, new DeltaChunk.AreaMap() {
            @Override
            public void fillDse(DeltaChunk.DseDestination destination) {
                for (Coord c : mesh) {
                    destination.include(c);
                }
            }
        }, true);
        idc.permit(
                DeltaCapability.COLLIDE,
                DeltaCapability.MOVE,
                DeltaCapability.ROTATE,
                DeltaCapability.DRAG,
                DeltaCapability.DIE_WHEN_EMPTY,
                DeltaCapability.INTERACT,
                DeltaCapability.BLOCK_PLACE,
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.REMOVE_ITEM_ENTITIES
                /* NORELEASE: DeltaCapability.COLLIDE_WITH_WORLD */
                );
        idc.setPosition(posX, posY, posZ);
        worldObj.spawnEntityInWorld(idc);
        grabIdc(idc);
    }

    private boolean grabExistingIdc(Coord front) {
        double extreme_max = 4096 * 4096;
        for (IDeltaChunk idc : DeltaChunk.getAllSlices(worldObj)) {
            if (!idc.can(DeltaCapability.COLLIDE_WITH_WORLD)) continue;
            if (!(idc.getController() instanceof EntityGrabController)) continue;
            EntityGrabController egc = (EntityGrabController) idc.getController();
            if (!egc.isUngrabbed()) continue;
            if (getDistanceSqToEntity(idc) > extreme_max) continue;
            Coord grabPoint = idc.real2shadow(front);
            if (SpaceUtil.contains(SpaceUtil.createAABB(idc.getCorner(), idc.getFarCorner()), grabPoint)) {
                grabIdc(idc);
                return true;
            }
        }
        return false;
    }

    private void grabIdc(IDeltaChunk idc) {
        //NORELEASE.fixme("translate the origin so that the origin's located at ourselves");
        EntityGrabController egc = new EntityGrabController(this, idc, DropMode.EVENTUALLY);
        worldObj.spawnEntityInWorld(egc);
        egc.mountEntity(this);
        grabber.trackEntity(egc);
        idc.setController(egc);
    }

    public void fzdsRelease() {
        if (worldObj.isRemote) return;
        if (!grabbed()) return;
        EntityGrabController egc = grabber.getEntity();
        if (egc == null) {
            Core.logWarning("Stepper Engine can't drop! DSE grabber entity is not loaded! Sorry! " + this + " " + grabber.getUUID()); // Ack!
            // Ah, but this probably won't happen since the thing'll be riding us!
            return;
        }
        egc.release();
        egc.mountEntity(null);
        grabber.trackEntity(null);
        broadcastFullUpdate();
    }

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false; // Probably will never be called due to always being on servo rails?
    }
}
