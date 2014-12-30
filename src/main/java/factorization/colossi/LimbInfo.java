package factorization.colossi;

import java.io.IOException;

import net.minecraft.world.World;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.EntityReference;
import factorization.shared.NORELEASE;

class LimbInfo {
    LimbType type = LimbType.UNKNOWN_LIMB_TYPE;
    BodySide side = BodySide.UNKNOWN_BODY_SIDE;
    byte parity = 0; // If you have a centipede, you kind of need to swap left & right every other foot
    int length; // How long the limb is
    EntityReference<IDeltaChunk> idc;
    
    byte lastTurnDirection = 0;
    
    public LimbInfo(LimbType type, BodySide side, int length, IDeltaChunk ent) {
        this(ent.worldObj);
        this.type = type;
        this.side = side;
        this.length = length;
        this.idc.trackEntity(ent);
    }
    
    public LimbInfo(World world) {
        idc = new EntityReference<IDeltaChunk>(world);
    }


    void putData(DataHelper data, int index) throws IOException {
        type = data.as(Share.VISIBLE, "limbType" + index).putEnum(type);
        side = data.as(Share.VISIBLE, "limbSide" + index).putEnum(side);
        parity = data.as(Share.VISIBLE, "limbParity" + index).putByte(parity);
        length = data.as(Share.VISIBLE, "limbLength" + index).putInt(length);
        idc = data.as(Share.VISIBLE, "entUuid" + index).put(idc);
        lastTurnDirection = data.as(Share.VISIBLE, "lastTurnDir" + index).put(lastTurnDirection);
    }
    
    boolean limbSwingParity() {
        return side == BodySide.RIGHT ^ (parity % 2 == 0) ^ type == LimbType.ARM;
    }
    
    public void setTargetRotation(Quaternion rot, int time, Interpolation interp) {
        IDeltaChunk dse = idc.getEntity();
        if (dse == null) return;
        dse.orderTargetRotation(rot, time, interp);
    }
    
    /**
     * Orders this limb to move to a target rotation. The rotation time is dependent on the rotational distance, the limb size, and power.
     * @param rot target rotation
     * @param power How much (completely fake) power to use. Higher amounts of power make the limb move faster. 1 is the default value.
     */
    public void target(Quaternion rot, double power) {
        target(rot, power, Interpolation.SMOOTH);
    }
    
    public void target(Quaternion rot, double power, Interpolation interp) {
        NORELEASE.fixme("implement!");
        setTargetRotation(rot, 20, interp);
    }
    
    public boolean isTurning() {
        IDeltaChunk dse = idc.getEntity();
        if (dse == null) return true; // isTurning() is used to block action, so we'll return true here
        return dse.hasOrderedRotation();
    }
    
    public void reset(int time, Interpolation interp) {
        IDeltaChunk dse = idc.getEntity();
        if (dse == null) return;
        Quaternion zero = new Quaternion();
        dse.multiplyParentRotations(zero);
        setTargetRotation(zero, time, interp);
    }
}