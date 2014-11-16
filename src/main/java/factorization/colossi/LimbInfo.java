package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.world.World;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.EntityReference;

class LimbInfo {
    static UUID fake_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
    LimbType type = LimbType.UNKNOWN_LIMB_TYPE;
    BodySide side = BodySide.UNKNOWN_BODY_SIDE;
    byte parity = 0; // If you have a centipede, you kind of need to swap left & right every other foot
    int length; // How long the limb is
    final EntityReference<IDeltaChunk> idc;
    Quaternion startRotation = new Quaternion(), endRotation = new Quaternion();
    private static final long NO_TIME = -1;
    long startTime = NO_TIME, endTime = NO_TIME;
    
    transient boolean act_violently = false;
    
    public LimbInfo(World world) {
        idc = new EntityReference<IDeltaChunk>(world);
    }
    
    public LimbInfo(LimbType type, BodySide side, int length, IDeltaChunk ent) {
        this(ent.worldObj);
        this.type = type;
        this.side = side;
        this.length = length;
        this.idc.trackEntity(ent);
    }


    void putData(DataHelper data, int index) throws IOException {
        type = data.as(Share.VISIBLE, "limbType" + index).putEnum(type);
        side = data.as(Share.VISIBLE, "limbSide" + index).putEnum(side);
        parity = data.as(Share.VISIBLE, "limbParity" + index).putByte(parity);
        length = data.as(Share.VISIBLE, "limbLength" + index).putInt(length);
        data.as(Share.VISIBLE, "entUuid" + index).put(idc);
        startRotation = data.as(Share.VISIBLE, "rotStart" + index).put(startRotation);
        endRotation = data.as(Share.VISIBLE, "rotEnd" + index).put(endRotation);
        startTime = data.as(Share.VISIBLE, "startTime" + index).putLong(startTime);
        endTime = data.as(Share.VISIBLE, "endTime" + index).putLong(endTime);
    }
    
    boolean limbSwingParity() {
        return side == BodySide.RIGHT ^ (parity % 1 == 0) ^ type == LimbType.ARM;
    }
    
    public void setTargetRotation(Quaternion rot, int time) {
        IDeltaChunk dse = idc.getEntity();
        if (dse == null) return;
        startRotation = new Quaternion(dse.getRotation());
        endRotation = new Quaternion(rot);
        startTime = dse.worldObj.getTotalWorldTime();
        endTime = startTime + time;
        
        // TODO: smooth acceleration
        double t = 1.0 / time;
        Quaternion velocity = startRotation.slerp(endRotation, t);
        dse.setRotationalVelocity(velocity);
    }
    
    void tick() {
        IDeltaChunk dse = idc.getEntity();
        if (dse == null) return;
        if (startRotation.equals(endRotation)) {
            return;
        }
        long now = dse.worldObj.getTotalWorldTime();
        if (now >= endTime) {
            // FIXME: We could be off by a tick, depending on when setTargetRotation gets called?
            dse.setRotationalVelocity(new Quaternion());
            dse.setRotation(endRotation);
            startRotation = endRotation;
            startTime = endTime = NO_TIME;
        }
    }
    
    boolean isTurning() {
        return endTime != NO_TIME;
    }
}